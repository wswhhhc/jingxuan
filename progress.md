# 进度日志

## 会话：2026-07-12（重新基线与自动实施授权）

### 阶段 R：重新基线审计与新方案（进行中）

- 通过逐问式访谈确认最终范围：本地与正式环境双重审计、生成新版方案、自动门禁通过后连续实施。
- 用户明确取消方案实施和 GitHub production 的人工审批，授权实际执行 PM2 + Nginx 正式迁移、部署与最终清理；Docker Compose 仍需保持可用。
- 完整恢复 `task_plan.md`、`findings.md`、`progress.md`；会话追赶后确认工作区干净，当前分支为 `refactor/v2`，HEAD 为 `9e33882`。
- 发现磁盘计划落后于最近阶段 6 DTO 内迁提交，当前必须重新按源码和运行证据计算完成度，不能沿用旧百分比。
- Codegraph 索引确认最新（553 文件、10,155 节点、20,210 边）；三路只读审计已并行覆盖后端、前端和工程/生产。
- 首轮文件与旧审计对比发现：Playwright/Lighthouse/MSW/迁移/监控能力已新增，但重复 SQL、旧布局和遗留 API 仍存在，旧审计结论已失效。
- 核对当前 POM/CI/根脚本后确认历史工具链闭环未落在 HEAD：Java/CI 仍为 21，统一 `verify` 未串联 API、覆盖率、E2E、Lighthouse、SBOM 或发布门禁。
- 首次运行当前 `npm run verify:quick`：工具链与安全配置通过，随后 9 个文件因 Prettier 漂移失败，前后端测试未由该命令执行；审计期保留失败基线，不立即机械修复。
- 分项验证：前端 ESLint 因 `WorkList.vue` 两个未使用类型失败；后端非 clean 测试 378 项中 59 失败/39 错误，并暴露 `target` 历史产物污染，下一步用 clean test 重建可靠基线。
- 后端 clean 基线完成：367 个生产源码编译即因 evaluation 类型迁移不完整和缺 Spring Modulith events 依赖产生 12 个错误；确认当前 HEAD 无法构建，不再采用污染的非 clean 测试数作为完成证据。
- 前端分项基线完成：typecheck 失败；Vitest coverage 78 项中 21 失败并有 3 个未处理网络错误，目标 HTTP/页面测试与旧生产实现不一致；失败覆盖率约 25.05/26.13/17.92/26.23。
- Git 恢复审计发现不可达 WIP `d7ff9ed`，其中包含当前 HEAD 缺失的大量已实现切片；已要求前后端审计只读比对，后续方案优先安全保护并选择性恢复，禁止直接 reset/整体覆盖。
- 按 Git 安全工作流创建 `codex/recovery-wip-d7ff9ed` 本地恢复引用，未切换当前分支、未改业务文件，消除不可达对象被回收风险。
- WIP 抽查结论：包含大量可复用实现，但自身也不可编译且落后当前 HEAD；已锁定为逐 hunk 恢复源，不作为可直接合并的完成版本。
- 首次 HEAD 指标汇总因 PowerShell 嵌套正则引号解析失败；已记录并改用固定字符串/分项统计方案，不重复原命令。
- HEAD 静态指标成功汇总：后端遗留根层/Result/Mapper 仍大量存在，前端旧 API 导入 50 对 v1 适配 5、生成客户端 3，features 为空，三套 SQL 与多页超 300 行均未清零。
- 静态质量脚本实跑：OpenAPI 7/22，冒烟契约暴露 POM/Compose/CI 回退，遗留扫描因 ESM/require 直接崩溃；门禁资产存在但未形成绿色闭环。
- 按官方来源核对锁定版本：Boot 4.1 支持 JDK25、MP 3.5.17 与 Modulith 2.1.0 仍为最新正式版、TS 6 已获 typescript-eslint 支持；新版方案保留主版本并修正当前回退/依赖接线。
- 三路审计已给出保守子系统结论：后端约 37%、前端约 29%，基础设施阶段0/1/7/8约58/62/25/12；总体仍需按统一阶段权重计算。
- 正式服务器与 GitHub 生产配置当前不可连接/未配置；迁移 CLI、Compose、Nginx、Docker、监控与 Release 流程存在生产阻断。
- 尝试获取 `origin/master` 时 GitHub 连接被重置，未创建远端 ref；已记录一次失败，不重复同一命令。
- 补跑质量元门禁：toolchain 0/1、coverage 0/3、smoke 1/23；生成/API 与 bundle 计算器测试 3/3，前端生产依赖 audit 为 0 漏洞。
- 完成统一阶段加权，当前总体完成度为 31%（±3%）；已重写 `docs/refactor/v2-completion-audit.md` 并新增完整执行方案 `docs/refactor/v2-execution-plan-2026-07-12.md`。
- 新方案正在接受无上下文 adversarial review，发现闭环后即进入阶段 A 自动实施，不等待人工审批。
- 对抗审查返回 8 个阻断并全部修正：迁移采用 expand/contract；审核移出事务；写屏障/回滚/数据不变量加强；Release 增加并发锁和 release manifest；purge 延后到 24 小时观察与完整回归后。
- 新版执行方案已审查收口，共 59 个任务；下一步进入阶段 A，先提交审计/方案 save point，再恢复本地绿色基线。
- 已创建中文 docs save point `af702da`；发现 cached diff check 未 fail-fast 且提交含 Markdown 行尾空格，当前正以独立格式提交修复，并强化后续提交链退出码检查。
- **状态：** in_progress

## 会话：2026-07-12

### 阶段 3：Task 4 注册与教师审批（进行中）

- 新增 v1 邮箱验证码接口：按规范化邮箱和可信客户端 IP 分别执行 1 次/分钟、5 次/小时 Redis 固定窗口限流；超限返回带 `Retry-After` 的 RFC Problem Details。
- 新增 v1 注册接口，输入 DTO 在边界完成校验，返回 201 和字符串 ID 的 `V1UserInfo`；教师沿用既有 `PENDING_APPROVAL` 状态。
- 将邮箱验证码校验改为 Redis Lua 比较并删除，避免并发下同一验证码重复完成注册。
- 新增教师审批应用用例与 v1 端点，强制 `user:approve` 权限码；批准启用账号，拒绝标记禁用并写入审计动作（含拒绝理由）。
- 新增 Flyway V3，将 `user:approve` 分配给内置管理员角色，避免端点因权限数据缺失而不可用。
- 验证：后端定向 11 项通过；前端 ESLint、类型检查与生产构建通过；OpenAPI 运行时导出、YAML 与 Orval 生成客户端一致性通过。
- **状态：** complete（真实 Redis/MySQL Testcontainers 验证仍受本机 Docker daemon 不可用限制）。

### 阶段 3：Task 5 会话收口（进行中）

- Access Token 已迁移至前端运行时内存，应用启动通过 refresh Cookie 恢复会话；登录、注销切换至 v1 认证接口。
- 新增 `POST /api/v1/auth/logout-all`，由 Redis refresh family 用户索引撤销当前账号所有刷新会话，并清除 Cookie、拉黑当前 Access JWT。
- 定向测试：`RedisRefreshTokenServiceUnitTest` 与 `V1AuthControllerTest` 通过；后续同步 OpenAPI/Orval 并完成完整前端构建复验。
- OpenAPI 运行时导出、YAML、Orval 生成客户端与语义门禁通过；前端生产构建通过。
- **状态：** complete。

### 阶段 4：批次、待办与作品（进行中）

- 待办不再在创建草稿时完成：学生提交审核时，`TaskWorkSubmissionWorkflow` 在单一事务中先提交作品、再完成待办；任何任务完成失败都会回滚作品状态。workflow 仅依赖 campaign 对外 API，不跨模块注入 Mapper，4 项定向测试通过。
- v1 创建作品和提交审核契约已拆分 `taskId` 责任；学生作品提交页创建草稿使用生成 v1 API，仅在提交阶段带上待办 ID。OpenAPI/Orval、前端类型检查与 ESLint 均已复验。
- 新增 `FileStorage` 接口及本地实现：流式 SHA-256、同目录临时文件、原子移动与不支持原子移动时的安全回退；`/api/file/upload` 已接入该边界。附件持久化失败时删除刚写入的文件，避免孤儿；Flyway V4 记录附件 SHA-256。
- 管理员删除作品会在附件记录删除前提取受控 `/uploads/` 相对路径，并在同一业务事务内发布逐文件的持久化事件。`FileDeletionListener` 仅在事务提交后执行删除；存储故障会使事件保留为失败状态，并由既有单实例恢复任务重试。
- 验证：`TaskWorkSubmissionWorkflowTest` 4/4、`LocalFileStorageTest` 与 `FileUploadControllerTest` 2/2 通过；OpenAPI 与生成客户端一致性检查通过。Flyway 空库迁移测试已更新为 V4 断言，仍等待 Docker daemon 可用后真实执行。
- 验证：文件删除监听器、管理员删除关联事件与既有作品服务共 18 项定向测试通过；真实 MySQL 上的持久化发布/失败重放集成验证仍待 Docker daemon。
- Modulith 校验首次发现 workflow 正在使用但未公开的 `campaign.api`；补充 `@NamedInterface("api")` 后，模块校验连同上述测试共 19 项通过。

