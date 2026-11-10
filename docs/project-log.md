# notehub 项目日志

> 每干一步追加一条。格式：日期 ｜ 做了什么 ｜ 测试/自审结论 ｜ commit ｜ 遗留问题











---

## [RETEST-2] 2026-11-10 ｜ 遗留问题收口（列表评论/AI 模型/周任务/搜索噪声）

**做了什么**
- 列表页评论入口：新增 `NoteCommentsDrawer` 共用组件，接入首页/搜索/他人主页/我的笔记/收藏夹；首页笔记流评论数改为可点击按钮；评论增删后刷新当前列表（题目页仍按单篇局部刷新，不卸载抽屉）
- AI 模型联调：先用本地 OpenAI 兼容 mock（9099）验证协议/缓存/限流/降级；再用用户提供的 Key 接入 DeepSeek `deepseek-chat` 真实联调（`ai.summary.base-url=https://api.deepseek.com/v1`），验证真实摘要、6h 缓存、仅模型生成 5 次/分钟、缓存命中不限流；Key 仅环境变量注入，未写入仓库/文档
- 每周孤儿清理：临时把 `upload.cleanup-cron` 调为 5 秒，放入 mtime 2 天前的无引用图片，确认首个周期删除；随后恢复默认 cron 重启
- 搜索噪声：natural language 改 BOOLEAN MODE 逐词加引号短语匹配，过滤 ngram 跨词 bigram 低分命中

**测试门**: 后端 **207/207**；前端 **41/41**；构建通过；I1 **34/34**（新增噪声用例）、I6 **32/32**（恢复本地摘要路径）；AI mock **18/18** + DeepSeek 真实模型 **13/13**；周任务实触发删除 1 个孤儿文件
**自审门（diff 自审清单）**
1. ✅ 评论抽屉共用组件不会造成 effect 循环：CommentsPanel effect 只依赖 page/noteId，onCountChange 变化不触发重载；列表页用现有 load 刷新，题目页用 getNote 局部替换
2. ✅ 首页评论区改 Button 保留原视觉（text/small/12px）并可键盘访问
3. ✅ 布尔查询对每个分词加引号并剔除残留双引号，空白输入返回空串；SQL 仍参数化
4. ✅ AI mock 仅用于测试，未改生产默认配置；DeepSeek 联调同样只通过环境变量注入 Key，仓库内无密钥；联调后按需恢复默认 AI 关闭
5. ✅ 周任务 cron 覆盖仅启动参数，未改仓库配置；孤儿文件为临时复制，已随任务删除
6. ✅ 数据清理：users=2/notes=2/questions=10/lists=2/checkins=0/orphan_tags=0，uploads 恢复 4 个既有文件
**commit**: 8d3da8e（列表评论入口）、44171ba（搜索短语匹配）、文档同步（本次提交）
**遗留问题**
1. 跨用户笔记计数不实时、封禁已有 WS 连接不断开、多实例 L1 30s（均属既有设计/限制）

---

## [RETEST] 2026-11-10 ｜ 全量复测（I1–I6 专项 + 44 项回归 + 浏览器 E2E）

**做了什么**
- 基线复现：后端 `mvn test` 198/198、前端 `npm test` 38/38、`npm run build` 通过；确认 8080/5173 旧进程不是 HEAD 代码后重启，前端重启 Vite 排除 HMR 陈旧
- I1 搜索 33 项：中文/标题检索、空/纯符号/超长/注入、分页、用户邮箱脱敏、相关度顺序、缓存键、缓存命中、笔记增删改后失效、myActions
- I2 数据 40 项：热力图回填/增量/删除回退/位图字节/闰年、top3、周月窗口与总榜、limit/period、Markdown 导出越权/404/未登录
- I3 实时 21 项：CONNECT、双标签页、点赞/评论未读、已读清零、HTTP 一致、无/非法 token、越权订阅、SEND、封禁用户 HTTP+WS
- I4 标签 28 项：考点拆分、双维度筛选、继承/覆盖/清空、大小写/# 归一、5 个与 20 字上限、题目标签重建、快照不回溯、删除守卫、孤立标签
- I5 深化 37 项：每日赞评藏增减、原始发生日回退、跨日级联删评论、滑动窗口 30/31、题库/分类/标签缓存失效、图片/头像清理、stats 边界
- I6 打卡/AI 32 项：打卡幂等/连击/recent、AI 本地摘要/缓存 TTL、连续 8 次不限流、编辑删除失效、404/未登录
- API 回归：仓库外 `acceptance.mjs` 44/44（auth 切面修复后复跑）
- 浏览器 E2E：登录/首页标签筛选/题单/写笔记（标签+AI）/点赞收藏评论/消息实时角标/搜索页/个人中心统计+打卡+导出/普通用户 403/管理端 5 页面

**测试门**: 后端 **206/206**；前端 **39/39**；构建通过；I1–I6 真实环境 **191/191**；历史验收 **44/44**
**冒烟门**: 上述全部真实 MySQL+Redis 执行；测试数据已清理（users=2/notes=2/questions=10/lists=2/checkins=0/orphan_tags=0），Redis 测试键已删除
**自审门（diff 自审清单）**
1. 🐛 P1：级联删除跨日父评论 500 → 先取存活回复按各自 `created_at` 回退；根因是 UNSIGNED 列 `count + (-2)` 先下穿，所有计数更新补 `CAST(... AS SIGNED)`（notes/comments/note_daily_stats）
2. 🐛 P2：搜索相关度顺序丢失 → `selectByIds` 后按 ID 顺序还原; 缓存键 `hashCode` 碰撞 → SHA-256; 笔记写后旧缓存 → Redis 版本号入键，afterCommit 自增并 try/catch（Redis 故障不影响主流程）
3. 🐛 P2：`/notes/download` 未登录 500 → AuthAspect 由“返回 ApiResponse”改为抛 BusinessException（ResponseEntity 接口强转根因）
4. 🐛 P2：评论后卡片评论数不刷新 → 单独 `getNote` 局部替换，不整页 reload；自审确认未引入 effect 循环（回调 useCallback 稳定、effect 仅依赖 page/noteId）
5. 🐛 P2：封禁用户旧 JWT 仍可建 WS → CONNECT 查 users；只拒绝新连接，既有连接不断开（已记录）
6. ✅ 自审修正：SearchService 缓存键只计算一次（避免 read/write 期间版本变化产生跨版本写）；WS 封禁判断先 `user==null` 再读 `isBanned`；测试 SQL 输出 CRLF 归一
7. ⚠️ 未覆盖：AI 真模型、每周孤儿 cron、列表页评论入口、搜索低分 bigram 噪声、跨用户实时笔记计数
**commit**: bba25d0（I5）、5ac9b5c（I1）、eda8e39（auth）、171869b（评论数）、db6f5e8（WS 封禁）
**遗留问题**
1. 列表页（首页/搜索/他人主页/收藏夹）评论按钮未接抽屉，P3
2. AI 真模型未联调；`AI_SUMMARY_API_KEY` 配置后需补 5 次/分钟实测
3. 每周一 03:30 孤儿清理未等到真实触发，单测/代码审查覆盖

---

## [I6-opt] 2026-11-10 ｜ I6 优化(缓存免限流/并发合并/本地摘要/打卡轨迹)

