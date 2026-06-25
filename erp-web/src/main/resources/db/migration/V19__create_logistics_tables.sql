-- V19: 物流管理建表
CREATE TABLE IF NOT EXISTS log_shipment (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    shipment_no VARCHAR(64) NOT NULL UNIQUE COMMENT '货运编号',
    order_id BIGINT COMMENT '关联订单ID', order_no VARCHAR(64) COMMENT '订单号',
    customer_id BIGINT, customer_name VARCHAR(128),
    method VARCHAR(16) COMMENT 'sea/air/rail/express',
    status VARCHAR(32) DEFAULT 'booked' COMMENT 'booked/loaded/in_transit/customs/arrived/delivered',
    carrier VARCHAR(128) COMMENT '承运人',
    vessel_flight VARCHAR(128) COMMENT '船名/航班号',
    container_no VARCHAR(64) COMMENT '集装箱号',
    seal_no VARCHAR(64) COMMENT '封条号',
    bl_no VARCHAR(64) COMMENT '提单号/空运单号',
    etd DATE COMMENT '预计离港日', eta DATE COMMENT '预计到港日',
    port_loading VARCHAR(128) COMMENT '装货港', port_discharge VARCHAR(128) COMMENT '卸货港',
    gross_weight DECIMAL(12,3) COMMENT '毛重(KG)',
    net_weight DECIMAL(12,3) COMMENT '净重(KG)',
    volume DECIMAL(12,3) COMMENT '体积(CBM)',
    package_count INT COMMENT '件数',
    shipping_marks VARCHAR(512) COMMENT '唛头',
    remark VARCHAR(512),
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted TINYINT DEFAULT 0,
    INDEX idx_status (status), INDEX idx_order (order_id), INDEX idx_etd (etd)
) ENGINE=InnoDB COMMENT='物流货运';

CREATE TABLE IF NOT EXISTS log_tracking (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    shipment_id BIGINT NOT NULL,
    tracking_date DATETIME NOT NULL COMMENT '跟踪时间',
    location VARCHAR(128) COMMENT '地点',
    event_code VARCHAR(32) COMMENT '事件代码',
    description VARCHAR(512) COMMENT '事件描述',
    operator VARCHAR(64) COMMENT '操作人',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_shipment (shipment_id)
) ENGINE=InnoDB COMMENT='物流轨迹';
