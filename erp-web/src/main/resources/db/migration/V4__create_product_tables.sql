-- V4: 产品模块表（prd_*）
-- B2.1 erp-product 模块：分类/HSCode/产品/多币种价格

CREATE TABLE prd_category (
    id          BIGINT        PRIMARY KEY AUTO_INCREMENT,
    parent_id   BIGINT        DEFAULT 0 COMMENT '父分类ID',
    cat_name    VARCHAR(64)   NOT NULL COMMENT '分类名称',
    cat_code    VARCHAR(32)   NOT NULL UNIQUE COMMENT '分类编码',
    sort_order  INT           DEFAULT 0,
    created_at  DATETIME      DEFAULT CURRENT_TIMESTAMP,
    updated_at  DATETIME      DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted     TINYINT       DEFAULT 0
) ENGINE=InnoDB COMMENT='产品分类';

CREATE TABLE prd_hs_code (
    id              BIGINT        PRIMARY KEY AUTO_INCREMENT,
    hs_code         VARCHAR(32)   NOT NULL UNIQUE COMMENT 'HS编码',
    description     VARCHAR(512)  COMMENT '描述',
    tariff_rate     DECIMAL(8,4)  COMMENT '关税税率(%)',
    vat_rate        DECIMAL(8,4)  COMMENT '增值税率(%)',
    export_refund_rate DECIMAL(8,4) COMMENT '出口退税率(%)',
    restrictions    VARCHAR(512)  COMMENT '进出口限制说明',
    created_at      DATETIME      DEFAULT CURRENT_TIMESTAMP,
    updated_at      DATETIME      DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB COMMENT='HS海关编码';

CREATE TABLE prd_product (
    id              BIGINT        PRIMARY KEY AUTO_INCREMENT,
    product_code    VARCHAR(64)   NOT NULL UNIQUE COMMENT '产品编码',
    product_name    VARCHAR(256)  NOT NULL COMMENT '产品名称(中文)',
    product_name_en VARCHAR(256)  COMMENT '产品名称(英文)',
    category_id     BIGINT        COMMENT '分类ID → prd_category',
    hs_code_id      BIGINT        COMMENT '→ prd_hs_code.id',
    hs_code         VARCHAR(32)   COMMENT '冗余HS编码',
    unit            VARCHAR(16)   NOT NULL DEFAULT '件' COMMENT '单位',
    specification   VARCHAR(512)  COMMENT '规格说明',
    origin_country  VARCHAR(64)   COMMENT '原产国',
    brand           VARCHAR(128)  COMMENT '品牌',
    purchase_price  DECIMAL(18,8) COMMENT '采购价(RMB)',
    sales_price     DECIMAL(18,8) COMMENT '销售参考价(USD)',
    cost_price      DECIMAL(18,8) COMMENT '成本价(USD)',
    weight_kg       DECIMAL(12,4) COMMENT '单件重量(kg)',
    volume_cbm      DECIMAL(12,6) COMMENT '单件体积(m³)',
    moq             INT           COMMENT '最小起订量',
    status          TINYINT       DEFAULT 1 COMMENT '1=启用 0=停用',
    created_by      BIGINT,
    updated_by      BIGINT,
    created_at      DATETIME      DEFAULT CURRENT_TIMESTAMP,
    updated_at      DATETIME      DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted         TINYINT       DEFAULT 0,
    INDEX idx_category (category_id),
    INDEX idx_hs_code (hs_code)
) ENGINE=InnoDB COMMENT='产品主表';

CREATE TABLE prd_product_price (
    id            BIGINT        PRIMARY KEY AUTO_INCREMENT,
    product_id    BIGINT        NOT NULL COMMENT '→ prd_product.id',
    price_type    VARCHAR(32)   NOT NULL COMMENT 'purchase|sales|cost|wholesale',
    currency_code VARCHAR(8)    NOT NULL COMMENT '币种: USD/CNY/EUR',
    price         DECIMAL(18,8) NOT NULL COMMENT '价格',
    valid_from    DATE          COMMENT '生效日期',
    valid_to      DATE          COMMENT '失效日期',
    created_at    DATETIME      DEFAULT CURRENT_TIMESTAMP,
    updated_at    DATETIME      DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_product (product_id),
    INDEX idx_currency (currency_code)
) ENGINE=InnoDB COMMENT='产品多币种价格';
