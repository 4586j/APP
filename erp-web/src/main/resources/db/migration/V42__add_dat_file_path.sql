SET NAMES utf8mb4;

-- V42: dat_file 物化路径，加速 WebDAV 共享继承可见性查询
-- 幂等：path 列与 idx_path 索引可能已由先前失败的执行创建，重复执行不报错。

-- 1. 加列（若不存在）
SET @col_exists = (SELECT COUNT(*) FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'dat_file' AND COLUMN_NAME = 'path');
SET @sql = IF(@col_exists = 0,
    'ALTER TABLE `dat_file` ADD COLUMN `path` VARCHAR(768) DEFAULT NULL COMMENT ''物化路径，根到自身的 id 链，如 /3/15/42/'' AFTER `parent_id`',
    'DO 0');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- 2. 索引（若不存在）
SET @idx_exists = (SELECT COUNT(*) FROM information_schema.STATISTICS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'dat_file' AND INDEX_NAME = 'idx_path');
SET @sql = IF(@idx_exists = 0,
    'CREATE INDEX `idx_path` ON `dat_file` (`path`)',
    'DO 0');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- 3. 回填：递归 CTE 算出每条记录从根到自身的 id 链。
--    关键修正：种子行用 CAST(... AS CHAR(1000)) 显式给定 CTE chain 列长度，
--    否则 MySQL 按种子推断出很短的 VARCHAR，层级稍深即 "Data too long for column 'chain'"(1406)。
WITH RECURSIVE file_path(id, parent_id, chain) AS (
    SELECT id, parent_id, CAST(CONCAT('/', id, '/') AS CHAR(1000)) FROM dat_file WHERE parent_id IS NULL
    UNION ALL
    SELECT f.id, f.parent_id, CONCAT(fp.chain, f.id, '/')
    FROM dat_file f JOIN file_path fp ON f.parent_id = fp.id
)
UPDATE dat_file f JOIN file_path fp ON f.id = fp.id SET f.path = fp.chain;
