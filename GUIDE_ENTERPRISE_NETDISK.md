# 企业网盘 / 共享硬盘 实施指南

> **目标**：把现有的数据上传（`dat_upload`）升级成带目录树、部门隔离、WebDAV 网络磁盘的企业级文件管理系统
> **创建日期**：2026-06-28
> **状态**：计划阶段

---

## 总体架构

```
┌──────────────────────────────────────────────────────────┐
│                  用户访问层                                │
│  ┌─────────────┐  ┌──────────────┐  ┌──────────────────┐ │
│  │ 网页文件管理器 │  │ Windows 资源  │  │ 第三方 WebDAV   │ │
│  │ (Phase 1)   │  │ 管理器挂载   │  │ 客户端 (Phase 2) │ │
│  └──────┬──────┘  └──────┬───────┘  └────────┬─────────┘ │
└─────────┼────────────────┼───────────────────┼───────────┘
          │                │                   │
┌─────────▼────────────────▼───────────────────▼───────────┐
│                   WebDAV 协议层 (Phase 2)                  │
│    PROPFIND / GET / PUT / MKCOL / DELETE / MOVE          │
│    所有请求先过 JWT 鉴权 + 部门隔离 + 权限校验             │
└─────────────────────────┬─────────────────────────────────┘
                          │
┌─────────────────────────▼─────────────────────────────────┐
│                   REST API 层 (Phase 0+1)                  │
│  GET /files?parentId=X    POST /files/folder              │
│  POST /files/upload       PUT /files/{id}/rename          │
│  PUT /files/{id}/move     DELETE /files/{id}              │
│  GET /files/{id}/download GET /files/breadcrumb?id=X      │
└─────────────────────────┬─────────────────────────────────┘
                          │
┌─────────────────────────▼─────────────────────────────────┐
│                   数据库 + 存储层                           │
│  dat_file (自引用树结构)    dat_file_share (共享部门)      │
│  物理文件: ./uploads/data/{dept_id}/{yyyy}/{mm}/{uuid}.ext │
└──────────────────────────────────────────────────────────┘
```

---

## Phase 0：数据库改造 — 目录树结构（预估 2-3 天）

### 0.1 表结构改造

```sql
-- dat_upload → dat_file
-- 新增 parent_id(自引用), is_directory, extension, mime_type
-- 物理路径改为按部门/日期分片
```

### 0.2 后端 API

| 接口 | 方法 | 说明 |
|------|------|------|
| `/api/v1/files` | GET | 列出目录内容（parentId 参数） |
| `/api/v1/files/breadcrumb` | GET | 面包屑路径 |
| `/api/v1/files/folder` | POST | 新建文件夹 |
| `/api/v1/files/upload` | POST | 上传文件到指定目录 |
| `/api/v1/files/{id}/rename` | PUT | 重命名 |
| `/api/v1/files/{id}/move` | PUT | 移动到其他目录 |
| `/api/v1/files/{id}` | DELETE | 移到回收站 |
| `/api/v1/files/{id}/download` | GET | 下载 |

### 0.3 数据隔离

沿用现有规则：
- 管理员 → 全部可见
- 普通用户 → 自己上传的 OR 本部门及下级部门的 OR 共享给本部门的

### 0.4 文件

- `V40__create_dat_file_table.sql` — 新建表
- `V41__migrate_dat_upload_to_dat_file.sql` — 迁移数据
- `DatFile.java` — 实体
- `DatFileMapper.java` — Mapper
- `DatFileService.java` — Service 接口
- `DatFileServiceImpl.java` — 实现
- `DatFileController.java` — 控制器

---

## Phase 1：前端文件管理器 UI（预估 3-4 天）

### 1.1 布局

```
┌────────────────────────────────────────────────────────────┐
│  ◀ 后退  ▶ 前进  ▲ 上级  🔄 刷新  📁 新建文件夹  ⬆ 上传  │
│  路径: 数据部 > 项目文档 > 技术方案                        │
├──────────┬─────────────────────────────────────────────────┤
│ 📁 数据部  │  名称         大小   类型   修改人   修改时间  │
│ ├📁 报表   │  📄 方案.docx  2MB   Word   张三    06-28    │
│ ├📁 项目   │  📊 数据.xlsx  5MB   Excel  李四    06-27    │
│ │ ├📁 技术 │  🖼️ 架构图.png 3MB   图片   王五    06-26    │
│ │ └📁 业务 │  📝 需求.txt   1KB   文本   张三    06-25    │
│ └📁 资料   │                                              │
└──────────┴─────────────────────────────────────────────────┘
```

