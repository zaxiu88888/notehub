# 同类项目调研报告（prior-art）

> 调研日期：2026-11-05 ｜ 目的：为 notehub 的设计决策提供同类项目参照
> 方法：抓取 GitHub README + Issues（高频需求与痛点），结合 notehub 定位（刷题 + 笔记分享社区）提炼

## 一、对标项目一览

| # | 项目 | Stars | 技术栈 | 与 notehub 的关系 |
|---|---|---|---|---|
| 1 | [liyupi/mianshiya-next](https://github.com/liyupi/mianshiya-next) | ~520 | Spring Boot + MySQL + MyBatis-Plus + Redis/Redisson + ES + React/Next.js | **领域最贴近**：面试刷题平台教学项目 |
| 2 | [QingdaoU/OnlineJudge](https://github.com/QingdaoU/OnlineJudge) | ~6.6k | Django REST + Vue + 沙箱判题机 | OJ 平台架构参考（题库/提交/比赛） |
| 3 | [DMOJ/online-judge](https://github.com/DMOJ/online-judge) | 大规模生产运营 | Django + Celery + WebSocket | OJ 社区功能参考（题解/评级/权限） |
| 4 | [apache/answer](https://github.com/apache/answer) | ~15.7k | Go + React | Q&A 社区设计参考（投票/通知/反垃圾） |
| 5 | [doocs/leetcode](https://github.com/doocs/leetcode) | ~36.8k | 内容仓库 + 静态站 | 内容组织参考（题解索引/模板/协作） |
| 补 | [halo-dev/halo](https://github.com/halo-dev/halo) | ~40.1k | Java（Gradle 多模块）+ 前后端分离 | Java 内容平台工程化参考（评论/插件/OpenAPI） |

## 二、各项目要点与可借鉴处

### 1. mianshiya-next（领域最贴近）
**README 要点**：题库/题目/题解管理、题目批量关联题库、分词检索、刷题记录日历图。
- **Redis BitMap 刷题日历**：一个用户一年刷题记录 = 365 bit，热力图查询零表扫描 → notehub 的刷题热力图采用此方案（原卡码笔记用 SQL 按天聚合，数据量大时慢）
- **多级缓存**：Redis + Caffeine 本地缓存 + HotKey 热点探测 → 首页热门题库可借鉴
- **ES 分词搜索 + 数据同步** → notehub MVP 用 MySQL FULLTEXT 起步，迭代期再评估 ES/Meilisearch
- **批量操作/批处理** → 题目批量导入功能同源，注意批处理写法
- **Sa-Token/Sentinel/Nacos** → notehub 用 Spring Security + 自研 JWT 拦截器即可，不必引入全家桶（教学项目炫技 ≠ 真实项目最优）

### 2. QingdaoU/OnlineJudge
- 前后端分离 + **判题机分层**（Judger 沙箱 → JudgeServer 服务）→ notehub 不做判题，但其「模块划分 problem/submission/contest/account」和「更合理的权限控制」值得对标
- Docker 一键部署 → notehub 后续提供 docker-compose

### 3. DMOJ
- **细粒度权限控制**、题面内嵌题解（editorial，赛后自动发布）、实时提交状态（WebSocket）、OAuth + 2FA、Elo-MMR 评级
- 借鉴：权限体系要一开始就分好 user/admin 两级并集中在注解里；「题解与题目联动」是内容型产品的好形态

### 4. apache/answer
- **投票并发安全**（Issue #1646：并发投票用过期数据覆盖 vote_count）→ notehub 计数一律原子 `UPDATE ... SET x = x + 1`，禁止读-改-写
- **顺序 ID 内容泄露**（#1506：已删除内容可被顺序 ID 枚举）→ 用户侧接口避免暴露自增 ID 的地方用雪花 ID/短 ID
- **反垃圾**（#1601：垃圾内容批量清理是运营刚需）→ 限流 + 封禁（users.is_banned）进设计
- **通知扇出限制**（#1617：@ 通知防滥用）
- 注册邮箱验证可配置开关（#1517）→ notehub 邮件做成开关降级
- 插件系统、AI 集成（PAT/AI provider/翻译）是社区最活跃方向 → notehub 迭代期 I6

### 5. doocs/leetcode
- **双维度索引**：既按题号/题集组织，又按算法考点（标签）分类 → notehub 的 tags/note_tags 设计依据
- **统一题解模板**（题目描述 + 思路 + 多语言代码）→ 笔记编辑器预设模板
- 工程化与协作规范（模板/贡献指南/许可证）→ notehub 的 docs 体系同思路

### 6. halo（补充参照）
- Java 多模块工程（api/application/ui 分离）、OpenAPI v3 文档、评论系统（时间倒序/回复计数是其 Issue 高频区）、备份恢复是核心数据安全痛点

## 三、Issues 高频痛点归纳（我们主动避坑）

| 痛点 | 来源 | notehub 对策 |
|---|---|---|
| 部署门槛高、文档失效 | OnlineJudge #504/#487/#506 | 环境手册 + docker-compose + README 快速开始 |
| 并发计数被覆盖 | answer #1646 | 所有计数原子更新，唯一约束防重复点赞 |
| 顺序 ID 泄露内容 | answer #1506 | 敏感场景非顺序 ID；删除内容接口校验归属 |
| 垃圾内容清理难 | answer #1601 | is_banned 封禁 + 管理端批量删除预留 |
| 判题链路不稳定/卡等待 | OnlineJudge #505/#492 | 不做判题（范围外），但异步任务要有超时与重试设计 |
| 安全漏洞（IDOR/开放重定向/SpEL 注入） | OnlineJudge #514、halo #10247/#10259 | 资源归属校验、白名单跳转、禁用模板引擎动态求值 |
| 评论排序/计数细节 | halo #10432/#10425 | 评论接口支持排序参数；reply_count 维护逻辑写进设计 |

## 四、可拓展方向（超越原项目的候选清单，按优先级）

1. **搜索升级**：MySQL FULLTEXT(ngram)+Jieba 起步 → 评估 Meilisearch/ES
2. **刷题热力图 BitMap 化**（Redis BitMap，面试亮点）
3. **WebSocket 实时通知**（原项目只有 5s 轮询）
4. **AI 辅助**：题解总结/笔记润色/站内问答（answer 社区最活跃方向）
5. **打卡 streak / 题单进度条 / 学习路线图**（doocs 双维度索引）
6. **工程化**：OpenAPI 文档（Apifox 导入）、docker-compose、CI（GitHub Actions 跑测试）

## 五、对 MVP 骨架的结论

MVP 只保留撑起核心闭环的最小集：**统一基础设施（响应/异常/JWT）→ 用户（注册登录）→ 题库（只读+管理端录入）→ 笔记核心 → 点赞/收藏/评论 → 消息**。
明确不进 MVP：判题沙箱、比赛、ES、插件系统、AI、标签（I4 加入）。功能按迭代路线（I1→I6）逐步加入，最终完整覆盖原项目 60+ 接口并超越。

## 参考链接
- [mianshiya-next](https://github.com/liyupi/mianshiya-next) ｜ [QingdaoU/OnlineJudge](https://github.com/QingdaoU/OnlineJudge) ｜ [DMOJ/online-judge](https://github.com/DMOJ/online-judge)
- [apache/answer](https://github.com/apache/answer) ｜ [doocs/leetcode](https://github.com/doocs/leetcode) ｜ [halo-dev/halo](https://github.com/halo-dev/halo)
