# WebDAV 网盘接入排障实录

> 场景：第 11 步 Windows 真机验收 WebDAV 网盘。后端运行在 8081（IDEA 启动），admin 密码 `admin123`。用户反馈"请求一直不正确"。
> 本文记录从"症状模糊"到"完全打通"的完整排障链路，以及从中提炼的方法论。

## 一、最终结论（先看这个）

WebDAV 链路要打通，必须同时满足 **5 个叠加条件**，缺任何一个都会表现为"请求不正确"：

| # | 条件 | 缺失时的症状 |
|---|------|--------------|
| 1 | WebDAV 相关 Bean 进入了运行时 Spring 容器 | 行为与代码矛盾，像是没改过 |
| 2 | 原生 Servlet 映射 `/webdav/*`，绕过 DispatcherServlet | PROPFIND/MKCOL 被 Spring MVC 接走 → 400 |
| 3 | Spring Security `StrictHttpFirewall` 放行 WebDAV 动词 | OPTIONS 通了但 PROPFIND 仍 400 |
| 4 | `WebDavAuthFilter` 只在 Security 链内运行一次，不重复注册 | 带 Basic 的 PROPFIND 被裸 401，SecurityContext 被洗成匿名 |
| 5 | 鉴权链匹配 `/webdav/**` 且优先级最高 | 无认证无 Basic challenge，被主 JWT 链抢走 |

打通后的正确响应指纹：
- 无认证 → `401` + `WWW-Authenticate: Basic realm="ERP WebDAV"`
- 带 Basic 的 `OPTIONS` → `200` + `DAV: 1,2` + `Allow: OPTIONS, PROPFIND, GET, PUT, MKCOL, DELETE, MOVE, LOCK, UNLOCK`
- `PROPFIND` → `207 Multi-Status`（XML 列出部门/资源）
- `MKCOL` → `201 Created`

## 二、最小正确请求模板

```bash
# 1. 探测（无认证，应返回 401 + Basic challenge）
curl.exe -i -X OPTIONS http://127.0.0.1:8081/webdav/

# 2. 探测（带认证，应返回 200 + DAV 头）
curl.exe -i -X OPTIONS http://127.0.0.1:8081/webdav/ \
  -H "Authorization: Basic YWRtaW46YWRtaW4xMjM="   # base64(admin:admin123)

# 3. PROPFIND 列目录（应返回 207）
curl.exe -i -X PROPFIND http://127.0.0.1:8081/webdav/ \
  -H "Authorization: Basic YWRtaW46YWRtaW4xMjM=" \
  -H "Depth: 1" \
  -H "Content-Type: application/xml; charset=utf-8" \
  --data "<?xml version='1.0'?><D:propfind xmlns:D='DAV:'><D:allprop/></D:propfind>"

# 4. MKCOL 建目录（应返回 201）
curl.exe -i -X MKCOL http://127.0.0.1:8081/webdav/test-mkcol \
  -H "Authorization: Basic YWRtaW46YWRtaW4xMjM="
```

生成 Basic 头：
```powershell
[Convert]::ToBase64String([Text.Encoding]::UTF8.GetBytes('admin:admin123'))
```

## 三、WebDAV 的三个硬约束（设计层面）

这三条是"为什么 WebDAV 不能像普通接口一样接"的根因，任何 WebDAV 接入都要遵守：

1. **不能走登录接口 / 验证码**：Windows 资源管理器只会发 Basic Auth，不会调 `/api/v1/auth/login`，也不会填验证码。WebDAV 必须用 `Authorization: Basic base64(user:pass)` 直连，由专用 Filter 校验账号密码。
2. **必须绕过 Spring MVC**：`PROPFIND`/`MKCOL`/`MOVE`/`LOCK`/`UNLOCK` 是非标准 HTTP 方法，Spring MVC 的 `@RequestMapping` 走 `DispatcherServlet` 会拒绝。必须用原生 `HttpServlet` 接收。
3. **必须放宽 HttpFirewall**：Spring Security 默认 `StrictHttpFirewall` 只放行标准方法，WebDAV 动词会被当非法方法拦掉 → 400。

## 四、逐层排障过程

整个调试沿请求生命周期逐层设卡，每层用一个**特征信号**判断是否命中。

### 层 1：端口与路径探测
- 现象：`OPTIONS http://127.0.0.1:8081/webdav/` 能命中服务返回 401。
- 结论：端口和路径至少通；401 是"被某层拦了"，需进一步定位是哪层。

### 层 2：区分"密码错" vs "链路没加载"
- 用正确 JSON 重测 `/api/v1/auth/login` → 200，确认 `admin/admin123` 有效。
- 用带 Basic 的 OPTIONS 仍 401 → **不是密码问题，是 WebDAV 链路没接住请求**。
- 教训：验证码只影响网页登录流程，与 WebDAV 无关，不要被带偏。

### 层 3：确认 Bean 是否进入运行时
- 怀疑：IDEA 运行 classpath 用了旧产物 / 没把 erp-data 新类编进去。
- 手段：加 `WebDavStartupDiagnostics` 启动日志，打印 `servletBean/securityConfigBean/authFilterBean/controllerBean` 是否为 true。
- 结果：四个 Bean 全 true → Bean 已加载，排除"没加载"假设。
- 价值：把"猜"变成"看"，避免继续盲改业务逻辑。

