# HANDOFF —— 会话交接文档

> 接手必读。本文件始终反映项目最新状态；每干一步由执行者刷新。
> 最后更新：2026-11-10（I1–I6 全量复测完成）

## 项目一句话
仿「卡码笔记」的刷题+笔记社区全栈项目（求职用），MVP=初始框架，按迭代路线逐步补齐并超越原项目。

## 当前进度
| 里程碑 | 状态 |
|---|---|
| D0 文档定稿包（README/技术设计/API 契约/环境手册/调研/日志/交接/规则） | ✅ 303e1ac |
| E0 环境（MySQL 8 + Node 22 + Redis 容器 + Docker Desktop） | ✅ 见下方环境说明 |
| M0 后端骨架（Boot 3.5.16 + common/security/config + JWT 链路） | ✅ 62a5403 |
| M1 用户模块（注册/登录/验证码队列/资料/管理端列表+封禁） | ✅ c668b6d |
| M2 题库模块（分类树/题目 CRUD/批量导入/题单/条目排序） | ✅ bf73394 |
| M3 笔记核心（笔记/点赞/收藏夹/评论/消息 23 接口，聚合防 N+1，事件驱动消息） | ✅ |
| M4 管理端统计看板 + 图片上传 + 每日快照 | ✅ 后端 MVP 完成 |
| M5 前端骨架 + 首页/题单/题目/个人中心/消息 + 管理端 5 页面 | ✅ a29c9bd 起 |
| S1 真实环境冒烟（21 项 curl；修复 4 个单测盲区） | ✅ |
| S3 全量验收（后端 101 + 前端 18 + E2E 44） | ✅ 5d33a61 |
| S4 独立复测 3×P1 + 9×P2 缺陷修复 | ✅ 3a463b4 |
| I1 搜索（FULLTEXT ngram + Jieba + Redis 缓存 + 前端搜索页） | ✅ cd41fc3 |
| I2 数据可视化（热力图/热度榜/数据卡片/Markdown 导出） | ✅ 66ef081 |
| I3 实时通知（WebSocket STOMP 未读推送，替代 30s 轮询） | ✅ 52239f4 |
| I4 标签体系（tags/question_tags/note_tags，题目/笔记双维度筛选） | ✅ 513ece8 |
| I5 深化（多级缓存/限流/每日互动统计/上传清理，含击穿保护/滑动窗口等优化） | ✅ ecd8a66 |
| I6 拓展（AI 题解总结、每日打卡 streak） | ✅ ce23dbd + d2766e2 |
| S5 全量复测（I1–I6 专项 192 项 + 44 项回归 + 浏览器 E2E） | ✅ 2026-11-10 |

## 当前测试状态
- 后端 `mvn test`：**207/207**
- 前端 `npm test`：**41/41**；`npm run build` 通过
- I1–I6 真实环境专项：**192/192**（I1 34 / I2 40 / I3 21 / I4 28 / I5 37 / I6 32）
- 历史 S3 端到端回归：**44/44**（auth 切面修改后已复跑）
- AI：本地 mock **18/18** + DeepSeek `deepseek-chat` 真实联调 **13/13**（Key 仅环境变量，未入库）
- 浏览器 E2E：用户端全链路 + 管理端 5 页面通过；发现并修复评论数不刷新
- 本轮修复见 `docs/project-log.md` [RETEST]/[RETEST-2] 与 `docs/test-report.md §9`（含 §9.7 遗留收口）

## 下一步
- 路线图 I1–I6 已全部完成，S5 全量复测通过
- 收尾建议：公开前检查清单、README 截图、DeepSeek 真模型演示（Key 用 `AI_SUMMARY_API_KEY` 环境变量，勿入库）
- 已收口：列表页评论入口已接线；搜索改布尔短语过滤 bigram 噪声；每周孤儿清理已 cron 实触发验证
- 已知限制见 `docs/test-report.md §9.5`

