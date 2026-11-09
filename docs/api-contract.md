# notehub API 契约 v1.0（MVP 全量接口）

> 对齐原项目路径便于对照学习；差异：所有 `/api/admin/**` 标注 `@NeedAdmin`（原项目缺失）。
> 通用前缀 `/api`；未登录可访问的接口标 🌐，需登录标 🔒，需管理员标 👑。

## 0. 通用约定

### 响应格式
```json
// 所有接口（除下载）统一
{ "code": 200, "message": "success", "data": { } }
// 分页 data
{ "list": [ ... ], "page": 1, "pageSize": 10, "total": 100 }
```
- 错误码见 technical-design.md §7；HTTP 状态一般 200（业务错误用 code 区分），系统异常 500
- 认证：请求头 `Authorization: Bearer <jwt>`（除 🌐 接口外必带；🌐 接口带 token 会返回"我"维度的附加字段，如是否已点赞）

### 分页参数（GET 均支持）
`page`(默认1) `pageSize`(默认10, 最大50)

---

## 1. 用户模块 /api/users

| 方法 | 路径 | 权限 | 说明 |
|---|---|---|---|
| POST | `/users` | 🌐 | 注册 |
| POST | `/users/login` | 🌐 | 登录 |
| POST | `/users/whoami` | 🔒 | 当前用户信息 |
| GET | `/users/{userId}` | 🌐 | 他人主页信息（含其公开统计） |
| PATCH | `/users/me` | 🔒 | 修改资料 |
| POST | `/users/avatar` | 🔒 | 上传头像（multipart, image/*, ≤5MB） |
| GET | `/admin/users` | 👑 | 用户列表（page/pageSize/account 模糊/isBanned 筛选） |
| PATCH | `/admin/users/{userId}/ban` | 👑 | 封禁/解封 `{isBanned: 0|1}` |

**POST /users** 请求体：
```json
{ "email": "x@qq.com", "code": "123456", "account": "abc", "username": "昵称", "password": "≥6位" }
```
校验：email 格式；code 6 位；account 4-20 字母数字；password ≥6。冲突 → 40901；验证码错 → 41001。
**响应 data**：`{ userId, token }`（登录成功即发 token）。

## 2. 邮件模块 /api/email

| 方法 | 路径 | 权限 | 说明 |
|---|---|---|---|
| GET | `/email/verify-code?email=` | 🌐 | 发送注册验证码（60s 限流 → 41002；队列异步发） |

data：`{ sent: true }`（dev 开关关闭时额外返回 devCode 便于联调，生产配置不返回）。

## 3. 分类模块 /api

| 方法 | 路径 | 权限 |
|---|---|---|
| GET | `/categories` | 🌐（树形，含子分类） |
| POST | `/admin/categories` | 👑 `{name, parentCategoryId?}` |
| PATCH | `/admin/categories/{id}` | 👑 `{name?, parentCategoryId?}` |
| DELETE | `/admin/categories/{id}` | 👑（有子分类/有题目时拒绝） |

## 4. 题目模块 /api

| 方法 | 路径 | 权限 | 说明 |
|---|---|---|---|
| GET | `/questions` | 🌐 | 列表：categoryId、difficulty、page 分页 |
| GET | `/questions/{id}` | 🌐 | 详情（浏览数 +1；🔒 附 myNote：我在这道题下的笔记） |
| POST | `/admin/questions` | 👑 | `{categoryId, title, difficulty(1-3), examPoint?, description?}` |
| POST | `/admin/questions/batch` | 👑 | `{categoryId, markdown}` Markdown 批量导入（模板见 §7） |
| PATCH | `/admin/questions/{id}` | 👑 | 同创建字段，均可选 |
| DELETE | `/admin/questions/{id}` | 👑 | 有笔记的题目拒绝删除 |

## 5. 题单模块 /api

| 方法 | 路径 | 权限 |
|---|---|---|
| GET | `/questionlists` | 🌐 列表 |
| GET | `/questionlists/{id}` | 🌐 详情 |
| GET | `/questionlist-items?questionListId=` | 🔒 题单内题目（含我每题是否已有笔记 → 刷题进度） |
| POST | `/admin/questionlists` | 👑 `{name, type, description}` |
| PATCH / DELETE | `/admin/questionlists/{id}` | 👑 |
| GET | `/admin/questionlist-items/{listId}` | 👑 |
| POST | `/admin/questionlist-items` | 👑 `{questionListId, questionId, sortRank?}`（uk 防重，不传 rank 追加末尾） |
| DELETE | `/admin/questionlist-items/{listId}/{questionId}` | 👑 |
| PATCH | `/admin/questionlist-items/sort` | 👑 `{questionListId, items:[{questionId}]}` 顺序即 1..n |

## 6. 笔记模块 /api

| 方法 | 路径 | 权限 | 说明 |
|---|---|---|---|
| GET | `/notes` | 🌐 | 列表：questionId、authorId 筛选；聚合作者/题目/我的互动状态（防N+1） |
| GET | `/notes/mine?questionId=` | 🔒 | 我在该题下的笔记ID（无则 -1）；个人中心列表用 `GET /notes?authorId=` |
| POST | `/notes` | 🔒 | `{questionId, content}`（Markdown，≤50000 字） |
| PATCH | `/notes/{noteId}` | 🔒 | 仅作者（40301） |
| DELETE | `/notes/{noteId}` | 🔒 | 仅作者 |

**NoteVO**：`{noteId, content, likeCount, commentCount, collectCount, createdAt, author:{userId,username,avatarUrl}, question:{questionId,title,difficulty}, myActions:{liked,collected}}`（🌐 时 myActions 恒 false）。

## 7. 批量导入模板（POST /admin/questions/batch）
```markdown
# 两数之和
难度：简单
考点：数组；哈希表
（题干正文……）

# 无重复字符的最长子串
难度：中等
考点：滑动窗口
……
```
解析规则：`# ` 开头为题目标题；`难度：`行 ∈ {简单,中等,困难} → 1/2/3；`考点：`行分号分隔；其余为题干（MVP 仅存题干为描述列）。响应：`{total, success, failures:[{title, reason}]}`。

## 8. 互动模块 /api

| 方法 | 路径 | 权限 | 说明 |
|---|---|---|---|
| POST | `/like/note/{noteId}` | 🔒 | 点赞（幂等，重复 → 200 不重复计数） |
| DELETE | `/like/note/{noteId}` | 🔒 | 取消点赞 |
| GET | `/collections` | 🔒 | 我的收藏夹列表（含每个夹内数量） |
| POST | `/collections` | 🔒 | `{name, description?}` |
| DELETE | `/collections/{collectionId}` | 🔒 | 仅创建者 |
| POST | `/collections/batch` | 🔒 | `{noteIds:[], collectionId}` 批量收藏（uk 幂等） |
| GET | `/collection-notes?collectionId=` | 🔒 | 收藏夹内笔记列表（复用 NoteVO 聚合） |
| DELETE | `/collection-notes/{collectionId}/{noteId}` | 🔒 | 移出收藏夹 |
| GET | `/comments?noteId=&page=` | 🌐 | 评论列表（含 2 级回复树、排序=时间正序；🔒 附 liked） |
| POST | `/comments` | 🔒 | `{noteId, parentId?, content}` |
| PATCH | `/comments/{commentId}` | 🔒 | 仅作者 |
| DELETE | `/comments/{commentId}` | 🔒 | 仅作者（级联软删回复，回复计数递减） |
| POST / DELETE | `/comments/{commentId}/like` | 🔒 | 评论点赞/取消 |

## 9. 消息模块 /api/messages（全部 🔒）

| 方法 | 路径 | 说明 |
|---|---|---|
| GET | `/messages` | 列表 `type`(1赞/2评/3系统) 筛选分页；MessageVO 含发送者摘要+目标摘要 |
| GET | `/messages/unread/count` | `{total, like, comment, system}` 分类型未读数（I3 起由 WebSocket 实时推送，本接口用于连接/重连补齐与消息页加载） |
| PATCH | `/messages/{messageId}/read` | 单条已读 |
| PATCH | `/messages/all/read` | 全部已读（非本人消息操作返回 40401） |
| PATCH | `/messages/batch/read` | `{messageIds:[]}` |
| DELETE | `/messages/{messageId}` | 删除 |

产生时机：他人点赞我的笔记（LIKE）、评论/回复我的笔记（COMMENT）；自赞自评不产生。

## 10. 统计模块 /api

| 方法 | 路径 | 权限 | 说明 |
|---|---|---|---|
| GET | `/statistic` | 👑 | 看板：最近 30 天 loginCount/registerCount/noteCount 曲线 + 今日 6 项指标 |

## 11. 上传模块 /api

| 方法 | 路径 | 权限 | 说明 |
|---|---|---|---|
| POST | `/upload/image` | 🔒 | multipart `file`，白名单 jpg/png/webp/gif，≤5MB；data `{url}`（经 /images/ 映射） |

## 12. 健康检查

| GET | `/api/hello` | 🌐 | `{message:"notehub ok"}` |

---

## 13. 搜索 /api/search（I1 已实现）

| 方法 | 路径 | 权限 | 说明 |
|---|---|---|---|
| GET | `/search/notes?keyword=&page=&pageSize=` | 🌐 | 笔记全文搜索（题目 + 正文，ngram 索引 + Jieba 预处理），按相关度排序；登录时返回 myActions |
| GET | `/search/users?keyword=&page=&pageSize=` | 🌐 | 用户搜索（昵称/账号模糊），不返回邮箱 |

- 空/纯符号关键词 → 空列表；关键词长度截断 64 字符；结果缓存 5 分钟（按关键词哈希+分页，仅缓存 ID 与总数）
- 前端入口：顶栏搜索框 → `/search?keyword=`

## 14. 数据可视化 /api/notes（I2 已实现）

| 方法 | 路径 | 权限 | 说明 |
|---|---|---|---|
| GET | `/notes/heatmap?year=` | 🔒 | 我的刷题热力图；默认今年，返回全年逐日 `{date,count}` 及 `total/activeDays/max` |
| GET | `/notes/ranklist?period=&limit=` | 🌐 | 笔记热度榜；`period=week`(近7天)/`month`(近30天)/`all`(总榜)，`limit` 1..50 默认 10 |
| GET | `/notes/top3count` | 🔒 | 个人数据卡片：笔记数/收获赞评藏/累计字数/活跃天数/当前与最长连续提交/本月新增 |
| GET | `/notes/download?noteId=` | 🔒 | Markdown 附件下载；不带 `noteId` 导出全部，带 `noteId` 导出单篇（仅作者） |

- 热力图按笔记创建日计数；Redis 位图 `heatmap:{userId}:{year}`（每天 1 字节）缓存 1 小时，笔记增删事件增量维护，Redis 不可用自动降级查库
- 热度榜 `hotScore = 赞*2 + 评论*3 + 收藏*2`；窗口榜单按互动记录时间过滤，结果缓存 5 分钟（公共数据）
- 导出响应 `Content-Type: text/markdown;charset=UTF-8`，`Content-Disposition: attachment`

## 15. 实时通知 WebSocket（I3 已实现）

| 项 | 说明 |
|---|---|
| 端点 | `ws://{host}/ws`（原生 WebSocket + STOMP 1.2，无 SockJS） |
| 鉴权 | STOMP `CONNECT` 原生头 `Authorization: Bearer <jwt>`（也兼容 `token` 头）；无效 token 返回 ERROR 并断开 |
| 订阅 | `/user/queue/unread`（仅允许 `/user/queue/**`，其他订阅/客户端 SEND 一律拒绝） |
| 推送载荷 | `{total, like, comment, system}`，与 `GET /messages/unread/count` 一致 |
| 触发时机 | 新消息落库（点赞/评论通知）、单条/批量/全部已读、删除消息 |
| 断线 | 前端 5s 自动重连；每次连接/重连后用 HTTP 补拉一次未读数 |

- 推送目标按 Principal(userId) 解析, 同一账号多标签页会同时收到
- 开发期前端经 Vite `/ws` 代理连接后端；生产同源直连

## 附：MVP 后迭代接口预留（不在本期实现）
- I4 标签：`GET /tags` `GET /questions?tag=`
