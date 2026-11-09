| M5-C 个人中心/他人主页/消息中心（15 测试绿, 浏览器验证） | ✅ 本次完成 |
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
| E0-4 Docker 路线跑 Redis | ✅ **2026-11-09 攻克**: 删除 MuMu VMM 驱动后第4次启用 VMP 存活; 引擎 29.9.2; notehub-redis 容器 Up + PONG; restart=unless-stopped; 计划任务 DockerDesktopAutoStart 已设开机自启; 邮件验证码+注册全链路真 Redis 回归通过 |
| M0 后端骨架（Boot 3.5.16 + common/security/config + JWT 链路） | ✅ 62a5403 |
| M1 用户模块（注册/登录/验证码队列/资料/管理端列表+封禁, 36 测试全绿） | ✅ c668b6d |
| M2 题库模块（分类树/题目 CRUD/批量导入/题单/条目排序, 56 测试全绿） | ✅ bf73394 |
| S1 真实环境冒烟（后端首启+21 项 curl 全过; 抓出 4 个单测盲区缺陷已修, 57/57 绿） | ✅ 本次完成 |
| M3 笔记核心（笔记/点赞/收藏夹/评论/消息 23 接口; 聚合防N+1; 事件驱动消息; 87 测试+23 项冒烟全绿） | ✅ 本次完成 |

## 关键决策（不要推翻，除非用户要求）
0. **邮件交付方式（2026-11-08 用户决定）**：取消 SMTP 授权码需求——验证码**保持 dev 开关模式交付**（enabled=false，验证码直接落 Redis 并经 devCode 返回/日志）；真发链路代码保留为可选项，谁配了 SMTP 谁打开
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
- 本地配置已生成：backend/src/main/resources/application-local.yaml（gitignore，含 DB 密码 + 随机 jwt.secret）
- 后端：`cd backend && mvn spring-boot:run` → 冒烟入口 `curl http://localhost:8080/api/hello`
- 测试：`mvn test`（当前 57/57 绿，无需 DB/Redis）
- **dev 邮件联调**：mail.verify-code.enabled=false 时 GET /api/email/verify-code 返回 devCode；**Redis 就绪前邮件/注册接口不可用**
- 已实现接口：hello、email/verify-code、users(注册/login/whoami/{id}/me)、admin/users(+ban)、categories(+admin)、questions(+admin/batch)、questionlists(+admin)、questionlist-items(+admin/sort) —— 契约见 docs/api-contract.md
- Redis 容器：重启电脑后双击 E:\docker\post-reboot-redis.cmd（Docker Desktop 已装入 E:\docker）
- 前端：frontend/ 尚未创建（M5）
- 冒烟参考：JSON 载荷在 E:\ka\downloads\smoke\（node 生成 UTF-8 文件 + curl --data-binary；Windows 控制台 curl 直发中文会 GBK 乱码）

## 后端构建注意（本机实测）
- `JAVA_HOME` 需指向 `E:\jdk 21`；mvn 用 `E:\Maven\apache-maven-3.9.11-bin\apache-maven-3.9.11\bin\mvn.cmd`；仓库 `E:\Maven\mvnrep`
- 用户全局 settings.xml 的 `<mirror>` 节点损坏（warning），构建直连中央仓库，能用；不建议擅动用户配置

## 待用户提供
1. ~~QQ 邮箱 SMTP 授权码~~ ❌ 已取消（2026-11-08 用户决定：邮件保持 dev 开关模式，真发为可选配置）
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
- **`wsl --install` 不等于 VirtualMachinePlatform 已启用**；DISM 3010 后仍可能被回滚（本机发生过两次，元凶= MuMu 模拟器 VMM 驱动 MuMuVMMDrv 与 VMP 互斥，每次开机回滚；已禁用。想玩 MuMu：管理员运行 `sc config MuMuVMMDrv start= demand` 改回，代价是 Docker/WSL2 失效）→ 用 PowerShell `Enable-WindowsOptionalFeature` 启用 + `bcdedit /set hypervisorlaunchtype auto` 双保险；ps1 脚本含中文必须 UTF-8 带 BOM，否则 PowerShell 5.1 按 GBK 读会解析爆炸（报"字符串缺少终止符"）
- Docker Desktop `--installation-dir=E:\docker` 是直装根目录：docker.exe 在 `E:\docker\resources\bin\`，GUI 在 `E:\docker\Docker Desktop.exe`
- **Docker Desktop 已汉化**（用户需求，2026-11-08）：项目 [asxez/DockerDesktop-CN](https://github.com/asxez/DockerDesktop-CN) 4.98.0 版完美匹配（该仓库逐版本跟进，GitHub 直连超时时用镜像 ghfast.top 下载）；替换 `E:\docker\frontend\resources\app.asar(+unpacked)`，**原版备份在同目录 `*.en.bak`**，回滚=改回文件名。官方语言设置在该版本不存在（仅 en-US 包）；asar 内中文验证必须按 UTF-8 读（latin1 读中文永远匹配不到）
- **汉化资产归档于 `E:\docker\plugin\`**（用户指定路径）：app-win.zip（4.98.0 汉化包）+ apply-cn.ps1/apply-cn.cmd（重汉化脚本，路径已指向 plugin 目录）+ apply.log；Docker Desktop 升级后界面变英文时：下载对应新版包放这里，右键管理员运行 apply-cn.ps1 即可

## 重启后的 Docker 安装步骤（重启 WSL2 生效后）
1. 以管理员身份运行 `E:\docker\install-docker-admin.cmd`（装 Docker Desktop → E:\docker、启动引擎、起 notehub-redis 容器，全自动）
2. 验证：`E:\docker\Docker Desktop\resources\bin\docker.exe exec notehub-redis redis-cli ping` → PONG

## 远程仓库（待推送）
- origin: https://github.com/zaxiu88888/notehub.git （2026-11-09 建仓, 用户要求**项目完成后再推送**）
- 本地已有 10+ commit, 未 push
- **公开前检查清单**（转 Public 之前必须做）:
  1. 清理本交接文档/项目日志中的本机绝对路径与「密码备忘文件」等描述（仅内部参考, 公开仓库无需暴露）
  2. 确认 application-local.yaml 未被跟踪（已 gitignore）
  3. 补 LICENSE（建议 MIT）
  4. 确认 uploads/ 与 target/ 未入库
  5. 最后再 push: git push -u origin main

## 相关资产
- 原项目逆向分析：E:\ka\kamanotes-分析报告.md ｜ 原源码：E:\ka\kamanotes\
- 本仓库：E:\ka\notehub（main 分支）

| M5-A 前端骨架+首页+题单+登录（真浏览器 E2E 通过） | ✅ a29c9bd |
| M4 管理端统计看板 + 图片上传 + 每日快照（101 测试全绿, 后端 MVP 完成） | ✅ 本次完成 |