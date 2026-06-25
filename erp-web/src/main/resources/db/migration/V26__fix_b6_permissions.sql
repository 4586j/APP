-- 补 V24 遗漏的 B6 菜单权限 + 授权给 admin 角色
SET NAMES utf8mb4;

-- 补充 data:view 权限（侧边栏用）
INSERT IGNORE INTO `sys_permission` (`perm_code`, `perm_name`, `perm_type`, `parent_id`, `path`, `icon`, `sort_order`, `visible`)
SELECT 'data:view', '数据查看', 'BUTTON', id, NULL, NULL, 0, 1
FROM sys_permission WHERE perm_code = 'data' AND deleted = 0;

-- 补充 dashboard:view 权限（仪表盘用，虽然 alwaysShow 自由访问，但补上）
INSERT IGNORE INTO `sys_permission` (`perm_code`, `perm_name`, `perm_type`, `parent_id`, `path`, `icon`, `sort_order`, `visible`)
SELECT 'dashboard:view', '仪表盘查看', 'BUTTON', id, NULL, NULL, 0, 1
FROM sys_permission WHERE perm_code = 'dashboard' AND deleted = 0;

-- 授权给 admin 角色（role_code=ROLE_ADMIN）
SET @admin_role = (SELECT id FROM sys_role WHERE role_code = 'ROLE_ADMIN' LIMIT 1);

INSERT IGNORE INTO `sys_role_permission` (`role_id`, `permission_id`)
SELECT @admin_role, id FROM sys_permission
WHERE perm_code IN ('data:view', 'dashboard:view') AND deleted = 0;
