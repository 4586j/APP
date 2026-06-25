-- V22: 单证权限 seed
INSERT IGNORE INTO sys_permission (id, parent_id, perm_name, perm_code, perm_type, sort_order) VALUES
  (180, 0, '单证管理', 'document', 'menu', 190),
  (181, 180, '单证查看', 'document:view', 'button', 191),
  (182, 180, '单证生成', 'document:create', 'button', 192),
  (183, 180, '单证编辑', 'document:update', 'button', 193),
  (184, 180, '单证删除', 'document:delete', 'button', 194);
INSERT IGNORE INTO sys_role_permission (role_id, permission_id)
SELECT 1, id FROM sys_permission WHERE perm_code LIKE 'document%';
