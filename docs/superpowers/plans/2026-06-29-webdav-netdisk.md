# WebDAV 网络磁盘 实现计划

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 在已完成的 `DatFile` 网盘上增加 WebDAV 协议适配层，使 Windows 资源管理器可映射网络驱动器直接编辑保存，全程受 ERP 部门权限管控。

**Architecture:** 薄协议适配层 `com.erp.data.webdav`，把 WebDAV 动词（PROPFIND/GET/PUT/MKCOL/DELETE/MOVE/LOCK/UNLOCK）翻译成对现有 `DatFileService` 的调用，复用并小幅放宽其权限逻辑。Basic Auth 经 `WebDavAuthFilter` 写入 SecurityContext 后由现有 `@CurrentUser` 解析。物化路径 `dat_file.path` 加速共享继承可见性查询。

**Tech Stack:** Spring Boot 3 / Spring Security 6 / MyBatis-Plus / MySQL 8.4 / Flyway / JUnit5 + Mockito + MockMvc

**关联 spec:** `docs/superpowers/specs/2026-06-29-webdav-netdisk-design.md`

## Global Constraints

- 传输协议：HTTP（局域网），Basic Auth 鉴权
- 服务端实现：手写 `@RestController` 适配层，**不引入 sardine / milton**
- 共享模型：文件级 + 文件夹继承（祖先文件夹共享 → 后代可见可改）
- 写入归属：新文件 `deptId = 所在目录部门`；编辑已存在文件不动 `deptId`
- 新建/上传仅限本部门目录（`canCreate` 仅本部门）；下级部门只读；跨部门 MOVE 拒绝
- LOCK 独占：锁前提 `canWrite`；被锁文件对非持锁者「可见但拒读」(GET 423)
- 物化路径：`dat_file.path` 字段（如 `/3/15/42/`），与 `sys_department.dept_path` 模式一致
- Flyway 续号：下一个版本号 **V42**
- 包路径：所有 WebDAV 新类放 `com.erp.data.webdav`
- WebDAV 响应：HTTP 状态码 + XML body，**不能用** 现有 `R<T>` JSON 包装
- 字符编码：所有 WebDAV 响应 `charset=utf-8`，中文文件名走 XML body 不放 header
- 不改：`DatFileController` REST API（网页管理器仍用）、`JwtAuthenticationFilter` 主体逻辑

---

## 文件结构

### 新建文件

| 文件 | 职责 |
|---|---|
| `erp-web/src/main/resources/db/migration/V42__add_dat_file_path.sql` | 加 `dat_file.path` 列 + 递归 CTE 回填 |
| `erp-data/src/main/java/com/erp/data/webdav/ResolvedPath.java` | 路径解析结果（类型 + deptId + DatFile） |
| `erp-data/src/main/java/com/erp/data/webdav/WebDavPathResolver.java` | `/销售部/报表/x.xlsx` → ResolvedPath |
| `erp-data/src/main/java/com/erp/data/webdav/WebDavPropFindXmlBuilder.java` | DatFile 列表 → multistatus XML |
| `erp-data/src/main/java/com/erp/data/webdav/WebDavLockStore.java` | 内存锁状态（tryLock/unlock/assertLockHeld） |
| `erp-data/src/main/java/com/erp/data/webdav/WebDavLockInfo.java` | 锁信息（fileId/owner/token/expireAt） |
| `erp-data/src/main/java/com/erp/data/webdav/WebDavAuthFilter.java` | Basic Auth → SecurityContext |
| `erp-data/src/main/java/com/erp/data/webdav/WebDavErrors.java` | 业务异常 → WebDAV 状态码/XML |
| `erp-data/src/main/java/com/erp/data/webdav/WebDavController.java` | 协议处理器（8 动词） |
| `erp-data/src/main/java/com/erp/data/webdav/WebDavSecurityConfig.java` | `/webdav/**` 放行进 Basic Auth |

### 修改文件

| 文件 | 改动 |
|---|---|
| `erp-data/.../entity/DatFile.java` | 加 `private String path;` |
| `erp-data/.../mapper/DatFileMapper.java` | 加 `selectSharedDescendantExists` / `selectAncestorShareDeptIds` / `updatePathPrefix` |
| `erp-data/.../service/DatFileService.java` | 加 `canCreate` / `writeContent` 接口方法 |
| `erp-data/.../service/impl/DatFileServiceImpl.java` | 放宽 `canAccess`/`canWrite`（共享继承）；加 `canCreate`/`writeContent`；维护 `path` |
| `erp-security/.../config/SecurityConfig.java` | `PUBLIC_PATHS` 加 `/webdav/**`（让 WebDavAuthFilter 接管，不走 JWT 白名单但允许匿名进入 filter） |

### 新建测试

| 文件 | 测什么 |
|---|---|
| `erp-data/src/test/.../webdav/WebDavPropFindXmlBuilderTest.java` | XML 结构/中文/字段 |
| `erp-data/src/test/.../webdav/WebDavLockStoreTest.java` | 加锁/解锁/超时/冲突 |
| `erp-data/src/test/.../webdav/WebDavPathResolverTest.java` | 各 ResolvedPath 类型/中文/嵌套 |
| `erp-data/src/test/.../service/impl/DatFilePermissionTest.java` | canAccess/canWrite/canCreate 矩阵 + path 维护 |
| `erp-web/src/test/.../webdav/WebDavControllerTest.java` | 协议动词 → 状态码/XML（@MockBean 业务层） |

---

## Task 1: V42 迁移 — dat_file.path 列 + 回填

**Files:**
- Create: `erp-web/src/main/resources/db/migration/V42__add_dat_file_path.sql`

**Interfaces:**
- Produces: `dat_file.path VARCHAR(768)` 列，存量数据回填为从根到自身的 id 链（如 `/3/15/42/`），根节点为 `/{id}/`

- [ ] **Step 1: 写迁移脚本**

```sql
SET NAMES utf8mb4;

-- V42: dat_file 物化路径，加速 WebDAV 共享继承可见性查询
ALTER TABLE `dat_file` ADD COLUMN `path` VARCHAR(768) DEFAULT NULL COMMENT '物化路径，根到自身的 id 链，如 /3/15/42/' AFTER `parent_id`;
CREATE INDEX `idx_path` ON `dat_file` (`path`);

-- 回填：递归 CTE 算出每条记录从根到自身的 id 链
WITH RECURSIVE file_path(id, parent_id, chain) AS (
    SELECT id, parent_id, CONCAT('/', id, '/') FROM dat_file WHERE parent_id IS NULL
    UNION ALL
    SELECT f.id, f.parent_id, CONCAT(fp.chain, f.id, '/')
    FROM dat_file f JOIN file_path fp ON f.parent_id = fp.id
)
UPDATE dat_file f JOIN file_path fp ON f.id = fp.id SET f.path = fp.chain;
```

- [ ] **Step 2: 验证迁移可执行**

Run: `mvn -pl erp-web test-compile -DskipTests -q` （确保新脚本被打包）
然后启动应用（或跑任意一个 erp-web 集成测试）确认 Flyway 无报错。

Expected: Flyway 应用 V42 成功，`dat_file` 多出 `path` 列且非空。

- [ ] **Step 3: Commit**

```bash
git add erp-web/src/main/resources/db/migration/V42__add_dat_file_path.sql
git commit -m "feat(data): V42 增加 dat_file.path 物化路径列并回填"
```

---

## Task 2: DatFile 实体加 path 字段 + Mapper 查询方法

**Files:**
- Modify: `erp-data/src/main/java/com/erp/data/entity/DatFile.java`
- Modify: `erp-data/src/main/java/com/erp/data/mapper/DatFileMapper.java`
- Test: `erp-data/src/test/java/com/erp/data/mapper/DatFileMapperTest.java`

**Interfaces:**
- Produces: `DatFile.path` 字段；Mapper 方法：
  - `boolean selectSharedDescendantExists(String pathPrefix, Long deptId)` — `pathPrefix` 形如 `/3/15/`，查该前缀下是否有文件共享给 deptId
  - `List<Long> selectAncestorShareDeptIds(Long fileId, Long deptId)` — 查 fileId 的祖先链上是否有文件夹共享给 deptId（命中返回含 deptId 的列表）
  - `int updatePathPrefix(String oldPrefix, String newPrefix)` — 移动文件夹时替换子树 path 前缀

- [ ] **Step 1: 实体加字段**

在 `DatFile.java` 的 `private Long parentId;` 下一行加：

```java
    private String path;
```

- [ ] **Step 2: 写 Mapper 测试（验证 SQL 语义，用 @MybatisTest 风格——但 erp-data 无数据源，改为纯 SQL 文本校验）**

erp-data 无测试数据库，Mapper 的 SQL 正确性靠 erp-web 集成测试覆盖。此处跳过 Mapper 单测，直接写实现，在 Task 11 集成测试中验证。

- [ ] **Step 3: 写 Mapper 实现**

在 `DatFileMapper.java` 接口末尾（`selectByUserId` 方法后）追加：

```java
    /**
     * 查询 path 前缀下是否存在被共享给指定部门的后代文件/文件夹。
     * @param pathPrefix 形如 "/3/15/"（含末尾斜杠）
     * @param deptId     被共享部门
     * @return true=存在
     */
    @Select("SELECT EXISTS(SELECT 1 FROM dat_file f " +
            "JOIN dat_file_share s ON s.file_id = f.id " +
            "WHERE f.path LIKE CONCAT(#{pathPrefix}, '%') AND s.dept_id = #{deptId} AND f.deleted = 0)")
    boolean selectSharedDescendantExists(@Param("pathPrefix") String pathPrefix, @Param("deptId") Long deptId);

    /**
     * 查询 fileId 的祖先链（含自身）上是否有共享给 deptId 的记录。
     * 利用 path 拆出祖先 id 集合。
     * @return 命中则返回含 deptId 的列表（非空即有权限）
     */
    @Select("SELECT s.dept_id FROM dat_file_share s " +
            "JOIN dat_file f ON f.id = s.file_id " +
            "WHERE f.id IN " +
            "(SELECT CAST(SUBSTRING_INDEX(SUBSTRING_INDEX(path_tokens.seq, '/', n.idx), '/', -1) AS UNSIGNED) " +
            " FROM dat_file cur " +
            " JOIN (SELECT 1 AS idx UNION SELECT 2 UNION SELECT 3 UNION SELECT 4 UNION SELECT 5 " +
            "       UNION SELECT 6 UNION SELECT 7 UNION SELECT 8 UNION SELECT 9 UNION SELECT 10) n " +
            "   ON n.idx <= (LENGTH(cur.path) - LENGTH(REPLACE(cur.path, '/', ''))) - 1 " +
            " CROSS JOIN (SELECT @seq) path_tokens " +
            " WHERE cur.id = #{fileId}) " +
            "AND s.dept_id = #{deptId}")
    List<Long> selectAncestorShareDeptIds(@Param("fileId") Long fileId, @Param("deptId") Long deptId);
```

> 说明：祖先 id 拆分用 SUBSTRING_INDEX 枚举（支持最多 10 层深度，覆盖绝大多数目录树；超出可扩展）。`@seq` 参数此处占位，实际改用更稳的写法（见 Step 4 修正）。

- [ ] **Step 4: 修正祖先查询为纯 JOIN 写法（避免会话变量）**

上述 `selectAncestorShareDeptIds` 含会话变量 `@seq` 在 MyBatis 下不稳定。替换为：先用一条查询取 `path` 字符串，在 Java 侧拆分祖先 id，再用 `IN` 批量查共享。改 Mapper 为：

```java
    /**
     * 查询一批 file id 中，被共享给指定部门的 id（用于祖先共享判定）。
     */
    @Select("<script>SELECT s.file_id FROM dat_file_share s WHERE s.dept_id = #{deptId} " +
            "AND s.file_id IN " +
            "<foreach collection='ancestorIds' item='aid' open='(' separator=',' close=')'>#{aid}</foreach></script>")
    List<Long> selectSharedFileIdsIn(@Param("ancestorIds") Collection<Long> ancestorIds, @Param("deptId") Long deptId);
```

