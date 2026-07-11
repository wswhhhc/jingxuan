# ADR-002：以 OpenAPI 为唯一 API 契约源

## 状态

已接受

## 日期

2026-07-11

## 背景

现有接口以角色前缀和 Controller 习惯为中心，存在 `/admin`、`/teacher`、`/student` 的重复路径，以及 `Result<T>` 包裹导致的 HTTP 语义失真。前端还直接手写 Axios 调用，DTO 与实际响应容易漂移。

## 决策

采用 OpenAPI YAML 作为唯一契约源，统一新接口到 `/api/v1`。

约束：

- 不再保留旧 API 适配层。
- 资源按业务而非角色建模。
- 创建返回 `201`，无响应操作返回 `204`。
- 错误统一为 RFC Problem Details，并扩展 `code`、`requestId`、`fieldErrors`。
- 雪花 ID 统一按不透明字符串暴露。
- 分页统一 `items + pageInfo`。
- 前端通过 Orval 生成 Axios client、DTO 与 Vue Query hooks，页面禁止直调底层 Axios。

## 备选方案

### 继续手写前后端 DTO

- 优点：初期看似更快。
- 缺点：接口变更易漏改，重复劳动高。
- 结论：不采用。

### 保留旧接口再加兼容层

- 优点：切换风险看似更低。
- 缺点：会让重复接口长期并存，拖慢重构。
- 结论：不采用。

## 结果

- API 成为明确边界，而不是 Controller 副产品。
- 前后端生成物可由 CI 校验一致性。