**做了什么**
- AI 限流: 只有真正调用模型才消耗 5/min; 缓存命中与本地摘要不限流(避免重复查看被限)
- AI 并发: 同一笔记缓存未命中用内存锁 + 双重检查合并, 只生成/调用一次; 本地缓存(10min)遇到模型已配置会自动升级重生成
- 本地摘要: 新增 `LocalSummarizer` 抽取式摘要(去 Markdown/分句/关键词打分/原文顺序取 3 句), 替代简单截断
- AI 结果截断 1000 字, 防超长输出
- 打卡: `status` 增加近 7 天 `recent[{date,checked}]`; 个人中心打卡卡展示 7 个圆点轨迹
- 文档: api-contract §18 / technical-design §5.11 同步

**测试门**: 后端 198/198; 前端 38/38; 构建通过
**冒烟门**: AI 连续 8 次调用均放行(本地摘要/缓存不计限流), 摘要内容正确; 打卡 status recent=7、昨天已打卡/今天未打卡、打卡后今天变绿、连击 2; 浏览器个人中心显示近 7 天圆点, AI 弹窗正常; 测试数据/缓存已清理
**自审门**: ① 并发锁 finally 移除, 双重检查避免重复生成; ② 本地缓存升级路径单测覆盖; ③ 打卡 recent 用 HashSet 判存在, 7 天含今天; ④ 前端 recent 圆点带 aria-label 便于测试/无障碍
**commit**: (本次提交)
---

## [I6] 2026-11-10 ｜ 拓展(AI 题解总结 + 每日打卡 streak)

**做了什么**
- AI 总结: `POST /notes/{id}/ai-summary`(登录, 5/min); `AiChatClient` 支持 OpenAI 兼容接口(配置 `ai.summary.enabled/api-key`, Key 走 `AI_SUMMARY_API_KEY` 环境变量); 未配置/调用失败自动降级 `MarkdownUtils` 本地摘要; 结果缓存 `ai:summary:{noteId}` 6h, 笔记编辑/删除后 afterCommit 失效
- 前端: 笔记卡片新增「AI 总结」按钮 + 弹窗(loading/source 标签/本地摘要提示)
- 打卡: 新表 `user_checkins`(PK user+date 去重); `POST /api/checkin` 幂等 + `GET /api/checkin/status`; 连击复用 StreakUtils; 个人中心数据统计页新增打卡卡片(连续/累计/最长 + 今日打卡按钮)
- DB: schema v1.3(19 表); 迁移 `I6_checkins.sql` 已执行

**测试门**: 后端 194/194; 前端 38/38; 构建通过
**冒烟门**: AI 本地摘要内容正确/缓存命中/不存在笔记 40401/第 6 次 42901; 打卡昨天已有→今日未打卡且连击存活/打卡后连击 2 天/重复打卡幂等; 浏览器: 个人中心打卡按钮切换「今日已打卡」+ toast, 题目页 AI 弹窗展示本地摘要; 测试打卡/缓存/限流数据已清理
**自审门**: ① AI 限流断言按"第 6 次"修正(前 5 次含缓存与 404 调用, 非代码缺陷); ② 笔记编辑/删除同步失效 AI 缓存, 走 afterCommit; ③ 打卡状态用主键幂等, 连击算法复用既有 StreakUtils; ④ Vite 长时间运行 HMR 再次漏更新, 重启后验证(已记入 HANDOFF)
**已知**: 未用真实模型 Key 联调(默认本地摘要路径已覆盖); 本地摘要为抽取式, 非生成式; 打卡按服务端本地日期
**commit**: (本次提交)
---

## [I5-opt] 2026-11-10 ｜ I5 优化(击穿保护/滑动窗口/发生日回退/批量扫描)

**做了什么**
- 缓存击穿: `TwoLevelCacheService` 改用 `Caffeine.get(key, loader)` 原子回源, 同一 key 并发未命中只查一次库; 新增 8 线程并发单测
- 限流: 固定窗口 → Redis ZSET + Lua 滑动窗口(Redis TIME), 窗口内精确 limit; 移除 INCR+PEXPIRE 实现
- 每日统计: 取消点赞/删评论/取消收藏时按原始发生日回退(新增 NoteLike/CollectionNote 查询, 收藏夹删除按条目日期)
- 上传清理: 周任务一次取全量正文/头像构建引用集合, 替代逐文件两次全表 LIKE
- 文档: technical-design §5.10 / api-contract §17 / README 同步

**测试门**: 后端 185/185; 前端 36/36; 构建通过
**冒烟门**: I5 全量脚本复跑全过（限流 30 放行/第 31 次 42901 在滑动窗口下仍成立; 统计/缓存失效/图片与头像清理均正常）; 测试数据已还原
**自审门**: ① 并发单测验证 Caffeine 只回源一次; ② 删除收藏夹改为读取条目(含 created_at), 同步修正日志变量; ③ 滑动窗口 member 加 UUID, 防同毫秒覆盖; ④ 统计回退对 null 时间兜底为今天
**commit**: (本次提交)
---

## [I5] 2026-11-10 ｜ 深化(多级缓存 / 限流 / 计数去重统计 / 上传清理)

**做了什么**
- 多级缓存: `TwoLevelCacheService` = L1 Caffeine(30s, 1000) → L2 Redis(2~5min) → DB; 接入题库列表/分类树/热门标签; 写操作按域失效(版本号+1)
- 限流: `@RateLimit` + AOP + Redis Lua(INCR+首次 PEXPIRE); 登录 10/min、注册 5/min、搜索 30/min、发笔记 10/min、评论 20/min、点赞 60/min、上传 20/min; 匿名按 IP; Redis 故障 fail-open
- 计数去重: 新表 `note_daily_stats`(PK note+date), 点赞/评论/收藏原子 upsert; 热度榜 week/month 改聚合该表; `db/migration/I5_interaction_stats.sql` 回填历史
- 上传清理: `ImageCleanupService` 提取 Markdown/HTML 图片, 引用计数为 0 才删; 笔记删除/编辑(afterCommit)、换头像即时清理; 每周一 03:30 扫描 24h 前孤儿文件
- DB: schema v1.2(18 表); 通知前端无需改动(42901 走既有错误提示)

**测试门**: 后端 183/183; 前端 36/36; 构建通过
**冒烟门(真实环境)**: 标签缓存写/删后失效可见 / note1 历史统计回填 1赞3评1藏 / 点赞与评论写入当日统计并归零 / 热度榜 hotScore=13(改用每日统计) / 搜索第31次 42901 / 含图笔记删除后图片文件清理 / 临时用户换头像旧文件清理且新头像可访问; 测试用户/消息/统计/孤儿标签/限流 key 已清理
**自审门**: 修复/加固 4 处 — ① 图片清理移到事务 afterCommit, 防回滚误删; ② 缓存失效统一改为提交后执行(AfterCommit), 防并发读回填旧值; ③ 删除图片后静态资源 404 被全局异常包装为 HTTP 200+40401, 冒烟断言按业务码修正(非缺陷); ④ 冒烟时发现 Docker/Redis 未启动, 限流 fail-open 与邮件 500 符合设计预期, 启动容器后全过
**已知**: 多级缓存版本号在单机内存, 多实例时其他节点 L1 最长滞后 30s; 固定窗口在边界可瞬时双倍; note_daily_stats 记录"当日净互动", 删旧评论计入删除日; 早于 24h 的无引用图片由周任务清理
**commit**: (本次提交)
---

