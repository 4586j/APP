# 外贸 ERP 系统 — 开发路线图 / 进度看板

> **项目位置**：`server3:/code/demo2`
> **文档来源**：基于 `guide/DEV_STEPS.md` 8 阶段路线图 + 当前代码现状盘点
> **创建日期**：2026-06-24
> **最后更新**：2026-06-24

---

## 📋 使用说明

- ✅ = 已完成
- 🟡 = 进行中
- ⬜ = 待办
- ⚠️ = 阻塞/有依赖

完成步骤后将对应方框从 `⬜` 改为 `✅`，整个任务完成后将任务标题前的 `⬜` 改为 `✅`。

---

## 🎯 PART A：已存在的资产（盘点）

### ✅ A1. 项目骨架与脚手架（部分）
- [✅] Maven 项目结构（`pom.xml` + `mvnw` + `mvnw.cmd`）
- [✅] Spring Boot 启动类 `Demo2Application.java`（空壳）
- [⚠️] **pom 父版本为 Spring Boot 4.1.0 RC，需降到 3.3.5 稳定版**（DEV_GUIDE 明确要求）
- [✅] `.gitignore`（含 IDEA / Maven / Spring）
- [⚠️] `.gitignore` 把 `guide/` 整目录忽略 — 团队文档无法进版本控制（PITFALLS §3）

### ✅ A2. 前端 UI 页面壳（30 个 .vue 文件）
- [✅] Vue 3 + Vite 5 + TypeScript + Element Plus + Pinia + ECharts 已配置
- [✅] 布局组件：`Layout.vue` / `Navbar.vue` / `Sidebar.vue`
- [✅] 路由：`router/index.ts`
- [✅] 用户 store：`store/user.ts`
- [✅] 10 大业务模块页面壳（共 17 个 view）
  - login / dashboard / system(用户+角色) / product / customer(客户+供应商)
  - order(销售列表/创建+采购列表) / finance(总览+资金审批+汇率)
  - logistics / document / data(上传+定价分析) / approval / error/404

### ✅ A3. 完整数据库设计
- [✅] `guide/erp_schema.sql` 含 36 张表 DDL（sys_/prd_/crm_/ord_/fin_/log_/doc_/dat_/app_/ntf_ 共 10 前缀）
- [✅] 含索引、外键、注释、字符集 utf8mb4

### ✅ A4. 完整设计文档
- [✅] `DEV_GUIDE.md` 开发指南（13 章，~52KB）
- [✅] `DEV_STEPS.md` 8 阶段开发步骤（~37KB）
- [✅] `API_DESIGN.md` 26 模块 REST API 规范（~42KB）

---

## 🚀 PART B：开发任务（按 DEV_STEPS 拆解，~ 32 周）

---

### ✅ B0. 第 0 步 — 项目初始化与环境搭建 ✅ 2026-06-24

> **目标**：环境就绪、Maven 多模块编译通过、中间件运行

#### B0.1 开发环境
- [x] JDK 17 安装确认（OpenJDK 17 GA, 华为云源，/opt/jdk/current） ✅ 2026-06-24
- [⬜] Node.js 20 LTS + pnpm 安装
- [⬜] Docker（server3 上未装，需走 CentOS 7 兼容方案）
- [⬜] 中间件 docker-compose.yml（MySQL 8 + Redis 7 + MinIO）

#### B0.2 父 POM 重构
- [x] Spring Boot **4.1.0 RC → 3.3.5 LTS** ✅
- [x] `<groupId>` `com.example` → `com.erp` ✅
- [x] `<artifactId>` `demo2` → `erp-parent` ✅
- [x] `<packaging>pom` ✅（B0.3 拆分时改为 pom，父工程聚合 14 子模块）
- [x] 添加 `<dependencyManagement>` 统一管理（10 项关键版本锁定，B0.3 子模块按需引入）：
  spring-boot-starter-web、mybatis-plus-spring-boot3-starter、mysql-connector-j、
  flyway-core/mysql、spring-boot-starter-data-redis、spring-boot-starter-security、
  easyexcel、knife4j、hutool-all、minio、poi、lombok

