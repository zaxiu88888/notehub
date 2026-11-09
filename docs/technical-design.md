# notehub 技术设计文档

> 版本：v1.0 ｜ 日期：2026-11-06 ｜ 状态：定稿
> 原则：真实可交付优先。每项选型都要能回答"为什么"，避免教学项目式的堆砌。

## 1. 系统架构

```
┌─────────────────────────────────────────────────┐
│  frontend (Vite + React18 + TS + Antd5)         │
│  apps/user ── 用户端 6 页面                       │
│  apps/admin ── 管理端 5 页面                      │
│  domain/* ── 按业务域组织 api/service/components  │
└──────────────────┬──────────────────────────────┘
                   │ HTTP (axios, Bearer JWT)
┌──────────────────▼──────────────────────────────┐
│  backend (Spring Boot 3.x, :8080)               │
│                                                 │
│  Filter链: TraceIdFilter                         │
│  Interceptor: TokenInterceptor (解析JWT→RequestScope)
│  AOP: @NeedLogin / @NeedAdmin 环绕校验            │
│                                                 │
│  Controller ── Service(接口+impl) ── Mapper(XML) │
│                                        │        │
│  event: MessageEvent(点赞/评论→消息, 事件解耦)      │
│  task: EmailTaskConsumer(3s轮询) / DailyStatistics(23:59)
└───────┬──────────────┬───────────────┬──────────┘
        │              │               │
   ┌────▼────┐   ┌─────▼─────┐   ┌─────▼─────┐
   │ MySQL 8 │   │ Redis 7   │   │ 本地磁盘   │
   │ 14 张表  │   │ 缓存/队列/限流│  │ uploads/  │
   └─────────┘   └───────────┘   └───────────┘
```

## 2. 技术选型与版本

| 层 | 选型 | 版本 | 理由 |
|---|---|---|---|
| 语言/运行时 | Java | 21 LTS（本机已装 E:\jdk 21） | Boot 3.x 官方支持，虚拟线程可用 |
| 框架 | Spring Boot | 3.3.x / 3.4.x（创建时取最新稳定） | 主流稳定线；javax→jakarta |
| Web | spring-boot-starter-web | 随 BOM | MVC 足够，不上 WebFlux |
| 安全 | spring-boot-starter-security + JJWT | 0.12.x 新 API | Security 只做 CORS/BCrypt/放行，鉴权自研（同原项目模式，可控易讲）；0.12 修了 0.9 的 API 缺陷与安全问题 |
| 持久层 | MyBatis (mybatis-spring-boot-starter) | 3.0.x | XML SQL 可控、对齐原项目便于对照；不引入 MyBatis-Plus（减少魔法，面试讲得清） |
| 数据库 | MySQL | 8.0.41（已装） | — |
| 缓存 | spring-boot-starter-data-redis (Lettuce) | 随 BOM | 默认 Lettuce 优于 Jedis（原项目用 Jedis） |
| 邮件 | spring-boot-starter-mail + QQ SMTP | 随 BOM | 真发验证码 |
| 定时 | spring @Scheduled | 随 BOM | 单机规模够用，不引 quartz |
| 日志 | Log4j2（排除 logback） | 随 BOM | 异步日志、对齐原项目 |
| 校验 | hibernate-validator (jakarta) | 随 BOM | 注解式参数校验 |
| Markdown | ~~flexmark-all~~ | — | 2026-11-08 决策: 批量导入改行级解析(更可控可测), Flexmark 移除; 如后续需要 AST 场景再引入 |
| 分词 | jieba-analysis | 1.0.2 | 搜索迭代期(I1)使用，MVP 不引 |
| 构建 | Maven | 3.9.11（E:\Maven） | — |
| 前端框架 | React + TypeScript | 18.3 / 5.x | 对齐原项目 |
| UI | Ant Design | 5.x | 组件全，管理后台省力 |
| 状态 | Redux Toolkit | 2.x | — |
| 路由 | react-router-dom | 6.x | — |
| HTTP | axios | 1.7+ | 比原项目自研 FetchClient 更主流 |
| 编辑器 | cherry-markdown | 0.8.x | 原项目同款，Markdown 编辑体验好 |
| 图表 | echarts (迭代期) | 5.x | 热力图/统计看板 |
| 测试 | JUnit5 + Spring Boot Test / Vitest | — | 测试门要求 |

## 3. 后端包结构（com.notehub）

