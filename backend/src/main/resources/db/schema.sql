-- =====================================================================
-- notehub 数据库结构 v1.1（17 张表: MVP 14 + I4 标签 3）
-- 执行: mysql -uroot -p notehub < schema.sql
-- 约定: 主键 BIGINT UNSIGNED; created_at/updated_at 由数据库维护;
--       不建物理外键(应用层保证一致性, 索引替代); 唯一约束防重复互动
-- =====================================================================
SET NAMES utf8mb4;

-- 1. 用户
CREATE TABLE IF NOT EXISTS users (
    user_id       BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY COMMENT '用户ID',
    account       VARCHAR(20)  NOT NULL COMMENT '登录账号',
    username      VARCHAR(30)  NOT NULL COMMENT '昵称',
    password      VARCHAR(60)  NOT NULL COMMENT 'BCrypt 散列',
    email         VARCHAR(64)  NOT NULL COMMENT '邮箱(验证码注册)',
    gender        TINYINT      NOT NULL DEFAULT 0 COMMENT '0未知 1男 2女',
    birthday      DATE                  DEFAULT NULL,
    avatar_url    VARCHAR(255)          DEFAULT NULL COMMENT '头像URL(经/images映射)',
    school        VARCHAR(64)           DEFAULT NULL,
    signature     VARCHAR(128)          DEFAULT NULL COMMENT '个性签名',
    is_banned     TINYINT      NOT NULL DEFAULT 0 COMMENT '0正常 1封禁',
    is_admin      TINYINT      NOT NULL DEFAULT 0 COMMENT '0普通 1管理员',
    last_login_at DATETIME              DEFAULT NULL,
    created_at    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_account (account),
    UNIQUE KEY uk_email (email),
    KEY idx_banned (is_banned)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT ='用户';

-- 2. 分类(两级)
CREATE TABLE IF NOT EXISTS categories (
    category_id         BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY COMMENT '分类ID',
    name                VARCHAR(32)  NOT NULL COMMENT '分类名',
    parent_category_id  BIGINT UNSIGNED       DEFAULT NULL COMMENT '父分类ID, 顶级为NULL',
    created_at          DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at          DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    KEY idx_parent (parent_category_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT ='题目分类';

-- 3. 题目
CREATE TABLE IF NOT EXISTS questions (
    question_id  BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY COMMENT '题目ID',
    category_id  BIGINT UNSIGNED NOT NULL COMMENT '所属分类',
    title        VARCHAR(128) NOT NULL COMMENT '题目标题',
    description  MEDIUMTEXT   NULL COMMENT '题干描述(Markdown)',
    difficulty   TINYINT      NOT NULL DEFAULT 1 COMMENT '1简单 2中等 3困难',
    exam_point   VARCHAR(128)          DEFAULT NULL COMMENT '考点, 分号分隔',
    view_count   INT UNSIGNED NOT NULL DEFAULT 0 COMMENT '浏览数',
    created_at   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    KEY idx_category (category_id),
    KEY idx_difficulty (difficulty),
    KEY idx_created (created_at)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT ='题目';

-- 4. 题单
CREATE TABLE IF NOT EXISTS question_lists (
    question_list_id BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY COMMENT '题单ID',
    name             VARCHAR(64)  NOT NULL COMMENT '题单名',
    type             TINYINT      NOT NULL DEFAULT 1 COMMENT '1官方推荐 2专题',
    description      VARCHAR(255)          DEFAULT NULL,
    created_at       DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at       DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT ='题单';

-- 5. 题单-题目(sort_rank: rank 是 MySQL8 保留字, 故意避开)
CREATE TABLE IF NOT EXISTS question_list_items (
    question_list_id BIGINT UNSIGNED NOT NULL COMMENT '题单ID',
    question_id      BIGINT UNSIGNED NOT NULL COMMENT '题目ID',
    sort_rank        INT      NOT NULL DEFAULT 0 COMMENT '排序值, 越小越靠前',
    created_at       DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at       DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_list_question (question_list_id, question_id),
    KEY idx_list_rank (question_list_id, sort_rank)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT ='题单条目';

-- 6. 笔记(核心表)
CREATE TABLE IF NOT EXISTS notes (
    note_id       BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY COMMENT '笔记ID',
    author_id     BIGINT UNSIGNED NOT NULL COMMENT '作者',
    question_id   BIGINT UNSIGNED NOT NULL COMMENT '关联题目',
    content       MEDIUMTEXT   NOT NULL COMMENT 'Markdown 内容',
    search_vector MEDIUMTEXT            DEFAULT NULL COMMENT '搜索向量(题目+正文纯文本, ngram 全文索引)',
    like_count    INT UNSIGNED NOT NULL DEFAULT 0 COMMENT '点赞数(原子更新维护)',
    comment_count INT UNSIGNED NOT NULL DEFAULT 0 COMMENT '评论数',
    collect_count INT UNSIGNED NOT NULL DEFAULT 0 COMMENT '收藏数',
    created_at    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    KEY idx_author (author_id),
    KEY idx_question (question_id),
    KEY idx_created (created_at),
    KEY idx_author_created (author_id, created_at),
    FULLTEXT KEY ft_notes_search (search_vector) WITH PARSER ngram
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT ='笔记';

-- 7. 笔记点赞
CREATE TABLE IF NOT EXISTS note_likes (
    note_id    BIGINT UNSIGNED NOT NULL,
    user_id    BIGINT UNSIGNED NOT NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_note_user (note_id, user_id) COMMENT '防重复点赞',
    KEY idx_created (created_at) COMMENT '热度榜窗口聚合'
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT ='笔记点赞';

-- 8. 笔记收藏(扁平收藏记录, 配合收藏夹)
CREATE TABLE IF NOT EXISTS note_collects (
    collect_id BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    note_id    BIGINT UNSIGNED NOT NULL,
    user_id    BIGINT UNSIGNED NOT NULL COMMENT '收藏者',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_note_user (note_id, user_id),
    KEY idx_user (user_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT ='笔记收藏';

-- 9. 收藏夹
CREATE TABLE IF NOT EXISTS collections (
    collection_id BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY COMMENT '收藏夹ID',
    name          VARCHAR(32)  NOT NULL COMMENT '夹名',
    description   VARCHAR(128)          DEFAULT NULL,
    creator_id    BIGINT UNSIGNED NOT NULL COMMENT '创建者',
    created_at    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    KEY idx_creator (creator_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT ='收藏夹';

-- 10. 收藏夹-笔记
CREATE TABLE IF NOT EXISTS collection_notes (
    collection_id BIGINT UNSIGNED NOT NULL,
    note_id       BIGINT UNSIGNED NOT NULL,
    created_at    DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at    DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_collection_note (collection_id, note_id),
    KEY idx_collection (collection_id),
    KEY idx_created (created_at) COMMENT '热度榜窗口聚合'
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT ='收藏夹条目';

-- 11. 评论(两级: parent_id 为空=顶层评论)
CREATE TABLE IF NOT EXISTS comments (
    comment_id  BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY COMMENT '评论ID',
    note_id     BIGINT UNSIGNED NOT NULL COMMENT '所属笔记',
    author_id   BIGINT UNSIGNED NOT NULL COMMENT '评论者',
    parent_id   BIGINT UNSIGNED          DEFAULT NULL COMMENT '父评论ID, 顶层为NULL',
    reply_to_id BIGINT UNSIGNED          DEFAULT NULL COMMENT '楼中楼@的目标评论(同父下定位用)',
    content     VARCHAR(2000) NOT NULL COMMENT '内容',
    like_count  INT UNSIGNED  NOT NULL DEFAULT 0,
    reply_count INT UNSIGNED  NOT NULL DEFAULT 0,
    is_deleted  TINYINT       NOT NULL DEFAULT 0 COMMENT '软删标记',
    created_at  DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at  DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    KEY idx_note (note_id),
    KEY idx_parent (parent_id),
    KEY idx_author (author_id),
    KEY idx_created (created_at) COMMENT '热度榜窗口聚合'
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT ='评论';

-- 12. 评论点赞
CREATE TABLE IF NOT EXISTS comment_likes (
    comment_id BIGINT UNSIGNED NOT NULL,
    user_id    BIGINT UNSIGNED NOT NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_comment_user (comment_id, user_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT ='评论点赞';

-- 13. 站内消息
CREATE TABLE IF NOT EXISTS messages (
    message_id  BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY COMMENT '消息ID',
    receiver_id BIGINT UNSIGNED NOT NULL COMMENT '接收者',
    sender_id   BIGINT UNSIGNED NOT NULL COMMENT '触发者(系统消息=0)',
    type        TINYINT      NOT NULL COMMENT '1点赞 2评论 3系统',
    target_type TINYINT      NOT NULL COMMENT '1笔记 2评论 3用户',
    target_id   BIGINT UNSIGNED NOT NULL COMMENT '目标ID(按 target_type 解释)',
    content     VARCHAR(500)          DEFAULT NULL COMMENT '摘要内容',
    is_read     TINYINT      NOT NULL DEFAULT 0,
    created_at  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    KEY idx_receiver_read (receiver_id, is_read),
    KEY idx_receiver_type (receiver_id, type)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT ='站内消息';

-- 14. 每日统计(管理看板)
CREATE TABLE IF NOT EXISTS statistics (
    id                   BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    login_count          INT UNSIGNED NOT NULL DEFAULT 0 COMMENT '当日登录次数',
    register_count       INT UNSIGNED NOT NULL DEFAULT 0 COMMENT '当日注册数',
    total_register_count INT UNSIGNED NOT NULL DEFAULT 0 COMMENT '累计注册数',
    note_count           INT UNSIGNED NOT NULL DEFAULT 0 COMMENT '当日新增笔记',
    submit_note_count    INT UNSIGNED NOT NULL DEFAULT 0 COMMENT '当日提交笔记人数',
    total_note_count     INT UNSIGNED NOT NULL DEFAULT 0 COMMENT '累计笔记数',
    date                 DATE NOT NULL COMMENT '统计日期',
    created_at           DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at           DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_date (date) COMMENT '每日一条, 定时任务幂等'
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT ='每日统计';

-- 15. 标签(题目/笔记共用词表; 名称唯一, 大小写不敏感由 collation 保证)
CREATE TABLE IF NOT EXISTS tags (
    tag_id     BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    name       VARCHAR(32) NOT NULL COMMENT '标签名',
    created_at DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uk_name (name)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT ='标签';

-- 16. 题目-标签(题目标签由 exam_point 自动同步)
CREATE TABLE IF NOT EXISTS question_tags (
    question_id BIGINT UNSIGNED NOT NULL,
    tag_id      BIGINT UNSIGNED NOT NULL,
    created_at  DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uk_question_tag (question_id, tag_id),
    KEY idx_tag_question (tag_id, question_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT ='题目-标签';

-- 17. 笔记-标签(作者可选, 默认继承题目标签快照)
CREATE TABLE IF NOT EXISTS note_tags (
    note_id    BIGINT UNSIGNED NOT NULL,
    tag_id     BIGINT UNSIGNED NOT NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uk_note_tag (note_id, tag_id),
    KEY idx_tag_note (tag_id, note_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT ='笔记-标签';
