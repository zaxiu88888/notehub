-- =====================================================================
-- I6 每日打卡(可重复执行)
-- 执行: mysql -uroot -p notehub < db/migration/I6_checkins.sql
-- =====================================================================
SET NAMES utf8mb4;

CREATE TABLE IF NOT EXISTS user_checkins (
    user_id      BIGINT UNSIGNED NOT NULL,
    checkin_date DATE            NOT NULL,
    created_at   DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (user_id, checkin_date),
    KEY idx_date (checkin_date)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT ='用户每日打卡';