## [I4] 2026-11-09 ｜ 标签体系(tags + question_tags/note_tags 双维度索引)

**做了什么**
- DB: 新增 tags/question_tags/note_tags(17 表); `db/migration/I4_tags.sql` 可重复执行, 从 questions.exam_point 回填题目标签并让存量笔记继承; seed.sql 新装环境同步回填
- 后端: Tag 实体/VO/Mapper + TagService(upsert 名称唯一, 大小写不敏感; 题目标签由考点全量重建; 笔记标签默认继承题目); `GET /api/tags` 热门标签(含题/笔记数)
- 筛选: `GET /questions?tag=` 与 `GET /notes?tag=` 走 EXISTS + (tag_id, owner_id) 索引; NoteVO/QuestionVO 批量补 tags, 与页大小无关
- 写入: 题目 create/update/batch 同步题目标签; 笔记 create 不传 tags 继承题目, update 传 null 保留/空数组清空; 删除笔记清关联
- 前端: 首页题目标签筛选(支持 `/?tag=` 深链)、最新笔记标签筛选与标签展示; 笔记卡片/题目页展示标签; 笔记编辑器标签选择(≤5, 可输入新标签, 预填题目标签)
- 文档: api-contract §16 / technical-design §5.9 / README 路线图 / 表数量 17

**测试门**: 后端 161/161; 前端 36/36; 构建通过
**冒烟门(真实环境)**: tags 热门列表计数正确 / 题目 tag=哈希表 命中 2 题且排除反转链表 / 笔记 tag=哈希表 命中 1 篇 / 新建笔记继承 [链表,递归,迭代] / 改标签后可检索且旧标签不再命中 / 6 个标签 40001 / 管理员建题考点“链表；队列;链表”去重同步 / 改考点重建 [图,DFS] / 删题后孤立标签不出现在热门列表 / 删除临时笔记后总数还原 2; 浏览器: `/?tag=哈希表` 过滤、笔记卡片标签、题目页标签链接、编辑器预填标签均正常
**自审门**: 修复 3 处 — ① NoteEditorModal 默认参数 `initialTags=[]` 每次渲染新建数组, effect 死循环(测试挂起暴露) → 提取稳定 EMPTY_TAGS; ② 本地 Vite dev server 长时间运行后 HMR 漏更新, 浏览器仍跑中间版本代码 → 重启 dev server 后复验(环境问题, 已记入 HANDOFF 踩坑); ③ 标签筛选与批量 VO 填充确认均只增加 1 条 SQL, 无 N+1; SQL 全参数化、标签渲染由 React 转义
**已知**: 删除题目/笔记后孤立标签保留(不展示, 不占热门榜); 题目标签变更后存量笔记标签为快照, 不回溯覆盖(避免冲掉作者自定义)
**commit**: (本次提交)
---

## [I3] 2026-11-09 ｜ 实时通知(WebSocket STOMP 替代 30s 轮询)

**做了什么**
- 后端: `spring-boot-starter-websocket`; `/ws` STOMP 端点(无 SockJS); `StompAuthChannelInterceptor` 在 CONNECT 帧校验 JWT 并绑定 Principal, 只允许 `/user/queue/**` 订阅、拒绝客户端 SEND
- 推送: `UnreadPushService` → `convertAndSendToUser(userId, "/queue/unread", {total,like,comment,system})`; 触发点 = 新消息落库(InteractionEventListener) + 单条/批量/全部已读 + 删除(MessageController)
- 配置: `app.ws.allowed-origins` 来源白名单; 前端 Vite `/ws` 代理(ws:true)
- 前端: `@stomp/stompjs` 客户端(5s 自动重连); `notificationSlice` 统一维护未读角标; 布局移除 30s 轮询, 连接/重连后 HTTP 补拉一次; 消息页已读/删除同步 store
- 测试: 后端 +9(Stomp 鉴权 5 / 推送 2 / 监听 2) = 153; 前端 +5(notificationSlice 3 / socket 2) = 34

**测试门**: 后端 153/153; 前端 34/34; 构建通过
**冒烟门(真实环境)**: 原生 STOMP 脚本 — 合法连接 CONNECTED / admin 点赞后 demo 实时收到 total+1 / 全部已读推送 total=0 / 无 token 连接 ERROR / 越权订阅 ERROR; 浏览器 — Vite 代理下角标实时 +1; 打开第二个标签页后, 全部已读推送使两页角标同时清零; 测试消息/点赞/已读状态已还原种子数据
**自审门**: 修复 3 处 — ① 无 token CONNECT 在 mock 返回 0 时未被拒(测试暴露), 增加 `userId <= 0` 防御; ② 鉴权失败会无限重连 → `onStompError` 主动 deactivate; ③ 消息页与布局各自持有未读状态, 已读后角标可能滞后 → 统一收敛到 notificationSlice
**已知**: 只推送计数不推送消息体(消息列表仍需进入页面拉取); 未读数每次推送查 3 条 count SQL(单机规模可接受, I5 可合并)
**commit**: (本次提交)
---

## [I2] 2026-11-09 ｜ 数据可视化(热力图 + 热度榜 + 数据卡片 + 笔记导出)

**做了什么**
- 后端 4 接口: `GET /notes/heatmap`(Redis 位图, 每天 u8, 事务提交后事件增量维护, 读未命中从库回填, Redis 故障降级) / `GET /notes/ranklist`(week/month/all, hotScore=赞*2+评*3+藏*2, 缓存 5min) / `GET /notes/top3count`(笔记/赞评藏/字数/活跃天数/连续提交) / `GET /notes/download`(Markdown 附件, 全部或单篇)
- 新增 NoteAnalyticsService + Mapper 聚合; 事件 NoteHeatmapEvent + NoteHeatmapListener; MarkdownUtils/StreakUtils
- DB 迁移 `db/migration/I2_analytics.sql`: notes(author_id,created_at)、note_likes/comments/collection_notes(created_at) 索引; schema.sql 同步
- 前端: 个人中心「数据统计」页签(3 大卡片 + 明细 + 全年热力图 + 年份切换); 首页「笔记热度榜」卡片(近7天/近30天/总榜); 我的笔记「导出全部」+ 笔记卡片单篇导出; 热力图纯 CSS 网格, 不引图表库
- 文档: api-contract 新增 §14、technical-design §5.7、README 路线图勾选

**测试门**: 后端 144/144; 前端 29/29; 构建通过
**冒烟门(真实环境)**: 登录 demo → top3(2 篇/1 赞/1 连击) / heatmap 365 天且今日 2 篇 / 创建临时笔记热力图 2→3 / 删除还原 3→2 / ranklist 本周 2 条且 hotScore 正确 / 非法周期 40001 / 全部导出含附件头与正文 / 单篇导出 / 不存在笔记 40401 / 清理后笔记数还原; 浏览器: 首页热度榜渲染、数据统计卡片与热力图 2026-11-09=2篇、导出全部触发 download 事件
**自审门**: 修复 4 处自身问题 — ① BITFIELD offset 是 bit 而非字节, 增量写错位置(冒烟发现并修, 含测试断言); ② 窗口榜单收藏数按人头 DISTINCT, 防一人多夹刷热度; ③ 删除笔记事件改用 note.authorId 而非操作者; ④ 前端 revokeObjectURL 延迟, 防部分浏览器中断下载
**commit**: (本次提交)
**遗留已知**: 热力图位图 TTL 1h 内依赖事件维护(极端并发回填窗口可能短暂少计, 1h 后自愈); 导出未做体量上限
---