并删除 Step 3 中含 `@seq` 的 `selectAncestorShareDeptIds`。祖先 id 拆分逻辑放 `DatFileServiceImpl`（Task 4）用 Java 完成：取 `file.path`（如 `/3/15/42/`），split 得 `[3,15,42]`，调 `selectSharedFileIdsIn`。

- [ ] **Step 5: 加 updatePathPrefix（移动文件夹时更新子树）**

在 Mapper 追加：

```java
    /**
     * 移动文件夹时，把子树 path 的旧前缀替换为新前缀。
     * @param oldPrefix 旧前缀，如 "/3/15/"
     * @param newPrefix 新前缀，如 "/7/15/"
     * @return 受影响行数
     */
    @Update("UPDATE dat_file SET path = CONCAT(#{newPrefix}, SUBSTRING(path, LENGTH(#{oldPrefix}) + 1)) " +
            "WHERE path LIKE CONCAT(#{oldPrefix}, '%')")
    int updatePathPrefix(@Param("oldPrefix") String oldPrefix, @Param("newPrefix") String newPrefix);
```

- [ ] **Step 6: 编译通过**

Run: `mvn -pl erp-data -am compile -q`
Expected: BUILD SUCCESS

- [ ] **Step 7: Commit**

```bash
git add erp-data/src/main/java/com/erp/data/entity/DatFile.java erp-data/src/main/java/com/erp/data/mapper/DatFileMapper.java
git commit -m "feat(data): DatFile 加 path 字段，Mapper 加共享继承查询"
```

---

## Task 3: DatFileServiceImpl 权限放宽 + path 维护

**Files:**
- Modify: `erp-data/src/main/java/com/erp/data/service/DatFileService.java`
- Modify: `erp-data/src/main/java/com/erp/data/service/impl/DatFileServiceImpl.java`
- Test: `erp-data/src/test/java/com/erp/data/service/impl/DatFilePermissionTest.java`

**Interfaces:**
- Consumes: Task 2 的 `DatFile.path`、`selectSharedDescendantExists`、`selectSharedFileIdsIn`、`updatePathPrefix`
- Produces:
  - `boolean canCreate(Long targetDeptId, LoginUser user)` — 仅本部门可建
  - `void writeContent(Long fileId, InputStream in, LoginUser user)` — 覆盖文件内容，不动 deptId
  - 放宽后的 `canAccess`（含祖先共享继承）/ `canWrite`（含共享可改）
  - `createFolder`/`uploadFile`/`move` 维护 `path`

- [ ] **Step 1: 接口加方法**

在 `DatFileService.java` 接口末尾追加：

```java
    /**
     * 检查用户是否可在指定部门目录下新建（仅本部门）。
     */
    boolean canCreate(Long targetDeptId, LoginUser user);

    /**
     * 覆盖已存在文件的内容（WebDAV PUT 保存），不改动 deptId。
     */
    void writeContent(Long fileId, java.io.InputStream in, LoginUser user);
```

- [ ] **Step 2: 写失败测试（权限矩阵，先于实现）**

新建 `DatFilePermissionTest.java`，用 Mockito mock `DatFileMapper`/`DatFileShareMapper`/`JdbcTemplate`，测各权限分支：

```java
package com.erp.data.service.impl;

import com.erp.data.entity.DatFile;
import com.erp.data.mapper.DatFileMapper;
import com.erp.data.mapper.DatFileShareMapper;
import com.erp.security.user.LoginUser;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DatFilePermissionTest {

    @Mock DatFileMapper mapper;
    @Mock DatFileShareMapper shareMapper;
    @Mock JdbcTemplate jdbcTemplate;
    @InjectMocks DatFileServiceImpl service;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(service, "uploadRoot", java.nio.file.Paths.get("./uploads/data"));
    }

    private LoginUser user(Long id, Long deptId, boolean admin) {
        return LoginUser.builder()
                .id(id).username("u" + id).departmentId(deptId)
                .roles(admin ? List.of("ROLE_ADMIN") : List.of()).build();
    }

    private DatFile file(Long id, Long deptId, Long createdBy, String path) {
        DatFile f = new DatFile();
        f.setId(id); f.setDeptId(deptId); f.setCreatedBy(createdBy);
        f.setPath(path); f.setIsDirectory(0); f.setDeleted(0);
        return f;
    }

    @Test
    void canCreate_ownDept_returnsTrue() {
        assertTrue(service.canCreate(10L, user(1L, 10L, false)));
    }

    @Test
    void canCreate_otherDept_returnsFalse() {
        assertFalse(service.canCreate(20L, user(1L, 10L, false)));
    }

    @Test
    void canWrite_admin_returnsTrue() {
        assertTrue(service.canWrite(file(1L, 20L, 99L, "/1/"), user(1L, 10L, true)));
    }

    @Test
    void canWrite_ownDept_returnsTrue() {
        assertTrue(service.canWrite(file(1L, 10L, 99L, "/1/"), user(2L, 10L, false)));
    }

    @Test
    void canWrite_subDept_returnsFalse() {
        DatFile f = file(1L, 11L, 99L, "/1/");
        when(jdbcTemplate.queryForList(any(String.class), eq(10L))).thenReturn(java.util.List.of(
                java.util.Map.of("id", 10L), java.util.Map.of("id", 11L)));
        assertFalse(service.canWrite(f, user(2L, 10L, false)));
    }

    @Test
    void canWrite_sharedToMyDept_returnsTrue() {
        DatFile f = file(1L, 20L, 99L, "/1/");
        when(shareMapper.selectDeptIdsByFileId(1L)).thenReturn(List.of(10L));
        assertTrue(service.canWrite(f, user(2L, 10L, false)));
    }

    @Test
    void canAccess_ancestorFolderShared_returnsTrue() {
        DatFile f = file(42L, 20L, 99L, "/3/15/42/");
        when(shareMapper.selectDeptIdsByFileId(42L)).thenReturn(List.of());
        when(mapper.selectSharedFileIdsIn(java.util.List.of(3L, 15L, 42L), 10L))
                .thenReturn(List.of(15L));
        assertTrue(service.canAccess(f, user(2L, 10L, false)));
    }

    @Test
    void canAccess_noRelation_returnsFalse() {
        DatFile f = file(42L, 20L, 99L, "/3/15/42/");
        when(shareMapper.selectDeptIdsByFileId(42L)).thenReturn(List.of());
        when(mapper.selectSharedFileIdsIn(any(), eq(10L))).thenReturn(List.of());
        assertFalse(service.canAccess(f, user(2L, 10L, false)));
    }
}
```

> 注：`canAccess`/`canWrite` 当前是 private，测试需把它们改为 package-private（去掉 `private`）或用反射。本计划采用**改为 package-private**（`canAccess`/`canWrite` 去掉修饰符的 `private`），这样同包测试可直连。

- [ ] **Step 3: 运行测试，确认失败**

Run: `mvn -pl erp-data -am test -Dtest=DatFilePermissionTest -q`
Expected: 编译失败（`canCreate` 未实现、`canAccess`/`canWrite` 仍 private）

- [ ] **Step 4: 实现 canCreate / 放宽 canWrite / 放宽 canAccess**

在 `DatFileServiceImpl.java` 中，把 `canAccess` 和 `canWrite` 的 `private` 修饰符去掉（改 package-private）。替换 `canWrite` 方法体为：

```java
    /** 检查用户是否有写入权限：本部门 OR 文件/祖先共享给本部门 OR 自己创建。 */
    boolean canWrite(DatFile file, LoginUser user) {
        if (file == null || user == null) return false;
        if (user.getRoles() != null && user.getRoles().contains("ROLE_ADMIN")) return true;
        if (user.getId() != null && user.getId().equals(file.getCreatedBy())) return true;
        if (file.getDeptId() != null && file.getDeptId().equals(user.getDepartmentId())) return true;
        if (user.getDepartmentId() != null && isSharedToMe(file, user.getDepartmentId())) return true;
        return false;
    }

    @Override
    public boolean canCreate(Long targetDeptId, LoginUser user) {
        if (user == null) return false;
        if (user.getRoles() != null && user.getRoles().contains("ROLE_ADMIN")) return true;
        return targetDeptId != null && targetDeptId.equals(user.getDepartmentId());
    }

    /** 文件本身或任一祖先被共享给本部门。 */
    private boolean isSharedToMe(DatFile file, Long myDeptId) {
        if (file == null || file.getId() == null) return false;
        if (shareMapper.selectDeptIdsByFileId(file.getId()).contains(myDeptId)) return true;
        List<Long> ancestorIds = parseAncestorIds(file.getPath());
        if (ancestorIds.isEmpty()) return false;
        return !mapper.selectSharedFileIdsIn(ancestorIds, myDeptId).isEmpty();
    }

    /** "/3/15/42/" → [3, 15, 42]。 */
    private List<Long> parseAncestorIds(String path) {
        if (path == null || path.isBlank()) return List.of();
        List<Long> ids = new java.util.ArrayList<>();
        for (String seg : path.split("/")) {
            if (seg.isBlank()) continue;
            try { ids.add(Long.valueOf(seg)); } catch (NumberFormatException ignore) {}
        }
        return ids;
    }
```

替换 `canAccess` 方法体为：

```java
    /** 检查用户是否有权限访问该文件/文件夹（含祖先共享继承）。 */
    boolean canAccess(DatFile file, LoginUser user) {
        if (file == null || user == null) return false;
        if (user.getRoles() != null && user.getRoles().contains("ROLE_ADMIN")) return true;
        if (user.getId() != null && user.getId().equals(file.getCreatedBy())) return true;
        if (file.getDeptId() != null && user.getDepartmentId() != null) {
            List<Long> visibleDeptIds = getDeptAndDescendantIds(user.getDepartmentId());
            if (visibleDeptIds.contains(file.getDeptId())) return true;
        }
        if (user.getDepartmentId() != null && isSharedToMe(file, user.getDepartmentId())) return true;
        return false;
    }
```

> 被锁文件「可见但拒读」由 `WebDavController` 在 GET 时查 LockStore 处理，canAccess 只管「条目是否可见」。

- [ ] **Step 5: 实现 writeContent**

在 `DatFileServiceImpl` 末尾（`download` 方法后）加：

```java
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void writeContent(Long fileId, java.io.InputStream in, LoginUser user) {
        DatFile f = mapper.selectById(fileId);
        if (f == null || f.getDeleted() == 1) {
            throw new BusinessException(R.CODE_NOT_FOUND, "文件不存在");
        }
        if (f.getIsDirectory() == 1) {
            throw new BusinessException(R.CODE_PARAM_INVALID, "不能写入文件夹");
        }
        if (!canWrite(f, user)) {
            throw new BusinessException(R.CODE_FORBIDDEN, "无权修改该文件");
        }
        java.nio.file.Path target = java.nio.file.Path.of(f.getStoragePath());
        try {
            java.nio.file.Files.createDirectories(target.getParent());
            long size;
            try (java.io.OutputStream out = java.nio.file.Files.newOutputStream(target,
                    java.nio.file.StandardOpenOption.CREATE, java.nio.file.StandardOpenOption.TRUNCATE_EXISTING)) {
                size = in.transferTo(out);
            }
            f.setFileSize(size);
            f.setUpdatedBy(user.getId());
            mapper.updateById(f);
        } catch (java.io.IOException ex) {
            throw new IllegalStateException("文件保存失败", ex);
        }
    }
```

- [ ] **Step 6: 维护 path — 改 createFolder**

`createFolder` 原方法取 `parent` 的逻辑改为 parentId==null 时 parent=null。把 `mapper.insert(f); return f.getId();` 段替换为：

