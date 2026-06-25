-- V30: B6 收尾 — 通知中心 + 报表导出 权限 seed，并授予 ROLE_ADMIN
SET NAMES utf8mb4;

-- 通知中心 notification:*（独立顶级菜单）
INSERT IGNORE INTO sys_permission (parent_id, perm_name, perm_code, perm_type, sort_order, visible, icon, path) VALUES
(0, '通知中心', 'notification', 'menu', 7, 1, 'Bell', '/notification/my');
SET @menu_ntf = (SELECT id FROM sys_permission WHERE perm_code = 'notification' AND perm_type = 'menu' LIMIT 1);

INSERT IGNORE INTO sys_permission (parent_id, perm_name, perm_code, perm_type, sort_order, visible) VALUES
(@menu_ntf, '通知查看',   'notification:view',   'button', 200, 1),
(@menu_ntf, '通知已读',   'notification:read',   'button', 201, 1),
(@menu_ntf, '通知创建',   'notification:create', 'button', 202, 1);

-- 报表导出 report:*（独立顶级菜单）
INSERT IGNORE INTO sys_permission (parent_id, perm_name, perm_code, perm_type, sort_order, visible, icon, path) VALUES
(0, '报表中心', 'report', 'menu', 8, 1, 'DataLine', '/report/export');
SET @menu_rpt = (SELECT id FROM sys_permission WHERE perm_code = 'report' AND perm_type = 'menu' LIMIT 1);

INSERT IGNORE INTO sys_permission (parent_id, perm_name, perm_code, perm_type, sort_order, visible) VALUES
(@menu_rpt, '报表查看', 'report:view',   'button', 210, 1),
(@menu_rpt, '报表导出', 'report:export', 'button', 211, 1);

-- 全部新码授予 ROLE_ADMIN
SET @admin_role = (SELECT id FROM sys_role WHERE role_code = 'ROLE_ADMIN' LIMIT 1);
INSERT IGNORE INTO sys_role_permission (role_id, permission_id)
SELECT @admin_role, id FROM sys_permission
WHERE perm_code LIKE 'notification%' OR perm_code LIKE 'report%';
