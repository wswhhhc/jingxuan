# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## 项目定位

**菁选校园作品展示平台** — IT 学院学生作品展示、教师评分、管理员审核的全流程管理平台。

- **前端**：Vue 3 + TypeScript + Element Plus + Vite 8 SPA
- **后端**：Spring Boot 3.2.5 + Java 17 + MyBatis-Plus 3.5.7
- **数据库**：MySQL 8 + Redis 7
- **部署**：Docker / PM2 + Nginx
- **CI/CD**：GitHub Actions（push 自动跑测试 + 构建）

## 快速代码探索

本项目使用 Codegraph 建立代码索引。需要理解某段逻辑、查找符号或追踪调用链路时，优先使用：

- `codegraph_explore` — 回答"X 怎么工作的 / 在哪 / 架构是怎样的"，直接返回相关源码（Read 等价，一次性返回多个文件）
- `codegraph_search` — 按名称查找符号位置
- `codegraph_callers` / `codegraph_callees` / `codegraph_impact` — 调用链追踪
- `codegraph_files` — 按目录/语言查看文件树

**不要**用 grep + Read 绕路——codegraph 已经建好索引了，一次调用就能拿到完整源码。

## 开发命令

```bash
# 前端
cd frontend && npm run dev          # 开发服务器（Vite HMR，端口 5173）
npm run build                       # 类型检查 + 生产构建 → dist/
npm run test                        # Vitest 全部测试
npm run test -- --run src/views/admin/audit/__tests__/index.test.ts  # 单个测试文件
npm run lint                        # ESLint
npm run format                      # Prettier
npx vue-tsc --noEmit                # TypeScript 类型检查

# 后端（需在 backend/ 目录下）
mvn compile                           # 编译
mvn test                              # 全部测试（含集成测试，需 MySQL+Redis）
mvn test -Dtest="WorkServiceImplTest" # 单个测试类
mvn test -Dtest="AdminApiTest,FileUploadTest"  # 多个测试类
mvn test -Dtest="com.jingxuan.modules.work.**" # 按包运行
mvn test -DfailIfNoTests=false        # 无测试不报错
mvn compile -q                        # 编译检查
mvn package -Dmaven.test.skip=true    # 打包 JAR

# 部署
cd /opt/jingxuan/backend
mvn package -Dmaven.test.skip=true
cp target/jingxuan-backend-1.0.0.jar .
pm2 restart jingxuan-back

# PM2 管理
pm2 restart jingxuan-back             # 重启
pm2 logs jingxuan-back                # 查看日志
pm2 status                            # 查看进程状态

# 冒烟测试
bash scripts/smoke-test.sh http://localhost:8080

# 静态资源缓存刷新
nginx -s reload
```

### Docker

```bash
docker compose up -d              # 启动全部服务（MySQL + Redis + 后端 + Nginx）
docker compose logs -f backend    # 查看后端日志
docker compose down               # 停止
docker compose up -d --build backend  # 重新构建并启动后端
```

### CI/CD

```bash
git push  # 自动触发 GitHub Actions：编译 → 单元测试 → 打包（.github/workflows/ci.yml）
```

注意：CI 只跑后端单元测试（不含集成测试），需要 MySQL+Redis 的测试在本地运行。

## 环境变量

| 变量 | 说明 | 默认值 |
|------|------|--------|
| `DB_PASSWORD` | MySQL 数据库密码 | `jingxuan123` |
| `DEEPSEEK_API_KEY` | DeepSeek API 密钥（留空 dev 环境 bypass） | — |
| `MAIL_HOST` | 邮件服务器地址 | `smtp.qq.com` |
| `MAIL_USERNAME` | 邮箱用户名 | — |
| `MAIL_PASSWORD` | 邮箱密码/授权码 | — |
| `MAIL_FROM` | 发件人地址 | — |

定义在项目根目录 `.env` 文件中。

## 关键文件位置