### 阶段 4：用户物理删除影响清单（进行中）

- 新增 v1 用户删除影响预览与确认删除：默认根管理员不可删除；存在待办、删除申请或通知时必须先确认，并在物理删除用户前物理清理这些关联记录。
- 对作品提交人与成员关系不做静默级联删除，而是标记为阻断影响，要求管理员先处理作品归属或作品删除，避免用户删除意外破坏作品聚合。
- 管理端删除弹窗改为先读取生成的 v1 impact API，展示会清理的数据或阻断原因，再发出确认删除。
- OpenAPI/Orval 已同步至 32 条 v1 路径、51 个生成文件与 37 个 v1 URL；后端服务/控制器/模块测试 7 项通过，前端类型检查、ESLint 和相关 Vitest 61 项通过。

### 阶段 4：管理员作品物理删除（进行中）

- 管理员删除作品不再调用带 `@TableLogic` 的通用 Mapper 删除，而是对作品、附件、审核、评论、点赞、成员、发布、评分、标签、奖品发放和删除申请执行显式物理删除；学生待办仍重置为待处理。
- 文件删除事件继续在数据库事务提交后执行，避免事务回滚导致磁盘文件提前丢失；当前旧表尚未重建为外键 `CASCADE`，显式删除语句是过渡实现，目标 Schema 迁移仍需收敛外键语义。
- 验证：作品服务与用户删除服务定向 20 项通过，包含十类关联物理清理和文件事件发布断言。

## 会话：2026-07-11

### 阶段 1：仓库基线与计划差距审计

- **状态：** complete
- **开始时间：** 2026-07-11
- 执行的操作：
  - 完整读取用户提供的 v2 全量重构计划。
  - 读取文件规划、增量实现、测试驱动、Git 工作流和代码审查技能说明。
  - 确认仓库位于 `refactor/v2`，初始工作区干净，并查看最近五条提交。
  - 创建持久化计划、发现和进度文件。
  - 并行启动后端与前端差距只读审计。
  - 盘点根目录、依赖版本、统一验证脚本、重构文档、迁移入口与最近 40 条 Git 提交。
  - 确认 Codegraph CLI 可用，但仓库索引当前未初始化。
  - 完成 Codegraph 本地索引初始化并确认索引为最新。
  - 使用 Codegraph 盘点后端 243 个主源码文件、53 个测试文件以及前端 278 个源码文件和 10 个测试文件。
  - 盘点本机工具链、Git 跟踪的 SQL/配置、CI 工作流和部署配置，确认多项最终态缺口。
  - 汇总后端、前端和工程部署审计，建立 `docs/refactor/v2-completion-audit.md`。
- 创建/修改的文件：
  - `task_plan.md`
  - `findings.md`
  - `progress.md`
  - `scripts/trim-generated-api.mjs`（仅 Prettier 机械格式化）
  - `docs/refactor/v2-completion-audit.md`

### 阶段 2：基础工程、契约与质量门禁补齐

