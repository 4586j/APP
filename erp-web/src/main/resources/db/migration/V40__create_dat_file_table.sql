SET NAMES utf8mb4;

-- ============================================================================
-- V40: 企业网盘 — dat_file 自引用树结构
-- 替代原有的扁平 dat_upload 表，支持无限目录层级
-- ============================================================================

-- 文件/文件夹表（自引用树结构）
CREATE TABLE IF NOT EXISTS `dat_file` (
    `id`            BIGINT        PRIMARY KEY AUTO_INCREMENT,
    `parent_id`     BIGINT        DEFAULT NULL COMMENT '父文件夹ID，NULL=根目录',
    `is_directory`  TINYINT       NOT NULL DEFAULT 0 COMMENT '0=文件 1=文件夹',
    `name`          VARCHAR(256)  NOT NULL COMMENT '文件/文件夹名',
    `display_name`  VARCHAR(256)  DEFAULT NULL COMMENT '展示名（可重命名）',
    `extension`     VARCHAR(32)   DEFAULT NULL COMMENT '扩展名，如 .docx .xlsx',
    `mime_type`     VARCHAR(128)  DEFAULT NULL COMMENT 'MIME 类型',
    `file_size`     BIGINT        DEFAULT 0 COMMENT '文件大小（字节）',
    `storage_path`  VARCHAR(1024) DEFAULT NULL COMMENT '物理存储路径（文件时有效）',
    `file_type`     VARCHAR(64)   DEFAULT NULL COMMENT '业务分类：market_data/analysis_code/shared_doc',
    `department`    VARCHAR(64)   DEFAULT NULL COMMENT '共享部门名（兼容旧数据）',
    `dept_id`       BIGINT        DEFAULT NULL COMMENT '所属部门ID',
    `row_count`     INT           DEFAULT 0 COMMENT '数据行数（Excel 类）',
    `parsed`        TINYINT       DEFAULT 0 COMMENT '是否已解析',
    `remark`        VARCHAR(512)  DEFAULT NULL COMMENT '备注',
    `created_by`    BIGINT        DEFAULT NULL COMMENT '创建人',
    `created_at`    DATETIME      DEFAULT CURRENT_TIMESTAMP,
    `updated_by`    BIGINT        DEFAULT NULL COMMENT '更新人',
    `updated_at`    DATETIME      DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `deleted`       TINYINT       DEFAULT 0 COMMENT '0=正常 1=删除',
    INDEX `idx_parent` (`parent_id`),
    INDEX `idx_dept` (`dept_id`),
    INDEX `idx_created_by` (`created_by`),
    INDEX `idx_deleted` (`deleted`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='企业网盘文件/文件夹（自引用树结构）';

-- 共享部门表（多对多）
CREATE TABLE IF NOT EXISTS `dat_file_share` (
    `id`          BIGINT    PRIMARY KEY AUTO_INCREMENT,
    `file_id`     BIGINT    NOT NULL COMMENT '文件/文件夹ID → dat_file.id',
    `dept_id`     BIGINT    NOT NULL COMMENT '共享部门ID → sys_department.id',
    `created_at`  DATETIME  DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY `uk_file_dept` (`file_id`, `dept_id`),
    INDEX `idx_dept` (`dept_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='网盘文件/文件夹共享部门表';
