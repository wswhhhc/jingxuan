# 发现与决策

## 需求

- 用户要求自行判断原 v2 全量重构计划哪些部分尚未实行，并把未完成部分补齐，最终以整份计划完成为目标。
- Git/GitHub 相关描述必须使用中文。
- `.env` 及其中的密码、API Key、邮件配置严禁提交。
- 2026-07-12 新授权：完成度审计同时覆盖本地仓库与正式环境；新方案可依据兼容性和风险调整技术细节。
- 新方案生成后不等待人工批准，自动验证通过即连续实施；GitHub production 环境也以自动门禁替代人工审批。
- 用户授权实际连接 PM2 + Nginx 正式环境执行迁移、部署及最终旧数据/文件清理；Docker Compose 仍需保持受支持。

## 研究发现

- 当前仓库是 Git 仓库，分支为 `refactor/v2`，初始工作区干净。
- 最近提交包含 v1 精选、幂等点赞、评论创建/删除和教师评分接口，说明计划至少已部分推进，但不能据此认定对应业务闭环完成。
- 用户计划锁定后端 JDK 25 / Spring Boot 4.1 / MyBatis-Plus Boot4 / Spring Modulith 2.1，前端 Node 24 / Vue 3.5 / Vite 8 / TypeScript 6，并要求 Flyway、Testcontainers、OpenAPI/Orval、Vue Query、MSW、Playwright、Lighthouse CI 等完整工程能力。
- 历史记录曾把真实生产操作视为未授权；该限制已被 2026-07-12 用户明确授权替代，后续仍必须以自动校验、可回滚条件和审计证据约束破坏性步骤。
- 根目录已经存在统一 npm 验证入口，包含工具链、安全配置、格式、前端 lint/typecheck/test/build/audit、后端单元与集成验证以及 OpenAPI 生成一致性检查。
- 后端已升级到 Spring Boot 4.1.0、MyBatis-Plus 3.5.17、Spring Modulith 2.1.0，并引入 Flyway、Testcontainers、ArchUnit、JaCoCo 和 Springdoc；但 `backend/pom.xml` 当前仍配置 Java 21，与计划锁定的 JDK 25 不一致。
- 前端依赖已经达到 Vue 3.5、Vite 8、TypeScript 6、Element Plus 2.14、Pinia 3，并已引入 TanStack Vue Query、Orval 与覆盖率工具。
- 已存在 9 份 ADR、阶段基线/功能矩阵/路由权限/实现日志文档，以及 Flyway Java 基线迁移。
- Git 历史显示阶段 0、阶段 1 以及身份、参考数据、批次待办、作品审核互动、教师评分等切片均有提交；当前 HEAD 为 `feat: 新增 v1 教师评分接口`，后续功能仍需逐项核验。
- 根目录仍存在 `sql/`，且 Maven 会把它作为 `legacy-sql` 资源打包；这与阶段 6“Flyway 唯一 Schema 来源、删除重复 SQL”的最终状态明显不符。
- 当前会话工具列表中没有 Codegraph MCP 调用能力，但本机安装了 `D:\npm-global\codegraph.ps1`；后续源码理解改用 Codegraph CLI，文件清单和非源码配置仍可用 `rg`/PowerShell 检查。
- Codegraph CLI 支持 `init/index/sync/query/files/callers/callees/impact/affected`，但当前 `codegraph status .` 报告仓库“Not initialized”；现有 `.codegraph/daemon.pid` 是陈旧残留，需重新初始化索引后才能按项目约定查询源码。
- 首次短超时的 `codegraph init` 实际已完成初始化；复查显示索引最新，覆盖 606 个文件、8,268 个节点、8,257 条边（Java 300、TypeScript 241、Vue 41 等），后续可直接使用索引审计。
- `docs/refactor/implementation-log.md` 明确说明目前只是渐进迁移：旧页面尚未全面切换生成客户端，Modulith 全量 `verify()` 尚未启用，旧跨模块 Mapper 与循环依赖仍大量存在，Flyway 也只是包装旧 SQL 的过渡基线。
- 当前 CI 已有契约、前端、后端、安全和依赖审查任务，但仍使用 JDK 21，且尚未看到计划要求的 Playwright、Lighthouse、性能回归、SBOM/镜像制品以及带人工批准的发布工作流。
- Codegraph 显示目标后端九模块中只有 `identityaccess`、`referencedata`、`campaign`、`portfolio` 和极薄的 `evaluation` 出现实质代码；`communication`、`moderation`、`operationsreporting`、`workflow` 仍只有 `package-info.java`，大量业务仍位于旧 `modules/*`、根 `entity/mapper/service/controller`。
- 前端仍以旧 `api/components/composables/layout/router/stores/views` 为主，四套 Layout 和角色型手写 API 全部存在；`shared/api/generated` 只是并存的生成客户端，尚未形成计划要求的 `app/features/shared` 架构。
- 后端测试共索引 53 个文件，其中 v1 新契约测试集中在认证、参考数据、批次/待办和作品 DTO；大量业务测试仍验证旧模块。前端仅 10 个 `*.test.ts`，尚未看到 MSW 测试或仓库内 Playwright 测试套件。
- 当前 HEAD 的 `npm run verify:quick` 基线不是绿色：工具链与安全配置通过，但 `format:check` 因 `scripts/trim-generated-api.mjs` 不符合 Prettier 而失败，后续前端与后端测试尚未执行。
- 机械格式化后，前端 lint、类型检查、56/56 Vitest 与生产构建全部通过；后端单元测试 181/181 通过。
- 当前前端构建明显不满足计划预算：`vendor-element` 约 220.48KB gzip，`vendor-echarts` 约 177.62KB gzip，Element Plus CSS 约 31KB gzip；仅两个 vendor JS 就远超公共入口 180KB gzip 目标，也证实 ECharts 仍被独立大包构建而尚未按仪表盘路由精确控制。
- 后端单元测试生成 JaCoCo 报告时出现多项“class files 与 execution data 不匹配”警告，说明未 clean 的增量运行会复用旧执行数据；覆盖率门禁目前没有可靠的阈值验证。
- 本机实际工具链为 Java 21.0.7、Maven 3.9.10、Node 24.14.1、npm 11.11.0；当前没有已配置的 JDK 25，因此提升 `java.version` 前需要先提供/安装 JDK 25 运行时。
- 已在仓库外准备 Microsoft Build of OpenJDK 25.0.3 LTS：`D:\java\microsoft-jdk-25.0.3\jdk-25.0.3+9`，ZIP SHA-256 为 `BA3CE9FA6EBB921ECCDA29B08C2BAD26B5F8839080931096C73F306E0DD9856F`；后续可用临时 `JAVA_HOME` 完成本地 JDK 25 验证，不污染 Git。
- `.env` 未被 Git 跟踪，仅 `.env.example` 在版本库中；安全边界当前有效。
- `.github/workflows` 只有 `ci.yml`，没有 release、nightly、Lighthouse 或性能工作流；`scripts/` 也没有计划要求的迁移 CLI、k6 或部署回滚编排。
- 旧 `sql/` 下 16 个脚本全部仍被 Git 跟踪，包括测试数据与 Docker 初始化脚本，阶段 6 的重复 Schema 清理尚未开始。
- 部署配置仍是旧架构：Nginx 会剥离 `/api` 前缀，Docker 初始化直接挂载 `sql/base/init_schema.sql`，根 Dockerfile 仍使用 Java 17/Node 20，均与 v2 最终契约和锁定工具链冲突。
- `application.yml` 仍启用全局逻辑删除、枚举序号处理器和根 `entity/mapper` 包；这直接违反“物理删除、稳定字符串状态、模块内基础设施”的最终约束。
- 前端干净覆盖率实测仅 Statements 15.49%、Branches 18.04%、Functions 11.87%、Lines 15.90%；56 个测试虽全通过，但连现有 24/25/17/25% 低门槛都未达到，距离计划的整体行/函数 80%、分支 70% 很远。
- 生成 API 当前把旧 Controller 一并纳入覆盖统计且页面完全未消费，既扩大了未覆盖面，也印证契约源尚未收敛到纯 v1。
- 后端 `clean test` 已完成且 181/181 通过；干净 JaCoCo 数据为 Line 63.36%、Method 68.14%、Branch 44.27%，同样低于计划的整体 80%/80%/70%。POM 当前没有 `jacoco:check` 阈值，所以 CI 不会因覆盖率不足失败。
- `npm run api:check` 证实契约漂移：重新导出后新增 v1 评论、点赞、教师评分生成客户端与评论 DTO，同时改动已有生成文件；因此当前 HEAD 在未同步这些产物前无法通过 API 契约一致性门禁。
- OpenAPI 导出过程仍会在跳过测试时读取旧 JaCoCo 执行数据并产生 class mismatch 警告，构建生命周期配置需要把覆盖率报告限制在真实测试执行场景。
- 前端 v2 HTTP 启动切片已修复重复 `/api`：生成接口现在从同源根路径请求；新增共享 QueryClient 并在 `main.ts` 注册 Vue Query Provider。回归测试从 56 增至 58 个且全绿。
- 工具链校验已进入 TDD：新增 Java 版本解析与声明契约测试，当前用 JDK 25 运行根校验会正确因 POM 仍声明 Java 21 而失败，等待后端 POM 并行切片结束后切换到 25。
- POM/CI/Docker 已切换到 JDK 25，根工具链校验在 Microsoft OpenJDK 25.0.3 下通过；但首次 `mvn clean test` 暴露 Lombok 注解处理在 JDK 25 下未生效，大量构造器/getter/setter缺失，需升级或显式配置编译期处理器后再验证。
- Lombok 实际版本为 1.18.46；根因是 JDK 23+ 不再默认扫描类路径处理器而旧 maven-compiler-plugin 3.11 未显式配置。升级到 3.14.1、使用 `release 25` 并显式声明 Lombok annotation processor 后，JDK 25 下 184/184 单元测试通过。
- Maven clean 现会删除固定 `%TEMP%/jingxuan-backend-jacoco.exec`，根后端命令统一从 `clean` 开始且 JaCoCo 显式 `append=false`；JDK 25 下再次运行 184/184 单元测试后不再出现 class mismatch。
- Docker Hub 直连在当前网络超时；通过 DaoCloud 镜像已确认 `eclipse-temurin:25-jre-alpine` 与 `node:24-alpine` 多架构清单存在。Maven 25 标签的 manifest 深度查询超时，静态门禁已锁定标签，后续 Docker 构建验证时再确认拉取链路。
- 身份模块下一阶段的具体改造点已确认：refresh Redis 值只有单 token 会话，没有 family/后继关系；Controller 从 JSON 读写 refresh token；注册验证码可重复尝试且教师直接启用；CORS 仍为 `* + credentials`；旧登录同步写日志并生成旧长 JWT。认证加固需要集中到新 identity-access 应用用例，避免继续在 v1 Controller 拼装。
- 旧身份 Schema 使用 `sys_role.role_code=ROLE_*`、无 `portal_type/is_builtin`，`sys_user.status` 为数字且班级仍指向 `sys_dict`，角色/用户/菜单/关联表都带 `deleted`；阶段 2 需要先用新增 Flyway 迁移建立稳定字符串状态、内置角色保护与独立班级引用，再切换应用层。
- v2 API 代理已在 Vite、主机 Nginx 和 Docker Nginx 中增加优先 `/api/v1` 保留规则，旧 `/api` rewrite 仅作为迁移期兼容；静态门禁可防止新路径再次被剥离。
- 恢复会话后，工具链、安全配置和根格式门禁已在 JDK 25 环境全部通过；此前仅剩的两份脚本格式漂移已机械修复。
- Codegraph 已再次同步当前工作区，确认索引包含最新 2 个脚本变更，后续身份模块查询可直接使用。
- `api:check` 已在 JDK 25 下完整通过：运行时 OpenAPI 与 `openapi/jingxuan-v1.yaml` 一致（25 个 v1 路径），生成客户端为 40 个文件、30 个 v1 URL，无漂移。
- Bundle 预算已正确接入 `verify:frontend` 和 CI，复核值仍为公共核心 84.27KB、作品列表首屏 201.97KB、初始 CSS 11.70KB，均在预算内。
- CI 的 `frontend-quality` 会运行需要 Java 25 的 `verify:toolchain`，但该 job 尚未配置 JDK；现有工具链测试只统计全文件两处 `java-version: 25`，无法发现 job 作用域遗漏。
- 根 `verify` 尚未包含 `api:check`，会使本地“完整验证”在 OpenAPI 或生成客户端漂移时假通过。
- 根格式门禁尚未串联前端 `format:check`；前端当前有 40 个 Orval 生成文件和 1 个手写测试文件不符合 Prettier。决定明确忽略机器生成目录、格式化手写文件，并把前端格式检查纳入根门禁。
- 上述三项基础门禁缺口已收口：工具链测试现在按 CI job 作用域检查 JDK 25；根 `verify` 包含 `api:check`；根格式检查串联前端，并由 `frontend/.prettierignore` 排除生成目录。
- 完整前端复验为 12 个测试文件、59/59 用例通过，ESLint、Vue TypeScript、生产构建和 bundle 预算同时通过；仅保留来自 `@vueuse/core` 上游 PURE 注释位置的 Rolldown 非阻断警告。
- JDK 25 后端干净单元复验为 185/185，通过且 JaCoCo 数据匹配；当前编译仍有旧 `LoginResponse` Builder 默认值、弃用 API、Mockito 动态 agent 等非阻断迁移警告。
- identity-access Task 1 当前约完成 30%：已有 256-bit 随机 refresh、SHA-256 Redis 键、8h/30d 选择和 `getAndDelete`，但缺 family、ACTIVE/USED tombstone、重放撤销后继、多键 Lua 原子轮换、绝对截止与真实 Redis 并发测试。
- `RefreshTokenService` 的 v1 业务调用者只有 `V1AuthController`，Task 1 可保持 HTTP 签名不动先改内部；现有 Mockito 测试和“旧 token 401”API 测试无法证明并发安全或 family 撤销。
- 本机 Docker CLI 无法连接 `dockerDesktopLinuxEngine`，且未发现 Docker Desktop 可执行文件；当前无法实际执行 Docker build 或 Testcontainers，但可继续实现并编译测试设施。
- Spring Data Redis 4.1 官方脚本文档确认 `RedisOperations.execute(RedisScript, keys, args)` 会优先 `EVALSHA`、缺失时回退 `EVAL`，并建议复用单个 `DefaultRedisScript` 实例避免重复计算 SHA1：<https://docs.spring.io/spring-data/redis/reference/redis/scripting.html>。
- Redis 官方文档确认 Lua 脚本在服务端原子执行，且所有脚本访问的 key 名必须显式通过 `KEYS` 传入；`PEXPIREAT` 使用绝对 Unix 毫秒时间戳，适合保持 family 绝对截止：<https://redis.io/docs/latest/develop/programmability/eval-intro/>、<https://redis.io/docs/latest/commands/pexpireat/>。
- Refresh family 领域测试已经从缺少类型的 RED 转为 4/4 通过；随后构建只因新 JaCoCo 门禁未尊重显式 `jacoco.skip` 而失败，覆盖率门禁正在修正。
- 一次 `mvn clean` 因 `backend/target` 被短暂占用而失败；PID 31012 是用户旧 Spring Boot 进程且按约束未终止，改用不清理的目标测试后生产/测试源码均成功编译。
- v2 Modulith/ArchUnit 门禁现在只分析九个目标模块并执行 `modules.verify()`；对当前 8 个 Controller 旧服务桥接和 7 处根 Mapper 注入使用具体类白名单，因此新增迁移债务会失败、现有债务只能逐项删除。
- 当时首次覆盖率门禁曾锁后端 63.30/68.10/44.20、前端 22.78/23.73/16.95/23.85；后端旧值后续已证明来自历史 exec 污染，不能再作为当前门槛。原计划最终 80/80/70 与核心 90% 仍是后续硬验收。
- 阶段 0 的 Testcontainers 接线实际已基本完成：BaseApiTest 自动供给 MySQL 8/Redis 7，Surefire/Failsafe 按 integration tag 分流，CI `backend:verify` 会运行；静态计数约 126 个集成测试，不再依赖本机数据库。
- 阶段 0 尚未闭环的是旧冒烟：当前脚本失败仍退出 0、教师/学生密码与 fixture 不符、默认创建作品且不清理、根命令和 CI 未接线；安全重写切片正在实施。
- Docker Compose 已改为 MySQL 8.0.42 / Redis 7.4.5-alpine，后端使用 `mysql`/`redis` 服务名并强制注入无默认 JWT_SECRET；移除旧 `sql/` entrypoint 挂载，空库统一交由 Flyway 创建。
- 续接时工作区包含大量阶段 0–2 的未提交变更，且 `safe_smoke_slice`、`persistent_event_foundation` 正在写入各自隔离文件；主代理继续避免并发修改 `scripts/smoke-test.sh`、`package.json`、POM/Flyway/事件配置，并保持后端 Maven 串行。
- Spring Modulith 2.1.0 官方 `spring-modulith-starter-jdbc` 确实传递 core、events-api、events-core、events-jackson 与 events-jdbc；官方 MySQL v2 DDL 与新增 Flyway V2 内容逐行一致。
- Spring Modulith 2.1.0 的 `EventPublicationAutoConfiguration` 已自动导入带 `@EnableAsync` 的 `AsyncEnablingConfiguration`（仅在缺少现有异步配置时生效），因此新增 `PersistentEventConfiguration` 属于冗余配置，应在审查闭环后删除并让集成测试直接证明 starter 自动配置生效。
- 官方 2.1 源码确认 `completion-mode=delete` 只在成功完成时删除；监听异常会标记 `FAILED`，`FailedEventPublications.resubmit(ResubmissionOptions)` 可重提，`republish-outstanding-events-on-restart=true` 会在启动时重放所有未完成 publication。
- 开启持久化事件后，原 `BaseApiTest` 在 Spring 上下文创建后才用 `@Sql test-schema.sql` 建表，会早于/晚于 Modulith 启动重放顺序而失败；已改为 Testcontainers MySQL 在上下文刷新期间由 Flyway V1/V2 建表，并删除重复测试 Schema。
- H2 OpenAPI 导出关闭 Flyway 时，必须同时排除 JDBC `JdbcEventPublicationAutoConfiguration` 和核心 `EventPublicationAutoConfiguration`；只排除 JDBC 会让官方 `StalenessMonitorConfiguration` 因缺少 `EventPublicationRegistry` 启动失败。
- Refresh family 适配器新增第二个测试构造器后，Spring 7 无法自动选择单参生产构造器；OpenAPI 真实启动暴露 `No default constructor found`，已在生产构造器上显式 `@Autowired`，随后 H2 OpenAPI 导出成功。
- 当前 POM 仍从 `${project.basedir}/../sql` 打包 Flyway V1 资源，而两个 Docker 构建都只复制 backend 内部文件；因此镜像中的 JAR 会缺基线 SQL。正在把 V1 资源迁入 backend classpath并补独立 CI 运行时冒烟。
- V1 精确引用的 14 个 SQL 已逐 blob 原样迁入 `backend/src/main/resources/legacy-sql`，POM 不再引用上级目录；根 `sql/` 与 `backend/sql/` 重复 Schema 已删除，仅保留测试专用 cleanup/test-data fixture。
- 独立 `legacy-runtime-smoke` CI job 已接线：随机脱敏凭据、唯一 Compose project、有界等待、Flyway V1/V2 与关键表断言、Redis 清空、cleanup→test-data 单事务、full 冒烟、失败日志和 always 销毁卷。当前本机仍无 Docker，真实 job 只能由 CI/可用 daemon 证明。
- 持久化事件恢复已形成单实例闭环：同一 `EVENT_RECOVERY_ENABLED` 同时控制启动重放和周期调度，官方 staleness 四键恢复 PUBLISHED/PROCESSING/RESUBMITTED，有界参数和 `lastResubmissionDate` 冷却避免无界重试；生产屏蔽 Modulith 内部可能 stringify 事件的 INFO 日志。
- `docs/specs/identity-access-v2.md` 已把身份阶段拆成 5 个可验证任务；当前仅 Task 1（Refresh family）正在收口，Task 2–5 的 Cookie/Origin、登录限流与算术 challenge/BCrypt 12、教师待审核审批、前端内存 Session 仍未实施，阶段 2 全门禁绿色后应按此顺序进入阶段 3。
- 当前会话已创建持续目标，目标范围为完成原计划中所有仓库内可实现工作；真实生产切换、GitHub 人工批准、SSH 部署与 Docker/Testcontainers 实跑仍需对应外部环境或授权，不能以静态配置冒充运行证据。
- Codegraph 复核确认 Task 2/5 的当前差距仍然真实存在：`V1AuthController` 的 refresh/logout 仍读取 JSON `V1RefreshRequest`，`V1LoginResponse.from(...)` 仍接收并暴露 refresh token；前端 `useAuthStore` 仍从 `sessionStorage/localStorage` 恢复 access token。后续必须先改 HTTP 契约并重新生成 Orval 客户端，再迁移内存 Session，避免前后端契约短暂失配。
- 身份安全预读进一步确认：`SecurityConfig` 当前全局关闭 CSRF、未配置严格 CORS/Origin 适配，`new BCryptPasswordEncoder()` 使用默认 cost 10 而非计划的 12；v2 Axios 出口仍通过旧 `getAuthToken()` 从浏览器存储注入 Bearer。Task 2–5 必须把 Cookie 端点的 Origin 校验、BCrypt 12/透明升级和内存 token 注入作为同一身份闭环验证，而不能只改 Controller DTO。
- 现有 `V1AuthApiTest` 仍从 JSON 响应读取 refresh token、以 JSON body 轮换，只断言旧 token 重放返回 401，未证明后继 token 同时失效；`V1AuthControllerTest` 也把 refresh 明文作为成功响应断言。Task 2 的 RED 测试应先反转这些契约，并保留 Task 1 的 family 后继失效集成断言。
- 阶段 2 独立审查发现真实安全阻塞：`scripts/apply-runtime-support.ps1` 和 runtime demo 示例曾跟踪相同的数据库默认密码。已通过 RED→GREEN 新增当前工作区敏感默认值门禁，移除两处默认值，并让 PowerShell 脚本从 `DB_PASSWORD`/安全提示读取且不再把密码放入 mysql argv；工作区已无该值。由于该密码已存在于 Git 历史，应视为泄露并由用户在真实 MySQL 环境轮换，不能仅靠删除当前文件恢复安全性。
- 额外搜索发现旧用户创建与 AI 导入仍存在弱默认密码 `123456`；它不是环境密钥泄露，但违反 v2 新密码策略，需在身份阶段结合 BCrypt 12、首次登录/临时凭据流程一起移除，避免用简单替换破坏导入闭环。
- Flyway V1 迁入后的 `legacy-sql/base/init_schema.sql` 曾在每个空库创建启用的 `admin/admin123`，而 Compose 自包含迁移会把它变成所有新部署的真实默认管理员。已新增 RED→GREEN 门禁、删除生产 seed，并给 Testcontainers Flyway 测试增加“迁移后 `sys_user` 为 0”断言；测试账号继续只存在于 `src/test/resources/sql/test-data.sql`。因此先前“14/14 SQL 与 HEAD blob 完全一致”的证据已被有意替代：13 个资源仍原样，init_schema 仅安全删除默认管理员块。
- 阶段 2 独立审查确认 OpenAPI 当前会静态假绿：Springdoc 默认 `springdoc.override-with-generic-response=true` 会把旧 `GlobalExceptionHandler` 的 `ResultVoid` 错误套到 v1；YAML 同时漏 401/422、给公开接口错误加全局 Bearer、提交构建端口 server，并保留多处雪花 ID `int64/number`。已分派独立 TDD 切片，以 RFC Problem Details、按真实公开/保护端点的 security、同源 server 和全量 ID 字符串语义门禁收口。
- 官方 Springdoc 属性文档明确：`springdoc.override-with-generic-response` 默认 `true`，会自动把 `@ControllerAdvice` 响应加入所有生成响应：<https://springdoc.org/properties>。Spring Framework 7 错误响应规范说明 Problem Details 是 RFC 9457 的结构化错误载体：<https://docs.spring.io/spring-framework/reference/web/webmvc/mvc-ann-rest-exceptions.html>。
- 前端/部署审查确认 `/jingxuan/` 仍是假支持：Vite 生成子路径资产 URL，但两套 Nginx 没有把该路径映射到 dist；CI runtime smoke 只启动 backend，无法发现前端 404。Compose 还向所有宿主网卡暴露无认证 Redis、MySQL root 与 backend，且缺 `.dockerignore`，会把宿主 node_modules/dist 等带入 Docker context。已分派单独切片修复子路径映射、镜像可复现、仅 Nginx 对外和经 Nginx 的运行时冒烟。
- v2 Axios 即使 `baseURL='/'` 仍允许调用方用 absolute `url`/覆盖 config，现有拦截器会把 Bearer 附到跨源请求；身份/前端阶段必须收窄 Orval options 并拒绝非 `/api/v1` 相对 URL，补 token 不跨源测试。
- v2 Axios 同源边界已收口：唯一出口现在只接受 `/api/v1` 或其子路径，`url/method/baseURL` 在 TypeScript 与运行时都不可由 Orval options 覆盖；absolute/protocol-relative URL 会在请求派发和 Bearer 附加前失败，合法 headers/params/signal 保留。Access Token 仍来自旧 Web Storage，仅作为阶段 2 边界加固，内存 Session 留给身份 Task 5。
- `backend/src/test/resources/schema-test.sql` 与 `data-test.sql` 是无人引用的 H2 重复 Schema/seed，违反 Flyway 唯一来源；已删除并在 V1 资源契约测试中永久禁止恢复。
- 弱默认密码调用链已定位：管理员单个/批量创建最终都进入 `SysUserServiceImpl.createUser()`，缺密码时统一写 `123456`；AI 解析还会指示 DeepSeek 生成该密码并在 UI 明文预览。安全收口应要求普通创建/JSON batch 显式提供初始密码，同时让 AI 只整理非敏感账号字段、由管理员在本地预览区输入统一初始密码后直送后端，避免把密码放进 LLM 消息；随后把全局 BCrypt cost 提升到 12。首次登录强制的后端安全边界仍属于身份阶段后续用例。
- Spring Security 官方密码存储文档用于后续 BCrypt/透明升级实现依据：<https://docs.spring.io/spring-security/reference/features/authentication/password-storage.html>。
- 三个阶段 2 收口切片停止写入后，主线程复跑 OpenAPI Node 语义门禁 10/10、部署安全门禁 21/21、相关前端 Vitest 20/20、Vue 类型检查与完整 `verify:config`（含 smoke 23/23）均通过。
- 主线程审查发现 `OpenApiConfig.customizeOperation()` 当前先删除 operation 已有的全部 4xx/5xx 响应，再只补通用 400/401/403/422/500/default；这会抹掉资源不存在 404、原计划明确要求的并发冲突 409 等显式契约。统一 Problem Details 应替换错误响应的媒体类型/Schema，而不应删除业务状态码。
- OpenAPI 独立审查进一步发现：复数雪花 ID `attachmentIds` 被 customizer 错改成标量 string；公开的登录/刷新虽然不需要 Bearer，运行时仍会返回 401；所有 201/204 operation 被 Springdoc 记成 200；Orval 默认 `TError=ProblemDetails` 与 mutator 实际抛出的 `ApiProblemError` 不一致；路由级 404/405 仍可能回退旧 Result；ProblemDetails 门禁未校验字段结构。
- 主线程已用 RED→GREEN 修复其中三项契约根因：保留并规范化已有错误状态、登录/刷新强制声明 401、普通 `Schema(type=array)` 的复数 ID 保持数组且仅把元素转为 string；Node 语义门禁扩展到 ProblemDetails 必需字段与复数 ID，13/13 通过，Java `OpenApiConfigTest` 3/3 通过。
- Orval 8.20.0 官方自定义 Axios 文档明确支持在 mutator 导出 `ErrorType<Error>` 与 `BodyType<BodyData>`，生成的 Query/SWR 泛型会使用该包装类型：<https://orval.dev/docs/guides/custom-axios.md>。当前已按该扩展点让生成客户端后续使用 `ApiProblemError<ProblemDetails>`，并新增生成产物门禁。
- 部署独立审查发现根 Dockerfile 的 dist 复制路径与 Nginx root 不一致、Compose 未门禁数据库密码默认值、`/jingxuan/` 静态资源可回退 HTML 导致 200 假绿、安全头/缓存被 location 继承规则绕过、缺应用健康检查且后端使用 MySQL root；已进入独立 TDD 修复切片。
- 初始密码独立审查发现服务端仅拒绝空白密码、AI 凭据关键词规则可绕过且会误伤“密码学/passwordless”、明文响应式状态未及时清理、合法 JSON `null` 会触发 NPE，以及 cost 12 下无界串行批量导入可能超时；已进入独立 TDD 修复切片。
- 阶段 2 的 9 类后端收口测试已在 JDK 25 下 52/52 通过。真实 `api:contract:update` 随后证明 Springdoc 可完整启动、生成并优雅停止，但运行时 `ProblemDetails` Schema 未输出显式 `type: object`；严格门禁正确拒绝写入旧/不完整契约，下一步应修 Java 模型声明而非放宽门禁。
- 运行时 `backend/target/openapi/openapi.json` 已确认 `ProblemDetails` 的 properties 与七个 required 字段全部正确，唯一缺口就是顶层 `type`；Java record 当前类级 `@Schema` 只声明 name/description，最小修复是显式声明 `type = "object"` 并用现有真实导出门禁验证。
- 实测类级 `@Schema(type = "object")` 在当前 Swagger Core/Springdoc 对 Java record 的导出中仍不产生顶层 `type`，因此不能依赖该注解闭环；应由现有 `OpenApiCustomizer` 在文档最终模型上规范化 `components.schemas.ProblemDetails.type`。
- OpenAPI/Orval 产物现已真实闭环：YAML 的 `ProblemDetails` 顶层为 object 且 required 完整，`attachmentIds` 保持 string 数组；生成模型为 `attachmentIds?: string[]`，10 个生成控制器客户端使用 `TError = ErrorType<ProblemDetails>`。完整 `api:check` 通过，旧产物阻断已关闭。
- 阶段 2 完整后端单元命令实际运行 266/266 全通过，但 JaCoCo check 只看到 Line 41.07% / Method 40.92% / Branch 32.72%，远低于此前锁定的 63.30/68.10/44.20。由于行为测试全绿而采集比例骤降，优先怀疑多个 Surefire fork 共用同一 exec 且 `append=false` 导致后写覆盖前写；不能以调低门槛收口。
- 当前 JaCoCo CSV 实际总量为 1900/4627 行、372/909 方法、669/2044 分支；大量旧 Facade、认证服务、待办和 Controller 仍为 0 覆盖。POM 没有显式多 fork，Surefire 使用默认单 fork，因此“append=false 被并行 fork 覆盖”尚未成立；下一步需对比分母变化、当前新增 class 与历史门槛生成方式。
- 当前工作区相对锁门槛时新增/扩张了大量生产实现：新的 Redis refresh 适配器源码 533 行（替换旧 103 行）、OpenApiConfig 净增约 251 行、持久化事件配置约 196 行、SecurityErrorResponseWriter 52 行，并有多处异常/AI 加固。六个未跟踪生产文件合计 859 源码行，足以解释 JaCoCo 分母从早期约 3000 行扩到当前 4627 行；覆盖率失败更像“门槛锁定后新增代码未同步补足覆盖”，而不是 exec 丢失。
- Effective POM 确认实际 Surefire 3.2.5 未配置 `forkCount`/`reuseForks`，使用默认单 fork；当前新增核心类自身大多已有较高行覆盖（OpenApiConfig 138/163、RedisRefreshTokenService 126/157、AI 导入 166/195），因此仍需检查 exec session 数和历史门槛是否混入集成覆盖，不能只凭源码分母推断。
- 用 JaCoCo Core 直接读取当前 exec，确认只有 1 个 session、5504 个 class execution entries；因此不存在多个 fork 用 `append=false` 覆盖数据。41% 是当前“仅 unit tag”套件的真实覆盖率，旧 63.30% 门槛的来源/口径已与当前单元命令不一致。
- 在 detached HEAD 临时 worktree 仅补 JDK 25 编译兼容后重跑 181 个旧单元测试，JaCoCo agent 参数明确没有 `append=false`，报告同时出现 7 个“execution data 与 class 不匹配”警告；旧 POM 也不会清理 `%TEMP%/jingxuan-backend-jacoco.exec`。这直接证明历史 63.36% 基线来自跨运行累积/污染数据，而非可靠 clean unit 覆盖。当前 clean 删除 exec + `append=false` 的单 session 报告才是可信口径。
- 首次纠偏后的可信 bundle XML 为 Line 1900/4626=41.0722%、Method 372/909=40.9241%、Branch 669/2044=32.7299%。阶段门禁按原“向下留一位”策略锁为 41.00/40.90/32.70，并继续保留最终 80/80/70 目标；后续最新实测已提升，门槛未降低。
- 密码/AI fresh-context 复审复现新阻断：`密码 Cedar!84Wave`、`password Cedar!84Wave`、`密码->Cedar!84Wave` 未被前后端检测，会发送给 DeepSeek；结构化字段中的“密码学/密码安全课程/Password Security”又会误判。批量 API 的 120 秒 Axios 超时也未越过 Nginx 60 秒 `proxy_read_timeout`，100 次 cost-12 BCrypt 仍可能网关超时后在后台部分写入。
- 原始 v2 计划与 `identity-access-v2.md` 均明确 AI 导入为“每管理员 10 次/小时”，且限流能力应由 identity-access 暴露为通用 API，后续游客评论复用；本轮修复不能把一次性限流缓存耦合进 AI 实现。身份 Task 2 随后必须把 refresh 从 JSON 改为 `HttpOnly + SameSite=Strict + Path=/api/v1/auth` Cookie，并对登录/刷新/注销做严格 Origin 校验。
- 覆盖率口径已在三份 refactor 文档中纠正；最终阶段 2 unit-only XML 为 41.8441/41.4661/34.0476，门槛保持 41.00/40.90/32.70，目标 80/80/70；旧 63.36/68.14/44.27 仅作为跨运行污染与 class mismatch 历史记录。
- 前端加固测试覆盖高代理项、组合/预组合变音、Unicode 箭头、纯字母/符号/连接词/白名单组合凭据、正常学术与邮箱文本，以及批量全成功/部分失败/全失败状态；最终全量 134/134，独立攻击矩阵 23/23 Approve。
- 后端 `RateLimitService` 当前由可注入 Clock 的 Caffeine 固定窗口实现，调用者只有 legacy `SysUserController`；HTTP 429 与 `Retry-After: 3600` 已由后续回归测试闭环。该实现仍只适用于单进程，必须在身份 Task 3 替换为 Redis 多实例原子限流。
- `V1ExceptionHandler` 已能把任何 400–599 的 `BusinessException` 映射为真实 Problem Details 状态，因此未来 v1 的 429 基础正确；legacy `GlobalExceptionHandler.handleBusinessException` 仍直接返回 `Result`，HTTP 默认为 200。最小兼容修复可仅对 code=429 返回 HTTP 429 + `Retry-After: 3600`，其他 legacy 业务错误继续保持现状。
- AI 边界实现已限制 40 条/单条 4000/总计 16000 字符、输出 users≤100、status∈{0,1}，结构化字段复用“凭据引用”而非裸术语检测；Caffeine 固定窗口通过 map.compute 原子递增，但 24 小时 access expiry 和单实例语义意味着它只能作为阶段性适配器，不能替代最终 Redis 多实例限流。
- legacy `GlobalExceptionHandler` 已对 `BusinessException(code=429)` 做最小兼容特例：返回真实 HTTP 429 与 `Retry-After: 3600`；其他 legacy `BusinessException` 仍保持 HTTP 200 + `Result`，避免阶段 2 扩大兼容面。v1 的错误继续由 `V1ExceptionHandler` 输出 Problem Details。
- 两套 Nginx 已把普通请求体上限收紧为 2MB，仅 `/api/file` 保留 1600MB，并把代理读取超时统一为 130 秒，覆盖 cost-12 批量导入与大文件上传的不同边界；后续若导入改为异步任务，应再缩短普通业务超时而不是继续放宽全局代理。
- AI 限流当前在进入业务校验前扣减，属于保守的成本保护语义；是否改为仅对实际外部模型调用计数应在 Redis 通用限流 Task 3 中以明确契约决定，不能在 Caffeine 过渡实现中静默改变。
- 批量导入部分成功后保留整批输入便于修复，但整批重试会由后端重复校验已成功账号并返回失败提示；当前不会重复创建，属于可用性风险而非数据一致性阻断，后续 UI 可按失败明细生成可重试子集。
- 阶段 2 最终全量门禁由主线程复验：工具链 7/7、配置 31/31、smoke 23/23、OpenAPI 17/17、前端 134/134、后端 331/331 全部通过；不是沿用旧代理结果或旧测试数量。
- 当前可靠后端 unit-only 覆盖率为 Line 41.8441%、Method 41.4661%、Branch 34.0476%，仍只代表防回退基线，距离最终 80/80/70 很远；前端 Statements 28.50%、Branches 28.70%、Functions 20.88%、Lines 29.99%，同样不能把门禁通过表述为最终覆盖率目标已达成。
- `git diff --check` 无空白错误；`.env` 既未跟踪也未进入工作区状态且被 `.gitignore` 命中。工作区仍有 338 个阶段性条目（含大量遗留删除与新 v2 文件），必须继续保留，不进行 reset/checkout 或无关清理。
- 阶段 3 Task 3 当前仅完成 BCrypt cost 12 和 AI 导入过渡限流；仍缺 Redis 登录/AI 通用限流、5/20 登录阈值、一次性算术 challenge 与旧低成本 BCrypt 哈希透明升级，不能表述为身份限流闭环。
- 阶段 2 独立安全复审发现凭据检测仍错误依赖“无连接词后的 token 必须含数字”，会放过 `password CedarWave!`、`密码 monkey`，且英文词表缺少 `secret`/`api key`；初始密码新策略不能代表历史口令或其他 secret，必须扩大安全边界并保留密码学/Password Security/邮箱等正常文本反例。
- 两套 Nginx 的普通前缀 `location /api/file` 会匹配 `/api/fileevil`，从而绕过普通 2MB 限制并让非上传路径接收至 1600MB；应按真实路由收紧到带边界的上传路径，并让配置门禁主动拒绝宽前缀。
- Spring Framework 7.0.8 `ResponseCookie.ResponseCookieBuilder` 官方 Javadoc 明确提供 `path`、`secure`、`httpOnly`、`sameSite` 与 `maxAge`；`maxAge(0)` 表示立即过期，`SameSite=Strict` 只随 same-site 请求发送，适合 Task 2 用同一构造器生成和清除 Cookie：<https://docs.spring.io/spring-framework/docs/7.0.8/javadoc-api/org/springframework/http/ResponseCookie.ResponseCookieBuilder.html>。
- Spring MVC 官方 CORS 文档强调 credentialed CORS 建立高信任边界，并建议尽可能使用有限 origins；因此当前 `allowedOriginPatterns("*") + allowCredentials(true)` 必须移除，不能仅依赖浏览器 Cookie 属性：<https://docs.spring.io/spring-framework/reference/web/webmvc-cors.html>。
- MDN Set-Cookie 参考确认 HttpOnly 会阻止 `document.cookie` 读取但浏览器仍会随 XHR/fetch 自动发送；`Max-Age=0` 立即过期，Path 只控制发送范围而不是独立安全边界。正式 HTTP 不能使用 Secure 是原计划已接受风险：<https://developer.mozilla.org/en-US/docs/Web/HTTP/Reference/Headers/Set-Cookie>。
- Nginx 上传边界已通过测试驱动收紧：只有 `^~ /api/file/` location 可以声明 1600m，且必须使用无 URI 的 `proxy_pass` 保留完整路径；`/api/fileevil` 会落入普通 `/api/` 规则并受 2m 限制。配置单测 31/31、实际基线与 smoke 23/23 通过。
- 凭据检测已增加 `secret`、`api key` 和无连接词旧口令反例，并在消息与模型结构化字段双边界拒绝；当前实现为了放行 `Password Security`/`Credentials Team`，英文无连接词分支仍采用启发式，已单独发起纯字母旧口令 adversarial 复审，不能仅凭现有 59/59 宣告彻底无绕过。
- adversarial 复审实际确认纯字母英文值仍可绕过：`password monkey`、`secret monkey`、`api key monkey` 均未命中。最小安全语义应改为“英文凭据标签 + 空格 + 任意非空 token 默认拒绝”，仅对锁定的正常短语 `Password Security` 与 `Credentials Team` 做窄排除；邮箱本身不含标签后的空格，无需额外豁免。
- Task 2 过滤器顺序可复用现有安全错误链：`RequestIdFilter` 已是 `Ordered.HIGHEST_PRECEDENCE`，Origin filter 应排在其后、Spring Security 前，从而让 403 Problem Details 始终带 requestId；公开的 `RestAccessDeniedHandler` 可供过滤器调用，避免在 identity 模块复制错误序列化逻辑。
- 当前 `WebMvcConfig` 明确配置 `allowedOriginPatterns("*") + allowCredentials(true)`，`SecurityConfig` 全局关闭 CSRF，而 Vite 的 `/api/v1` 代理使用 `changeOrigin: true`。Task 2 必须同时移除通配 CORS、增加三条 Cookie POST 的精确 Origin 校验，并把 v1 开发代理保留浏览器 Host/Origin 语义，否则合法 localhost:5173 请求会被后端误判为跨源。
- 第二轮凭据实现通过全局删除 `Password Security`/`Credentials Team` 再扫描，会把这些短语作为其他 secret 的值或带连接符扩展一起抹掉，实际放过 `secret Password Security`、`api key Credentials Team`、`Password Security -> monkey`。正确做法是逐个检查原文本中的凭据候选，仅忽略候选本身精确等于锁定正常短语且后续没有 connector+value 的情形，不能先重写整段输入。
- Spring Framework 7.0.8 官方 Javadoc 说明 `OncePerRequestFilter` 通过 already-filtered request attribute 保证单次 dispatch 执行，并支持 `shouldNotFilter(HttpServletRequest)` 做精确路径豁免；Task 2 Origin filter 可只覆盖三个认证 POST，不影响 legacy 或普通 Bearer API：<https://docs.spring.io/spring-framework/docs/7.0.8/javadoc-api/org/springframework/web/filter/OncePerRequestFilter.html>。
- Spring Security 官方文档说明 unsafe HTTP 方法默认启用 CSRF 防护；本项目为无状态 Bearer API 已全局关闭 CSRF，因此 Cookie 化后的 login/refresh/logout 必须由计划锁定的严格 Origin filter 提供窄边界补偿，并以“过滤器在业务副作用前拒绝”测试证明，不能把 SameSite 单独当作完整 CSRF 防线：<https://docs.spring.io/spring-security/reference/servlet/exploits/csrf.html>。
- 候选级白名单修复关闭了嵌套 secret、标点与箭头组合绕过，但限定复审仍发现英文赋值连接词 `equals`/`equal to`/`as` 未被现有 `is/set to/use/using` 集合覆盖；最终修复应一次补齐常见赋值语义（含 `mean/means`），避免中英文规则明显不对称。
- 最终凭据实现已补齐 `equal to/equals/as/mean/means`，保留逐候选扫描，不再重写整段文本；限定攻击矩阵 23/23 Approve，前端相关 61/61、后端 93/93，并由最终全量 134/134 与 331/331 覆盖。
- OpenAPI 导出首次在高系统提交内存下崩溃的直接证据是 forked JVM 的 ergonomic MaxHeap 4028MB、Metaspace unlimited；专用 profile 现固定 Xms128m/Xmx512m、MaxMetaspaceSize 256m、CompressedClassSpaceSize 128m，并由工具链第 7 个测试锁定。相同环境真实 `api:check` 复验通过。

