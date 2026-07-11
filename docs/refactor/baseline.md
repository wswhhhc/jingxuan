# v1 重构基线

记录日期：2026-07-11

## 当前规模

- 前端：40 个 Vue SFC、52 个 TypeScript 文件；存在多个 500–1200 行单体页面。
- 后端：约 190 个主代码 Java 文件；根级实体/Mapper/Service 与 `modules/*` 并存。
- HTTP：约 130 个 Handler，包含角色前缀、兼容别名和重复通知路径。
- 数据库：约 27 张业务表；无 Flyway、无外键，根 SQL、后端 SQL 与测试 Schema 存在漂移。

## 已复现的基线问题

- `npm run test`：10 个测试文件、56 个用例通过。
- `npm run build`：通过；Element Plus JavaScript chunk 约 220KB gzip，ECharts chunk 约 178KB gzip。
- 阶段 0 修复后的 `npm run lint`、`npm run typecheck`、`npm run format:check` 均通过且为零告警。
- 前端覆盖率可信基线：语句 24.28%、分支 25.10%、函数 17.89%、行 25.57%；CI 先禁止回退，后续逐模块提升至发布门槛。
- 后端单元测试已稳定为 151/151 通过；JaCoCo 执行文件改写到 `%TEMP%/jingxuan-backend-jacoco.exec`，规避 Windows 中文路径乱码。
- API 集成测试已从本机 `jingxuan_test` 迁移到 Testcontainers，并在当前 Podman 环境下验证通过。
- 集成测试镜像默认仍锁定 `mysql:8.0.42` 与 `redis:7.4.5-alpine`，必要时可通过 `JINGXUAN_TEST_IMAGE_REGISTRY` 覆写镜像前缀；当前机器使用 `docker.m.daocloud.io` 预热并完成 `mvn verify`。
- Maven 模型重复声明 `spring-boot-starter-mail`。
- CI 仅覆盖部分后端包，未执行前端 lint、类型、测试和构建。
- 主配置曾包含固定 JWT 密钥；已开始改为环境变量强制注入。

## 运行时取证

2026-07-11 对本机已运行的 v1 开发环境仅执行了公开页面和只读接口检查，没有运行会创建作品的旧冒烟脚本。

- [公开作品展廊截图](../../output/playwright/v1-public-works.png)
- [公开排行榜截图](../../output/playwright/v1-public-ranking.png)
- [学生登录页截图](../../output/playwright/v1-student-login.png)
- 公开作品、班级、标签接口均返回 HTTP 200。
- 作品展廊出现 6 个封面资源 HTTP 500，说明数据库引用与本地 uploads 已存在漂移；迁移校验必须逐文件检查大小与 SHA-256。
- 现有路由与权限入口见 [v1 路由与权限清单](routes-and-permissions.md)。

## 主要性能证据

- 作品分页对每条记录再次查询提交者、批次、发布信息和成员数，形成 N+1。
- 公开作品列表先读取全部发布记录，再逐作品查询成员、标签和点赞。
- 评论分页仍读取作品下全部回复。
- 仪表盘在 Java 中读取全表聚合。
- 教师导出在浏览器逐页串行请求，学生首页拉取作品列表后在浏览器统计。
- 排行缓存清理使用 Redis `KEYS`。

## 基线保护原则

- 阶段 0 只修复质量工具、测试环境、敏感配置和 CI，不改变业务行为。
- 每次增量修改后只运行受影响测试；完成检查点时运行全量门禁。
- 生产数据库、Redis 和 uploads 在实现阶段只读；任何清理仅由最终迁移 CLI 在维护窗口执行。
