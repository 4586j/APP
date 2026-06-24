-- V5: 产品模块权限 seed
-- 产品管理菜单 + 按钮权限

SET @menu_product = (SELECT id FROM sys_permission WHERE perm_code = 'product' AND perm_type = 'menu' LIMIT 1);

-- 产品管理菜单
INSERT IGNORE INTO sys_permission (parent_id, perm_name, perm_code, perm_type, path, component, icon, sort_order, visible, created_at)
VALUES (0, '产品管理', 'product', 'menu', '/product/list', 'product/ProductList.vue', 'Goods', 3, 1, NOW());

SET @menu_product = (SELECT id FROM sys_permission WHERE perm_code = 'product' AND perm_type = 'menu' LIMIT 1);

-- 按钮权限
INSERT IGNORE INTO sys_permission (parent_id, perm_name, perm_code, perm_type, sort_order, visible, created_at) VALUES
(@menu_product, '产品查询', 'product:view', 'button', 1, 1, NOW()),
(@menu_product, '产品新增', 'product:create', 'button', 2, 1, NOW()),
(@menu_product, '产品修改', 'product:update', 'button', 3, 1, NOW()),
(@menu_product, '产品删除', 'product:delete', 'button', 4, 1, NOW());
