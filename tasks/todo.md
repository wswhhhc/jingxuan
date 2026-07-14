# CI 修复任务清单

- [x] 统一旧适配器测试与 `/api` 后端真实路径
- [x] 统一公共接口在 Vite、Security、限流器和控制器中的路径
- [x] 恢复 V1 401/403/404/405/422/429 Problem Details 与 Request ID
- [x] 修正旧认证响应、限流隔离与并发计数测试
- [x] 清理 CI 中错误标注的 JDK 25 步骤
- [x] 固定 Docker Nginx 信任边界并收紧后端端口暴露
- [x] 重建三角色冒烟测试并加入 Legacy Docker CI job
- [x] 移除不再参与运行时的 `backend/sql` 重复脚本
- [x] 完成实时 OpenAPI 语义同步、快照与生成客户端门禁
- [x] 运行全部本地质量门禁
- [x] 对最终差异做五维代码复核
