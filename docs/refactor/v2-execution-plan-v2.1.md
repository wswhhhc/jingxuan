# 菁选 v2 全量重构执行方案 v2.1

制定日期：2026-07-12
当前 HEAD：9a29e8
当前完成度：约 31%
本机环境：JDK 21（不是 25）、Node 24、MySQL 8、Redis 7（无 Docker daemon）
Git 远端：origin https://github.com/wswhhhc/jingxuan-.git（当前不可达）
执行方式：自动门禁驱动，通过即继续

---

## 与上一版方案（v2-execution-plan-2026-07-12.md）的关键调整

1. **JDK 21 替代 JDK 25**：本机 JDK 为 21.0.7，POM 使用 elease 21。正式环境若需要 JDK 25 可后续升级，但本机开发和 CI 匹配本机 JDK 21。
2. **Git 远端不可达时独立工作**：在 origin/master 可 fetch 之前，所有提交在本地 efactor/v2 分支积累，首推时再重建血缘关系。
3. **本机无 Docker daemon**：Docker Compose 配置保持完整但不在本机验证；Docker 相关的 Testcontainers 测试标记为条件跳过。
4. **阶段 A（恢复绿色基线）拆分为更细的检查点**：确保每 2-3 个任务后就有可运行证据。

---

## 阶段划分总览

| 阶段 | 内容 | 预计任务数 | 依赖 |
|------|------|-----------|------|
| A | 恢复绿色基线（编译、测试、格式、契约） | 12 | 无 |
| B | 身份权限闭环补齐 | 8 | A |
| C | 批次/待办/作品闭环补齐 | 10 | B |
| D | 审核/发布/互动/删除闭环补齐 | 8 | C |
| E | 评分/排行/奖品/剩余模块补齐 | 8 | D |
| F | 遗留清理与质量收口 | 6 | E |
| G | 迁移/部署/生产切换 | 7 | F |

---

## 阶段 A：恢复唯一可信绿色基线（12 个任务）

### A1. 修复 POM JDK 版本（release 21）
- 将 pom.xml 中 <release>25</release> → <release>21</release>
- 确认 maven-compiler-plugin 版本兼容 JDK 21
- 确认 maven-surefire-plugin 和 jacoco-maven-plugin 无 JDK 兼容问题
- **验收**：mvn clean compile -q 成功
- **验证**：运行后端编译

### A2. 修复后端编译错误（12 个编译错误）
- 逐文件修复因迁移缺口导致的编译错误
- 确认 valuation 模块的 RankService/RankVO 等引用正确
- 确认所有模块间依赖和 spring-modulith-starter-jdbc 依赖正确
- **验收**：mvn clean compile -q 成功，366 个源文件全部通过

### A3. 修复后端测试编译与运行
- 确保 mvn clean test-compile 全部通过
- 排除因缺少 Docker 的 Testcontainers 测试
- 运行完整后端单元测试
- **验收**：后端单元测试全部通过（预期 320+ 项）
- **验收门禁**：mvn clean test -Dtest="com.jingxuan.modules.**" 全绿

### A4. 修复前端 lint/typecheck
- 修复 WorkList.vue 未使用类型问题
- 确保 
pm run lint 通过
- 确保 
pm run typecheck 通过
- **验收**：lint + typecheck 全绿

### A5. 修复前端测试（21 个失败）
- 修复 6 个测试文件中的 21 个失败测试
- 修复 3 个未处理网络错误
- 确保 MSW handlers 覆盖正确的 API 路径
- **验收**：
pm run test 78 项全部通过

### A6. 恢复 OpenAPI/Orval 契约一致性
- 运行 
pm run api:export 生成当前 OpenAPI YAML
- 运行 
pm run api:generate 重新生成 Orval 客户端
- 运行 
pm run api:check 验证一致性
- 修复 YAML 与 Controller 之间的漂移
- **验收**：OpenAPI 契约全部通过

### A7. 修复格式化与 CRLF 基线
- 运行 
pm run format 修复 Prettier 漂移
- 添加 .gitattributes 强制 Shell/YAML/JS 使用 LF
- **验收**：
pm run format:check 通过

### A8. 修复工具链门禁
- 修复 erify-toolchain.mjs 的模块导出和 Windows 兼容性
- 修复覆盖率元测试
- 修复冒烟元测试
- **验收**：toolchain/coverage/smoke 元测试全部通过

### A9. 建立根 verify 命令
- 创建或修复 
pm run verify:quick 串联：格式、lint、类型、构建、单元测试
- **验收**：
pm run verify:quick 全绿

### A10. 修复依赖审计
- 修复 Axios 生产依赖漏洞（如有）
- 确认 ESLint Vue parser 配置正确
- 确认 Maven 无重复 mail 依赖
- **验收**：
pm audit --production 零漏洞，Maven dependency:tree 无冲突

