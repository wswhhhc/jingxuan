# 菁选 v2 生产切换与运维手册

## 1. 维护窗口计划

### 1.1 窗口时长
预计 2-4 小时（视数据量而定，建议选在低峰期，如周末凌晨）

### 1.2 前置条件
- [ ] preflight 校验通过（源库可达、目标库为空）
- [ ] 最新构建的 JAR 包已就绪（`mvn package -Dmaven.test.skip=true`）
- [ ] 前端构建产物已就绪（`npm run build`）
- [ ] `JWT_SECRET`、`DB_PASSWORD` 等环境变量已确认

### 1.3 切换步骤

```
1. 进入维护模式，停止旧后端写入
   docker compose stop backend
   # 或: pm2 stop jingxuan-back

2. 停止旧 Nginx（或切换维护页面）
   docker compose stop nginx
   # 或: cp /usr/share/nginx/html/maintenance.html /usr/share/nginx/html/index.html

3. 运行数据迁移
   node scripts/migrate.mjs preflight \
     --source-db=mysql://root:xxx@localhost:3306/jingxuan \
     --target-db=mysql://root:xxx@localhost:3306/jingxuan_v2
   
   node scripts/migrate.mjs migrate \
     --source-db=mysql://root:xxx@localhost:3306/jingxuan \
     --target-db=mysql://root:xxx@localhost:3306/jingxuan_v2

4. 迁移验证
   node scripts/migrate.mjs verify --target-db=mysql://root:xxx@localhost:3306/jingxuan_v2

5. 部署 v2
   # Docker 部署
   docker compose up -d --build backend nginx
   # PM2 部署
   cp target/jingxuan-backend-*.jar /opt/jingxuan/backend/
   pm2 restart jingxuan-back
   npm --prefix frontend run build
   cp -r frontend/dist/* /usr/share/nginx/html/jingxuan/

6. 冒烟验证
   bash scripts/smoke-test.sh http://localhost:8080

7. 开放流量
   # Nginx 恢复
   docker compose start nginx
   # 或恢复 index.html

8. 全员重新登录（JWT 密钥可能变更）
```

## 2. 回滚方案

### 2.1 开放写入前的回滚
**前提**：还没有用户写入 v2 数据。

```bash
# 恢复 v1 应用 + 原始数据库
docker compose stop backend nginx
docker compose -f docker-compose.v1.yml up -d
```

### 2.2 开放写入后的回滚
**风险**：v2 中产生的新数据会丢失。

```bash
# 1. 停止 v2
docker compose stop backend

# 2. 回滚数据库（从迁移前备份恢复）
mysql -u root -p jingxuan < backups/jingxuan-pre-migration.sql

# 3. 启动 v1
docker compose -f docker-compose.v1.yml up -d
```

### 2.3 构建回滚库
迁移 CLI 的 `migrate` 命令同时会创建一份**只含有效数据的 v1 回滚库**（`jingxuan_rollback`），包含：
- 有效数据的完整副本（不含 deleted=1）
- 有效 uploads 文件归档
- 原 ID 不变

## 3. 迁移数据规则

| 表 | 排除规则 | 保留规则 |
|----|---------|---------|
| sys_user | deleted=1, 7 天未绑附件 | 保留 ID、密码哈希 |
| work | deleted=1, status=0(草稿) | 通过作品 |
| work_attachment | 文件不存在或 SHA256 校验失败 | 有效文件 |
| work_score | 关联作品被排除 | 通过评分 |
| work_comment | 关联作品被排除 | 通过评论 |
| work_member | 关联作品被排除 | 通过成员 |
| sys_role | 全部保留 | 内置角色 |
| sys_menu | 全部保留 | 菜单树 |
| score_batch | 全部保留 | 评分批次 |
| reward_config | 全部保留 | 奖品配置 |
| work_publish | 关联作品被排除 | 发布记录 |
| work_audit | 关联作品被排除 | 审核记录 |
| work_like | 关联作品被排除 | 点赞记录 |
| delete_request | 关联作品被排除 | 删除申请 |
| student_task | deleted=1 | 有效待办 |
| notification | 全部保留 | 通知记录 |

**彻底排除（不迁移、不备份）：**
- `work_runtime` 表（废弃功能）
- `port_manage` 表（废弃功能）
- 所有 `deleted=1` 的记录
- 超过 7 天的未绑定附件及文件
- 审核未通过的作品
- 测试数据、Seed 数据

## 4. 验证清单

### 4.1 迁移后验证
- [ ] 三角色登录正常（admin/t001/2022001）
- [ ] 公开展廊可访问
- [ ] 作品详情页正常
- [ ] 排行榜可查看
- [ ] 评论可加载
- [ ] 教师评分页正常
- [ ] 管理端仪表盘正常
- [ ] 公告/通知正常运行
- [ ] 文件可下载/预览
- [ ] 统计数对比（迁移前后记录数一致）

### 4.2 性能验证
- [ ] 核心接口响应 P95 < 500ms
- [ ] 首页加载 LCP < 2.5s
- [ ] 无 500/503 错误
- [ ] 慢 SQL 日志无异常

## 5. 运维命令

```bash
# 查看运行状态
pm2 status
docker compose ps

# 查看日志
pm2 logs jingxuan-back
docker compose logs -f backend

# 健康检查
curl http://localhost:8080/actuator/health

# Prometheus 指标
curl http://localhost:8080/actuator/prometheus

# 慢 SQL 日志
tail -f logs/slow-sql.log

# JSON 日志查询
tail -f logs/jingxuan-backend.json.log | jq 'select(.level == "ERROR")'

# Grafana（监控 profile 启用时）
# http://localhost:3000 (admin/admin)
```

## 6. 架构决策记录

- **HTTP 安全**：正式环境使用 HTTP，这是已接受风险。Cookie 使用 HttpOnly + SameSite=Strict，路径限定 /api/v1/auth
- **双部署链路**：Docker 与 PM2 均为受支持部署方式，切换前两套方案都需通过冒烟
- **实时排行**：排行榜不保存冻结快照，使用 MySQL 窗口函数实时计算 + Redis generation key 缓存
- **物理删除**：今后删除为物理删除，共享根数据删除前返回影响清单
