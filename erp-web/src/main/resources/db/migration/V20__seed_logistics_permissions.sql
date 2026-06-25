-- V20: 物流权限 seed
INSERT IGNORE INTO sys_permission (id, parent_id, perm_name, perm_code, perm_type, sort_order) VALUES
  (170, 0, '物流管理', 'logistics', 'menu', 180),
  (171, 170, '物流查看', 'logistics:view', 'button', 181),
  (172, 170, '物流新建', 'logistics:create', 'button', 182),
  (173, 170, '物流编辑', 'logistics:update', 'button', 183),
  (174, 170, '物流删除', 'logistics:delete', 'button', 184);
INSERT IGNORE INTO sys_role_permission (role_id, permission_id)
SELECT 1, id FROM sys_permission WHERE perm_code LIKE 'logistics%';
