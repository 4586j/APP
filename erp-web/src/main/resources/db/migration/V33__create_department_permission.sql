-- ============================================================================
-- V33: 部门-权限关联表 + 权限管理菜单
-- 实现部门维度功能权限：用户最终权限 = 角色权限 ∪ 部门权限
-- ============================================================================

-- 部门-权限关联表（部门维度功能授权）
CREATE TABLE sys_department_permission (
    id            BIGINT  PRIMARY KEY AUTO_INCREMENT,
    department_id BIGINT  NOT NULL COMMENT '部门ID → sys_department.id',
    permission_id BIGINT  NOT NULL COMMENT '权限ID → sys_permission.id',
    UNIQUE KEY uk_dept_perm (department_id, permission_id),
    INDEX idx_perm (permission_id)
) ENGINE=InnoDB COMMENT='部门-权限关联（部门维度功能授权）';

-- 权限管理菜单（已有 system:perm 菜单id=12，此处补充按钮权限已在V2中）
-- 给 ROLE_ADMIN 绑定所有现有权限（如果之前遗漏）
INSERT INTO sys_role_permission (role_id, permission_id)
SELECT 1, id FROM sys_permission WHERE id NOT IN (
    SELECT permission_id FROM sys_role_permission WHERE role_id = 1
);