- **状态：** complete
- 执行的操作：
  - 修复根格式门禁漂移。
  - 重新生成缺失的 v1 评论、点赞和评分客户端。
  - 在仓库外准备并验证 OpenJDK 25.0.3 LTS 便携运行时。
  - 修复 v2 生成客户端同源基址并注册 Vue Query Provider；新增 2 个前端回归测试。
  - 为工具链声明与 Java 版本解析新增 3 个 Node 测试，并确认 POM 21 会触发 RED。
  - 将 POM、CI 和 Docker 工具链切换到 JDK 25 / Node 24，并在本地 JDK 25 下通过根工具链校验。
  - 为 Vite 与两套 Nginx 增加 `/api/v1` 保留前缀的优先代理，并加入 2 个配置回归测试。
  - 恢复并复核 `task_plan.md`、`progress.md`、`findings.md`，运行会话追赶脚本，确认无未同步上下文。
  - 重新同步 Codegraph 索引，纳入本轮 2 个脚本格式变更。
  - 仅对 `verify-security-baseline.mjs` 与 `verify-toolchain.mjs` 执行 Prettier 机械格式化。
  - 在 JDK 25 环境下复验工具链、安全配置与根格式门禁，全部通过。
  - 完整运行 `api:check`，确认 25 个 v1 路径的 YAML/运行时契约与 40 个生成客户端文件一致。
  - 完成基础门禁只读复核，确认 bundle 预算集成正确，并识别 CI job 级 JDK、根统一验证和前端格式三项收口缺口。
  - 先新增 job 级 JDK 与统一质量脚本测试，确认当前配置分别以缺 JDK、缺 `api:check`、缺前端格式链路进入 RED。
  - 为 `frontend-quality` 配置 JDK 25；将 `api:check` 纳入根 `verify`；将前端格式检查纳入根门禁，并用 `.prettierignore` 明确排除 Orval 生成目录。
  - 格式化唯一不合规的手写前端测试文件，新增门禁测试与实际配置校验全部转绿。
  - 运行完整前端验证：ESLint、类型检查、59 个 Vitest、生产构建与 bundle 预算全部通过。
  - 串行运行 JDK 25 后端单元测试，185/185 通过且 JaCoCo 正常生成。
  - 完成身份会话 Task 1 只读调用链审计，确认现有实现仅覆盖随机 token、哈希存储、单次消费与时长选择，family/重放撤销/原子轮换仍未实现。
  - 为 Refresh family 先写领域与 Redis 集成失败测试；领域测试首次因类型缺失 RED，随后实现绝对生命周期状态机与 Lua 原子轮换适配器。
  - 依据 Spring Data Redis 4.1 与 Redis 7 官方文档，使用复用 `RedisScript`、显式 `KEYS` 和 `PEXPIREAT` 实现 issue/rotate/revoke。
  - 定向编译确认 246 个生产源码和 58 个测试源码可编译，领域测试 4/4 通过；当前仅被定向运行不应触发的 bundle 覆盖率门禁拦截。
  - 探测 Docker 运行环境，确认本机无可用 daemon，暂不能执行 Testcontainers 或镜像构建。
  - 新增阶段 1 Modulith/ArchUnit 渐进门禁：精确发现九模块、校验循环依赖，并冻结 Controller 旧桥接与跨模块 Mapper 白名单；定向 4/4 通过。
  - 当时首次新增前后端覆盖率不下降门禁：后端曾锁 Line 63.30% / Method 68.10% / Branch 44.20%，前端 Statements 22.78% / Branches 23.73% / Functions 16.95% / Lines 23.85%；后端旧门槛后续经 detached HEAD 复现实锤为历史 exec 污染，已由 41.00%/40.90%/32.70% 可靠门槛替代。
  - 完成阶段 0 Testcontainers/冒烟只读审计：容器化集成接线基本完成，共 126 个 integration 测试；旧冒烟脚本仍会假绿、凭据错误并污染数据。
  - 为 Docker Compose 先写失败配置测试，确认旧镜像、localhost、缺 JWT 和旧 SQL 挂载被捕获；随后切换固定 MySQL/Redis、容器服务名与 Flyway 独占建表，9/9 配置测试通过。
  - 续接会话后重新完整读取原始重构计划、审计、计划、发现与进度文件，确认仍处于阶段 2；保留两个正在实施的冒烟与持久化事件切片，并并行启动身份 Task 2–5 的只读差距审计。
  - 复核安全冒烟切片：离线契约测试 7/7 通过，`bash -n` 通过；随后启动独立只读审查，重点检查假绿、退出清理与响应内容泄露。
  - 对照 Spring Modulith 2.1.0 官方文档、GitHub 标签源码与 starter POM，核对 JDBC 依赖、MySQL v2 DDL、完成/失败/重提语义及自动异步配置；发现新增 `@EnableAsync` 配置与 starter 自动配置重复。
  - 为持久化事件启动路径新增 3 个 RED 契约：API 集成测试必须由 Flyway 预建表、H2 OpenAPI 必须排除事件仓储自动配置、starter 异步配置不得重复；随后启用 BaseApiTest Flyway、删除 test-schema、移除冗余配置并修正 OpenAPI profile，6/6 转绿。
  - 增强 Testcontainers 事件集成测试源码：新增监听线程、真实事务激活、成功提交和失败回滚探针；JDK 25 干净编译 246 个生产源、60 个测试源通过。
  - 实际运行 H2 OpenAPI 导出：先后捕获 Refresh 适配器构造器选择错误及只排除 JDBC 自动配置仍触发 staleness bean 的问题；分别修复后导出成功。
  - 完成单实例事务事件恢复：17 个定向测试由 RED 转 GREEN；新增陈旧状态、上下界、真实重试冷却、统一开关、日志脱敏和多实例协调约束。
  - 完成 Docker 自包含与 CI runtime smoke：14 个 V1 SQL 原样迁入 backend classpath，删除两套重复 SQL；静态契约 11/11、配置 9/9、格式与 diff 检查通过。
  - 主代理复核 14 个新资源与 HEAD 原文件 Git blob 全部一致；首次 Java 资源契约受 stale target 中旧 test_data 影响失败，改为检查源码资源不存在后 18/18 事件/资源定向测试通过。
  - 续接后重新读取 `task_plan.md`、`progress.md`、`findings.md`、完成度审计与原始 v2 计划，确认阶段 2 状态和外部验收边界未变化。
  - 创建持续目标，锁定“完成原计划全部仓库内可实现工作并提供验证证据”；读取身份规范，确认 Task 2–5 是阶段 3 的后续实施序列。
  - 使用 Codegraph 定位身份契约与前端会话现状，确认 JSON refresh token 和 Web Storage access token 仍在，形成 Task 2→Task 5 的契约优先实施依据。
  - 预读身份 HTTP 与 Security 配置，确认 CSRF/CORS、BCrypt cost 10 和旧存储型 Bearer 注入仍需在身份阶段加固；首次猜错 SecurityConfig 路径已记录并改由 Codegraph 定位。
  - 复核现有认证 Controller/API 测试，确认测试本身仍固化 JSON refresh 契约且 family 重放断言不完整，为下一切片的测试先行改造建立基线。
  - 根据独立审查先写 2 个敏感默认值 RED 测试，确认校验器缺失后实现并转绿；移除 PowerShell/runtime demo 中的数据库默认密码，避免 mysql argv 泄露，11/11 Node 测试、实际安全基线和 PowerShell 语法检查通过。
  - 全仓复查确认当前工作区不再包含已暴露的数据库密码；同时记录旧用户创建/AI 导入的 `123456` 弱默认密码，留待身份闭环按测试驱动移除。
  - 为 Flyway 生产基线默认管理员先写失败门禁，确认会内置可登录账号后删除 `admin/admin123` seed；12/12 安全配置测试与实际门禁通过，并为可用 Docker 环境补充迁移后用户数为 0 的集成断言。
  - 独立五轴审查发现 OpenAPI Problem Details/ID 精度、`/jingxuan/` 静态服务、Compose 端口暴露、Docker context、Axios absolute URL 与孤儿测试 SQL 等阶段 2 假绿；分别分派 OpenAPI 和部署切片，并由主线程删除两个孤儿 H2 SQL、补资源回归断言。
  - 独立审查 Refresh family 领域/Lua 实现并复跑 JDK 25 无容器测试，`RefreshFamilyTest` 7/7、`RedisRefreshTokenServiceUnitTest` 12/12，共 19/19 通过；真实 Redis 8 个测试仍待 Docker。
  - 使用 Codegraph 追踪管理员创建、批量创建与 AI 导入的弱默认密码链路，确定“显式初始密码 + AI 不接触密码 + BCrypt 12”的后续安全切片，避免简单替换成另一个全局默认值。
  - 完成 v2 Axios 同源边界 TDD：旧实现 12 个恶意 URL/options 用例进入 RED，修复后定向 Vitest 16/16、前端 typecheck 与 lint 通过；生成客户端配置无法再覆盖请求身份或跨源携带 Bearer。
  - 收拢 OpenAPI、初始密码与 `/jingxuan/`/Compose 三个并行切片，冻结写入后由主线程开始测试优先的五轴审查。
  - 主线程并行复验非 Maven 门禁：OpenAPI 语义 10/10、部署安全 21/21、初始密码/同源边界相关前端 20/20、Vue 类型检查及完整 `verify:config`（安全 21/21 + smoke 23/23）全部通过。
  - 审查识别 OpenAPI 定制器会删除既有 404/409 等显式业务错误响应；已作为阶段 2 阻断项进入测试驱动修复，不以当前静态全绿冒充契约完整。
  - 为 OpenAPI 错误状态保留、公开认证 401、复数雪花 ID 数组和 ProblemDetails 字段结构新增 RED；Node 门禁初跑 3 项按预期失败，Java 定向测试证明 409 被删除。
  - 修复 customizer 后 Node OpenAPI 语义门禁 13/13、JDK 25 `OpenApiConfigTest` 3/3 通过；显式 404/409 等状态现保留描述并统一替换为 ProblemDetails，`attachmentIds` 保持数组。
  - 依据 Orval 8.20.0 官方 custom Axios 文档，在 mutator 导出 `ErrorType`/`BodyType` 并新增生成客户端错误包装门禁；Node 3/3、HTTP 定向 Vitest 16/16 通过，待最终 OpenAPI 重生验证生成泛型。
  - 分派 v1 路由级 404/405 ProblemDetails、全部 operation 201/204 成功状态、部署硬化和密码/AI 反例修复四个互不重叠切片；后端 Maven继续由主线程串行。
  - 阶段 2 收口定向后端测试 52/52 通过；随后真实 Springdoc 导出成功启动并停止，但契约写入门禁因 `ProblemDetails` 缺少显式 `type: object` 失败，确认生成产物仍未闭环且门禁有效。
  - 为 OpenAPI 3.1 序列化新增精确 RED 测试，证明类级 `@Schema(type = "object")` 仍会丢失顶层类型；在最终 `components.schemas.ProblemDetails` 上规范化后 4/4 转绿。
  - 真实更新 OpenAPI YAML 并重新生成 Orval 客户端；完整 `api:check` 17/17 语义测试、25 个 v1 路径、41 个生成文件与 30 个 v1 URL 全部一致。
  - 复核最终产物：`attachmentIds` 为 `string[]`，ProblemDetails 明确 `type: object`，10 个控制器客户端使用 `ErrorType<ProblemDetails>`；201/204 与登录/刷新 401 由成功状态和认证语义门禁锁定。
  - 用 detached HEAD 复现旧 JaCoCo 临时 exec 追加与 class mismatch，证明原 63% 门槛失真；覆盖率门禁测试先 RED，随后锁定 clean/append=false 与当前可靠 41.00/40.90/32.70 基线并 3/3 转绿，最终目标仍为 80/80/70。
  - 同步纠正 `baseline.md`、`implementation-log.md`、`v2-completion-audit.md` 的覆盖率口径，旧数值仅作为污染历史保留；文档格式与 diff 检查通过。
  - 初始密码/AI 加固首次独立复审发现仍有两类阻断：无赋值符号的常见凭据写法可绕过；100 条同步 BCrypt 可能超过两套 Nginx 60 秒读取超时。结构化模型字段还会误伤密码学/密码安全等正常术语，切片尚未关闭。
  - 完成初始密码与 AI 导入最终加固：密码统一要求 8–72 UTF-8 字节且必须含字母和数字，BCrypt cost 12；空白更新不覆盖旧哈希，批量上限 100 且 null 条目受控，AI 不接收或返回自由文本密码。
  - 补齐组合字符、Unicode 箭头、无连接词凭据与正常术语/邮箱反例；前端只在最终批量提交时注入本地初始密码，部分或全部失败时保留重试输入且不再误报成功。
  - 为 AI 对话增加 40 条、单条 4000 字符、总计 16000 字符限制，并约束模型输出 users≤100、status∈{0,1}；新增每管理员 10 次/小时的可注入 Clock 固定窗口限流，明确其为阶段 2 单进程适配器，阶段 3 Task 3 必须替换为 Redis 多实例实现。
  - 为 legacy `BusinessException(code=429)` 完成 HTTP 语义 RED→GREEN：真实返回 429 与 `Retry-After: 3600`；其他 legacy 业务异常继续维持 HTTP 200 + `Result` 兼容行为，v1 仍使用 Problem Details。
  - 两套 Nginx 收紧普通请求体为 2MB，仅 `/api/file` 保留 1600MB，并把后端读取超时统一为 130 秒；部署配置回归门禁覆盖该差异化策略。
  - 最新安全切片后端定向测试 72/72 通过；前端代理报告全量 Vitest 113/113 通过，等待主线程阶段 2 全门禁复验后作为最终证据写入。
  - 阶段 2 基础门禁主线程复验：工具链 6/6、配置 30/30、smoke 契约 23/23、覆盖率配置 3/3 通过；格式门禁仅发现新增 `user.test.ts` 的机械格式漂移，已精确格式化，待复验。
  - 格式门禁复验通过；真实 `api:check` 再次完成 Springdoc 导出和 Orval 生成，17/17、25 个 v1 路径、41 个生成文件、30 个 v1 URL 全部一致。
  - 前端全量主线程复验通过：14 个测试文件、113/113；可靠当前覆盖率 Statements 28.35%、Branches 28.61%、Functions 20.88%、Lines 29.83%，生产构建与三项 bundle 预算全部通过。
  - JDK 25 后端干净单元门禁通过：289/289，JaCoCo 分析 171 个类且可靠覆盖率 Line 41.7076%、Method 41.4020%、Branch 33.7321%，均高于防回退门槛。
  - `git diff --check` 退出 0，仅报告 Windows 行尾提示；当前分支 `refactor/v2` 有 338 个工作区条目，均作为现有重构成果保留。`.env` 未跟踪、未出现在状态中且命中忽略规则。
  - 阶段 2 fresh-context 安全复审拒绝直接放行，发现两个门禁未覆盖的阻断：无数字旧口令/`secret`/`api key` 仍可进入 DeepSeek；普通 Nginx `location /api/file` 会让 `/api/fileevil` 获得 1600MB 配额。
  - 将两个阻断拆为独立 RED→GREEN 切片：凭据边界仅修改 AI 导入前后端及测试；上传路径边界仅修改两套 Nginx 和安全配置门禁。阶段 2 在两项复验前保持 `in_progress`。
  - 凭据边界 RED：前端新增 4 个反例失败（其余 31 通过），后端消息/结构化字段新增 8 个反例失败（其余 51 通过）；最小修复后前端相关 44/44、后端 JDK 25 定向 59/59 通过，并继续放行密码学、Password Security、Credentials Team 与 `password@example.com`。
  - Nginx 上传边界 RED：旧校验器会接受 `location /api/file`，Node 测试 29/31；修正校验器后实际配置按预期失败。两套配置收紧为 `location ^~ /api/file/` 与无 URI `proxy_pass` 后，配置 31/31、实际安全基线和 smoke 23/23 全绿。
  - 对凭据修复追加 fresh-context 复审，专门检查英文纯字母旧口令是否仍可绕过；该复审结论返回前仍不关闭阶段 2。
  - 第二次凭据复审复现剩余阻断：`password monkey`、`secret monkey`、`api key monkey` 在前端实际探针与后端同构规则中仍被放行；已追加纯字母英文值 RED→GREEN 修复轮次，现有 44/44 与 59/59 不能作为最终闭环证据。
  - 第二轮修复完成后基础探针已能拒绝纯字母英文值，前端相关 51/51、后端 73/73；但最终独立复审发现“先全局删除白名单短语”会放过 `secret Password Security`、`Password Security -> monkey` 等组合载荷，因此再次拒绝放行并进入第三轮候选级匹配修复。
  - 第三轮改为逐候选扫描后，前端相关 56/56、后端 83/83，并关闭此前组合绕过；限定复审又发现 `equals`/`equal to`（以及 `as`）未计入英文赋值连接词，已进入最后一轮连接词集合补齐。
  - 最后一轮连接词 RED：前端新增 `equals/equal to/as/means/mean` 5 例失败，后端消息/结构化字段新增 10 例失败；补齐多词优先的 connector 后，前端相关 61/61、后端 JDK 25 定向 93/93 通过，等待限定范围最终复审。
  - 限定范围最终凭据复审 Approve：完整攻击矩阵 23/23、前端探针 52/52，纯字母、组合白名单、候选迭代与赋值连接词全部拒绝，正常短语和邮箱继续放行。
  - OpenAPI 最终复验一度因导出 JVM 默认 MaxHeap 4028MB、系统提交内存接近上限而原生 OOM；新增专用 JVM 内存 RED→GREEN 门禁并锁定 Xms128m/Xmx512m、Metaspace 256m、CompressedClassSpace 128m 后，在同一高压环境真实导出成功。
  - 阶段 2 最终全量复验完成：工具链 7/7、配置 31/31、smoke 23/23、格式、覆盖率配置 3/3、OpenAPI 17/17、前端 134/134、后端 331/331 全绿。
  - 最新可靠覆盖率：前端 Statements 28.50%、Branches 28.70%、Functions 20.88%、Lines 29.99%；后端 Line 41.8441%、Method 41.4661%、Branch 34.0476%。它们高于防回退门槛但仍远低于最终 80/80/70。
  - 清理本轮 JVM 崩溃生成且未进入 Git 状态的 `hs_err`/`replay` 诊断文件；再次 `git diff --check` 退出 0，仅保留 Windows 行尾提示。
