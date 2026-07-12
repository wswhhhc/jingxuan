# Spec：identity-access v2 认证与会话安全

## Objective

将当前“v1 Controller 拼装旧登录 + JSON refresh token”的过渡实现收敛为 `identity-access` 模块内的完整认证用例，使学生、教师和管理员使用统一 `/api/v1/auth` 契约，并满足短期 Access JWT、HttpOnly Refresh Cookie、轮换重放检测、严格同源、教师待审核与 Redis 自适应限流要求。

目标用户：学生、教师、管理员及自定义角色用户。  
成功状态：前端 JavaScript 无法读取 refresh token；Access Token 不持久化；旧 refresh token 重放会撤销整个 family；受保护接口由权限码而非页面菜单决定安全边界。

## Assumptions

1. 正式环境继续使用 HTTP 的风险已被接受，因此 refresh Cookie 在生产保持 `Secure=false`；系统必须同源部署在可信校园网。
2. Cookie 固定为 `HttpOnly`、`SameSite=Strict`、`Path=/api/v1/auth`；开发和测试也沿用相同语义。
3. Access JWT 有效期 15 分钟，只返回给前端内存 session，不写 localStorage/sessionStorage。
4. 默认 refresh family 有效期 8 小时，“记住我”30 天；轮换不延长 family 的绝对截止时间。
5. 学生注册后为 `ENABLED`，教师注册后为 `PENDING_APPROVAL`；管理员批准后才能登录和评分。
6. 旧 BCrypt 哈希原样保留；新密码使用 cost 12，成功登录旧低成本哈希时允许透明升级。
7. 邮件验证码与风控算术挑战是两个资源：邮件验证码验证邮箱归属；连续失败达到阈值后额外要求一次性算术挑战。

## Threat Model

| 边界/资产 | 主要威胁 | 控制 |
|---|---|---|
| 登录请求 → 凭据验证 | 爆破、账号枚举、登录 CSRF | Redis 失败计数、统一错误、Origin 校验、阈值验证码 |
| Refresh Cookie → Redis session | Cookie 窃取、并发轮换、旧令牌重放 | 仅存哈希、原子轮换、USED tombstone、family 全撤销、Strict Cookie |
| Access JWT → API | token 泄露、权限提升 | 15 分钟 TTL、内存保存、权限码鉴权、注销黑名单 |
| 注册/邮件请求 | 邮件轰炸、IP 滥用、伪造教师 | 地址/IP 双限流、一次性验证码、教师待审核 |
| 管理员审批 | 越权、抵赖 | 权限码、不可修改内置角色、结构化审计日志 |

## REST Contract

所有错误使用 `application/problem+json`，至少包含 `status`、`code`、`requestId`；校验错误增加 `fieldErrors`。

| 方法与路径 | 输入 | 成功响应 | 说明 |
|---|---|---|---|
| `POST /api/v1/auth/login` | `{ username, password, rememberMe?, challengeId?, challengeAnswer? }` | `200 { accessToken, tokenType, expiresIn, user }` + Set-Cookie | 不返回 refresh token |
| `POST /api/v1/auth/refresh` | 无 body，读取 Cookie | `200 { accessToken, tokenType, expiresIn, user }` + 轮换 Set-Cookie | 旧 token 重放返回 401 并撤销 family |
| `POST /api/v1/auth/logout` | 无 body | `204` + 清 Cookie | 撤销当前 family，并拉黑剩余 Access JWT TTL |
| `POST /api/v1/auth/logout-all` | 无 body | `204` + 清 Cookie | 撤销当前用户全部 family |
| `GET /api/v1/auth/me` | — | `200 UserInfo` | ID 为字符串 |
| `POST /api/v1/auth/email-verifications` | `{ email, role }` | `202` | 每地址/IP 1 次/分钟、5 次/小时 |
| `POST /api/v1/auth/registrations` | 学生/教师注册 DTO | `201 UserInfo` | 邮件验证码一次性消费 |
| `POST /api/v1/auth/challenges` | `{ purpose }` | `201 { id, question, expiresIn }` | Redis 算术挑战，5 分钟过期 |
| `POST /api/v1/users/{id}/approval-decisions` | `{ decision, reason? }` | `204` | 需要 `user:approve` |

Cookie 名称固定为 `jingxuan_refresh`。清除 Cookie 时必须使用与创建完全一致的 Path 与 SameSite 属性。

## Session Model

Redis 只存 refresh token SHA-256，不存明文。

- `jingxuan:v2:refresh:token:{hash}`：token 状态 `ACTIVE/USED`、familyId、userId、绝对过期时间。
- `jingxuan:v2:refresh:family:{familyId}`：用户、当前 token hash、状态 `ACTIVE/REVOKED`、rememberMe、绝对过期时间。
- `jingxuan:v2:refresh:user:{userId}`：该用户有效 familyId 集合，用于全端注销。

轮换必须原子完成：校验 token 与 family → 标记旧 token `USED` → 写入新 token → 更新 family 当前 hash。读取到 `USED` 或非当前 hash 时，立即将 family 标记 `REVOKED` 并删除/失效其当前 token。

## Rate Limits

