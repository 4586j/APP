-- ============================================================================
-- Flyway V1: 系统模块基础表 (B1.2)
-- 6 张表: sys_department, sys_user, sys_role, sys_permission,
--        sys_user_role, sys_role_permission
-- 字符集 utf8mb4_unicode_ci，引擎 InnoDB
-- 数据库: erp_demo2 (由运维预建，Flyway 不创建库)
-- ============================================================================
-- ============================================================================
-- 1. 系统模块 (sys) — 用户、角色、权限
-- ============================================================================

-- 部门字典表（支持多级组织架构：总部→分公司→部门）
CREATE TABLE sys_department (
    id          BIGINT        PRIMARY KEY AUTO_INCREMENT,
    parent_id   BIGINT        DEFAULT 0 COMMENT '父部门ID，0=顶级组织(总部/分公司)',
    dept_code   VARCHAR(32)   NOT NULL UNIQUE COMMENT '部门编码: HQ/SH_BRANCH/SALES/PURCHASE',
    dept_name   VARCHAR(64)   NOT NULL COMMENT '部门名称',
    dept_path   VARCHAR(512)  COMMENT '部门路径(祖级链): /1/2/5, 便于递归查询子部门',
    sort_order  INT           DEFAULT 0,
    created_at  DATETIME      DEFAULT CURRENT_TIMESTAMP,
    updated_at  DATETIME      DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted     TINYINT       DEFAULT 0,
    INDEX idx_parent (parent_id),
    INDEX idx_dept_path (dept_path)
) ENGINE=InnoDB COMMENT='部门字典(多级组织架构)';

-- 用户表 (sys_user)
-- superior_id: 直属上级，用于审批流中定位"部门主管"或"直属上级"，避免依赖角色推断
-- failed_login_count + locked_until: 防暴力破解，5次失败后锁定15分钟
CREATE TABLE sys_user (
    id                BIGINT        PRIMARY KEY AUTO_INCREMENT,
    username          VARCHAR(64)   NOT NULL UNIQUE COMMENT '登录名',
    password          VARCHAR(256)  NOT NULL COMMENT 'BCrypt 密文',
    real_name         VARCHAR(64)   COMMENT '真实姓名',
    email             VARCHAR(128)  COMMENT '邮箱',
    phone             VARCHAR(32)   COMMENT '手机号',
    avatar_url        VARCHAR(256)  COMMENT '头像URL',
    department_id     BIGINT        COMMENT '部门ID → sys_department.id',
    superior_id       BIGINT        COMMENT '直属上级 → sys_user.id (审批流定位用)',
    status            TINYINT       DEFAULT 1 COMMENT '1=启用 0=禁用',
    pwd_reset_required TINYINT      DEFAULT 0 COMMENT '1=下次登录强制修改密码',
    failed_login_count INT          DEFAULT 0 COMMENT '连续登录失败次数',
    locked_until      DATETIME      COMMENT '锁定截止时间 (NULL=未锁定)',
    last_login_at     DATETIME      COMMENT '最后登录时间',
    last_login_ip     VARCHAR(64)   COMMENT '最后登录IP',
    created_by        BIGINT        COMMENT '创建人(admin)',
    created_at        DATETIME      DEFAULT CURRENT_TIMESTAMP,
    updated_at        DATETIME      DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted           TINYINT       DEFAULT 0,
    INDEX idx_department (department_id),
    INDEX idx_superior (superior_id),
    INDEX idx_status (status)
) ENGINE=InnoDB COMMENT='系统用户';

-- 角色表 (sys_role)
-- data_scope: 数据权限粒度 — 1=仅本人数据, 2=本部门数据, 3=本部门及子部门, 4=全部数据
CREATE TABLE sys_role (
    id          BIGINT        PRIMARY KEY AUTO_INCREMENT,
    role_name   VARCHAR(64)   NOT NULL COMMENT '角色名称',
    role_code   VARCHAR(64)   NOT NULL UNIQUE COMMENT '角色编码: ROLE_ADMIN',
    data_scope  TINYINT       DEFAULT 2 COMMENT '数据范围: 1=SELF, 2=DEPT, 3=DEPT_AND_CHILDREN, 4=ALL',
    description VARCHAR(256)  COMMENT '描述',
    status      TINYINT       DEFAULT 1 COMMENT '1=启用 0=禁用',
    created_at  DATETIME      DEFAULT CURRENT_TIMESTAMP,
    updated_at  DATETIME      DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted     TINYINT       DEFAULT 0
) ENGINE=InnoDB COMMENT='系统角色';

