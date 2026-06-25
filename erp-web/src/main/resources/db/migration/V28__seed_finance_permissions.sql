-- V28: B4 权限码补齐（对齐规范 finance:xxx:view / approval:request:approve）并授予 ROLE_ADMIN
-- 说明：V15/V18 已有旧短码(exchange-rate:view 等)，前端在用，保留不动；
--       本脚本新增「规范前缀」权限码，与新加的接口(@PreAuthorize)对齐，并全部授予 admin。
SET NAMES utf8mb4;

SET @menu_fin = (SELECT id FROM sys_permission WHERE perm_code = 'finance' AND perm_type = 'menu' LIMIT 1);

-- 汇率 finance:exchange-rate:*
INSERT IGNORE INTO sys_permission (parent_id, perm_name, perm_code, perm_type, sort_order, visible) VALUES
(@menu_fin, '汇率查看', 'finance:exchange-rate:view',   'button', 110, 1),
(@menu_fin, '汇率录入', 'finance:exchange-rate:create', 'button', 111, 1),
(@menu_fin, '汇率删除', 'finance:exchange-rate:delete', 'button', 112, 1),
-- 应收 finance:receivable:*
(@menu_fin, '应收查看',   'finance:receivable:view',    'button', 120, 1),
(@menu_fin, '应收录入',   'finance:receivable:create',  'button', 121, 1),
(@menu_fin, '应收修改',   'finance:receivable:update',  'button', 122, 1),
(@menu_fin, '应收删除',   'finance:receivable:delete',  'button', 123, 1),
(@menu_fin, '应收确认收款','finance:receivable:confirm', 'button', 124, 1),
-- 应付 finance:payable:*
(@menu_fin, '应付查看',   'finance:payable:view',       'button', 130, 1),
(@menu_fin, '应付录入',   'finance:payable:create',     'button', 131, 1),
(@menu_fin, '应付修改',   'finance:payable:update',     'button', 132, 1),
(@menu_fin, '应付删除',   'finance:payable:delete',     'button', 133, 1),
(@menu_fin, '应付确认付款','finance:payable:confirm',    'button', 134, 1),
-- 资金审批 finance:fund:*
(@menu_fin, '资金申请查看', 'finance:fund:view',         'button', 140, 1),
(@menu_fin, '资金申请提交', 'finance:fund:create',       'button', 141, 1),
(@menu_fin, '资金审批操作', 'finance:fund:approve',      'button', 142, 1);

-- 通用审批引擎 approval:* （独立顶级菜单）
INSERT IGNORE INTO sys_permission (parent_id, perm_name, perm_code, perm_type, sort_order, visible, icon, path) VALUES
(0, '审批中心', 'approval', 'menu', 6, 1, 'Stamp', '/approval/pending');
SET @menu_app = (SELECT id FROM sys_permission WHERE perm_code = 'approval' AND perm_type = 'menu' LIMIT 1);

INSERT IGNORE INTO sys_permission (parent_id, perm_name, perm_code, perm_type, sort_order, visible) VALUES
-- 工作流配置
(@menu_app, '工作流查看', 'approval:workflow:view',   'button', 150, 1),
(@menu_app, '工作流配置', 'approval:workflow:manage', 'button', 151, 1),
-- 审批请求
(@menu_app, '审批请求查看', 'approval:request:view',    'button', 160, 1),
(@menu_app, '审批请求提交', 'approval:request:submit',  'button', 161, 1),
(@menu_app, '审批操作',     'approval:request:approve', 'button', 162, 1);

-- 全部新码授予 ROLE_ADMIN
SET @admin_role = (SELECT id FROM sys_role WHERE role_code = 'ROLE_ADMIN' LIMIT 1);
INSERT IGNORE INTO sys_role_permission (role_id, permission_id)
SELECT @admin_role, id FROM sys_permission
WHERE perm_code LIKE 'finance:%' OR perm_code LIKE 'approval%';