- 登录：同账号或 IP 连续失败 5 次/15 分钟后要求 challenge，20 次/15 分钟返回 429。
- 邮件验证码：每地址/IP 1 次/分钟、5 次/小时。
- AI 导入：每管理员 10 次/小时（由 identity-access 暴露通用限流 API，业务模块调用）。
- 游客评论：5 次/10 分钟后要求 challenge、20 次/小时返回 429（由 engagement/portfolio 使用同一限流 API）。

429 Problem Details 使用 `code=RATE_LIMITED`，并返回 `Retry-After`。

## Commands

```powershell
# JDK 25 单元测试
$env:JAVA_HOME='D:\java\microsoft-jdk-25.0.3\jdk-25.0.3+9'
$env:Path="$env:JAVA_HOME\bin;$env:Path"
mvn -f backend/pom.xml -B -ntp clean test

# 指定认证集成测试
mvn -f backend/pom.xml -B -ntp clean verify -DskipUnitTests=true "-Dit.test=V1AuthApiTest"

# 前端认证测试与类型检查
npm --prefix frontend run test -- --run src/features/identity
npm run frontend:typecheck
```

## Project Structure

```text
backend/src/main/java/com/jingxuan/identityaccess/
  api/                         # 跨模块只读 DTO、权限码、用户会话视图
  web/                         # Controller、Cookie/Origin HTTP 适配
  internal/application/        # 登录、刷新、注销、注册、审批用例
  internal/domain/             # Refresh family、状态与限流策略
  internal/infrastructure/     # Redis、Mapper、密码与邮件适配

frontend/src/features/identity/
  api/                         # 对生成 hooks 的薄封装
  model/                       # 内存 session
  ui/                          # 登录、注册、教师待审核页面
```

## Code Style

Controller 只做边界适配，不直接拼装 token 或访问 Redis：

```java
@PostMapping("/refresh")
ResponseEntity<AccessSessionResponse> refresh(HttpServletRequest request,
                                               HttpServletResponse response) {
    var session = authenticationUseCases.refresh(cookieReader.require(request));
    refreshCookieWriter.write(response, session.refreshCookie());
    return ResponseEntity.ok(session.accessSession());
}
```

## Testing Strategy

- 单元测试：family 状态机、绝对 TTL、重放撤销、限流阈值、challenge 一次性消费、Cookie 属性。
- Redis 集成测试：并发刷新只能一个成功；旧 token 重放后新 token 也失效；全端注销清理所有 family。
- HTTP 集成测试：登录/刷新/注销 Origin、Cookie、Problem Details、教师待审核、权限码审批。
- 前端 Vitest + MSW：Access Token 仅内存、刷新后重放原请求、登出清 session。
- Playwright：学生登录、教师待审核→管理员批准→教师登录、刷新轮换与全端注销。

## Boundaries

- Always：验证 Origin、使用参数化 Mapper、Cookie 不进入日志、令牌只存哈希、所有认证失败返回统一消息、管理员审批写审计事件。
- Already approved by v2 plan：数据库迁移、Redis 会话模型、Cookie 认证变化、CORS 收紧、BCrypt cost 12、CI 测试增加。
- Never：提交真实密钥；在响应、日志或数据库中保存 refresh 明文；在 localStorage/sessionStorage 保存 Access/Refresh Token；用前端菜单代替后端授权。

## Success Criteria

- [x] 登录响应不包含 refresh token，响应 Cookie 满足锁定属性。
- [x] refresh 原子轮换；旧 token 重放会撤销 family，后继 token 无法继续使用。
- [ ] 单端/全端注销均有集成测试，Cookie 被正确清除。
- [x] 登录、刷新、注销拒绝未知 Origin；无通配 CORS。
- [ ] BCrypt cost 12；教师注册为 `PENDING_APPROVAL`，批准前不可登录/评分。
- [ ] 四类限流和算术 challenge 有确定性测试。
- [ ] Access Token 只存在前端内存，刷新页面通过 Cookie 恢复会话。
- [ ] 认证核心模块覆盖率 ≥90%，全部受保护端点有权限测试。

## Tasks

- [x] Task 1：Refresh family domain + Redis 原子轮换
  - Acceptance：并发轮换单成功、重放撤销 family、绝对 TTL 不延长
  - Verify：domain 单测 + Redis Testcontainers
  - Files：domain 记录、RefreshTokenService、Redis adapter、测试（≤5 个逻辑文件）
- [x] Task 2：Cookie/Origin HTTP 契约
  - Acceptance：登录/刷新/注销不再通过 JSON 传 refresh，Cookie 属性与 Origin 测试全绿
  - Verify：Controller 单测 + HTTP 集成测试
- [ ] Task 3：登录限流、算术 challenge 与 BCrypt 12
  - Acceptance：5/20 阈值、一次性 challenge、旧 hash 透明升级
  - Verify：策略单测 + 登录 API 集成测试
- [ ] Task 4：注册、教师待审核与审批
  - Acceptance：学生启用、教师待审核、管理员权限码审批
  - Verify：注册/越权/审批集成测试
- [ ] Task 5：前端内存 session
  - Acceptance：无 token Web Storage；Cookie refresh 可恢复；注销清内存
  - Verify：Vitest + MSW + Playwright 核心流

## Open Questions

- 无阻断问题。真实生产 `Secure=false` 与同源 HTTP 风险已在原计划和 ADR-008 中明确接受。
