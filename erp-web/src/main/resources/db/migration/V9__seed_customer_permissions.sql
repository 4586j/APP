-- V9: 客户/供应商权限 seed + 分配给 ROLE_ADMIN

-- 菜单权限
INSERT IGNORE INTO sys_permission (parent_id, perm_name, perm_code, perm_type, sort_order, visible, created_at)
VALUES (0, '客户管理', 'customer', 'menu', 4, 1, NOW());
SET @menu_customer = (SELECT id FROM sys_permission WHERE perm_code = 'customer' AND perm_type = 'menu' LIMIT 1);
UPDATE sys_permission SET icon = 'User', path = '/customer/list', component = 'customer/CustomerList.vue' WHERE id = @menu_customer;

INSERT IGNORE INTO sys_permission (parent_id, perm_name, perm_code, perm_type, sort_order, visible, created_at) VALUES
(@menu_customer, '客户查询', 'customer:view', 'button', 1, 1, NOW()),
(@menu_customer, '客户新增', 'customer:create', 'button', 2, 1, NOW()),
(@menu_customer, '客户修改', 'customer:update', 'button', 3, 1, NOW()),
(@menu_customer, '客户删除', 'customer:delete', 'button', 4, 1, NOW()),
(@menu_customer, '供应商查询', 'supplier:view', 'button', 5, 1, NOW()),
(@menu_customer, '供应商新增', 'supplier:create', 'button', 6, 1, NOW()),
(@menu_customer, '供应商修改', 'supplier:update', 'button', 7, 1, NOW()),
(@menu_customer, '供应商删除', 'supplier:delete', 'button', 8, 1, NOW());

-- 分配给 ROLE_ADMIN
SET @admin_role = (SELECT id FROM sys_role WHERE role_code = 'ROLE_ADMIN' LIMIT 1);
INSERT IGNORE INTO sys_role_permission (role_id, permission_id)
SELECT @admin_role, p.id FROM sys_permission p
WHERE p.perm_code IN ('customer:view','customer:create','customer:update','customer:delete',
                       'supplier:view','supplier:create','supplier:update','supplier:delete');
