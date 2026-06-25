-- V27: B4 财务与审批 — 补齐字段（additive）
-- 说明：B4 核心表已在 V14/V17 建好，前端已绑定现有列名（currencyFrom/rateDate 等），
--       本迁移只做「加列」，不重命名、不删列，保证已上线前端与 B5/B6 不受影响。
--       MySQL 8 不支持 ADD COLUMN IF NOT EXISTS；DB 当前在 V26，本脚本仅执行一次，列必不存在。
SET NAMES utf8mb4;

-- 1) 汇率表：补 version(乐观锁) + deleted(逻辑删除)，支持历史版本追溯软删除
ALTER TABLE `fin_exchange_rate`
  ADD COLUMN `version` INT NOT NULL DEFAULT 1 COMMENT '乐观锁版本',
  ADD COLUMN `deleted` TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除: 0=正常 1=已删除',
  ADD INDEX `idx_xr_currency_pair` (`currency_from`, `currency_to`, `rate_date`);

-- 2) 应收：补 version；deleted 已存在
ALTER TABLE `fin_receivable`
  ADD COLUMN `version` INT NOT NULL DEFAULT 1 COMMENT '乐观锁版本';

-- 3) 应付：补 version；deleted 已存在
ALTER TABLE `fin_payable`
  ADD COLUMN `version` INT NOT NULL DEFAULT 1 COMMENT '乐观锁版本';

-- 4) 资金审批：补 审批人 / 自动分配的审批角色 / 审批备注 / version
ALTER TABLE `fin_fund_approval`
  ADD COLUMN `approver_role` VARCHAR(64) DEFAULT NULL COMMENT '按金额阈值自动分配的审批角色: dept_manager/finance_director/general_manager',
  ADD COLUMN `approver` BIGINT DEFAULT NULL COMMENT '实际审批人ID',
  ADD COLUMN `approve_comment` VARCHAR(512) DEFAULT NULL COMMENT '审批备注',
  ADD COLUMN `version` INT NOT NULL DEFAULT 1 COMMENT '乐观锁版本';
