-- 添加角色分配用户权限
INSERT INTO sys_permission (id, parent_id, perm_name, perm_code, perm_type, http_method, sort_order, visible)
VALUES (115, 11, '分配用户', 'role:assign-user', 'button', 'PUT', 6, 1);

-- 绑定到 ROLE_ADMIN
INSERT INTO sys_role_permission (role_id, permission_id) VALUES (1, 115);