| 文件 | 用途 |
|------|------|
| `backend/src/main/resources/application.yml` | 主配置（数据源、JWT、Redis、上传路径、DeepSeek） |
| `application-dev.yml` | 开发环境（DeepSeek bypass） |
| `application-test.yml` | 测试环境（独立数据库 jingxuan_test） |
| `application-prod.yml` | 生产环境（DeepSeek reject） |
| `frontend/vite.config.ts` | Vite 构建 + 开发代理配置 |
| `nginx-jingxuan.conf` | Nginx 反代配置（安全头、Gzip、缓存策略） |
| `ecosystem.config.cjs` | PM2 进程配置（JVM 参数） |
| `docker-compose.yml` | Docker 编排 |
| `.env` | 敏感配置（**不提交 Git**） |
| `sql/` | 数据库迁移脚本（按日期命名） |

## 架构概览

### 请求流转

```
用户 → Nginx (80)
 ├── /            → frontend/dist/（SPA 静态文件）
 ├── /api/*       → localhost:8080/*（Nginx 剥离 /api 前缀）
 ├── /api/file/*  → localhost:8080/api/file/*（文件上传保留前缀）
 └── /uploads/*   → localhost:8080/uploads/*（用户上传文件）
```

`/novel2/` 路由代理到另一个独立项目 `/opt/Novel2Script/`，与本项目无关。

### 开发代理（Vite）

前端开发服务器 (`localhost:5173`) 通过 vite.config.ts 中的 proxy 将 `/api/*` 转发到后端 `localhost:8080`，请求路径中 `/api` 前缀会被剥离（文件上传路径保留）。

### Adapter 控制器模式

所有前端请求 → Nginx（剥离 `/api`）→ Adapter 控制器 → Service：

| 控制器 | 路径 | 角色 |
|--------|------|------|
| `AdminApiController` | `/admin/*` | 管理员 |
| `TeacherApiController` | `/teacher/*` | 教师 |
| `StudentApiController` | `/student/*` | 学生 |
| `PublicApiController` | `/public/*` | 游客 |
| `AuthController` | `/auth/*` | 认证（登录/注册/验证码） |
| NotificationController | `/{role}/notify/*` | 三端通知 |

Adapter 控制器（`modules/adapter/`）负责接收前端请求，调用 Facade / Service 执行业务逻辑，返回 `Result<T>` 统一响应。

### 安全认证流程

1. 用户登录 → `AuthController` 验证凭据 → `JwtTokenProvider` 生成 JWT
2. 后续请求携带 `Authorization: Bearer <token>`
3. `JwtAuthenticationFilter` 解析 token → 注入 `SecurityContext`
4. `SecurityUtils.requireCurrentUserId()` 获取当前用户 ID
5. `SecurityConfig` 按角色（STUDENT/TEACHER/ADMIN）配置端点权限
6. 登入 token 加入 `InMemoryTokenBlacklistService`（Caffeine Cache）
7. 公开接口限流：`PublicRateLimitFilter` 用 Caffeine Cache 限流（20 次/秒）

### 业务模块（modules/）

```
work → audit → publish → score → scorebatch → rank → comment → prize
       → notification → notice → sensitive → userimport → auth → dict → log
       → task → deleterequest
```

模块间依赖方向：work → audit → publish → score/scorebatch → rank（只进不出）。

### 核心业务流程

```
学生创建作品 → 上传附件/视频 → 提交审核
                                         → 管理员审核通过 → 教师评分 → 排行榜 → 发布展示
                                         → 管理员驳回 → 学生修改后重新提交
学生待办：管理员创建批次 → 发布待办 → 学生待办列表 → 选择批次 → 提交作品
删除申请：学生申请删除已审核作品 → 管理员审批 → 同意（执行删除）或拒绝
```

## 新增模块说明

### 学生待办（task 模块）
- 表 `student_task`：userId, batchId, workId, title, content, status(0=待处理 1=已完成 2=已驳回 3=已截止)
- 管理员创建批次发布待办 → 批量创建 student_task
- 学生提交作品 → 自动标记完成；审核驳回 → 标记已驳回；管理员删除作品 → 重置为待处理
- 数据库唯一索引 `(email, role_id, deleted)` 支持软删除后重新注册（物理删除用户后用 `physicalDeleteById`）

### 删除申请（deleterequest 模块）
- 表 `delete_request`：workId, studentId, reason, status(0=待处理 1=已同意 2=已拒绝), adminReply
- 学生端：已通过作品可申请删除，填原因
- 管理端：审核页新增「删除申请」Tab，可同意（执行 adminDeleteWork 清关联数据+发通知）或拒绝（填原因+发通知）

