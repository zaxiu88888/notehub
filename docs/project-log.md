# notehub 项目日志

> 每干一步追加一条。格式：日期 ｜ 做了什么 ｜ 测试/自审结论 ｜ commit ｜ 遗留问题

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