- 创建/修改的文件：
  - `scripts/trim-generated-api.mjs`
  - `frontend/src/shared/api/generated/**`
  - `frontend/src/shared/api/http.ts`
  - `frontend/src/shared/api/__tests__/http.test.ts`
  - `frontend/src/app/providers/queryClient.ts`
  - `frontend/src/main.ts`
  - `scripts/verify-toolchain.mjs`
  - `scripts/verify-toolchain.test.mjs`

## 测试结果

| 测试                           | 输入                                                                        | 预期结果                                                    | 实际结果                                                               | 状态                                      |
| ------------------------------ | --------------------------------------------------------------------------- | ----------------------------------------------------------- | ---------------------------------------------------------------------- | ----------------------------------------- |
| Git 基线检查                   | `git status --short --branch`                                               | 识别分支且无既有未提交改动                                  | `refactor/v2`，初始工作区干净                                          | 通过                                      |
| 快速全门禁（首次）             | `npm run verify:quick`                                                      | 当前 HEAD 全部通过                                          | 工具链/安全配置通过；`trim-generated-api.mjs` 格式不合规，流程提前失败 | 失败（基线缺陷）                          |
| 根配置格式复验                 | `npm run format:check`                                                      | 全部目标符合 Prettier                                       | 全部匹配                                                               | 通过                                      |
| 前端完整门禁                   | `npm run verify:frontend`                                                   | lint、类型、测试、构建通过                                  | 10 个测试文件、56 个用例全部通过；构建成功                             | 通过                                      |
| 后端单元测试                   | `npm run backend:test:unit`                                                 | 单元测试通过                                                | 181 个用例通过；JaCoCo 报告有旧执行数据不匹配警告                      | 通过（覆盖率数据需清理）                  |
| 前端覆盖率基线                 | `npm --prefix frontend run test:coverage`                                   | 达到计划整体行/函数 80%、分支 70%                           | 行 15.90%、函数 11.87%、分支 18.04%，且低于当前配置门槛                | 失败（重大缺口）                          |
| 后端旧覆盖率样本（后证实污染） | `mvn -f backend/pom.xml -B -ntp clean test` + JaCoCo XML                    | 仅保留历史诊断                                              | 181/181；63.36%/68.14%/44.27%，来自未清理且默认追加的临时 exec         | 无效历史样本（最终口径见阶段 2 最终复验） |
| API 契约一致性基线             | `npm run api:check`                                                         | 生成产物与提交内容一致                                      | 新增评论/点赞/评分客户端并修改既有生成文件，Git diff 非空              | 失败（契约漂移已生成待纳入变更）          |
| 前端 v2 启动切片               | 定向 Vitest + 全量 Vitest + ESLint + vue-tsc                                | URL 不重复 `/api` 且 Query Provider 可用                    | 定向 2/2、全量 58/58，lint/typecheck 通过                              | 通过                                      |
| 工具链单元测试                 | `node --test scripts/verify-toolchain.test.mjs`                             | 版本解析与声明偏离可检测                                    | 3/3 通过                                                               | 通过                                      |
| 工具链声明 RED                 | JDK 25 环境下 `npm run verify:toolchain`                                    | 当前偏离被门禁捕获                                          | 正确报告 POM `java.version` 必须为 25                                  | 失败（预期 RED）                          |
| JDK 25 工具链 GREEN            | JDK 25 环境下 `npm run verify:toolchain`                                    | Node/npm/Java 与声明均锁定                                  | Node 24.14.1、npm 11.11.0、Java 25                                     | 通过                                      |
| JDK 25 后端首次编译            | JDK 25 环境下 `mvn clean test`                                              | 编译并运行单元测试                                          | Lombok 生成成员缺失，编译失败                                          | 失败（兼容性缺口）                        |
| JDK 25 后端复验                | compiler 3.14.1 + 显式 Lombok processor 后 `mvn clean test`                 | 编译与单元测试通过                                          | 184/184 通过                                                           | 通过（发现 JaCoCo 外部数据残留）          |
| JaCoCo 干净复验                | JDK 25 下 `npm run backend:test:unit`                                       | 外部执行数据被 clean 且报告匹配                             | 184/184 通过，无 class mismatch                                        | 通过                                      |
| Docker 目标镜像探测            | Docker Hub / DaoCloud manifest                                              | JDK 25 与 Node 24 镜像存在                                  | Temurin 25 JRE、Node 24 已确认；Maven 25 查询超时                      | 部分通过                                  |
| 阶段 2 恢复门禁                | `npm run verify:toolchain`、`npm run verify:config`、`npm run format:check` | 恢复后基础门禁绿色                                          | 工具链 5/5、安全配置 5/5、Prettier 全部通过                            | 通过                                      |
| Codegraph 增量同步             | `codegraph sync .`                                                          | 索引覆盖当前工作区                                          | 同步 2 个变更文件、33 个节点                                           | 通过                                      |
| API 契约恢复复验               | `npm run api:check`                                                         | YAML、运行时契约和生成客户端无漂移                          | 25 个 v1 路径；40 个文件、30 个 v1 URL                                 | 通过                                      |
| Bundle 预算集成复核            | CI/根脚本 + `frontend:bundle:check`                                         | 构建后执行预算门禁                                          | 84.27/180KB、201.97/220KB、11.70/50KB                                  | 通过                                      |
| 基础门禁新增测试 RED           | 两个 Node 测试文件 + 当前仓库配置                                           | 新规则应捕获三项缺口                                        | 单测先因缺导出失败；实现校验器后实际门禁正确报告缺 JDK/契约/格式链路   | 失败（预期 RED）                          |
| 基础门禁收口 GREEN             | `verify:toolchain`、`verify:config`、`format:check`                         | 新规则与实际配置全部通过                                    | 6/6、7/7，根与前端 Prettier 全绿                                       | 通过                                      |
| 前端完整恢复复验               | `npm run verify:frontend`                                                   | lint、类型、测试、构建、预算全部通过                        | 12 个测试文件、59/59；三项预算均通过                                   | 通过                                      |
| 后端单元恢复复验               | `npm run backend:test:unit`（JDK 25）                                       | 干净编译与单元测试通过                                      | 185/185，通过且 JaCoCo 分析 163 个类                                   | 通过                                      |
| Refresh family 领域 RED        | 定向 `RefreshFamilyTest`                                                    | 新领域类型尚不存在时失败                                    | 4 个用例均因 `RefreshFamily` 未定义失败                                | 失败（预期 RED）                          |
| Refresh family 领域 GREEN      | 定向 `RefreshFamilyTest`                                                    | 绝对 TTL 与旋转状态机通过                                   | 4/4 通过；生产/测试源码均编译                                          | 通过（Maven 总体被定向覆盖率误拦截）      |
| Docker/Testcontainers 环境     | `docker version` + 安装路径探测                                             | daemon 可用                                                 | Docker pipe 不存在，未发现 Docker Desktop                              | 阻塞环境验证                              |
| v2 模块边界门禁                | `ApplicationModulesTest,V2ModuleArchitectureTest`                           | 九模块与三类边界规则通过                                    | 4/4 通过                                                               | 通过                                      |
| 覆盖率配置门禁                 | `npm run verify:coverage-gates` + 前端 coverage                             | 阈值与 CI 接线不可静默放松                                  | 配置测试 3/3；前端 59/59 达到当前基线                                  | 通过                                      |
| Docker Compose 配置 RED        | `npm run verify:config`                                                     | 当前旧配置被新门禁捕获                                      | 正确报告 6 项镜像/网络/JWT/SQL 问题                                    | 失败（预期 RED）                          |
| Docker Compose 配置 GREEN      | `npm run verify:config`                                                     | 容器服务名、固定版本、Flyway 独占                           | 9/9 配置测试与实际基线通过                                             | 通过                                      |
| 安全冒烟切片复验               | `npm run smoke:contract` + `bash -n scripts/smoke-test.sh`                  | 默认只读、失败非零、full 回收且语法有效                     | 7/7 通过；Bash 语法通过（WSL 输出非阻断启动告警）                      | 通过                                      |
| 持久化事件静态契约首次运行     | JDK 25 `PersistentEventFoundationContractTest`                              | 3 个静态契约测试通过                                        | `List.getFirst()` 的旧增量测试类出现未解析编译桩                       | 失败（测试实现缺陷）                      |
| 持久化事件静态契约复验         | 将测试改为 `sources.get(0)` 后定向运行                                      | 依赖、配置与 Flyway DDL 契约通过                            | 3/3 通过，JaCoCo 按显式 skip 跳过                                      | 通过                                      |
| 持久化事件启动路径 RED         | 扩展 `PersistentEventFoundationContractTest`                                | 三条真实启动路径缺口被捕获                                  | Flyway、OpenAPI排除、冗余 async 三项均按预期失败                       | 失败（预期 RED）                          |
| 持久化事件启动路径 GREEN       | 修复 BaseApiTest/POM并移除冗余配置                                          | 事件基础不破坏集成测试与 OpenAPI                            | 6/6 通过；JDK 25 clean test-compile 通过                               | 通过                                      |
| H2 OpenAPI 第一次真实启动      | `npm run backend:openapi:export`                                            | 文档上下文正常启动                                          | Refresh 服务多构造器未显式注入，Spring 报无默认构造器                  | 失败（运行时缺陷）                        |
| H2 OpenAPI 第二次真实启动      | 标记生产构造器后重跑                                                        | 排除 JDBC 仓储后正常启动                                    | 核心 staleness 自动配置缺 EventPublicationRegistry                     | 失败（排除范围不足）                      |
| H2 OpenAPI 第三次真实启动      | 同时排除 JDBC 与核心事件自动配置                                            | 成功导出 v1 OpenAPI                                         | Boot 4.1/Spring 7.0.8 启动、生成并优雅停止                             | 通过                                      |
| 事件恢复与 V1 资源定向复验     | 4 个测试类，JDK 25，JaCoCo skip                                             | 事件恢复、配置、基础契约、资源自包含全部通过                | 18/18 通过                                                             | 通过                                      |
| Docker/CI 静态契约             | `npm run verify:config`                                                     | 安全、Compose、CI runtime smoke 与冒烟脚本合同通过          | 配置 9/9、smoke 11/11                                                  | 通过（容器实跑仍阻塞）                    |
| 阶段 2 后端收口定向测试        | 9 个测试类，JDK 25，JaCoCo skip                                             | OpenAPI、状态码、路由错误、密码与 AI 导入切片通过           | 52/52 通过                                                             | 通过                                      |
| OpenAPI 真实重生首次尝试       | `npm run api:contract:update`                                               | 运行时契约写入并通过语义门禁                                | Springdoc 导出成功；`ProblemDetails` 缺显式 `type: object` 被门禁拒绝  | 失败（模型 Schema 缺口）                  |
| OpenAPI 3.1 序列化回归         | `OpenApiConfigTest`                                                         | ProblemDetails 序列化为 object                              | 新测试先失败，定制器修复后 4/4 通过                                    | 通过（RED→GREEN）                         |
| OpenAPI/Orval 最终一致性       | `npm run api:check`                                                         | 运行时、YAML、生成客户端完全一致                            | 17/17；25 路径、41 文件、30 个 v1 URL                                  | 通过                                      |
| 可靠覆盖率门禁纠偏             | `node --test scripts/verify-coverage-gates.test.mjs`                        | 锁定 clean、append=false 与真实 unit-only 基线              | 新断言先失败，修复后 3/3 通过                                          | 通过（RED→GREEN）                         |
| AI/用户/密码/限流安全切片      | JDK 25 下 6 类定向测试                                                      | 输入输出边界、密码策略、限流与 429 兼容通过                 | 72/72 通过                                                             | 通过                                      |
| 前端 AI 导入与凭据防泄漏切片   | 全量 Vitest                                                                 | 所有成功/部分失败/全部失败及凭据反例通过                    | 主线程全量 134/134；攻击矩阵独立复审 23/23                             | 通过                                      |
| Nginx 上传与超时策略           | 配置门禁                                                                    | 普通 2MB、文件 1600MB、读取 130 秒                          | 两套配置均已锁定                                                       | 通过                                      |
| 阶段 2 基础门禁主线程复验      | 工具链、配置、格式、覆盖率配置                                              | 四项全部绿色                                                | 6/6、30/30、23/23、3/3；格式复验通过                                   | 通过                                      |
| 阶段 2 OpenAPI 最终复验        | `npm run api:check`                                                         | 运行时、YAML、生成客户端无漂移                              | 17/17；25 路径、41 文件、30 个 v1 URL                                  | 通过                                      |
| 阶段 2 前端最终复验            | `npm run verify:frontend`                                                   | lint、类型、覆盖率、构建、预算全部通过                      | 14 文件、134/134；Lines 29.99%、Functions 20.88%、Branches 28.70%      | 通过                                      |
| 阶段 2 后端最终复验            | JDK 25 `npm run backend:test:unit`                                          | 干净测试与可靠 JaCoCo 门禁通过                              | 331/331；Line 41.8441%、Method 41.4661%、Branch 34.0476%               | 通过                                      |
| Git 与敏感文件收口             | `git diff --check` + 状态/.env 审计                                         | 无空白错误且 `.env` 不进入变更                              | diff check 0；`.env` 未跟踪、未出现在状态且被忽略                      | 通过                                      |
| 凭据绕过安全回归               | 前端相关测试 + JDK 25 `AiUserImportServiceImplTest`                         | 消息与模型结构化字段拒绝旧口令/secret/api key，正常术语放行 | 最终轮 GREEN 前端 61/61、后端 93/93；独立攻击矩阵 23/23                | 通过                                      |
| Nginx 上传路径边界             | 配置单测 + 实际基线 + smoke                                                 | `/api/fileevil` 不得继承 1600MB，上传路径保持完整           | RED 29/31；GREEN 31/31 + smoke 23/23                                   | 通过                                      |
| OpenAPI 导出内存可靠性         | 工具链静态门禁 + `npm run api:check`                                        | 高内存压力下不再使用 4GB 默认堆崩溃                         | RED 缺导出内存约束；GREEN 工具链 7/7，API 17/17                        | 通过                                      |
| Task 2 Cookie/Origin 定向回归  | Cookie、Controller、Origin、OpenAPI、架构测试                               | JSON 不含 refresh、严格同源、契约和模块边界一致             | 65/65 通过；OpenAPI 25 路径、40 个生成文件、30 个 v1 URL               | 通过                                      |
| Task 2 非默认端口回归          | 安全配置单测 + 实际配置 + smoke                                             | `$host` 反例失败，`$http_host` 保留客户端 authority         | RED 34/35；GREEN 35/35，实际配置与 smoke 23/23 通过                    | 通过                                      |
| Task 2 最终独立五轴复审        | 正确性、可读性、架构、安全、性能                                            | 无 Critical/Important 遗留                                  | 首轮 1 个 Important 已修复，第二轮 Approve                             | 通过                                      |