```
com.notehub
├── NotehubApplication.java
├── common/            # 与业务无关的地基
│   ├── ApiResponse<T>          # {code, message, data}
│   ├── Pagination / PageVO<T>  # 分页
│   ├── ErrorCode               # 错误码枚举（见 §7）
│   ├── BusinessException       # 业务异常（带 ErrorCode）
│   ├── GlobalExceptionHandler  # @RestControllerAdvice
│   ├── RequestScope            # 请求级上下文(userId/login/isAdmin)
│   └── TraceIdFilter           # MDC traceId
├── config/
│   ├── SecurityConfig          # 放行 /api/**、CORS、BCrypt
│   ├── WebConfig               # 拦截器注册、/images/** 静态映射
│   ├── RedisConfig             # RedisTemplate 序列化
│   └── ScheduleConfig          # @EnableScheduling
├── security/
│   ├── JwtUtil                 # 0.12 API：解析/签发/校验
│   ├── TokenInterceptor        # Authorization: Bearer → RequestScope
│   ├── NeedLogin / NeedAdmin   # 注解
│   └── AuthAspect              # 环绕校验两注解
├── controller/        # 13 个（见 api-contract.md）
├── service/ + impl/   # 17 个接口
├── mapper/            # 14 个接口 + resources/mapper/*.xml
├── model/
│   ├── entity/  # 14 张表对应
│   ├── dto/     # 请求体（@Valid 校验）
│   ├── vo/      # 响应体
│   └── enums/   # RedisKey、MessageType 等
├── event/
│   ├── MessageEvent           # 应用事件
│   └── MessageEventListener   # @EventListener 异步写 messages 表
└── task/
    ├── EmailTaskConsumer      # Redis List 队列，fixedDelay 3s
    └── DailyStatistics        # cron 23:59 统计落库
```

**与原项目（卡码笔记）的关键差异**：admin 接口补 `@NeedAdmin`（原项目裸奔）；事件有真监听者（原项目只定义未使用）；删除遗留废表 note_comment。

## 4. 数据库设计（首版 14 张表，MySQL 8，utf8mb4）

> 完整 DDL 见 `backend/src/main/resources/db/schema.sql`（E0 阶段执行）。

| 表 | 关键列 | 索引/约束 |
|---|---|---|
| users | user_id PK, account UK, email UK, username, password(BCrypt), gender, birthday, avatar_url, school, signature, is_banned, is_admin, last_login_at | uk_account, uk_email |
| categories | category_id PK, name, parent_category_id | idx_parent |
| questions | question_id PK, category_id FK, title, difficulty(1/2/3), exam_point, view_count | idx_category, idx_difficulty |
| question_lists | question_list_id PK, name, type, description | — |
| question_list_items | question_list_id, question_id, rank | uk(list,question), idx_list_rank |
| notes | note_id PK, author_id, question_id, content(MEDIUMTEXT), like_count, comment_count, collect_count | idx_author, idx_question, idx_created |
| note_likes | note_id, user_id | **uk(note,user)**（防重复点赞） |
| note_collects | collect_id PK, note_id, user_id | uk(note,user) |
| collections | collection_id PK, name, description, creator_id | idx_creator |
| collection_notes | collection_id, note_id | uk(collection,note) |
| comments | comment_id PK, note_id, author_id, parent_id(可空), content, like_count, reply_count | idx_note, idx_parent |
| comment_likes | comment_id, user_id | uk(comment,user) |
| messages | message_id PK, receiver_id, sender_id, type(1赞/2评/3系统), target_id, target_type, content, is_read | idx_receiver_read |
| statistics | id PK, login_count, register_count, total_register_count, note_count, submit_note_count, total_note_count, date UK | uk_date |

**设计要点**：
- 计数列（like_count 等）冗余存储，更新用原子 `UPDATE ... SET x = x + 1 WHERE ...`（answer Issue #1646 教训）；唯一约束防重复点赞/收藏
- 所有表 `created_at / updated_at` 由 SQL `DEFAULT CURRENT_TIMESTAMP / ON UPDATE` 维护
- 不设物理外键（应用层保证一致性，便于分表迁移），加索引替代
- 预留（迭代期加）：`tags` + `note_tags`（I4）、`notes.search_vector` FULLTEXT ngram（I1）、邮件开关配置走 yaml 不进表

## 5. 核心流程设计

### 5.1 注册 / 登录 / JWT 链路
```
注册: POST /api/email/verify-code?email=
  ├─ 校验 60s 限流（Redis key email:limit:{email}, TTL 60s）
  ├─ 生成 6 位随机码 → EmailTask JSON LPUSH 到 queue:email:task
  └─ EmailTaskConsumer(3s) RPOP → JavaMailSender 发信 → 验证码存 Redis(TTL 5min)
POST /api/users {email, code, account, password}
  ├─ 比对 Redis 验证码 → 删除(一次性)
  ├─ 唯一性校验(account/email) → BCrypt 加密 → 入库
登录: POST /api/users/login → BCrypt.matches → 签发 JWT(HS256, userId, TTL 30d)
请求: TokenInterceptor 解 Bearer → RequestScope(userId/login/isAdmin)
鉴权: @NeedLogin / @NeedAdmin AOP 环绕，未过直接返回 ApiResponse(错误码)
```
- **邮件开关**：`mail.verify-code.enabled=false` 时 dev 模式验证码写日志并直接返回（无 SMTP 也能开发），`true` 走真邮件
- JWT secret 从 `application-local.yaml` 读，禁止硬编码（原项目 abc123 写在 yaml 入库——反面教材）

### 5.2 笔记列表聚合（防 N+1，原项目核心亮点，照做）
```
1. notes 主表分页查询
2. 收集 authorIds/questionIds/noteIds 去重
3. 批量查: userMap / questionMap / 我赞过的Set / 我收藏过的Set
4. 内存组装 NoteVO(笔记+作者摘要+题目摘要+用户行为) → PageVO
```
全程 SQL 次数 = 5 + 常数，与页大小无关。