## 关键决策（不要推翻，除非用户要求）
0. **邮件交付方式（2026-11-08 用户决定）**：验证码保持 dev 开关模式（enabled=false，devCode 返回）；真发链路保留为可选
1. 技术栈：Spring Boot 3.x + JDK21 + MyBatis(XML) + MySQL8 + Redis + JJWT 0.12；前端 Vite+React18+TS+Antd5+RTK+axios
2. 鉴权：Security 只做 CORS/BCrypt；TokenInterceptor + RequestScope + @NeedLogin/@NeedAdmin
3. 消息：点赞/评论 → Spring Event → @TransactionalEventListener(AFTER_COMMIT) + @Async 写 messages
4. 计数：原子 UPDATE + 唯一约束；并发死锁 @RetryOnDeadlock
5. 列表聚合防 N+1：联表 + IN 批量补互动状态
6. 敏感配置全部 application-local.yaml（gitignore），绝不硬编码入库
7. I2：热力图 Redis 位图（每天 u8）+ 事件增量 + 1h TTL 回填；热度榜公共数据缓存 5min；导出为 Markdown 附件
8. I3：STOMP 只推未读计数不推消息体；JWT 放 CONNECT 头；只允许订阅 `/user/queue/**`；前端 5s 重连 + 重连后 HTTP 补拉
9. I4：题目标签唯一来源是 `exam_point`（不新增管理入口）；笔记标签默认继承题目标签快照，作者可改；标签自动建词表
10. I5：多级缓存版本号在单机内存，多实例 L1 最长滞后 30s；限流用 Redis ZSET 滑动窗口（Redis TIME），故障 fail-open；取消互动按原始发生日回退统计
11. I6：AI 默认关闭（LocalSummarizer 本地降级），Key 只走环境变量；仅模型生成消耗 5/min（缓存/本地摘要不限流）；同笔记并发合并；打卡为显式每日一次，status 含近 7 天轨迹
12. S5 复测：搜索缓存键 = 版本号 + SHA-256（弃 String.hashCode）；笔记增删改 afterCommit 自增 Redis 版本号立即失效；搜索结果按 ID 顺序还原相关度
13. S5 复测：STOMP CONNECT 除 JWT 外查 users 表，封禁/已删除用户拒绝新连接；鉴权切面统一抛 BusinessException（不再直接返回 ApiResponse，避免 ResponseEntity 接口 500）

## 工作纪律（每步必须）
三道门 + 日志：改动 → commit；测试全绿；里程碑前 diff 自审并附清单；追加 docs/project-log.md；刷新本文件。详见 memory.md。

## 如何运行
- 数据库已就绪：notehub 库 + 19 表 + 种子数据（admin/admin123 管理员、demo/user123456）；迁移 `I4_tags.sql` / `I5_interaction_stats.sql` / `I6_checkins.sql` 均已在本机执行
- 本地配置：backend/src/main/resources/application-local.yaml（gitignore，含 DB 密码 + jwt.secret）
- 后端：`cd backend && mvn spring-boot:run` → `curl http://localhost:8080/api/hello`
- 前端：`cd frontend && npm run dev` → http://localhost:5173（Vite 代理 /api、/images 到 8080）
- 测试：`mvn test`（207，无需 DB/Redis）；`npm test`（41）/ `npm run build`
- I4 标签：`GET /api/tags`；题目/笔记列表支持 `?tag=`
- I5：热点列表走 Caffeine+Redis；滑动窗口限流 42901；`GET /notes/{id}/stats`
- I6：`POST /notes/{id}/ai-summary`（默认本地摘要，配置 `AI_SUMMARY_API_KEY` 启用模型）；`POST /api/checkin`、`GET /api/checkin/status`
- AI 真模型（DeepSeek 实测）：`AI_SUMMARY_API_KEY=<key> SPRING_APPLICATION_JSON='{"ai":{"summary":{"enabled":true,"base-url":"https://api.deepseek.com/v1","model":"deepseek-chat"}}}' mvn spring-boot:run`；Key 只放环境变量，勿写入仓库
- 本机迁移：`I4_tags.sql`、`I5_interaction_stats.sql`、`I6_checkins.sql` 均已执行
- WebSocket：`ws://localhost:8080/ws`（STOMP；前端开发期经 Vite `/ws` 代理）；CONNECT 校验 JWT+用户未封禁；冒烟脚本 `E:\ka\downloads\smoke\i3-ws-smoke.mjs`
- 搜索缓存：`search:notes:v{version}:{sha256}:{page}:{size}`；笔记增删改后版本号自增（`search:notes:version`），旧缓存 5min 自然过期
- **dev 邮件联调**：mail.verify-code.enabled=false 时 GET /api/email/verify-code 返回 devCode；Redis 不可用时邮件/注册不可用
- I2 接口：`/api/notes/heatmap`、`/api/notes/ranklist`、`/api/notes/top3count`、`/api/notes/download` —— 契约见 docs/api-contract.md §14
- Redis 容器：重启后双击 E:\docker\post-reboot-redis.cmd；docker.exe 实际路径 `E:\docker\resources\bin\docker.exe`
- 冒烟脚本：`E:\ka\downloads\smoke\i2-smoke.mjs`（仓库外，Node fetch；其他 JSON 载荷同目录）

