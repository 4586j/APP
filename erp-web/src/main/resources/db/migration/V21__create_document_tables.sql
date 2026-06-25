-- V21: 单证管理建表
CREATE TABLE IF NOT EXISTS doc_document (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    doc_no VARCHAR(64) NOT NULL UNIQUE COMMENT '单证编号',
    doc_type VARCHAR(32) NOT NULL COMMENT 'invoice/packing_list/bl/co/contract',
    order_id BIGINT, order_no VARCHAR(64),
    shipment_id BIGINT,
    title VARCHAR(256) NOT NULL,
    status VARCHAR(16) DEFAULT 'draft' COMMENT 'draft/final/archived',
    template_code VARCHAR(64) COMMENT '模板代码',
    file_path VARCHAR(512) COMMENT '文件路径(MinIO)',
    file_name VARCHAR(256) COMMENT '文件名',
    file_size BIGINT COMMENT '文件大小(字节)',
    generated_by BIGINT COMMENT '生成人',
    generated_at DATETIME,
    remark VARCHAR(512),
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted TINYINT DEFAULT 0,
    INDEX idx_type (doc_type), INDEX idx_order (order_id), INDEX idx_status (status)
) ENGINE=InnoDB COMMENT='单证';

CREATE TABLE IF NOT EXISTS doc_document_version (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    document_id BIGINT NOT NULL,
    version_no INT NOT NULL COMMENT '版本号',
    file_path VARCHAR(512) COMMENT '文件路径',
    file_name VARCHAR(256) COMMENT '文件名',
    file_size BIGINT COMMENT '文件大小',
    created_by BIGINT COMMENT '上传人',
    remark VARCHAR(512),
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_document (document_id)
) ENGINE=InnoDB COMMENT='单证版本';
