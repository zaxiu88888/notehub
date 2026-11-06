# HANDOFF —— 会话交接文档

> 接手必读。本文件始终反映项目最新状态；每干一步由执行者刷新。
> 最后更新：2026-11-06（D0 完成）

## 项目一句话
仿「卡码笔记」的刷题+笔记社区全栈项目（求职用），MVP=初始框架，按迭代路线逐步补齐并超越原项目。

## 当前进度
| 里程碑 | 状态 |
|---|---|
| D0 文档定稿包（README/技术设计/API 契约/环境手册/调研/日志/交接/规则） | ✅ 本次完成 |
| E0 环境准备（Node 22、Redis 安装；建库 notehub + schema/seed.sql） | ⏳ 下一步 |
| 阶段 1 编码（基础设施→用户→题库→笔记→互动→消息→管理端→前端两端） | 未开始 |
| 阶段 2 迭代（I1 搜索 … I6 AI 拓展） | 未开始 |

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
见 docs/environment-setup.md §6（MySQL80 + Redis → mvn spring-boot:run → npm run dev）。
当前仓库还没有代码，E0 完成后此节会更新为实际命令。

## 待用户提供
1. **QQ 邮箱 SMTP 授权码**（M1 用户模块前；没有则先用 mail.enabled=false 的 dev 模式开发）
2. ~~MySQL root 密码~~ ✅ 已提供（root/12345，8.0.41 连通已验证）

## 环境（本机实测 2026-11-05/06）
- JDK 21.0.12 → E:\jdk 21 ｜ Maven 3.9.11 → E:\Maven ｜ MySQL 8.0.41 → C:\Program Files（服务 MySQL80）
- Node.js：未装（计划 E:\nodejs）｜ Redis：未装（计划 E:\redis）
- 用户密码备忘在 E:\ka\新建 文本文档.txt（仓库外，不入库）

## 相关资产
- 原项目逆向分析：E:\ka\kamanotes-分析报告.md ｜ 原源码：E:\ka\kamanotes\
- 本仓库：E:\ka\notehub（main 分支）
