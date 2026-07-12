# 菁选 v2 新版全量执行方案

制定日期：2026-07-12  
当前基线：`refactor/v2` / `9e33882`  
当前完成度：约 31%  
执行方式：自动门禁驱动；门禁通过后直接进入下一任务，不等待人工批准。

## 1. 执行目标

在保留全部有效业务和历史数据的前提下，把当前“新旧实现交错且不可构建”的仓库收敛为可验证的 v2 模块化单体，并完成真实生产迁移、PM2 + Nginx 自动发布、Docker Compose 支持、旧数据清理和上线后验证。

完成定义不是“文件存在”，而是：

1. 对应行为已接入真实入口。
2. 单元、集成、契约、E2E 和构建证据通过。
3. 旧实现和兼容层在该切片结束时被删除。
4. 自动门禁能够阻止回退。
5. 生产步骤有可验证前置条件、回滚条件和审计报告。

## 2. 保留与调整的决策

### 保留

- 模块化单体，不引入微服务、消息队列、对象存储、Elasticsearch、SSR 或移动 App。
- Spring Boot 4.1、MyBatis-Plus Boot4 3.5.17、Spring Modulith 2.1、Vue 3.5、Vite 8、TypeScript 6、Element Plus 2.14、Pinia 3。
- JDK 25、Node 24 LTS、MySQL 8、Redis 7、Flyway、Testcontainers、OpenAPI/Orval、Vue Query、MSW、Playwright、Lighthouse CI。
- `/api/v1`、RFC Problem Details、字符串 ID、字符串状态、ISO-8601 `+08:00` 时间、统一分页、幂等命令和乐观锁。
- Refresh Cookie 在正式 HTTP 环境使用 `Secure=false` 的已接受风险；保持同源和可信校园网限制。
- 有效数据、原 ID、密码哈希、时间和有效文件全部迁移；软删、测试、废弃数据不迁移且不备份。
- 物理删除、实时排行、公示只控制可见性、奖品发放快照。
- Docker Compose 与 PM2 + Nginx 双路径都必须通过同一冒烟。

### 调整

- Node 24 固定到当前 LTS 补丁线，初始目标为 24.18.x；不使用非 LTS Node 25。
- TypeScript 6 已获 typescript-eslint 正式支持，无需继续保留“等待支持”的旧前提。
- `d7ff9ed` 只作为恢复素材，禁止整体 cherry-pick、reset 或覆盖当前 HEAD。
- 当前孤立 Git 历史不能直接作为正式发布源；必须先重建在远端真实 `master` 血缘上的实施分支。
- GitHub `production` Environment 保留，但不配置 required reviewer；自动质量门禁通过后无人值守发布。
- 所有破坏性生产步骤采用 fail-closed 自动门禁：目标身份、计数、哈希、回滚产物或冒烟任一不满足即停止，不依赖人工点击。

## 3. 全局实施规则

- 每个任务采用 `RED → GREEN → REFACTOR → 五轴审查 → 中文原子提交`。
- 后端 Maven 串行运行，避免共享 `target` 和 JaCoCo 数据竞争；前后端独立任务可并行。
- 每 2–3 个任务设置检查点，检查点失败不得进入下一阶段。
- OpenAPI YAML 是契约源；生成客户端和运行时文档必须双向一致。
- 生产密钥只进入 GitHub Environment Secrets 或服务器安全环境文件，禁止进入命令参数、日志、报告或 Git。
- 当前工作分支和恢复分支均保留；禁止 `reset --hard`、强推和无审查的批量删除。
- 远端、生产或凭据输入缺失时进入可恢复的 `BLOCKED` 门禁；不得把候选主机、历史命令或静态配置冒充运行证据。
- 每次发布生成唯一不可变 release manifest，绑定 commit、OpenAPI、Flyway 版本、迁移器、JAR、前端包、镜像、SBOM 与全部 SHA-256。
- 生产部署和 purge 共用不可取消的并发锁与生产版本 CAS；任何并发工作流不得交错执行迁移、切换或清理。

## 4. 阶段 A：恢复唯一可信绿色基线

### A1. 保护恢复资产与建立真实 Git 血缘

- [x] 创建 `codex/recovery-wip-d7ff9ed` 保护不可达 WIP。
- [ ] 网络恢复后获取远端 `master`，核验仓库身份、根提交和默认分支。
- [ ] 从 `origin/master` 创建 `codex/refactor-v2-recovery`，逐提交迁入当前有效差异；当前孤立分支保留为归档证据。
- [ ] 推送新分支并启用主分支保护、必需状态检查和禁止强推。
- [ ] 远端不可达期间允许 A2–A7 在本地恢复分支继续，但在 A1 完成前禁止首次推送、GitHub CI 结论和任何部署。

