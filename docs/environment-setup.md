# notehub 环境手册（Windows 本机）

> 按本机实际情况编写：JDK/Maven/MySQL 已装，Node 与 Redis 需安装。
> 所有新增工具统一放 E 盘，与现有习惯一致。

## 1. 已具备（核对即可）

| 组件 | 版本 | 位置 |
|---|---|---|
| JDK | 21.0.12 | `E:\jdk 21`（另有 E:\jdk = JDK11，勿用） |
| Maven | 3.9.11 | `E:\Maven\apache-maven-3.9.11-bin`（本地仓库 E:\Maven\mvnrep） |
| MySQL Server | 8.0.41 | `C:\Program Files\MySQL\MySQL Server 8.0`（服务名 MySQL80） |
| Apifox | — | `E:\Apifox`（接口调试） |

核对命令：
```bash
"E:\jdk 21\bin\java.exe" -version     # 21.x
E:\Maven\apache-maven-3.9.11-bin\apache-maven-3.9.11\bin\mvn.cmd -v
sc query MySQL80                       # 服务存在且 RUNNING
```

## 2. Node.js 安装（前端必需）

1. 下载 Node.js 22 LTS Windows zip（https://nodejs.org/dist/latest-v22.x/ ，选 `node-v22.x.x-win-x64.zip`）
2. 解压到 `E:\nodejs`（要求 `E:\nodejs\node.exe` 存在）
3. 加入用户 PATH：`setx PATH "%PATH%;E:\nodejs"`（或 系统设置→环境变量）
4. 验证（新开终端）：`node -v` → v22.x；`npm -v`
5. 镜像已配好：`C:\Users\11733\.npmrc` → registry.npmmirror.com

## 3. Redis（Docker 方案，实际采用）

> 决策记录：用户选 Docker 路线（放弃 zip 版）。WSL2 需先启用（一次性，管理员 + 重启）。

一次性准备（已完成/执行顺序）：
1. **启用 WSL2**（管理员）：`wsl --install --no-distribution` → **重启电脑**
2. **安装 Docker Desktop**（管理员）：直接运行 `E:\docker\install-docker-admin.cmd` —— 脚本自动完成：安装到 `E:\docker`（`--installation-dir`）→ 启动引擎 → `docker run -d --name notehub-redis -p 6379:6379 --restart unless-stopped redis:7-alpine`
3. 验证：
```bash
"E:\docker\Docker Desktop\resources\bin\docker.exe" exec notehub-redis redis-cli ping   # PONG
# 连接串(后端配置): redis://localhost:6379
```

镜像加速（拉 redis 镜像慢时配置）：Docker Desktop → Settings → Docker Engine 加
```json
"registry-mirrors": ["https://docker.m.daocloud.io", "https://dockerproxy.net"]
```

## 4. 数据库初始化

```bash
# 1) 建库（utf8mb4）
"C:\Program Files\MySQL\MySQL Server 8.0\bin\mysql.exe" -uroot -p -e "CREATE DATABASE IF NOT EXISTS notehub DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;"

# 2) 建表 + 种子数据（schema.sql 落地后执行）
... -uroot -p notehub < backend\src\main\resources\db\schema.sql
... -uroot -p notehub < backend\src\main\resources\db\seed.sql
```

## 5. 敏感配置（application-local.yaml，gitignore 不入库）

创建 `backend/src/main/resources/application-local.yaml`：
```yaml
spring:
  datasource:
    username: root
    password: "12345"
mail:
  username: 你的QQ邮箱@qq.com
  password: 你的SMTP授权码        # QQ邮箱→设置→账户→开启SMTP→获取授权码
jwt:
  secret: <随机≥32位字符串>
```
激活：`application.yaml` 中 `spring.profiles.active: dev,local`。

## 6. 启动顺序

```bash
# 1. MySQL80 服务（通常开机自启）
# 2. Redis
E:\redis\redis-server.exe
# 3. 后端（端口 8080）
cd backend && mvn spring-boot:run
# 验证: curl http://localhost:8080/api/hello
# 4. 前端（端口 5173）
cd frontend && npm install && npm run dev
# 浏览器 http://localhost:5173
```

## 7. 常见问题
| 现象 | 处理 |
|---|---|
| mysql 不是内部命令 | 用绝对路径 `C:\Program Files\MySQL\...\bin\mysql.exe` |
| node 不生效 | 新开终端（setx 不影响已开窗口） |
| Docker 引擎起不来 | 先确认 WSL2 已启用（`wsl --status`）；MuMu 模拟器与 Hyper-V 冲突时开 MuMu 设置里的兼容模式 |
| 拉镜像超时 | 配 registry-mirrors（见 §3） |
| 后端连不上 Redis | `docker ps` 看 notehub-redis 是否 Up；端口 6379 是否被占 |
| 邮件发送失败 | 授权码是否用「授权码」而非 QQ 密码；465 SSL 是否开启 |
| 8080 被占用 | `netstat -ano | findstr 8080` 找 PID 结束，或改 server.port |
