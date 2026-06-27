SET NAMES utf8mb4;

-- ============================================================================
-- V39: 数据上传部门隔离 + 多共享部门
-- 规则：上级可见下级，同级不可见，被共享的部门可见，自己上传的始终可见
-- ============================================================================

-- 上传记录增加上传者部门ID（用于层级隔离）
ALTER TABLE `dat_upload`
    ADD COLUMN `dept_id` BIGINT DEFAULT NULL COMMENT '上传者所属部门ID' AFTER `department`,
    ADD INDEX `idx_dept_id` (`dept_id`);

-- 上传记录共享部门表（多对多）
CREATE TABLE IF NOT EXISTS `dat_upload_dept_share` (
    `id`          BIGINT    PRIMARY KEY AUTO_INCREMENT,
    `upload_id`   BIGINT    NOT NULL COMMENT '上传记录ID → dat_upload.id',
    `dept_id`     BIGINT    NOT NULL COMMENT '共享部门ID → sys_department.id',
    `created_at`  DATETIME  DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY `uk_upload_dept` (`upload_id`, `dept_id`),
    INDEX `idx_dept` (`dept_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='上传记录共享部门表';
