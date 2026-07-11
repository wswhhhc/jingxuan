# 菁选 v2 实施记录

## 约束

- 分支：`refactor/v2`（当前为本地恢复仓库；远端首次 fetch 因网络重置失败）。
- 不直接修改或清理生产 MySQL、Redis、uploads。
- 每个增量必须通过受影响测试后再进入下一项。
- Git commit、PR 和发布说明使用中文。

## 进度

### 阶段 0：冻结与安全网

- [x] 建立功能等价矩阵和 v1 基线文档。
- [x] 建立初版威胁模型并记录 HTTP 已接受风险。
- [x] 主配置改为从 `JWT_SECRET` 注入，不再硬编码固定密钥。
- [x] 修复前端 lint、类型、覆盖率与生产依赖审计。
- [x] 后端 Testcontainers 单元/集成测试分层。
- [x] 前后端并行 CI、秘密扫描和统一根命令。
- [x] 完成阶段 0 全量验证并建立本地基线提交。

#### 2026-07-11 验证结论

- `npm run verify:config` 通过。
- `npm run verify:frontend` 通过：ESLint、`vue-tsc`、Vitest（56/56）和生产构建均成功。
- `npm run frontend:audit` 通过：生产依赖 0 漏洞。
- `npm run backend:test:unit` 通过：后端单元测试 151/151 通过，并生成 JaCoCo 报告。
- `mvn -f backend/pom.xml -B -ntp clean test` 通过。
- `mvn -f backend/pom.xml -B -ntp verify` 已通过：单元测试与 Testcontainers 集成测试共 168 个用例全部通过。
- 已修复 JaCoCo 在中文路径下把 `jacoco.exec` 写到乱码目录的问题，改为写入 `%TEMP%/jingxuan-backend-jacoco.exec`。
- 已将 Testcontainers 的 Ryuk 依赖在测试 JVM 中关闭，消除 Podman rootless 环境下的 `testcontainers/ryuk:0.6.0` 拉取失败。
- 已为集成测试增加可配置镜像前缀能力：
  - 默认仍锁定 `mysql:8.0.42` 与 `redis:7.4.5-alpine`
  - 可通过 `JINGXUAN_TEST_IMAGE_REGISTRY` 覆写镜像前缀
  - 当前机器使用 `docker.m.daocloud.io` 完成镜像预热和 `mvn verify`

#### 当前状态

- 阶段 0 的仓库内改造和全量门禁验证已完成。
- 下一步可以建立中文本地基线提交，并切入阶段 1 的工程与契约基础改造。

### 后续阶段

见 `docs/refactor/feature-matrix.md` 与线程内已确认的 v2 实施计划。只有矩阵全部“已验证”后才能宣告目标完成。

### 阶段 1：工程与契约基础

- [x] 完成目标版本兼容性核验，见 `docs/refactor/stage1-compatibility.md`。
- [x] 建立首批 ADR：
  - `ADR-001` 模块化单体
  - `ADR-002` API 契约优先
  - `ADR-003` 认证与 RBAC
  - `ADR-004` 数据库删除语义
  - `ADR-005` 前端工程化架构
  - `ADR-006` 双部署链路
  - `ADR-007` 实时排行榜
  - `ADR-008` 生产 HTTP 风险接受
- [x] 完成后端工程基础第一刀：
  - `Spring Boot 4.1.0`
  - `MyBatis-Plus Boot4 3.5.17`
  - `Springdoc 3.0.3`
  - `Flyway Core`
  - `Spring Modulith 2.1.0` 依赖与 `@Modulithic` 骨架
  - `ApplicationModules` smoke test
- [x] 开始实际版本升级与工程骨架迁移。

#### 2026-07-11 阶段 1 后端骨架验证