```java
        DatFile f = new DatFile();
        f.setParentId(parentId);
        f.setIsDirectory(1);
        f.setName(name);
        f.setDisplayName(name);
        f.setDeptId(parent == null ? user.getDepartmentId() : parent.getDeptId());
        f.setCreatedBy(user.getId());
        mapper.insert(f);
        String parentPath = parent == null ? "/" : parent.getPath();
        f.setPath(parentPath + f.getId() + "/");
        mapper.updateById(f);
        return f.getId();
```

并把方法开头 `if (parentId != null) { DatFile parent = ... }` 改为方法级 `DatFile parent = parentId == null ? null : mapper.selectById(parentId);`（保留 not found 与 canWrite 校验）。

- [ ] **Step 7: 维护 path — 改 uploadFile**

同样把方法开头 parent 取值改为方法级变量，在 `mapper.insert(f);` 之后、`saveShareDepts` 之前加：

```java
        String parentPath = parent == null ? "/" : parent.getPath();
        f.setPath(parentPath + f.getId() + "/");
        mapper.updateById(f);
```

- [ ] **Step 8: 维护 path — 改 move**

在 `move` 的 `f.setParentId(targetParentId);` 之后、`mapper.updateById(f);` 之前加子树前缀替换：

```java
        String oldPrefix = f.getPath();
        String newPrefix = (target == null ? "/" : target.getPath()) + f.getId() + "/";
        f.setPath(newPrefix);
        mapper.updateById(f);
        if (f.getIsDirectory() != null && f.getIsDirectory() == 1) {
            mapper.updatePathPrefix(oldPrefix, newPrefix);
        }
```

（`target` 为目标父 DatFile，parentId==null 时为 null；原方法已有 `target` 变量，确认其作用域覆盖此处。）

- [ ] **Step 9: 运行测试，确认通过**

Run: `mvn -pl erp-data -am test -Dtest=DatFilePermissionTest -q`
Expected: 全部 PASS

- [ ] **Step 10: Commit**

```bash
git add erp-data/src/main/java/com/erp/data/service/DatFileService.java \
        erp-data/src/main/java/com/erp/data/service/impl/DatFileServiceImpl.java \
        erp-data/src/test/java/com/erp/data/service/impl/DatFilePermissionTest.java
git commit -m "feat(data): 权限放宽共享继承，加 canCreate/writeContent，维护 path"
```

---

## Task 4: WebDavLockStore — 内存独占锁

**Files:**
- Create: `erp-data/src/main/java/com/erp/data/webdav/WebDavLockInfo.java`
- Create: `erp-data/src/main/java/com/erp/data/webdav/WebDavLockStore.java`
- Test: `erp-data/src/test/java/com/erp/data/webdav/WebDavLockStoreTest.java`

**Interfaces:**
- Produces:
  - `WebDavLockInfo`（`fileId`/`ownerUserId`/`token`/`expireAt`）
  - `WebDavLockStore.tryLock(Long fileId, Long ownerUserId, long ttlSeconds)` → `String token`（已被他人持锁返回 null）
  - `unlock(Long fileId, String token)` → `boolean`
  - `isLockedByOther(Long fileId, Long userId)` → `boolean`
  - `assertLockHeld(Long fileId, String token)` → 锁住但 token 不符抛 `BusinessException(423)`

- [ ] **Step 1: 在 R.java 加 CODE_LOCKED（若不存在）**

检查 `erp-common/src/main/java/com/erp/common/model/R.java`，若无 423 常量，加：

```java
    /** WebDAV Locked（423）。 */
    public static final int CODE_LOCKED = 423;
```

- [ ] **Step 2: 写失败测试**

```java
package com.erp.data.webdav;

import com.erp.common.exception.BusinessException;
import com.erp.common.model.R;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class WebDavLockStoreTest {

    private final WebDavLockStore store = new WebDavLockStore();

    @Test
    void tryLock_success_returnsToken() {
        assertNotNull(store.tryLock(1L, 100L, 60));
    }

    @Test
    void tryLock_alreadyLockedByOther_returnsNull() {
        store.tryLock(1L, 100L, 60);
        assertNull(store.tryLock(1L, 200L, 60));
    }

    @Test
    void tryLock_sameOwner_relock_returnsToken() {
        store.tryLock(1L, 100L, 60);
        assertNotNull(store.tryLock(1L, 100L, 60));
    }

    @Test
    void unlock_correctToken_succeeds() {
        String token = store.tryLock(1L, 100L, 60);
        assertTrue(store.unlock(1L, token));
        assertNotNull(store.tryLock(1L, 200L, 60));
    }

    @Test
    void unlock_wrongToken_returnsFalse() {
        store.tryLock(1L, 100L, 60);
        assertFalse(store.unlock(1L, "wrong"));
    }

    @Test
    void isLockedByOther_otherHolds_returnsTrue() {
        store.tryLock(1L, 100L, 60);
        assertTrue(store.isLockedByOther(1L, 200L));
    }

    @Test
    void isLockedByOther_selfHolds_returnsFalse() {
        store.tryLock(1L, 100L, 60);
        assertFalse(store.isLockedByOther(1L, 100L));
    }

    @Test
    void assertLockHeld_lockedWrongToken_throws423() {
        store.tryLock(1L, 100L, 60);
        BusinessException ex = assertThrows(BusinessException.class,
                () -> store.assertLockHeld(1L, "wrong"));
        assertEquals(R.CODE_LOCKED, ex.getCode());
    }

    @Test
    void assertLockHeld_notLocked_passes() {
        assertDoesNotThrow(() -> store.assertLockHeld(1L, null));
    }

    @Test
    void tryLock_expired_reclaimed() throws InterruptedException {
        store.tryLock(1L, 100L, 1);
        Thread.sleep(1100);
        assertNotNull(store.tryLock(1L, 200L, 60));
    }
}
```

- [ ] **Step 3: 运行测试，确认失败**

Run: `mvn -pl erp-data -am test -Dtest=WebDavLockStoreTest -q`
Expected: 编译失败（类不存在）

- [ ] **Step 4: 实现 WebDavLockInfo**

```java
package com.erp.data.webdav;

import lombok.AllArgsConstructor;
import lombok.Data;

/** WebDAV 锁信息（独占）。 */
@Data
@AllArgsConstructor
public class WebDavLockInfo {
    private Long fileId;
    private Long ownerUserId;
    private String token;
    private long expireAt; // epoch millis
}
```

- [ ] **Step 5: 实现 WebDavLockStore**

```java
package com.erp.data.webdav;

import com.erp.common.exception.BusinessException;
import com.erp.common.model.R;
import org.springframework.stereotype.Component;

import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/** 内存独占锁存储（单机；多机后续换 Redis）。 */
@Component
public class WebDavLockStore {

    private final ConcurrentHashMap<Long, WebDavLockInfo> locks = new ConcurrentHashMap<>();

    public synchronized String tryLock(Long fileId, Long ownerUserId, long ttlSeconds) {
        purgeExpired(fileId);
        WebDavLockInfo existing = locks.get(fileId);
        if (existing != null && !existing.getOwnerUserId().equals(ownerUserId)) {
            return null;
        }
        String token = "opaquelocktoken:" + UUID.randomUUID();
        long expireAt = System.currentTimeMillis() + ttlSeconds * 1000L;
        locks.put(fileId, new WebDavLockInfo(fileId, ownerUserId, token, expireAt));
        return token;
    }

    public synchronized boolean unlock(Long fileId, String token) {
        WebDavLockInfo info = locks.get(fileId);
        if (info == null) return false;
        if (token == null || !token.equals(info.getToken())) return false;
        locks.remove(fileId);
        return true;
    }

    public synchronized boolean isLockedByOther(Long fileId, Long userId) {
        purgeExpired(fileId);
        WebDavLockInfo info = locks.get(fileId);
        return info != null && !info.getOwnerUserId().equals(userId);
    }

    public synchronized void assertLockHeld(Long fileId, String token) {
        purgeExpired(fileId);
        WebDavLockInfo info = locks.get(fileId);
        if (info == null) return;
        if (token == null || !token.equals(info.getToken())) {
            throw new BusinessException(R.CODE_LOCKED, "文件已被他人锁定");
        }
    }

    private void purgeExpired(Long fileId) {
        WebDavLockInfo info = locks.get(fileId);
        if (info != null && System.currentTimeMillis() > info.getExpireAt()) {
            locks.remove(fileId);
        }
    }
}
```

- [ ] **Step 6: 运行测试，确认通过**

Run: `mvn -pl erp-data -am test -Dtest=WebDavLockStoreTest -q`
Expected: 全部 PASS

- [ ] **Step 7: Commit**

```bash
git add erp-common/src/main/java/com/erp/common/model/R.java \
        erp-data/src/main/java/com/erp/data/webdav/WebDavLockInfo.java \
        erp-data/src/main/java/com/erp/data/webdav/WebDavLockStore.java \
        erp-data/src/test/java/com/erp/data/webdav/WebDavLockStoreTest.java
git commit -m "feat(webdav): 内存独占锁 WebDavLockStore"
```

---

## Task 5: WebDavPropFindXmlBuilder — multistatus XML 生成

**Files:**
- Create: `erp-data/src/main/java/com/erp/data/webdav/WebDavPropFindXmlBuilder.java`
- Test: `erp-data/src/test/java/com/erp/data/webdav/WebDavPropFindXmlBuilderTest.java`

**Interfaces:**
- Produces: `String build(String basePath, List<DatFile> children, List<VirtualDept> depts)` → multistatus XML。`basePath` 当前目录 href；children 真实子项；depts 根目录虚拟部门（仅根用）。`VirtualDept` 为内部 record。

- [ ] **Step 1: 写失败测试**

```java
package com.erp.data.webdav;

import com.erp.data.entity.DatFile;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class WebDavPropFindXmlBuilderTest {

    private final WebDavPropFindXmlBuilder builder = new WebDavPropFindXmlBuilder();

    private DatFile file(Long id, String name, boolean dir, long size) {
        DatFile f = new DatFile();
        f.setId(id); f.setName(name); f.setDisplayName(name);
        f.setIsDirectory(dir ? 1 : 0); f.setFileSize(size);
        f.setExtension(".xlsx"); f.setMimeType("application/vnd.openxmlformats");
        return f;
    }

    @Test
    void build_hasMultistatusRoot() {
        String xml = builder.build("/webdav/sales/", List.of(file(1L, "a.xlsx", false, 100)), List.of());
        assertTrue(xml.contains("<D:multistatus"));
        assertTrue(xml.contains("</D:multistatus>"));
    }

    @Test
    void build_fileHasHrefAndProps() {
        String xml = builder.build("/webdav/sales/", List.of(file(1L, "a.xlsx", false, 100)), List.of());
        assertTrue(xml.contains("<D:href>/webdav/sales/a.xlsx</D:href>"));
        assertTrue(xml.contains("100</D:getcontentlength>"));
        assertTrue(xml.contains("<D:iscollection>0</D:iscollection>"));
    }

    @Test
    void build_directoryIsCollection1() {
        String xml = builder.build("/webdav/sales/", List.of(file(2L, "sub", true, 0)), List.of());
        assertTrue(xml.contains("/webdav/sales/sub/</D:href>"));
        assertTrue(xml.contains("<D:iscollection>1</D:iscollection>"));
    }

    @Test
    void build_chineseNameEncodedInHref_displaynameRaw() {
        String xml = builder.build("/webdav/sales/", List.of(file(3L, "数据.xlsx", false, 50)), List.of());
        assertFalse(xml.contains("/webdav/sales/数据.xlsx</D:href>"));
        assertTrue(xml.contains("<D:displayname>数据.xlsx</D:displayname>"));
    }

    @Test
    void build_rootListsVirtualDepts() {
        var dept = new WebDavPropFindXmlBuilder.VirtualDept(10L, "销售部");
        String xml = builder.build("/webdav/", List.of(), List.of(dept));
        assertTrue(xml.contains("/webdav/%E9%94%80%E5%94%AE%E9%83%A8/</D:href>"));
        assertTrue(xml.contains("<D:displayname>销售部</D:displayname>"));
        assertTrue(xml.contains("<D:iscollection>1</D:iscollection>"));
    }
}
```

