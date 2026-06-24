-- V12: 订单权限 seed + ROLE_ADMIN 分配

INSERT IGNORE INTO sys_permission (parent_id, perm_name, perm_code, perm_type, sort_order, visible, created_at)
VALUES (0, '订单管理', 'order', 'menu', 2, 1, NOW());
SET @menu_order = (SELECT id FROM sys_permission WHERE perm_code = 'order' AND perm_type = 'menu' LIMIT 1);
UPDATE sys_permission SET icon = 'Document', path = '/order/sales' WHERE id = @menu_order;

INSERT IGNORE INTO sys_permission (parent_id, perm_name, perm_code, perm_type, sort_order, visible, created_at) VALUES
(@menu_order, '订单查询', 'order:view', 'button', 1, 1, NOW()),
(@menu_order, '订单新增', 'order:create', 'button', 2, 1, NOW()),
(@menu_order, '订单修改', 'order:update', 'button', 3, 1, NOW()),
(@menu_order, '订单删除', 'order:delete', 'button', 4, 1, NOW());

SET @admin_role = (SELECT id FROM sys_role WHERE role_code = 'ROLE_ADMIN' LIMIT 1);
INSERT IGNORE INTO sys_role_permission (role_id, permission_id)
SELECT @admin_role, id FROM sys_permission WHERE perm_code LIKE 'order:%';
