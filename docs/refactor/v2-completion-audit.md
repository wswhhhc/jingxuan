# 菁选 v2 全量重构完成度审计（当前 HEAD）

审计日期：2026-07-12

审计基线：`refactor/v2` / `9e33882`

验收基线：用户提供的《菁选校园作品展示平台 v2 全量重构计划》

结论：**总体完成度约 31%（误差 ±3%），剩余约 69%**。

> 该完成度不按文件数量或提交标题计分。只有实现已经接入、契约一致且有可执行证据，才可获得完整分值。当前 9 个原计划阶段均未达到各自发布检查点。

## 计分方法

- `0%`：不存在或与目标相反。
- `25%`：只有文档、骨架、测试草稿或孤立组件。
- `50%`：已有实现，但未接入主流程、遗留实现仍主导或验证失败。
- `75%`：主流程已接入，仍缺完整集成/E2E/生产证据。
- `100%`：目标行为、自动门禁和对应检查点全部通过。

| 原阶段 | 权重 | 当前完成度 | 加权分 | 当前证据与主要缺口 |
|---|---:|---:|---:|---|
| 0 冻结与安全网 | 8% | 40% | 3.20 | 有矩阵、ADR、Testcontainers 和安全脚本；但 Git 血缘不真实、当前构建不绿、toolchain/coverage/smoke 元门禁失败 |
| 1 工程与契约基础 | 12% | 45% | 5.40 | Boot 4.1、MP 3.5.17、Modulith 2.1、Springdoc、OpenAPI/Orval 已建；JDK/CI/Docker 回退，后端不可编译，契约 7/22 |
| 2 身份权限与基础数据 | 15% | 30% | 4.50 | Refresh family、限流、challenge 等代码/测试存在；HEAD 仍使用 JSON refresh、Web Storage、角色鉴权，教师审批与规范化模型未闭环 |
| 3 批次、待办和作品 | 14% | 38% | 5.32 | v1 入口与旧业务桥接较多；最终 Schema、乐观锁、幂等、workflow 接管和前端迁移未完成 |
| 4 审核、发布、互动和删除 | 14% | 39% | 5.46 | 审核/发布/评论/点赞/删除入口存在；物理级联、可靠文件事件、回复分页、游客风控和完整 E2E 未完成 |
| 5 评分、排行、奖品及剩余模块 | 14% | 29% | 4.06 | 评分/排行/奖品/通知/审核/报表已有 v1 外壳；窗口排行、generation cache、奖品快照、CSV、事件化和页面迁移缺失 |
| 6 移除遗留 | 8% | 11% | 0.88 | 有 DTO 内迁提交；仍有 10 个旧 Adapter、25 个根 Entity、25 个根 Mapper、49 个 `Result` 文件和三套 SQL |
| 7 性能、安全与发布候选 | 9% | 19% | 1.71 | 有 bundle/k6/Lighthouse/monitoring 草案；未接 CI，无 300 并发报告、覆盖率目标、RED 指标、JSON 日志和真实可访问性门禁 |
| 8 迁移与切换 | 6% | 9% | 0.54 | 有模拟迁移脚本和手工 runbook；无真实五命令 CLI、演练、Release workflow、生产凭据链路或可验证回滚 |
| **合计** | **100%** |  | **31.07%** | **四舍五入为 31%** |

## 当前可执行证据

| 门禁/检查 | 当前结果 |
|---|---|
| `npm run verify:quick` | 失败：9 个已提交文件不符合 Prettier |
| 前端 lint/typecheck | 失败：`WorkList.vue` 两个未使用类型 |
| 前端 Vitest coverage | 78 项中 57 通过、21 失败，另有 3 个未处理网络错误；失败运行覆盖率约 S25.05/B26.13/F17.92/L26.23 |
| 后端 `mvn clean test` | 失败：367 个生产源码编译阶段出现 12 个错误，当前无法重建 JAR/OpenAPI |
| OpenAPI 语义元测试 | 7/22 通过；提交 YAML 与 HEAD Controller 双向漂移 |
| Toolchain / coverage / smoke 元测试 | 0/1、0/3、1/23 |
| 生成客户端/Bundle 工具自身单测 | 3/3、3/3；但没有接入根验证和 CI |
| 前端生产依赖审计 | 0 个漏洞 |
| 正式环境只读检查 | 未完成：高概率候选主机 SSH/HTTP 超时，无法确认现网身份和状态 |

## 关键事实

1. **当前 HEAD 不是绿色基线。** 最近阶段 6 提交把目标测试、DTO 和部分模块代码提交进来，但遗漏了依赖、枚举、类型和生产实现。
2. **Codegraph 索引漂移。** 索引实际对应不可达 WIP `d7ff9ed` 的部分内容；关键结论已改用 Git HEAD 和实跑验证。
3. **WIP 可恢复但不可整体合并。** 已创建 `codex/recovery-wip-d7ff9ed` 保护引用。该 WIP 包含高价值实现，也包含确定的源码损坏，只能逐文件/逐 hunk 取证恢复。
4. **Git 血缘不合格。** 本地 `refactor/v2` 是独立根历史，与远端 `master` 不共享对象；v2 未推送，GitHub 无 production Environment、Secrets、Variables 或分支保护。
5. **模块化仍是外壳。** 九个模块都有文件，但仍约有 128 条旧包依赖；ArchUnit/Modulith 没有证明目标边界已经清零。
6. **前端仍由旧架构主导。** 50 个文件导入旧 API，仅 5 个导入手写 v1 适配器、3 个引用生成客户端；Vue Query hooks 页面消费为 0，`features/` 为空。
7. **Flyway 尚未成为唯一 Schema 来源。** Java V1 继续执行 14 个 legacy SQL，根/后端/classpath SQL 并存，V5 与测试版本断言漂移。
8. **生产迁移和部署只是草案。** `migrate.mjs` 不连接数据库，runbook 与代码能力冲突；Compose、Nginx、Docker、PM2 和 GitHub Release 均未形成自动回滚闭环。

## 子系统审计结果

- 后端 v2 完成度：约 **37%**。
- 前端 v2 完成度：约 **29%**。
- 基础设施：阶段 0/1/7/8 分别约 **58% / 62% / 25% / 12%**（资产存在度；并不代表可发布）。
- 当前发布状态：**阻断**。后端不能编译、前端质量门禁失败、生产身份未核实。

## 版本结论

主版本目标无需降级：Spring Boot 4.1 支持 Java 17–26；MyBatis-Plus Boot4 3.5.17 与 Spring Modulith 2.1.0 仍为当前正式版；typescript-eslint 当前支持 TypeScript `<6.1.0`，所以 TypeScript 6.0 可保留。新版执行统一采用 JDK 25 和 Node 24 LTS，并修复当前仓库的 21/17/20 漂移。

官方依据：

- <https://docs.spring.io/spring-boot/system-requirements.html>
- <https://repo1.maven.org/maven2/com/baomidou/mybatis-plus-spring-boot4-starter/maven-metadata.xml>
- <https://docs.spring.io/spring-modulith/reference/events.html>
- <https://nodejs.org/dist/index.json>
- <https://typescript-eslint.io/users/dependency-versions/>

## 新执行方案

完整方案见 [v2-execution-plan-2026-07-12.md](./v2-execution-plan-2026-07-12.md)。方案从“恢复唯一可信绿色基线”开始，之后按身份、作品、评分等纵向闭环迁移，最终执行两次迁移演练和正式自动切换。
