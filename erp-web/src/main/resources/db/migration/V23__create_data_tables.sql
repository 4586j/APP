-- B6: Data Upload, Pricing Analysis
SET NAMES utf8mb4;
CREATE TABLE IF NOT EXISTS `dat_upload` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `file_name` VARCHAR(200) NOT NULL,
  `file_type` VARCHAR(50) NOT NULL DEFAULT '',
  `original_name` VARCHAR(500) NOT NULL DEFAULT '',
  `file_size` BIGINT NOT NULL DEFAULT 0,
  `file_path` VARCHAR(500) NOT NULL DEFAULT '',
  `upload_type` VARCHAR(50) NOT NULL DEFAULT 'manual',
  `department` VARCHAR(100) DEFAULT NULL,
  `row_count` INT NOT NULL DEFAULT 0,
  `parsed` TINYINT(1) NOT NULL DEFAULT 0,
  `remark` VARCHAR(500) DEFAULT NULL,
  `created_by` VARCHAR(50) DEFAULT NULL,
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_type` (`file_type`),
  KEY `idx_created_at` (`created_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS `dat_pricing_analysis` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `product_id` BIGINT NOT NULL,
  `title` VARCHAR(200) NOT NULL DEFAULT '',
  `cost_price` DECIMAL(12,2) NOT NULL DEFAULT 0.00,
  `target_price` DECIMAL(12,2) NOT NULL DEFAULT 0.00,
  `competitor_price` DECIMAL(12,2) DEFAULT NULL,
  `suggested_price` DECIMAL(12,2) DEFAULT NULL,
  `margin` DECIMAL(5,2) DEFAULT NULL,
  `market_trend` VARCHAR(500) DEFAULT NULL,
  `analysis_data` TEXT DEFAULT NULL,
  `status` VARCHAR(20) NOT NULL DEFAULT 'draft',
  `remark` VARCHAR(500) DEFAULT NULL,
  `created_by` VARCHAR(50) DEFAULT NULL,
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_product` (`product_id`),
  KEY `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