验收：新实施分支与 `origin/master` 有 merge-base；当前树和恢复树均有可追溯引用；所有 Git/GitHub 描述为中文。  
验证：`git merge-base`、树差异清单、远端分支/保护规则 API。

### A2. 统一确定性工具链

- [ ] POM 使用 JDK 25、`release 25`、兼容 JDK 25 的 compiler plugin 和显式 Lombok processor。
- [ ] 本机脚本、CI、根/后端/前端 Dockerfile、PM2 文档统一 JDK 25 与 Node 24 LTS。
- [ ] 修复 `verify-toolchain` 的模块导出、Windows 执行和 job 级版本检查。

验收：本地、CI、Docker 构建声明无 21/17/20 漂移。  
验证：toolchain 元测试、JDK 25 clean compile、镜像构建。

### A3. 恢复可复现依赖与文本基线

- [ ] 在 lockfile 中正式加入 MSW、Playwright、LHCI 等已提交代码所需依赖，消除 extraneous 依赖。
- [ ] 增加 `.gitattributes`，Shell/YAML/JS 固定 LF；修复 CRLF 冒烟脚本。
- [ ] 将机械格式化作为独立提交，修复当前 9 个 Prettier 漂移和前端 lint/typecheck 基线。
- [ ] 锁定 ESLint Vue parser、Axios 生产依赖审计和 Maven 单一 mail starter，防止阶段 0 已知回退。

验收：全新目录 `npm ci` 可复现；格式、lint、typecheck 全绿。  
验证：clean install、`format:check`、ESLint、Vue TypeScript。

### A4. 恢复后端可编译状态

- [ ] 引入 `spring-modulith-starter-jdbc` 和正确的事件 publication 配置。
- [ ] 修复 evaluation 的 `RankService`/`RankVO` 迁移缺口。
- [ ] 恢复 `PENDING_APPROVAL`、物理删除 Mapper、密码升级 CAS、文件存储构造器等测试要求的生产契约。
- [ ] 消除两个 RefreshToken Service 的 Bean 名称/职责冲突。
- [ ] 纠正 Flyway V5、事件恢复和 Testcontainers 相关测试漂移。

验收：JDK 25 `mvn clean test` 编译通过且无未解析历史符号。  
验证：定向测试后运行完整 unit suite。

### A5. 恢复前端 v2 HTTP 与 Query 基线

- [ ] 从恢复源选择性重建独立 v2 Axios mutator：仅允许 `/api/v1`，禁止覆盖 URL/method/baseURL，支持直接 DTO、201/204 和 `ApiProblemError`。
- [ ] 安装 VueQueryPlugin，接通统一 QueryClient、取消信号和错误边界。
- [ ] 修复当前 21 个失败 Vitest 与 3 个未处理网络错误，不删除目标安全测试。

验收：HTTP 安全测试、全部 Vitest、typecheck、lint、build 全绿。  
验证：前端 clean test/coverage/build。

### A6. 让 OpenAPI 重新代表真实实现

- [ ] 先修 Controller 与目标契约，再从运行时导出 OpenAPI。
- [ ] 补齐注册、验证码、Cookie、401/403/409/422、201/204、字符串 ID、分页和公开 security 语义。
- [ ] 重新生成 Orval DTO、Axios 客户端和 Vue Query hooks，删除 phantom/旧生成产物。

验收：运行时、YAML、生成客户端无漂移；22 项契约语义全部通过。  
验证：`api:check`、生成客户端元测试、受保护/公开 API 契约测试。

### A7. 恢复全部基础门禁

- [ ] 修复 toolchain、coverage、smoke、legacy、bundle 等元测试自身。
- [ ] 根 `verify` 串联格式、lint、类型、覆盖率、OpenAPI、单元、Testcontainers、构建和静态安全检查。
- [ ] CI 并行执行相同门禁，不允许本地绿而 CI 漏跑。

验收：当前 0/1、0/3、1/23 的元门禁全部转绿。  
验证：本地根验证 + GitHub Actions 运行记录。

### 检查点 A

- [ ] 当前实施分支来自真实远端历史。
- [ ] 后端 JDK 25 clean build/test 通过。
- [ ] 前端 clean install/lint/type/test/build 通过。
- [ ] OpenAPI/Orval、配置、安全、覆盖率和冒烟元门禁全部通过。
- [ ] 生成新的基线截图、功能矩阵和测试报告。

## 5. 阶段 B：最终数据库、模块边界与公共基础

### B1. 固化最终数据模型与迁移映射

