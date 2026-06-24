-- ============================================================================
-- Flyway V2: button 级权限 seed (B1.5)
-- 对应 erp-user/controller/{Department,User,Role,Permission}Controller 的 @PreAuthorize 列表
-- parent_id 指向 V1 的菜单 (10=user, 11=role, 12=permission, 13=department)
-- ============================================================================

INSERT INTO sys_permission (id, parent_id, perm_name, perm_code, perm_type, http_method, sort_order, visible) VALUES
  -- 用户管理 button (parent=10 system:user)
  (100, 10, '查看用户',     'user:view',           'button', 'GET',    1, 1),
  (101, 10, '新建用户',     'user:create',         'button', 'POST',   2, 1),
  (102, 10, '编辑用户',     'user:update',         'button', 'PUT',    3, 1),
  (103, 10, '删除用户',     'user:delete',         'button', 'DELETE', 4, 1),
  (104, 10, '重置密码',     'user:reset-password', 'button', 'POST',   5, 1),
  (105, 10, '分配角色',     'user:assign-role',    'button', 'PUT',    6, 1),

  -- 角色管理 button (parent=11 system:role)
  (110, 11, '查看角色',     'role:view',           'button', 'GET',    1, 1),
  (111, 11, '新建角色',     'role:create',         'button', 'POST',   2, 1),
  (112, 11, '编辑角色',     'role:update',         'button', 'PUT',    3, 1),
  (113, 11, '删除角色',     'role:delete',         'button', 'DELETE', 4, 1),
  (114, 11, '分配权限',     'role:assign-perm',    'button', 'PUT',    5, 1),

  -- 权限管理 button (parent=12 system:perm)
  (120, 12, '查看权限',     'permission:view',     'button', 'GET',    1, 1),
  (121, 12, '新建权限',     'permission:create',   'button', 'POST',   2, 1),
  (122, 12, '编辑权限',     'permission:update',   'button', 'PUT',    3, 1),
  (123, 12, '删除权限',     'permission:delete',   'button', 'DELETE', 4, 1),

  -- 部门管理 button (parent=13 system:dept)
  (130, 13, '查看部门',     'department:view',     'button', 'GET',    1, 1),
  (131, 13, '新建部门',     'department:create',   'button', 'POST',   2, 1),
  (132, 13, '编辑部门',     'department:update',   'button', 'PUT',    3, 1),
  (133, 13, '删除部门',     'department:delete',   'button', 'DELETE', 4, 1);

-- 把新加的 button 权限全部绑到 ROLE_ADMIN (id=1)
INSERT INTO sys_role_permission (role_id, permission_id)
SELECT 1, id FROM sys_permission WHERE id >= 100;
