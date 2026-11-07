# HANDOFF —— 会话交接文档

> 接手必读。本文件始终反映项目最新状态；每干一步由执行者刷新。
> 最后更新：2026-11-06（D0 完成）

## 项目一句话
仿「卡码笔记」的刷题+笔记社区全栈项目（求职用），MVP=初始框架，按迭代路线逐步补齐并超越原项目。

## 当前进度
| 里程碑 | 状态 |
|---|---|
| D0 文档定稿包（README/技术设计/API 契约/环境手册/调研/日志/交接/规则） | ✅ commit 303e1ac |
| E0-1 建库 notehub（utf8mb4） | ✅ root/12345 验证通过 |
| E0-2 Node 22.23.4 → E:\nodejs（PATH 已配，npm 走 npmmirror） | ✅ |
| E0-3 schema.sql（14 表）+ seed.sql 执行 | ✅ 2 用户/8 题目/1 题单已入库 |
| E0-4 Docker 路线跑 Redis | 🔄 WSL2 已装好；**等用户重启电脑** → 管理员运行 E:\docker\install-docker-admin.cmd |
| M0 后端骨架（Boot 3.5.16 + common/security/config + JWT 链路 + 14 测试全绿） | ✅ 本次完成 |
| 阶段 1 编码（M1 用户模块→题库→笔记→互动→消息→管理端→前端两端） | ⏳ 下一步 M1 |

## 关键决策（不要推翻，除非用户要求）
1. 技术栈：Spring Boot 3.x + JDK21 + MyBatis(XML) + MySQL8 + Redis + JJWT 0.12；前端 Vite+React18+TS+Antd5+RTK+axios
2. MVP 范围：完整管理后台 5 页面 ✚ 真发邮件验证码 ✚ 完整收藏夹；搜索/热力图/排行/WebSocket/标签推后到迭代期
3. 鉴权：Security 只放行+CORS+BCrypt，鉴权 = TokenInterceptor + RequestScope + @NeedLogin/@NeedAdmin AOP（修复原项目 admin 裸奔）
4. 消息：点赞/评论 → Spring Event → 异步写 messages（原项目事件没监听者，我们补上）
5. 计数：一律原子 UPDATE + 唯一约束（answer Issue 教训）
6. 敏感配置全部 application-local.yaml（gitignore），绝不硬编码入库

## 工作纪律（每步必须）
三道门 + 日志：改动 → commit；测试全绿；里程碑前 diff 自审并附清单；追加 docs/project-log.md；刷新本文件。详见 memory.md。

## 如何运行
- 数据库已就绪：notehub 库 + 14 表 + 种子数据（admin/admin123 管理员、demo/user123456）
- 后端：`cd backend && mvn spring-boot:run`（首次启动前必须：`cp src/main/resources/application-local-template.yaml src/main/resources/application-local.yaml` 并填 MySQL 密码 + jwt.secret）
- 测试：`mvn test`（当前 14/14 绿，无需 DB/Redis）
- Redis 容器：重启电脑后运行 E:\docker\install-docker-admin.cmd（自动装 Docker→E:\docker 并起 notehub-redis）
- 前端：frontend/ 尚未创建（M5）

## 后端构建注意（本机实测）
- `JAVA_HOME` 需指向 `E:\jdk 21`；mvn 用 `E:\Maven\apache-maven-3.9.11-bin\apache-maven-3.9.11\bin\mvn.cmd`；仓库 `E:\Maven\mvnrep`
- 用户全局 settings.xml 的 `<mirror>` 节点损坏（warning），构建直连中央仓库，能用；不建议擅动用户配置

## 待用户提供
1. **QQ 邮箱 SMTP 授权码**（M1 用户模块前；没有则先用 mail.enabled=false 的 dev 模式开发）
2. ~~MySQL root 密码~~ ✅ 已提供（root/12345，8.0.41 连通已验证）

## 环境（本机实测 2026-11-05/06）
- JDK 21.0.12 → E:\jdk 21 ｜ Maven 3.9.11 → E:\Maven ｜ MySQL 8.0.41 → C:\Program Files（服务 MySQL80）
- Node.js：未装（计划 E:\nodejs）｜ Redis：未装（计划 E:\redis）
- 用户密码备忘在 E:\ka\新建 文本文档.txt（仓库外，不入库）

## 环境踩坑记录（E0 实测）
- Node 的 zip 用 Git Bash 的 GNU tar 解不开（"does not look like a tar archive"），要用 PowerShell `Expand-Archive`
- GitHub 直连下载会中断（exit 56）；nodejs.org/desktop.docker.com 直连正常
- `setx` 截断风险 → 配 PATH 用 PowerShell `[Environment]::SetEnvironmentVariable(...,'User')`
- `rank` 是 MySQL8 保留字 → 列名用 `sort_rank`
- 本机 shell 默认非管理员 → 提权走 `Start-Process -Verb RunAs`（用户点 UAC）
- WSL/VMware 类提示输出是 UTF-16LE，`iconv -f UTF-16LE -t UTF-8` 转码可读

## 重启后的 Docker 安装步骤（重启 WSL2 生效后）
1. 以管理员身份运行 `E:\docker\install-docker-admin.cmd`（装 Docker Desktop → E:\docker、启动引擎、起 notehub-redis 容器，全自动）
2. 验证：`E:\docker\Docker Desktop\resources\bin\docker.exe exec notehub-redis redis-cli ping` → PONG

## 相关资产
- 原项目逆向分析：E:\ka\kamanotes-分析报告.md ｜ 原源码：E:\ka\kamanotes\
- 本仓库：E:\ka\notehub（main 分支）
