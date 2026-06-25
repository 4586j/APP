SET NAMES utf8mb4;
INSERT IGNORE INTO `sys_permission` (`perm_code`, `perm_name`, `perm_type`, `parent_id`, `path`, `icon`, `sort_order`, `visible`) VALUES
('data', '数据中心', 'MENU', NULL, '/data/upload', 'DataAnalysis', 70, 1);
SET @data_id = (SELECT id FROM sys_permission WHERE perm_code='data' LIMIT 1);
INSERT IGNORE INTO `sys_permission` (`perm_code`, `perm_name`, `perm_type`, `parent_id`, `path`, `icon`, `sort_order`, `visible`) VALUES
('data:upload:create', '上传文件', 'BUTTON', @data_id, NULL, NULL, 1, 1),
('data:upload:delete', '删除上传', 'BUTTON', @data_id, NULL, NULL, 2, 1),
('data:pricing', '定价分析', 'MENU', @data_id, '/data/pricing', NULL, 3, 1),
('data:pricing:create', '新建分析', 'BUTTON', @data_id, NULL, NULL, 4, 1),
('data:pricing:update', '修改分析', 'BUTTON', @data_id, NULL, NULL, 5, 1),
('data:pricing:delete', '删除分析', 'BUTTON', @data_id, NULL, NULL, 6, 1),
('dashboard', '仪表盘', 'MENU', NULL, '/dashboard', 'DataBoard', 5, 1);