- Task 2 已完成并经两轮独立五轴审查：refresh 令牌只通过 `jingxuan_refresh` HttpOnly Cookie 传输，login/refresh/logout 严格校验 Origin，登录响应不再泄露 refresh，OpenAPI/Orval 同步为 25 个 v1 路径、40 个生成文件与 30 个 v1 URL。
- 严格 Origin 比较必须保留客户端完整 authority。Nginx `$host` 会去掉 `${HTTP_PORT}` 等非默认端口，使 `Origin: http://host:8088` 与后端看到的 `http://host:80` 不一致；两套 `/api/v1` 代理现固定使用 `$http_host`，并由 `$host` 反例门禁防回退。
- Task 2 的剩余验证缺口仅是环境证据：本机无 Docker daemon，尚不能真实执行 `V1AuthApiTest`、Redis 并发和经过 Nginx 非默认端口的认证冒烟；仓库内实现、编译、单元/契约、配置和静态门禁均已完成。
- Task 3 只读审计确认：Caffeine `InMemoryRateLimitService` 仅保护 AI 导入，`PublicRateLimitFilter` 是另一套非线程安全且代理 IP 语义错误的 Caffeine 计数器；登录没有限流，challenge 不存在，旧 BCrypt cost 10 登录后不会透明升级。Redis Lua、脱敏日志与 Testcontainers 测试模式可复用 Refresh 适配器。
- Task 3 锁定实施顺序：Redis 通用计数决策与原子 Lua → AI/公开过滤器迁移 → 一次性 5 分钟算术 challenge → 账号/IP 登录 5/20 阈值与 `RATE_LIMITED + Retry-After` → BCrypt CAS 透明升级 → OpenAPI/Orval 收口。账号成功只清账号失败计数，IP 失败量保留到窗口结束，避免共享 IP 被任意成功登录清零。
- BCrypt 重做版已避免“认证后重新查询哈希再 CAS”与大小写不敏感比较两个已知竞态。首轮唯一失败是测试错误地要求最终 `Authentication.principal` 携带新哈希；实际 CAS、数据库 cost 12 与 `updatePassword` 均已发生。Spring Security 7.1 保留认证前 principal 是框架行为，测试应直接断言 password service 返回的新 `JwtUserDetails`，生产实现不应为满足错误断言退回第二次查询手写升级。
- Task 3 当前登录入口仍由 `V1AuthController` 直接调用 legacy `AuthService.login()` 后自行签发 v1 Access/Refresh，且 `AuthServiceImpl` 仍用旧 `IpUtil` 仅记录成功日志；账号/IP 失败计数、challenge 判定和统一失败编排尚未存在。按身份规范，最小正确边界应新增 identity-access 登录保护用例，由 Controller 传入可信解析后的 IP，不能把 Redis/阈值逻辑散落进 legacy AuthService。
- `LoginRequest` 当前只限制 challengeId 最长 64，未锁定实际 22 字符 Base64URL，也未表达 challengeId/challengeAnswer 必须成对出现；`RedisChallengeService` 对非法参数返回 false、存储故障抛普通 `IllegalStateException`。Task 3 需要在 HTTP 边界收紧格式/成对语义，并把存储不可用稳定映射为 503。
- `RedisRateLimitService` 已实现 subject 哈希、策略隔离和 fail-closed，但 limit/window 只有正数校验，没有上限；恶意或误配置调用者可制造异常大策略值。通用 API 应锁定可支持所有已知策略的保守上限，并由单元测试证明超界输入不访问 Redis。
- 当前代码没有 `RATE_LIMITED`、`CHALLENGE_REQUIRED` 等机器码异常类型，`V1ExceptionHandler` 对普通 `BusinessException` 固定输出 `BUSINESS_ERROR`、对 `UnauthorizedException` 固定输出 `UNAUTHENTICATED/FORBIDDEN`。登录保护要满足稳定 Problem code，需引入窄的身份访问异常模型或显式 handler，不能仅靠不同中文消息区分。
- `SecurityConfig` 已公开 `/api/v1/auth/challenges`，但 challenge 端点本身没有 IP 限流；攻击者可无限发行 5 分钟 Redis key。发行限流必须在调用 `ChallengeService.issue` 前使用可信 IP 与 Redis 通用限流，Redis 失败时返回 503。
- Redis 限流通用 API 已收紧不受信任/误配置策略：最大 limit 1000、窗口 24 小时、subject 512 字符。超界请求 fail-closed 且不访问 Redis；该上限覆盖当前所有锁定策略（每分钟/小时/15分钟/10分钟、最大20次），并抑制策略 key 与 SHA-256 输入的高基数滥用。
- Task 3 登录保护已完成最小身份访问用例：先 inspect 账号/IP 的 20 次策略（任一触发立即 429），再 inspect 两个 5 次策略（任一触发要求 LOGIN challenge）；凭据失败后四个策略均 consume，成功仅 reset 两个账号策略。v1 Controller 在实际认证前后调用该用例，因此 hard-limit 不会继续消耗 BCrypt。
- 为保证 v1 错误契约不依赖中文消息，新增 `IdentityAccessProblemException`。登录风控和 challenge 发行分别以该类型表达 `RATE_LIMITED`、`LOGIN_CHALLENGE_REQUIRED`、`LOGIN_CHALLENGE_INVALID`、`LOGIN_CREDENTIALS_INVALID` 与 `CHALLENGE_UNAVAILABLE`；`V1ExceptionHandler` 统一产生 RFC Problem Details，并仅在异常携带剩余秒数时写 `Retry-After`。
- 独立审查确认可信代理范围、公开 API 原始 URI 高基数、AI 实际 Retry-After 传播和 Nginx 静态门禁仍需继续修复；Task 3 不得仅凭当前登录纵切绿色就关闭。
- 审查中的可信代理绕过已修复为部署显式契约：默认只信任 loopback CIDR；PM2 后端只监听 loopback，Docker 因 backend 不发布端口而通过 Compose 显式授权容器私网 CIDR。任意 RFC1918 直连客户端不再因地址类别自动获得 X-Real-IP 覆盖权。
- 公开限流的 Redis subject 现只含可信解析后的 IP，不再包含动态 URI 或 matrix parameter，避免路由轮换既绕过额度又制造 key 高基数；同时 v1 已公开的 showcase/参考数据进入相同全局 IP 桶。
- 旧接口保持 `Result` 兼容格式，但 `RateLimitedException` 将真实 Redis `retryAfterSeconds` 传递为 HTTP 429 的 Retry-After；v1 身份路径已使用 RFC Problem Details 的 `RATE_LIMITED`。
- Task 3 的 OpenAPI/Orval 已从 25 条扩展为 26 条 v1 路径：公开 challenge 被显式纳入成功状态、匿名 security 与生成客户端 URL 门禁，避免“已实现路由但契约分类器未登记”的假绿。
- Task 3 独立五轴复审最终结论：仓库内实现可批准进入下一任务；已关闭挑战发行 key DoS、Redis 503、challenge 输入成对、可信代理、公开 URI 分桶、Lua 响应语义、真实 Retry-After 与 Nginx 假阴性。仍未获得的不是代码证据，而是 Docker/Testcontainers 和经真实 Nginx 的运行环境证据；不得将其写成已实跑。
- Task 4 审计确认：现有 `RegistrationService` 只提供 legacy `/auth/*` Map 请求，教师注册与学生同样设置 `ENABLED`；`UserStatusEnum` 仅 0/1，`CustomUserDetailsService` 只拒绝 `DISABLED`，因此待审核教师当前会直接登录。管理员用户管理只存在 legacy status 更新，没有 v1 `user:approve` 权限端点。需按规范增加 PENDING_APPROVAL、v1 注册/邮件/审批契约、一次性验证码消费和对应权限测试。