## 错误日志

| 时间戳     | 错误                                                                                             | 尝试次数 | 解决方案                                                                                                              |
| ---------- | ------------------------------------------------------------------------------------------------ | -------- | --------------------------------------------------------------------------------------------------------------------- |
| 2026-07-11 | 批量读取技能文件时输出超过限制被截断                                                             | 1        | 分别重新读取 TDD 与 Git 技能全文                                                                                      |
| 2026-07-11 | `Get-Command cg` 未找到，使 Codegraph 探测命令返回非零                                           | 1        | 使用已确认存在的 `codegraph` CLI                                                                                      |
| 2026-07-11 | `codegraph init .` 在 1 秒超时下被终止                                                           | 1        | 查询命令选项后改用可容纳首次索引的执行方式                                                                            |
| 2026-07-11 | `npm run verify:quick` 在 Prettier 检查失败                                                      | 1        | 格式化目标脚本后从失败点继续验证                                                                                      |
| 2026-07-11 | 并行工具链/文件跟踪探测因一个子命令非零而整体失败                                                | 1        | 拆分并对可选路径使用显式存在性判断                                                                                    |
| 2026-07-11 | 覆盖率并行任务因前端低于门槛提前拒绝，后端结果未汇总                                             | 1        | 单独复跑后端干净覆盖率                                                                                                |
| 2026-07-11 | Temurin 25 下载跳转 GitHub 后连接超时                                                            | 1        | 使用微软官方 OpenJDK 25 下载地址成功获取并校验哈希                                                                    |
| 2026-07-11 | 两个代理并发 Maven 导致 `backend/target` 竞争                                                    | 1        | 后端构建改为串行执行                                                                                                  |
| 2026-07-11 | 读取不存在的 `queryClient.test.ts`                                                               | 1        | 通过目录清单确认测试集中在 `http.test.ts`                                                                             |
| 2026-07-11 | JDK 25 编译时 Lombok 注解处理未生效                                                              | 1        | 诊断依赖与编译插件版本并升级/显式配置                                                                                 |
| 2026-07-11 | Maven 诊断参数在 PowerShell 中被错误拆分为生命周期/插件前缀                                      | 2        | Refresh 独立复验首次又漏引 `-Djacoco.skip=true`；立即改为整体加引号后 19/19 通过，后续所有 system property 均强制引用 |
| 2026-07-11 | 更新计划文件时补丁上下文放错目标文件                                                             | 1        | 定位实际行后分别修正 `findings.md` 与 `progress.md`                                                                   |
| 2026-07-11 | Docker Hub 与 DaoCloud Maven manifest 查询超时                                                   | 1        | 保留标签静态门禁，后续通过实际构建验证                                                                                |
| 2026-07-11 | DaoCloud 临时 token 被 PowerShell 组装为无效请求头                                               | 1        | 停止手写 registry 调用，改用标准 Docker 客户端                                                                        |
| 2026-07-11 | 代理配置门禁误把 `/api/file` 当作通用 `/api/`                                                    | 1        | 使用精确 location 块正则                                                                                              |
| 2026-07-11 | 并行门禁中 `format:check` 失败使首次汇总提前结束                                                 | 1        | 修复格式后使用 `Promise.allSettled` 独立保留每项结果                                                                  |
| 2026-07-11 | 从根目录执行 Prettier 时误用了 `../scripts` 路径                                                 | 1        | 改用根目录相对路径 `scripts/...` 完成格式化                                                                           |
| 2026-07-11 | 新增 CI job 解析器误用了 JavaScript 不支持的正则 `\\z`                                           | 1        | 改为逐行解析 `jobs:` 下的二级 job 块，避免脆弱的跨行正则                                                              |
| 2026-07-11 | Maven clean 无法删除被占用的 `backend/target`                                                    | 1        | 保留用户 PID 31012，改用不清理的定向编译继续验证                                                                      |
| 2026-07-11 | 定向领域测试通过后仍被 bundle JaCoCo 基线判失败                                                  | 1        | 修正覆盖率门禁以尊重显式 `jacoco.skip`，完整套件保持默认强制                                                          |
| 2026-07-11 | Docker Compose 有效 JWT fixture 因空格被正则截断                                                 | 1        | 捕获整行并 trim，再校验 `${JWT_SECRET:?...}` 无默认值形式                                                             |
| 2026-07-11 | 误把两个普通命令提交给只接受运行中 cell 的 `wait` 工具                                           | 1        | 不重复该调用；改用 `functions.exec` 内并行执行两个独立 shell 命令                                                     |
| 2026-07-11 | 读取不存在的 `backend/src/test/resources/application-test.yml`                                   | 1        | 定位到真实文件 `backend/src/main/resources/application-test.yml`，后续按实际资源目录读取                              |
| 2026-07-11 | Spring 文档版本 URL 重定向到 HTTP，PowerShell 默认拒绝不安全跳转                                 | 1        | 直接访问官方 HTTPS 当前文档，并用 GitHub `2.1.0` 标签源码完成版本锁定核对                                             |
| 2026-07-11 | 提取 HTML 片段时 PowerShell 命令中的实体字符触发解析错误                                         | 1        | 改为先 `HtmlDecode` 再对纯文本做正则提取                                                                              |
| 2026-07-11 | 持久化事件契约测试首次运行出现 `List.getFirst()` 未解析编译桩                                    | 1        | 改为兼容且等价的 `sources.get(0)`，3/3 复验通过                                                                       |
| 2026-07-11 | OpenAPI 实际启动发现 Refresh Service 多构造器导致 Spring 需要默认构造器                          | 1        | 给单参生产构造器显式 `@Autowired`，保留包内可控时钟测试构造器                                                         |
| 2026-07-11 | OpenAPI 只排除 Modulith JDBC 自动配置后，staleness 配置仍因缺 registry 失败                      | 1        | 同时排除官方核心 `EventPublicationAutoConfiguration`，真实导出通过                                                    |
| 2026-07-11 | V1 资源契约在未 clean 的 target 中看到已删除 `test_data.sql`                                     | 1        | 避免让源码契约依赖陈旧构建输出；改为检查 `src/main/resources` 不含 fixture，随后 18/18 通过                           |
| 2026-07-11 | `rg` 检查已删除 SQL 文档引用时“无匹配”返回退出码 1                                               | 1        | 将无匹配视为预期结果；后续同类 PowerShell 检查显式处理 `$LASTEXITCODE=1`                                              |
| 2026-07-11 | 预读身份相关文件时误用不存在的 `backend/src/main/java/com/jingxuan/security/SecurityConfig.java` | 1        | 改用 Codegraph 定位实际配置文件，不重复猜测路径                                                                       |
| 2026-07-11 | 组合读取 Flyway SQL 与 fixture 的 PowerShell 命令因反引号/正则引用返回 1                         | 1        | 拆成资源行读取与简单 `rg` 两个独立命令，使用 `Promise.allSettled` 汇总                                                |
| 2026-07-11 | 猜测的 Swagger Core 2.2.38 Javadoc 深链返回 404                                                  | 1        | 停止猜测版本 URL；采用 Springdoc/Spring Framework 官方页面，并让 Maven 编译与运行时导出验证实际模型 API               |
| 2026-07-11 | OpenAPI 真实导出后 `ProblemDetails` 缺少显式 `type: object`                                      | 1        | 保留严格语义门禁，修正 Java 模型 Schema 声明后重新真实导出，不通过放宽校验绕过                                        |
| 2026-07-11 | 首次组合更新三份计划文件时一处精确上下文少了空格                                                 | 1        | 用 `rg -n` 定位实际文本后拆成精确补丁并成功写入                                                                       |
| 2026-07-11 | `@Schema(type = "object")` 未改变 Springdoc record 的顶层类型输出                                | 1        | 停止原样重跑，检查 OpenAPI 定制链路并在最终组件 Schema 上显式规范化类型                                               |
| 2026-07-11 | 阶段 2 全局格式门禁发现两个 OpenAPI Node 文件漂移                                                | 1        | 仅对 `check-openapi-contract.mjs` 及其测试执行 Prettier 机械格式化后复验                                              |
| 2026-07-11 | 后端 266/266 测试通过但 JaCoCo 仅采集 41.07/40.92/32.72，低于锁定基线                            | 1        | 不下调阈值；诊断 Surefire fork 与 JaCoCo exec 聚合配置，修复采集可靠性后重跑                                          |
| 2026-07-11 | 汇总未跟踪 Java 文件行数的 PowerShell foreach 后直接管道导致解析失败                             | 1        | 先把 foreach 结果赋给变量，再排序输出；改用 `Promise.allSettled` 保留其他诊断结果                                     |
| 2026-07-11 | 临时 HEAD worktree 在 JDK 25 下因旧 compiler 3.11 未启用 Lombok 处理而无法编译                   | 1        | 仅在临时 worktree 应用当前已验证的 compiler 3.14.1/显式 Lombok processor 兼容补丁，再重跑旧源码基线                   |
| 2026-07-11 | 历史 JaCoCo 63.36% 门槛来自未清理且默认追加的临时 exec                                           | 1        | 用 detached HEAD 复现实锤 class mismatch；改锁当前干净单 session unit-only 基线 41.00/40.90/32.70，最终目标不变       |
| 2026-07-11 | 临时 worktree 清理前的注册路径精确字符串校验未匹配                                               | 1        | 查看 porcelain 实际路径格式，统一斜杠/大小写后再由 `git worktree remove` 安全移除                                     |
| 2026-07-11 | 覆盖率门禁搜索同时包含不存在的 `verify-coverage-gates.mjs` 导致 rg 返回 1                        | 1        | 使用真实存在的测试文件继续定位；无对应实现脚本是预期目录结构                                                          |
| 2026-07-11 | 新增 clean-plugin 回归断言误把 artifactId 当 XML 元素名                                          | 1        | 修正为匹配 `<artifactId>maven-clean-plugin</artifactId>` 后复验                                                       |
| 2026-07-11 | 前端全局格式门禁发现新增 `src/api/admin/__tests__/user.test.ts` 漂移                             | 1        | 仅对该测试文件执行 Prettier 机械格式化，保留测试语义                                                                  |
| 2026-07-11 | 首次精确格式化仍按 `npm --prefix` 后工作目录推断了错误相对路径                                   | 1        | 改用仓库根相对路径 `frontend/src/api/admin/__tests__/user.test.ts` 成功格式化                                         |
| 2026-07-12 | Nginx 安全校验器通过逻辑测试但未通过全局 Prettier                                                | 1        | 仅机械格式化 `scripts/verify-security-baseline.mjs` 后全局格式复验通过                                                |
| 2026-07-12 | OpenAPI 导出 JVM 原生内存分配失败，未生成契约文件                                                | 1        | 读取 crash 证据定位默认 4028MB 最大堆；增加专用内存上限与 1 个工具链回归测试后真实导出通过                            |
| 2026-07-12 | PowerShell `rg` 对 `application*.yml` 的通配路径按字面传入，Windows 返回路径语法错误             | 1        | 改用 `Get-ChildItem -Filter application*.yml` 后逐文件读取，不重复向 `rg` 传 Windows 通配路径                         |
| 2026-07-12 | 首次组合更新进度日志的补丁上下文未匹配                                                           | 1        | 用 `rg -n -C` 定位实际行后拆为两个精确补丁写入                                                                        |
| 2026-07-12 | 会话追赶首次硬编码了不存在的 Python 3.11 路径                                                    | 1        | 改用 `Get-Command python` 解析当前解释器后成功运行脚本                                                                |
| 2026-07-12 | Task 2 独立复审发现 Nginx `$host` 会丢失非默认客户端端口                                         | 1        | 新增反例门禁后改用 `$http_host`，35/35 与复审通过                                                                     |
| 2026-07-12 | Task 2 端口门禁实现后全局格式检查发现脚本漂移                                                    | 1        | 精确格式化安全门禁脚本后全局 Prettier 通过                                                                            |
| 2026-07-12 | BCrypt 透明升级 RED 首跑被测试内 `List.getFirst()` 未解析桩阻断                                  | 1        | 改用 `get(0)` 后再运行行为 RED，不把测试缺陷当作需求证据                                                              |
| 2026-07-12 | Redis 限流 RED 首跑混入 `List.removeFirst/getFirst` 未解析桩                                     | 1        | 改为 `ArrayDeque`/`get(0)`，第二次只因目标接口和实现缺失而失败                                                        |
| 2026-07-12 | 限流 GREEN 首跑的可信代理解析器因 `InetAddress.ofLiteral` 出现未解析桩                           | 1        | 改为兼容的无 DNS 字面量解析，不依赖当前语言服务缺失的 JDK 25 API                                                      |
| 2026-07-12 | Challenge 字段纠偏复验被并行限流测试的未来过滤器构造器阻断                                       | 1        | Challenge 相关测试均通过；限流实现完成后统一复跑 SecurityConfigAuthorizationTest                                      |