## [I1] 2026-11-09 ｜ 搜索能力（FULLTEXT ngram + Jieba + Redis 缓存）

**做了什么**
- 数据库: notes 增加 search_vector(MEDIUMTEXT) + FULLTEXT(ngram) 索引；`db/migration/I1_search.sql` 可重复执行并回填历史数据；schema.sql 同步新装环境
- 写入维护: 笔记创建/编辑时生成向量(题目标题+正文), NoteMapper insert/updateContent 同步落库
- 查询: SearchUtils(Jieba 分词 + 去 Markdown/标点 + 去重 + 64 字截断) → MATCH AGAINST(NATURAL LANGUAGE)；用户搜索 LIKE 且邮箱脱敏
- 缓存: Redis key=search:notes:{keywordHash}:{page}:{size}，只缓存 {ids,total}(与用户无关)，VO 按当前用户实时补 myActions；TTL 5min，缓存读失败降级查库
- 接口: GET /api/search/notes、/api/search/users；前端顶栏搜索框(输入+按钮/回车) + /search 结果页(笔记/用户页签、分页)
- 测试: 后端 +8（SearchUtils 4、SearchServiceImpl 4）；前端 +3（SearchPage）

**测试门**: 后端 131/131；前端 21/21；构建通过
**冒烟门**: 真实环境 `keyword=哈希表` → 命中 1 条(两数之和)；用户搜索 demo 命中且 email=null；空关键词 total=0；Redis 键存在且 TTL≈294s、内容 `{"ids":[1],"total":1}`；浏览器顶栏搜索→/search 结果正确、用户页签可用
**自审门**: 修复 2 处自身问题 — ① 缓存若含 VO 会串用户，改为只缓存 ID/总数；② 顶栏 Input.Search 的回车在自动化下不稳定，改为显式输入框+按钮(可访问名明确)
**commit**: (本次提交)
---

## [S4] 2026-11-09 ｜ 独立复测缺陷修复（3×P1 + 9×P2 全部闭环）

**做了什么**
- P1: 客户端异常 4xx 映射(40001/405/415/BindException) / 死锁重试切面(0×500) / 删父评论级联软删+计数
- P2: 分页溢出钳制 / 消息越权 40401 / 回复消息 target 统一 / /admin 403 稳定 / admin 前置守卫 / 邮箱限流大小写 / 错误文案去 Java 细节与字段重复 / difficulty 校验 / 契约文档同步
- 新增测试 22 个: GlobalExceptionHandler(9) / DeadlockRetryAspect(3) / PageUtils(3) / TokenInterceptor(4) / 级联与回复 target / 邮箱归一 / difficulty / 绑定失败文案
- 文档: docs/test-report.md 追加"第二轮修复记录"12 条；technical-design 错误码表补 40902/40500/41500 并把 40001 的 HTTP 列改为 200；api-contract 修正 /notes/mine 与消息越权说明

**测试门**: 后端 123/123；前端 18/18；构建通过；真实环境修复复测 7/7（含 100 次并发混合点赞 0×500）；浏览器 /admin 403 稳定
**自审门**: 修复过程中另修正 3 处自身疏漏（preHandle 三参签名、deliverTask 变量遗漏、FieldError 构造参数），均已在提交前通过测试
**commit**: (本次提交)
---

## [S3] 2026-11-09 ｜ 全量验收（后端 101 + 前端 18 + E2E 44）

**做了什么**
- 后端 mvn test: 101/101; 前端 vitest: 18/18; 前端构建通过
- 编写仓库外验收脚本 acceptance.mjs(Node fetch), 覆盖基础/注册登录/笔记/权限/点赞消息/评论/收藏/题单/上传/管理端 共 44 项断言 — 44/44 通过
- 测试数据清理: 删除验收账号及其笔记/评论/收藏/消息, 用户表恢复 2 个种子账号
- 产出 docs/test-report.md(结论/覆盖表/明细/性能安全自检/已知限制/复现方式)

**测试门**: 见上全部绿
**自审门**: 报告中的已知限制逐条来自 M3-M6 自审记录, 未发现新缺陷; 验收脚本与数据均在仓库外
**commit**: (本次提交)
---

## [M6] 2026-11-09 ｜ 管理端 5 页面 + 前后端联调（MVP 全部完成）

**做了什么**
- 管理端布局: 独立路由 /admin + Sider 导航 + **admin 守卫**(未登录跳首页/非管理员 403/自己恢复 whoami)
- 页面: 数据看板(今日 6 指标卡片 + 每日快照趋势表) / 题库管理(筛选+新建编辑+删除+Markdown 批量导入含失败明细) / 题单管理(CRUD + 条目增删与上下移排序) / 分类管理(两级树表+增改删) / 用户管理(账号搜索+封禁开关)
- 领域层: domain/admin/api.ts 收敛全部管理接口; statistic 类型
- 后端补漏: UserVO 增加 isBanned(管理端此前无法显示/判断封禁状态)
- 测试: 新增 AdminLayout(403/菜单) + Dashboard(指标/趋势) = 18/18 绿; 构建通过

**冒烟门(内置浏览器, admin 账号)**
看板真实数据(登录3/注册1/笔记2/趋势1天) → 题库管理新建题目(成功后自动跳末页可见) → UI 删除成功 → 题单类型正确显示「官方推荐」→ 分类树子分类展开 → 用户管理封禁 demo(aria-checked=true, 状态变已封禁) → 解封(API 复核 isBanned=0) → Markdown 批量导入"共1题成功1题" → 清理测试数据

**自审门**
- 🐛 修复: 直接访问 /admin 永久卡「校验权限中」(whoami 恢复只在用户端布局) → AdminLayout 自行恢复
- 🐛 修复: 题单类型恒显示「专题」(render 首参是 value, 漏了 dataIndex) → 补 dataIndex
- 🐛 修复: 分类树子行默认不展开(异步数据下 defaultExpandAllRows 失效) → 受控 expandedRowKeys
- 🐛 修复(后端): UserVO 缺 isBanned → 管理端无法反映封禁状态
- ✅ 修复: 新建题目后列表看不到(升序 ID 在末页) → 创建后自动跳末页
- ⚠️ 已知: 管理端题库"全部分类"含顶级分类(应先只列二级, I5 优化); 批量导入 UI 未做分类默认值记忆
**commit**: (本次提交)
---

## [M5-C] 2026-11-09 ｜ 个人中心 + 他人主页 + 消息中心（用户端页面收尾）

**做了什么**
- 个人中心(3 面板): 资料(头像上传/昵称/性别/生日/学校/签名, 保存后同步 store) / 我的笔记(分页, NoteCard 作者操作) / 我的收藏(左夹右笔记, 删夹/移出夹)
- 他人主页 /users/:id: 公开资料(脱敏)+TA 的笔记+加入时间/篇数, 404 态
- 消息中心: 全部/点赞/评论/系统 分页签(带未读角标), 点击已读并跳转目标笔记所在题目, 全部已读, 删除
- 导航: 登录后显示消息角标(30s 轮询, 退出即停), 用户下拉菜单(个人中心/我的主页/退出)
- 测试: 新增 MessagePage 2 个, 合计 15/15 绿; 构建通过

