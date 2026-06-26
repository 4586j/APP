# 外贸 ERP 系统（demo2）

> **项目位置**：`server3:/code/demo2`
> **技术栈**：Spring Boot 3.3.5 + MyBatis-Plus + MySQL 8 + Redis 7 + Vue 3 + Element Plus + Vite 5
> **目标用户**：100 人左右外贸企业（采购/销售/数据/财务/管理 5 部门）
> **总工期**：约 8 个月（32 周）
> **当前阶段**：🟢 第 6 阶段 — 数据报表 + 工作台 + 工作报表管理（开发中）
> **最后更新**：2026-06-26

---

## 📋 当前状态总览

| 维度 | 状态 | 说明 |
|------|------|------|
| 后端骨架 | ✅ 完成 | 14 子模块 Maven 多模块工程，Spring Boot 3.3.5 LTS，编译/打包/启动正常 |
| 数据库 | ✅ 完成 | Flyway 37 个迁移脚本已执行，MySQL 8 + Redis 7 已部署运行 |
| 认证授权 | ✅ 完成 | JWT + Spring Security + 权限注解 `@PreAuthorize`，DB 登录端到端 |
| 前端骨架 | ✅ 完成 | Vue 3 + Vite + TypeScript + Element Plus + Pinia，axios 拦截器、路由守卫、i18n 齐全 |
| 系统管理 | ✅ 完成 | 用户/角色/部门/权限 CRUD，部门树，用户批量导入，权限绑定 |
| 数据中心 | ✅ 完成 | 文件上传下载、定价分析、Excel 批量导入/模板下载 |
| 工作台 | ✅ 完成 | 欢迎区、快捷入口、待办、通知、审批、数据概览、工作计划/日志 |
| 工作报表 | ✅ 完成 | 工作计划（08:30-10:30 限时）+ 工作日志 + 管理界面 + 批量审批 |
| 中间件 | 🟡 部分 | MySQL 8 + Redis 7 ✅，MinIO ❌（本地文件系统暂代） |
| Redis 缓存 | ✅ 完成 | Token 黑名单 / 验证码 / 部门选项 / 权限树 / 角色列表 / 用户信息 / 登录失败计数 |
| 文档 | ✅ 完整 | guide/ 下 4 份 MD + ROADMAP.md + PITFALLS.md（**application.yaml 已脱敏，真配置不进 git**） |

---

## 🏗️ 架构总览

```
┌──────────────┐    HTTP/JSON     ┌────────────────────────┐
│ Vue3 前端     │ ───────────────► │  Spring Boot 后端       │
│ Element Plus  │  JWT Bearer      │  14 子模块单体应用       │
│ Vite + Pinia  │                  │  /api/v1/*              │
└──────────────┘                  └────────┬───────────────┘
                                            │
                       ┌────────────────────┼────────────────────┐
                       │                    │                    │
                  ┌────▼─────┐         ┌────▼─────┐         ┌────▼─────┐
                  │  MySQL 8  │         │  Redis 7  │         │  MinIO   │
                  │ 36 张表    │         │ Token黑名单 │         │ 单证/附件 │
                  └──────────┘         └──────────┘         └──────────┘
```

### 后端 14 子模块（DEV_GUIDE §3）

```
erp-parent (pom)
├── erp-common         基础公共：BaseEntity / R<T> / 异常 / 枚举 / MinioTemplate
├── erp-security       Spring Security + JWT + 权限注解 + @CurrentUser
├── erp-user           用户/角色/权限/部门（sys_*）+ 部门-权限关联
├── erp-product        产品/分类/HS编码/多币种价格（prd_*）
├── erp-customer       客户/供应商（crm_*）
├── erp-order          销售单/采购单/状态流转（ord_*）— 核心模块
├── erp-finance        汇率/应收/应付/结算/资金审批/工作流（fin_* / app_*）
├── erp-approval       工作流/审批引擎（预留，实际在 erp-finance 实现）
├── erp-logistics      物流跟踪（log_*）
├── erp-document       单证生成与版本（doc_*）
├── erp-data           数据上传/定价分析（dat_*）+ Excel 批量导入
├── erp-notification   通知中心（ntf_*）+ 站内通知/未读/已读
├── erp-report         报表导出 + 工作报表（工作计划/日志/审批）
└── erp-web            启动入口 + 全局配置 + Flyway 迁移 + JacksonConfig
```

### 前端模块（已联调完成的页面）