- [ ] **Step 2: 运行测试，确认失败**

Run: `mvn -pl erp-data -am test -Dtest=WebDavPropFindXmlBuilderTest -q`
Expected: 编译失败

- [ ] **Step 3: 实现 WebDavPropFindXmlBuilder**

```java
package com.erp.data.webdav;

import com.erp.data.entity.DatFile;
import org.springframework.stereotype.Component;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;

/** 生成 WebDAV PROPFIND multistatus XML（Windows 资源管理器靠此显示文件列表）。 */
@Component
public class WebDavPropFindXmlBuilder {

    public record VirtualDept(Long deptId, String name) {}

    public String build(String basePath, List<DatFile> children, List<VirtualDept> depts) {
        StringBuilder sb = new StringBuilder();
        sb.append("<?xml version=\"1.0\" encoding=\"utf-8\"?>");
        sb.append("<D:multistatus xmlns:D=\"DAV:\">");
        appendResponse(sb, basePath, ".", 0, true);
        for (VirtualDept d : depts) {
            appendResponse(sb, basePath + encode(d.name()) + "/", d.name(), 0, true);
        }
        for (DatFile f : children) {
            String name = f.getDisplayName() != null ? f.getDisplayName() : f.getName();
            boolean dir = f.getIsDirectory() != null && f.getIsDirectory() == 1;
            String href = basePath + encode(name) + (dir ? "/" : "");
            appendResponse(sb, href, name, f.getFileSize() == null ? 0 : f.getFileSize(), dir);
        }
        sb.append("</D:multistatus>");
        return sb.toString();
    }

    private void appendResponse(StringBuilder sb, String href, String displayName, long size, boolean isCollection) {
        sb.append("<D:response>");
        sb.append("<D:href>").append(escapeXml(href)).append("</D:href>");
        sb.append("<D:propstat><D:prop>");
        sb.append("<D:displayname>").append(escapeXml(displayName)).append("</D:displayname>");
        sb.append("<D:iscollection>").append(isCollection ? 1 : 0).append("</D:iscollection>");
        if (!isCollection) {
            sb.append("<D:getcontentlength>").append(size).append("</D:getcontentlength>");
        }
        sb.append("<D:resourcetype>").append(isCollection ? "<D:collection/>" : "").append("</D:resourcetype>");
        sb.append("</D:prop><D:status>HTTP/1.1 200 OK</D:status></D:propstat>");
        sb.append("</D:response>");
    }

    private String encode(String s) {
        return URLEncoder.encode(s, StandardCharsets.UTF_8).replace("+", "%20");
    }

    private String escapeXml(String s) {
        if (s == null) return "";
        return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;")
                .replace("\"", "&quot;").replace("'", "&apos;");
    }
}
```

- [ ] **Step 4: 运行测试，确认通过**

Run: `mvn -pl erp-data -am test -Dtest=WebDavPropFindXmlBuilderTest -q`
Expected: 全部 PASS

- [ ] **Step 5: Commit**

```bash
git add erp-data/src/main/java/com/erp/data/webdav/WebDavPropFindXmlBuilder.java \
        erp-data/src/test/java/com/erp/data/webdav/WebDavPropFindXmlBuilderTest.java
git commit -m "feat(webdav): PROPFIND multistatus XML 生成器"
```

---

## Task 6: WebDavPathResolver — 路径 ↔ DatFile 映射

**Files:**
- Create: `erp-data/src/main/java/com/erp/data/webdav/ResolvedPath.java`
- Create: `erp-data/src/main/java/com/erp/data/webdav/WebDavPathResolver.java`
- Test: `erp-data/src/test/java/com/erp/data/webdav/WebDavPathResolverTest.java`

**Interfaces:**
- Consumes: `DatFileMapper`（selectByParentId/selectById/selectRootFilesByDeptId）、`sys_department` 查询（经 JdbcTemplate）
- Produces: `WebDavPathResolver.resolve(String webdavPath, LoginUser user)` → `ResolvedPath`
  - `ResolvedPath.Type`: ROOT / DEPT_ROOT / FOLDER / FILE / NOT_FOUND
  - 字段：`type`、`deptId`（DEPT_ROOT 及以下）、`datFile`（FOLDER/FILE 时非 null）、`name`（最后一段）

- [ ] **Step 1: 实现 ResolvedPath**

```java
package com.erp.data.webdav;

import com.erp.data.entity.DatFile;
import lombok.Data;

/** 路径解析结果。 */
@Data
public class ResolvedPath {
    public enum Type { ROOT, DEPT_ROOT, FOLDER, FILE, NOT_FOUND }

    private Type type;
    private Long deptId;          // DEPT_ROOT/FOLDER/FILE 时为所在部门
    private DatFile datFile;      // FOLDER/FILE 时非 null
    private String basePath;      // 当前路径的 href（用于 PROPFIND basePath）

    public static ResolvedPath root(String basePath) {
        ResolvedPath r = new ResolvedPath();
        r.type = Type.ROOT; r.basePath = basePath; return r;
    }
    public static ResolvedPath deptRoot(Long deptId, String basePath) {
        ResolvedPath r = new ResolvedPath();
        r.type = Type.DEPT_ROOT; r.deptId = deptId; r.basePath = basePath; return r;
    }
    public static ResolvedPath of(DatFile f, String basePath) {
        ResolvedPath r = new ResolvedPath();
        r.type = f.getIsDirectory() != null && f.getIsDirectory() == 1 ? Type.FOLDER : Type.FILE;
        r.datFile = f; r.deptId = f.getDeptId(); r.basePath = basePath; return r;
    }
    public static ResolvedPath notFound(String basePath) {
        ResolvedPath r = new ResolvedPath();
        r.type = Type.NOT_FOUND; r.basePath = basePath; return r;
    }
}
```

- [ ] **Step 2: 写失败测试**

```java
package com.erp.data.webdav;

import com.erp.data.entity.DatFile;
import com.erp.data.mapper.DatFileMapper;
import com.erp.security.user.LoginUser;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class WebDavPathResolverTest {

    @Mock DatFileMapper mapper;
    @Mock JdbcTemplate jdbcTemplate;
    @InjectMocks WebDavPathResolver resolver;

    private LoginUser user(Long id, Long deptId) {
        return LoginUser.builder().id(id).username("u").departmentId(deptId).build();
    }

    private DatFile dir(Long id, String name, Long deptId, Long parent) {
        DatFile f = new DatFile();
        f.setId(id); f.setName(name); f.setIsDirectory(1); f.setDeptId(deptId); f.setParentId(parent); f.setDeleted(0);
        return f;
    }
    private DatFile file(Long id, String name, Long deptId, Long parent) {
        DatFile f = new DatFile();
        f.setId(id); f.setName(name); f.setIsDirectory(0); f.setDeptId(deptId); f.setParentId(parent); f.setDeleted(0);
        return f;
    }

    @Test
    void resolve_root_returnsROOT() {
        assertEquals(ResolvedPath.Type.ROOT, resolver.resolve("/webdav/", user(1L, 10L)).getType());
        assertEquals(ResolvedPath.Type.ROOT, resolver.resolve("/webdav", user(1L, 10L)).getType());
    }

    @Test
    void resolve_deptRoot_returnsDEPT_ROOT() {
        when(jdbcTemplate.queryForList(any(String.class), eq("销售部")))
                .thenReturn(List.of(java.util.Map.of("id", 20L)));
        ResolvedPath r = resolver.resolve("/webdav/销售部/", user(1L, 10L));
        assertEquals(ResolvedPath.Type.DEPT_ROOT, r.getType());
        assertEquals(20L, r.getDeptId());
    }

    @Test
    void resolve_unknownDept_returnsNOT_FOUND() {
        when(jdbcTemplate.queryForList(any(String.class), eq("不存在")))
                .thenReturn(List.of());
        ResolvedPath r = resolver.resolve("/webdav/不存在/", user(1L, 10L));
        assertEquals(ResolvedPath.Type.NOT_FOUND, r.getType());
    }

    @Test
    void resolve_folderInDept_returnsFOLDER() {
        when(jdbcTemplate.queryForList(any(String.class), eq("销售部")))
                .thenReturn(List.of(java.util.Map.of("id", 20L)));
        when(mapper.selectRootFilesByDeptId(20L)).thenReturn(List.of(dir(30L, "报表", 20L, null)));
        ResolvedPath r = resolver.resolve("/webdav/销售部/报表", user(1L, 10L));
        assertEquals(ResolvedPath.Type.FOLDER, r.getType());
        assertEquals(30L, r.getDatFile().getId());
    }

    @Test
    void resolve_fileInFolder_returnsFILE() {
        when(jdbcTemplate.queryForList(any(String.class), eq("销售部")))
                .thenReturn(List.of(java.util.Map.of("id", 20L)));
        when(mapper.selectRootFilesByDeptId(20L)).thenReturn(List.of(dir(30L, "报表", 20L, null)));
        when(mapper.selectByParentId(30L)).thenReturn(List.of(file(40L, "数据.xlsx", 20L, 30L)));
        ResolvedPath r = resolver.resolve("/webdav/销售部/报表/数据.xlsx", user(1L, 10L));
        assertEquals(ResolvedPath.Type.FILE, r.getType());
        assertEquals(40L, r.getDatFile().getId());
    }

    @Test
    void resolve_missingSegment_returnsNOT_FOUND() {
        when(jdbcTemplate.queryForList(any(String.class), eq("销售部")))
                .thenReturn(List.of(java.util.Map.of("id", 20L)));
        when(mapper.selectRootFilesByDeptId(20L)).thenReturn(List.of());
        ResolvedPath r = resolver.resolve("/webdav/销售部/不存在", user(1L, 10L));
        assertEquals(ResolvedPath.Type.NOT_FOUND, r.getType());
    }

    @Test
    void resolve_urlDecodedChinese() {
        when(jdbcTemplate.queryForList(any(String.class), eq("销售部")))
                .thenReturn(List.of(java.util.Map.of("id", 20L)));
        ResolvedPath r = resolver.resolve("/webdav/%E9%94%80%E5%94%AE%E9%83%A8/", user(1L, 10L));
        assertEquals(ResolvedPath.Type.DEPT_ROOT, r.getType());
    }
}
```

- [ ] **Step 3: 运行测试，确认失败**

Run: `mvn -pl erp-data -am test -Dtest=WebDavPathResolverTest -q`
Expected: 编译失败

- [ ] **Step 4: 实现 WebDavPathResolver**

