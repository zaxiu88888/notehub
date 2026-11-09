-- =====================================================================
-- I1 搜索能力迁移(可重复执行): notes.search_vector + ngram 全文索引
-- 执行: mysql -uroot -p notehub < db/migration/I1_search.sql
-- =====================================================================
SET NAMES utf8mb4;

-- 1) 搜索向量列(题干 + 笔记正文, 写入时由应用维护)
ALTER TABLE notes ADD COLUMN search_vector MEDIUMTEXT NULL COMMENT '搜索向量(题目+正文纯文本)';

-- 2) ngram 全文索引(中文按 bigram 切分; 查询端再用 Jieba 预处理关键词)
ALTER TABLE notes ADD FULLTEXT INDEX ft_notes_search (search_vector) WITH PARSER ngram;

-- 3) 回填历史数据(题目标题 + 笔记正文)
UPDATE notes n
JOIN questions q ON n.question_id = q.question_id
SET n.search_vector = CONCAT(q.title, '\n', n.content)
WHERE n.search_vector IS NULL;
