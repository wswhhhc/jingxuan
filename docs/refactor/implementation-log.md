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
- [ ] 完成阶段 0 全量验证并建立本地基线提交。

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