```java
package com.erp.data.webdav;

import com.erp.data.entity.DatFile;
import com.erp.data.mapper.DatFileMapper;
import com.erp.security.user.LoginUser;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

/** /webdav/销售部/报表/x.xlsx → ResolvedPath。 */
@Component
public class WebDavPathResolver {

    private static final String PREFIX = "/webdav";

    private final DatFileMapper mapper;
    private final JdbcTemplate jdbcTemplate;

    public WebDavPathResolver(DatFileMapper mapper, JdbcTemplate jdbcTemplate) {
        this.mapper = mapper;
        this.jdbcTemplate = jdbcTemplate;
    }

    public ResolvedPath resolve(String rawPath, LoginUser user) {
        String basePath = normalizeBase(rawPath);
        // 去掉 /webdav 前缀
        String p = rawPath == null ? "" : rawPath;
        // 定位 /webdav 之后的内容
        int idx = p.indexOf(PREFIX);
        if (idx >= 0) p = p.substring(idx + PREFIX.length());
        p = URLDecoder.decode(p, StandardCharsets.UTF_8);
        // 分段
        String[] segs = p.split("/");
        java.util.List<String> parts = new java.util.ArrayList<>();
        for (String s : segs) if (!s.isEmpty()) parts.add(s);

        if (parts.isEmpty()) {
            return ResolvedPath.root(basePath);
        }
        // 第一段 = 部门名
        String deptName = parts.get(0);
        Long deptId = lookupDeptId(deptName);
        if (deptId == null) return ResolvedPath.notFound(basePath);

        if (parts.size() == 1) {
            return ResolvedPath.deptRoot(deptId, basePath);
        }
        // 从部门根逐级下钻
        List<DatFile> current = mapper.selectRootFilesByDeptId(deptId);
        DatFile matched = null;
        for (int i = 1; i < parts.size(); i++) {
            String seg = parts.get(i);
            matched = null;
            for (DatFile f : current) {
                String name = f.getDisplayName() != null ? f.getDisplayName() : f.getName();
                if (seg.equals(name)) { matched = f; break; }
            }
            if (matched == null) return ResolvedPath.notFound(basePath);
            if (i < parts.size() - 1) {
                if (matched.getIsDirectory() == null || matched.getIsDirectory() != 1) {
                    return ResolvedPath.notFound(basePath);
                }
                current = mapper.selectByParentId(matched.getId());
            }
        }
        return ResolvedPath.of(matched, basePath);
    }

    /** /webdav/销售部/报表/ → /webdav/%E9%94%80%E5%94%AE%E9%83%A8/%E6%8A%A5%E8%A1%A8/ (编码留给 XmlBuilder，此处保留解码原样作 basePath 前缀) */
    private String normalizeBase(String rawPath) {
        String p = rawPath == null ? "/webdav/" : rawPath;
        if (!p.endsWith("/")) p = p + "/";
        return p;
    }

    private Long lookupDeptId(String deptName) {
        List<Map<String, Object>> rows = jdbcTemplate.queryForList(
                "SELECT id FROM sys_department WHERE dept_name = ? AND deleted = 0", deptName);
        if (rows.isEmpty()) return null;
        return ((Number) rows.get(0).get("id")).longValue();
    }
}
```

> 注：`basePath` 在 PROPFIND 中传给 XmlBuilder，XmlBuilder 会负责 href 编码。resolve 返回的 basePath 用原始路径（末尾补 /），XmlBuilder 内部对拼接到 href 的文件名段做编码。当前 XmlBuilder 的 `build(basePath, ...)` 假定 basePath 已是编码形式——故 Controller 调用时应传编码后的 basePath。此处保留解码 basePath 供 controller 灵活处理；Task 9 Controller 会用 `URLEncoder` 重新编码 basePath 段。**为避免歧义**：Controller 传给 XmlBuilder 的 basePath 一律用编码段拼接（见 Task 9 Step 说明）。

- [ ] **Step 5: 运行测试，确认通过**

Run: `mvn -pl erp-data -am test -Dtest=WebDavPathResolverTest -q`
Expected: 全部 PASS

- [ ] **Step 6: Commit**

```bash
git add erp-data/src/main/java/com/erp/data/webdav/ResolvedPath.java \
        erp-data/src/main/java/com/erp/data/webdav/WebDavPathResolver.java \
        erp-data/src/test/java/com/erp/data/webdav/WebDavPathResolverTest.java
git commit -m "feat(webdav): 路径解析器 WebDavPathResolver"
```

---

## Task 7: WebDavAuthFilter + 安全配置

**Files:**
- Create: `erp-data/src/main/java/com/erp/data/webdav/WebDavAuthFilter.java`
- Create: `erp-data/src/main/java/com/erp/data/webdav/WebDavSecurityConfig.java`
- Modify: `erp-security/src/main/java/com/erp/security/config/SecurityConfig.java`

**Interfaces:**
- Consumes: `UserDetailsLoader.loadByUsername`、`PasswordEncoder`
- Produces: `/webdav/**` 请求经 Basic Auth 校验后，SecurityContext 写入 `UsernamePasswordAuthenticationToken(username)`，后续 `@CurrentUser LoginUser` 可解析

**设计要点**：不生成临时 JWT。WebDavAuthFilter 校验 Basic Auth 账号密码 → 写 SecurityContext（principal=username 字符串）→ 现有 `JwtAuthenticationFilter` 因请求头是 `Basic` 而忽略（不覆盖）→ `CurrentUserArgumentResolver` 从 principal 取 username 调 `loadByUsername` 重加载 LoginUser。

- [ ] **Step 1: SecurityConfig 放行 /webdav/****

在 `SecurityConfig.java` 的 `PUBLIC_PATHS` 数组末尾（`"/swagger-ui.html"` 后）加：

```java
            // WebDAV：走 WebDavAuthFilter 的 Basic Auth，不进 JWT 白名单裁决
            "/webdav/**"
```

> 因 `anyRequest().authenticated()` 会拦未认证请求，`/webdav/**` 加白名单后由 `WebDavAuthFilter` 负责认证；filter 不通过时由 filter 自身返回 401。

- [ ] **Step 2: 实现 WebDavAuthFilter**

```java
package com.erp.data.webdav;

import com.erp.security.user.LoginUser;
import com.erp.security.user.UserDetailsLoader;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/** Basic Auth → SecurityContext（WebDAV 专用）。 */
@Component
public class WebDavAuthFilter extends OncePerRequestFilter {

    private final UserDetailsLoader userDetailsLoader;
    private final PasswordEncoder passwordEncoder;

    public WebDavAuthFilter(UserDetailsLoader userDetailsLoader, PasswordEncoder passwordEncoder) {
        this.userDetailsLoader = userDetailsLoader;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request,
                                    @NonNull HttpServletResponse response,
                                    @NonNull FilterChain chain) throws ServletException, IOException {
        String header = request.getHeader("Authorization");
        if (StringUtils.hasText(header) && header.startsWith("Basic ")) {
            if (tryAuthenticate(header.substring(6).trim())) {
                chain.doFilter(request, response);
                return;
            }
            // 凭证错误 → 401
            challenge(response);
            return;
        }
        // 无 Basic 头 → 401（WebDAV 强制要求认证）
        challenge(response);
    }

    private boolean tryAuthenticate(String b64) {
        try {
            String decoded = new String(Base64.getDecoder().decode(b64), StandardCharsets.UTF_8);
            int colon = decoded.indexOf(':');
            if (colon <= 0) return false;
            String username = decoded.substring(0, colon);
            String password = decoded.substring(colon + 1);
            LoginUser user = userDetailsLoader.loadByUsername(username);
            if (user == null) return false;
            if (!passwordEncoder.matches(password, user.getEncryptedPassword())) return false;
            List<SimpleGrantedAuthority> authorities = Stream.concat(
                    user.getRoles() == null ? Stream.<String>empty() : user.getRoles().stream(),
                    user.getPermissions() == null ? Stream.<String>empty() : user.getPermissions().stream())
                    .distinct().map(SimpleGrantedAuthority::new).collect(Collectors.toList());
            UsernamePasswordAuthenticationToken auth =
                    new UsernamePasswordAuthenticationToken(username, null, authorities);
            SecurityContextHolder.getContext().setAuthentication(auth);
            return true;
        } catch (UsernameNotFoundException ex) {
            return false;
        } catch (IllegalArgumentException ex) {
            return false; // Base64 解码失败
        }
    }

    private void challenge(HttpServletResponse response) throws IOException {
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setHeader("WWW-Authenticate", "Basic realm=\"ERP WebDAV\"");
        response.setContentType("text/plain; charset=utf-8");
        response.getWriter().write("401 Unauthorized");
    }

    @Override
    protected boolean shouldNotFilter(@NonNull HttpServletRequest request) {
        return !request.getRequestURI().startsWith("/webdav");
    }
}
```

- [ ] **Step 3: 实现 WebDavSecurityConfig — 注册 Filter 顺序**

```java
package com.erp.data.webdav;

import com.erp.security.filter.JwtAuthenticationFilter;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.boot.autoconfigure.security.SecurityProperties;
import org.springframework.core.annotation.Order;
import org.springframework.context.annotation.Bean;

/**
 * 注册 WebDavAuthFilter 在 JwtAuthenticationFilter 之前，
 * 使 /webdav/** 走 Basic Auth。
 */
@Configuration
public class WebDavSecurityConfig {

