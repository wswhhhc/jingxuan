# 菁选 v2 全量重构完成度审计

审计日期：2026-07-11  
审计基线：用户提供的《菁选校园作品展示平台 v2 全量重构计划》  
分支：`refactor/v2`

## 结论

当前仓库已完成原计划阶段 0–1 的仓库内工程、安全网、契约与部署基础，并完成 identity-access Task 1–2、密码/AI 加固等阶段 2 切片；但它仍不是“接近完成的 v2”。后端多数 v1 Controller 仍代理旧 Service/Facade，前端业务页面尚未全面消费生成客户端，目标数据模型、物理删除、实时排行、迁移发布和遗留清零均未形成闭环。

## 阶段完成度

| 原计划阶段                         | 当前判断   | 已有证据                                                                  | 主要未完成项                                                                                                                          |
| ---------------------------------- | ---------- | ------------------------------------------------------------------------- | ------------------------------------------------------------------------------------------------------------------------------------- |
| 阶段 0：冻结与安全网               | 仓库内完成 | JDK 25/Node 24 统一门禁、Testcontainers/CI 接线、安全冒烟、可靠覆盖率防回退、前后端全量验证 | 当前机器无 Docker daemon，真实 Compose/Testcontainers 复跑与最终 80/80/70 覆盖率目标仍待后续环境和持续补测                         |
| 阶段 1：工程与契约基础             | 完成       | Boot 4.1、MP 3.5.17、Modulith 2.1、Springdoc、Flyway、OpenAPI/Orval、ArchUnit、持久化事务事件、9 份 ADR | Modulith/ArchUnit 仍冻结历史桥接白名单，需随业务迁移逐项清零，而非新增债务                                                            |
| 阶段 2：身份权限与基础数据         | 部分完成   | Refresh family/Lua、Cookie/Origin、BCrypt 12、初始密码与 AI 导入加固、标签/基础数据只读、Problem Details | Redis 通用限流与 challenge、旧哈希透明升级、教师待审核审批、权限码/portalType、新表、动态菜单和前端内存 session 尚未完成               |
| 阶段 3：批次、待办和作品           | 少量切片   | 批次/待办只读、待办完成、作品草稿/详情/编辑/提交适配接口                  | 新数据模型、幂等发布、乐观锁、FileStorage、事务工作流、学生页面迁移均未完成                                                           |
| 阶段 4：审核、发布、互动和删除     | 契约适配   | 审核、发布/下线/精选、评论创建/删除、幂等点赞、删除申请入口               | 队列/历史完整契约、评论分页/限流、物理级联删除、可靠文件清理、用户影响清单与公开页面迁移未完成                                        |
| 阶段 5：评分、排行、奖品和剩余模块 | 极少量切片 | 教师评分 v1 PUT 入口                                                      | 评分历史/CSV、窗口函数排行、generation 缓存、奖品快照、通知公告、审核、日志报表及对应页面均未迁移                                     |
| 阶段 6：移除遗留实现               | 未开始     | 无                                                                        | 旧 Controller/Adapter/Entity/Mapper/Service、`Result`、角色型路由、手写 Axios、重复 SQL 全部仍在                                      |
| 阶段 7：性能、安全与发布候选       | 未开始     | Actuator 依赖与基础 requestId                                             | k6、查询数门禁、预算检查、Lighthouse、JSON 日志、Micrometer RED、Prometheus/Grafana、安全加固均不完整                                 |
| 阶段 8：迁移与切换                 | 未开始     | 无                                                                        | 迁移 CLI、两次脱敏演练、清理/回滚产物、Release workflow、双模式自动部署和失败回滚均缺失                                               |

## 可执行基线

| 门禁                               | 结果                                                                                                                       |
| ---------------------------------- | -------------------------------------------------------------------------------------------------------------------------- |
| 前端 lint / 类型 / 单元测试 / 构建 | 通过；14 个文件、134 个用例，生产构建成功                                                                                  |
| 后端单元测试                       | 通过；JDK 25 干净运行 387 个用例                                                                                           |
| 前端覆盖率                         | Statements 28.53%、Branches 28.74%、Functions 20.88%、Lines 30.02%；仍低于最终 80/70 目标                                 |
| 后端覆盖率                         | 单次 unit-only XML：Line 43.3005%、Method 43.1034%、Branch 35.7275%；仍只代表防回退基线，最终目标 80%/80%/70%             |
| API 生成一致性                     | 通过；25 个 v1 路径、40 个生成文件、30 个 v1 URL 完全一致                                                                 |
| 前端包预算                         | 通过；公共入口 84.28KB、作品列表首屏 201.93KB、初始 CSS 11.70KB gzip                                                      |

此前审计记录的后端 63.36%/68.14%/44.27% 来自未清理且默认追加的 `%TEMP%/jingxuan-backend-jacoco.exec`，并出现 class mismatch。该组数值仅用于保留问题发现的历史背景，不是当前可靠基线；当前口径要求 `clean` 删除 exec、`append=false`，且仅统计单次 unit-only 会话生成的 XML。当前 Task 2 收口后的可靠 XML 为 Line 43.3005%、Method 43.1034%、Branch 35.7275%。

## 高风险缺口

1. `application.yml` 仍使用枚举序号和全局逻辑删除，数据语义与目标方案相反。
2. Access Token 仍进入浏览器存储，`/logout-all` 尚未实现；登录限流/challenge、教师待审核和前端 Cookie 恢复会话仍未形成闭环。
3. 新模块仍通过冻结白名单桥接旧 Mapper/Service；Modulith/ArchUnit 门禁能阻止新增债务，但历史边界尚未清零。
4. Flyway 已成为运行时建表入口且资源随后端自包含；当前机器无 Docker daemon，空 MySQL、Redis family 并发与 Compose 实跑需在可用环境复验。
5. 文件上传直接落盘，没有 SHA-256、临时生命周期、抽象存储和事务后可靠清理。
6. 排行缓存仍使用固定 key 与 Redis `KEYS`，评分事务内刷新缓存，存在回滚后不一致风险。
7. AI 导入限流仍是 Caffeine 单进程适配器；登录、邮件验证码、游客评论和 AI 导入尚未统一到 Redis 多实例限流与一次性算术 challenge。

## 实施顺序

1. 按 `identity-access-v2.md` 继续完成 Redis 限流/challenge/旧哈希升级、教师审批、`/logout-all` 和前端内存 session。
2. 完成 identity-access/reference-data/campaign 的目标新表、新权限和完整前后端垂直闭环。
3. 完成 portfolio/workflow/FileStorage，再迁移审核、发布、互动与物理删除。
4. 完成 evaluation、communication、moderation、operations-reporting。
5. 前端迁入 `app/features/shared`，统一 `WorkspaceShell`，删除页面直调 Axios并补 URL 状态、MSW、Playwright。
6. 删除全部遗留实现，收紧 Modulith/ArchUnit 白名单并启用遗留扫描。
7. 完成性能、安全、可观测性、迁移 CLI、双部署 Release workflow 与本地演练。

## 外部验收边界

仓库内可以完成代码、脚本、测试、容器化演练和发布工作流。真实生产维护窗口、删除旧生产数据库/文件、GitHub `production` 人工批准、SSH 部署和校园网验收需要生产凭据与明确执行授权，不能在本地审计阶段擅自进行。