## 技术决策

| 决策                                                     | 理由                                       |
| -------------------------------------------------------- | ------------------------------------------ |
| 优先使用 Codegraph 理解源码与调用链                      | 遵循仓库 AGENTS.md，并减少低效文本搜索     |
| 使用 Git 状态、依赖清单、模块/契约文件和测试执行共同审计 | 单一证据不足以区分“骨架”“部分实现”和“完成” |
| 新增或修改行为遵循 RED→GREEN→REFACTOR                    | 为大规模续建提供可重复的回归证据           |
| 不对用户已有改动做重置、覆盖或无关清理                   | 保护共享工作区和用户成果                   |

## 遇到的问题

| 问题                                                                                       | 解决方案                                                                                  |
| ------------------------------------------------------------------------------------------ | ----------------------------------------------------------------------------------------- |
| 技能批量读取输出截断                                                                       | 将关键技能改为逐个完整读取                                                                |
| 探测 Codegraph 备用命令时 `cg` 不存在导致命令返回 1                                        | 已确认正式命令 `codegraph` 存在，后续只调用该命令                                         |
| 首次 `codegraph init .` 使用 1 秒命令超时，初始化被终止                                    | 先查看 `init --help`，再使用适合完整索引的超时或后台方式                                  |
| 快速全门禁在 Prettier 检查阶段失败                                                         | 先机械格式化 `scripts/trim-generated-api.mjs`，再重跑从失败点开始的门禁                   |
| 并行基础设施探测中有一个 PowerShell 子命令返回非零，导致其余结果未汇总                     | 拆分为容错的独立检查，不再把可能不存在的路径放在同一 Promise 失败域                       |
| 前后端覆盖率并行运行时前端门槛失败，使后端命令结果未返回                                   | 单独运行后端 `clean test` 并从 JaCoCo XML/CSV读取真实数据                                 |
| Adoptium API 跳转 GitHub 后连接超时                                                        | 改用微软官方 `aka.ms/download-jdk` 下载 OpenJDK 25 便携包，并记录本地 SHA-256             |
| 两个后端代理并发执行 Maven 导致共享 `backend/target` 被清理，Flyway 测试出现暂时性未解析类 | 串行化后端 Maven：先让 Flyway 切片独占 clean/IT，再执行安全契约测试                       |
| 复核前端切片时误以为 QueryClient 有独立测试文件                                            | 实际两个断言都位于 `shared/api/__tests__/http.test.ts`，按真实文件清单复核                |
| JDK 25 首次后端编译出现大量 Lombok 生成成员缺失                                            | 检查实际 Lombok、maven-compiler-plugin 与 JDK 25 注解处理兼容性，修复处理链而非手写访问器 |
| PowerShell 把未加引号的 Maven `-D...` 参数按点号/冒号错误拆分                              | 后续所有带点号或冒号的 Maven system property 参数整体加双引号                             |
| 更新进度时把属于 `progress.md` 的表格行误用为 `findings.md` 补丁上下文                     | 先定位真实行号，再分别更新对应文件                                                        |
| Docker Hub manifest 查询连接超时，DaoCloud Maven manifest 深查也超时                       | 已确认另外两个目标镜像；Maven 标签留待实际 Docker build，通过可配置镜像源验证             |
| 手工调用 DaoCloud registry token 时 PowerShell 将返回值拼成无效 Authorization 头           | 不继续自制 registry 协议，使用 Docker 客户端或实际构建验证                                |
| 代理门禁把 `location /api/file` 误识别为通用 `location /api/`                              | 用带 `{` 边界的正则精确定位 Nginx location 块                                             |
| 一组并行门禁被单个格式失败提前终止汇总                                                     | 修复格式后改用 `Promise.allSettled`，保证每项验证都有独立证据                             |
| Prettier 首次格式化命令误用 `../scripts`                                                   | 确认 npm `--prefix` 不改变这些参数的根目录解析，改用 `scripts/...`                        |
| CI job 解析器首次使用了 JavaScript 不支持的 `\\z` 正则锚点                                 | 换成按缩进逐行提取 `jobs:` 下 job，测试覆盖缺失 JDK 的作用域问题                          |
| Docker daemon 不可连接且本机无 Docker Desktop                                              | 保留 Testcontainers 测试代码，记录环境缺口；不伪造容器执行结果                            |
| Maven clean 无法删除被占用的 `backend/target`                                              | 不终止用户 PID 31012，改用非 clean 定向编译验证；稍后再尝试完整干净门禁                   |
| 定向测试被 bundle JaCoCo 基线误拦截                                                        | 让覆盖率执行显式尊重 `-Djacoco.skip=true`，完整门禁仍默认强制检查                         |
| Docker Compose JWT fixture 中的空格导致校验器误报                                          | 读取完整 YAML 标量而不是只匹配非空白 token                                                |