### 5.3 互动与消息（事件解耦）
```
点赞: POST /api/like/note/{id} → INSERT note_likes(uk 防重) → 原子 UPDATE notes.like_count+1
     → 若非自赞 → publish MessageEvent(LIKE) → @EventListener 异步 INSERT messages
评论: 同模式，父评论存在才允许回复；回复数维护 reply_count
未读数: GET /api/messages/unread/count（前端 30s 轮询；I3 换 WebSocket 真推送）
```

### 5.4 题目批量导入（行级模板解析）
```
POST /api/admin/questions/batch {categoryId, markdown}
→ QuestionMarkdownParser 行级解析(# 标题/难度：/考点：/题干)
→ 逐题校验(标题必填、难度行必填) → 合法项批量 INSERT, 失败项明细返回 {total, success, failures}
→ @Transactional: 校验类失败逐条剔除; DB 级异常整体回滚
```
> 变更记录(2026-11-08): 模板是固定行协议, 行级解析比 Flexmark AST 更可控可测, 故弃用 Flexmark;
> Flexmark 从依赖中移除, 预留给后续笔记摘要/导出场景(§2 选型表同步)。

### 5.5 每日统计（管理看板数据源）
23:59 `@Scheduled` 聚合 6 项指标（当日登录/注册/累计注册/当日笔记/当日提交人数/累计笔记）INSERT statistics（uk_date 幂等）。

## 6. 前端设计

```
frontend/src/
├── apps/
│   ├── user/    # 布局(NavBar+侧栏) + 6 页面: home/question/questionSet/questionList/userCenter/userHome + message
│   └── admin/   # 布局 + 5 页面: dashBroad/adminCategory/adminQuestion/adminQuestionList/adminUser
├── domain/{user,question,note,comment,collection,message,category,statistic}/
│   ├── api/     # 接口定义（[method, url] 元组表）
│   ├── service/ # 调用封装
│   ├── components/
│   └── types/
├── request/     # axios 实例：拦截器注入 token、统一解 {code,message,data}、401 跳登录
├── store/       # RTK: userSlice(token/用户信息), appSlice(全局 UI 态)
└── base/        # 通用组件: NotFound/TimeAgo/分页器
```
路由：`/`首页(题目+笔记流) `/question/:id` `/question-set` `/question-list` `/user-center/*` `/user/:id` `/messages`；admin 挂 `/admin/*` 并做 isAdmin 守卫。

## 7. 错误码体系（common/ErrorCode）

| code | 含义 | HTTP |
|---|---|---|
| 200 | 成功 | 200 |
| 40001 | 参数校验失败 | 200 |
| 40101 | 未登录 | 200 |
| 40102 | 需要管理员权限 | 200 |
| 40103 | 账号或密码错误 | 200 |
| 40301 | 无权操作该资源 | 200 |
| 40401 | 资源不存在 | 200 |
| 40901 | 唯一性冲突(账号/邮箱已存在) | 200 |
| 41001 | 验证码错误或过期 | 200 |
| 41002 | 发送验证码过于频繁 | 200 |
| 42901 | 请求过于频繁 | 200 |
| 40902 | 并发冲突(死锁重试耗尽, 可重试) | 200 |
| 40500/41500 | 方法/媒体类型不支持 | 405 / 415 |
| 50000 | 服务器内部错误 | 500 |

统一响应：`{code, message, data}`；分页响应 data 含 `{list, page, pageSize, total}`。

## 5.6 搜索设计（I1）

```
写: 笔记创建/编辑 → search_vector = 题目标题 + 正文(纯文本)
库: notes.search_vector + FULLTEXT INDEX ... WITH PARSER ngram
读: 关键词 → SearchUtils(Jieba 分词/去 Markdown 符号/去重/截断) → MATCH ... AGAINST(... IN NATURAL LANGUAGE MODE)
缓存: Redis "search:notes:{keywordHash}:{page}:{size}" → {ids,total}, TTL 5min
     缓存内容与用户无关; VO 每次按当前用户批量补 myActions(不缓存个性化字段)
用户搜索: username/account LIKE, 不缓存, 邮箱脱敏
```

> 说明: ngram 负责中文 bigram 索引, Jieba 负责查询侧分词与噪声清理, 两者互补;
> 写入路径已维护向量, 历史数据由 `db/migration/I1_search.sql` 回填。

## 8. 安全设计清单
- 密码 BCrypt（强度默认 10）；JWT HS256 + 30 天过期 + local.yaml 密钥
- 所有 admin 接口 `@NeedAdmin`；资源写操作校验归属（owner/40301）
- 防重：点赞/收藏唯一约束兜底，接口幂等
- 限流：验证码 60s/次（Redis）；登录失败不锁（MVP 规模）
- SQL 全参数化（MyBatis #{}），无拼接
- 上传：仅图片后缀白名单 + 大小限制 5MB，文件名 UUID 重写，存本地 uploads/ 经 /images/** 访问
- CORS 白名单 `http://localhost:5173`
