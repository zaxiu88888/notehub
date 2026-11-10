-- =====================================================================
-- I5 笔记每日互动统计(可重复执行): note_daily_stats + 历史回填
-- 执行: mysql -uroot -p notehub < db/migration/I5_interaction_stats.sql
-- =====================================================================
SET NAMES utf8mb4;

CREATE TABLE IF NOT EXISTS note_daily_stats (
    note_id       BIGINT UNSIGNED NOT NULL,
    stat_date     DATE            NOT NULL,
    like_count    INT UNSIGNED    NOT NULL DEFAULT 0,
    comment_count INT UNSIGNED    NOT NULL DEFAULT 0,
    collect_count INT UNSIGNED    NOT NULL DEFAULT 0,
    updated_at    DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (note_id, stat_date),
    KEY idx_date (stat_date)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT ='笔记每日互动统计';

-- 回填点赞(按当前剩余点赞的创建日)
INSERT INTO note_daily_stats (note_id, stat_date, like_count)
SELECT note_id, DATE(created_at), COUNT(*)
FROM note_likes
GROUP BY note_id, DATE(created_at)
ON DUPLICATE KEY UPDATE like_count = VALUES(like_count);

-- 回填有效评论
INSERT INTO note_daily_stats (note_id, stat_date, comment_count)
SELECT note_id, DATE(created_at), COUNT(*)
FROM comments
WHERE is_deleted = 0
GROUP BY note_id, DATE(created_at)
ON DUPLICATE KEY UPDATE comment_count = VALUES(comment_count);

-- 回填收藏条目(按收藏者去重)
INSERT INTO note_daily_stats (note_id, stat_date, collect_count)
SELECT cn.note_id, DATE(cn.created_at), COUNT(DISTINCT c.creator_id)
FROM collection_notes cn
JOIN collections c ON cn.collection_id = c.collection_id
GROUP BY cn.note_id, DATE(cn.created_at)
ON DUPLICATE KEY UPDATE collect_count = VALUES(collect_count);
