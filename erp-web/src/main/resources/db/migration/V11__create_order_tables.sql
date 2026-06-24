-- V11: 订单模块建表（销售单+采购单+明细+状态历史）
CREATE TABLE IF NOT EXISTS ord_sales_order (
    id                  BIGINT        PRIMARY KEY AUTO_INCREMENT,
    order_no            VARCHAR(64)   NOT NULL UNIQUE COMMENT '订单号 SO-yyyyMMdd-NNNN',
    customer_id         BIGINT        COMMENT '→ cust_customer.id',
    customer_order_no   VARCHAR(128)  COMMENT '客户PO号',
    order_date          DATE          NOT NULL COMMENT '下单日期',
    currency            VARCHAR(8)    DEFAULT 'USD' COMMENT '币种',
    trade_terms         VARCHAR(16)   COMMENT 'FOB/CIF/EXW/DDP',
    payment_terms       VARCHAR(32)   COMMENT 'TT_30_70/LC_SIGHT等',
    port_loading        VARCHAR(128)  COMMENT '起运港',
    port_destination    VARCHAR(128)  COMMENT '目的港',
    expected_delivery   DATE          COMMENT '预计交期',
    total_amount        DECIMAL(18,2) COMMENT '原币总额',
    total_cny_amount    DECIMAL(18,2) COMMENT '折合人民币',
    exchange_rate       DECIMAL(10,6) COMMENT '汇率',
    remarks             TEXT          COMMENT '备注',
    status              VARCHAR(32)   NOT NULL DEFAULT 'draft' COMMENT 'draft/submitted/approved/purchasing/shipping/delivered/settled/cancelled',
    created_by          BIGINT,
    updated_by          BIGINT,
    created_at          DATETIME      DEFAULT CURRENT_TIMESTAMP,
    updated_at          DATETIME      DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted             TINYINT       DEFAULT 0,
    INDEX idx_customer (customer_id), INDEX idx_status (status), INDEX idx_order_date (order_date)
) ENGINE=InnoDB COMMENT='销售订单';

CREATE TABLE IF NOT EXISTS ord_sales_order_item (
    id            BIGINT        PRIMARY KEY AUTO_INCREMENT,
    order_id      BIGINT        NOT NULL COMMENT '→ ord_sales_order.id',
    line_no       INT           COMMENT '行号',
    product_id    BIGINT        COMMENT '→ prd_product.id',
    product_code  VARCHAR(64)   COMMENT '冗余产品编码',
    product_name  VARCHAR(256)  COMMENT '冗余产品名称',
    hs_code       VARCHAR(32)   COMMENT 'HS编码',
    specification VARCHAR(512)  COMMENT '规格',
    quantity      DECIMAL(12,2) NOT NULL COMMENT '数量',
    unit          VARCHAR(16)   COMMENT '单位',
    unit_price    DECIMAL(18,6) COMMENT '单价',
    total_price   DECIMAL(18,2) COMMENT '金额',
    created_at    DATETIME      DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_order (order_id)
) ENGINE=InnoDB COMMENT='销售订单明细';

CREATE TABLE IF NOT EXISTS ord_purchase_order (
    id                  BIGINT        PRIMARY KEY AUTO_INCREMENT,
    order_no            VARCHAR(64)   NOT NULL UNIQUE COMMENT '采购单号 PO-yyyyMMdd-NNNN',
    supplier_id         BIGINT        COMMENT '→ cust_supplier.id',
    related_sales_order_id BIGINT     COMMENT '关联销售单',
    order_date          DATE          NOT NULL,
    expected_delivery   DATE          COMMENT '预计到货',
    total_amount        DECIMAL(18,2) COMMENT '总额',
    currency            VARCHAR(8)    DEFAULT 'CNY',
    payment_terms       VARCHAR(64),
    remarks             TEXT,
    status              VARCHAR(32)   DEFAULT 'draft' COMMENT 'draft/confirmed/shipping/received/cancelled',
    created_by          BIGINT,
    updated_by          BIGINT,
    created_at          DATETIME      DEFAULT CURRENT_TIMESTAMP,
    updated_at          DATETIME      DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted             TINYINT       DEFAULT 0,
    INDEX idx_supplier (supplier_id), INDEX idx_status (status)
) ENGINE=InnoDB COMMENT='采购订单';

CREATE TABLE IF NOT EXISTS ord_purchase_order_item (
    id                  BIGINT        PRIMARY KEY AUTO_INCREMENT,
    order_id            BIGINT        NOT NULL COMMENT '→ ord_purchase_order.id',
    line_no             INT,
    product_id          BIGINT,
    product_code        VARCHAR(64),
    product_name        VARCHAR(256),
    specification       VARCHAR(512),
    quantity            DECIMAL(12,2) NOT NULL,
    unit                VARCHAR(16),
    unit_price          DECIMAL(18,6),
    total_price         DECIMAL(18,2),
    related_sales_item_id BIGINT     COMMENT '关联销售单明细',
    created_at          DATETIME      DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_order (order_id)
) ENGINE=InnoDB COMMENT='采购订单明细';

CREATE TABLE IF NOT EXISTS ord_status_history (
    id            BIGINT        PRIMARY KEY AUTO_INCREMENT,
    order_type    VARCHAR(16)   NOT NULL COMMENT 'sales/purchase',
    order_id      BIGINT        NOT NULL,
    from_status   VARCHAR(32),
    to_status     VARCHAR(32)   NOT NULL,
    operator      BIGINT        COMMENT '操作人',
    remark        VARCHAR(256),
    created_at    DATETIME      DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_order (order_type, order_id)
) ENGINE=InnoDB COMMENT='订单状态变更历史';
