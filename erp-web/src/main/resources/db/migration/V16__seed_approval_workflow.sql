-- V16: 审批工作流 seed 数据
INSERT IGNORE INTO app_workflow (workflow_code, workflow_name, target_type, description) VALUES
('WF-FUND-DEFAULT', '资金审批(默认)', 'fund_approval', '资金审批默认流程'),
('WF-ORDER-DEFAULT', '销售订单审批(默认)', 'sales_order', '销售订单默认流程');
INSERT IGNORE INTO app_workflow_node (workflow_id, node_order, node_name, approver_role, min_amount, max_amount) VALUES
(1, 1, '一级审批(≤5万)', 'manager', 0, 50000),
(1, 2, '二级审批(5万~20万)', 'director', 50000.01, 200000),
(1, 3, '三级审批(>20万)', 'boss', 200000.01, 999999999),
(2, 1, '一级审批(≤10万)', 'manager', 0, 100000),
(2, 2, '二级审批(>10万)', 'boss', 100000.01, 999999999);