#### ✅ B0.3 创建 14 个子模块目录
- [x] erp-common / erp-security / erp-user / erp-product / erp-customer ✅
- [x] erp-order / erp-finance / erp-approval / erp-logistics / erp-document ✅
- [x] erp-data / erp-notification / erp-report / erp-web ✅
  - 命名修正：erp-dashboard → erp-report（对齐 DEV_GUIDE §3.2）
- [x] erp-common 骨架：BaseEntity / BaseQuery / R<T> / PageResult / BusinessException / EnableStatus ✅
- [x] erp-web 骨架：ErpApplication 迁入 + GlobalExceptionHandler + spring-boot-maven-plugin ✅
- [x] 模块依赖关系：erp-common 无内部依赖；erp-security 依赖 erp-common；
  业务模块依赖 erp-common+erp-security；erp-web 聚合全部 13 个 ✅

#### B0.4 编译验收
- [x] `mvn clean compile` BUILD SUCCESS ✅（14/14 模块，22.5s）
- [x] `mvn test` BUILD SUCCESS ✅（contextLoads 通过，临时排除 DataSource/Flyway 自动配置）
- [x] `mvn package` 产出 erp-web/target/erp-web.jar 58MB ✅
- [⬜] Docker 三件套（MySQL/Redis/MinIO）ping 通 ⏸ 待 B1.1 阶段一起装

---

### 🟢 B1. Phase 1 — 基础设施（第 1-4 周）进行中 ~ 95%

> **目标**：用户能登录系统、看到侧边栏菜单
> **里程碑**：登录 → 拿 JWT → 调 `/auth/me` → 进入仪表盘

#### ✅ B1.1 erp-common ✅ 2026-06-24
- [x] `BaseEntity`（id/version/created_at/updated_at/created_by/updated_by/deleted） — commit b3d2a8a
- [x] `BaseItemEntity`（明细基类：id+createdAt+updatedAt，无逻辑删/乐观锁） — commit 2b13b2c
- [x] `R<T>` 统一响应、`PageResult<T>` 分页 — commit b3d2a8a
- [x] `BusinessException`（409 OptimisticLockConflict 预设） — commit b3d2a8a
- [x] `GlobalExceptionHandler`（400/403/409/500）→ 放 erp-web（PITFALLS §10） — commit b3d2a8a
- [x] 基础枚举：`EnableStatus` / `Department` / `Currency` / `OrderStatus`（带状态机校验） — commit 2b13b2c
- [x] 工具类：`DateUtils` / `StringUtils` / `IdGenerator` — commit 88d5914
- [x] 单元测试：45 个 @Test 通过（DateUtils 8 / IdGenerator 4 / StringUtils 13 / Currency 5 / Department 4 / OrderStatus 11）

#### ✅ B1.2 Flyway + 数据库 ✅ 2026-06-24
- [x] MySQL 8.0.46 yum 安装（repo.mysql.com 官方源，CentOS 7）+ 应用账号 `erp/erp_demo2`
- [x] Redis 7.2.5 安装（remi SJTU 镜像）+ ping=PONG
- [x] `V1__init_system_tables.sql`（部门/用户/角色/权限 6 张表 + admin/ROLE_ADMIN seed，143 行）
- [x] 放在 `erp-web/src/main/resources/db/migration/`
- [x] `erp-web/pom.xml` 加 `spring-boot-starter-jdbc`（Flyway 不传递 DataSource starter，PITFALLS §13）
- [x] `application.yaml` 接 DataSource + Hikari + Flyway + server.port=8081（避让 nginx）
- [x] **真实启动 + Flyway 迁移成功**：`Started ErpApplication in 2.61s`，flyway_schema_history v1=success
- [x] 端到端登录验证：`POST /api/v1/auth/login` 返 JWT，`/me` 携带 token 200，错密码 401
- [x] 还原 ErpApplicationTests（去 B0.3 临时 exclude），`mvn -B test` 全模块 56/56 通过