    @Bean
    @Order(SecurityProperties.DEFAULT_FILTER_ORDER - 1)
    public SecurityFilterChain webdavFilterChain(HttpSecurity http,
                                                 JwtAuthenticationFilter jwtFilter,
                                                 WebDavAuthFilter webDavAuthFilter) throws Exception {
        http
                .securityMatcher("/webdav/**")
                .csrf(org.springframework.security.config.Customizer.withDefaults())
                .csrf(AbstractHttpConfigurer -> {})
                .sessionManagement(s -> s.sessionCreationPolicy(org.springframework.security.config.http.SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(a -> a.anyRequest().permitAll())
                .addFilterBefore(webDavAuthFilter, JwtAuthenticationFilter.class)
                .addFilterAfter(jwtFilter, WebDavAuthFilter.class);
        return http.build();
    }
}
```

> 说明：用独立 `SecurityFilterChain`（`securityMatcher("/webdav/**")`）把 WebDavAuthFilter 插在 JwtAuthenticationFilter 前。该链 permitAll（认证由 filter 内 401 处理），不与主链冲突。注意 `csrf().disable()` 写法用 lambda。

**修正 Step 3 代码**（清理重复 csrf 调用，用规范写法）：

```java
package com.erp.data.webdav;

import com.erp.security.filter.JwtAuthenticationFilter;
import org.springframework.boot.autoconfigure.security.SecurityProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
public class WebDavSecurityConfig {

    @Bean
    @Order(SecurityProperties.DEFAULT_FILTER_ORDER - 1)
    public SecurityFilterChain webdavFilterChain(HttpSecurity http,
                                                 JwtAuthenticationFilter jwtFilter,
                                                 WebDavAuthFilter webDavAuthFilter) throws Exception {
        http
                .securityMatcher("/webdav/**")
                .csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(a -> a.anyRequest().permitAll())
                .addFilterBefore(webDavAuthFilter, JwtAuthenticationFilter.class);
        return http.build();
    }
}
```

> 用此修正版替换上一段。`/webdav/**` 不再需要进 `SecurityConfig.PUBLIC_PATHS`，因为本链 `securityMatcher` 已接管该路径，主链不再匹配它。**因此撤销 Step 1 对 PUBLIC_PATHS 的改动**（不加 `/webdav/**`）。

- [ ] **Step 4: 撤销 Step 1 的 PUBLIC_PATHS 改动**

回退 `SecurityConfig.java`，不加 `/webdav/**`（由 webdavFilterChain 的 securityMatcher 接管）。

- [ ] **Step 5: 编译通过**

Run: `mvn -pl erp-data -am compile -q`
Expected: BUILD SUCCESS

- [ ] **Step 6: 手动冒烟（鉴权 401）**

启动应用，执行：
```bash
curl -i http://localhost:8080/webdav/
```
Expected: `HTTP/1.1 401`，含 `WWW-Authenticate: Basic realm="ERP WebDAV"`

```bash
curl -i -u admin:admin123 http://localhost:8080/webdav/
```
Expected: 非 401（404 或 207，取决于此时 Controller 是否已实现——本 Task 后 Controller 尚未实现，预期 404，因无 /webdav 映射）

- [ ] **Step 7: Commit**

```bash
git add erp-data/src/main/java/com/erp/data/webdav/WebDavAuthFilter.java \
        erp-data/src/main/java/com/erp/data/webdav/WebDavSecurityConfig.java
git commit -m "feat(webdav): Basic Auth 过滤器与安全配置"
```

---

## Task 8: WebDavErrors — 业务异常 → WebDAV 状态码/XML

**Files:**
- Create: `erp-data/src/main/java/com/erp/data/webdav/WebDavErrors.java`
- Test: `erp-data/src/test/java/com/erp/data/webdav/WebDavErrorsTest.java`

**Interfaces:**
- Produces:
  - `int statusCode(BusinessException)` → 401/403/404/409/423/400
  - `String errorXml(int statusCode)` → `<D:error>...<D:status>HTTP/1.1 NNN ...</D:status></D:error>`
  - `void write(HttpServletResponse, BusinessException)` → 设状态码 + 写 XML body

- [ ] **Step 1: 写失败测试**

```java
package com.erp.data.webdav;

import com.erp.common.exception.BusinessException;
import com.erp.common.model.R;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class WebDavErrorsTest {

    @Test
    void statusCode_forbidden() {
        assertEquals(403, WebDavErrors.statusCode(new BusinessException(R.CODE_FORBIDDEN, "x")));
    }

    @Test
    void statusCode_notFound() {
        assertEquals(404, WebDavErrors.statusCode(new BusinessException(R.CODE_NOT_FOUND, "x")));
    }

    @Test
    void statusCode_locked() {
        assertEquals(423, WebDavErrors.statusCode(new BusinessException(R.CODE_LOCKED, "x")));
    }

    @Test
    void statusCode_paramInvalid_mapsTo400() {
        assertEquals(400, WebDavErrors.statusCode(new BusinessException(R.CODE_PARAM_INVALID, "x")));
    }

    @Test
    void statusCode_unknown_defaults500() {
        assertEquals(500, WebDavErrors.statusCode(new BusinessException(9999, "x")));
    }

    @Test
    void errorXml_containsStatus() {
        String xml = WebDavErrors.errorXml(403);
        assertTrue(xml.contains("<D:error"));
        assertTrue(xml.contains("403 Forbidden"));
    }
}
```

- [ ] **Step 2: 运行测试，确认失败**

Run: `mvn -pl erp-data -am test -Dtest=WebDavErrorsTest -q`
Expected: 编译失败

- [ ] **Step 3: 实现 WebDavErrors**

```java
package com.erp.data.webdav;

import com.erp.common.exception.BusinessException;
import com.erp.common.model.R;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;

import java.io.IOException;

/** 业务异常 → WebDAV 状态码 + XML error body。 */
public final class WebDavErrors {

    private WebDavErrors() {}

    public static int statusCode(BusinessException ex) {
        int code = ex.getCode();
        if (code == R.CODE_UNAUTHORIZED) return HttpStatus.UNAUTHORIZED.value();
        if (code == R.CODE_FORBIDDEN) return HttpStatus.FORBIDDEN.value();
        if (code == R.CODE_NOT_FOUND) return HttpStatus.NOT_FOUND.value();
        if (code == R.CODE_LOCKED) return 423;
        if (code == R.CODE_PARAM_INVALID) return HttpStatus.BAD_REQUEST.value();
        return HttpStatus.INTERNAL_SERVER_ERROR.value();
    }

    public static String errorXml(int statusCode) {
        String reason = HttpStatus.resolve(statusCode) != null
                ? HttpStatus.resolve(statusCode).getReasonPhrase()
                : "Error";
        return "<?xml version=\"1.0\" encoding=\"utf-8\"?>" +
                "<D:error xmlns:D=\"DAV:\"><D:status>HTTP/1.1 " + statusCode + " " + reason + "</D:status></D:error>";
    }

    public static void write(HttpServletResponse response, BusinessException ex) throws IOException {
        int sc = statusCode(ex);
        response.setStatus(sc);
        response.setContentType("application/xml; charset=utf-8");
        response.getWriter().write(errorXml(sc));
    }
}
```

> 确认 `R` 中常量名：`CODE_UNAUTHORIZED`/`CODE_FORBIDDEN`/`CODE_NOT_FOUND`/`CODE_PARAM_INVALID`（已在 DatUploadServiceImpl 中使用过 `R.CODE_NOT_FOUND`/`R.CODE_FORBIDDEN`/`R.CODE_PARAM_INVALID`）。若 `CODE_UNAUTHORIZED` 不存在，用字面量 401。

- [ ] **Step 4: 运行测试，确认通过**

Run: `mvn -pl erp-data -am test -Dtest=WebDavErrorsTest -q`
Expected: 全部 PASS

- [ ] **Step 5: Commit**

```bash
git add erp-data/src/main/java/com/erp/data/webdav/WebDavErrors.java \
        erp-data/src/test/java/com/erp/data/webdav/WebDavErrorsTest.java
git commit -m "feat(webdav): 业务异常到 WebDAV 状态码映射"
```

---

## Task 9: WebDavController — 协议处理器（8 动词）

**Files:**
- Create: `erp-data/src/main/java/com/erp/data/webdav/WebDavController.java`

**Interfaces:**
- Consumes: `WebDavPathResolver`、`DatFileService`、`WebDavPropFindXmlBuilder`、`WebDavLockStore`、`@CurrentUser LoginUser`
- Produces: `/webdav/**` 的 OPTIONS/PROPFIND/GET/PUT/MKCOL/DELETE/MOVE/LOCK/UNLOCK 端点

- [ ] **Step 1: 实现 WebDavController**

```java
package com.erp.data.webdav;

import com.erp.common.exception.BusinessException;
import com.erp.data.entity.DatFile;
import com.erp.data.service.DatFileService;
import com.erp.security.annotation.CurrentUser;
import com.erp.security.user.LoginUser;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/webdav")
@RequiredArgsConstructor
public class WebDavController {

    private final WebDavPathResolver resolver;
    private final DatFileService fileService;
    private final WebDavPropFindXmlBuilder xmlBuilder;
    private final WebDavLockStore lockStore;

    // ========== OPTIONS ==========
    @RequestMapping(value = "/**", method = RequestMethod.OPTIONS)
    public void options(HttpServletResponse response) {
        response.setHeader("DAV", "1,2");
        response.setHeader("Allow",
                "OPTIONS, PROPFIND, GET, PUT, MKCOL, DELETE, MOVE, LOCK, UNLOCK");
        response.setHeader("MS-Author-Via", "DAV");
        response.setStatus(200);
    }

    // ========== PROPFIND ==========
    @RequestMapping(value = "/**", method = RequestMethod.PROPFIND)
    public void propfind(HttpServletRequest request, HttpServletResponse response,
                         @CurrentUser LoginUser user) throws IOException {
        try {
            ResolvedPath rp = resolver.resolve(request.getRequestURI(), user);
            List<DatFile> children;
            List<WebDavPropFindXmlBuilder.VirtualDept> depts = List.of();
            switch (rp.getType()) {
                case ROOT -> {
                    children = List.of();
                    depts = listVisibleDepts(user);
                }
                case DEPT_ROOT -> {
                    children = filterVisible(fileService, user,
                            ((com.erp.data.service.DatFileService) fileService).listFiles(
                                    deptRootQuery(rp.getDeptId()), user));
                }
                case FOLDER -> {
                    children = filterVisible(fileService, user,
                            fileService.listFiles(folderQuery(rp.getDatFile().getId()), user));
                }
                case FILE -> { children = List.of(rp.getDatFile()); }
                case NOT_FOUND -> { response.setStatus(404); return; }
                default -> { children = List.of(); }
            }
            String basePath = encodedBasePath(request.getRequestURI());
            response.setStatus(207);
            response.setContentType("application/xml; charset=utf-8");
            response.getWriter().write(xmlBuilder.build(basePath, children, depts));
        } catch (BusinessException ex) {
            WebDavErrors.write(response, ex);
        }
    }

    // ========== GET ==========
    @RequestMapping(value = "/**", method = RequestMethod.GET)
    public void get(HttpServletRequest request, HttpServletResponse response,
                    @CurrentUser LoginUser user) throws IOException {
        try {
            ResolvedPath rp = resolver.resolve(request.getRequestURI(), user);
            if (rp.getType() == ResolvedPath.Type.NOT_FOUND) { response.setStatus(404); return; }
            if (rp.getType() == ResolvedPath.Type.FILE) {
                if (lockStore.isLockedByOther(rp.getDatFile().getId(), user.getId())) {
                    WebDavErrors.write(response, new BusinessException(com.erp.common.model.R.CODE_LOCKED, "locked"));
                    return;
                }
                fileService.download(rp.getDatFile().getId(), response);
                return;
            }
            // 目录 GET → 重定向到 PROPFIND 行为（返回 207 列表）由客户端 PROPFIND 处理，此处返 405
            response.setStatus(405);
        } catch (BusinessException ex) {
            WebDavErrors.write(response, ex);
        }
    }

    // ========== PUT ==========
    @RequestMapping(value = "/**", method = RequestMethod.PUT)
    public void put(HttpServletRequest request, HttpServletResponse response,
                    @CurrentUser LoginUser user) throws IOException {
        try {
            ResolvedPath rp = resolver.resolve(request.getRequestURI(), user);
            String lockToken = lockTokenHeader(request);
            if (rp.getType() == ResolvedPath.Type.FILE) {
                // 覆盖已存在
                lockStore.assertLockHeld(rp.getDatFile().getId(), lockToken);
                fileService.writeContent(rp.getDatFile().getId(), request.getInputStream(), user);
                response.setStatus(204);
                return;
            }
            // 新文件：父必须是 FOLDER 或 DEPT_ROOT
            ResolvedPath parent = resolver.resolve(parentPath(request.getRequestURI()), user);
            Long parentDeptId = parentDeptId(parent);
            if (!fileService.canCreate(parentDeptId, user)) {
                WebDavErrors.write(response, new BusinessException(com.erp.common.model.R.CODE_FORBIDDEN, "无权在此目录上传"));
                return;
            }
            String fileName = lastSegment(request.getRequestURI());
            Long newId = fileService.uploadFile(
                    toMultipartPlaceholder(request, fileName), parentFolderId(parent), null,
                    parentDeptId, null, user);
            response.setStatus(201);
            response.setHeader("Location", request.getRequestURI());
        } catch (BusinessException ex) {
            WebDavErrors.write(response, ex);
        }
    }

    // ========== MKCOL ==========
    @RequestMapping(value = "/**", method = RequestMethod.MKCOL)
    public void mkcol(HttpServletRequest request, HttpServletResponse response,
                      @CurrentUser LoginUser user) throws IOException {
        try {
            ResolvedPath parent = resolver.resolve(parentPath(request.getRequestURI()), user);
            Long parentDeptId = parentDeptId(parent);
            if (!fileService.canCreate(parentDeptId, user)) {
                WebDavErrors.write(response, new BusinessException(com.erp.common.model.R.CODE_FORBIDDEN, "无权创建文件夹"));
                return;
            }
            fileService.createFolder(parentFolderId(parent), lastSegment(request.getRequestURI()), user);
            response.setStatus(201);
        } catch (BusinessException ex) {
            int sc = WebDavErrors.statusCode(ex);
            // 同名 → 409
            response.setStatus(sc == 400 ? 409 : sc);
            response.setContentType("application/xml; charset=utf-8");
            response.getWriter().write(WebDavErrors.errorXml(sc == 400 ? 409 : sc));
        }
    }

    // ========== DELETE ==========
    @RequestMapping(value = "/**", method = RequestMethod.DELETE)
    public void delete(HttpServletRequest request, HttpServletResponse response,
                       @CurrentUser LoginUser user) throws IOException {
        try {
            ResolvedPath rp = resolver.resolve(request.getRequestURI(), user);
            if (rp.getType() == ResolvedPath.Type.NOT_FOUND
                    || (rp.getDatFile() == null && rp.getType() != ResolvedPath.Type.FILE
                        && rp.getType() != ResolvedPath.Type.FOLDER)) {
                response.setStatus(404); return;
            }
            lockStore.assertLockHeld(rp.getDatFile().getId(), lockTokenHeader(request));
            fileService.delete(rp.getDatFile().getId(), user);
            response.setStatus(204);
        } catch (BusinessException ex) {
            WebDavErrors.write(response, ex);
        }
    }

    // ========== MOVE ==========
    @RequestMapping(value = "/**", method = RequestMethod.MOVE)
    public void move(HttpServletRequest request, HttpServletResponse response,
                     @CurrentUser LoginUser user) throws IOException {
        try {
            ResolvedPath src = resolver.resolve(request.getRequestURI(), user);
            if (src.getDatFile() == null) { response.setStatus(404); return; }
            String dest = request.getHeader("Destination");
            if (dest == null) { response.setStatus(400); return; }
            String destPath = stripHost(dest);
            ResolvedPath destParent = resolver.resolve(parentPath(destPath), user);
            Long destDeptId = parentDeptId(destParent);
            // 跨部门移动被拒：源与目标必须同部门
            if (!src.getDatFile().getDeptId().equals(destDeptId)) {
                WebDavErrors.write(response, new BusinessException(com.erp.common.model.R.CODE_FORBIDDEN, "不能跨部门移动"));
                return;
            }
            fileService.move(src.getDatFile().getId(), parentFolderId(destParent), user);
            response.setStatus(201);
        } catch (BusinessException ex) {
            WebDavErrors.write(response, ex);
        }
    }

    // ========== LOCK / UNLOCK ==========
    @RequestMapping(value = "/**", method = RequestMethod.LOCK)
    public void lock(HttpServletRequest request, HttpServletResponse response,
                     @CurrentUser LoginUser user) throws IOException {
        try {
            ResolvedPath rp = resolver.resolve(request.getRequestURI(), user);
            if (rp.getType() != ResolvedPath.Type.FILE && rp.getType() != ResolvedPath.Type.FOLDER) {
                response.setStatus(404); return;
            }
            if (!fileService.canWrite(rp.getDatFile(), user)) {
                WebDavErrors.write(response, new BusinessException(com.erp.common.model.R.CODE_FORBIDDEN, "无写权限不能加锁"));
                return;
            }
            String token = lockStore.tryLock(rp.getDatFile().getId(), user.getId(), 1800);
            if (token == null) {
                WebDavErrors.write(response, new BusinessException(com.erp.common.model.R.CODE_LOCKED, "已被他人锁定"));
                return;
            }
            response.setStatus(200);
            response.setContentType("application/xml; charset=utf-8");
            response.setHeader("Lock-Token", "<" + token + ">");
            response.getWriter().write(lockXml(token, rp.getDatFile(), request.getRequestURI()));
        } catch (BusinessException ex) {
            WebDavErrors.write(response, ex);
        }
    }

    @RequestMapping(value = "/**", method = RequestMethod.UNLOCK)
    public void unlock(HttpServletRequest request, HttpServletResponse response,
                       @CurrentUser LoginUser user) throws IOException {
        ResolvedPath rp = resolver.resolve(request.getRequestURI(), user);
        if (rp.getDatFile() == null) { response.setStatus(404); return; }
        String token = lockTokenHeader(request);
        if (lockStore.unlock(rp.getDatFile().getId(), token)) {
            response.setStatus(204);
        } else {
            response.setStatus(409);
        }
    }

    // ========== 辅助 ==========
    private String lockTokenHeader(HttpServletRequest req) {
        String t = req.getHeader("Lock-Token");
        if (t == null) return null;
        return t.replace("<", "").replace(">", "").trim();
    }

    private String encodedBasePath(String uri) {
        // 把每段编码，保留 /
        String after = uri.indexOf("/webdav") >= 0 ? uri.substring(uri.indexOf("/webdav")) : uri;
        StringBuilder sb = new StringBuilder();
        for (String seg : after.split("/")) {
            if (seg.isEmpty()) continue;
            sb.append("/").append(URLEncoder.encode(URLDecoder.decode(seg, StandardCharsets.UTF_8), StandardCharsets.UTF_8).replace("+", "%20"));
        }
        sb.append("/");
        return sb.length() == 0 ? "/webdav/" : sb.toString();
    }

    private String lastSegment(String uri) {
        String decoded = URLDecoder.decode(uri, StandardCharsets.UTF_8);
        String[] parts = decoded.split("/");
        return parts.length == 0 ? "" : parts[parts.length - 1];
    }

    private String parentPath(String uri) {
        String decoded = URLDecoder.decode(uri, StandardCharsets.UTF_8);
        int i = decoded.lastIndexOf('/');
        if (i <= 0) return "/webdav/";
        String parent = decoded.substring(0, i);
        if (parent.equals("/webdav") || parent.isEmpty()) return "/webdav/";
        return parent;
    }

    private Long parentFolderId(ResolvedPath parent) {
        if (parent.getType() == ResolvedPath.Type.FOLDER) return parent.getDatFile().getId();
        return null; // DEPT_ROOT → parentId=null
    }

    private Long parentDeptId(ResolvedPath parent) {
        if (parent.getType() == ResolvedPath.Type.DEPT_ROOT) return parent.getDeptId();
        if (parent.getDatFile() != null) return parent.getDatFile().getDeptId();
        return null;
    }

    private String stripHost(String url) {
        int idx = url.indexOf("/webdav");
        return idx >= 0 ? url.substring(idx) : url;
    }

    private String lockXml(String token, DatFile f, String href) {
        return "<?xml version=\"1.0\" encoding=\"utf-8\"?>" +
                "<D:prop xmlns:D=\"DAV:\"><D:lockdiscovery><D:activelock>" +
                "<D:locktype><D:write/></D:locktype>" +
                "<D:lockscope><D:exclusive/></D:lockscope>" +
                "<D:depth>0</D:depth>" +
                "<D:timeout>Second-1800</D:timeout>" +
                "<D:locktoken><D:href>" + token + "</D:href></D:locktoken>" +
                "</D:activelock></D:lockdiscovery></D:prop>";
    }

    /** 列出用户可见部门（根目录用）。 */
    private List<WebDavPropFindXmlBuilder.VirtualDept> listVisibleDepts(LoginUser user) {
        // 本部门 + 下级
        List<Map<String, Object>> rows = new ArrayList<>();
        boolean admin = user.getRoles() != null && user.getRoles().contains("ROLE_ADMIN");
        if (admin) {
            rows.addAll(jdbcTemplate().queryForList("SELECT id, dept_name FROM sys_department WHERE deleted = 0"));
        } else if (user.getDepartmentId() != null) {
            rows.addAll(jdbcTemplate().queryForList(
                    "SELECT id, dept_name FROM sys_department WHERE deleted = 0 AND " +
                    "(dept_path LIKE CONCAT((SELECT dept_path FROM sys_department WHERE id = ?), '%') " +
                    "OR id IN (SELECT DISTINCT f.dept_id FROM dat_file f JOIN dat_file_share s ON s.file_id = f.id WHERE s.dept_id = ?))",
                    user.getDepartmentId(), user.getDepartmentId()));
        }
        List<WebDavPropFindXmlBuilder.VirtualDept> depts = new ArrayList<>();
        for (Map<String, Object> r : rows) {
            depts.add(new WebDavPropFindXmlBuilder.VirtualDept(
                    ((Number) r.get("id")).longValue(), (String) r.get("dept_name")));
        }
        return depts;
    }

    private org.springframework.jdbc.core.JdbcTemplate jdbcTemplate;
    @org.springframework.beans.factory.annotation.Autowired
    public void setJdbcTemplate(org.springframework.jdbc.core.JdbcTemplate jt) { this.jdbcTemplate = jt; }

    private com.erp.data.dto.DatFileQuery deptRootQuery(Long deptId) {
        com.erp.data.dto.DatFileQuery q = new com.erp.data.dto.DatFileQuery();
        q.setDeptId(deptId); return q;
    }
    private com.erp.data.dto.DatFileQuery folderQuery(Long parentId) {
        com.erp.data.dto.DatFileQuery q = new com.erp.data.dto.DatFileQuery();
        q.setParentId(parentId); return q;
    }

    /** 过滤可见子项（canAccess 已在 listFiles 内执行；此处额外保留含共享后代的文件夹）。 */
    private List<DatFile> filterVisible(DatFileService svc, LoginUser user, List<com.erp.data.dto.DatFileVO> vos) {
        List<DatFile> result = new ArrayList<>();
        for (com.erp.data.dto.DatFileVO vo : vos) {
            DatFile f = toEntity(vo);
            if (f.getIsDirectory() != null && f.getIsDirectory() == 1) {
                // 文件夹：保留（listFiles 已按 canAccess 过滤；共享后代保留逻辑见下注释）
                result.add(f);
            } else {
                result.add(f);
            }
        }
        return result;
    }

    private DatFile toEntity(com.erp.data.dto.DatFileVO vo) {
        DatFile f = new DatFile();
        f.setId(vo.getId()); f.setName(vo.getName()); f.setDisplayName(vo.getDisplayName());
        f.setIsDirectory(vo.getIsDirectory()); f.setFileSize(vo.getFileSize());
        f.setExtension(vo.getExtension()); f.setMimeType(vo.getMimeType());
        f.setDeptId(vo.getDeptId()); f.setPath(null);
        return f;
    }

    /** PUT 新文件需 MultipartFile；WebDAV 用 InputStream，封装为简易 MultipartFile。 */
    private org.springframework.web.multipart.MultipartFile toMultipartPlaceholder(HttpServletRequest request, String fileName) {
        return new com.erp.data.webdav.StreamMultipartFile(fileName, request);
    }
}
```

> **重要**：`filterVisible` 中「文件夹含共享后代则保留」需补充查询。`DatFileService.listFiles` 已用 `canAccess` 过滤，但**不含共享后代保留**。需在 Task 3 的 `listFiles` 增加该逻辑，或在 controller 补查 `mapper.selectSharedDescendantExists`。为集中权限逻辑，**在 Task 3 的 `listFiles` 实现中补充**：对每个被 canAccess 过滤掉的**文件夹**，额外用 `selectSharedDescendantExists(folder.path+"/", user.deptId)` 判定，命中则保留。Task 9 的 `filterVisible` 因此简化为直接返回（listFiles 已处理好）。`filterVisible` 实现简化为：

```java
    private List<DatFile> filterVisible(DatFileService svc, LoginUser user, List<com.erp.data.dto.DatFileVO> vos) {
        List<DatFile> result = new ArrayList<>();
        for (com.erp.data.dto.DatFileVO vo : vos) result.add(toEntity(vo));
        return result;
    }
```

- [ ] **Step 2: 补充 Task 3 的 listFiles 共享后代保留逻辑**

回到 `DatFileServiceImpl.listFiles`：在 `canAccess` 过滤循环中，对 `isDirectory==1` 且 `canAccess==false` 的文件夹，追加：
```java
if (!canAccess(f, user) && f.getIsDirectory() != null && f.getIsDirectory() == 1
        && user.getDepartmentId() != null
        && mapper.selectSharedDescendantExists(f.getPath() + "/", user.getDepartmentId())) {
    // 保留：含被共享给本部门的后代
    filtered.add(f);
}
```
（需把现有 `.filter(f -> isAdmin || canAccess(f, user))` 改为传统 for 循环以支持该补充保留。）

- [ ] **Step 3: 实现 StreamMultipartFile（PUT 辅助）**

新建 `erp-data/src/main/java/com/erp/data/webdav/StreamMultipartFile.java`：

```java
package com.erp.data.webdav;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

/** 把 WebDAV PUT 的请求流包装成 MultipartFile，复用 DatFileService.uploadFile。 */
public class StreamMultipartFile implements MultipartFile {

