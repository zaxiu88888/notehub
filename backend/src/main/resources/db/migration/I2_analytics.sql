-- =====================================================================
-- I2 数据可视化迁移(只执行一次): 热力图/热度榜所需索引
-- 执行: mysql -uroot -p notehub < db/migration/I2_analytics.sql
-- =====================================================================
SET NAMES utf8mb4;

-- 热力图: 按作者 + 时间范围聚合
ALTER TABLE notes ADD INDEX idx_author_created (author_id, created_at);

-- 热度榜窗口聚合: 互动表按时间过滤
ALTER TABLE note_likes ADD INDEX idx_created (created_at);
ALTER TABLE comments ADD INDEX idx_created (created_at);
ALTER TABLE collection_notes ADD INDEX idx_created (created_at);
