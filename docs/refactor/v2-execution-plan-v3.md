# 菁选 v2 全量重构执行方案 v3

> 制定日期：2026-07-12 | 基线 HEAD：ad563e0 | 分支：refactor/v2
> 本机环境：JDK 21、Node 24 LTS、npm 11、无 Docker daemon、无 MySQL/Redis 运行
> Git 远端：不可达（本地积累，手动推送）
> 门禁策略：自动通过即继续，无需人工审批
> 完成度：约 30%（架构骨架就绪，业务/前端/部署尚未落地）

---

## 与 v2.1 的关键调整

1. **阶段 A 重排**：先修复 Orval/OpenAPI 管道，然后再补其他门禁——因为生成客户端是前端所有重构的前提。
2. **阶段 B-F 按业务模块垂直切分**：不再把"后端所有模块"和"前端所有页面"分开做，而是按业务域逐个垂直落地（后端 + 对应前端页面 + 测试一次完成）。
3. **生产部署最后做**：G 阶段需要你提供凭据，在此之前所有代码都交付在本地分支。
4. **每个阶段结束时都有可运行的"步行骨架"**：不用等到全部做完才看到效果。

---

## 阶段划分

| 阶段 | 内容 | 预计任务数 | 产出物 |
|------|------|-----------|--------|
| A | 绿色基线恢复 + Orval 管道打通 | 8 | 全绿 CI 门禁 + 生成客户端可用 |
| B | 身份权限垂直落地 | 6 | 登录/注册/角色/用户管理完整 |
| C | 批次/待办/作品垂直落地 | 8 | 学生完整创作流程 |
| D | 审核/发布/互动/删除垂直落地 | 6 | 公开展廊完整 |
| E | 评分/排行/奖品/剩余模块垂直落地 | 6 | 全部功能 v1 等价 |
| F | 遗留清理 + 质量收口 | 4 | 只剩 v2 实现 |
| G | 迁移/部署/生产切换 | 5 | 正式环境上线 |

---

## 阶段 A：绿色基线恢复 + Orval 管道打通（8 个任务）

### A1. 修复 Orval 生成管道
**现状**：rontend/orval.config.ts 使用 	ags-split mode，但 openapi/jingxuan-v1.yaml 没有 tags，导致 Failed to resolve input。
**操作**：
1. 在 OpenAPI YAML 的每个 path 中添加 	ags（按模块分：Auth、Users、Roles、Campaign、Portfolio、Evaluation 等）
2. 或修改 orval.config.ts 的 mode 为 single 先跑通
3. 运行 
pm run api:generate 验证生成
**验收**：rontend/src/shared/api/generated/ 下有正确的 endpoints + models

### A2. 修复前端测试基线
**现状**：WorkList.test.ts 因生成客户端 import 断裂失败。
**操作**：
1. 确认生成客户端就位后修复 import
2. 运行全部前端测试通过
**验收**：
pm run test 全绿

### A3. 建立根 verify 门禁
**操作**：
1. 在根 package.json 添加 erify:quick（format → lint → typecheck → build → test）
2. 验证通过
**验收**：
pm run verify:quick 全绿

### A4. 后端门禁对齐
**操作**：
1. 确认 mvn test -Dtest="com.jingxuan.modules.**" 全绿
2. 确认 V2ModuleArchitectureTest 全绿
3. 修复可能存在的编译警告
**验收**：后端子集测试全绿

### A5. OpenAPI 契约一致性门禁
**操作**：
1. 运行 python scripts/sync-openapi.py 同步 YAML
2. 验证 YAML 与 Controller 路径一致
3. 添加 
pm run api:check 脚本
**验收**：api:check 通过

### A6. 修复 Prettier 格式化基线
**操作**：
1. 运行 
pm run format
2. 添加 .gitattributes 强制 LF
**验收**：
pm run format:check 通过

### A7. Flyway 基线对齐
**操作**：
1. 删除后端重复建表 sql（sql/ 目录）
2. 确认 Flyway 迁移脚本是最新 Schema 的唯一来源
**验收**：FlywayBaselineMigrationTest 通过

### A8. 阶段 A 检查点
- 提交：中文描述，6-8 个原子提交
- 门禁：verify:quick 全绿 + 后端测试全绿 + Orval 生成成功

---

## 阶段 B：身份权限垂直落地（6 个任务）

### B1. 迁移用户/角色/权限 Controller 到生成客户端
**操作**：
1. 将 V1AuthController、V1RegistrationController、V1UserAdminController、V1RoleAdminController、V1MenuAdminController、V1UserApprovalController 的 API 响应体从旧 Result<T> 切换到 ProblemDetails/直接返回
2. 更新 OpenAPI YAML 同步 schema
3. 重新生成 Orval 客户端
**验收**：生成客户端有 auth/users/roles 类型