#### ✅ B1.3 前端初始化补齐 ✅ 2026-06-24
- [x] 包管理切到 pnpm 9（npmmirror 镜像，pnpm-lock.yaml 64KB） — commit cb37982
- [x] Vite 代理 `/api` → `localhost:8080`（已在 vite.config.ts）
- [x] 缺失目录补齐：`api/` `utils/` `types/` `locales/` `styles/` — commit cb37982
- [x] `api/request.ts` Axios 实例（token 注入 / R<T> 拆包 / 401 ElMessageBox 重登 / traceId）
- [x] `api/auth.ts` `api/system.ts` 接口定义（对齐后端 erp-security）
- [x] `types/api.ts` `types/auth.ts`（PageResult/PageQuery/BizCode/UserInfo/Department）
- [x] `utils/storage.ts`（localStorage 前缀化）+ `utils/format.ts`（日期/金额/字节/截断）
- [x] `store/user.ts` 真接 JWT（login/logout/fetchProfile/hasPermission，ADMIN 直通）
- [x] SCSS 全局变量（品牌深蓝 #1d4ed8）+ Element Plus CSS var 覆盖
- [x] vue-i18n 9（zh-CN / en-US，浏览器语言探测 + localStorage 持久化）
- [x] vite dev 启动验证 OK（http://localhost:3000）

#### ✅ B1.4 erp-security + 认证（第 2-3 周）Phase 1+2 完成 ✅ 2026-06-24

**Phase 1 — 最小可用认证 ✅ 2026-06-24（commit 56d4f86，Claude 协作 + 我修依赖）**
- [x] `JwtTokenProvider` 生成 Access(30min) + Refresh(30day) Token（jjwt 0.12.x 新 API）
- [x] `JwtAuthenticationFilter` 每请求拦截（OncePerRequestFilter）
- [x] Redis Token 黑名单（`TokenBlacklist`，对 Redis 异常 fail-open）
- [x] `@CurrentUser` 注解 + `CurrentUserArgumentResolver`（@RequirePermission 留 Phase 3）
- [x] `POST /api/v1/auth/login`（明文密码 + BCrypt 校验，RSA/防暴破/验证码留 Phase 2）
- [x] `POST /api/v1/auth/logout`（拉黑当前 JWT）
- [x] `GET  /api/v1/auth/me`
- [x] `InMemoryUserDetailsLoader` dev profile 内存用户（admin/123456）
- [x] 测试：5 个 JwtTokenProviderTest + 5 个 AuthControllerTest + 1 个 ErpApplicationTests
- [x] **修依赖**：erp-security/pom.xml 加 spring-boot-starter-web（PITFALLS §12）

**Phase 2 — 防护升级 ⬜**
- [⬜] `POST /auth/login` 加 RSA 公钥加密（`GET /auth/public-key`）
- [⬜] `GET /auth/captcha` 验证码 + 防暴破 5 次锁 15 分钟
- [⬜] `POST /auth/refresh` 用 refresh token 换新 access
- [⬜] `PUT /auth/password` 修改密码
- [⬜] `POST /auth/refresh-permissions`

**Phase 3 — 业务接入 ⬜**
- [⬜] `@RequirePermission("xxx:yyy")` 注解（AOP 实现）
- [✅] 前端 `login/index.vue` 接真 API（captcha + login + me 全链路）
- [✅] 路由守卫：未登录跳 `/login`、刷新懒拉 `/me`（菜单按 permissions 过滤待 B1.5 erp-user 后）
- [✅] 前端登出清 Redis Token + 清本地

#### ✅ B1.5 erp-user（第 4 周）后端 ✅ · DB 登录端到端 ✅

### ✅ B1.6 侧边栏菜单 + 系统管理联调 ✅ 2026-06-25
- [✅] JWT permissions authorities（roles+permissions）
- [✅] 权限码对齐 V2 seed
- [✅] RoleVO.permissionIds 回填
- [✅] 前端 UserManage/RoleManage 接真实系统 API
- [✅] 侧边栏按 DB permissions 过滤
- [⬜] 用户新建/编辑/删除 UI（并入 B2）
- [⬜] Excel/CSV 批量导入（B2 计划）
- [✅] `sys_department` 树形 CRUD（含 `parent_id` + `dept_path`）
- [✅] `sys_user` CRUD（含直属上级 superior_id、登录失败锁定）
- [✅] `sys_role` CRUD（含 `data_scope` 数据权限粒度 1/2/3/4）
- [✅] `sys_permission` CRUD（菜单+按钮+API，含 `http_method`）
- [✅] 用户-角色绑定、角色-权限绑定
- [✅] 管理员重置密码、用户改密（接口）
- [⬜] 前端 `system/UserManage.vue` / `RoleManage.vue` 联调
- [✅] `MysqlUserDetailsLoader` 装配修复（`@ConditionalOnProperty` 替换 `@ConditionalOnBean`）
- [⬜] DB 列对齐（BaseEntity ↔ schema：`updated_by` 等缺列待 V3 migration）