### 阶段 3：身份权限与基础数据闭环补齐

- **状态：** in_progress
- **开始时间：** 2026-07-12
- 执行的操作：
  - 续接会话后完整读取原始 v2 计划、`task_plan.md`、`progress.md` 与 `findings.md`，确认阶段 2 已收口且当前切片仍为 Task 2。
  - 复核工作区健康状态：根/前端 Prettier 全绿，`git diff --check` 退出 0；`.env` 命中忽略规则且未被 Git 跟踪。
  - 检查文档中的旧测试数与覆盖率表述，确认 113/113、289/289 等只存在于按时间记录的历史过程，当前最终口径仍为前端 134/134、后端 331/331。
  - 为 Task 2 新增 OpenAPI RED 门禁：旧校验器无法拒绝 refresh/logout JSON 请求体、缺失 `Set-Cookie` 或登录响应泄露 refresh 字段，新增 4 个契约测试后按预期失败。
  - 为严格同源配置新增 RED 门禁：旧校验器无法拒绝 Vite v1 `changeOrigin: true` 与 `allowedOriginPatterns("*") + allowCredentials(true)`，新增 2 个测试后按预期失败。
  - 实现两组静态校验器后，OpenAPI 18/18、安全配置单测 33/33 通过；随后移除后端通配凭据 CORS，并将仅 `/api/v1` 的 Vite 代理改为 `changeOrigin: false`，实际安全基线恢复绿色。
  - 本次续接按磁盘计划恢复上下文，并并行发起 Task 2 五轴独立审查、原计划剩余项审计和 Task 3 只读上下文探索；不重做已通过的实现，也不触碰用户旧后端 PID 31012。
  - Task 2 已完成：`jingxuan_refresh` 使用 HttpOnly/SameSite=Strict/Path 锁定 Cookie，refresh/logout 不再接收 JSON token，三条认证 POST 执行严格 Origin 校验，OpenAPI/Orval 已同步。
  - 首轮独立五轴复审发现两套 Nginx 的 `$host` 会在 `${HTTP_PORT}` 非默认端口下造成合法 Origin 误拒；新增 RED 门禁后改为 `$http_host`，35/35 配置单测、实际安全基线、smoke 23/23 与第二轮复审全部通过。
  - Task 2 收口门禁：格式检查、`git diff --check`、`.env` 未跟踪/忽略审计全绿；真实 Nginx 非默认端口与 `V1AuthApiTest` 仍因本机无 Docker daemon 无法实跑。
  - Task 3 BCrypt 重做版主线程首轮 GREEN：`SecurityConfigAuthorizationTest` 1/1、`SecurityConfigPasswordEncoderTest` 2/2 通过；`PasswordUpgradeAuthenticationProviderTest` 5 项中 1 项失败，cost-10 哈希认证成功后仍未升级到 cost 12。失败已保留为回归证据，下一步只定位 Provider/UserDetailsPasswordService 触发链，不放宽旧哈希 CAS 与 MySQL 二进制比较语义。
  - BCrypt 失败被证明为测试误判：CAS 与数据库 cost 12 实际已成功，Spring Security 7.1 仅保留认证前 principal。修正断言后主线程在 JDK 25 下复跑 `PasswordUpgradeAuthenticationProviderTest`、`SecurityConfigAuthorizationTest`、`SecurityConfigPasswordEncoderTest`，8/8 通过；同轮 Maven testCompile 也接受新增的两份 Redis Testcontainers 并发测试源码。
  - 新增真实 Redis 集成覆盖：限流跨实例共享、5/6 边界、reset、100 并发精确限额、真实 PTTL 与窗口恢复；challenge 跨实例一次性消费、错误答案消费、100 并发单成功及 TTL≤300 秒。因本机无 Docker daemon，当前只完成编译，未声称运行通过。
  - Redis 通用限流安全收口：先添加超大 limit/window/subject 仍访问 Redis 的 RED 反例，再将策略限制为 limit≤1000、窗口≤24小时、subject≤512 字符，并让 fail-closed 返回值也受同一上限约束；`RedisRateLimitServiceUnitTest` 6/6 转绿。
  - Task 3 登录保护纵切：从缺失 `LoginProtectionService` 的 RED 开始，新增账号/IP 双策略（15分钟、前5次失败后 challenge、前20次失败后429），失败同时计入四个策略、成功仅清账号；控制器使用可信 IP 解析并统一已知/未知凭据失败为 `LOGIN_CREDENTIALS_INVALID`。定向用例 6/6 转绿。
  - Challenge 纵切收口：公开发行端点移至 `ChallengeIssuanceService`，按可信 IP 每分钟10次 Redis 限流；Redis challenge 故障改为脱敏 `CHALLENGE_UNAVAILABLE` 503。v1 统一 `IdentityAccessProblemException` 输出稳定 Problem Details 与实际 `Retry-After`；挑战 429/503、challenge 字段成对/精确 ID 校验及登录失败统一错误由 `V1AuthControllerTest` 13/13 覆盖。
  - Redis Lua 返回值加固：先以矛盾计数、超窗口 Retry-After、浮点数 RED 复现 fail-open，再要求 consume/inspect 语义一致、只接受精确整型与不超过窗口的 Retry-After；`RedisRateLimitServiceUnitTest` 7/7 转绿。
  - 独立审查可信代理修复：将“全部 RFC1918 均可信”替换为显式 CIDR 列表，默认仅 loopback；PM2 启动参数绑定 `127.0.0.1`，Docker Compose 明确授权其内部 `172.16.0.0/12` Nginx 网络。可信 IP 单测 5/5 通过。
  - 公开限流修复：移除原始 URI 作为 subject，统一为 `ip:<地址>`，覆盖 v1 showcase 与公开参考数据并剥离 matrix parameter；`PublicRateLimitFilterTest` 5/5 通过。
  - AI 限流传播修复：`RateLimitedException` 携带 Redis Decision 的实际重试秒数，legacy 429 不再固定写 3600；控制器和全局异常定向测试 7/7 通过。
  - Nginx 静态门禁修复：逐个检查四个 API/uploads 代理块均覆盖真实 IP 头，并验证 asset、子路径和 HTML 三类 location 的完整安全头/缓存语义；Node 配置门禁 36/36 通过。
  - Task 3 OpenAPI/Orval 收口：将 `POST /api/v1/auth/challenges` 纳入公开操作和锁定成功状态 201，补充 challenge 429/503、登录 401/429/503 文档；运行真实 Springdoc 导出、更新 YAML、重新生成 Orval。契约现为 26 条 v1 路径、43 个生成文件、31 个 v1 URL，语义/生成校验通过。
  - Task 3 五轴复审：基于原始 5/20/一次性/透明升级规范复核测试、实现、认证边界、Redis Lua 和部署配置。审查先发现并关闭 1 个 challenge Redis key DoS Critical 与 7 个 Important（503、字段成对、代理范围、路由分桶、Lua 语义、Retry-After、Nginx 门禁）；最终未发现新的仓库内阻断。真实 Redis/MySQL/Testcontainers 与 Nginx 运行时测试仍因本机 Docker daemon 缺失，只保留为外部环境验证缺口。
  - Task 4 第一切片：新增 `PENDING_APPROVAL(2)`，教师自助注册写入待审核状态，认证加载仅接受 `ENABLED`，从而在审批端点完成前已经阻断教师登录。`RegistrationServiceTest` 与 `CustomUserDetailsServiceApprovalTest` 2/2 通过。
- 下一实施切片：Task 3 Redis 通用原子限流；先建立通用计数决策与 Redis Lua RED/GREEN，再迁移 AI/公开过滤器，随后实现 challenge、登录 5/20 与 BCrypt 透明升级。

## 五问重启检查

| 问题           | 答案                                                             |
| -------------- | ---------------------------------------------------------------- |
| 我在哪里？     | 阶段 3：身份权限与基础数据闭环补齐                               |
| 我要去哪里？   | 按阶段补齐工程、业务、遗留清理、性能安全、迁移部署并完成全量回归 |
| 目标是什么？   | 以用户计划为基线完成仓库内所有未完成工作，并给出验证证据         |
| 我学到了什么？ | 见 `findings.md`                                                 |
| 我做了什么？   | 见上方记录                                                       |

---

_每个阶段完成后或遇到错误时更新此文件_