- [ ] 为所有 v1 表到 v2 表建立逐列映射、排除规则、外键策略、删除影响和文件归属清单。
- [ ] 更新数据库删除、排行、文件和迁移 ADR，消除 runbook 与代码冲突。

验收：每张有效源表都有唯一目标或明确排除理由。  
验证：Schema/mapping 静态测试和 ADR 审查。

### B2. 建立唯一且可渐进演化的 Flyway 历史

- [ ] 把现有 14 个 legacy SQL 收敛为一个自包含、可校验的 Flyway V1 兼容基线；根目录和 `backend/sql` 不再参与运行时建表。
- [ ] 后续按业务模块采用 additive expand migration：先新增 v2 表/列/约束，再迁移消费者和数据，最后在 F 阶段 contract 删除旧结构。
- [ ] v1 源库快照只作为测试/迁移 fixture，不作为第二生产 Schema 来源。
- [ ] 每次迁移后仍保持当前已迁业务可构建、未迁业务可运行，禁止一次性切断所有旧消费者。

验收：空库可由 Flyway 安装；v1 snapshot 可升级；任一中间版本都有明确兼容消费者。  
验证：Testcontainers empty-install、逐版本 upgrade 和 expand/contract 测试。

### B3. 实现身份与参考数据 Schema

- [ ] 用户、角色、权限、菜单、角色权限、角色菜单、班级、字典、标签使用字符串状态和明确约束。
- [ ] 三个内置角色带 `portal_type/is_builtin`，单账号单角色，受引用根对象使用 RESTRICT。
- [ ] 本任务只做 expand Schema/Repository；旧身份消费者在 C 阶段迁走前保留受测兼容读取。

验收：内置角色不可改代码/删除；自定义角色可组合权限。  
验证：Repository/约束/影响清单测试。

### B4. 实现 campaign 与 portfolio Schema

- [ ] `score_batch_class`、唯一 student task、作品/成员/有序标签、`file_asset/work_attachment`、审核、发布、互动和删除申请全部关系化。
- [ ] 聚合内部 CASCADE，共享根 RESTRICT；作品和用户删除走影响清单。

验收：无 JSON class scopes、无 tech_stack 字符串、无聚合孤儿。  
验证：MySQL 外键、唯一约束、并发和级联测试。

### B5. 实现 evaluation、communication 与 operations Schema

- [ ] 评分唯一约束和四维 CHECK、`award_tier`、奖励发放快照、公告通知、审计日志和事件 publication 表完成。
- [ ] 雪花主键统一 BIGINT，时间统一 `DATETIME(3)`，字符集/排序规则统一 `utf8mb4_0900_ai_ci`，组合索引明确。

验收：评分、排行、发奖和审计所需字段均由数据库约束保护。  
验证：Repository Testcontainers 测试。

### B6. 按模块退出逻辑删除与枚举序号

- [ ] 每个业务纵切先迁移对应实体/Repository 到物理删除和字符串状态，再删除该模块的旧逻辑删除路径。
- [ ] 最后一个旧消费者迁走后，才删除全局 `EnumOrdinalTypeHandler`、`@TableLogic` 配置和兼容列。
- [ ] F 阶段执行全仓 contract migration 和零遗留扫描。

验收：任何中间提交均可构建；已迁模块无数字状态/逻辑删除，未迁模块有显式临时兼容测试。  
验证：模块级 Schema/ArchUnit/序列化扫描。

### B7. 建立可靠事务事件基础

- [ ] 通知、日志、缓存失效、文件清理使用持久化事务事件，提交后执行并可重试。
- [ ] DeepSeek、邮件、Redis 和磁盘 I/O 不进入数据库事务。
- [ ] 多实例恢复有界、幂等且可观测。

验收：提交成功才产生副作用；监听失败可重放且不丢失。  
验证：事务提交/回滚/重试 Testcontainers 测试。

### B8. 公共 REST 命令基础

- [ ] 统一 Idempotency-Key 存储、命令结果重放、乐观锁 version 和 409 Problem Details。
- [ ] 统一分页、字段错误、类型错误、404/405、限流和 requestId/MDC。
- [ ] `pageSize` 全局最大 100；雪花 ID 统一 BIGINT 存储、字符串 API；时间统一 `DATETIME(3)` 和 `+08:00` 输出。

验收：创建、批量发布、奖励发放等重复请求不会产生重复副作用。  
验证：并发 API 契约测试。

### 检查点 B

- [ ] 空库与升级测试通过。
- [ ] Flyway 是唯一 Schema 来源。
- [ ] Modulith `verify()` 和 ArchUnit 无临时新增白名单。
- [ ] 公共事件、幂等、并发和 Problem Details 测试通过。

