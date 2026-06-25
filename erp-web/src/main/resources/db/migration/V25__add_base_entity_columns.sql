-- B6: Add BaseEntity columns (deleted, version) for MP logical delete
-- V23/V24 already applied, so this is a follow-up ALTER migration
SET NAMES utf8mb4;

ALTER TABLE `dat_upload`
  ADD COLUMN `deleted` TINYINT(1) NOT NULL DEFAULT 0 COMMENT '逻辑删除标记',
  ADD COLUMN `version` INT NOT NULL DEFAULT 1 COMMENT '乐观锁版本',
  ADD COLUMN `updated_by` VARCHAR(50) DEFAULT NULL AFTER `remark`,
  ADD INDEX `idx_deleted` (`deleted`);

ALTER TABLE `dat_pricing_analysis`
  ADD COLUMN `deleted` TINYINT(1) NOT NULL DEFAULT 0 COMMENT '逻辑删除标记',
  ADD COLUMN `version` INT NOT NULL DEFAULT 1 COMMENT '乐观锁版本',
  ADD COLUMN `updated_by` VARCHAR(50) DEFAULT NULL AFTER `remark`,
  ADD INDEX `idx_deleted` (`deleted`);