### 层 4：Servlet 映射（绕过 DispatcherServlet）
- 现象：Bean 在，但 PROPFIND 仍 400 → 请求被 `DispatcherServlet` 抢走。
- 修复：从 `ServletRegistrationBean` 改为 `ServletContextInitializer` 手动注册：
  ```java
  servletContext.addServlet("webdavServlet", controller).addMapping("/webdav/*");
  ```
- 结果：MKCOL 能进 Controller 返回 XML 403，证明 Servlet 映射通了。

### 层 5：HttpFirewall 放行 WebDAV 动词
- 现象：OPTIONS 通了（200），但 PROPFIND 仍 400。
- 根因：`StrictHttpFirewall` 默认白名单不含 PROPFIND/MKCOL 等。
- 修复：扩展 firewall 允许的方法集。
- 结果：PROPFIND 不再 400。

### 层 6：认证上下文被洗掉（最隐蔽的坑）
- 现象：无认证 PROPFIND 被 WebDAV Filter 拦住（正确）；带 Basic 的 PROPFIND 反而被裸 401。
- 根因：`WebDavAuthFilter` 是 `@Component`，被 Spring Boot **自动注册成全局 Servlet Filter**，先跑一次写入 SecurityContext；随后主 Spring Security 链又把 SecurityContext **重置成匿名**，于是带认证请求被当未认证拒绝。
- 修复：禁用该 Filter 的全局 Servlet Filter 自动注册，只保留它在 Spring Security WebDAV 链内运行一次。
- 结果：带 Basic 的 PROPFIND → 207，彻底打通。

## 五、提炼的方法论

### 1. 用"信号差异"定位层级，而不是猜
每次只改一个变量，观察**状态码 + 响应头特征**变化。不同层返回的 401/400 长得不一样，响应头是辨别"哪一层在说话"的指纹：

| 响应特征 | 指向的层 |
|----------|----------|
| 裸 401，无 `WWW-Authenticate` | 主 JWT 链拦截（没进 WebDAV 链） |
| 401 + `WWW-Authenticate: Basic realm="ERP WebDAV"` | WebDAV 链正确拦截，只是没带认证 |
| 400，无 XML body | Spring MVC / Firewall 拒绝非标准方法 |
| 403/409 + XML `<D:error>` body | 已进 WebDAV Controller，业务层拒绝 |
| 207 Multi-Status | 完全打通 |

### 2. 区分"代码对"和"运行时加载"
Bean 单测通过 ≠ 运行时生效。当行为和代码矛盾时，**第一时间怀疑运行产物**（IDEA 旧 classpath / 旧 jar / 没真正重启）。用启动日志打印 Bean 是否存在，是省时手段。

### 3. 加诊断而非改业务
排障时优先加只读诊断代码（启动日志、请求日志 `[WebDAV-FILTER]`/`[WebDAV-CTRL]`），不碰业务逻辑。能快速收敛假设，又不会引入新变量。任务完成后移除诊断类。

### 4. 每改一处，必须完整重启验证
Spring Security 防火墙、Filter 注册等配置是启动期确定的，热替换不生效。每次改完都要**确认 PID 真的换了**再测（`netstat -ano | findstr :8081`），否则会得出错误结论。

### 5. 警惕"同一组件被注册两次"
本案例最隐蔽的坑：`OncePerRequestFilter` 同时被 Spring Boot 自动注册为全局 Filter 和放进 Security 链，导致 SecurityContext 被洗掉。
**通用规则**：`OncePerRequestFilter` 进 Spring Security 链时，务必禁用其全局自动注册（`FilterRegistrationBean.setEnabled(false)`）。

### 6. PowerShell / curl 在 Windows 下的坑
- PowerShell 的 `Invoke-WebRequest -Method` 不支持自定义动词（`PROPFIND`），用 `curl.exe -X PROPFIND` 代替。
- PowerShell 单引号 JSON 会被吃掉引号，用 `ConvertTo-Json` 或 `curl.exe --data '...'`。
- 命令行 Maven 默认可能挂在 JRE 上（"No compiler is provided"），需显式 `JAVA_HOME` 指向 JDK 17。
- 中文编码：PowerShell 默认编码会搅乱 UTF-8 中文，重读时用 `-Encoding UTF8`。

## 六、涉及的关键文件

| 文件 | 作用 |
|------|------|
| `erp-data/.../webdav/WebDavController.java` | 原生 HttpServlet，分发 PROPFIND/MKCOL 等非标准方法 |
| `erp-data/.../webdav/WebDavServletRegistration.java` | `ServletContextInitializer` 手动注册 `/webdav/*` |
| `erp-data/.../webdav/WebDavSecurityConfig.java` | WebDAV 专用 SecurityFilterChain + Firewall 放行 + 禁用 Filter 全局注册 |
| `erp-data/.../webdav/WebDavAuthFilter.java` | Basic Auth 校验，写入 SecurityContext |
| `erp-web/.../WebDavStartupDiagnostics.java` | 启动诊断日志（任务完成后可移除） |

## 七、一句话总结

> 调试 WebDAV 这类"多框架叠加"问题，核心方法是：**沿请求生命周期逐层设卡观察（端口 → Servlet 映射 → 防火墙 → 安全链 → Controller），每层用一个特征信号判断是否命中，绝不跳层猜测；遇到行为与代码矛盾，先验证运行时加载再改逻辑。**

---

*记录时间：2026-06-29*