## 资源

- 原始计划：`C:\Users\86193\.codex\attachments\d062c1c0-b5cc-4fe4-a1ba-6e282e6b6801\pasted-text.txt`
- 项目根目录：`D:\AI Demo\菁选`

## 视觉/浏览器发现

- 尚未进入浏览器验证阶段。

---

_每执行2次查看/浏览器/搜索操作后更新此文件_
_防止视觉信息丢失_

# 2026-07-12：Task 4 决策与发现

- 旧注册服务只在成功插入后删除 Redis 验证码，两个并发注册请求都能先读取相同验证码；已改为 Lua `GET`/比较/`DEL` 原子消费。
- v1 注册验证码限流使用已存在的 `RateLimitService`，地址和可信 IP 均独立实施分钟/小时策略；Redis 不可用继续由 `V1ExceptionHandler` 映射为 `503 RATE_LIMIT_UNAVAILABLE`。
- 现有状态模型没有 `REJECTED`，拒绝待审核教师被锁定为 `DISABLED`，避免处于待审核状态的账号被重复审批；审批理由进入结构化审计目标字段。
- `user:approve` 使用方法级 `hasAuthority`，不以管理员角色或菜单可见性作为安全边界。
- 对拒绝决定，服务端强制提供非空原因；前端弹窗同样强制输入，避免仅依赖客户端约束。
- 基线菜单数据不存在 `user:approve`；已加入 V3 数据迁移，内置管理员将获得该独立权限。

