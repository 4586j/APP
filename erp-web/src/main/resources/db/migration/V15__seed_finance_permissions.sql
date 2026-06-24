-- V15: 财务权限 seed
INSERT IGNORE INTO sys_permission (parent_id, perm_name, perm_code, perm_type, sort_order, visible, created_at)
VALUES (0, '财务管理', 'finance', 'menu', 5, 1, NOW());
SET @menu_fin = (SELECT id FROM sys_permission WHERE perm_code = 'finance' AND perm_type = 'menu' LIMIT 1);
UPDATE sys_permission SET icon = 'Money', path = '/finance/overview' WHERE id = @menu_fin;

INSERT IGNORE INTO sys_permission (parent_id, perm_name, perm_code, perm_type, sort_order, visible, created_at) VALUES
(@menu_fin, '财务概览', 'finance:view', 'button', 1, 1, NOW()),
(@menu_fin, '汇率管理', 'exchange-rate:view', 'button', 2, 1, NOW()),
(@menu_fin, '汇率录入', 'exchange-rate:create', 'button', 3, 1, NOW()),
(@menu_fin, '资金审批', 'fund:view', 'button', 4, 1, NOW()),
(@menu_fin, '资金审批操作', 'fund:approve', 'button', 5, 1, NOW());

SET @admin_role = (SELECT id FROM sys_role WHERE role_code = 'ROLE_ADMIN' LIMIT 1);
INSERT IGNORE INTO sys_role_permission (role_id, permission_id)
SELECT @admin_role, id FROM sys_permission WHERE perm_code LIKE 'finance:%' OR perm_code LIKE 'exchange-rate:%' OR perm_code LIKE 'fund:%';