- `mvn -f backend/pom.xml -B -ntp clean compile` 通过。
- `mvn -f backend/pom.xml -B -ntp clean test` 通过。
- `mvn -f backend/pom.xml -B -ntp clean verify` 通过。
- `npm run backend:openapi:export` 通过，并生成 `backend/target/openapi/openapi.json`。
- 迁移过程中额外解决了以下兼容问题：
  - Spring Boot 4 的 `TestRestTemplate` 拆分到 `spring-boot-resttestclient` / `spring-boot-restclient`
  - MyBatis-Plus Boot4 下 `IService` / `ServiceImpl` 包路径迁移到 `com.baomidou.mybatisplus.spring.service`
  - `PaginationInnerInterceptor` 需要显式引入 `mybatis-plus-jsqlparser`
  - Spring Boot 4 的 `ErrorController` 包路径迁移到 `org.springframework.boot.webmvc.error`
  - Spring 管理的 Jackson Bean 改为 `tools.jackson` 命名空间
  - JaCoCo 对 `jsqlparser` 超大方法插桩失败，已通过排除 `net/sf/jsqlparser/**` 解决

#### 2026-07-11 阶段 1 数据库与契约脚手架

- 已引入 Flyway 基线迁移 `db.migration.V1__Baseline`，通过 Java migration 顺序执行现有已验证 SQL 基线。
- 已将根目录 `sql/` 作为只读 legacy 资源打包到 `classpath:legacy-sql/`，供过渡迁移复用。
- `application-test.yml` 已关闭 Flyway，保持 Testcontainers + `@Sql` 集成测试稳定。
- 已为 OpenAPI 导出增加 Maven profile `openapi-export`：
  - 使用 H2 MySQL 兼容模式启动应用
  - 在 `http://localhost:18080/v3/api-docs` 拉取文档
  - 输出到 `backend/target/openapi/openapi.json`
- CI 后端 JDK 基线已从 17 对齐到 21，避免与当前 `pom.xml` 偏离。

#### 2026-07-11 阶段 1 前端契约生成链

- 已引入 Orval 8 与 TanStack Vue Query 5，建立 `OpenAPI → Axios 客户端 → DTO → Vue Query hooks` 生成链。
- 生成代码统一写入 `frontend/src/shared/api/generated`，并通过 `frontend/src/shared/api/http.ts` 复用现有认证与错误拦截器。
- 旧页面暂不切换到生成客户端；阶段 2 起按业务闭环逐步迁移，禁止新增页面直接调用底层 Axios。
- 根命令新增：
  - `npm run api:generate`：导出 OpenAPI 并生成前端客户端。
  - `npm run api:check`：重新生成并检查提交的客户端是否与契约一致。
- CI 新增独立“API 契约一致性”任务，同时配置 JDK 21 与 Node 24，阻止契约漂移进入主分支。
- 修正 HTTP bearer 安全方案中无效的 `name` 字段，使导出的 OpenAPI 3.1 文档通过 Orval 校验。
- `npm run verify:frontend` 已通过：ESLint、类型检查、Vitest 56/56 与生产构建均成功。

#### 当前工具链差异

- 目标基线仍然是 `JDK 25 LTS`。
- 当前本机 Maven 运行时仍为 `Java 21.0.7`；本轮验证已确认 Spring Boot 4.1 可在 Java 21 上完成迁移与测试。
- 后续需要在具备 JDK 25 的环境中把 `java.version` 从 21 提升到 25，并重跑同一套门禁。

#### 当前已确认的 Modulith 结构问题

- 现有代码能够被 `ApplicationModules.of(Application.class)` 识别。
- 一旦执行 `verify()`，会立即暴露大量历史边界问题：
  - `modules/*` 对 `mapper/*` 的直接依赖
  - adapter/facade 直接注入 Mapper
  - 安全层反向依赖业务和 mapper
  - `auth`、`controller`、`mapper` 与 `modules` 之间存在循环依赖
  - 多处字段注入
- 这说明 Modulith 骨架已经生效，但正式边界校验要等阶段 2 之后按模块逐步收敛。

#### 2026-07-11 目标模块边界锚点

- 新增 9 个目标模块包，并使用 `@ApplicationModule` 显式登记：
  `identityaccess`、`referencedata`、`campaign`、`portfolio`、`evaluation`、
  `communication`、`moderation`、`operationsreporting`、`workflow`。
- 新增 `ApplicationModulesTest` 断言，确保上述目标模块持续可被发现。
- 本增量不移动旧业务代码、不改变旧 API；迁移完成前不启用全量 `verify()`，避免把历史耦合一次性引入不可运行状态。
- 详细边界决策记录在 `docs/decisions/ADR-009-目标模块包边界.md`。
- `mvn -f backend/pom.xml -B -ntp -DskipTests compile` 与目标模块发现测试均通过。