**冒烟门(内置浏览器)**
消息中心真实消息(站长 点赞/评论) → 点赞筛选只剩点赞条 → 全部已读按钮在 0 未读时禁用(正确) → 个人中心表单回填(昵称/签名) → 改签名保存成功并同步 → 我的笔记 2 篇 → 我的收藏「前端验证收藏 1 篇」→ 他人主页资料与笔记渲染 → 截图确认

**自审门**
- ✅ 修复: 未登录访问个人中心静默跳首页 → 主动广播弹登录框
- ✅ 修复: 消息点击只改 hash 无实际跳转 → 先取笔记并跳 /questions/{id}#note-{id}
- ✅ 修复 antd destroyOnClose 弃用告警(Modal/Drawer 3 处)
- ⚠️ 已知: 主页笔记数用当前页长度(20 上限, 未取 total); 消息删除后未同步导航角标(等下次轮询)
**commit**: (本次提交)
---

## [M5-B] 2026-11-09 ｜ 题目详情 + 笔记编辑 + 点赞/收藏/评论（真浏览器闭环）

**做了什么**
- 渲染: MarkdownView(react-markdown + remark-gfm, 默认不渲染原始 HTML 天然防 XSS); **决策调整**: 弃用 cherry-markdown(体积 400KB+ 且 SSR/测试不友好), 改 TextArea + 双栏实时预览, 依赖净减 96 包
- 组件: NoteEditorModal(双栏/编辑/预览三视图, Ctrl+Enter 保存, 新建/编辑双态)、NoteCard(Markdown 全文折叠/点赞乐观更新+失败回滚/收藏/评论入口/作者编辑删除)、CollectModal(选夹或新建并收藏)、CommentsPanel(顶层分页+楼中楼+回复@+点赞+本人编辑删除)
- 页面: QuestionPage(题干+该题笔记列表+写/改我的笔记+评论抽屉+404 态)
- 工具: requireLogin(未登录广播弹登录)、handleAuthError(40101 统一处理)
- 测试: 13 个(vitest) — 新增 MarkdownView(GFM/防 XSS) 2、NoteEditorModal(新建拦截/编辑更新) 2; 构建通过

**冒烟门(内置浏览器真实 E2E, demo 账号)**
编辑已有笔记(旧内容→Markdown 代码块, 保存后立即渲染) → 点赞 1→2 → 取消 2→1 → 打开评论抽屉(顶层+楼中楼+回复@+本人操作) → 发评论从界面发的一条评论并出现在列表 → 收藏(现场新建收藏夹前端验证收藏, star 0→1, 弹窗自动关闭) → 换题1 新建笔记双指针解法并渲染成功 → 截图确认排版

**自审门**
- 🐛 **渲染死循环(严重)**: 评论抽屉 onCountChange 触发整页 reload → 提前 return 卸载抽屉 → 抽屉重挂载再触发 → 无限循环(页面卡在骨架屏)。修复: 仅首屏显示骨架 + 移除该回调(注释说明原因)
- ✅ 修复: 测试 mock 未清导致跨用例污染(createNote 误判被调用)
- ⚠️ 已知: 笔记 commentCount 在抽屉内评论后不即时刷新卡片数字(设计上避免整页 reload, C 批次由消息/列表页兜底); 评论点赞与回复按钮 UI 已实现, E2E 未逐一点测(后端接口此前已冒烟)
**commit**: (本次提交)
---

## [M5-A] 2026-11-09 ｜ 前端骨架 + 首页/题单页（含真浏览器验证）

**做了什么**
- 工程: Vite5 + React18 + TS + Antd5 + RTK2 + react-router6 + axios + Vitest; ESM 别名用 import.meta.url(避开 __dirname); dev 代理 /api /images → 8080
- 请求层: axios 拦截器注入 Bearer; unwrap 纯函数统一解 {code,message,data}; ApiError 携带业务码; 40101 广播 notehub:unauthorized
- 状态: userSlice(login/register/whoami thunk + token 持久化 + logout/clearAuth)
- 页面: 布局(导航/登录弹窗/用户菜单) + 首页(题库筛选 + 最新笔记双栏) + 题单页(左单右题 + 刷题进度) + NotFound
- 组件: LoginModal(登录/注册双 Tab, dev 模式自动回填验证码+60s 倒计时)、DifficultyTag
- 测试: 9 个(vitest) — unwrap/token 4、userSlice 3、HomePage 渲染+空态 2; 生产构建通过
- 真浏览器验证(内嵌浏览器): 首页题库/笔记流真实数据渲染、难度筛选 简单→6题/困难→0题、题单导航、demo 账号 UI 登录、登录后题单进度 1/6 且已写笔记标记正确、截图确认排版

**测试门**: 前端 9/9 绿 +  成功(tsc 严格模式)
**自审门(diff 自审)**
- ✅ 修复 a11y: 分类/难度筛选无访问名(自动化与读屏都无法定位) → 加 aria-label, 浏览器验证通过
- ✅ 修复时序缺陷: 登录后题单页不重新拉取(进度依赖登录态) → effect 依赖 token
- ✅ 修复 ESM 运行时坑: vite config 用 __dirname 在 type:module 下为 undefined → import.meta.url
- ✅ 构建产物治理: tsc -b 会把 vite.config 生成 .js/.d.ts 并误入库 → 改 tsc --noEmit + 删除生成物; manualChunks 拆分 react/antd/editor
- ⚠️ 已知: 40101 由页面捕获, 写操作统一用 handleAuthError 在批次 B 接入
**commit**: a29c9bd(骨架) + 后续构建配置修正
---

## [M4] 2026-11-09 ｜ 管理端统计看板 + 图片上传 + 每日快照（后端 MVP 收尾）