### A11. Flyway 对齐
- 确认 Flyway 迁移脚本与当前 Schema 一致
- 删除重复的 SQL 文件
- **验收**：Flyway 迁移可应用到空数据库

### A12. 阶段 A 检查点
- 更新 	ask_plan.md 和 progress.md
- 中文原子提交
- 后端 mvn clean test（单元测试）
- 前端 
pm run verify:quick
- 根 erify 串联
- **门禁**：以上全部通过

---

## 阶段 B：身份权限闭环补齐（8 个任务）

### B1. 确认认证/RBAC 后端完整性
- 审计现有 Controller：登录、刷新、注销、注册、教师审批
- 确认 Refresh Cookie 安全属性（HttpOnly、SameSite=Strict、Path）
- 确认 BCrypt cost 12、PENDING_APPROVAL 逻辑
- 确认 Redis 限流（登录 5/20、算术 challenge、AI 导入）
- **验收**：所有认证路径有测试覆盖

### B2. 补齐缺失的认证端点
- /logout-all 端点
- 教师审批端点完整实现
- **验收**：全部认证 REST API 完整

### B3. 确认前端 session 管理
- Access Token 仅保存在内存
- Refresh Cookie 由浏览器自动发送
- 注销时清除内存 Token 并调用注销 API
- **验收**：
pm run test 覆盖 session 管理

### B4. 确认前端动态菜单与布局
- WorkspaceShell 统一布局
- 动态菜单从后端权限码生成
- 三种角色（admin/teacher/student）以及公开端布局正确
- **验收**：角色切换后菜单正确

### B5. 用户管理页面
- 用户列表、创建、编辑、禁用
- AI 用户导入
- **验收**：页面功能完整，使用生成客户端

### B6. 角色权限页面
- 角色列表、创建、编辑、权限分配
- 三种内置角色不可删除或改代码
- **验收**：权限分配生效

### B7. 字典与标签页面
- 字典管理
- 标签管理
- **验收**：CRUD 功能完整

### B8. 阶段 B 检查点
- 全部后端测试通过
- 全部前端测试通过
- OpenAPI 契约一致
- 三种角色端到端验证
- 中文提交

---

## 阶段 C：批次/待办/作品闭环补齐（10 个任务）

### C1. 确认批次/待办后端完整性
- 批次 CRUD
- 评分批次（score_batch）相关表结构
- 学生待办（student_task）发布与完成
- **验收**：所有 API 有测试覆盖

### C2. 确认作品后端完整性
- 作品草稿创建、编辑、提交
- 作品成员、标签、附件
- 作品状态机正确（草稿→已提交→已驳回→已通过）
- **验收**：核心作品流程有测试覆盖

### C3. 提交作品触发待办完成
- 学生提交作品 → 自动标记待办完成
- 审核驳回 → 待办标记已驳回
- **验收**：待办状态与作品状态联动

### C4. 内容审核事件
- 作品提交后触发内容审核事件
- DeepSeek 审核集成
- 通知事件
- **验收**：审核事件链完整

### C5. 学生首页
- 学生首页展示待办、作品状态
- **验收**：页面使用生成客户端

### C6. 作品提交页面
- 创建/编辑作品
- 上传附件
- 提交审核
- **验收**：完整创作流程

### C7. 我的作品页面
- 列表展示已提交、草稿、已通过
- 编辑草稿、重新提交驳回作品
- **验收**：列表和操作完整

### C8. 待办页面
- 待办列表
- 点击待办跳转到对应批次提交
- **验收**：待办驱动工作流

### C9. 作品展示（公开端）
- 展廊页面
- 作品详情页
- **验收**：公开端作品浏览

### C10. 阶段 C 检查点
- 学生"注册→待办→创建作品→上传→提交"全流程
- 全部后端测试
- 全部前端测试
- OpenAPI 一致
- 中文提交

---

## 阶段 D：审核/发布/互动/删除闭环补齐（8 个任务）

### D1. 审核队列与审核历史
- 审核队列展示
- 审核历史记录
- 审核通过/驳回
- **验收**：审核流程完整

### D2. 发布与下线
- 审核通过后发布
- 手动下线
- 精选设置
- **验收**：发布状态切换正确

### D3. 公开展廊与详情
- 已发布作品公开展示
- 作品详情页（含评论、点赞）
- iframe 预览
- **验收**：公开端完整

### D4. 评论树分页
- 评论创建（含游客评论）
- 评论树分页加载
- **验收**：评论功能完整

### D5. 点赞
- 幂等点赞（PUT/DELETE）
- 浏览量聚合
- **验收**：点赞/浏览量正确

### D6. 作品物理删除
- 数据库级联清理
- 文件在事务提交后可靠删除
- **验收**：删除后所有关联数据清空

### D7. 用户物理删除
- 删除前影响清单
- 保护根管理员账号
- **验收**：删除流程安全

### D8. 阶段 D 检查点
- 提交→审核→发布→公开展示→删除申请完整通过
- 无跨模块 Mapper
- 全部测试通过

