# 外贸 ERP（demo2）— 项目踩坑与解决方案

> **项目位置**：`server3:/code/demo2`
> **文档定位**：开发/运维过程中遇到的问题、根因分析、最终解法
> **创建日期**：2026-06-24（项目初始化阶段建立骨架，开发推进中持续补充）
> **结构约定**：每条按「问题 → 根因 → 解法 → 验证 / 教训」三段式

---

## 目录

1. [⚠️ 已识别的开局陷阱（开发开始前必读）](#1-已识别的开局陷阱开发开始前必读)
2. [server3 / CentOS 7 环境兼容性](#2-server3--centos-7-环境兼容性)
3. [Maven 多模块与 Spring Boot 版本](#3-maven-多模块与-spring-boot-版本)
4. [数据库 / Flyway / MyBatis-Plus](#4-数据库--flyway--mybatis-plus)
5. [Spring Security / JWT / RSA 加密登录](#5-spring-security--jwt--rsa-加密登录)
6. [前端 Vite / Element Plus / Axios](#6-前端-vite--element-plus--axios)
7. [Docker 中间件部署](#7-docker-中间件部署)
8. [Git / 版本控制 / 文档同步](#8-git--版本控制--文档同步)
9. [通用约束（沿用 B2B 项目规则）](#9-通用约束沿用-b2b-项目规则)

---

## 1. ⚠️ 已识别的开局陷阱（开发开始前必读）

> 这些问题在代码盘点阶段就已发现，开始 B0 第 0 步前先处理。

### 1.1 pom.xml 父版本是 Spring Boot 4.1.0 (RC)

- **问题**：`pom.xml` 第 8 行 `<version>4.1.0</version>`
- **根因**：脚手架默认拉了最新版（4.1 是 RC 不是 LTS）
- **影响**：MyBatis-Plus、Knife4j、Spring Security 等核心依赖**还没适配 Spring Boot 4.x**；4.x 对 Jakarta EE 9 / Servlet 6 / Spring 7 还在演进，生产用极度危险
- **解法**：按 `DEV_GUIDE §2.1` 备注降到 **3.3.5 LTS**
  ```xml
  <parent>
      <groupId>org.springframework.boot</groupId>
      <artifactId>spring-boot-starter-parent</artifactId>
      <version>3.3.5</version>
      <relativePath/>
  </parent>
  ```
- **验证**：`./mvnw -v` + `./mvnw clean compile` BUILD SUCCESS

### 1.2 guide/ 被 .gitignore 整目录忽略

- **问题**：`.gitignore` 第 3 行 `guide/` — 设计文档（DEV_GUIDE / DEV_STEPS / API_DESIGN / erp_schema.sql 共 ~13 万字）不会进版本控制
- **根因**：脚手架阶段把 guide 当作私人草稿目录排除
- **影响**：团队协作时新成员 git clone 不到设计文档；CI/CD 也读不到 schema
- **解法**：二选一
  - **A 推荐**：从 `.gitignore` 删除 `guide/`，把文档纳入版本控制（设计文档变更可追溯）
  - **B**：保留忽略 + 团队共享网盘单独维护
- **验证**（用 B2B 项目经验的二次校验规则）：
  ```bash
  # 改完 .gitignore 后必须复查
  git check-ignore -v guide/DEV_GUIDE.md  # 选 A 后应无输出
  git ls-files | grep -c "guide/"          # 选 A 后应 > 0
  ```

### 1.3 后端只有 1 个空启动类

- **问题**：`src/main/java/com/example/demo/Demo2Application.java` 是 Spring Initializr 默认产物，包路径是 `com.example.demo` 而非 `com.erp`
- **解法**：B0.2 重构父 POM 时连带改包路径，后续 14 个子模块统一在 `com.erp.{模块}` 下
- **教训**：脚手架后第一步先校对包名与 groupId，避免后续大量包路径迁移

### 1.4 前端缺 api/ 与 utils/ 业务目录

- **问题**：30 个 `.vue` 页面只是 UI 壳，无 axios 实例、无接口调用层、无路由守卫逻辑
- **解法**：B1.3 必须先补齐：
  ```
  erp-frontend/src/
  ├── api/
  │   ├── request.ts       # axios 实例 + 拦截器
  │   ├── auth.ts          # /auth/* 接口封装
  │   ├── user.ts
  │   └── ...
  ├── utils/
  │   ├── auth.ts          # token 存取
  │   └── rsa.ts           # RSA 加密（jsencrypt）
  └── types/               # TS 接口定义
  ```
- **教训**：UI 壳完成 ≠ 前端完成，联调前请检查接口层

### 1.5 前端包管理混用风险

- **现状**：`erp-frontend/package.json` 存在 + `package-lock.json` 存在（说明用了 npm）
- **DEV_STEPS 建议**：用 pnpm
- **解法**：B1.3 切换时 `rm package-lock.json && pnpm install`（生成 `pnpm-lock.yaml`）
- **教训**：包管理器决定一次，别 npm/yarn/pnpm 混用，lockfile 冲突会让 CI 抓狂

---

## 2. server3 / CentOS 7 环境兼容性

> server3 是 **CentOS 7 + glibc 2.17**，已有大量沿用 B2B 项目的经验。

### 2.1 JDK 17 安装

- **问题**：CentOS 7 默认 yum 仓库只有 OpenJDK 8/11
- **预期解法**（待 B0.1 验证）：
  - 用 Adoptium / Zulu 的 portable 包（tar.gz 解压即用，不依赖 glibc 2.28+）
  - 或 SDKMAN：`curl -s "https://get.sdkman.io" | bash && sdk install java 17.0.13-zulu`
- **教训**：CentOS 7 EOL，新语言运行时一律走便携式构建

### 2.2 Node 20 + pnpm

- **沿用 B2B 项目经验**：
  - nvm 0.40.x 从 Gitee 镜像安装（GitHub 在 server3 被墙）
  - `nvm install 20` 走 npmmirror 的 glibc-217 非官方构建
  - `npm install -g pnpm` 用 npmmirror

### 2.3 Docker 在 CentOS 7

- **风险**：Docker 26+ 已停止 CentOS 7 支持
- **预期解法**：
  - 锁定 Docker 24.0.x（最后一个支持 CentOS 7 的大版本）
  - 或：宿主只跑 MySQL/Redis/MinIO 原生二进制（避开 Docker）
  - 或：把中间件挪到 server4（Ubuntu 24.04，已装 Docker 29.6）
- **决策**：待 B0.1 实地验证

### 2.4 端口占用排查

- 已知 server3 上 PM2 跑过 B2B 项目（目前已 stop，但 3000 端口若未释放干净需注意）
- ERP 后端默认 8080，前端 dev 5173，与 B2B 项目（Nginx 监听 8080）**冲突**
- **解法**：开发期把 ERP 后端改 8081 或 8088；上线时分两个 Nginx server_name

---

## 3. Maven 多模块与 Spring Boot 版本

> 暂未实战，骨架预留。开发过程中遇到坑随时填充。

- **3.1** Spring Boot 3.x → 4.x 升级注意事项（如未来需升）
- **3.2** MyBatis-Plus 3.5.7 与 Spring Boot 3.3.5 适配（starter artifactId 是 `mybatis-plus-spring-boot3-starter` 不是 `mybatis-plus-boot-starter`）
- **3.3** 子模块循环依赖排查
- **3.4** Maven Profile 切换开发/测试/生产配置
- **3.5** Lombok + IDEA：必须装 Lombok 插件 + 开启 annotation processing
- **3.6** 多模块下 `application.yml` 的加载顺序（建议主配置只在 `erp-web` 模块）

---

## 4. 数据库 / Flyway / MyBatis-Plus

> 骨架预留，B1.2 起步时填充实际遇到的问题。

- **4.1** Flyway baseline 与已有数据库的对接
- **4.2** MySQL 8 utf8mb4_unicode_ci vs utf8mb4_0900_ai_ci（外贸数据建议后者）
- **4.3** MyBatis-Plus 自动填充 `@TableField(fill = FieldFill.INSERT)` 与 BaseEntity
- **4.4** 软删除 `@TableLogic` + 索引（`WHERE deleted = 0` 必须走索引）
- **4.5** 乐观锁 `@Version` 与 update 失败的业务处理（抛 409）
- **4.6** Flyway 与 Spring Boot 3.x 配置：`spring.flyway.locations=classpath:db/migration`
- **4.7** `guide/erp_schema.sql` 拆成 V1/V2/V3... 多个迁移脚本的策略

---

## 5. Spring Security / JWT / RSA 加密登录

> 骨架预留，B1.4 实战时填充。

- **5.1** Spring Security 6.x SecurityFilterChain 配置（已无 WebSecurityConfigurerAdapter）
- **5.2** JWT Access + Refresh 双 Token 设计（Redis 黑名单）
- **5.3** RSA 公钥发到前端 + 前端用 jsencrypt 加密密码（API_DESIGN §2 要求）
- **5.4** 验证码触发条件（连续 3 次失败 → 强制验证码）
- **5.5** `@RequirePermission` 切面与 AOP 顺序
- **5.6** 数据权限 `data_scope`（1=本人/2=本部门/3=本部门及子部门/4=全部）的 SQL 拼接

---

## 6. 前端 Vite / Element Plus / Axios

> 骨架预留，B1.3 + 联调时填充。

- **6.1** Vite 5 代理 `/api` 到后端的写法
- **6.2** Element Plus 按需自动导入（unplugin-vue-components 已配，注意与 Element Plus Icons 的命名冲突）
- **6.3** Axios 拦截器：401 触发 refresh-token 的去重排队（避免并发请求触发多次 refresh）
- **6.4** 路由守卫：根据后端返回的 `permissions[]` 动态生成菜单
- **6.5** Pinia 持久化（pinia-plugin-persistedstate）vs 仅 sessionStorage
- **6.6** vue-i18n 9.x setup 模式（不能用 legacy mode 否则 Composition API 不生效）

---

## 7. Docker 中间件部署

> B0.1 实操时填充。

- **7.1** MySQL 容器持久化 volume 与备份脚本
- **7.2** Redis 持久化策略（RDB vs AOF）
- **7.3** MinIO `--console-address` 与 9001 端口暴露
- **7.4** docker compose v1 vs v2 命令差异（CentOS 7 注意）
- **7.5** 容器互连用 service name 而非 localhost

---

## 8. Git / 版本控制 / 文档同步

### 8.1 `.gitignore` 改动验证规则（沿用 B2B 项目铁律）

- **规则**：用 patch/write_file 整文件覆盖 `.gitignore` 后，**必须二次验证**：
  ```bash
  # 1. 关键忽略规则仍生效
  git check-ignore -v target/
  git check-ignore -v erp-web/src/main/resources/application-dev.yml  # 若含凭据需忽略

  # 2. 索引中无真凭据/编译产物
  git ls-files | grep -iE "\.env$|\.production|password|target/"
  ```
- **历史教训**：B2B 项目曾因整文件覆盖 .gitignore 丢失追加规则，差点把含真密码的 `ecosystem.config.js` 提交入库

### 8.2 多服务器文档同步

- **现状**：本项目仅在 server3，不需要多机同步
- **若以后用 server2 备份**：参照 B2B 项目用 `base64 -w0 file | ssh host 'echo $B64 | base64 -d > /path'` 管道传输 + md5sum 双向核对

### 8.3 大文件提交保护

- `guide/erp_schema.sql` 51KB 可以入库
- 单证示例 PDF / 培训视频禁止入 git（用 Git LFS 或 OSS）

---

## 9. 通用约束（沿用 B2B 项目规则）

### 9.1 禁止删除任何文件

- **不能用 `rm` / `unlink` / `delete`**
- 需要"删除"时：
  - 改为重命名加 `.bak` / `.old` 后缀
  - 或移动到 `/code/rebbish/`（如不存在则创建）
- **配置文件改动同样按此规则处理**，便于回滚

### 9.2 文档同步前后台核对

- 任何文档/配置文件改动 → `md5sum` 三处（server0 / server3 / 备份处）核对一致后才算完成

### 9.3 SSH 频繁连接限制

- server3 的 SSH 配了 MaxStartups（CentOS 7 老 sshd）
- 频繁并发 ssh 调用之间需 `sleep 2-3`
- 优先合并命令：用 base64 一次性传 shell 脚本到远端执行

### 9.4 进度汇报真实性

- 不夸大完成度
- 部分完成的项目标 🟡（进行中）不是 ✅
- ROADMAP 看板表的数字必须随勾选同步更新

---

## 📝 维护说明

- 本文档随项目推进**持续填充**
- 每次踩坑后第一时间记录，不要等"以后补"——上下文一过就忘了
- 章节 3-7 现在多是骨架占位，开发到对应阶段时把真实问题填进去
- 文档变更日志维护在 `ROADMAP.md` 的"变更日志"表，本文不重复

| 日期 | 变更 |
|------|------|
| 2026-06-24 | 文档骨架建立 + 已识别的 5 个开局陷阱 + CentOS 7 沿用经验 + 通用约束 |
| 2026-06-26 | 追加 §31~§35：systemd 自动重启、雪花 ID 精度丢失、软删除唯一索引冲突、erp-report 缺 mybatis-plus 依赖、Vue el-table 类型推断 |
 
### 6. JDK 17 国内源大多 404，仅华为云 OpenJDK GA 可用（CentOS 7）

**症状**：
- npmmirror Adoptium 镜像：`[NOT_FOUND] Binary "adoptium" not found`
- 清华 TUNA：SPA 化主页，老旧路径 `/Adoptium/17/jdk/x64/linux/...` 全 404
- 中科大 USTC：403 Forbidden
- 腾讯云/BFSU：404
- GitHub Adoptium releases：能连通但下载 3 KB/s（实测 3 分钟 1.3MB / 182MB）

**唯一稳定的国内源**：
```bash
# OpenJDK 17 GA (Oracle 官方编译)，速度 10 MB/s
curl -fL -o jdk17.tar.gz   https://mirrors.huaweicloud.com/openjdk/17/openjdk-17_linux-x64_bin.tar.gz
```
注意：华为云只有 17 GA（无 17.0.1/17.0.2 之后的子版本），但 Spring Boot 3.3.5 + Spring Framework 6 完全兼容 17 GA。如果未来需要 Temurin/特定子版本，备选：先从能联通的 GitHub 慢速下载到 server0（境外更快），再 base64 推送到 server3。

### 7. Maven Wrapper（mvnw）首次启动会卡住

**症状**：项目自带的 `.mvn/wrapper/maven-wrapper.properties` 指向 `repo.maven.apache.org`，CentOS 7 直连境外 Maven 中心仓库速度极慢，`./mvnw` 首次运行会卡在下载 Maven 二进制 8MB 这一步。

**解决**：直接装本地 Maven 跳过 wrapper：
```bash
curl -fL -o /tmp/maven.tar.gz   https://mirrors.huaweicloud.com/apache/maven/maven-3/3.9.9/binaries/apache-maven-3.9.9-bin.tar.gz
tar xzf /tmp/maven.tar.gz -C /opt && mv /opt/apache-maven-3.9.9 /opt/maven
cat > /etc/profile.d/maven.sh << 'EOF'
export M2_HOME=/opt/maven
export PATH=$M2_HOME/bin:$PATH
EOF
```
配套 `~/.m2/settings.xml` 用阿里云镜像：
```xml
<mirror>
  <id>aliyunmaven</id>
  <mirrorOf>central</mirrorOf>
  <url>https://maven.aliyun.com/repository/public</url>
</mirror>
```

### 8. Git root-commit stage 残留：旧文件会被一起入库

**症状**：仓库从未提交过，先用 `git add` stage 了一批文件，后续又改文件、移动目录、做新 stage，**之前的 stage 仍然有效**。`git commit` 时会把所有累积的 stage 一起 commit，包括已经在工作区不存在的旧文件。

本次踩坑：B0.2 把 `src/main/java/com/example/demo/Demo2Application.java` 移走改成 `com.erp`，但首次 commit 的 `git status` 显示这个文件是 `AD`（add+delete），意思是「之前 stage 过 add，现在又 stage 过 delete」。`git commit` 仍然把它 `create mode`，因为 `AD` 不会自动跳过 add 部分——你需要再次 `git rm` 一遍才能真正从 commit 树里清除。

**验证方法**：
```bash
git ls-tree -r HEAD | grep -i 旧路径关键字   # 必须空
```
**修复**：
```bash
git rm -rf src/main/java/com/example src/test/java/com/example
git commit -m "cleanup: 移除入库的旧包"
```

**预防**：root-commit 之前先 `git status` 完整审查一遍，所有 `AD`/`AM` 行都要确认是想要的状态；移动目录后做一次 `git add -A`，让 git 同步检测删除。
---

## §31. 旧 jar 包 systemd 自动重启导致代码修改不生效【优化阶段 1】

### 现象
- 修改 Java 代码后重新打包，复制到 `/tmp/erp-web.jar`，重启 systemd 服务
- 接口行为仍然是旧的（比如删除后重建部门仍然报唯一索引冲突）
- `systemctl restart erp-web.service` 后日志显示启动的 jar 路径不对

### 根因
server3 上存在 systemd 服务 `erp-web.service`，配置为：
```ini
ExecStart=/usr/bin/java -jar /tmp/erp-web.jar
Restart=always
```
但 `/tmp/erp-web.jar` 在重启后可能被系统清理，或者复制时没覆盖成功。

### 解法
1. 确保 `/tmp/erp-web.jar` 确实被新包覆盖（`cp -f`）
2. 或者修改 systemd 服务指向固定路径（如 `/opt/erp/erp-web.jar`）
3. 重启后等待 10-15 秒再验证（Spring Boot 启动需要时间）

### 验证
```bash
systemctl restart erp-web.service
sleep 15
journalctl -u erp-web.service --since "10 seconds ago" | tail -5
# 应看到 "Started ErpApplication in Xs" 且版本号正确
```

**入坑日期**：2026-06-26（优化阶段 1）

---

## §32. 雪花 ID 精度丢失：Java Long 超出 JS MAX_SAFE_INTEGER【优化阶段 1】

### 现象
- 后端生成的 Snowflake ID（如 `2070199920249511938`）传给前端后，最后几位变成 0
- 前端删除部门后再创建同名部门，报唯一索引冲突（因为删除时传的 ID 是错的）
- 前端表格中 ID 显示为 `2070199920249512000`（精度丢失）

### 根因
JavaScript `Number.MAX_SAFE_INTEGER = 9007199254740991`（约 16 位），而 Snowflake ID 是 19 位。JSON 解析时 `2070199920249511938` 被截断为 `2070199920249512000`。

### 解法
全局 Jackson 配置将所有 `Long` 序列化为 `String`：
```java
@Bean
public Jackson2ObjectMapperBuilderCustomizer longToStringCustomizer() {
    return builder -> {
        builder.serializerByType(Long.class, ToStringSerializer.instance);
        builder.serializerByType(Long.TYPE, ToStringSerializer.instance);
    };
}
```

前端统一类型：
```typescript
export type Id = string | number;
```

### 验证
- 后端返回 `"userId": "2070199920249511938"`（带引号的字符串）✅
- 前端删除/更新操作使用正确 ID ✅
- 不再出现"删除后重建报唯一索引冲突"✅

**入坑日期**：2026-06-26（优化阶段 1）

---

## §33. 软删除 + 唯一索引冲突：删除后无法重建同名记录【优化阶段 1】

### 现象
- 删除部门（逻辑删除 `deleted=1`）后，再创建同名部门报 `Duplicate entry 'xxx' for key 'dept_code'`
- 前端无法删除"数据部"这个旧部门（精度丢失导致删除的是错误记录）

### 根因
MySQL 唯一索引 `UNIQUE INDEX uk_dept_code (dept_code)` 不考虑 `deleted` 字段，所以即使记录被逻辑删除，`dept_code` 仍然占用。

### 解法
改为复合唯一索引：
```sql
ALTER TABLE sys_department DROP INDEX dept_code;
ALTER TABLE sys_department ADD UNIQUE INDEX uk_dept_code_deleted (dept_code, deleted);
```
这样 `(dept_code='数据部', deleted=0)` 和 `(dept_code='数据部', deleted=1)` 可以同时存在。

### 验证
- 删除部门后再创建同名部门 ✅
- 已删除的部门代码不冲突 ✅

**入坑日期**：2026-06-26（优化阶段 1）

---

## §34. erp-report 模块缺 mybatis-plus 依赖导致编译失败【工作报表开发】

### 现象
新建 `RptWorkPlanMapper` / `RptWorkLogMapper`（继承 `BaseMapper`）后编译报错：
```
package com.baomidou.mybatisplus.core.mapper does not exist
```

### 根因
`erp-report/pom.xml` 原有依赖只有 `erp-common`、`erp-security`、`erp-order`、`erp-finance`、`easyexcel`，没有 MyBatis-Plus starter。

### 解法
```xml
<dependency>
    <groupId>com.baomidou</groupId>
    <artifactId>mybatis-plus-spring-boot3-starter</artifactId>
</dependency>
```
注意 artifactId 是 `mybatis-plus-spring-boot3-starter`（不是 `mybatis-plus-boot-starter`，后者是 SB2 的）。

**入坑日期**：2026-06-26（工作报表开发）

---

## §35. Vue el-table 行类型推断失败导致 TypeScript 报错【前端】

### 现象
`WorkReportManage.vue` 中使用 `<template #default="{ row }">` 访问 `row.planId` 时，TypeScript 报错：
```
Type 'DefaultRow' is missing the following properties from type 'WorkReportVO'
```

### 根因
Element Plus 的 `el-table` 默认行类型是 `DefaultRow`，与自定义的 `WorkReportVO` 不匹配。

### 解法
方案 A：显式声明 `data` 泛型（推荐）：
```vue
<el-table :data="records" :row-key="(row: WorkReportVO) => row.userId">
```

方案 B：slot 中用 `any` 然后手动断言（简单场景）：
```vue
<template #default="{ row }: any">
  <div @click="showDetail(row as WorkReportVO, 'plan')">
```

**入坑日期**：2026-06-26（工作报表管理界面开发）

---

## §36. `@ConditionalOnBean(DataSource.class)` 时序陷阱 ❌

**症状**：执行 `mvn clean compile` 在第一个子模块就报 `Non-parseable POM`：
```
TEXT must be immediately followed by END_TAG and not START_TAG
(position: START_TAG seen ...<description>基础共用层：BaseEntity / R<T>...)
```

**根因**：XML 字面 `<T>` 被当作未闭合的元素标签。Java 泛型语法在 XML 文本节点里属于非法字符。

**解决**：所有出现在 XML 文本节点中的 `<` `>` 必须实体化：
```xml
<description>R&lt;T&gt; / PageResult&lt;T&gt;</description>
```

或者干脆不在 description 里写泛型签名。

**预防**：B0.3 阶段批量生成子 pom 时，对 description 字段做转义，或者干脆只写中文描述不带类型签名。

---

## §10 erp-common 不应依赖 Spring Web（架构分层陷阱）【B0.3】

**症状**：把 `GlobalExceptionHandler` 放到 erp-common 时报：
```
package org.springframework.http does not exist
cannot find symbol: class RestControllerAdvice
cannot find symbol: class ResponseEntity
```

**根因**：架构边界违反。erp-common 是最底层依赖，被所有 13 个模块引用；它不应该引入任何 Web/Servlet 依赖，否则会污染所有业务模块（连 erp-security 都得拉 spring-web）。

**正解**：分层放置：
- `erp-common.exception.BusinessException` ← 纯业务异常类型，框架无关
- `erp-web.exception.GlobalExceptionHandler` ← Web 层异常翻译，依赖 `spring-boot-starter-web`

业务模块抛 `BusinessException`，由 web 层 `@RestControllerAdvice` 统一翻译成 `R.fail(code, msg)`。

**经验**：base 层只能依赖 starter（不带 -web），任何 `@RestController*` `ResponseEntity` `HttpStatus` 都属于 web 层。

---

## §11 父 pom 引了 spring-boot-starter-parent 时，子模块 contextLoads 测试默认期望 DataSource【B0.3】

**症状**：B0.3 加了 mybatis-plus + flyway + mysql-connector 的 dependencyManagement 后，erp-web 的 contextLoads 测试报：
```
Failed to determine a suitable driver class
DataSourceBeanCreationException
```

**根因**：B0.3 阶段还没装 MySQL（B1.1 才装），但 spring-boot-autoconfigure 一旦在 classpath 看到 HikariCP + mybatis-plus，就会尝试初始化 DataSource。

**临时解决**（B0.3 阶段）：测试类排除自动配置：
```java
@SpringBootTest
@EnableAutoConfiguration(exclude = {
    DataSourceAutoConfiguration.class,
    FlywayAutoConfiguration.class,
    HibernateJpaAutoConfiguration.class
})
class ErpApplicationTests {}
```

**B1.1 阶段**：装好 MySQL/Redis 后改回标准 `@SpringBootTest`，让 Flyway 真正运行迁移。

**经验**：跨阶段的临时绕过要在代码注释 + PITFALLS 留痕，避免 B1.1 时忘记还原。

---

## §12 erp-security 只引 starter-security 不引 starter-web → 编译炸【B1.4】

**症状**：B1.4 让 Claude 写完 14 个 Java 文件，跑 `mvn -pl erp-security,erp-web -am compile`：

```
JwtAuthenticationFilter.java:[8,23] package jakarta.servlet does not exist
JwtAuthenticationFilter.java:[39,8] cannot access jakarta.servlet.Filter
JwtAuthenticationFilter.java:[53,46] cannot find symbol (HttpServletRequest)
SecurityConfig.java:[63,34] JwtAuthenticationFilter cannot be converted to jakarta.servlet.Filter
... 级联 30+ 个 cannot find symbol
```

**根因**：`erp-security/pom.xml` 只引了：
- `spring-boot-starter-security`（带 spring-security-core/web/config）
- `spring-boot-starter-data-redis`

但 `OncePerRequestFilter` / `HttpServletRequest` / `Filter` 这些 servlet API 在
`spring-web` 模块里，而 `spring-web` 是由 `spring-boot-starter-web`（带 tomcat-embed）
传递进来的，**`starter-security` 不包含**。

**典型迷惑点**：`spring-security-web` 里有 `OncePerRequestFilter`，确实就在 starter-security
传递依赖里能找到，但它 `extends GenericFilterBean` 需要 `jakarta.servlet.Filter` 接口——
这个接口在 `tomcat-embed-core`（由 starter-web 引入）里。所以 IDE 跳转能看到符号，但 javac 编译失败。

**解决**：erp-security/pom.xml 加 starter-web：

```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-web</artifactId>
</dependency>
```

加完后 `mvn compile` 一次通过，`mvn test` 全模块 56 个测试 0 失败。

**经验**：
1. 任何模块写 `JwtAuthenticationFilter` / `OncePerRequestFilter` / Servlet API，都必须显式
   引 `starter-web`，**不能依赖 starter-security 传递**
2. Claude Code 写完不跑测试时（`-p` 模式 + acceptEdits 拦了 mvn），人工必须立刻
   `mvn compile` 兜底；它在 TASK_REPORT 里说"预期通过"≠ 实际通过
3. dependencyManagement 在父 pom 锁了版本，子模块加依赖**不需要写 version**，但**必须显式加**

## §13 — Flyway 不自带 DataSource starter，必须显式引 spring-boot-starter-jdbc

**症状**：erp-web/pom.xml 已引 `flyway-core` + `flyway-mysql` + `mysql-connector-j`，Spring Boot 启动时 Flyway 自动配置不生效，`@EnableAutoConfiguration` 报告 `FlywayAutoConfiguration` 因 `@ConditionalOnBean(DataSource.class)` 而跳过 —— 但项目根本没引 DataSource starter。

**根因**：
- `flyway-core` 只是迁移引擎，不是 Spring Boot starter
- `mysql-connector-j` 只是 JDBC 驱动，不创建 DataSource bean
- `spring-boot-autoconfigure` 里的 `DataSourceAutoConfiguration` 需要 `spring-boot-starter-jdbc` 把 HikariCP 和 `JdbcTemplate` 拉进来才会激活

**修复**：erp-web/pom.xml 显式加：
```xml
<dependency>
  <groupId>org.springframework.boot</groupId>
  <artifactId>spring-boot-starter-jdbc</artifactId>
</dependency>
```

**验证方式**：启动日志里看到这两段同时出现才算齐了：
```
HikariPool-1 - Start completed.
Flyway Community Edition X.Y.Z by Redgate
Migrating schema `xxx` to version "1 - ..."
```

**对比**：如果项目用 `spring-boot-starter-data-jpa`，它已经传递依赖 `spring-boot-starter-jdbc`，所以 JPA 项目里不会踩这个坑；纯 MyBatis-Plus 或纯 Flyway 项目必须显式声明。

**入坑日期**：2026-06-24（B1.2 阶段，CentOS 7 + MySQL 8.0.46 + Spring Boot 3.3.5）

---

## §14 — CentOS 7 装 MySQL 8 / Redis 7 的国内镜像可行通路

**症状**：tuna / aliyun / huawei / ustc 的 mysql/yum 路径全部 404；epel 默认 redis 是 3.2.12（过老）。

**可行通路**（2026-06 实测）：
1. **MySQL 8**：直接走官方 `https://repo.mysql.com/mysql80-community-release-el7-11.noarch.rpm` 装 repo rpm，再 `yum install mysql-community-server`。官方源在国内速度可接受。
2. **Redis 7**：用 SJTU 镜像的 remi 源：
   ```
   baseurl=https://mirror.sjtu.edu.cn/remi/enterprise/7/remi/$basearch/
   ```
   然后 `yum --enablerepo=remi,epel,base,updates,extras install -y redis-7.2.5`（必须把所有依赖源同时打开，remi 的 redis 依赖 base/epel 里的库）。

**踩雷**：
- `remi-release.rpm` 装完默认 `[remi]` 仓库 baseurl 指向法国主仓库，国内极慢，手写 `/etc/yum.repos.d/remi.repo` 用镜像覆盖
- `yum-config-manager` 在最小化 CentOS 7 上不存在，要装 `yum-utils`（或者直接 sed/手写 repo 文件）

**入坑日期**：2026-06-24（B1.2 阶段）

## §15 — CentOS 7 minimal 缺 fontconfig，java.awt.Font 抛 NPE 导致验证码生成失败

**症状**：CaptchaService 调 `Graphics2D.drawString()` 渲染 PNG，单测 `CaptchaServiceTest.generate_*` 抛 `java.lang.InternalError: java.lang.reflect.InvocationTargetException` → root cause：
```
Caused by: java.lang.NullPointerException: Cannot load from short array because "sun.awt.FontConfiguration.head" is null
  at java.desktop/sun.awt.FontConfiguration.getVersion(...)
  at java.desktop/sun.awt.X11FontManager.createFontConfiguration(...)
```

**根因**：JDK17 的 `sun.awt.X11FontManager` 启动时调用 `fc-list`/`fc-match` 读 fontconfig 配置；CentOS 7 minimal 镜像没装 `fontconfig` 和任何 ttf 字体，`FontConfiguration.head` 是 null。

**修复**（在测试/生产服务器都得跑）：
```bash
yum install -y fontconfig dejavu-sans-fonts dejavu-serif-fonts
fc-list | head   # 验证：≥ 1 行
```

**避坑替代方案**（如果不能装系统包）：
1. 让 JDK 跑 headless：`-Djava.awt.headless=true`（启动参数）—— 但 X11FontManager 仍需要 fontconfig
2. 改用第三方验证码库（Hutool/Kaptcha），它们用自己打包的 TTF 不依赖系统字体 —— 但拖入依赖
3. 用 PIL/wkhtmltopdf 等外部工具生成 —— 不推荐

**入坑日期**：2026-06-24（B1.4 Phase 2，CentOS 7 + JDK 17 + 纯 java.awt Captcha）

---

## §16 — Claude Code sandbox 拒 mvn/curl 时如何让它把活干完

**症状**：派 Claude Code 跑后台开发任务，它写完代码后想自己跑 `mvn test` 验收，但 `--permission-mode acceptEdits` 不自动允许 shell 命令，最终 TASK_REPORT 里写"未跑成 mvn / curl"，把验收锅甩回给我。

**修复**（下次派 Claude 前预热 `.claude/settings.json`）：
```json
{
  "permissions": {
    "allow": [
      "Bash(mvn:*)",
      "Bash(curl:*)",
      "Bash(ssh:*)",
      "Bash(git status:*)",
      "Bash(git diff:*)"
    ]
  }
}
```
放在 `/code/demo2/.claude/settings.json`（项目级），下次 `claude -p ...` 会自动允许这些命令。**禁用清单**保持空，让 Claude 不能写 git commit/push 之类的不可逆动作（我自己来做）。

**入坑日期**：2026-06-24（B1.4 Phase 2）

---

## 28. `@ConditionalOnBean(DataSource.class)` 时序陷阱 ❌

### 现象
- `mvn test` 71/71 全绿 ✅
- fat jar 生产环境启动正常，但登录走的是 `InMemoryUserDetailsLoader`（硬编码 admin/admin123）
- erp-user 模块 9 个 Bean（ServiceImpl×4 + Controller×4 + MysqlUserDetailsLoader）全部 **静默跳过装配**
- 应用日志看不到 `MysqlUserDetailsLoader instantiated`

### 根因
```
Spring Boot 启动顺序：
1. @Component 扫描阶段
   → 评估 @ConditionalOnBean(DataSource.class)
   → DataSource 还没装配 → 条件 false → erp-user Bean 全部跳过 ❌
2. AutoConfiguration 阶段
   → DataSource Bean 才注册（HikariDataSource）
   → 但此时 erp-user 的扫描已经过去了，没回头检查
```

`@ConditionalOnBean` 设计上是给 **AutoConfiguration** 用的（auto-config 之间的依赖判定），不是给 `@Component` 用的。`@Component` 扫描发生在 auto-config 之前。

测试环境为什么"假绿"？因为测试用 `@EnableAutoConfiguration(exclude = DataSourceAutoConfiguration.class)`，DataSource 主动被排除 → 条件评估对测试和生产都是 false → InMemory（`@ConditionalOnProperty(havingValue="memory")`）兜底 → 测试通过。生产真有 DataSource，但扫描时机太早评估不到，照样退化。

### 治法
改用 **`@ConditionalOnProperty`**，在 environment 阶段评估（最早），不依赖 Bean 注册时机：

```java
// erp-user 所有 Bean
@org.springframework.boot.autoconfigure.condition.ConditionalOnProperty(
    name = "erp.user.persistence",
    havingValue = "mysql",
    matchIfMissing = true
)

// erp-web InMemoryUserDetailsLoader
@org.springframework.boot.autoconfigure.condition.ConditionalOnProperty(
    name = "erp.user.persistence",
    havingValue = "memory"
)
```

```yaml
# application.yaml（生产默认）
erp:
  user:
    persistence: mysql  # mysql | memory
```

```java
// 5 个测试类
@org.springframework.test.context.TestPropertySource(properties = "erp.user.persistence=memory")
```

### 验证
- fat jar 启动日志出现 `MysqlUserDetailsLoader instantiated` ✅
- 不再出现 `InMemoryUserDetailsLoader 已装载 N 个开发账号` ✅
- 测试 71/71 仍全绿 ✅

### 备选方案（更重）
- 独立 starter 模块：把 erp-user 拆成 `erp-user-spring-boot-starter`，用 `META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports` 注册 → 自然延迟到 auto-config 阶段，此时 `@ConditionalOnBean(DataSource.class)` 才会正确评估
- `BeanFactoryPostProcessor`：手动控制 Bean 注册时机

我们选 `@ConditionalOnProperty` —— 一行注解 + 一行配置，最小侵入。

### 一句话教训
> **`@ConditionalOnBean` 不能用在 `@Component` 上判定 AutoConfiguration 注册的 Bean。**
> 想做"测试/生产切换"用 `@ConditionalOnProperty`，想做"Bean 间依赖判定"用 AutoConfiguration。

**入坑日期**：2026-06-24（B1.5 后端集成）  
**修复 commit**：`5d5079b`

---

## 29. BaseEntity ↔ DB schema 列名漂移

### 现象
`@ConditionalOnProperty` 修好装配时序后，登录返回 500：
```
java.sql.SQLSyntaxErrorException: Unknown column 'updated_by' in 'field list'
```

### 根因
- `BaseEntity` 抽象类带 `createdBy / updatedBy / version` 三个审计字段（INSERT_UPDATE 自动填充）
- V1 init schema 只在 `sys_user` 上加了 `created_by`，其他 sys_* 表（department/role/permission）三个字段全缺
- B1.5 后端代码本地编译 / 测试都因 `excludeAutoConfiguration=DataSourceAutoConfiguration` 不真连 DB，掩盖了缺列
- 装配修复后第一次真打 DB，立刻暴露

### 治法
V3 migration 补列：

```sql
ALTER TABLE sys_user ADD COLUMN updated_by BIGINT, ADD COLUMN version INT DEFAULT 0;
ALTER TABLE sys_department ADD COLUMN created_by BIGINT, ADD COLUMN updated_by BIGINT, ADD COLUMN version INT DEFAULT 0;
ALTER TABLE sys_role ADD COLUMN created_by BIGINT, ADD COLUMN updated_by BIGINT, ADD COLUMN version INT DEFAULT 0;
ALTER TABLE sys_permission ADD COLUMN created_by BIGINT, ADD COLUMN updated_by BIGINT, ADD COLUMN version INT DEFAULT 0;
```

### 验证
- Flyway 自动 v2 → v3 升级 ✅
- `flyway_schema_history` 多一行 v3 success=1 ✅
- 登录返回 200 + JWT + `/me` 返回 24 条 DB 权限（含 B1.5 V2 button 权限）✅

### 一句话教训
> **凡是继承 BaseEntity 的表，schema 必须包含 BaseEntity 的全部字段。**
> 后续可在 CI 加 schema 校验脚本，对每张表跑 `DESC` 对照 BaseEntity 字段列表。

**入坑日期**：2026-06-24（B1.5 收尾）  
**修复 commit**：V3 migration + 后续 commit


---

## 30. JWT authorities 只带 roles，不带 permissions

### 现象
B1.6 前端接真实 `/api/v1/system/*` 后：
- `POST /auth/login` 正常返回 JWT
- `/auth/me` 能看到 24 条 DB 权限
- 但 `GET /system/users` 返回 500，日志本质是：

```text
org.springframework.security.authorization.AuthorizationDeniedException: Access Denied
```

### 根因
`JwtTokenProvider` 只把 `roles` 写进 access token；`JwtAuthenticationFilter` 也只把 `roles` 转成 `SimpleGrantedAuthority`。

但系统接口用的是按钮/操作权限：

```java
@PreAuthorize("hasAuthority('user:view')")
@PreAuthorize("hasAuthority('role:assign-perm')")
```

所以用户即使在 `/me` 里有 permissions，进入 Spring Security MethodSecurity 时也没有对应 authority。

### 治法
- access token 增加 `permissions` claim
- `JwtAuthenticationFilter` 组装 authorities 时使用 `roles + permissions`，并 `distinct()` 去重
- refresh token 换 access 时重新从 `UserDetailsLoader` 加载用户权限

### 同期修复
- Controller 权限码对齐 V2 seed：
  - `user:assign` → `user:assign-role`
  - `role:assign` → `role:assign-perm`
- `RoleVO.permissionIds` 从 `sys_role_permission` 回填，避免角色权限保存时传 `null`

### 验证
- `/system/users?page=1&size=10` → 200 ✅
- `/system/roles` → 200 ✅
- `/system/permissions/tree` → 200 ✅
- `/system/departments` → 200 ✅
- `/system/roles/1/permissions` → 200 + `code=0 ok` ✅
- `mvn test` → 通过 ✅
- `pnpm build` → 通过 ✅

### 一句话教训
> 前后端权限模型要统一：如果接口用 `hasAuthority('xxx:yyy')`，JWT/SecurityContext 里就必须放权限码，不只是角色码。

**入坑日期**：2026-06-25（B1.6 系统管理联调）

---

## §37. 批量导入 N+1 事务：循环调用 `@Transactional` 方法导致千次数据库往返【优化阶段 4】

### 现象
`UserServiceImpl.batchCreateUsers()` 循环调用 `createUser()`（每个都有 `@Transactional`），Excel 导入 1000 条用户 = 1000 次独立事务 + 1000 次密码加密 + 2000 次 INSERT（用户+角色）。

### 根因
Spring 的 `@Transactional` 基于代理：同类内部方法调用不走代理，事务不生效；但 `batchCreateUsers` 调用 `createUser` 是**不同 public 方法**，代理会拦截，每个调用都开启/提交独立事务。

### 解法
1. **提取无事务内部方法** `doCreateUser()`，包含实际 INSERT 逻辑
2. **批量方法加 `@Transactional`**，循环调用 `doCreateUser()`，整个 batch 在一个事务中
3. **预加载已有用户名到内存 Set**，避免循环内重复查询数据库

```java
@Transactional(rollbackFor = Exception.class)
public BatchImportResult batchCreateUsers(List<UserCreateRequest> list) {
    Set<String> existing = userMapper.selectList(null).stream()
        .map(SysUser::getUsername).collect(Collectors.toSet());
    for (...) {
        if (existing.contains(req.getUsername())) { ... continue; }
        doCreateUser(req); // 无事务，复用同一连接
        existing.add(req.getUsername());
    }
}
```

### 验证
- 批量导入 100 条用户：事务数从 100 → 1 ✅
- `importUsersFromExcel` 同样预加载用户名，减少 N+1 查询 ✅

**入坑日期**：2026-06-26（优化阶段 4）

---

## §38. 分页接口缺少 `pageSize` 上限：客户端传 `size=100000` 可拖垮数据库【优化阶段 4】

### 现象
`DatUploadServiceImpl.listPage()` 直接使用 `new Page<>(q.getPageNum(), q.getPageSize())`，未限制最大值。恶意或误操作请求 `pageSize=100000` 会一次性加载 10 万条记录。

### 根因
MyBatis Plus 的 `Page` 对象不会自动限制 size，完全信任客户端输入。

### 解法
统一加 `Math.min(size, 100)` 上限：
```java
Page<DatUpload> p = mapper.selectPage(
    new Page<>(q.getPageNum(), Math.min(q.getPageSize(), 100)), w);
```

**涉及文件**（3 处未限制，已修复）：
- `DatUploadServiceImpl.listPage()`
- `PricingServiceImpl.listPage()`
- `UserServiceImpl.pageUsers()`

其余 15 个分页接口已有限制。

**入坑日期**：2026-06-26（优化阶段 4）

---

## §39. Swagger `@Tag` 注解导致编译失败：注解依赖未下沉到业务模块【优化阶段 4】

### 现象
给 26 个 Controller 添加 `@Tag(name = "...")` 后编译报错：
```
package io.swagger.v3.oas.annotations.tags does not exist
```

### 根因
`knife4j-openapi3-jakarta-spring-boot-starter` 只在 `erp-web/pom.xml` 中引入，业务模块（erp-user/erp-data 等）没有 swagger-annotations 依赖。

### 解法
在 `erp-common/pom.xml` 引入轻量级注解库（所有业务模块都依赖 erp-common）：
```xml
<dependency>
    <groupId>io.swagger.core.v3</groupId>
    <artifactId>swagger-annotations-jakarta</artifactId>
</dependency>
```
父 pom `dependencyManagement` 中统一版本：`2.2.22`。

### 一句话教训
> 跨模块使用的注解库，必须放在**最底层公共模块**（erp-common）或父 pom 的 `<dependencies>` 中，不能只在启动模块引入。

**入坑日期**：2026-06-26（优化阶段 4）
