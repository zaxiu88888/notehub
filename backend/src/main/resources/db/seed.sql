-- =====================================================================
-- notehub 种子数据 v1.0
-- 执行(在 schema.sql 之后): mysql -uroot -p notehub < seed.sql
-- 默认账号: admin  / admin123   (管理员)
--           demo   / user123456 (普通用户)
-- =====================================================================
SET NAMES utf8mb4;

-- ---------- 用户 ----------
INSERT INTO users (account, username, password, email, is_admin, signature) VALUES
('admin', '站长', '$2a$10$wvelxA1eMxBq5U4t0C5v1uuivenu2SnuRpo/71vjdKLmIolN11bOu', 'admin@notehub.dev', 1, 'notehub 管理员'),
('demo',  '刷题小白', '$2a$10$m52AnOMgPHhFRzyuALMIWO3KN.O4qBHKji3/QcOTb89wOyJ6gaI8.', 'demo@notehub.dev', 0, '每天进步一点点');

-- ---------- 一级分类 ----------
INSERT INTO categories (category_id, name, parent_category_id) VALUES
(1, '数据结构与算法', NULL),
(2, '计算机基础', NULL);

-- ---------- 二级分类 ----------
INSERT INTO categories (category_id, name, parent_category_id) VALUES
(10, '数组与双指针', 1),
(11, '哈希表',        1),
(12, '链表',          1),
(13, '栈与队列',      1),
(14, '二叉树',        1),
(15, '动态规划',      1),
(20, '操作系统',      2),
(21, '计算机网络',    2);

-- ---------- 题目 ----------
INSERT INTO questions (category_id, title, description, difficulty, exam_point) VALUES
(10, '两数之和 II - 输入有序数组', '给定一个已按非递减顺序排列的整数数组 numbers，请从数组中找出两个数满足相加之和等于目标数 target，返回下标（从 1 开始）。', 1, '数组;双指针;二分查找'),
(11, '两数之和', '给定整数数组 nums 和目标值 target，找出和为目标值的两个整数下标。', 1, '数组;哈希表'),
(12, '反转链表', '给你单链表的头节点 head，请你反转链表，并返回反转后的链表。', 1, '链表;递归;迭代'),
(13, '有效的括号', '给定一个只包括 ( ) { } [ ] 的字符串 s，判断字符串是否有效。', 1, '栈;字符串'),
(14, '二叉树的层序遍历', '给你二叉树的根节点 root，返回其节点值自底向上的层序遍历结果。', 2, '二叉树;BFS;队列'),
(15, '爬楼梯', '假设你正在爬楼梯，需要 n 阶你才能到达楼顶，每次可以爬 1 或 2 个台阶，问有多少种不同的方法。', 1, '动态规划;记忆化搜索'),
(20, '进程与线程的区别', '简述进程与线程的概念及其区别，并说明多线程的适用场景。', 2, '操作系统;并发'),
(21, 'TCP 三次握手与四次挥手', '描述 TCP 建立连接的三次握手与断开连接的四次挥手过程，并解释为什么需要三次握手。', 2, '网络;TCP');

-- ---------- 题单 ----------
INSERT INTO question_lists (question_list_id, name, type, description) VALUES
(1, '新手热身 10 题', 1, '从数组、哈希、栈开始的热身题单');

INSERT INTO question_list_items (question_list_id, question_id, sort_rank) VALUES
(1, 2, 1), (1, 1, 2), (1, 4, 3), (1, 3, 4), (1, 6, 5), (1, 5, 6);