## 6. 阶段 C：身份权限与基础数据闭环

### C1. 登录、刷新和注销

- [ ] Access JWT 15 分钟且只返回前端内存。
- [ ] Refresh family 使用哈希、绝对 8h/30d、原子轮换、USED 重放撤销、单端/全端注销。
- [ ] Cookie 固定 HttpOnly/SameSite=Strict/Path，三条 POST 严格校验 Origin。
- [ ] 注销把当前 Access JWT 按剩余 TTL 加入黑名单；全端注销撤销用户所有 family。

验收：JavaScript 无法读取 refresh；旧 token 重放使后继 token 失效。  
验证：领域、Redis 并发、HTTP 集成和安全测试。

### C2. 限流、验证码与密码

- [ ] 登录账号/IP 5/20 阈值、邮件地址/IP 1/min 与 5/hour、AI 10/hour、游客评论 5/10min 与 20/hour。
- [ ] Redis 一次性算术 challenge 5 分钟过期。
- [ ] 新密码 BCrypt cost 12，旧低成本哈希登录后 CAS 透明升级。

验收：多实例一致、Retry-After 正确、存储故障 fail-closed。  
验证：100 并发 Redis 测试、认证 API 测试。

### C3. 注册、资料与教师审批

- [ ] 学生注册后启用，教师进入 PENDING_APPROVAL，管理员 `user:approve` 批准后才能登录/评分。
- [ ] 完成资料、改密、首次登录和审计事件。

验收：未批准教师在所有入口均被拒绝。  
验证：注册/审批/越权/改密集成测试。

### C4. 用户、角色、权限与菜单

- [ ] 后端按权限码和数据范围鉴权，不以菜单或固定角色作为安全边界。
- [ ] 三个内置角色受保护，自定义角色可分配权限和门户类型。
- [ ] 用户 CRUD、状态、批量导入、AI 导入和物理删除影响清单完成。

验收：同权限不同角色得到一致结果；根管理员受保护。  
验证：权限矩阵、所有权和删除影响测试。

### C5. 班级、字典与标签

- [ ] 独立 academic class、字典和有序标签 CRUD 完成。
- [ ] 删除共享根前返回引用清单，确认后按策略执行。

验收：受引用数据不会静默破坏作品/批次。  
验证：Repository 与 API 冲突测试。

### C6. 前端 Session、WorkspaceShell 和动态导航

- [ ] Session 只在内存，启动通过 refresh Cookie 恢复；401 使用单飞刷新并重放原请求。
- [ ] 三端统一 WorkspaceShell，菜单来自权限数据。
- [ ] 增加 403、catch-all 404 和统一加载/空/错误状态。
- [ ] 保留 `/jingxuan/` 和全部现有用户可见路由，不以迁移为由改变用户入口。

验收：Web Storage 无 token；自定义角色只看到且只能访问授权能力。  
验证：Vitest + MSW + Playwright。

### C7. 身份与基础数据页面迁移

- [ ] 登录、注册、资料、用户、角色、菜单、班级、字典和标签页面使用 Orval hooks + Vue Query。
- [ ] 删除对应旧角色 API、重复 DTO 和直接 Axios。

验收：页面不再消费 legacy Result。  
验证：DOM 用户交互测试、生成客户端扫描。

### 检查点 C

- [ ] 三种内置角色和至少一个自定义角色端到端通过。
- [ ] 教师待审核→批准→登录闭环通过。
- [ ] 身份核心覆盖率 ≥90%。

## 7. 阶段 D：批次、待办、作品、审核与互动闭环

### D1. 批次、范围与学生待办

- [ ] 批次 CRUD、班级范围、任务模板、幂等发布和截止状态完成。
- [ ] `(user_id,batch_id)` 唯一，提交/驳回/删除正确迁移待办状态。

验收：重复发布不重复建任务。  
验证：事务/并发测试与学生待办 E2E。

### D2. 作品聚合与状态机

- [ ] 草稿、成员、有序标签、附件、编辑、提交和版本冲突完成。
- [ ] 非法状态迁移由领域层拒绝，重复提交幂等。

验收：作品状态不依赖 Controller 分支拼装。  
验证：领域状态机和 API 集成测试。

### D3. 文件资产生命周期

- [ ] FileStorage 流式写入、魔数、大小、SHA-256、临时文件、原子绑定和过期清理完成。
- [ ] `file_asset` 明确 `TEMP/BOUND/EXPIRED` 生命周期，7 天未绑定资产可由受控任务转为 EXPIRED 并清理。
- [ ] 图片 10MB、压缩包/PDF 200MB、MP4 1.5GB 限制由配置和测试锁定。

