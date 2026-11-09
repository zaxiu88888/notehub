-- =====================================================================
-- I4 标签体系迁移(可重复执行): tags / question_tags / note_tags + 历史回填
-- 执行: mysql -uroot -p notehub < db/migration/I4_tags.sql
-- =====================================================================
SET NAMES utf8mb4;

CREATE TABLE IF NOT EXISTS tags (
    tag_id     BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    name       VARCHAR(32) NOT NULL COMMENT '标签名',
    created_at DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uk_name (name)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT ='标签';

CREATE TABLE IF NOT EXISTS question_tags (
    question_id BIGINT UNSIGNED NOT NULL,
    tag_id      BIGINT UNSIGNED NOT NULL,
    created_at  DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uk_question_tag (question_id, tag_id),
    KEY idx_tag_question (tag_id, question_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT ='题目-标签';

CREATE TABLE IF NOT EXISTS note_tags (
    note_id    BIGINT UNSIGNED NOT NULL,
    tag_id     BIGINT UNSIGNED NOT NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uk_note_tag (note_id, tag_id),
    KEY idx_tag_note (tag_id, note_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT ='笔记-标签';

-- 回填: 把 questions.exam_point(分号分隔) 拆成题目标签
DROP TEMPORARY TABLE IF EXISTS tmp_tag_seq;
CREATE TEMPORARY TABLE tmp_tag_seq (n INT PRIMARY KEY);
INSERT INTO tmp_tag_seq (n) VALUES (1),(2),(3),(4),(5),(6),(7),(8),(9),(10);

DROP TEMPORARY TABLE IF EXISTS tmp_question_tags;
CREATE TEMPORARY TABLE tmp_question_tags AS
SELECT q.question_id,
       TRIM(SUBSTRING_INDEX(SUBSTRING_INDEX(REPLACE(q.exam_point, '；', ';'), ';', s.n), ';', -1)) AS name
FROM questions q
JOIN tmp_tag_seq s
  ON s.n <= 1 + CHAR_LENGTH(REPLACE(q.exam_point, '；', ';'))
            - CHAR_LENGTH(REPLACE(REPLACE(q.exam_point, '；', ';'), ';', ''))
WHERE q.exam_point IS NOT NULL AND q.exam_point <> '';

DELETE FROM tmp_question_tags WHERE name = '';

INSERT IGNORE INTO tags (name)
SELECT DISTINCT name FROM tmp_question_tags;

INSERT IGNORE INTO question_tags (question_id, tag_id)
SELECT x.question_id, t.tag_id
FROM tmp_question_tags x JOIN tags t ON t.name = x.name;

-- 存量笔记继承所属题目的标签快照
INSERT IGNORE INTO note_tags (note_id, tag_id)
SELECT n.note_id, qt.tag_id
FROM notes n JOIN question_tags qt ON qt.question_id = n.question_id;

DROP TEMPORARY TABLE IF EXISTS tmp_question_tags;
DROP TEMPORARY TABLE IF EXISTS tmp_tag_seq;
