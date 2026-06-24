-- V13: 移除 order_no 的 UNIQUE 约束（软删除导致唯一键冲突）
ALTER TABLE ord_sales_order DROP INDEX order_no;
ALTER TABLE ord_purchase_order DROP INDEX order_no;
-- 改为普通索引
ALTER TABLE ord_sales_order ADD INDEX idx_order_no (order_no);
ALTER TABLE ord_purchase_order ADD INDEX idx_order_no (order_no);
