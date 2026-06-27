SET NAMES utf8mb4;

-- ============================================================================
-- 部门用户权限表：部长/上级部长为部门内用户分配具体权限
-- 管理员(ROLE_ADMIN) 不受此表限制，拥有所有权限
-- ============================================================================
CREATE TABLE IF NOT EXISTS `sys_dept_user_permission` (
    `id`            BIGINT    PRIMARY KEY AUTO_INCREMENT,
    `user_id`       BIGINT    NOT NULL COMMENT '用户ID',
    `dept_id`       BIGINT    NOT NULL COMMENT '所属部门ID',
    `permission_id` BIGINT    NOT NULL COMMENT '权限ID',
    `granted_by`    BIGINT    DEFAULT NULL COMMENT '授权人用户ID',
    `created_at`    DATETIME  DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY `uk_user_dept_perm` (`user_id`, `dept_id`, `permission_id`),
    INDEX `idx_dept` (`dept_id`),
    INDEX `idx_user` (`user_id`),
    INDEX `idx_perm` (`permission_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='部门用户权限表';
