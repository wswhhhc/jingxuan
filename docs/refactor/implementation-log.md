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
