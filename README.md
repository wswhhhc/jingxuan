# 菁选 (Jingxuan) — 校园作品展示平台

[![CI](https://github.com/wswhhhc/jingxuan-/actions/workflows/ci.yml/badge.svg)](https://github.com/wswhhhc/jingxuan-/actions/workflows/ci.yml)

基于 **Spring Boot 3.2.5 + Vue 3** 的全栈校园作品展示平台，支持学生提交作品、教师匿名评分、管理员审核发布、前台公开展示。集成 DeepSeek AI 内容安全审核、Redis 排行榜缓存、Docker 一键部署、GitHub Actions CI/CD 流水线。

---

## 技术栈

| 层 | 技术 |
|---|---|
| 后端框架 | Spring Boot 3.2.5 / Java 17 |
| ORM | MyBatis-Plus 3.5.7（雪花 ID + 逻辑删除） |
| 数据库 | MySQL 8.0+（utf8mb4） |
| 缓存 | Redis 7+（Lettuce，排行榜缓存 + 限流计数器） |
| 安全 | Spring Security + JWT（jjwt 0.12.5） |
| API 文档 | Knife4j 4.5.0（OpenAPI 3） |
| 内容审核 | DeepSeek API + DFA 敏感词 |
| 前端 | Vue 3 + TypeScript + Vite 8 |
| UI | Element Plus 2.14 + Pinia 3 |
| 测试 | JUnit 5 + Mockito + Vitest |
| 部署 | Docker / PM2 + Nginx |
| CI/CD | GitHub Actions |

---

## 功能概览

| 角色 | 功能 |
|------|------|
| 学生 | 提交作品、上传附件/视频、**在线代码运行预览**、审核状态跟踪、查看评分与排行、消息通知 |
| 教师 | 匿名评分（创新性25 + 技术难度25 + 完成度30 + 实用性20）、排行榜、评分历史 |
| 管理员 | 审核发布、精选作品、评分批次、用户/角色/权限管理、奖品配置、数据字典、公告、评论管理、操作日志 |
| 游客 | 浏览作品、筛选、作品详情、评论互动、点赞、排行榜 |

---

## 快速开始

### Docker 部署（推荐）

```bash
# 1. 克隆仓库
git clone https://github.com/wswhhhc/jingxuan-.git && cd jingxuan-

# 2. 配置环境变量
cp .env.example .env
# 编辑 .env，填入 DB_PASSWORD、DEEPSEEK_API_KEY、邮件配置

# 3. 一键启动（MySQL + Redis + 后端 + Nginx）
docker compose up -d

# 4. 初始化数据库（首次部署）
docker compose exec mysql bash -c "mysql -uroot -p\$DB_PASSWORD jingxuan < /docker-entrypoint-initdb.d/01-init-schema.sql"

# 5. 访问
# 前端: http://localhost
# API 文档: http://localhost:8080/doc.html
```

### 手动部署（PM2）

```bash
# 环境要求：JDK 17+ / MySQL 8.0+ / Redis 7+ / Node.js 20+

# 1. 建库 & 导入 SQL
mysql -u root -p -e "CREATE DATABASE IF NOT EXISTS jingxuan CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;"
mysql -u root -p jingxuan < sql/base/init_schema.sql
mysql -u root -p jingxuan < sql/business/work_schema.sql
# 按时间顺序执行 sql/business/ 下的增量迁移

# 2. 配置环境变量
cp .env.example .env

# 3. 构建并启动后端
cd backend
mvn package -Dmaven.test.skip=true
cp target/jingxuan-backend-1.0.0.jar .
pm2 start ../ecosystem.config.cjs

# 4. 构建前端
cd ../frontend
npm install && npm run build
```

### 本地开发

```bash
# 环境要求：JDK 17+ / MySQL 8.0+ / Redis 7+ / Node.js 20+

# 1. 配置环境变量
cp .env.example .env
# 编辑 .env，填入 DB_PASSWORD

# 2. 后端开发（终端 1）
cd backend
mvn compile                          # 编译
# 在 IDE 中运行 Application.java，或：
mvn spring-boot:run                  # 启动后端（端口 8080）

# 3. 前端开发（终端 2）
cd frontend
npm install
npm run dev                          # Vite HMR（端口 5173，自动代理 /api 到 8080）
```

### 访问地址

| 服务 | 地址 |
|------|------|
| 前端页面 | `http://服务器IP/` |
| 后端 API | `http://127.0.0.1:8080/` |
| API 文档 | `http://127.0.0.1:8080/doc.html` |

### 测试账号

| 账号 | 密码 | 角色 | 姓名 |
|------|------|------|------|
| admin | admin123 | 管理员 | 系统管理员 |
| t001 | test123 | 教师 | 张教授 |
| 2022001 | test123 | 学生 | 张三 |
| teststu | test123 | 学生 | 测试学生 |

---

## 架构

### 请求流转

```
用户 → Nginx (80)
          ├── / → frontend/dist/（SPA 静态文件）
          ├── /api/* → 后端 localhost:8080/*（代理剥离 /api 前缀）
          ├── /api/file/* → 后端 /api/file/*（上传保留前缀）
          └── /uploads/* → 后端 /uploads/*（用户文件）
```

### Adapter 控制器模式

采用 Adapter 控制器桥接前端 URL 与后端业务模块，前端请求经 Nginx 去 `/api` 前缀后，由各角色 Adapter 分发：

| 控制器 | 路径 | 职责 |
|--------|------|------|
| `AdminApiController` | `/admin/*` | 审核、评论、公告、字典、日志、批次、奖品、标签、仪表盘、用户、角色、菜单、规则（48端点） |
| `TeacherApiController` | `/teacher/*` | 评分、排行榜、历史、仪表盘、批次、通知（14端点） |
| `StudentApiController` | `/student/*` | 作品 CRUD、提交、排名、批次、通知（14端点） |
| `PublicApiController` | `/public/*` | 作品浏览、详情、排行榜、评论、标签、班级（16端点） |
| `AuthController` | `/auth/*` | 登录、注册、验证码、密码修改、个人信息（9端点） |
| `NotificationController` | `/{role}/notify/*` | 三端通知统一查询/已读 |

### 业务模块

```
modules/
 ├── work/         作品 CRUD + 文件上传（魔数校验） + 成员管理 + 内容审核
 ├── audit/        审核（通过/驳回）+ 审核历史
 ├── score/        评分（Upsert 原子操作）+ 平均分计算
 ├── scorebatch/   批 처리 CRUD + 状态管理 + 通知
 ├── rank/         排行榜（SQL 聚合 + Redis 缓存）
 ├── publish/      发布/下线/精选
 ├── comment/      评论（登录用户 + 游客）+ 敏感词拦截
 ├── prize/        奖项配置 + 奖品发放 + 小组通知
 ├── notification/ 消息通知（通用发送 + 三端统一查询）
 ├── notice/       公告管理
 ├── sensitive/    DeepSeek AI 内容审核 + DFA 敏感词
 ├── userimport/   AI 批量导入用户
 ├── auth/         注册 + 邮箱验证码
 ├── dict/         数据字典
 └── log/          操作日志
```

---

## 项目结构（关键路径）

```
├── backend/                     # Spring Boot 后端
│   ├── src/main/java/com/jingxuan/
│   │   ├── common/              # Result/PageResult/BaseEntity
│   │   ├── config/              # 安全/Jackson/Knife4j/MyBatis-Plus
│   │   ├── modules/adapter/     # Adapter 控制器（4 角色 + 公共）
│   │   ├── modules/             # 14 个业务模块（work/audit/score/rank…）
│   │   ├── security/            # JWT 认证链 + 限流过滤器
│   │   └── exception/           # 全局异常处理 + 404
│   └── pom.xml
├── frontend/                    # Vue 3 + TypeScript SPA
│   ├── src/
│   │   ├── api/                 # API 调用层（按角色拆分）
│   │   ├── views/               # 页面（admin 12页/teacher 4页/student 5页/public 4页）
│   │   └── composables/         # useApiList / useCrudDialog / useNotificationPolling
│   └── vite.config.ts
├── sql/                         # 数据库迁移脚本
│   ├── base/init_schema.sql     # 基础表
│   └── business/                # 业务表 + 增量迁移
├── docs/                        # 16 份项目文档
├── docker-compose.yml           # Docker 编排
├── nginx-jingxuan.conf          # Nginx 反代配置
└── ecosystem.config.cjs         # PM2 进程配置
```

详细信息请参阅 [CLAUDE.md](CLAUDE.md)。

---

## 测试状态

| 测试类型 | 结果 | 覆盖 |
|---------|------|------|
| 前端单元测试 | 56/56 ✅ 100% | 组件逻辑、API 适配、路由 |
| 后端单元测试 | 97/97 ✅ 100% | 14 个业务模块 Service 层 |
| 集成测试 | 77/77 ✅ 100% | 10 个 Adapter 测试类 + 9 条 E2E |
| 接口测试 | 109/109 ✅ 100% | 全部 API 端点 |
| 手工测试 | 38/38 ✅ 100% | 四端功能全覆盖 |
| 性能测试 | ~220 req/s | 100 并发 0 失败 |
| 安全测试 | ✅ XSS / SQL 注入 / 越权 / 文件魔数 / DeepSeek 实测 |
| 验收测试 | ✅ 41 项全部通过（张永琪/徐嘉豪/王耀） |

---

## 开发命令

```bash
# 前端
cd frontend
npm run dev          # 开发服务器（Vite HMR）
npm run build        # 类型检查 + 生产构建
npm run test         # Vitest 单元测试

# 后端
cd backend
mvn compile          # 编译
mvn test             # 运行测试
mvn package -Dmaven.test.skip=true  # 打包 JAR

# 冒烟测试（部署后验证）
bash scripts/smoke-test.sh http://localhost:8080

# CI/CD（GitHub Actions）
git push  # 自动触发：类型检查 → 单元测试 → 集成测试 → 构建 → Docker
```

---

## 评分规则

四维度评分，满分 100 分：

| 维度 | 分值 |
|------|------|
| 创新性 | 25 |
| 技术难度 | 25 |
| 完成度 | 30 |
| 实用性 | 20 |

总分 = 创新性 + 技术难度 + 完成度 + 实用性。多位教师评分取平均分，按总分→完成度→创新性→提交时间排序。

---

## 安全特性

- 文件上传：扩展名白名单 + Hutool 文件魔数双重校验，拦截 HTML/JS/SVG/XSS 向量
- 权限控制：Spring Security 角色隔离（学生/教师/管理员）
- 内容审核：DeepSeek AI + DFA 敏感词双层过滤
- 公开 API：Caffeine 限流（20 次/秒/IP）
- JWT：Token 黑名单机制（登出即失效）
- 响应头：Nginx 配置 CSP/X-Frame-Options/X-Content-Type-Options 等安全头

---

## 环境变量

| 变量 | 说明 | 默认值 |
|------|------|--------|
| `DB_PASSWORD` | MySQL 数据库密码 | `jingxuan123` |
| `DEEPSEEK_API_KEY` | DeepSeek API 密钥 | — |
| `MAIL_HOST` | 邮件服务器地址 | `smtp.qq.com` |
| `MAIL_PORT` | 邮件服务器端口 | `587` |
| `MAIL_USERNAME` | 邮箱用户名 | — |
| `MAIL_PASSWORD` | 邮箱密码/授权码 | — |

---

## 数据库迁移规范

- 基础表：`sql/base/init_schema.sql`
- 业务表：`sql/business/work_schema.sql`
- 增量迁移：`sql/business/yyyy-MM-dd-描述.sql`
- 所有实体继承 `BaseEntity`（雪花算法 id、createTime、updateTime、逻辑删除 deleted）
- `init_schema.sql` 和 `work_schema.sql` 内含 `USE jingxuan;`，导入测试库时需用 `sed` 剔除

---

## 安全注意

- `.env` 含数据库密码、DeepSeek API Key、邮件配置，**严禁提交到 Git**
- JWT secret 在 `application.yml`，生产环境请替换
- Nginx 已配置安全头与 1600m 客户端上传大小限制
