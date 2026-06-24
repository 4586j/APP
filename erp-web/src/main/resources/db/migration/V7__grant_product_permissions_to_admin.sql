-- V7: 分配产品/客户权限给 ROLE_ADMIN
-- V5 只创建了权限记录但没分配给 ADMIN 角色

SET @admin_role = (SELECT id FROM sys_role WHERE role_code = 'ROLE_ADMIN' LIMIT 1);

INSERT IGNORE INTO sys_role_permission (role_id, permission_id)
SELECT @admin_role, p.id
FROM sys_permission p
WHERE p.perm_code IN (
  'product:view', 'product:create', 'product:update', 'product:delete',
  'customer:view', 'supplier:view'
);