验收：DB 失败无孤儿文件，事务回滚不提前删文件。  
验证：文件系统集成与上传/下载/预览 Playwright。

### D4. 跨模块提交流程

- [ ] 先实现 moderation port 和 DeepSeek/本地规则 provider；外部审核在数据库事务外执行。
- [ ] 外部审核结果携带作品 version；进入事务后重新校验 version，再原子完成作品提交、待办完成和通知事件登记。
- [ ] 审核超时按环境 fallback；若采用异步模式则显式进入 `PENDING_MODERATION` 并由可靠事件推进，禁止在事务内等待网络。
- [ ] workflow 只依赖模块 api，不注入 Mapper。

验收：外部调用不占数据库事务；版本变化会拒绝陈旧审核结果；数据库步骤任一失败全部回滚。  
验证：事务集成与 Modulith 边界测试。

### D5. 审核、发布和精选

- [ ] 审核队列、不可变历史、驳回重提、发布/下线/精选完整实现。
- [ ] 精选和发布规则有并发保护。

验收：提交→审核→发布→下线状态一致。  
验证：管理员 E2E 和并发测试。

### D6. 公开展廊与详情 read model

- [ ] 列表/详情使用 JOIN、批量查询或专用 read model，无随记录数增长的查询数。
- [ ] 筛选、分页、排序进 URL；浏览量聚合且不阻塞主请求。
- [ ] iframe 增加 title、referrerpolicy、noopener 和最小 sandbox 权限。

验收：公开首屏和详情无 N+1。  
验证：查询数测试、Playwright、Lighthouse。

### D7. 评论、回复与点赞

- [ ] 顶级评论和回复分别分页，游客风控复用统一 challenge/限流。
- [ ] 点赞仅保留幂等 PUT/DELETE，删除 toggle。

验收：大评论树不会全量载入；重复点赞无副作用。  
验证：滥用/权限/分页/E2E 测试。

### D8. 删除申请、用户和作品物理删除

- [ ] 删除申请同意/拒绝、管理员影响预览、二次确认和根管理员保护完成。
- [ ] 作品从属数据由外键 CASCADE，文件提交后事件可靠删除；共享引用先阻断。

验收：删除后无孤儿 DB/文件记录，失败事件可重试。  
验证：级联、事件重放和 E2E。

### D9. 前端业务迁移与拆分

- [ ] 批次、待办、作品编辑器、审核、公开详情、评论等全部使用 Orval hooks。
- [ ] 拆分 WorkDetail、WorkSubmit、审核和批次页面；超过 300 行必须继续拆分或记录理由。
- [ ] 搜索 300ms 防抖并取消旧请求。
- [ ] 页面目标不超过 250 行；300 行是强制拆分上限而不是常态目标。

验收：对应旧 API 文件被删除，页面状态由 URL/Vue Query 驱动。  
验证：DOM 测试、页面行数和 legacy 扫描。

### 检查点 D

- [ ] 学生“注册→待办→上传→提交→驳回重提→删除申请”全流程通过。
- [ ] 管理员“审核→发布→公开展示→删除处理”全流程通过。
- [ ] 文件上传、下载、预览和物理删除 E2E 通过。

## 8. 阶段 E：评分、实时排行、奖品与剩余模块

### E1. 教师评分与导出

- [ ] 四维度数据库/领域约束、教师唯一 Upsert、乐观冲突、汇总和历史完成。
- [ ] CSV 由服务端流式导出，不在内存构造全量文件。

验收：并发改分无重复记录，非法分值被数据库拒绝。  
验证：Repository 并发测试与教师评分 E2E。

### E2. 实时排行榜

- [ ] MySQL 窗口函数按总分→完成度→创新性→提交时间排名。
- [ ] Redis generation key 缓存；评分和有效删除提交后立即失效，禁止 `KEYS`。
- [ ] 公示只控制可见性，不保存冻结快照。

验收：已公示排行改分后实时变化。  
验证：规则、缓存失效和性能测试。

### E3. 奖项和发放快照

- [ ] `award_tier` CRUD、幂等发放、取消和影响清单完成。
- [ ] 发放记录固化名次、分数、奖项和时间快照。

验收：实时排行变化不改写历史发放。  
验证：事务/幂等/E2E 测试。

### E4. 公告与个人通知

- [ ] 公告发布范围、个人通知、未读、已读和删除完成。
- [ ] 业务通知通过提交后持久化事件产生，失败可恢复。

验收：三端通知一致且不重复。  
验证：事件重试、轮询和 E2E。

### E5. 内容审核

