-- V17: 应收/应付/结算建表
CREATE TABLE IF NOT EXISTS fin_receivable (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    receipt_no VARCHAR(64) NOT NULL UNIQUE COMMENT '应收编号',
    source_type VARCHAR(32) NOT NULL COMMENT '来源类型: sales_order',
    source_id BIGINT COMMENT '来源ID',
    customer_id BIGINT COMMENT '→ cust_customer.id',
    customer_name VARCHAR(128),
    total_amount DECIMAL(18,2) NOT NULL COMMENT '应收总额',
    received_amount DECIMAL(18,2) DEFAULT 0 COMMENT '已收金额',
    balance DECIMAL(18,2) GENERATED ALWAYS AS (total_amount - received_amount) STORED COMMENT '余额',
    due_date DATE COMMENT '到期日',
    currency VARCHAR(8) DEFAULT 'CNY',
    status VARCHAR(32) DEFAULT 'pending' COMMENT 'pending/partially/received/bad_debt',
    remark VARCHAR(512),
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted TINYINT DEFAULT 0,
    INDEX idx_status (status), INDEX idx_customer (customer_id), INDEX idx_due (due_date)
) ENGINE=InnoDB COMMENT='应收账款';

CREATE TABLE IF NOT EXISTS fin_payable (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    pay_no VARCHAR(64) NOT NULL UNIQUE COMMENT '应付编号',
    source_type VARCHAR(32) NOT NULL COMMENT '来源类型: purchase_order',
    source_id BIGINT COMMENT '来源ID',
    supplier_id BIGINT COMMENT '→ cust_supplier.id',
    supplier_name VARCHAR(128),
    total_amount DECIMAL(18,2) NOT NULL COMMENT '应付总额',
    paid_amount DECIMAL(18,2) DEFAULT 0 COMMENT '已付金额',
    balance DECIMAL(18,2) GENERATED ALWAYS AS (total_amount - paid_amount) STORED COMMENT '余额',
    due_date DATE COMMENT '到期日',
    currency VARCHAR(8) DEFAULT 'CNY',
    status VARCHAR(32) DEFAULT 'pending' COMMENT 'pending/partially/paid',
    remark VARCHAR(512),
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted TINYINT DEFAULT 0,
    INDEX idx_status (status), INDEX idx_supplier (supplier_id), INDEX idx_due (due_date)
) ENGINE=InnoDB COMMENT='应付账款';

CREATE TABLE IF NOT EXISTS fin_settlement (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    settlement_no VARCHAR(64) NOT NULL UNIQUE COMMENT '结算编号',
    direction VARCHAR(16) NOT NULL COMMENT '方向: receipt(收款)/payment(付款)',
    receivable_id BIGINT COMMENT '→ fin_receivable.id',
    payable_id BIGINT COMMENT '→ fin_payable.id',
    related_type VARCHAR(32) COMMENT '关联类型',
    related_id BIGINT COMMENT '关联ID',
    amount DECIMAL(18,2) NOT NULL COMMENT '结算金额',
    payment_method VARCHAR(32) DEFAULT 'bank_transfer' COMMENT '方式: bank_transfer/cash/check/wechat/alipay',
    bank_account VARCHAR(64) COMMENT '银行账号',
    description VARCHAR(512) COMMENT '备注',
    settled_by BIGINT COMMENT '经办人',
    settled_at DATETIME COMMENT '结算日期',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_direction (direction), INDEX idx_receivable (receivable_id), INDEX idx_payable (payable_id)
) ENGINE=InnoDB COMMENT='结算记录';