---

## 阶段 E：评分/排行/奖品/剩余模块补齐（8 个任务）

### E1. 教师评分（Upsert）
- 四维度评分
- 评分提交/修改
- **验收**：评分 API 完整

### E2. 评分汇总与排行
- 实时排行 SQL
- Redis generation key 缓存
- 评分变化立即失效缓存
- **验收**：排行实时更新

### E3. 公示控制
- 公示开关
- 公示只控制可见性
- **验收**：公示切换不影响排行计算

### E4. 奖品发放
- 奖项模板
- 奖品发放快照（固化名次、分数）
- **验收**：发放记录不受后续评分影响

### E5. CSV 导出
- 服务端流式 CSV 导出
- **验收**：导出文件正确

### E6. 公告与通知
- 公告管理
- 个人通知列表
- 通知实时推送（轮询）
- **验收**：通知功能完整

### E7. 敏感审核与日志
- 敏感词规则
- DeepSeek 内容审核
- 操作日志
- **验收**：审核与日志功能完整

### E8. 阶段 E 检查点
- 全部功能矩阵达到 v1 等价
- 实时改分会更新已公示排行
- 全部测试通过
- 中文提交

---

## 阶段 F：遗留清理与质量收口（6 个任务）

### F1. 删除旧 Adapter/Controller
- 删除旧 modules/adapter/ 目录
- 删除旧 Controller（AdminApi、TeacherApi、StudentApi、PublicApi）
- **验收**：CI 扫描确认无旧路由

### F2. 删除根 Entity/Mapper/Service
- 删除旧根级 Entity、Mapper、Service
- 删除 Result 统一响应
- **验收**：无旧实现残留

### F3. 删除重复 SQL
- 删除 sql/ 目录中的重复建表脚本
- Flyway 为唯一 Schema 来源
- **验收**：无重复 SQL

### F4. 前端遗留清理
- 删除旧 API 导入（@/api/admin/* 等）
- 确认所有页面使用生成客户端
- 确认无直接 Axios 调用
- **验收**：前端只有 Orval 客户端

### F5. 覆盖率提升
- 整体行/函数 ≥80%，分支 ≥70%
- 核心模块（认证、作品、审核、评分）≥90%
- **验收**：覆盖率门禁通过

### F6. 阶段 F 检查点
- 后端只剩九模块 v2 实现
- 前端只剩 pp/features/shared
- 全部质量门禁通过
- 中文提交

---

## 阶段 G：迁移/部署/生产切换（7 个任务）

### G1. 修复迁移 CLI
- 修复 scripts/migrate.mjs（当前只输出模拟 SQL）
- 实现 preflight/migrate/verify/rollback/purge 五命令
- **验收**：迁移脚本可用

### G2. 修复 Docker Compose
- 确保 Docker Compose 配置与当前版本匹配
- JDK 21、Node 24
- **验收**：docker compose up 可启动（需要 Docker daemon）

### G3. 修复 PM2 + Nginx 配置
- 确保 PM2 配置与当前版本匹配
- 确保 Nginx 配置正确
- **验收**：配置语法正确

### G4. GitHub Actions 修复
- 修复 CI 配置（JDK 21、Node 24）
- 修复 Release workflow
- 确保所有门禁串联
- **验收**：CI 配置文件完整

### G5. 生产部署准备
- SSH 连接验证
- GitHub Secrets 配置
- 数据库凭据
- **验收**：生产可部署

### G6. 迁移演练
- 从正式环境制作脱敏迁移源
- 完整执行维护、迁移、校验、回滚流程
- **验收**：2-4 小时窗口内通过

### G7. 正式切换与观察
- 获取生产并发锁
- 进入维护模式
- 执行迁移
- 完成切换
- 观察 24 小时
- 最终 purge
- **验收**：切换成功，功能正常

---

## 执行顺序说明

1. **先做 A1-A12**：恢复绿色基线是后续所有工作的前提
2. **B1-B8**：身份权限是其他模块的基础
3. **C1-C10**：作品流程是核心业务
4. **D1-D8**：审核和互动依赖作品流程
5. **E1-E8**：评分排行依赖作品和审核
6. **F1-F6**：遗留清理在全部功能就绪后做
7. **G1-G7**：迁移和部署在最后

每个任务都会：
- 先确认当前状态
- 执行变更
- 验证通过后提交
- 更新进度

---

## 完成标准

- 功能矩阵全部验证
- 后端只剩九模块 v2 实现
- 前端只剩 pp/features/shared
- Flyway 唯一 Schema
- 物理删除、实时排行、发奖快照全部生效
- 覆盖率、E2E、性能、bundle、Lighthouse、安全门禁通过
- 两次演练完成
- 正式生产切换完成

---

## 自动停止条件

以下情况自动停止并优先修复：
- clean build 失败
- 契约测试失败
- 数据不一致
- 生产部署后健康检查失败