- [ ] 在 D4 已接入的 moderation port 上完成敏感规则管理、严格输出校验、bypass/reject/warning 策略和管理员连通测试。
- [ ] 补齐 provider 指标、超时、熔断式降级和故障注入，不改变 D4 的事务外调用边界。

验收：Provider 故障不会悬挂业务事务。  
验证：契约测试与故障注入。

### E6. 操作日志和报表

- [ ] 管理/教师仪表盘改 SQL 聚合，审计日志结构化保存 actor/action/target/requestId。
- [ ] 报表模块仅允许只读跨表查询。

验收：无 Java 全表聚合、无跨模块写 Mapper。  
验证：SQL 查询数、权限和仪表盘 E2E。

### E7. 教师、管理员和公开端剩余页面

- [ ] 评分、历史、排行、奖品、公告、通知、审核、日志和仪表盘全部迁入 features。
- [ ] ECharts 只在仪表盘路由加载。

验收：功能矩阵所有业务行为达到 v1 等价或更优。  
验证：Vitest + MSW + Playwright。

### 检查点 E

- [ ] 18 行功能矩阵全部有自动化和 E2E 证据。
- [ ] 实时改分、公示、发奖快照和删除影响全部通过。

## 9. 阶段 F：彻底移除遗留实现

### F1. 删除后端旧入口和响应

- [ ] 删除角色型 Controller/Adapter、旧 API 别名、根级 Controller 和 `Result`。
- [ ] 所有前端请求只存在 `/api/v1`。

验收：旧路由和 `Result<` 扫描为 0。  
验证：legacy scanner、OpenAPI 路径扫描。

### F2. 删除根级业务层与模块桥接

- [ ] 删除根 Entity/Mapper/Service 和旧 `modules/*`，目标模块自有 domain/infrastructure。
- [ ] 删除 ArchUnit 临时白名单，执行完整 `ApplicationModules.verify()`。

验收：目标模块只能引用其他模块 api；报表只读例外被显式限制。  
验证：Modulith + ArchUnit。

### F3. 删除前端三套网络层和重复布局

- [ ] 删除旧角色 API、手写重复 v1 DTO/API、页面直接 Axios和三套重复 Shell。
- [ ] 目录收敛为 `app → features → shared`，Pinia 只保留 session/theme。

验收：页面全部通过 Orval/Vue Query；WorkspaceShell 唯一。  
验证：import 扫描、TypeScript、构建。

### F4. 删除重复 SQL、配置和漂移资源

- [ ] 删除根/后端/测试重复 Schema、无效 Docker 初始化和散落 Java 文件。
- [ ] 文档、示例、Compose、PM2 和 Nginx 只描述 v2 事实。

验收：Flyway 唯一来源，runbook 与代码一致。  
验证：资源契约与文档测试。

### F5. 页面与测试遗留清零

- [ ] `wrapper.vm` 迁移为 DOM 用户行为测试。
- [ ] 超 300 行页面清零或有已审查理由。
- [ ] 数字 ID、数字状态、toggle 点赞和硬编码角色菜单清零。

验收：所有遗留扫描为 0。  
验证：CI legacy job。

### 检查点 F

- [ ] 仓库只剩 v2 实现。
- [ ] 全量功能、架构、契约和构建门禁仍绿。

## 10. 阶段 G：性能、安全、可访问性与发布候选

### G1. 覆盖率提升

- [ ] 后端按模块、前端按 feature 增量补测。
- [ ] 整体行/函数 ≥80%、分支 ≥70%；认证、作品状态、审核、评分核心 ≥90%。

验收：JaCoCo/Vitest 在 CI 中硬失败，不允许下调阈值绕过。  
验证：clean coverage 报告。

### G2. 完整 Playwright E2E

- [ ] 覆盖学生、教师、管理员、游客、文件和物理删除全部原计划流程。
- [ ] 禁止 localStorage 假 token 替代真实登录。

验收：核心 E2E 在干净 Testcontainers/Compose 环境通过。  
验证：视频、trace、截图和 JUnit 报告。

### G3. 查询性能和 k6

- [ ] 消除列表 N+1、全量回复、Java 聚合和 Redis KEYS。
- [ ] 生成 1 万用户、10 万作品、30 万评分、50 万评论的 v2 数据集。
- [ ] 0→300 并发持续 10 分钟。

验收：核心 P95 <500ms、复杂列表/排行 <1s、错误率 <1%。  
验证：保存 k6 报告和查询计数基线。

### G4. 前端性能与无障碍

- [ ] 公共入口 ≤180KB、作品列表 ≤220KB、初始 CSS ≤50KB gzip。
- [ ] Lighthouse performance ≥90、accessibility ≥95；LCP≤2.5s、INP≤200ms、CLS≤0.1。
- [ ] 键盘、焦点、label、aria、图片 alt 和 iframe 安全属性完成。
- [ ] 删除 Element Plus 全量手工 chunk；ECharts 仅由仪表盘路由动态加载。