### B2. 前端 session 管理接入生成客户端
**操作**：
1. 将登录/注册/刷新/注销页面从旧 @/api/auth 切换到生成客户端 hooks
2. 确认 Access Token 仅存内存
**验收**：登录/注册/注销走生成客户端

### B3. 用户管理页面接入生成客户端
**操作**：
1. 用户列表、创建、编辑、禁用页面切到生成客户端
2. AI 用户导入切到生成客户端
**验收**：用户管理页面功能完整

### B4. 角色权限页面接入生成客户端
**操作**：
1. 角色列表、创建、编辑、权限分配切到生成客户端
2. 三种内置角色不可删除
**验收**：角色管理页面功能完整

### B5. 动态菜单 + WorkspaceShell
**操作**：
1. WorkspaceShell 统一布局替换三套旧布局
2. 动态菜单从后端权限码生成
**验收**：三种角色布局正确

### B6. 阶段 B 检查点
- 三种角色登录 → 动态菜单 → 管理页面完整
- 全部测试通过
- 中文提交

---

## 阶段 C：批次/待办/作品垂直落地（8 个任务）

### C1. 迁移作品 Controller 到 v2 模式
**操作**：
1. V1PortfolioController 切换到 ProblemDetails 响应、雪花 ID 字符串化、状态字符串枚举
2. 同步 OpenAPI YAML
**验收**：生成客户端有作品类型

### C2. 迁移批次/待办 Controller 到 v2 模式
**操作**：
1. V1CampaignAdminController、V1CampaignController 切换到 v2 API 模式
2. 同步 OpenAPI
**验收**：生成客户端有批次/待办类型

### C3. 学生首页/待办页面接入生成客户端
**操作**：
1. Home、TodoList 页面切到生成客户端
2. 待办驱动工作流
**验收**：待办页面完整

### C4. 作品提交页面接入生成客户端
**操作**：
1. WorkSubmit 页面切到生成客户端
2. 上传附件 + 提交审核
**验收**：完整创作流程

### C5. 我的作品页面接入生成客户端
**操作**：
1. MyWorks 页面切到生成客户端
2. 草稿编辑、驳回重提
**验收**：作品管理页面完整

### C6. 作品状态机 + 待办联动测试
**操作**：
1. 确认作品状态机（草稿→已提交→已驳回→已通过）正确
2. 确认提交作品 → 自动标记待办完成
3. 新增后端测试覆盖
**验收**：待办状态与作品状态联动测试通过

### C7. 作品展示页面接入生成客户端
**操作**：
1. 公开端 WorkList、WorkDetail 页面切到生成客户端
2. iframe 预览保留
**验收**：公开展廊页面完整

### C8. 阶段 C 检查点
- 学生"注册→待办→创建作品→上传→提交"全流程
- 全部测试通过
- 中文提交

---

## 阶段 D：审核/发布/互动/删除垂直落地（6 个任务）

### D1. 迁移审核 Controller 到 v2 模式 + 前端接入
**操作**：
1. V1AuditController、V1PublicationController 切换到 v2 模式
2. 审核队列、审核历史、发布/下线/精选页面切到生成客户端
**验收**：审核流程完整

### D2. 迁移评论 Controller + 前端接入
**操作**：
1. V1CommentController、V1CommentAdminController 切换到 v2 模式
2. 评论树分页、游客评论页面切到生成客户端
**验收**：评论功能完整

### D3. 迁移点赞 Controller + 前端接入
**操作**：
1. V1LikeController 切换到 v2 模式（幂等 PUT/DELETE）
2. 点赞 + 浏览量页面切到生成客户端
**验收**：点赞功能完整

### D4. 作品物理删除完整实现
**操作**：
1. 确认后端物理删除级联 10 张关联表
2. 文件在事务提交后删除
3. 删除申请页面切到生成客户端
**验收**：删除后所有关联数据清空

### D5. 用户物理删除完整实现
**操作**：
1. 删除前影响清单
2. 保护根管理员账号
**验收**：删除流程安全

### D6. 阶段 D 检查点
- 提交→审核→发布→公开展示→删除申请完整 E2E
- 无跨模块 Mapper（扩展 DOCUMENTED_MAPPER_BRIDGES 白名单的方式过渡）
- 全部测试通过
- 中文提交

---

## 阶段 E：评分/排行/奖品/剩余模块垂直落地（6 个任务）

### E1. 迁移评分 Controller + 前端接入
**操作**：
1. V1ScoreController、V1ScoreAdminController、V1MyScoreController 切换到 v2 模式
2. 教师评分页面切到生成客户端
3. 四维度 Upsert、评分历史
**验收**：评分 API + 页面完整

