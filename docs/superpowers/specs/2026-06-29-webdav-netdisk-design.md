# WebDAV 网络磁盘 设计规格

> **目标**：在已完成的 `DatFile` 网盘基础上增加 WebDAV 协议层，使用户能在 Windows 资源管理器里映射网络驱动器，双击打开文件、编辑后 Ctrl+S 自动保存到服务器，全程受 ERP 部门权限管控。
>
> **创建日期**：2026-06-29
> **状态**：设计已确认，待实现
> **关联**：`GUIDE_ENTERPRISE_NETDISK.md` Phase 2（指南中 Phase 0 网页文件管理器与目录树已完成）

---

## 1. 背景与范围

### 1.1 现状（已完成，本次复用）

- `erp-data` 模块已有完整网盘后端：
  - `DatFile` 实体（自引用树结构：`parentId`/`isDirectory`/`extension`/`mimeType`/`storagePath`/`deptId`）
  - `DatFileController` REST API（list/breadcrumb/folder/upload/rename/move/delete/download）
  - `DatFileServiceImpl` 部门隔离权限（`canAccess`/`canWrite`）
  - `dat_file` / `dat_file_share` 表，Flyway V40/V41 已建表迁移
- `erp-frontend` 已有网页文件管理器 `FileManage.vue`（左侧部门树 + 右侧列表 + 面包屑 + 上传/重命名/删除/下载）

### 1.2 本次范围

**仅实现 WebDAV 协议层**（指南 Phase 2）。不含：在线预览（Phase 3）、OnlyOffice（Phase 4）。网页文件管理器（Phase 1）已存在，本次仅同步调整其受影响的权限逻辑。

### 1.3 核心诉求

1. Windows 资源管理器映射网络驱动器后，直接操作文件
2. 双击用 Office/WPS/记事本打开，Ctrl+S 自动保存到服务器
3. 全程受 ERP 部门权限管控

### 1.4 已确认的关键决策

| 决策项 | 选择 |
|---|---|
| 传输协议 | HTTP（局域网） |
| 编辑场景 | Office/WPS + 记事本/图片都要 → 完整实现 LOCK/UNLOCK |
| 根目录结构 | 部门为顶层（与网页管理器一致） |
| 服务端实现 | 手写 `@RestController` 适配层（不用 sardine/milton） |
| 共享模型 | 文件级 + 文件夹继承 |
| 递归可见性查询 | 物化路径（`dat_file.path` 字段），与 `sys_department.dept_path` 模式一致 |
| LOCK 存储 | 内存（单机），多机后续换 Redis |

---

## 2. 整体架构

```
┌─────────────────────────────────────────────────┐
│  Windows 资源管理器 (映射网络驱动器 \\host@80\webdav) │
│  Basic Auth: ERP 账号密码                        │
└────────────────────────┬────────────────────────┘
                         │  HTTP WebDAV 动词
                         │  OPTIONS/PROPFIND/GET/PUT
                         │  MKCOL/DELETE/MOVE/LOCK/UNLOCK
┌────────────────────────▼────────────────────────┐
│  WebDavAuthFilter  (新)                          │
│  Basic Auth → 校验账号密码 → 生成临时 JWT        │
│  → 复用现有 JwtAuthenticationFilter 链路         │
└────────────────────────┬────────────────────────┘
                         │
┌────────────────────────▼────────────────────────┐
│  WebDavController  (新, 薄协议适配层)            │
│  路径 ↔ DatFile 映射  (/销售部/报表/x.xlsx)      │
│  PROPFIND XML 生成 / LOCK 状态管理              │
└────────────────────────┬────────────────────────┘
                         │  复用，不重写
┌────────────────────────▼────────────────────────┐
│  DatFileService  (现有, 小改权限 + 维护 path)    │
│  部门隔离 / 共享继承 / canAccess / canWrite      │
└────────────────────────┬────────────────────────┘
                         │
┌────────────────────────▼────────────────────────┐
│  DatFile 表(+path) + dat_file_share + 物理文件   │
└─────────────────────────────────────────────────┘
```

WebDAV 层是薄适配层，**不碰业务逻辑**。只做三件事：①把 WebDAV 路径翻译成 `DatFile` 的 `deptId + parentId` 链；②生成 PROPFIND XML 响应；③管理 LOCK 锁状态。所有权限、存储、树操作委托给 `DatFileService`。

---

## 3. 权限规则（最终版）

