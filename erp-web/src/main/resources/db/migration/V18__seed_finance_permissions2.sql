-- V18: 应收/应付/结算权限（修正列名）
INSERT IGNORE INTO sys_permission (id, parent_id, perm_name, perm_code, perm_type, sort_order) VALUES
  (159, 154, '应收查看', 'receivable:view', 'button', 80),
  (160, 159, '应收录入', 'receivable:create', 'button', 81),
  (161, 159, '应收修改', 'receivable:update', 'button', 82),
  (162, 159, '应收删除', 'receivable:delete', 'button', 83),
  (163, 154, '应付查看', 'payable:view', 'button', 90),
  (164, 163, '应付录入', 'payable:create', 'button', 91),
  (165, 163, '应付修改', 'payable:update', 'button', 92),
  (166, 163, '应付删除', 'payable:delete', 'button', 93),
  (167, 154, '结算查看', 'settlement:view', 'button', 100),
  (168, 167, '结算录入', 'settlement:create', 'button', 101),
  (169, 167, '结算删除', 'settlement:delete', 'button', 103);
-- 给 admin 角色分配权限
INSERT IGNORE INTO sys_role_permission (role_id, permission_id)
SELECT 1, id FROM sys_permission WHERE perm_code LIKE 'receivable:%' OR perm_code LIKE 'payable:%' OR perm_code LIKE 'settlement:%';
