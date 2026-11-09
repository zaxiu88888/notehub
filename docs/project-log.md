# notehub 项目日志

> 每干一步追加一条。格式：日期 ｜ 做了什么 ｜ 测试/自审结论 ｜ commit ｜ 遗留问题





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
- 生成 application-local.yaml（root/12345 + 随机 48 字节 jwt.secret，gitignore 验证 ✅）
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
- MySQL：验证 root/12345 连通（8.0.41）；建库 notehub（utf8mb4）；编写并执行 schema.sql（14 张表，含防重唯一约束/索引/软删字段）与 seed.sql（管理员 admin/admin123 + demo 账号、2 个一级分类 8 个二级分类、8 道题、1 个题单 6 条目）——BCrypt 散列用 Node bcryptjs 现场生成，非网络拷贝
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
- MySQL 连通性实测：root/12345 → 8.0.41 连接成功（密码仅存本机 gitignore 外的 E:\ka 记事本 + 环境手册引用，不入库）

**commit**：（本次提交）
**遗留问题**
1. SMTP 授权码未提供（M1 用户模块前需要，否则注册用 dev 开关模式）
2. Node.js 22、Redis 未安装（E0 执行）
3. schema.sql/seed.sql 未创建（E0 执行）