    private final String name;
    private final HttpServletRequest request;
    private Path temp;

    public StreamMultipartFile(String name, HttpServletRequest request) {
        this.name = name;
        this.request = request;
    }

    @Override public String getName() { return "file"; }
    @Override public String getOriginalFilename() { return name; }
    @Override public String getContentType() { return request.getContentType(); }
    @Override public boolean isEmpty() { return false; }
    @Override public long getSize() { return -1; }
    @Override public byte[] getBytes() throws IOException {
        return request.getInputStream().readAllBytes();
    }
    @Override public InputStream getInputStream() throws IOException {
        return request.getInputStream();
    }
    @Override public void transferTo(File dest) throws IOException {
        try (InputStream in = request.getInputStream()) {
            Files.copy(in, dest.toPath(), StandardCopyOption.REPLACE_EXISTING);
        }
    }
    @Override public void transferTo(Path dest) throws IOException {
        try (InputStream in = request.getInputStream()) {
            Files.copy(in, dest, StandardCopyOption.REPLACE_EXISTING);
        }
    }
}
```

> `uploadFile` 内部调 `file.transferTo(target)`，会走 `transferTo(Path)`，把 PUT 流写入磁盘。注意 `getSize()` 返回 -1，`uploadFile` 里 `file.getSize()` 会存 -1；可接受（WebDAV 文件大小后续以磁盘实际为准），或在 transferTo 后用 `Files.size` 回填——Task 3 uploadFile 已用 `file.getSize()`，此处保留，controller PUT 新文件后可不必精确。**为准确**：覆盖 `getSize()` 返回 `request.getContentLengthLong()`。

修正 `StreamMultipartFile.getSize()`：
```java
    @Override public long getSize() {
        long len = request.getContentLengthLong();
        return len < 0 ? 0 : len;
    }
```

- [ ] **Step 4: 编译通过**

Run: `mvn -pl erp-data -am compile -q`
Expected: BUILD SUCCESS（若 `DatFileService.canWrite` 仍 private，Task 3 已改 package-private；controller 在同包 `com.erp.data.webdav`，而 `canWrite` 在 `com.erp.data.service.impl`，**跨包不可见**。需在 `DatFileService` 接口暴露 `canWrite`，或在 controller 改用 `canCreate`+其他判断。）

**修正**：把 `canWrite(DatFile, LoginUser)` 提升到 `DatFileService` 接口（public），Task 3 同步加接口声明。补 `DatFileService.java`：
```java
    /** 检查写权限（WebDAV LOCK 校验用）。 */
    boolean canWrite(DatFile file, LoginUser user);
```
Task 3 Step 4 的 `canWrite` 加 `@Override`。

- [ ] **Step 5: Commit**

```bash
git add erp-data/src/main/java/com/erp/data/webdav/WebDavController.java \
        erp-data/src/main/java/com/erp/data/webdav/StreamMultipartFile.java \
        erp-data/src/main/java/com/erp/data/service/DatFileService.java \
        erp-data/src/main/java/com/erp/data/service/impl/DatFileServiceImpl.java
git commit -m "feat(webdav): WebDavController 八动词协议处理器"
```

---

## Task 10: WebDAV 协议集成测试

**Files:**
- Test: `erp-web/src/test/java/com/erp/web/webdav/WebDavControllerTest.java`

**说明**：erp-web 现有测试排除数据源 + 内存用户（`erp.user.persistence=memory`）。WebDavPathResolver/DatFileService 依赖数据库，故用 `@MockBean` 替换它们，专注测「WebDAV 动词 → 状态码/XML」映射与鉴权。真实业务逻辑已在 erp-data 单测覆盖。

- [ ] **Step 1: 写集成测试**

```java
package com.erp.web.webdav;

import com.baomidou.mybatisplus.autoconfigure.MybatisPlusAutoConfiguration;
import com.erp.data.entity.DatFile;
import com.erp.data.service.DatFileService;
import com.erp.data.webdav.ResolvedPath;
import com.erp.data.webdav.WebDavPathResolver;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.flyway.FlywayAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@AutoConfigureMockMvc
@EnableAutoConfiguration(exclude = {DataSourceAutoConfiguration.class, FlywayAutoConfiguration.class,
        HibernateJpaAutoConfiguration.class, MybatisPlusAutoConfiguration.class})
@ActiveProfiles("dev")
@org.springframework.test.context.TestPropertySource(properties = "erp.user.persistence=memory")
@SpringBootTest
class WebDavControllerTest {