这是所有设计的地基，权限校验集中在 `DatFileServiceImpl`，WebDAV 与网页管理器共用同一套。

### 3.1 读取 canAccess（能看见）

- 管理员 → 全部
- 自己创建的
- 文件所属部门 ∈ 本部门 + 下级部门（管理者能看下属部门）
- 文件本身被共享给本部门
- **任一祖先文件夹被共享给本部门**（继承，新增，修复嵌套文件触达）
- 被他人锁住的文件：对非持锁者**仍可见文件名**，但 GET 返回 423 Locked

### 3.2 编辑已存在文件 canWrite（Ctrl+S 保存、改内容）

- 管理员
- 自己创建的
- 文件所属部门 == 本部门
- 文件本身或任一祖先文件夹被共享给本部门（共享即可改）
- `deptId` 保持不变（归属权不随编辑者转移）

### 3.3 新建/上传 canCreate（MKCOL 建文件夹、PUT 新文件、拖拽上传）

- **仅本部门目录**（「只能上传到本部门」）
- 新文件 `deptId = 本部门`（只能在本部门目录建，故「归属所在目录部门」= 本部门，自洽）
- 下级部门、共享部门目录 → 只读，不能新建

### 3.4 下级部门

仅可读（管理者监督下属），不可写。除非显式共享。

### 3.5 LOCK（独占锁）

- 锁前提：必须 `canWrite`（无写权限不能锁）
- 锁住后：非持锁者 GET/PUT/DELETE → 423 Locked（列目录仍显示文件名）
- 持锁者：可读可写
- UNLOCK 或超时释放

### 3.6 MOVE

- 仅本部门目录内移动/重命名，**跨部门移动被拒**
- 源 `canWrite` + 目标父 `canCreate`（目标必须同属本部门）

### 3.7 需改动的现有权限代码

- `canAccess`：增加「祖先文件夹共享」判定（沿 `path` 拆祖先 id 批量查 `dat_file_share`）+ 被锁文件可见但拒读
- `canWrite`：放宽到「本部门 OR 文件/祖先共享给本部门」
- 新增 `canCreate(folderOrDept, user)`：仅 `目标dept == 本部门`

---

## 4. 路径映射模型

### 4.1 路径 ↔ DatFile 映射

现有表结构特点：部门根不是一条记录，而是 `parentId=null AND deptId=X` 的一组散文件。故 WebDAV 根目录 `/` 要**虚拟**出部门列表。

| WebDAV 路径 | 映射结果 |
|---|---|
| `/` (根) | 虚拟目录，列出用户可见部门 |
| `/销售部` | 虚拟部门入口 → `parentId=null, deptId=销售部ID` 的根文件 |
| `/销售部/报表` | 真实文件夹 → `DatFile`（沿 parentId 链定位） |
| `/销售部/报表/数据.xlsx` | 真实文件 → `DatFile` |

### 4.2 解析算法（WebDavPathResolver）

1. URL 解码路径，按 `/` 分段 → `[销售部, 报表, 数据.xlsx]`
2. 第一段是部门名 → 查 `sys_department` 得 `deptId`（找不到或无权 → 404/403）
3. 第二段起沿 `parentId` 链逐段匹配 `name`：
   - 段 1 后无更多段 → 返回「部门根」虚拟节点
   - 有更多段 → `SELECT * FROM dat_file WHERE parent_id=上节点id AND name=本段 AND deleted=0` 逐级下钻
4. 任一级匹配不到 → 404

### 4.3 两个虚拟层

- **根 `/`**：PROPFIND 返回用户可见部门列表（部门名当文件夹名），不对应任何 `DatFile` 记录
- **部门入口 `/销售部`**：PROPFIND 返回 `parentId=null, deptId=销售部` 的文件列表；MKCOL 在此创建 `parentId=null, deptId=销售部` 的记录

### 4.4 列目录可见性（共享继承）

取该目录直接子项，逐个 `canAccess` 过滤。关键补充：**文件夹本身没共享、但其后代有文件被共享给本部门 → 该文件夹仍要显示**（否则用户点不进、到不了被共享的深层文件）。由 `selectSharedDescendantExists(pathPrefix, deptId)` 一条 LIKE 查询实现。

### 4.5 「可见部门」根目录列表

- 本部门 + 下级部门
- **有任意文件（含深层）被共享给本部门**的部门 → 去重

### 4.6 URL 编码

