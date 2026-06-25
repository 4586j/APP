-- 工作报表模块菜单和权限
INSERT INTO sys_permission (id, parent_id, perm_name, perm_code, perm_type, icon, path, component, sort_order, visible)
VALUES
(300, 0, '工作报表', 'work:report', 'menu', 'Memo', '/report', 'Layout', 60, 1);

INSERT INTO sys_permission (id, parent_id, perm_name, perm_code, perm_type, sort_order, visible)
VALUES
(301, 300, '工作计划填写', 'work:plan:create', 'button', 1, 1),
(302, 300, '工作计划查看', 'work:plan:view', 'button', 2, 1),
(303, 300, '工作计划提交', 'work:plan:submit', 'button', 3, 1),
(304, 300, '工作日志填写', 'work:log:create', 'button', 4, 1),
(305, 300, '工作日志查看', 'work:log:view', 'button', 5, 1),
(306, 300, '工作日志提交', 'work:log:submit', 'button', 6, 1),
(307, 300, '报表管理', 'work:report:manage', 'button', 7, 1),
(308, 300, '报表审批', 'work:report:approve', 'button', 8, 1);

-- 所有新权限自动赋给管理员角色
INSERT INTO sys_role_permission (role_id, permission_id)
SELECT 1, id FROM sys_permission WHERE id BETWEEN 300 AND 308;