**做了什么**
- 统计: Statistic 实体 + StatisticMapper(upsert/近期趋势) + UserMapper/NoteMapper 6 项聚合查询 + StatisticServiceImpl(今日实时 6 指标 + 趋势升序) + DailyStatisticTask(23:59 cron, 异常不外抛)
- 看板接口: GET /api/statistic?days=30 (@NeedAdmin), 趋势天数收敛 1~90
- 上传: UploadService/Impl(后缀白名单 jpg/jpeg/png/gif/webp、5MB、UUID 重命名、拒绝 svg 防 XSS、路径穿越安全) + UploadController POST /api/upload/image(@NeedLogin)
- 头像: POST /api/users/avatar(multipart→存图+updateAvatar), 打通契约; /images/** 静态映射实测可访问
- 错误码治理: GlobalExceptionHandler 新增 5 类客户端异常映射(坏JSON/缺参数/类型错/缺文件字段/超大小) → 40001, 不再落 50000

**测试门**: 101/101 绿(新增 14: Statistic 3/Upload 5/Task 2/ExceptionHandler 4)
**冒烟门(真环境 8+3 项)**: 看板 6 指标真实聚合(login=3/reg=1/note=1)、普通用户 40102、PNG 上传成功并可 HTTP 200 取回(image/png)、svg 拒绝、未登录 40101、头像更新、upsert 幂等实测(两次执行仍 1 行)、趋势读快照、坏JSON/缺参数/缺文件均 40001
**自审门**
- ✅ 修复: 客户端错误被兜底成 50000(坏 JSON/缺参数/缺 multipart 字段/超 5MB) → 5 类映射 + 单测
- ✅ 检查: UUID 文件名防路径穿越(测试含 ../../evil.png)、svg 排除理由(内嵌脚本 XSS)、定时任务异常不外抛
- ⚠️ 已知限制: ① 换头像不删旧文件(磁盘累积, I5 清理任务) ② upsert 用 VALUES() 语法(MySQL 8.0.41 仍支持, 8.0.20+ 标记弃用, 升级 9.x 前改 alias 语法) ③ 登录数按 last_login_at 聚合(非逐次登录日志, 与原项目同口径)
**commit**: (本次提交)
**遗留**: 后端 MVP 完成, 转前端
---

## [M3] 2026-11-09 ｜ 笔记核心（CRUD/聚合防N+1/点赞/收藏夹/评论/事件驱动消息）

**做了什么**
- 实体 7 个: Note/NoteLike/Collection/CollectionNote/Comment/CommentLike/Message(带联表附带列); 枚举 MessageType/MessageTargetType
- Mapper 7 + XML 6: 列表联表作者/题目一次取齐; 批量互动状态 IN 查询; 原子计数 GREATEST(x+delta,0); INSERT IGNORE 幂等; 评论两级结构(顶层 parent_id IS NULL + 楼中楼批量 IN)
- Service 6: NoteServiceImpl(CRUD + 聚合防N+1 + 收藏夹内容复用批量查)/NoteInteractionServiceImpl(点赞/评论写 + 事件发布)/CommentQueryServiceImpl(顶层分页+楼中楼装配+点赞标记)/CollectionServiceImpl(多收藏夹)/MessageServiceImpl(分类型未读)
- 事件驱动: InteractionEvent + @TransactionalEventListener(AFTER_COMMIT) + @Async → 业务事务提交后才写消息, 回滚不产生幽灵消息; 自赞自评不通知; 楼中楼双收件人去重
- Controller 5 组: notes/comments/like/collections/messages 共 23 个接口
- M2 遗留收口: 题目详情 myNoteId 真实回填(注入 NoteMapper)

**测试门**: 87/87 绿(新增 30 个: Note 7/Interaction 9/Comment 5/Collection 5/Message 3 + 越界页/批量查断言)
**冒烟门(真环境 23 项)**: 写笔记→重复40901→myNoteId回填→点赞幂等→**点赞消息落库(unread like=1)**→评论+楼中楼→**评论消息(comment=1)**→全部已读归零→收藏夹(40301越权/40401校验/幂等added)→收藏数→评论点赞计数回落→删他人笔记40301

**自审门(diff 自审 55 文件, 发现并修复 7 项)**
1. 🐛 **deleteCollection 顺序 BUG**: 先删条目再查 noteId → 收藏数永不递减(真环境实测已修: 1→0)
2. 🐛 **评论页码越界 500**: total>0 但本页空 → selectRepliesByParentIds 生成 IN () 语法错误
3. 🐛 **listNotesByIds N+1**: 逐条 selectById(页大小=SQL 数) → 新增 selectByIds 批量 + 顺序还原
4. 🐛 **createNote 未校验题目存在** → 可写出挂幽灵题的笔记; 已加 40401
5. 🐛 **PATCH 复用创建 DTO**: 编辑笔记/评论被迫带 questionId/noteId → 拆 NoteUpdateBody/CommentUpdateBody
6. 🐛 **收藏数多夹误递减**: 用户其他夹仍收着也扣 → countByCreatorAndNote 判定(remove+delete 两处)
7. 🐛 **评论点赞消息 target 错误**(指向笔记) → InteractionEvent.likeComment(targetType=COMMENT, targetId=commentId); 另 OGNL 字符串比较改 'hot'.equals 规范写法
- ⚠️ 已知限制(记录待迭代): ① 同一用户多夹重复收藏同一笔记时 collect_count 语义为夹条目数-跨夹去重(已用 OTHER 判定收敛, 极端场景仍可能偏差, I5 用唯一化统计表治理) ② 收藏夹路径不写 note_collects(判定已 UNION 兼容) ③ 消息投递失败仅记日志不重试(I3 WebSocket 时加可靠投递)
**commit**: (本次提交)
**遗留**: 前端未开始(M5)
---

## [S2] 2026-11-09 ｜ Docker/Redis 环境攻克 + 邮件验证码/注册全链路回归

**做了什么**
- Docker 四天拉锯战终结: 元凶=MuMu 模拟器 MuMuVMMDrv 驱动(每次开机回滚 VirtualMachinePlatform)。处置: 用户确认不用 MuMu → 卸载残留+sc delete 驱动 → 第4次启用 VMP 即时生效(State=Enabled 历史首次) → 重启后 vmcompute Running、引擎 29.9.2、notehub-redis Up + PONG
- 长期化: 容器 restart=unless-stopped; 计划任务 DockerDesktopAutoStart(Docker Desktop 开机自启); zip 版 Redis 备胎留存 E:edis
- 后端连真 Redis 回归: 发验证码(devCode) → 60s 限流 41002 → 正确码注册(userId=3+JWT) → 同码复用 41001(一次性) → 新号登录 200, 全链路 5/5 绿

**测试门**: 冒烟 5/5 绿(见上); 单测未动(57/57 基线)
**自审门**: 无代码改动(纯环境+回归); 环境教训已固化至 HANDOFF 踩坑记录(MuMu/PS1 BOM/UTF-16)
**commit**: (本次提交)
**遗留**: 无环境遗留。下一步 M3 笔记核心

---

## [S1] 2026-11-08 ｜ 真实环境冒烟测试（首次启动后端）

**做了什么**
- 生成 application-local.yaml（本地 DB 密码 + 随机 48 字节 jwt.secret，gitignore 验证 ✅）
- 启动后端（Boot 3.5.16 首次真跑）+ curl 冒烟 21 项

**测试门：冒烟 21 项全过 + 单测回归 57/57 绿**
| 场景 | 结果 |
|---|---|
| 分类树两级结构 / 题目列表联表分类名 | ✅ |
| 详情浏览数 0→1（原子自增实测） | ✅ |
| 题单列表含 questionCount=6 | ✅ |
| admin/demo 双角色登录（JWT） | ✅ |
| whoami 全量 / 40101 未登录 / 40102 普通用户访问管理端 | ✅ |
| 管理端用户列表（email 已脱敏） | ✅（修复后） |
| 修改资料（中文昵称/签名） | ✅ |
| 管理端建题 + 批量导入（total=2, success=1, failures 明确"缺少「难度：」行"） | ✅ |
| 加条目 200 / 重复加 40901"该题已在题单中" / 越权排序 40001 / 正向重排 200 + 顺序验证 | ✅ |
| 封禁后登录 → 40301"账号已被封禁" + 解封恢复 | ✅ |
| 删除守卫：有题分类拒删 40001 | ✅ |
| 难度筛选 total=4 与种子数据交叉验证一致 | ✅ |
| Redis 依赖项（邮件验证码/注册） | ⏳ 引擎未启动，重启后回归 |

**自审门：冒烟抓出 4 个真缺陷（全部单测盲区）并修复**
1. 🐛 **启动阻断**：dev 模式无 spring.mail 配置 → Boot 不创建 JavaMailSender → EmailServiceImpl 注入失败。修复：MailConfig 兜底 @ConditionalOnMissingBean（配了真 SMTP 自动退位）
2. 🐛 UserMapper.countByQuery 缺 @Param("param") → MyBatis ReflectionException（XML 的 param.account 引用从没真跑过）。修复：补注解
3. 🐛 MySQL 限制：INSERT 子查询引用目标表 → SQLException("can't specify target table")。修复：删除 SQL 内技巧，新增 selectMaxSortRank，追加排序计算挪到 Service（含单测）
4. 🐛 契约偏差：Lombok boolean isSuccess() 被 Jackson 序列化成 "success" 字段。修复：@JsonIgnore
- ✅ 测试侧教训记录：① Windows Git Bash 的 curl 发中文 JSON 是 GBK 字节 → 必须 UTF-8 文件体（--data-binary @file，node 生成）② spring-boot:run 的孙进程杀不干净占 8080 → netstat+taskkill ③ 两次把"测试数据已被我删掉"误判为后端 bug（40401/40001 实为正确守卫）——先查数据再怀疑代码
**commit**：（本次提交）
**遗留问题**
1. 重启电脑 → post-reboot-redis.cmd → 回归 Redis 依赖项（邮件验证码/注册全链路）
2. frontend/ 未创建（M5）

---

## [M2] 2026-11-08 ｜ 题库模块（分类/题目/题单/条目 + Markdown 批量导入）

**做了什么**
- 实体/Mapper：Category、Question(联表附带 categoryName)、QuestionList、QuestionListItem(联表附带题目摘要) + 4 份 XML（密码无关表; 列表排序稳定 ORDER BY id/sort_rank）
- 分类：getCategoryTree 内存建树（顶级保序）；create/update 校验父分类必须是顶级（两级约束）；delete 三重守卫（存在/子分类/题目）
- 题目：分页列表（联表分类名, 不带题干）、详情（view_count 原子自增 + myNoteId 占位待 M3）、CRUD（分类存在性校验、有笔记拒删）、批量导入（行级解析 + @Transactional + 失败明细）
- 批量导入解析器 QuestionMarkdownParser：# 标题切块（裸 # 报错、## 视为正文）、难度：简单/中等/困难→1/2/3（中英冒号兼容）、考点分号分隔、缺难度行进 failures
- 题单：列表（批量 countGroupByListId 填 questionCount）、CRUD（删单连带条目）、条目添加（uk 防重→DUPLICATED/题目存在性）、移除、批量排序（越权排序校验 + 顺序即 1..n）
- 用户端条目进度：listItems 登录时 selectQuestionIdsWithNote 批量标记 hasMyNote（一条 SQL 防 jurídico N+1）
- 契约同步：批量导入 body 补 categoryId；排序 body 补 questionListId；技术设计 §2/§5.4 记录 Flexmark→行级解析变更

**测试门**
- `mvn test`：**Tests run: 56, Failures: 0, Errors: 0 — BUILD SUCCESS**（新增 20 个：解析器 5 / QuestionService 7 / 分类树+题单 8）

**自审门（diff 自审清单）**
- ✅ 修复：裸 `#` 行 substring 越界 → 标题提取改正则剥离 ^#+\s*
- ✅ 修复：ApiResponse.ok(String) 重载与 ok(T) 产生二义（Void 端点编译错/语义陷阱）→ 撤销重载，Void 端点统一 ok()
- ✅ 修复：排序接口初版路径变量绕弯（requireListIdFromItems 抛 UnsupportedOperationException 的烂设计）→ listId 入 body，单端点
- ✅ 修复：CategoryServiceImpl.delete 残留无效防御块；QuestionListItem 实体误加占位字段
- ✅ 清理：QuestionMapper.countGroupByCategory 死方法（接口+XML 同删）
- ✅ 测试侧修正：create 场景 insert 未回填 ID 导致 NPE
- ⚠️ 已知限制：① 批量导入 DB 级异常整体回滚（校验失败逐条剔除，语义已在技术设计注明）② 详情接口 myNoteId 恒 null（M3 笔记模块回填）
**commit**：（本次提交）
**遗留问题**
1. Docker：已装入 E:\docker（exit 0），引擎需重启后生效 → 用户重启电脑后双击 E:\docker\post-reboot-redis.cmd
2. application-local.yaml 用户尚未创建（后端启动前置）

---

## [M1] 2026-11-07 ｜ 用户模块（注册/登录/验证码/资料/管理端用户管理）

**做了什么**
- Mapper 层：UserMapper + XML（selectByAccount/Email/Id、insert、updateProfile 动态 set、updateLastLoginAt、updateBanned、管理端分页 count/list；列表 SQL 用 publicColumns 不查 password）
- DTO/VO：RegisterRequest（@Valid 邮箱/验证码/账号/密码校验）、LoginRequest、UpdateUserRequest（account/email/password 不可改）、UserQueryParam、UserVO（本人视角全量/公开视角脱敏）、LoginUserVO{userId,token}
- 邮件模块：RedisService 抽象+Impl（薄封装便于 mock）、RedisKey 常量、EmailTask、EmailServiceImpl（60s 限流 setIfAbsent / dev 开关直接落 Redis 并返回 devCode / 真发模式任务 JSON 入 Redis 队列）、EmailTaskConsumer（3s 轮询 RPOP 投递，坏 JSON/发送失败不中断循环）、EmailController（GET /api/email/verify-code）
- 用户模块：UserServiceImpl（注册=验证码一次性消费+双唯一校验+BCrypt+签发 token；登录=BCrypt+封禁拦截+刷新 last_login_at，错误文案统一防账号枚举；whoami 全量/公开主页脱敏 account+email；资料更新；管理端列表分页收敛 1~50；封禁）、UserController（8 个接口全落地，构造器注入）
- 鉴权增强：TokenInterceptor 接入 users 表——isAdmin 实时判定（降权即时生效）、封禁用户按匿名处理；ErrorCode 新增 LOGIN_FAILED(40103)，technical-design §7 同步

**测试门**
- `mvn test`：**Tests run: 36, Failures: 0, Errors: 0 — BUILD SUCCESS**（新增 22 个：UserServiceImplTest 11 / EmailServiceImplTest 7 / EmailTaskConsumerTest 4）

**自审门（diff 自审清单）**
- ✅ 真实缺陷修复：UpdateUserRequest 全空 → MyBatis `<set>` 空片段 → SQL 语法错误；service 层短路直接返回
- ✅ 修复：EmailServiceImpl 误 import UserVO；deliver 方法跨包不可见 → 提升为接口方法 deliverTask
- ✅ 修复：pom 漏掉 spring-boot-starter-mail（MimeMessage 类缺失）
- ✅ 修复：UserController 初版写乱（双构造器）→ 重写为单构造器注入
- ✅ 测试侧修正 3 处：公开主页断言字段用错 / mock 下 whoami 重读旧数据 / never() 与前序 verify 矛盾
- ⚠️ 已知限制（I5 硬化清单）：① TokenInterceptor 每次认证请求 1 次 DB 查询（规模上来后加缓存）② 注册并发同账号依赖 DB 唯一键兜底（DuplicateKeyException → 500，可转 40901）③ 邮件发送失败不回队（用户重发即可）
**commit**：（本次提交）
**遗留问题**
1. Docker（重启后 install-docker-admin.cmd）与 SMTP 授权码仍待用户提供
2. application-local.yaml 用户尚未创建（启动前置）

---

## [M0] 2026-11-07 ｜ 后端骨架（Spring Boot 3.5.16 + JWT 鉴权链路）

**做了什么**
- 工程：pom.xml（Boot 3.5.16/JDK21；web/security/redis/aop/validation/log4j2/mybatis 3.0.4/mysql-j/jjwt 0.12.6/lombok/hutool；compiler 插件显式声明 lombok 处理器 + proc=full）
- common 包：ApiResponse（{code,message,data}）/ ErrorCode（11 个错误码）/ BusinessException / GlobalExceptionHandler（业务 200、参数 40001、兜底 500）/ RequestContext（请求级登录态）/ TraceIdFilter（MDC + X-Trace-Id）/ PageVO
- security 包：JwtUtil（jjwt 0.12 新 API，secret≥32B fail-fast）/ TokenInterceptor（Bearer→RequestContext）/ @NeedLogin @NeedAdmin + AuthAspect 环绕鉴权（补上原项目缺失的 admin 校验）
- config 包：SecurityConfig（无状态/CORS 5173/BCrypt，业务放行）、WebConfig（拦截器 + /images/** 静态映射）、RedisConfig
- 配置：application.yaml（config.import optional local）、application-dev.yaml（无凭据入库）、application-local-template.yaml、log4j2-spring.xml（pattern 含 traceId）
- HealthController GET /api/hello；测试 3 组共 14 个（ApiResponse/RequestContext 逻辑、JwtUtil 签发/过期/篡改/异钥/短密钥、hello MockMvc）

**测试门**
- `mvn test`：Tests run: 14, Failures: 0, Errors: 0 — **BUILD SUCCESS**

**自审门（diff 自审清单）**
- ✅ 根因修复：类名 RequestScope 与 Spring 注解冲突（"已在该编译单元中定义"导致 javac 跳过注解处理，Lombok 全量报错）→ 重命名 RequestContext，全库引用清理
- ✅ 修复：compiler 插件缺 annotationProcessorPaths → Lombok 不生效；pom 显式声明 + proc=full
- ✅ 修复：RedisConfig 误用 Boot 定制 ObjectMapper（无 default typing，回读对象会 Missing type id）→ 改无参 GenericJackson2JsonRedisSerializer
- ✅ 修复：ApiResponseTest 用错 ok() 重载（测试侧缺陷）
- ⚠️ 已知限制：TokenInterceptor 暂将 isAdmin 置 false，M1 用户模块接入 users 表后由 UserService 覆盖（代码内已注明）
- ⚠️ 观察：用户全局 Maven settings.xml 有损坏的 `<mirror>` 节点（warning），暂不影响构建；建议后续用户修复，勿擅动
**commit**：（本次提交）
**遗留问题**
1. Docker：等用户重启电脑 → 管理员运行 E:\docker\install-docker-admin.cmd（WSL2 已装好）
2. SMTP 授权码仍未提供（M1 邮件模块用 dev 开关模式先开发）
3. 用户尚未创建 application-local.yaml（模板已给，启动前必须建）

---

## [E0] 2026-11-06 ｜ 环境准备（数据库/Node 完成，Docker 进行中）

**做了什么**
- MySQL：验证本地 DB 账号连通（8.0.41）；建库 notehub（utf8mb4）；编写并执行 schema.sql（14 张表，含防重唯一约束/索引/软删字段）与 seed.sql（管理员 admin/admin123 + demo 账号、2 个一级分类 8 个二级分类、8 道题、1 个题单 6 条目）——BCrypt 散列用 Node bcryptjs 现场生成，非网络拷贝
- Node.js 22.23.4 zip 解压到 E:\nodejs（PowerShell Expand-Archive；GNU tar 解不开 zip 踩坑）；用户 PATH 追加 E:\nodejs（SetEnvironmentVariable，避开 setx 截断风险）；npm registry 确认 npmmirror
- Docker 路线（用户决策：坚持 Docker、装到 E:\docker）：Docker Desktop Installer.exe（595MB，PE32+ 校验通过）已下载到 E:\docker；编写重启后自动安装脚本 install-docker-admin.cmd（安装→启动引擎→起 notehub-redis 容器→验证）；发起 WSL2 提权安装（wsl --install --no-distribution，UAC 已交给用户确认）
- 文档同步：environment-setup.md §3/§7 改为 Docker 实际方案 + 镜像加速；HANDOFF.md 刷新进度与踩坑记录

**测试/自审结论**
- schema 实测：14 表创建成功（SHOW TABLES 全列出）；seed 实测：users=2, questions=8
- BCrypt 散列在库中可验证（M1 登录测试再验匹配逻辑）
- 自审清单：
  - ✅ 发现并修正：`rank` 是 MySQL8 保留字 → question_list_items 排序列改名 `sort_rank`（同步注释）
  - ✅ 发现并补充：comments 增加 `reply_to_id`（楼中楼定位）与 `is_deleted`（契约写了软删但初版 DDL 漏列）
  - ✅ seed 密码散列为真实 BCrypt；敏感项（root 密码/SMTP）未写入任何入库文件
  - ⚠️ 遗留：WSL2 安装在等待用户 UAC/下载，Docker 安装依赖重启后执行
**commit**：（本次提交）
**遗留问题**
1. 用户侧待办：确认屏幕 UAC/WSL 安装窗口 → 重启电脑 → 管理员运行 E:\docker\install-docker-admin.cmd
2. SMTP 授权码仍未提供（M1 前需要，否则用 dev 开关模式）

---

## [D0] 2026-11-06 ｜ 开工定稿文档包

**做了什么**
- 初始化 Git 仓库（main 分支）+ .gitignore（敏感配置/构建产物排除）
- 确立工作规则：三道门（commit 门/测试门/自审门）+ 过程文档纪律，写入 memory.md 并同步跨会话持久记忆
- 前期准备（D0 之前完成的调研与分析）：
  - 逆向分析原项目卡码笔记（E:\ka\kamanotes-分析报告.md）：15 张表、60+ 接口、鉴权/邮件队列/搜索/统计等实现要点
  - 调研 5 个同类项目（mianshiya-next/QingdaoU OJ/DMOJ/apache answer/doocs leetcode）README 与 Issues
- 定稿 4 份文档：technical-design.md（架构/选型/14 表设计/核心流程/错误码/安全清单）、api-contract.md（60+ 接口契约）、environment-setup.md（环境手册）、README.md（门面）
- 用户决策记录：MVP 完整管理后台 5 页面、真发邮件验证码、完整收藏夹、搜索推后（MVP 不做）

**测试/自审结论**
- 文档类改动无可执行测试；自审清单：
  - ✅ api-contract.md 补充了原项目缺失的 /admin/users 封禁接口（原项目无用户管理写接口，看板需要）
  - ✅ 修正原项目反面教材：JWT secret 不入库（application-local.yaml 已 gitignore）
  - ✅ 接口契约与 technical-design §7 错误码对齐（41001/41002/40901 已引用）
  - ⚠️ 发现：原项目 /api/notes/download、ranklist 等接口未进 MVP 契约，已在「附录：迭代预留」中登记，防止遗漏
- MySQL 连通性实测：本地 DB 账号 → 8.0.41 连接成功（凭据仅存本机 gitignore 的配置文件，不入库）

**commit**：（本次提交）
**遗留问题**
1. SMTP 授权码未提供（M1 用户模块前需要，否则注册用 dev 开关模式）
2. Node.js 22、Redis 未安装（E0 执行）
3. schema.sql/seed.sql 未创建（E0 执行）
