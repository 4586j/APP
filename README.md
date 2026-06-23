# 外贸 ERP 系统（demo2）

> **项目位置**：`server3:/code/demo2`
> **技术栈**：Spring Boot 3.3.5 + MyBatis-Plus + MySQL 8 + Redis 7 + Vue 3 + Element Plus + Vite 5
> **目标用户**：100 人左右外贸企业（采购/销售/数据/财务/管理 5 部门）
> **总工期**：约 8 个月（32 周）
> **当前阶段**：🟡 第 0 步 — 项目初始化（骨架已生成，待重构）

---

## 📋 当前状态总览

| 维度 | 状态 | 说明 |
|------|------|------|
| 后端骨架 | ⬜ 待重构 | 仅 1 个空 `Demo2Application.java` + 空 yaml，需重构为 14 子模块 Maven 父子工程 |
| pom 父版本 | ⚠️ 待修正 | 当前 Spring Boot **4.1.0 (RC)**，**必须降到 3.3.5 稳定版**（见 PITFALLS.md §1） |
| 前端骨架 | 🟡 部分完成 | 30 个 `.vue` 页面壳（10 大模块 UI 已成型），但**缺 axios 拦截器、api/ 业务模块、路由守卫、Pinia 业务 store** |
| 数据库 | ⬜ 未执行 | `guide/erp_schema.sql` 含 36 张表 DDL，待用 Flyway 迁移 |
| 中间件 | ⬜ 未部署 | server3 上 MySQL/Redis/MinIO 均未安装 |
| 文档 | ✅ 完整 | guide/ 下 4 份 MD 共 ~13 万字（**注意：guide/ 被 .gitignore 忽略，不进 git**） |

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
├── erp-common         基础公共：BaseEntity / R<T> / 异常 / 枚举
├── erp-security       Spring Security + JWT + 权限注解
├── erp-user           用户/角色/权限/部门（sys_*）
├── erp-product        产品/分类/HS编码/多币种价格（prd_*）
├── erp-customer       客户/供应商（crm_*）
├── erp-order          销售单/采购单/状态流转（ord_*）— 核心模块
├── erp-finance        汇率/应收/应付/结算/税务（fin_*）
├── erp-approval       工作流/审批引擎（app_*）
├── erp-logistics      物流跟踪（log_*）
├── erp-document       单证生成与版本（doc_*）
├── erp-data           数据上传/定价分析（dat_*）
├── erp-notification   通知中心（ntf_*）
├── erp-dashboard      仪表盘聚合查询
└── erp-web            启动入口 + 全局配置 + Flyway 迁移
```

### 前端模块（已存在的页面壳）

```
erp-frontend/src/views/
├── login/              ✓ 登录
├── dashboard/          ✓ 仪表盘
├── system/             ✓ 用户管理 + 角色管理
├── product/            ✓ 产品列表
├── customer/           ✓ 客户列表 + 供应商列表
├── order/              ✓ 销售订单列表/创建 + 采购订单列表
├── finance/            ✓ 财务总览 + 资金审批 + 汇率
├── logistics/          ✓ 物流列表
├── document/           ✓ 单证列表
├── data/               ✓ 数据上传 + 定价分析
├── approval/           ✓ 待我审批
└── error/              ✓ 404
```

⚠️ **缺失目录**：`api/`、`utils/`（含 axios 拦截器）、按模块拆分的 Pinia store、路由守卫逻辑、i18n 配置。

---

## 🚀 运行方式（目标态，当前未启动）

### 前置依赖
- JDK 17 LTS
- Maven 3.9+（项目含 `mvnw`）
- Node.js 20 LTS + pnpm
- Docker（启动 MySQL + Redis + MinIO）

### 启动步骤
```bash
# 1. 启动中间件（先在项目根创建 docker-compose.yml，见 DEV_GUIDE §12.3）
docker compose up -d

# 2. 后端
./mvnw clean compile
./mvnw -pl erp-web spring-boot:run
# 启动后 Flyway 自动迁移 36 张表

# 3. 前端
cd erp-frontend
pnpm install
pnpm dev
# 访问 http://localhost:5173
```

### 默认登录
- 待 `V1__init_system_tables.sql` 注入种子数据后确定

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