```
erp-frontend/src/views/
├── login/              ✅ 登录（JWT + captcha + 自动重登）
├── dashboard/          ✅ 个人工作台（欢迎/快捷入口/待办/通知/审批/统计/计划/日志）
├── system/             ✅ 用户管理 + 角色管理 + 部门管理 + 权限管理
├── product/            ✅ 产品列表
├── customer/           ✅ 客户列表 + 供应商列表
├── order/              ✅ 销售订单列表/创建 + 采购订单列表
├── finance/            ✅ 财务总览 + 资金审批 + 汇率
├── logistics/          ✅ 物流列表
├── document/           ✅ 单证列表
├── data/               ✅ 数据上传（支持下载）+ 定价分析（Excel 批量导入）
├── approval/           ✅ 待我审批
├── report/             ✅ 工作报表管理（合并显示/筛选/批量审批）
└── error/              ✅ 404
```

**前端基础设施**：`api/`（按模块拆分）、`utils/`（storage/format）、`types/`（TS 接口）、`store/`（Pinia userStore）、`router/`（路由守卫 + 权限过滤）、`components/`（DepartmentSelect 等复用组件）。
**特性**：axios 拦截器（401 自动跳转）、vue-i18n 9、SCSS 主题、Element Plus 自动导入。

---

## 🚀 运行方式

### 前置依赖
- JDK 17 LTS（已安装于 `/opt/jdk/current`）
- Maven 3.9+（或 `./mvnw`）
- Node.js 20 LTS + pnpm
- MySQL 8 + Redis 7（server3 已部署，systemd 管理）

### 后端启动
```bash
# 1. 编译打包
cd /code/demo2
mvn clean package -DskipTests

# 2. 复制 jar 并启动（systemd 服务）
cp erp-web/target/erp-web.jar /tmp/erp-web.jar
systemctl restart erp-web.service

# 查看日志
journalctl -u erp-web.service -f
```

### 前端启动
```bash
cd /code/demo2/erp-frontend
pnpm install
pnpm dev
# 访问 http://localhost:3000（Vite 代理 /api → localhost:8081）
```

### 默认登录
- **admin / admin123**（系统管理员，拥有所有权限）
- 登录后 JWT Token 有效期 30 分钟，Refresh Token 30 天

---

## 📚 文档索引

| 文档 | 路径 | 用途 |
|------|------|------|
| 开发指南 | `guide/DEV_GUIDE.md` | 技术选型、架构、模块、安全模型、多币种、编码规范（13章） |
| 开发步骤 | `guide/DEV_STEPS.md` | 32 周分阶段实施路线图（每周任务清单 + 验收标准） |
| API 设计 | `guide/API_DESIGN.md` | 26 个模块的 REST API 完整规范（含统一响应、错误码、数据权限） |
| 数据库 schema | `guide/erp_schema.sql` | 36 张表 DDL（已含索引/约束/注释） |
| **路线看板** | `ROADMAP.md` | **本仓库开发进度跟踪（基于 DEV_STEPS 拆解）** |
| **踩坑总集** | `PITFALLS.md` | **项目过程中遇到的问题、根因、解法** |

> ⚠️ guide/ 整目录被 .gitignore 忽略（参见 PITFALLS.md §3），本仓库以 ROADMAP/PITFALLS 作为团队协作文档。

---

## 🎯 推荐执行顺序

按 DEV_STEPS 路线图，第一批必须做完才能解锁后续：

```
🥇 第 0-1 步：环境 + erp-common + 数据库 + 认证 → 解锁所有后续模块
🥈 Phase 2：产品 + 客户              → 解锁订单
🥉 Phase 3：订单全部流程              → 项目核心
📦 Phase 4-8：财务/物流/单证/数据/部署
```

详见 `ROADMAP.md`。

---

## 🔗 版本与变更

| 日期 | 变更 |
|------|------|
| 2026-06-24 | 项目文档骨架建立（README + ROADMAP + PITFALLS），基线现状盘点完成 |
| 2026-06-24 | B0 完成：JDK17/Maven/14 子模块/编译验证 |
| 2026-06-24 | B1 完成：MySQL 8 + Redis 7 + Flyway V1~V3 + JWT 认证 + 前端基建 |
| 2026-06-25 | B1.6 完成：系统管理联调（用户/角色/部门/权限）+ 修复 `@ConditionalOnBean` 时序陷阱 |
| 2026-06-26 | **优化阶段 3 完成**：工作台重构 + 数据下载/导入 + 工作报表管理 + ID 精度修复 + 软删除修复 + application.yaml 脱敏
| 2026-06-26 | **优化阶段 4 完成**：批量导入事务修复 + 分页 size 上限 + Swagger/Knife4j 接口文档
| 2026-06-26 | **优化阶段 5 完成**：Redis 缓存优化（Key 规范 + 权限树/角色列表/用户缓存 + 登录失败计数 Redis 化 + CacheTemplate 降级封装）