# 2026-07-12：作品提交与文件存储纵切

- 待办的正确完成时机是作品通过附件等校验并提交审核之后；创建草稿即完成会将未提交作品错误计为完成。跨模块 workflow 因此固定为 `submitWork → completeTask`，并以事务保证第二步失败时回滚第一步。
- `FileStorage` 将本地磁盘细节隔离在 portfolio infrastructure：上传过程用 `DigestInputStream` 流式计算 SHA-256，先写目标目录临时文件、再原子移动；少数不支持 `ATOMIC_MOVE` 的挂载卷仅回退为同目录 `REPLACE_EXISTING` 移动。
- 旧上传 Controller 曾在文件落盘后才验证作品归属且 DB 插入异常时遗留文件。现在先完成归属校验，并在附件持久化失败时立即删除新写的相对路径；删除失败会保留可检索日志，后续需要以持久化事务事件实现提交后删除与重试。
- `work_attachment.sha256` 通过 Flyway V4 增量添加。空 MySQL Flyway 测试已同步期望版本和列，但本机 Docker daemon 不可用，尚未把它表述为已实跑。
- 管理员删除作品必须在事务提交后才触碰文件系统；若在事务内直接删除，随后数据库回滚会造成不可恢复的数据丢失。逐文件 `FileDeletionRequested` 事件可避免附件列表过长超过事件序列化载荷，并允许单个失败文件独立重试。
- 文件清理事件只从受控的本地 `/uploads/` URL 构造，拒绝外链、绝对路径、反斜线和 `..`。监听器向外抛失败，Spring Modulith 因而保留 publication；现有恢复任务会重提失败 publication，避免吞掉文件系统故障。
- `campaign.api` 虽按包名表达了对外契约，但 Spring Modulith 默认仅公开模块根包。为使 workflow 对 `CampaignTaskCompletion` 的依赖可验证，需在子包增加 `@NamedInterface("api")`；模块校验由此从 RED 转 GREEN。