中文路径 UTF-8 百分号编码。`WebDavPathResolver` 用 `URLDecoder.decode(path, UTF-8)` 还原。文件名里的 `/` 在存库时已被 `safeName` 替换为 `_`，不破坏分段。

---

## 5. 组件清单

### 5.1 新增组件（`com.erp.data.webdav` 包）

**1. `WebDavAuthFilter`** — Basic Auth → JWT
- 职责：拦截 `/webdav/**`，取 `Authorization: Basic xxx`，Base64 解码得账号密码，调 `AuthService` 校验，成功则用 `JwtTokenProvider` 生成临时 access token 塞进 `Authorization: Bearer`，交给后续 `JwtAuthenticationFilter`
- 注册位置：Spring Filter，排在 `JwtAuthenticationFilter` 之前
- 依赖：`AuthService`、`JwtTokenProvider`（现有）
- 临时 JWT 不入 Redis 黑名单（单次请求生命周期，用完即弃）

**2. `WebDavController`** — 协议处理器
- 职责：`@RestController` + `@RequestMapping("/webdav/**")`，处理 OPTIONS/PROPFIND/GET/PUT/MKCOL/DELETE/MOVE/LOCK/UNLOCK
- 流程：每个方法 → `WebDavPathResolver.resolve` → 调 `DatFileService`（权限在 Service 内）→ 返回 WebDAV 响应
- 依赖：`WebDavPathResolver`、`DatFileService`、`WebDavPropFindXmlBuilder`、`WebDavLockStore`

**3. `WebDavPathResolver`** — 路径 ↔ DatFile 映射
- 职责：`/销售部/报表/x.xlsx` → `{deptId, parentId链, 目标DatFile或虚拟节点}`
- 接口：`resolve(String path, LoginUser user)` 返回 `ResolvedPath`（类型：ROOT/DEPT_ROOT/FOLDER/FILE/NOT_FOUND）
- 依赖：`DatFileMapper`、`sys_department` 查询

**4. `WebDavPropFindXmlBuilder`** — XML 响应生成
- 职责：把 DatFile 列表渲染成 WebDAV PROPFIND 的 multistatus XML（`<D:response>` 含 href/displayname/getcontentlength/getcontenttype/iscollection 等）
- 接口：纯函数 `build(ResolvedPath, List<DatFile>)` → XML 字符串
- 依赖：无

**5. `WebDavLockStore`** — 锁状态
- 职责：内存 `ConcurrentHashMap<fileId, LockInfo>`，LOCK 写入、UNLOCK 删除、PUT/DELETE 前校验锁 token、超时自动清理
- 接口：`tryLock(fileId, user, timeout)` / `unlock(fileId, token)` / `assertLockHeld(fileId, token)` / `isLockedByOther(fileId, user)`
- 依赖：无（单机内存；多机后续换 Redis）

**6. `WebDavSecurityConfig`** — 安全配置
- 职责：`/webdav/**` 放行进 Basic Auth 流程（不走 JWT 白名单），其余接口不变
- 依赖：`SecurityConfig` 现有配置

### 5.2 改动组件

**7. `DatFile` 实体** — 加 `path` 字段
- 新增 `private String path;`（物化路径，如 `/3/15/42/`）

**8. `DatFileServiceImpl`** — 权限放宽 + 路径维护
- `canAccess`：增加祖先共享判定 + 被锁文件可见但拒读
- `canWrite`：放宽到「本部门 OR 文件/祖先共享给本部门」
- 新增 `canCreate(folderOrDept, user)`：仅 `目标dept == 本部门`
- 新增 `writeContent(fileId, inputStream, user)`：供 WebDAV PUT 覆盖保存复用，不动 `deptId`
- `createFolder`/`uploadFile`/`move`：维护 `path`（新建=父path+自己id；移动=更新子树 path 前缀，一条 UPDATE LIKE）

**9. `DatFileMapper`** — 新增 SQL
- `selectSharedDescendantExists(pathPrefix, deptId)`：LIKE 查共享后代存在性
- `selectAncestorShares(ancestorIds, deptId)`：祖先共享判定

### 5.3 迁移脚本

**V42__add_dat_file_path.sql**：加 `path` 列 + 回填存量数据（递归 CTE 一次性算出每条记录的 path）。MySQL 8.4 支持。

---

## 6. 数据流（各 WebDAV 动词端到端）