### 在线体验（PreviewDialog 组件）
- `components/PreviewDialog.vue`：弹窗 + iframe 内嵌预览
- 支持小窗（70vw × 80vh）和全屏模式，白屏超时提示
- **重要**：`el-dialog` 使用 `append-to-body`，弹窗 DOM 移出组件树，scoped 样式无效。样式必须放在全局 `<style>`（非 scoped）中，加 `!important` 覆盖 Element Plus 默认值

## 前端架构

### 目录结构

```
frontend/src/
 ├── api/             # API 层（按角色分 admin/teacher/student/public）
 │   ├── request.ts   # Axios 实例 + 拦截器（token 注入、统一错误处理）
 │   ├── types.ts     # 共享类型（WorkListVO/WorkDetailVO/RankItem/UserInfo）
 │   └── student/task.ts / deleteRequest.ts  # 新增模块
 ├── components/      # 共享组件（NotificationList/PaginationBar/AppThemeToggle/PreviewDialog）
 ├── composables/     # 组合式函数（useApiList/useCrudDialog/useNotificationPolling）
 ├── layout/          # 四端布局（Admin/Teacher/Student/Public）
 ├── router/          # 路由配置（modules/ 下按角色拆分）
 ├── stores/          # Pinia 状态（auth, theme）
 └── views/           # 页面（admin 12页/teacher 4页/student 6页/public 4页）
```

### 关键约定

- **响应拦截**：`request.ts` 中 `res.code === 0 || res.code === 200` 为成功，非零或网络错误弹出 `ElMessage.error()`
- **分页**：`useApiList<T>(fetchFn)` + `<PaginationBar>`，page/size 由视图管理
- **弹窗 CRUD**：admin 管理页用 `useCrudDialog()` 统一管理 create/edit/delete 状态
- **深色模式**：`<html data-theme="dark">` 切换，CSS 变量在 `style.css`

## 后端关键约定

- **统一返回**：`Result<T>`（`ok(data)` / `fail(msg)`），异常由 `GlobalExceptionHandler` 捕获
- **BaseEntity**：所有实体继承（雪花 id + createTime + updateTime + 逻辑删除 `deleted`）
- **雪花 ID**：Jackson 全局 Long→String，防前端 JS 精度丢失
- **分页**：`PageUtil.query(pageNum, pageSize, mapper, wrapperConsumer)` → `PageResult<T>`
- **评分 Upsert**：`WorkScoreMapper.upsert()` 原子 INSERT ... ON DUPLICATE KEY UPDATE
- **DeepSeek fallback**：`bypass`(dev) / `reject`(prod) / `warning`(test)
- **物理删除用户**：`SysUserMapper.physicalDeleteById()` 直接 DELETE，不触发 `@TableLogic`，确保邮箱和用户名可重新注册
- **管理员删除作品**：`WorkServiceImpl.adminDeleteWork()` 清理 10 张关联表（附件/评分/评论/点赞/审核记录/成员/发布/标签/奖品发放/待办），发通知，重置待办

### 实体枚举

| 枚举 | 值 |
|---|---|
| `AuditStatusEnum` | 0=草稿, 1=已提交, 2=已驳回, 3=已通过 |
| `PublishStatusEnum` | 0=未发布, 1=已发布, 2=已下线 |
| `UserStatusEnum` | 0=禁用, 1=启用 |
| `RoleEnum` | 1=STUDENT, 2=TEACHER, 3=ADMIN |

## 测试指南

### 测试结构

**命名规范：**
- 后端单元测试：`*Test.java`（如 `WorkServiceImplTest`），扩展 `BaseServiceTest`，Mockito 模拟 Mapper 层
- 后端集成测试：`*ApiTest.java`（如 `AdminApiTest`），扩展 `BaseApiTest`（Spring Boot Test + 随机端口）
- 前端测试：`*.test.ts`（如 `index.test.ts`），Vitest + `@vue/test-utils`

**后端集成测试**：扩展 `BaseApiTest`，使用内置 `ApiClient` 发送 HTTP 请求并解析 JSON 响应：