### E2. 实时排行实现
**操作**：
1. 确认排行 SQL（MySQL 窗口函数）
2. Redis generation key 缓存 + 评分变化立即失效
3. 公示开关
4. 排行榜页面切到生成客户端
**验收**：排行实时更新

### E3. 奖品发放
**操作**：
1. 奖项模板 API 切换到 v2
2. 奖品发放快照（固化名次、分数）
3. 奖品页面切到生成客户端
**验收**：发放记录不受后续评分影响

### E4. 公告/通知/敏感审核/日志
**操作**：
1. V1NoticeController、V1NotificationController、V1ContentModerationController、V1SensitiveRuleController、V1LogController、V1DashboardController 切换到 v2 模式
2. 对应页面切到生成客户端
**验收**：全部页面完整

### E5. CSV 导出
**操作**：
1. 确认后端服务端流式 CSV 导出
**验收**：导出文件正确

### E6. 阶段 E 检查点
- 全部功能矩阵达到 v1 等价或更优
- 实时改分会更新已公示排行
- 全部测试通过
- 中文提交

---

## 阶段 F：遗留清理 + 质量收口（4 个任务）

### F1. 删除旧遗留代码
**操作**：
1. 删除 modules/adapter/ 目录
2. 删除根 ntity/、mapper/、service/（全部迁移后）
3. 删除旧 Result<T>
4. 删除旧 SQL 脚本
**验收**：CI 扫描确认无旧路由、旧响应、旧实体

### F2. 删除旧 API 别名
**操作**：
1. 删除 @/api/admin/*、@/api/teacher/*、@/api/student/*、@/api/public/* 目录
2. 确认前端没有直接 Axios 调用
**验收**：前端只有 Orval 客户端

### F3. 覆盖率提升
**操作**：
1. 新增后端模块测试（identityaccess、campaign、portfolio、evaluation 等）
2. 新增前端组件测试
3. 目标：整体 ≥80%，核心模块 ≥90%
**验收**：覆盖率门禁通过

### F4. 阶段 F 检查点
- 后端只剩九模块 v2 实现
- 前端只剩 app/features/shared
- Flyway 唯一 Schema
- 全部质量门禁通过
- 中文提交

---

## 阶段 G：迁移/部署/生产切换（5 个任务）

### G1. GitHub 推送
**操作**：
1. 在本地积累全部 A-F 阶段提交后，通知你手动 git push
2. 或你提前配置 GitHub Token/SSH
**验收**：远端仓库同步

### G2. 修复 CI 配置
**操作**：
1. 确认 GitHub Actions 配置 JDK 21 / Node 24
2. 确认 Release workflow 完整
**验收**：CI 配置文件完整

### G3. 迁移 CLI
**操作**：
1. 实现 scripts/migrate.mjs：preflight / migrate / verify / rollback / purge
2. SQL 迁移脚本：有效数据 + 原 ID + 密码哈希 + 文件校验
**验收**：迁移脚本可用

### G4. Docker + PM2 配置对齐
**操作**：
1. 确认 Docker Compose、PM2、Nginx 配置与当前版本匹配
**验收**：配置语法正确

### G5. 正式切换
> 需要你提供 SSH/数据库凭据
**操作**：
1. 维护窗口：2-4 小时
2. Flyway 创建空 jingxuan_v2
3. 迁移数据 + 文件校验
4. 三角色冒烟
5. 部署 + 重载 Nginx
6. 开放流量，强制全员重新登录
**验收**：切换成功，观察 24 小时

---

## 门禁自动通过条件（无人工审批）

每个任务完成后自动验证：
1. ✅ 后端 mvn compile -q 通过
2. ✅ 后端模块测试全绿
3. ✅ 后端架构测试全绿
4. ✅ 前端 lint/typecheck 全绿
5. ✅ 前端测试全绿
6. ✅ OpenAPI 契约一致
7. ✅ Orval 生成成功
8. ✅ verify:quick 全绿

任何一项失败 → 停止当前任务 → 修复 → 继续。

---

## 完成标准

- 功能矩阵全部验证通过
- 后端只剩九模块 v2 实现
- 前端只剩 app/features/shared
- Flyway 唯一 Schema
- 物理删除、实时排行、发奖快照全部生效
- 覆盖率 ≥80%（核心 ≥90%）
- 两次迁移演练完成
- 正式生产切换完成

---

## 自动停止条件

以下情况自动停止并优先修复：
- clean build 失败
- 契约测试失败
- 数据不一致
- 生成客户端 import 断裂
- 生产部署后健康检查失败