## 后端构建注意（本机实测）
- `JAVA_HOME` 需指向 `E:\jdk 21`；mvn 用 `E:\Maven\apache-maven-3.9.11-bin\apache-maven-3.9.11\bin\mvn.cmd`；仓库 `E:\Maven\mvnrep`
- 用户全局 settings.xml 的 `<mirror>` 节点损坏（warning），构建直连中央仓库，能用；不建议擅动用户配置

## 待用户提供
- 无（邮件保持 dev 模式；MySQL root 密码已在本地配置）

## 环境（本机实测）
- JDK 21.0.12 → E:\jdk 21 ｜ Maven 3.9.11 → E:\Maven ｜ MySQL 8.0.41 → C:\Program Files（服务 MySQL80）
- Node.js 22.23.4 → E:\nodejs（PATH 已配，npm 走 npmmirror）
- Redis：Docker 容器 notehub-redis（6379，restart=unless-stopped）；Docker Desktop → E:\docker

## 环境踩坑记录（E0 实测）
- Node 的 zip 用 Git Bash 的 GNU tar 解不开（"does not look like a tar archive"），要用 PowerShell `Expand-Archive`
- GitHub 直连下载会中断（exit 56）；nodejs.org/desktop.docker.com 直连正常
- `setx` 截断风险 → 配 PATH 用 PowerShell `[Environment]::SetEnvironmentVariable(...,'User')`
- `rank` 是 MySQL8 保留字 → 列名用 `sort_rank`
- 本机 shell 默认非管理员 → 提权走 `Start-Process -Verb RunAs`（用户点 UAC）
- WSL/VMware 类提示输出是 UTF-16LE，`iconv -f UTF-16LE -t UTF-8` 转码可读
- **`wsl --install` 不等于 VirtualMachinePlatform 已启用**；DISM 3010 后仍可能被回滚（本机发生过两次，元凶= MuMu 模拟器 VMM 驱动 MuMuVMMDrv 与 VMP 互斥，每次开机回滚；已禁用。想玩 MuMu：管理员运行 `sc config MuMuVMMDrv start= demand` 改回，代价是 Docker/WSL2 失效）→ 用 PowerShell `Enable-WindowsOptionalFeature` 启用 + `bcdedit /set hypervisorlaunchtype auto` 双保险；ps1 脚本含中文必须 UTF-8 带 BOM，否则 PowerShell 5.1 按 GBK 读会解析爆炸（报“字符串缺少终止符”）
- Docker Desktop `--installation-dir=E:\docker` 是直装根目录：docker.exe 在 `E:\docker\resources\bin\`，GUI 在 `E:\docker\Docker Desktop.exe`
- **Vite dev server 长时间运行后偶发 HMR 漏更新**（浏览器仍执行中间版本代码，接口已变但页面不显示）：重启 `npm run dev` 并刷新页面即可；判断方法：在页面里 fetch `/src/...` 检查源码是否包含最新字符串
- **Docker Desktop 已汉化**（用户需求，2026-11-08）：项目 [asxez/DockerDesktop-CN](https://github.com/asxez/DockerDesktop-CN) 4.98.0 版完美匹配；替换 `E:\docker\frontend\resources\app.asar(+unpacked)`，**原版备份在同目录 `*.en.bak`**；汉化资产归档于 `E:\docker\plugin\`

## 远程仓库
- origin: https://github.com/zaxiu88888/notehub.git（main，**已推送**）
- 公开前检查清单（转 Public 前）：LICENSE ✅ 已补；application-local.yaml 未跟踪 ✅；uploads/target 未入库 ✅；文档中本机绝对路径仅内部参考（如转 Public 可再清一轮）

## 相关资产
- 原项目逆向分析：E:\ka\kamanotes-分析报告.md ｜ 原源码：E:\ka\kamanotes\
- 本仓库：E:\ka\notehub（main 分支）
