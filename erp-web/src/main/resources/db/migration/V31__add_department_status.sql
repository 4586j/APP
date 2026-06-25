-- 给 sys_department 表添加 status 字段，支持启用/禁用
ALTER TABLE sys_department ADD COLUMN status TINYINT DEFAULT 1 COMMENT '1=启用 0=禁用' AFTER sort_order;

-- 更新现有数据为启用状态
UPDATE sys_department SET status = 1 WHERE status IS NULL;
