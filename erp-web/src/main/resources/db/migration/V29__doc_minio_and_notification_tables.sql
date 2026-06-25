-- V29: B6 收尾 — 单证 MinIO 字段补全 + 通知中心建表
SET NAMES utf8mb4;

-- 1. doc_document 增加 MinIO 对象存储字段
ALTER TABLE doc_document
    ADD COLUMN minio_object_key VARCHAR(512) COMMENT 'MinIO 对象 key' AFTER file_size,
    ADD COLUMN content_type     VARCHAR(128) COMMENT 'MIME 类型' AFTER minio_object_key;
-- file_size 字段 V21 已存在，此处不重复添加

-- 2. 通知主表（继承 BaseEntity：含 created_by/updated_by/version 审计字段）
CREATE TABLE IF NOT EXISTS ntf_notification (
    id          BIGINT       PRIMARY KEY COMMENT '雪花ID',
    title       VARCHAR(256) NOT NULL COMMENT '标题',
    content     TEXT         COMMENT '内容',
    type        VARCHAR(16)  NOT NULL DEFAULT 'system' COMMENT 'system/approval/business',
    source_type VARCHAR(32)  COMMENT '业务来源类型，如 approval_request',
    source_id   BIGINT       COMMENT '业务来源ID',
    created_at  DATETIME     DEFAULT CURRENT_TIMESTAMP,
    updated_at  DATETIME     DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    created_by  BIGINT,
    updated_by  BIGINT,
    version     INT          DEFAULT 0,
    deleted     TINYINT      DEFAULT 0,
    INDEX idx_type (type), INDEX idx_source (source_type, source_id)
) ENGINE=InnoDB COMMENT='通知主表';

-- 3. 用户通知关联表（每个接收人一条已读状态）
CREATE TABLE IF NOT EXISTS ntf_user_notification (
    id              BIGINT   PRIMARY KEY COMMENT '雪花ID',
    notification_id BIGINT   NOT NULL COMMENT '→ ntf_notification.id',
    user_id         BIGINT   NOT NULL COMMENT '接收人用户ID',
    is_read         TINYINT  DEFAULT 0 COMMENT '0=未读 1=已读',
    read_at         DATETIME COMMENT '已读时间',
    created_at      DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at      DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    created_by      BIGINT,
    updated_by      BIGINT,
    version         INT      DEFAULT 0,
    deleted         TINYINT  DEFAULT 0,
    INDEX idx_user_read (user_id, is_read), INDEX idx_notification (notification_id)
) ENGINE=InnoDB COMMENT='用户通知关联';