#### 2026-07-11 v1 Problem Details 基础

- 新增 `@V1Api` 标记：标记后的 Controller 自动挂载 `/api/v1` 前缀，并使用 v1 专用错误处理器。
- 新增 RFC Problem Details DTO，扩展 `code`、`requestId` 和 `fieldErrors` 字段。
- 新增 `RequestIdFilter`，为请求生成或透传 `X-Request-Id`，并写回响应头。
- 新增 v1 专用异常映射：业务异常、认证/授权、资源不存在、参数校验和未知异常均返回统一结构；旧接口仍由 `Result` 处理。
- 新增 3 个契约测试；`npm run backend:test:unit` 全部通过。

#### 2026-07-11 identity-access 首个垂直切片

- 新增 `/api/v1/auth/login`、`/api/v1/auth/me`、`/api/v1/auth/logout`，Controller 只编排现有 `AuthService`，没有复制认证逻辑。
- v1 登录响应使用 `V1LoginResponse` / `V1UserInfo`，雪花 ID 在 DTO 层转换为不透明字符串；注销返回 HTTP 204。
- OpenAPI 导出和 Orval 客户端已重新生成，前端类型检查通过。
- 当前旧服务尚未提供 refresh token 能力，本切片不伪造 refresh 接口；待认证会话基础设施完成后再补齐刷新轮换。
- 曾发现并修复 `@V1Api` 元注解路径不会与 Controller 路径自动拼接的问题，现由 Controller 明确声明完整 `/api/v1/...` 路径。

#### 2026-07-11 v1 refresh 会话基础设施

- 新增 Redis refresh token 服务：客户端持有随机不透明令牌，Redis 仅保存 SHA-256 哈希键和最小会话载荷。
- 默认 refresh 会话 8 小时，“记住我”30 天；每次轮换使用 Redis `getAndDelete` 消费旧令牌，重复使用直接失败。
- v1 access token 独立配置为 15 分钟，旧接口的 24 小时 JWT 配置保持兼容。
- 新增 `/api/v1/auth/refresh`，注销可同时撤销 refresh token；登录/刷新响应均提供 access 与 refresh 令牌。
- `/api/v1/auth/login` 和 `/api/v1/auth/refresh` 已加入匿名路径白名单。
- Redis refresh 服务、认证 Controller 和前端生成客户端均已测试/同步；正式 Redis 集成测试仍需在 Testcontainers Redis 环境执行。

#### 2026-07-11 refresh token Redis 集成验证

- 新增 `V1AuthApiTest`，通过 Testcontainers MySQL 8 与 Redis 7 发起真实 HTTP 登录、刷新和重放请求。
- 已验证 refresh token 轮换后旧令牌会返回 `401 UNAUTHENTICATED`。
- 修正 `SecurityConfig`：v1 登录和刷新路径必须在 Spring Security 的 `permitAll` 规则中显式声明，仅配置 JWT 过滤器忽略路径不足以放行请求。
- 运行 `$env:DOCKER_HOST='npipe:////./pipe/docker_engine'; $env:JINGXUAN_TEST_IMAGE_REGISTRY='docker.m.daocloud.io'; mvn -f backend/pom.xml -B -ntp "-Dit.test=V1AuthApiTest" -DskipUnitTests=true verify` 通过。

#### 2026-07-11 reference-data 只读闭环

- 新增 `/api/v1/classes`、`/api/v1/dictionaries/{type}`、`/api/v1/tags`。
- 班级继续从过渡期 `sys_dict` 的 `class` 类型读取；输出统一转换为 v1 字符串 ID DTO。
- 标签查询从旧 Adapter 收敛到 `referencedata.internal.application.ReferenceDataQueryService`，为后续替换 `tag` 表建立单一迁移入口。
- 上述 GET 接口仅提供注册与公开筛选所需的参考数据，已显式配置为匿名可读。
- 单元测试、OpenAPI/Orval 生成、前端类型检查通过；Testcontainers HTTP 测试验证班级/标签可公开访问且返回字符串 ID。
- 字典/标签写操作和删除影响清单尚未迁移，仍保留在后续独立切片。