    @Autowired MockMvc m;
    @MockBean DatFileService fileService;
    @MockBean WebDavPathResolver resolver;

    private String basic(String user, String pass) {
        return "Basic " + Base64.getEncoder().encodeToString((user + ":" + pass).getBytes(StandardCharsets.UTF_8));
    }

    @Test
    void noAuth_returns401() throws Exception {
        m.perform(propfind("/webdav/")).andExpect(status().isUnauthorized());
    }

    @Test
    void wrongPassword_returns401() throws Exception {
        m.perform(propfind("/webdav/").header("Authorization", basic("admin", "wrong")))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void options_returnsDavHeader() throws Exception {
        m.perform(options("/webdav/").header("Authorization", basic("admin", "admin123")))
                .andExpect(status().isOk())
                .andExpect(header().string("DAV", "1,2"));
    }

    @Test
    void propfind_root_returns207() throws Exception {
        when(resolver.resolve(any(), any())).thenReturn(ResolvedPath.root("/webdav/"));
        when(fileService.listFiles(any(), any())).thenReturn(List.of());
        m.perform(propfind("/webdav/").header("Authorization", basic("admin", "admin123")))
                .andExpect(status().is(207))
                .andExpect(content().contentTypeCompatibleWith("application/xml"));
    }

    @Test
    void propfind_notFound_returns404() throws Exception {
        when(resolver.resolve(any(), any())).thenReturn(ResolvedPath.notFound("/webdav/x/"));
        m.perform(propfind("/webdav/x/").header("Authorization", basic("admin", "admin123")))
                .andExpect(status().isNotFound());
    }

    @Test
    void mkcol_forbidden_returns403() throws Exception {
        when(resolver.resolve(any(), any())).thenReturn(ResolvedPath.root("/webdav/"));
        when(fileService.canCreate(any(), any())).thenReturn(false);
        m.perform(mkcol("/webdav/销售部/新文件夹").header("Authorization", basic("admin", "admin123")))
                .andExpect(status().isForbidden());
    }

    private org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder propfind(String url) {
        return request("PROPFIND", url).contentType(MediaType.APPLICATION_XML).content("<x/>");
    }
    private org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder mkcol(String url) {
        return request("MKCOL", url);
    }
    private org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder options(String url) {
        return request("OPTIONS", url);
    }
    private org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder request(String method, String url) {
        return org.springframework.test.web.servlet.request.MockMvcRequestBuilders.request(
                org.springframework.http.HttpMethod.valueOf(method), url);
    }
}
```

> 注：`admin/admin123` 来自 dev profile 内存用户（与 `AuthControllerTest` 一致）。`PROPFIND`/`MKCOL` 等自定义方法用 `MockMvcRequestBuilders.request(HttpMethod, url)`。

- [ ] **Step 2: 运行测试，确认通过**

Run: `mvn -pl erp-web -am test -Dtest=WebDavControllerTest -q`
Expected: 全部 PASS。若 `@MockBean WebDavPathResolver` 导致 `WebDavController` 构造注入冲突（因 controller 用构造注入），需确认 `@MockBean` 能替换——Spring Boot 会用 mock 覆盖 bean。若报多 bean，给 mock 加 `@Primary`。

- [ ] **Step 3: Commit**

```bash
git add erp-web/src/test/java/com/erp/web/webdav/WebDavControllerTest.java
git commit -m "test(webdav): 协议动词状态码与鉴权集成测试"
```

---

## Task 11: Windows 真机验收 + .reg 脚本 + 文档

**Files:**
- Create: `docs/webdav-windows-setup.md`（Windows 客户端配置 + 验收清单）

**说明**：单测过不了 Windows WebDAV 客户端怪癖，必须真机验收。

- [ ] **Step 1: 写 Windows 配置文档与 .reg 说明**

```markdown
# WebDAV 网络磁盘 — Windows 客户端配置

## 1. 映射网络驱动器

资源管理器 → 此电脑 → 映射网络驱动器 → 选盘符（如 Z）→ 文件夹填：

    http://<服务器IP>:8080/webdav

勾选「使用其他凭据」→ 输入 ERP 账号密码。

## 2. 注册表配置（HTTP 下必需）

Windows 默认不对 HTTP 发送 Basic Auth，且限制单文件 50MB。新建 `webdav-fix.reg`：

    Windows Registry Editor Version 5.00

    [HKEY_LOCAL_MACHINE\SYSTEM\CurrentControlSet\Services\WebClient\Parameters]
    "BasicAuthLevel"=dword:00000002
    "FileSizeLimitInBytes"=dword:ffffffff

双击导入，重启 WebClient 服务（或重启电脑）：
`net stop WebClient && net start WebClient`

## 3. 验收清单

- [ ] 映射成功，Z 盘出现
- [ ] 根目录列出本部门 + 下级部门 + 共享部门
- [ ] 双击 .docx 用 Word 打开，Ctrl+S 保存无报错
- [ ] 关闭后他人打开看到新内容
- [ ] 并发：A 打开编辑时，B 双击同一文件收到锁定/只读提示
- [ ] 跨部门目录无法新建文件（权限拒绝）
- [ ] 下级部门目录只读
- [ ] 删除文件后资源管理器刷新消失
- [ ] 重命名/移动在本部门内正常
```

- [ ] **Step 2: 真机执行验收清单**

按文档逐项验证。记录任何失败项，回查对应 Task。

- [ ] **Step 3: 全量回归测试**

Run: `mvn -pl erp-data,erp-web -am test -q`
Expected: 所有单测 + 集成测试 PASS

- [ ] **Step 4: Commit**

```bash
git add docs/webdav-windows-setup.md
git commit -m "docs(webdav): Windows 客户端配置与验收清单"
```

---

## 实现注意事项（汇总）

执行时优先消化这些跨 Task 的约束，避免反复返工：

1. **canWrite 提升为接口方法**：Task 9 controller 跨包调用 `canWrite`，必须把 `DatFileService.canWrite(DatFile, LoginUser)` 提升为接口 public 方法（Task 3 Step 4 加 `@Override`，Task 9 Step 4 加接口声明）。`canAccess` 保持 package-private（仅 erp-data 内部用）。

2. **listFiles 补共享后代保留**：Task 3 的 `listFiles` 必须把 `canAccess` 过滤从 stream 改为 for 循环，对「被过滤的文件夹」补查 `selectSharedDescendantExists` 命中则保留（Task 9 Step 2）。否则共享深层文件夹不可见。

3. **WebDavPathResolver 与 XmlBuilder 的 basePath 编码**：resolver 返回的 basePath 是解码原样；controller `encodedBasePath()` 重新编码后传给 XmlBuilder。测试以 controller 行为为准。

4. **WebDavSecurityConfig 用独立 FilterChain**：`securityMatcher("/webdav/**")` 接管该路径，**不要**改 `SecurityConfig.PUBLIC_PATHS`（Task 7 Step 4 已撤销）。

5. **PUT 新文件归属**：`uploadFile` 的 `deptId` 参数传 `parentDeptId`（所在目录部门）；因 `canCreate` 仅允许本部门，实际等同本部门。

6. **MOVE 跨部门**：controller 显式判 `src.deptId != destDeptId → 403`，双保险（`canCreate` 也会卡）。

7. **LOCK 锁前提 canWrite**：controller LOCK 先 `canWrite` 再 `tryLock`；GET 额外查 `isLockedByOther` 返 423。

8. **StreamMultipartFile.getSize**：返回 `request.getContentLengthLong()`，避免 -1 入库。

---

## Self-Review 结果

- **Spec 覆盖**：spec 第 3 节权限 → Task 3；第 4 节路径映射 → Task 6；第 5 节组件 → Task 4/5/6/7/8/9；第 6 节数据流 → Task 9 各动词；第 7 节错误处理 → Task 8；第 8 节测试 → Task 4/5/6/10 + Task 3 权限单测；第 9 节验收 → Task 11。✓ 全覆盖。
- **占位符**：无 TBD/TODO；所有代码块完整。✓
- **类型一致性**：`ResolvedPath.Type`（ROOT/DEPT_ROOT/FOLDER/FILE/NOT_FOUND）在 Task 6 定义，Task 9/10 引用一致；`WebDavLockStore` 方法签名 Task 4 定义、Task 9 引用一致；`VirtualDept` record Task 5 定义、Task 9 引用一致。✓
- **已知简化**：Task 9 controller 含实现中自我修正段（跨包 canWrite、listFiles 共享后代、StreamMultipartFile.getSize），已集中在「实现注意事项」汇总，执行者据此一次性处理。