验收：LHCI 使用 `error` 门禁，不允许 warn 假通过。  
验证：manifest budget、LHCI 报告和浏览器测试。

### G5. 可观测性

- [ ] JSON 日志、MDC requestId、Micrometer RED、慢 SQL、Redis、事件和外部服务指标完成。
- [ ] Actuator 只暴露受控端点；Docker 提供可选 Prometheus/Grafana profile、仪表盘和告警。

验收：一次完整 E2E 可由 requestId 串起日志、指标和事件。  
验证：指标抓取、仪表盘和故障注入。

### G6. 供应链与安全门禁

- [ ] Gitleaks、依赖审查、npm/Maven/Trivy、镜像扫描、SBOM 和校验和完成。
- [ ] 所有受保护接口有 401/403/权限/所有权测试。

验收：无 Critical/High 生产依赖漏洞，无密钥入库。  
验证：CI 安全报告。

### G7. 双部署发布候选

- [ ] Docker Compose 无默认密钥、仅 Nginx 对外、显式服务主机和健康检查。
- [ ] PM2 使用版本化 JAR/前端包、原子 symlink、健康检查和自动回滚。
- [ ] Nginx 保留 `/api/v1`，正确托管 `/jingxuan/` 和 uploads。

验收：同一版本在 Docker 和 PM2 两种模式均通过相同冒烟。  
验证：两套部署演练记录。

### G8. 完整 CI 与无人值守 Release

- [ ] PR 并行执行格式、类型、Modulith、契约、覆盖率、Testcontainers、构建、E2E、bundle、扫描和 SBOM。
- [ ] main/nightly 执行完整 E2E、Lighthouse 和性能回归。
- [ ] Release 构建 JAR、前端包、GHCR 镜像、checksums、SBOM；门禁成功后自动 SSH 部署并失败回滚。
- [ ] Release 使用 `cancel-in-progress: false` 的生产并发锁、生产版本 CAS 和统一 release manifest；迁移器与所有制品必须来自同一 commit/摘要集合。
- [ ] 工作流支持 `docker` 与 `pm2` 部署模式，正式默认 PM2 + Nginx，两种模式复用同一制品和冒烟。

验收：GitHub production Environment 无 reviewer，但所有 needs 门禁强制；Secrets/known_hosts 配置完整。  
验证：失败注入发布演练。

### 检查点 G

- [ ] 功能、覆盖率、性能、安全、可访问性、观测和双部署全部满足发布标准。

## 11. 阶段 H：真实迁移、切换与清理

### H1. 重写真实迁移 CLI

- [ ] 实现 `preflight / migrate / verify / build-sanitized-rollback / purge`。
- [ ] 默认 dry-run；URI、密码和 token 全程脱敏。
- [ ] 支持批次、事务、断点、审计报告和确定的退出码。

验收：CLI 实际连接独立测试数据库，不再只打印模拟 SQL。  
验证：命令级集成测试。

### H2. 停写屏障与迁移前安全回滚产物

- [ ] 维护模式之外增加数据库级写屏障：撤销应用写权限或切只读、排空 HTTP/后台任务/连接池，并记录 GTID/binlog 位点。
- [ ] 在任何 v2 数据写入前，从已冻结源构建只含有效数据的不可变 v1 回滚库和有效 uploads 归档。
- [ ] 回滚产物加密、异地/隔离保存，与源库、目标库和 purge 路径分离；保留至观察窗口结束。
- [ ] 自动恢复旧应用并完成三角色和核心流程冒烟，之后复核源 GTID/binlog 位点未变化。

验收：写屏障生效、源位点稳定、回滚产物可恢复且不含任何排除数据。  
验证：连接排空、位点比较、恢复演练和产物 manifest。

### H3. 数据和文件迁移与强校验

- [ ] 迁移有效用户、角色、权限、业务数据、原 ID、密码哈希和时间。
- [ ] 排除 `deleted=1`、测试/废弃表、`port_manage`、`work_runtime` 和 7 天未绑定附件。
- [ ] 建立文件 manifest，逐项校验存在性、大小和 SHA-256，复制有效 uploads。
- [ ] 除计数外，验证逐行规范化 checksum、外键/唯一性、状态映射、角色权限不变量、时区精度、密码哈希格式和关键聚合。
- [ ] 使用隔离且可清理的生产冒烟账号执行真实写入流程，验证新写数据的关联与删除语义。