# 2026-07-12：用户物理删除审计

- `SysUserServiceImpl.deleteUser` 已使用 `physicalDeleteById`，但此前没有影响清单或关联清理：`delete_request.student_id`、`student_task.user_id`、`sys_notification.user_id` 等仍可能留下孤儿数据。旧 `/admin/users/{id}` 仍返回 `Result`，并且没有 v1 删除预览/确认契约。
- 当前根管理员保护仅依赖 `roleId=ADMIN && username=admin`；这可保护默认根账号，但尚未形成“删除前影响清单、二次确认、按明确根账号策略保护”的完整目标用例。下一切片必须先建立 v1 应用用例和行为测试，不能在旧 Controller 上直接扩展删除。

# 2026-07-12：用户删除影响清单实施

- 用户删除将 `delete_request`、`student_task` 与 `sys_notification` 视为可随用户物理清理的从属记录；有任一记录时必须显式 `confirm=true`。`work.submitter` 与 `work_member` 是作品聚合关系，当前不能安全静默级联，故作为阻断影响返回 409，等待作品删除/归属工作流统一处理。
- 管理前端不再直接调用旧 `/admin/users/{id}` 删除，而是先使用 Orval 生成的 `/api/v1/users/{id}/deletion-impact`，随后调用带确认参数的 v1 DELETE。生成 DTO 把 `references` 标记为可选，页面需要按空数组降级处理。
- 新增 operation 后，OpenAPI 门禁会拒绝所有未登记成功状态的路径；必须同步扩展 `EXPECTED_SUCCESS_STATUSES`，否则即便 Springdoc 输出了 200/204，契约仍会正确判为未分类。