### 1.2 功能点

- **左侧目录树**：部门层级树
- **主区域**：列表视图 + 网格视图切换
- **面包屑导航**：每段可点击跳转
- **右键菜单**：新建文件夹、重命名、移动、下载、删除
- **拖拽上传**：支持文件和文件夹拖拽
- **搜索**：当前目录及子目录
- **预览**：图片/PDF/文本直接预览

### 1.3 文件

- `FileManage.vue` — 主页面
- `api/data.ts` — 新增文件管理 API
- `router/index.ts` — 新增路由

---

## Phase 2：WebDAV 网络磁盘（预估 4-5 天）

### 2.1 实现方式

Spring Boot 实现 WebDAV 协议处理器：

```java
@RestController
@RequestMapping("/webdav")
public class WebDavController {
    // PROPFIND → 列出目录（Windows 打开文件夹时调用）
    // GET → 下载文件（双击打开时调用）
    // PUT → 保存文件（Ctrl+S 时调用）
    // MKCOL → 新建文件夹
    // DELETE → 删除
    // MOVE → 重命名/移动
    // LOCK/UNLOCK → 文件锁定
}
```

### 2.2 鉴权

Windows WebDAV 客户端支持 Basic Auth：
```
用户映射网络驱动器时输入 ERP 账号密码
后端拦截 Basic Auth → 校验 → 生成临时 JWT → 走正常鉴权
```

### 2.3 数据隔离

所有 WebDAV 请求走同一套部门隔离逻辑：
- 列出目录 → 部门隔离 + 共享部门可见
- 读取文件 → 检查权限
- 写入文件 → 检查权限

### 2.4 文件

- `WebDavController.java` — WebDAV 协议处理器
- `WebDavAuthFilter.java` — Basic Auth → JWT 转换
- 依赖：`sardine` 库或原生 `javax.servlet` 实现

---

## Phase 3：在线预览（预估 2-3 天）

| 类型 | 方案 |
|------|------|
| 图片 (.jpg/.png/.gif/.svg) | `<img>` 标签 |
| PDF | 浏览器 `<embed>` 或 PDF.js |
| 文本 (.txt/.md/.json/.xml/.log) | Monaco Editor 只读 |
| 视频 (.mp4/.webm) | HTML5 `<video>` |
| Office (.docx/.xlsx/.pptx) | 提示下载（或后续 OnlyOffice） |

---

## Phase 4：OnlyOffice 在线编辑（预估 5-7 天，按需）

> **注意**：Phase 2 WebDAV 实现后，用户可以直接在本地用 WPS/Office 打开编辑，Ctrl+S 自动保存。OnlyOffice 仅当需要在浏览器内直接编辑时才需要。

### 部署

```bash
docker run -d -p 8088:80 onlyoffice/documentserver
```

### 集成

- 后端：OnlyOffice 回调 API 处理保存
- 前端：OnlyOffice 编辑器 iframe 嵌入

---

## 实施顺序建议

```
Phase 0 ──→ Phase 1 ──→ Phase 2 ──→ Phase 3 ──→ Phase 4
  (基础)      (UI)       (网络磁盘)    (预览)      (按需)
  2-3天      3-4天       4-5天        2-3天      5-7天
                    ↓
             用户获得核心体验：
             Windows 资源管理器直接操作
             同时受 ERP 权限管控
```

---

## 拆解步骤（按实现顺序）

### Step 1: Flyway V40 — 新建 dat_file 表
### Step 2: Flyway V41 — 迁移 dat_upload 数据到 dat_file
### Step 3: 实体 + Mapper — DatFile.java / DatFileMapper.java
### Step 4: Service — DatFileService 接口 + 实现（含部门隔离）
### Step 5: Controller — DatFileController REST API
### Step 6: 前端 API — api/data.ts 新增文件管理接口
### Step 7: 前端 UI — FileManage.vue 文件管理器（列表视图 + 面包屑 + 目录树）
### Step 8: 前端路由 — 替换旧数据上传路由
### Step 9: 在线预览 — 图片/PDF/文本预览组件
### Step 10: WebDAV — 协议处理器 + 鉴权