### 6.1 OPTIONS（连接前探测）
返回固定头：`DAV: 1,2`、`Allow: OPTIONS,PROPFIND,GET,PUT,MKCOL,DELETE,MOVE,LOCK,UNLOCK`。让 Windows 客户端确认这是 WebDAV 服务。

### 6.2 PROPFIND（列目录/取属性，最频繁）
1. `WebDavAuthFilter` 鉴权 → `LoginUser`
2. `WebDavPathResolver.resolve` → `ResolvedPath`
3. 按类型取子项：
   - `ROOT` → 用户可见部门列表
   - `DEPT_ROOT` → `selectRootFilesByDeptId`，逐个 `canAccess` 过滤，子文件夹含共享后代则保留
   - `FOLDER` → `selectByParentId`，同样过滤 + 后代存在性保留
   - `FILE` → 返回该文件单个属性
4. `WebDavPropFindXmlBuilder` 生成 multistatus XML

### 6.3 GET（下载/打开）
1. 鉴权 → resolve 到 `FILE`
2. `canAccess` 校验（被他人锁 → 423）→ 不通过 403
3. 流式返回物理文件，`Content-Type` 取 `mimeType`

### 6.4 PUT（上传新文件 / Ctrl+S 保存）
1. 鉴权 → resolve
2. 分两种：
   - **新文件**（path 不存在）：`canCreate(所在目录dept)` 校验（仅本部门）→ 落临时文件 → `uploadFile`（`deptId=本部门`）→ 维护 `path`
   - **覆盖保存**（已存在）：`canWrite` + `assertLockHeld` → 覆盖物理文件 + 更新 `fileSize`/`updatedBy`/`updatedAt`，**不动 `deptId`** → 持锁则保持锁

### 6.5 MKCOL（新建文件夹）
1. 鉴权 → resolve 父路径
2. `canCreate(父目录dept)`（仅本部门）
3. `createFolder` → 维护 `path`

### 6.6 DELETE（删除）
1. 鉴权 → resolve
2. `canWrite` + `assertLockHeld`
3. 逻辑删除（`deleted=1`），文件物理保留

### 6.7 MOVE（重命名/移动）
1. 鉴权 → resolve 源 + 目标父
2. 源 `canWrite` + 目标父 `canCreate`（跨部门被拒）
3. `move` → 更新 `path`（子树前缀替换）

### 6.8 LOCK / UNLOCK
1. LOCK：resolve 到文件 → `canWrite`（无写权限拒锁）→ `tryLock` → 返回锁 token（XML）
2. UNLOCK：取 `Lock-Token` 头 → `unlock(fileId, token)` → 校验归属
3. PUT/DELETE 前查锁：被锁且无正确 token → 423

---

## 7. 错误处理

WebDAV 不能用项目现有 `R<T>` JSON 包装，必须返回 **HTTP 状态码 + XML body**。

### 7.1 状态码映射

| 场景 | 状态码 |
|---|---|
| 鉴权失败 | 401 + `WWW-Authenticate: Basic realm="ERP WebDAV"` |
| 无权限访问/写入、跨部门移动 | 403 |
| 路径不存在 | 404 |
| 文件被他人锁定 / LOCK 冲突 | 423 Locked |
| MKCOL 同名 / MOVE 目标已存在 | 409 Conflict |
| 请求格式错误 | 400 |
| PROPFIND 成功 | 207 Multi-Status |
| GET/PUT/MKCOL/DELETE/MOVE/LOCK 成功 | 200 / 201(新建) / 204(无内容) |

### 7.2 异常策略

- Controller 内 catch `BusinessException`，翻译成对应状态码 + 简短 XML error body
- `WebDavErrors` 工具类集中维护「业务异常 code → 状态码」映射，复用 `BusinessException.CODE_FORBIDDEN/CODE_NOT_FOUND` 等
- XML 错误体统一格式：
  ```xml
  <?xml version="1.0" encoding="utf-8"?>
  <D:error xmlns:D="DAV:"><D:status>HTTP/1.1 403 Forbidden</D:status></D:error>
  ```

### 7.3 字符编码

所有响应 `Content-Type` 带 `charset=utf-8`，XML 声明 `encoding="utf-8"`。中文文件名走 XML body 传，不放 header（规避 Windows 对 header 中文处理的老毛病）。

### 7.4 大文件/中断

PUT 流式写临时文件，失败回滚（删临时文件，不留半截）。下载同理流式。

---