验收：源/目标计数、逐行校验和、权限/时间/哈希不变量和文件 manifest 全部一致。  
验证：迁移 verify 报告和可清理业务验证。

### H4. 两次脱敏演练

- [ ] 从正式环境制作仅含有效数据的脱敏迁移源。
- [ ] 完整执行维护、迁移、校验、回滚、部署、清理流程两次。
- [ ] 每次记录耗时、数据量、异常、修复和最终窗口预算。

验收：连续两次在 2–4 小时窗口内通过，第二次无未解释人工步骤。  
验证：签名迁移报告和制品哈希。

### H5. 恢复并核实生产连接

- [ ] 通过可信渠道确认正式主机指纹、账号、项目路径、数据库实例和 Nginx/PM2 身份。
- [ ] GitHub Secrets 配置 SSH key、known_hosts、主机、端口、部署用户和数据库凭据。
- [ ] 只读 preflight 证明目标不是错误服务器/错误数据库。
- [ ] 生成并签名 production target manifest：主机指纹、账号、端口、部署根、PM2 进程、Nginx 配置、源/目标数据库和允许清理的绝对路径。
- [ ] 上述可信输入或管理权限缺失时标记 `BLOCKED`，绝不自动采用 `139.5.110.245` 等候选值。

验收：目标指纹、应用标识、数据库名和预期版本全部匹配。  
验证：自动只读生产审计报告。

### H6. 正式维护窗口自动切换

- [ ] 获取生产并发锁，校验 target manifest、当前生产版本 CAS 和 release manifest 全部摘要。
- [ ] 进入维护模式，执行 H2 数据库级写屏障、连接排空和最终源位点稳定复核。
- [ ] 确认迁移前安全回滚产物已经恢复演练通过，再由 Flyway 创建空 `jingxuan_v2` 并执行 H3。
- [ ] 执行 verify、三角色登录和全部核心冒烟；门禁通过后部署版本化制品、原子切换 PM2/Nginx，再次冒烟。
- [ ] 开放流量并强制全员重新登录。

验收：任何校验失败均在开放写入前自动回滚；开放后仅向前修复。  
验证：Release workflow、迁移报告、健康检查和 smoke 报告。

### H7. 观察窗口与最终 purge

- [ ] 开放流量后至少观察 24 小时，并完成全部生产回归；期间保留冻结 v1 源和 H2 回滚产物，禁止 purge。
- [ ] 门禁要求：错误率/P95 满足目标、事件失败积压为 0、关键数据不变量持续一致、文件校验无新增缺口、前向修复与有效 v2 恢复产物可用。
- [ ] 观察期通过后，purge 再次校验生产锁、版本 CAS、target manifest、排除清单哈希和一次性执行锁。
- [ ] 删除原始 v1 数据库及被排除文件，禁止重复、跨目录或对未列入 manifest 的对象执行删除。

验收：只删除计划排除项；生产完整回归在删除前完成；不生成包含排除数据的备份。  
验证：观察报告、删除前后 manifest、计数和路径边界测试。

### H8. 上线后验证与收口

- [ ] 监控错误率、P95、Redis、事件失败、慢 SQL、磁盘和文件校验。
- [ ] purge 后再次执行生产三角色与游客完整回归，并验证恢复/前向修复机制。
- [ ] 归档中文 Release、SBOM、校验和、迁移/回滚/性能/安全报告。

验收：观察窗口无发布阻断，全部功能矩阵标记“已验证”。  
验证：生产验收报告。

## 12. 自动停止条件

以下任一发生时，自动停止破坏性后续步骤并优先修复：

- Git 远端身份或生产主机指纹无法确认。
- clean build、契约、Testcontainers、E2E、安全或迁移 verify 失败。
- 数据计数、关键聚合、文件大小/SHA-256 不一致。
- 安全回滚产物不可启动或冒烟失败。
- 生产部署后健康检查/核心冒烟失败且自动回滚也失败。

这类停止不是人工审批门禁；修复并重新通过自动门禁后继续。

## 13. 完成标准

- 功能矩阵 18 行全部“已验证”。
- 后端只剩九模块 v2 实现，无旧 Controller/Adapter/根 Entity/Mapper/Service、无 `Result`、无跨模块 Mapper。
- 前端只剩 `app/features/shared`、WorkspaceShell 和 Orval/Vue Query，旧 API/布局/直接 Axios 为 0。
- Flyway 唯一 Schema、字符串状态、物理删除、实时排行和发奖快照全部生效。
- 覆盖率、E2E、性能、bundle、Lighthouse、安全、SBOM、观测和双部署门禁全部通过。
- 两次演练和正式生产切换完成，旧无效数据/文件已按授权清除。