---

### ✅ B2. Phase 2 — 产品与客户（第 5-8 周）（完成）

- [✅] erp-product：产品 CRUD / 分类树 / HS 编码 / 多币种价格
- [✅] erp-customer：客户 + 供应商 CRUD（海关编码/SWIFT/Tax ID/付款条款）
- [✅] 前端 `product/ProductList.vue` 联调完成
- [⬜] EasyExcel 批量导入/导出

---

### ✅ B3. Phase 3 — 订单生命周期（第 9-14 周）核心

- [✅] 销售订单 CRUD + 状态流转（draft→submitted→approved→purchasing→shipping→delivered→settled/cancelled）
- [✅] 采购订单 CRUD + 关联销售单
- [✅] 订单明细 ord_sales_order_item / ord_purchase_order_item 子表
- [✅] 状态变更历史 `ord_status_history`
- [✅] 订单利润核算 `ord_profit`（按60%估算成本，`GET /api/v1/sales-orders/{id}/profit`）
- [✅] 前端 `order/SalesOrderList.vue` 联调（含详情弹窗+状态操作按钮）。SalesOrderCreate + PurchaseOrderList 待接真实 API

---

### ⬜ B4. Phase 4 — 财务与审批（第 15-18 周）

- [⬜] 汇率管理 `fin_exchange_rate`
- [⬜] 应收/应付/结算/税务
- [⬜] 资金审批 `fin_fund_approval`（按金额阈值分配审批人）
- [⬜] 审批引擎（`app_workflow` / `app_workflow_node` / `app_approval_request` / `app_approval_history`）
- [⬜] 前端 finance/* + approval/ApprovalPending.vue 联调

---

### ⬜ B5. Phase 5 — 物流、单证与通知（第 19-22 周）

- [⬜] 物流跟踪 `log_shipment` / `log_tracking`
- [⬜] 单证生成（Invoice / Packing List / B/L / C/O）模板化（Apache POI）
- [⬜] 单证版本管理 `doc_document_version`
- [⬜] MinIO 单证存储
- [⬜] 通知中心 `ntf_notification` / `ntf_user_notification`
- [⬜] 前端 logistics/ShipmentList.vue / document/DocumentList.vue 联调

---

### ⬜ B6. Phase 6 — 数据与报表（第 23-26 周）

- [⬜] 数据上传 `dat_upload`（EasyExcel）
- [⬜] 定价分析 `dat_pricing_analysis`
- [⬜] 仪表盘 ECharts 接 `/dashboard` API
- [⬜] 报表导出（订单/利润/资金）
- [⬜] 前端 data/DataUpload.vue / PricingAnalysis.vue / dashboard/index.vue 联调

---

### ⬜ B7. Phase 7 — 测试、安全与部署（第 27-30 周）

- [⬜] 单元测试覆盖率 ≥ 60%
- [⬜] 接口集成测试（Spring Boot Test + Testcontainers）
- [⬜] 前端 e2e（Playwright）
- [⬜] 安全：OWASP 扫描、依赖漏洞（snyk/owasp-dependency-check）
- [⬜] 多实例 + Nginx 负载均衡
- [⬜] CI/CD（GitLab CI 或 Jenkins）
- [⬜] 备份策略 + 日志聚合

---

### ⬜ B8. Phase 8 — 培训与交付（第 31-32 周）

- [⬜] 用户手册 / 管理员手册
- [⬜] 培训视频（5 个部门各一份）
- [⬜] 数据迁移脚本（如有旧系统）
- [⬜] 上线 checklist + 试运行
- [⬜] 售后支持流程

---

## 📊 总体进度看板

| 阶段 | 任务数 | 已完成 | 进行中 | 待办 | 完成率 |
|------|--------|--------|--------|------|--------|
| **A 现有资产** | 4 | 4 | 0 | 0 | 100% |
| **B0 初始化** | 4 | 3 | 1 | 0 | 75% |
| **B1 基础设施** | 5 | 3 | 1 | 1 | 60% |
| **B2 产品/客户** | 4 | 4 | 0 | 0 | 100% |
| **B3 订单（核心）** | 6 | 6 | 0 | 0 | 100% |
| **B4 财务/审批** | 5 | 0 | 0 | 5 | 0% |
| **B5 物流/单证** | 6 | 0 | 0 | 6 | 0% |
| **B6 数据/报表** | 5 | 0 | 0 | 5 | 0% |
| **B7 测试/部署** | 7 | 0 | 0 | 7 | 0% |
| **B8 交付** | 5 | 0 | 0 | 5 | 0% |

**全局进度**：现状盘点 100% / 实际开发 ≈ 46%（B2.1 产品模块后端+前端完成）（B0 ✅ / B1.1 ✅ / B1.2 ✅ / B1.3 ✅ / B1.4 Phase 1+2 ✅ / B1 前端登录 ✅ / **B1.5 erp-user 后端 ✅**（@ConditionalOnProperty 修复装配时序）/ 共 15 commits 71 测试通过 + Flyway V2 button 权限 + MysqlUserDetailsLoader 生产装配）

---

## 🎯 推荐执行顺序

| 优先级 | 任务 | 理由 |
|--------|------|------|
| 🥇 P0 | B0 + B1（环境 + 认证 + erp-common + 用户管理） | 阻塞所有后续，是地基 |
| 🥈 P1 | B2 + B3（产品/客户 + 订单） | 项目核心业务，外贸 ERP 的脊柱 |
| 🥉 P2 | B4 + B5（财务审批 + 物流单证） | 业务闭环 |
| 📦 P3 | B6 + B7 + B8（报表 + 测试部署 + 交付） | 收尾与上线 |

---

## ⚠️ 关键风险与依赖

1. **CentOS 7 兼容性**（server3 是 CentOS 7 + glibc 2.17）
   - Docker 在 CentOS 7 可装但 Docker Compose v2 需新版（同 B2B 项目经验）
   - JDK 17 需用 zulu 或 openjdk-portable 而非系统包
   - Node 20 用 nvm + npmmirror 镜像
   - 详见 `PITFALLS.md`

2. **Spring Boot 版本必须降级**
   - 当前 `pom.xml` 是 4.1.0 (RC)，DEV_GUIDE 明确要求 3.3.5 LTS
   - 4.1 改 API 较多，第三方组件（MyBatis-Plus / Knife4j）尚未跟上

3. **guide/ 被 .gitignore 忽略**
   - 团队协作时设计文档不会随代码提交
   - 建议：①移出 guide 到独立 docs 仓；②或从 .gitignore 删除 `guide/`

4. **前端缺失 api/ 与 store 业务模块**
   - 现有 30 个 view 都是纯样式，没接口调用，开始 B1.4 前必须先补齐 `api/request.ts`

---

## 📝 变更日志

| 日期 | 变更 | 负责 |
|------|------|------|
| 2026-06-24 | ROADMAP 初版创建：盘点现有资产 + 基于 DEV_STEPS 拆解 9 阶段 51 任务 | 系统 |
| 2026-06-24 | B0 完成（JDK17/Maven/14 子模块/编译验证），commit 6b190e7→b3d2a8a | 实施 |
| 2026-06-24 | B1.1 完成（erp-common 9 类 + 45 单测），commit 88d5914 + 2b13b2c | 实施 |
| 2026-06-24 | B1.3 完成（前端基建 pnpm/axios/i18n/JWT store/主题），commit cb37982 | 实施 |
| 2026-06-24 | B1.4 Phase 1 完成（erp-security JWT 最小认证 + 11 单测），commit 56d4f86 | 实施 + Claude 协作 |
| 2026-06-24 | B1.2 完成（MySQL 8 + Redis 7 yum 装、Flyway V1 6 表 + admin seed、starter-jdbc 修复、端到端登录 OK） | 实施 |
| 2026-06-24 | B1.4 Phase 2 完成（change-password / refresh / captcha + 16 单测 + 8 段 e2e 验证全绿，fontconfig 修复） | 实施 + Claude 协作 |
| 2026-06-24 | PITFALLS 追加 §12（erp-security 缺 starter-web）| 实施 |
