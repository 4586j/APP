-- V14: 财务与审批模块建表
CREATE TABLE IF NOT EXISTS fin_exchange_rate (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    currency_from VARCHAR(8) NOT NULL COMMENT '源币种',
    currency_to VARCHAR(8) NOT NULL COMMENT '目标币种',
    rate DECIMAL(12,6) NOT NULL COMMENT '汇率',
    rate_date DATE NOT NULL COMMENT '汇率日期',
    source VARCHAR(64) COMMENT '数据来源',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_date (rate_date), INDEX idx_currency (currency_from)
) ENGINE=InnoDB COMMENT='汇率管理';

CREATE TABLE IF NOT EXISTS fin_fund_approval (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    request_no VARCHAR(64) NOT NULL UNIQUE COMMENT '申请编号',
    title VARCHAR(256) NOT NULL COMMENT '申请标题',
    fund_type VARCHAR(32) NOT NULL COMMENT '类型: purchase/expense/prepay',
    amount DECIMAL(18,2) NOT NULL COMMENT '金额',
    currency VARCHAR(8) DEFAULT 'CNY',
    supplier_id BIGINT COMMENT '→ cust_supplier.id',
    description TEXT COMMENT '说明',
    status VARCHAR(32) DEFAULT 'pending' COMMENT 'pending/approved/rejected/paid',
    applicant BIGINT COMMENT '申请人',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted TINYINT DEFAULT 0,
    INDEX idx_status (status), INDEX idx_type (fund_type)
) ENGINE=InnoDB COMMENT='资金审批';

CREATE TABLE IF NOT EXISTS app_workflow (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    workflow_code VARCHAR(64) NOT NULL UNIQUE,
    workflow_name VARCHAR(128) NOT NULL,
    target_type VARCHAR(32) COMMENT 'sales_order/purchase_order/fund_approval',
    description VARCHAR(512),
    status TINYINT DEFAULT 1,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB COMMENT='审批工作流定义';

CREATE TABLE IF NOT EXISTS app_workflow_node (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    workflow_id BIGINT NOT NULL,
    node_order INT NOT NULL COMMENT '审批顺序',
    node_name VARCHAR(128),
    approver_role VARCHAR(64) COMMENT '审批人角色',
    min_amount DECIMAL(18,2) COMMENT '最小金额阈值',
    max_amount DECIMAL(18,2) COMMENT '最大金额阈值',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_workflow (workflow_id)
) ENGINE=InnoDB COMMENT='审批节点';

CREATE TABLE IF NOT EXISTS app_approval_request (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    request_no VARCHAR(64) NOT NULL UNIQUE COMMENT '审批编号',
    workflow_id BIGINT,
    target_type VARCHAR(32) NOT NULL COMMENT '业务类型',
    target_id BIGINT COMMENT '业务ID',
    title VARCHAR(256) NOT NULL,
    amount DECIMAL(18,2),
    currency VARCHAR(8) DEFAULT 'CNY',
    status VARCHAR(32) DEFAULT 'pending' COMMENT 'pending/approved/rejected',
    applicant BIGINT,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_status (status), INDEX idx_target (target_type,target_id)
) ENGINE=InnoDB COMMENT='审批请求';

CREATE TABLE IF NOT EXISTS app_approval_history (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    request_id BIGINT NOT NULL,
    node_id BIGINT,
    approver BIGINT COMMENT '审批人',
    action VARCHAR(32) NOT NULL COMMENT 'approved/rejected',
    comment VARCHAR(512),
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_request (request_id)
) ENGINE=InnoDB COMMENT='审批历史';
