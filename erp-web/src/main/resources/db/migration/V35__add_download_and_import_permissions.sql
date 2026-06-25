-- ============================================================================
-- V35: 添加数据下载和定价分析批量导入权限码
-- ============================================================================

SET @data_id = (SELECT id FROM sys_permission WHERE perm_code='data' LIMIT 1);

INSERT INTO sys_permission (id, parent_id, perm_name, perm_code, perm_type, icon, path, component, sort_order, visible) VALUES
(224, @data_id, '下载文件', 'data:upload:download', 'BUTTON', NULL, NULL, NULL, 7, 1),
(225, @data_id, '批量导入分析', 'data:pricing:import', 'BUTTON', NULL, NULL, NULL, 8, 1);

-- 绑定给 ROLE_ADMIN
INSERT INTO sys_role_permission (role_id, permission_id)
SELECT 1, id FROM sys_permission WHERE id IN (224, 225);
