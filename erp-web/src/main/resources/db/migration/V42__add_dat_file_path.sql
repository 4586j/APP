SET NAMES utf8mb4;

-- V42: dat_file 物化路径，加速 WebDAV 共享继承可见性查询
ALTER TABLE `dat_file` ADD COLUMN `path` VARCHAR(768) DEFAULT NULL COMMENT '物化路径，根到自身的 id 链，如 /3/15/42/' AFTER `parent_id`;
CREATE INDEX `idx_path` ON `dat_file` (`path`);

-- 回填：递归 CTE 算出每条记录从根到自身的 id 链
WITH RECURSIVE file_path(id, parent_id, chain) AS (
    SELECT id, parent_id, CONCAT('/', id, '/') FROM dat_file WHERE parent_id IS NULL
    UNION ALL
    SELECT f.id, f.parent_id, CONCAT(fp.chain, f.id, '/')
    FROM dat_file f JOIN file_path fp ON f.parent_id = fp.id
)
UPDATE dat_file f JOIN file_path fp ON f.id = fp.id SET f.path = fp.chain;
