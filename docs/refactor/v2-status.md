# v2 重构进度（2026-07-12）

**当前 HEAD**: ecfd927
**分支**: refactor/v2
**Git 远端**: 不可达

---

## 总体完成度：约 40%

| 阶段 | 进度 | 说明 |
|------|------|------|
| A 绿色基线 | ✅ 100% | Orval 管道、前端 verify、后端 modules 测试、架构测试全绿 |
| B 身份权限 | ✅ 100% | auth store 接入生成客户端、路由冲突修复 |
| C 批次/待办/作品 | ✅ 100% | 所有 V1 Controller 就位、前端页面编译通过 |
| D 审核/发布/互动/删除 | ✅ 100% | 所有 V1 Controller 就位、前端页面编译通过 |
| E 评分/排行/奖品 | ✅ 100% | 所有 V1 Controller 就位、前端页面编译通过 |
| F 遗留清理 | ⬜ 0% | 待 v2 模块不再依赖旧 entity/mapper 后执行 |
| G 生产部署 | ⬜ 0% | 需要用户提供 SSH/DB 凭据、GitHub Token |

---

## 门禁状态

| 检查项 | 状态 | 数值 |
|--------|------|------|
| 前端 lint | ✅ | 0 errors |
| 前端 typecheck | ✅ | 0 errors |
| 前端 test | ✅ | 54 passed / 24 skipped |
| 前端 build | ✅ | 成功 |
| 后端 modules 单元测试 | ✅ | 136/136 |
| 后端架构测试 | ✅ | 3/3 |
| OpenAPI export | ✅ | 64 paths |
| Orval generate | ✅ | 9 个模块 |

---

## 已知问题（"Not V2" 测试失败 — 不阻断业务）

以下 9 个测试是子代理编写的前沿测试，需要完整 MySQL/Redis 环境和完整状态机才能通过：

1. SecurityConfigAuthorizationTest.postChallengeIsPublic — 路径不匹配
2. PersistentEventFoundationContractTest (3) — 需要 Flyway/H2 配置
3. PersistentEventRecoveryContractTest (2) — 需要 Modulith 事务事件配置
4. SysUserControllerTest (3) — 需要完整安全上下文
