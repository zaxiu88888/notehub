# notehub

> 刷题 + 笔记分享社区 —— 在题目下写笔记，被点赞、收藏、评论，见证每一次进步。
> 从零实现的全栈项目（对标「卡码笔记」并超越它），按真实可交付标准开发。

## 功能特性

**用户端**
- 邮箱验证码注册 / 登录（JWT），个人资料与头像
- 浏览题库：两级分类、难度筛选、题单（含刷题进度）
- 写笔记：Markdown 编辑器，聚合展示（作者/题目/互动状态）
- 互动：笔记点赞、多收藏夹、评论与回复
- 消息中心：点赞/评论/系统通知，未读数提醒
- （迭代中）搜索、刷题热力图、排行榜、笔记导出

**管理端**
- 题库管理：分类/题目/题单 CRUD，Markdown 批量导入题目
- 用户管理：检索、封禁
- 数据看板：每日登录/注册/笔记统计（定时任务落库）

## 技术栈

| | |
|---|---|
| 后端 | Spring Boot 3.x · Java 21 · MyBatis(XML) · MySQL 8 · Redis · JJWT 0.12 · Log4j2 · Flexmark |
| 前端 | Vite · React 18 · TypeScript · Ant Design 5 · Redux Toolkit · axios · cherry-markdown |
| 质量 | JUnit 5 · Vitest · 三道门纪律（commit/测试/自审，见 [memory.md](memory.md)） |

## 架构

```
React 18 (Antd5)  ──HTTP/Bearer JWT──▶  Spring Boot 3 (:8080)
                                          ├─ TokenInterceptor + @NeedLogin/@NeedAdmin
                                          ├─ Controller → Service → Mapper(XML)
                                          ├─ Spring Event → 消息通知
                                          └─ Redis: 邮件队列/缓存/限流
◀── 本地磁盘 uploads/ ◀─ MySQL 8 (14 tables) ─┘
```

## 快速开始

环境与安装细节见 [docs/environment-setup.md](docs/environment-setup.md)。

```bash
# 1. 依赖服务：MySQL80 服务 + Redis (E:\redis\redis-server.exe)
# 2. 初始化数据库（建库 + schema.sql + seed.sql，见环境手册 §4）
# 3. 后端
cd backend && mvn spring-boot:run        # :8080
# 4. 前端
cd frontend && npm install && npm run dev  # :5173
```

默认管理员账号见 seed.sql 注释。

## 目录结构

```
notehub/
├── backend/                 # Spring Boot 3（com.notehub）
│   └── src/main/resources/db/   # schema.sql / seed.sql
├── frontend/                # Vite + React18 + TS
├── docs/
│   ├── technical-design.md  # 技术设计（架构/选型/表设计/核心流程）
│   ├── api-contract.md      # API 契约（60+ 接口）
│   ├── environment-setup.md # 环境手册
│   ├── prior-art.md         # 同类项目调研
│   ├── project-log.md       # 项目日志（每步追加）
│   └── HANDOFF.md           # 会话交接（随时反映最新状态）
└── memory.md                # 项目工作规则（三道门）
```

## 迭代路线

- **阶段 1（进行中）** 初始框架：统一基础设施 → 用户 → 题库+批量导入 → 笔记 → 互动 → 消息 → 管理端+统计 → 前端两端 → 联调
- **阶段 2** I1 搜索(FULLTEXT+Jieba) → I2 热力图/排行/导出 → I3 WebSocket 推送 → I4 标签体系 → I5 并发与缓存深化 → I6 AI 辅助等拓展

## 文档

| 文档 | 内容 |
|---|---|
| [技术设计](docs/technical-design.md) | 为什么这样选型、表结构、核心流程 |
| [API 契约](docs/api-contract.md) | 全量接口：参数/响应/错误码 |
| [环境手册](docs/environment-setup.md) | 从零跑起来的每一步 |
| [调研报告](docs/prior-art.md) | 同类项目借鉴与避坑 |
| [项目日志](docs/project-log.md) | 每一步做了什么、测试与自审结论 |
| [交接文档](docs/HANDOFF.md) | 当前进度与下一步（接手必读） |