-- 权限表 (sys_permission) — 菜单+按钮+API
-- perm_type=api + http_method: 区分同一资源的 GET/POST/PUT/DELETE，如 order:sales:delete
-- deleted: 逻辑删除，与其他表保持一致的软删除机制
CREATE TABLE sys_permission (
    id          BIGINT        PRIMARY KEY AUTO_INCREMENT,
    parent_id   BIGINT        DEFAULT 0 COMMENT '父权限ID，0=顶级',
    perm_name   VARCHAR(64)   NOT NULL COMMENT '权限名称',
    perm_code   VARCHAR(128)  NOT NULL UNIQUE COMMENT '权限码: order:sales:delete',
    perm_type   VARCHAR(16)   NOT NULL DEFAULT 'menu' COMMENT 'menu|button|api',
    http_method VARCHAR(8)    COMMENT 'HTTP方法(GET/POST/PUT/DELETE/*)，仅perm_type=api时有效',
    icon        VARCHAR(64)   COMMENT 'Element Plus 图标名',
    path        VARCHAR(128)  COMMENT '前端路由路径',
    component   VARCHAR(256)  COMMENT '前端组件路径',
    sort_order  INT           DEFAULT 0,
    visible     TINYINT       DEFAULT 1 COMMENT '1=可见 0=隐藏',
    created_at  DATETIME      DEFAULT CURRENT_TIMESTAMP,
    updated_at  DATETIME      DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted     TINYINT       DEFAULT 0,
    INDEX idx_parent (parent_id),
    INDEX idx_perm_code (perm_code)
) ENGINE=InnoDB COMMENT='权限(菜单/按钮/API)';

-- 用户-角色关联表
CREATE TABLE sys_user_role (
    id        BIGINT  PRIMARY KEY AUTO_INCREMENT,
    user_id   BIGINT  NOT NULL COMMENT '用户ID',
    role_id   BIGINT  NOT NULL COMMENT '角色ID',
    UNIQUE KEY uk_user_role (user_id, role_id),
    INDEX idx_role (role_id)
) ENGINE=InnoDB COMMENT='用户-角色关联';

-- 角色-权限关联表
CREATE TABLE sys_role_permission (
    id            BIGINT  PRIMARY KEY AUTO_INCREMENT,
    role_id       BIGINT  NOT NULL COMMENT '角色ID',
    permission_id BIGINT  NOT NULL COMMENT '权限ID',
    UNIQUE KEY uk_role_perm (role_id, permission_id),
    INDEX idx_perm (permission_id)
) ENGINE=InnoDB COMMENT='角色-权限关联';

-- ============================================================================
-- 初始数据 (seed)
-- ============================================================================

-- 顶级部门
INSERT INTO sys_department (id, parent_id, dept_code, dept_name, dept_path, sort_order)
VALUES (1, 0, 'HQ', '总部', '/1', 0);

-- 默认管理员角色
INSERT INTO sys_role (id, role_name, role_code, data_scope, description, status)
VALUES (1, '系统管理员', 'ROLE_ADMIN', 4, '系统超级管理员，拥有所有权限', 1);

-- 默认 admin 用户 (BCrypt 密码: admin123)
-- 生成命令: htpasswd -bnBC 10 "" admin123 | tr -d ':\n' | sed 's/^\$2y/\$2a/'
INSERT INTO sys_user (id, username, password, real_name, email, department_id, status, pwd_reset_required)
VALUES (1, 'admin', '$2a$10$.f/Swt4alCgklCCacCMzn.W4D15lY6SPqp6Z5NpdelxV6wGyRo4OO',
        '系统管理员', 'admin@erp.local', 1, 1, 0);

-- 绑定 admin 到 ROLE_ADMIN
INSERT INTO sys_user_role (user_id, role_id) VALUES (1, 1);

-- 顶级菜单：系统管理
INSERT INTO sys_permission (id, parent_id, perm_name, perm_code, perm_type, icon, path, component, sort_order, visible)
VALUES (1, 0, '系统管理', 'system', 'menu', 'Setting', '/system', 'Layout', 1, 1);

-- 子菜单
INSERT INTO sys_permission (id, parent_id, perm_name, perm_code, perm_type, icon, path, component, sort_order, visible) VALUES
  (10, 1, '用户管理', 'system:user',  'menu', 'User',       '/system/user',       'system/user/index', 1, 1),
  (11, 1, '角色管理', 'system:role',  'menu', 'UserFilled', '/system/role',       'system/role/index', 2, 1),
  (12, 1, '权限管理', 'system:perm',  'menu', 'Key',        '/system/permission', 'system/permission/index', 3, 1),
  (13, 1, '部门管理', 'system:dept',  'menu', 'OfficeBuilding', '/system/department', 'system/department/index', 4, 1);

-- 将所有权限赋给 ROLE_ADMIN
INSERT INTO sys_role_permission (role_id, permission_id)
SELECT 1, id FROM sys_permission;
