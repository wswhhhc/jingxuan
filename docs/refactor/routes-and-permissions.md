# v1 路由与权限清单

记录日期：2026-07-11。该清单用于约束 v2 功能等价和验证旧角色型 API 最终归零，不代表 v1 的授权设计是安全目标。

## 前端可见路由

| 门户 | 路由 | 功能 |
|---|---|---|
| 公共 | `/works`、`/works/:id`、`/ranking` | 展廊、详情、排行榜 |
| 通用 | `/login`、`/register`、`/change-password`、`/profile` | 登录、注册、改密、资料 |
| 学生 | `/student/home`、`/student/works`、`/student/works/create`、`/student/works/edit/:id`、`/student/works/view/:id`、`/student/todos`、`/student/ranking`、`/student/notify` | 首页、作品、待办、评分、通知 |
| 教师 | `/teacher/dashboard`、`/teacher/score`、`/teacher/history`、`/teacher/ranking`、`/teacher/notify` | 工作台、评分、历史、排行、通知 |
| 管理 | `/admin/dashboard`、`/admin/audit`、`/admin/notice`、`/admin/comment`、`/admin/rules`、`/admin/prize`、`/admin/score-batch`、`/admin/roles`、`/admin/users`、`/admin/notify`、`/admin/log`、`/admin/dict` | 后台全部管理功能 |

Vue Router 仅按 `student`、`teacher`、`admin` 三种角色做前端导航拦截；这不是安全边界。

## 后端入口族

源码共有 141 个 Spring MVC Mapping 注解，核心入口族如下：

| 入口族 | v1 鉴权方式 | v2 去向 |
|---|---|---|
| `/auth/*` 与 `/api/auth/*` | 登录、注册、验证码公开；存在兼容别名 | `/api/v1/auth/*`，删除别名 |
| `/public/*` | 全部公开 | `/api/v1/showcase/*`、公开排行和基础数据 |
| `/admin/*` | Adapter 类级 `ADMIN`，部分根 Controller 方法级 `ADMIN` | 删除角色前缀，改权限码和数据范围 |
| `/teacher/*` | Adapter 类级 `TEACHER` | 删除角色前缀，改权限码和数据范围 |
| `/student/*` | Adapter 类级 `STUDENT` | 删除角色前缀，改 `/me/*` 与资源所有权策略 |
| `/admin/notify`、`/teacher/notify`、`/student/notify` | 同一 Controller 三套别名 | `/api/v1/me/notifications` |
| `/comment/*` | 列表和游客发表公开，其余仅要求认证 | `/api/v1/showcase/works/{id}/comments`，细化权限与限流 |
| `/api/file/*` | 仅要求认证 | `/api/v1/files/*`，权限、生命周期和所有权校验 |
| `/score-batch/*` | 类级 `ADMIN` | `/api/v1/batches/*` |
| `/users` | `/admin/users` 的兼容别名 | 删除，只保留 `/api/v1/users` |

## v1 安全边界基线

- `SecurityConfig` 公开 Swagger、认证、GET uploads、`/public/**`、评论列表和游客评论；其余路径只要求已认证。
- 源码仅有 24 处 `@PreAuthorize`/角色表达式，主要集中在四个 Adapter、用户/角色/菜单、批次和敏感词 Controller。
- 大量业务权限依赖 Adapter 路由和 Service 内部所有权检查，缺少统一权限码与数据范围模型。
- v2 必须为每个受保护 OpenAPI 操作声明权限要求，并用 API 集成测试覆盖 401、403、所有权和越权场景。