## 8. 测试策略

### 8.1 单元测试（纯逻辑）

- `WebDavPathResolverTest`：各 ResolvedPath 类型、中文解码、多层嵌套、非法字符
- `WebDavPropFindXmlBuilderTest`：multistatus 结构、href 编码、displayname 中文、isdirectory、字段完整
- `WebDavLockStoreTest`：加锁/解锁/超时清理/重复锁冲突/非持锁者拒锁
- `DatFileServiceImpl` 权限测试：
  - `canAccess`：本部门✓、下级✓、共享✓、祖先共享继承✓、被锁可见✓、无权✗
  - `canWrite`：本部门✓、共享可写✓、下级只读✗、无权✗
  - `canCreate`：本部门✓、其他部门✗
  - path 维护：新建/移动后 path 正确、子树前缀替换正确

### 8.2 WebDAV 协议集成测试（MockMvc）

发 PROPFIND/GET/PUT/MKCOL/DELETE/MOVE/LOCK/UNLOCK，验证：
- 各动词正确状态码（207/200/201/204/403/404/423/409）
- PROPFIND XML 可解析、含正确文件列表
- 鉴权：无 Basic Auth / 错误密码 → 401
- 端到端：建文件夹 → 上传 → PROPFIND 看到它 → GET 内容一致 → 锁定 → 他人 GET 423 → 持锁者 PUT 覆盖 → 解锁 → 他人可读

### 8.3 权限矩阵测试（数据驱动，@ParameterizedTest）

用户（数据部/被共享的销售部/管理员）× 操作（读/新建/编辑/删除/移动/锁）× 目标（本部门根/下级/共享目录），枚举断言预期状态码。

### 8.4 Windows 客户端实测（手动验收）

单测过不了 Windows 客户端怪癖，必须真机验证：
- 映射 `\\<host>@80\webdav`，输 ERP 账号密码
- 改注册表 `BasicAuthLevel=2`（HTTP 下允许 Basic Auth）+ `FileSizeLimitInBytes`（解除 50MB 限制），提供 `.reg` 脚本
- 验证：列目录、双击 Word 打开、Ctrl+S 保存、关闭、他人打开看到新内容、并发编辑锁定提示

### 8.5 不测

物理文件 IO 极端情况（磁盘满等）交运维；Office 客户端内部行为不可控。

---

## 9. 验收标准（done）

1. 单元测试 + 协议集成测试 + 权限矩阵测试全部通过
2. Windows 真机映射网络驱动器成功，列目录/打开/编辑保存/并发锁定提示均符合预期
3. 权限规则第 3 节全部生效（本部门可写、共享可改、跨部门拒建、下级只读、锁独占）
4. 网页文件管理器与 WebDAV 权限行为一致（同一套 `DatFileService`）

---

## 10. 实施顺序（供 writing-plans 细化）

1. V42 迁移：`dat_file.path` 列 + 回填
2. `DatFile` 实体加 `path` 字段
3. `DatFileMapper` 新增两个查询方法
4. `DatFileServiceImpl` 改权限（canAccess/canWrite）+ 新增 canCreate + path 维护 + writeContent
5. `WebDavAuthFilter` + `WebDavSecurityConfig`
6. `WebDavPathResolver`
7. `WebDavPropFindXmlBuilder`
8. `WebDavLockStore`
9. `WebDavController`（各动词）
10. `WebDavErrors` 错误映射
11. 测试：单元 + 集成 + 权限矩阵
12. Windows 真机验收 + `.reg` 脚本

---

## 11. 风险与备注

- **HTTP + Basic Auth**：Windows 默认对 HTTP 不发密码，需客户端改注册表 `BasicAuthLevel=2`。`.reg` 脚本随交付。生产建议后续切 HTTPS。
- **WebDAV 客户端差异**：仅针对 Windows 资源管理器 + Office/WPS 验证，其他客户端（Mac Finder 等）不在本次范围。
- **LOCK 内存存储**：单机部署够用；服务重启锁丢失（可接受，Office 会重新 LOCK）。多机部署需换 Redis。
- **物化路径维护**：移动文件夹时需更新整棵子树的 `path` 前缀（一条 UPDATE LIKE），注意事务。
- **指南勘误**：`GUIDE_ENTERPRISE_NETDISK.md` Phase 2 中「依赖 sardine 库」有误，sardine 是 WebDAV 客户端库，服务端不适用，本设计改为手写适配层。