- `adminApi`：管理员（admin / admin123）
- `teacherApi`：教师（t001 / test123）
- `studentApi`：学生（2022001 / test123）
- `testStuApi`：测试学生账号（teststu / test123）
- `publicApi`：无 token 的公开端

**后端单元测试**：扩展 `BaseServiceTest`，使用 Mockito 模拟 Mapper 层。注意：`@RequiredArgsConstructor` 新增依赖时需要同步更新测试类的构造函数调用和 `@Mock` 字段。

**集成测试依赖**：需运行中的 MySQL（数据库 `jingxuan_test`）+ Redis。CI 环境中不跑集成测试。

```bash
# 运行集成测试时需设置数据库密码
export DB_PASSWORD=252629
mvn test
```

### 测试状态（当前）

| 类型 | 通过率 | 运行方式 |
|------|--------|---------|
| 前端单元测试 | 56/56 100% | `npm run test` |
| 后端单元测试 | 299/299 100% | `mvn test -Dtest="com.jingxuan.modules.**"` |
| 集成测试 | 全部通过 | `mvn test`（需 MySQL+Redis） |
| 全部后端测试 | 317/317 100% | `mvn test`（需 MySQL+Redis） |
| 冒烟测试 | 14/14 100% | `bash scripts/smoke-test.sh` |

### 关键测试文件

| 测试文件 | 用例数 | 覆盖 |
|---------|--------|------|
| `WorkServiceImplTest` | 13 | CRUD、提交审核、管理员删除 |
| `ScoreServiceImplTest` | 12 | 评分 upsert、维度校验、DeepSeek fallback |
| `ScoreBatchServiceImplTest` | 8 | 批次 CRUD、排行榜公示、发布待办 |
| `AuditServiceImplTest` | 6 | 审核通过/驳回、审核历史 |
| `DeleteRequestServiceImplTest` | 8 | 学生提交申请、管理员同意/拒绝 |
| `ScoreBatchServiceImplTest` | 8 | 批次 CRUD、发布待办 |
| `AiUserImportServiceImplTest` | 9 | AI 导入解析 |

## 数据库迁移

- 基础表：`sql/base/init_schema.sql`
- 业务表：`sql/business/work_schema.sql`
- 增量迁移：`sql/business/yyyy-MM-dd-描述.sql`
- 所有实体继承 `BaseEntity`（雪花算法 id、createTime、updateTime、逻辑删除 deleted）
- 新增唯一索引注意兼容软删除：`UNIQUE KEY uk_email_role_deleted (email, role_id, deleted)`

## 评分规则

四维度评分，满分 100 分：创新性(25) + 技术难度(25) + 完成度(30) + 实用性(20)。多位教师评分取平均分，按总分→完成度→创新性→提交时间排序。

## PPT 制作背景设计参考

本项目 PPT 推荐浅色但有层次的设计，避免纯白单调背景：

- **渐变背景**：浅蓝→浅紫渐变（#E8F4FD → #F0E6FF），或浅蓝→暖灰渐变，每页微调角度
- **几何装饰**：页面边缘加半透明圆形/波浪装饰元素，颜色用主色（#409EFF）的 5%-10% 透明度
- **色块分区**：标题区域用浅蓝渐变条底色，正文区域用极浅灰卡片式底纹
- **底部装饰**：页面底部加细渐变横条或小三角装饰，打破空白
- **勿过度**：装饰控制在"有设计感但不抢内容"的程度

## 安全注意事项

- `.env` 含 `DB_PASSWORD`、`DEEPSEEK_API_KEY`、邮件配置，**严禁提交到 Git**
- JWT 密钥在 `application.yml` 的 `jwt.secret`，生产环境请替换
- 文件上传：扩展名白名单 + 魔数（Hutool FileTypeUtil）双重检测
- Token 黑名单：`InMemoryTokenBlacklistService` 用 Caffeine Cache 存储登出 token
- Nginx 已配置 CSP/X-Frame-Options/X-Content-Type-Options 等安全头
- 物理删除用户时注意清理关联数据（delete_request, student_task, notification）
- `frame-src '*'` 允许 iframe 嵌入外部项目地址（在线体验功能需要）