# 2026-07-12：管理员作品物理删除实施

- 旧 `BaseEntity` 的全局 `@TableLogic` 使 MyBatis-Plus `delete(...)` 只更新 `deleted`，即使管理员删除流程枚举了关联表也不会满足物理删除目标。过渡期为每个 work 从属表提供显式 `DELETE FROM … WHERE work_id = ?` Mapper 方法，作品根最后物理删除。
- 删除申请同样引用作品，因此纳入第十类关联记录；学生待办不是从属历史数据，而是按现有业务约定回到待处理状态。当前 Schema 没有目标外键，后续数据模型迁移应将这些显式语句收敛为受约束的 `ON DELETE CASCADE`。

# 2026-07-12：公开评论与互动审计

- `CommentService.getWorkCommentsWithUserInfo` 只对顶级评论分页，但会读取该作品所有非顶级回复后在内存构建树；作品回复量增长时查询和内存都不受顶级页大小约束，未达到“评论不再加载全部回复”的性能目标。
- 公开作品页仍调用 legacy `/comment/*`，而 v1 仅提供创建/删除评论，没有公开评论分页读取接口。下一切片应定义 v1 `items + pageInfo` 评论根分页与按父评论懒加载回复接口，并将公开页面迁移到 Orval 生成客户端。

# 2026-07-12：当前 HEAD 重新基线

- 会话追赶后工作区原本干净，当前分支 `refactor/v2`，HEAD `9e33882 feat: 阶段 6 — referencedata/communication DTO 内迁`；磁盘计划停留在阶段 4，不能代表真实 HEAD。
- Codegraph 索引最新：553 个文件、10,155 个节点、20,210 条边，其中 Java 371、TypeScript 107、Vue 41。
- 旧 `v2-completion-audit.md` 仍把 Playwright、Lighthouse、迁移 CLI、监控和生产发布描述为缺失，但当前仓库已经出现 `frontend/e2e`、`lighthouserc.json`、MSW、`scripts/migrate.mjs`、`monitoring/` 等实现，必须逐项验证后重算。
- 同一文件清单仍存在根 `sql/`、`backend/sql/`、`backend/src/main/resources/legacy-sql/` 三套 SQL，以及四套旧 Layout、旧 `api/` 和旧视图目录；阶段 6 不能仅依据提交标题判定完成。
- 当前完成度必须区分“文件/骨架存在”“静态门禁存在”“本地实际运行通过”“正式环境验证通过”四个证据等级。
- 当前 `backend/pom.xml` 实际声明 Java 21、compiler 3.11.0，CI 的 API/后端 job 也配置 JDK 21；这与历史进度中“JDK 25 已闭环”的记录冲突，当前 HEAD 必须判定为回退或未合入。
- 根 `verify` 当前没有 `api:check`、覆盖率、Playwright、Lighthouse、bundle budget、SBOM 或镜像验证；前端 package 也没有 e2e/lighthouse 脚本。对应文件存在不等于 CI 门禁已接线。
- `.github/workflows` 当前只有 `ci.yml`，且仅在 master push/PR 运行基础契约、前端、后端、安全与依赖审查；没有 release/nightly/性能/自动生产部署工作流。
- 当前依赖版本已到 Boot 4.1.0、MyBatis-Plus 3.5.17、Modulith 2.1.0、Vue 3.5.34、Vite 8.0.12、TypeScript 6.0.2、Element Plus 2.14.0、Pinia 3.0.4，但工具链一致性仍未完成。
- 当前 HEAD 的 `npm run verify:quick` 实跑在 `format:check` 失败，9 个已提交文件不符合 Prettier；工具链和安全配置已通过，但该统一命令未触达前端/后端测试，不能宣称当前提交全绿。
- `npm run verify:frontend` 在 ESLint 首步失败：`frontend/src/views/public/WorkList.vue` 的 `PublicClassItem` 与 `TagItem` 未使用，因此类型、Vitest、构建未被统一命令触达。
- 非 clean `mvn test` 运行 378 项后出现 59 失败、39 错误；大量错误引用当前源码不存在的方法/枚举/构造器，且 Maven 报告 Nothing to compile，证明 `backend/target` 存在历史编译污染。必须以 `clean test` 重新判定当前 HEAD。
- `mvn clean test` 已排除缓存污染并在 367 个生产源码编译阶段失败：`PrizeQueryService` 缺 `RankService`、`LeaderboardQueryService` 缺 `RankVO`，文件删除监听和事件恢复代码使用 `org.springframework.modulith.events` 类型但 POM 未提供对应依赖，共 12 个编译错误。当前 HEAD 无法重建后端 JAR、OpenAPI 或测试基线。
- 前端 `vue-tsc` 因 `WorkList.vue` 两个未使用类型失败；`vitest --coverage` 共 78 项、21 失败、3 个未处理网络错误。新 `shared/api/http` 测试要求同源 `/api/v1`、禁止 options 覆盖和抛出 `ApiProblemError`，实际运行仍进入旧 `src/api/request.ts` 的 `Result` 逻辑。
- 待办、日志、公告、仪表盘等目标测试 mock 新 v1 适配器，但页面仍走旧 API/真实网络；这是“测试先提交、生产迁移未闭环”的直接证据。
- 当前失败运行的前端覆盖率为 Statements 25.05%、Branches 26.13%、Functions 17.92%、Lines 26.23%；因测试失败只能作为规模参考，不能作为发布覆盖率。
- Git 对象库中存在未被分支引用的 WIP merge commit `d7ff9ed`（基于 `048c260`），包含此前日志描述的大量 POM/CI/认证/事件/物理删除/前端旧 API 清理等实现；当前 HEAD 的三个阶段 6 提交只恢复了其中一部分，导致测试与生产代码错位。新版方案应先把该 WIP 作为只读恢复来源逐文件评估，而不是全部重写或直接整体合并。
- 当前仓库只有本地 `refactor/v2` 分支，远端为 `https://github.com/wswhhhc/jingxuan-.git`；不可达 WIP 尚未受分支/标签保护，后续实施前应先创建安全引用，避免 Git GC 丢失可恢复成果。
- 已创建本地只读恢复分支 `codex/recovery-wip-d7ff9ed` 指向该 WIP；当前工作分支仍为 `refactor/v2`，工作树未因保护动作改变。
- 后端抽查确认 WIP 是 Codegraph 陈旧索引的来源，确实含 Cookie refresh、challenge、logout-all、JDK25、Modulith JDBC、物理删除、文件事件与 CI 配置等可复用片段；但它基于旧 `048c260` 且自身 `WorkServiceImpl` 存在缺方法声明的裸语句块，无法整体编译。恢复策略必须是逐文件/逐 hunk 取证 + 当前 HEAD 测试驱动重做，禁止整体 cherry-pick/reset。
- HEAD 静态规模：367 个后端主源码、100 个后端测试文件、41 个 Vue 文件、16 个前端单测文件、1 个 E2E 文件、4 个 Flyway migration、1 个 GitHub workflow。
- 遗留规模仍大：10 个旧 adapter、3 个根 controller、25 个根 entity、25 个根 mapper、6 个根 service，49 个生产 Java 文件仍使用 `Result<`；目标模块内仍有 7 个文件直接 import 根 Mapper。
- 前端消费比例明显未迁完：50 个文件导入旧 `@/api/`，只有 5 个导入 `@/api/v1/`、3 个引用生成客户端；`frontend/src/features/` 为 0 个文件，四套旧 Layout 全部保留。
- SQL 仍有三套：根 `sql/` 15 个、`backend/sql/` 7 个、classpath `legacy-sql/` 14 个；Flyway 不是唯一 Schema 来源。
- 页面拆分目标严重未达：`WorkDetail.vue` 1273 行、教师评分 1189 行、用户管理 740 行、批次 721 行、审核 647 行、作品提交 566 行等至少 12 个页面超过 300 行。
- OpenAPI 静态语义测试 22 项仅 7 通过、15 失败：已提交 YAML 缺注册/邮件验证码操作，并未满足 Cookie refresh、锁定成功状态、Bearer/401/403/422、雪花 ID string 等目标语义。
- 冒烟契约测试检测到当前 POM 继续从 `../sql` 打包资源、Compose 仍使用默认数据库密码/旧 SQL 初始化/旧镜像与端口暴露，CI 也缺 `legacy-runtime-smoke`；历史安全加固未落入 HEAD。
- `scripts/check-legacy-removed.mjs` 在根 ESM 项目中调用 CommonJS `require()`，Node 24 直接报错，遗留清零门禁自身不可执行。
- 官方 Spring Boot 4.1.0 系统要求确认支持 Java 17 至 26，JDK 25 无兼容性障碍：<https://docs.spring.io/spring-boot/system-requirements.html>。
- Maven Central 官方元数据在 2026-07-08 显示 MyBatis-Plus Boot4 最新正式版仍为 3.5.17：<https://repo1.maven.org/maven2/com/baomidou/mybatis-plus-spring-boot4-starter/maven-metadata.xml>。
- Spring Modulith 官方事件文档要求持久化 JDBC publication registry 使用 `spring-modulith-starter-jdbc`，并提供 `@ApplicationModuleListener`；当前 POM 只有 core，与编译错误一致：<https://docs.spring.io/spring-modulith/reference/events.html>。2.1.0 仍是最新正式版。
- Node 官方发布索引显示 Node 24 最新 LTS 为 24.18.0（Krypton，npm 11.16.0）：<https://nodejs.org/dist/index.json>。Vite 8 的 Node 要求为 20.19+ 或 22.12+，Node 24 满足：<https://vite.dev/guide/>。
- typescript-eslint 官方当前支持 TypeScript `>=4.8.4 <6.1.0`，因此 TypeScript 6.0.2 已进入正式支持范围，无需继续等待 TypeScript 7：<https://typescript-eslint.io/users/dependency-versions/>。
- 后端按 v2 验收口径保守完成度约 37%：27 个 v2 Controller/约 101 个操作和九模块资产已形成，但目标模块仍有约 128 条旧包 import，当前 HEAD 不可编译，认证/数据库/事件/RBAC/REST/性能均未闭环。
- 前端按 v2 验收口径完成度约 29%：工具链、Orval、部分安全/预览基础存在，但 31 个页面中约 30 个仍走旧角色 API，Vue Query hooks 消费为 0，WorkspaceShell 未使用，session 仍落 Web Storage。
- 基础设施审计保守完成度：阶段 0 约 58%、阶段 1 约 62%、阶段 7 约 25%、阶段 8 约 12%；这些百分比只代表资产存在度，当前绿色可发布状态更低。
- 本地 `refactor/v2` 是与远端 `master` 不共享对象的新根历史，且 v2 从未推送；GitHub 无 production environment、Actions secrets/variables、远端 v2 分支和分支保护。当前 `git fetch origin master` 又因连接重置失败，本机尚无远端 ref 可安全整合。
- 本机唯一 known_host 与历史 SSH 记录指向一个已脱敏的高概率候选主机，但历史命令没有本项目路径上下文，因此它不是已核实生产主机；SSH BatchMode 与 HTTP/health 均超时，目前无法核验 PM2、Nginx、数据库或 Flyway。
- Docker/部署当前不可用：Compose 带默认密码、旧 SQL 挂载、错误服务环境、后端 Java17/前端 Node20、Nginx 仍剥离 `/api`；Docker daemon 未运行，中文路径下 Compose 还缺显式 project name。
- `scripts/migrate.mjs` 目前只输出模拟 SQL、不连接数据库，缺 `build-sanitized-rollback`/`purge`、真实文件迁移与 SHA-256 校验，并可能输出含密码 URI；不能作为生产迁移 CLI。
- `docs/ops/production-switch.md` 声称迁移 CLI 能构建脱敏回滚库，但代码没有该能力；文档同时要求完整 v1 备份，可能违反“不备份软删/测试/废弃数据”，且示例直写含密码 URI。CLI、回滚语义和手册必须先统一为可测试契约。
- 当前 bundle 工具可对旧 dist 计算出 87.64KB/205.98KB/12.92KB，但当前构建失败且脚本未接 package/CI，所以不能算发布预算通过。Lighthouse 全为 warn，k6/数据集无运行报告，Prometheus/Grafana 未接部署 profile。
- 当前质量元门禁实跑：toolchain 0/1、coverage 0/3、smoke 1/23；生成客户端与 bundle 计算器自身单测各 3/3，但未被根 verify/CI 接线。前端生产依赖审计为 0 个漏洞。
- Flyway 存在 Java `V1__Baseline`，会执行 14 个 legacy SQL，因此不能称为“完全无 V1”；问题是 POM 仍从根目录打包重复 Schema，资源契约测试与实际文件冲突，版本测试仍断言 V4 而仓库已有 V5，空库/升级路径没有当前绿色证据。
- 新方案的无上下文对抗审查发现 8 个阻断，均判定为 valid + actionable：purge 过早、写屏障不足、回滚产物过晚、迁移校验过弱、Schema 横向切断旧消费者、审核事务冲突、发布缺并发/摘要绑定、生产可信输入缺失。方案已逐项修正。
- 按用户“尽量减少人为干预”的既定偏好，本轮不调用外部 Gemini/Codex CLI 做跨模型复核；继续采用内部 fresh-context 审查结果。
