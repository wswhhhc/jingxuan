import http from 'k6/http';
import { check, sleep } from 'k6';
import { SharedArray } from 'k6/data';

// 场景：1 万用户、10 万作品、30 万评分、50 万评论
// 核心接口 P95 < 500ms，复杂列表与排行 P95 < 1s
export const options = {
  stages: [
    { duration: '1m', target: 50 },  // 热身
    { duration: '2m', target: 200 }, // 爬升
    { duration: '5m', target: 300 }, // 峰值
    { duration: '2m', target: 0 },   // 下降
  ],
  thresholds: {
    // 核心接口
    'http_req_duration{name:leaderboard}': ['p(95)<1000'],
    'http_req_duration{name:login}': ['p(95)<500'],
    'http_req_duration{name:workList}': ['p(95)<500'],
    'http_req_duration{name:workDetail}': ['p(95)<500'],
    'http_req_duration{name:comments}': ['p(95)<500'],
    'http_req_duration{name:dashboard}': ['p(95)<1000'],
    // 总体
    http_req_duration: ['p(95)<2000', 'p(99)<5000'],
    http_req_failed: ['rate<0.01'],
  },
};

const BASE_URL = __ENV.BASE_URL || 'http://localhost:8080';

// 预先生成测试用户（避免压测中实时注册）
export function setup() {
  // 尝试用默认账号登录获取 token
  const loginPayload = JSON.stringify({ username: 'admin', password: 'admin123' });
  const loginRes = http.post(`${BASE_URL}/api/v1/auth/login`, loginPayload, {
    headers: { 'Content-Type': 'application/json' },
  });
  const adminToken = loginRes.json('accessToken');

  return {
    adminToken,
    // 测试用的作品 ID（实际测试时可从列表接口获取）
    sampleWorkId: 1,
    sampleBatchId: 1,
  };
}

export default function(data) {
  // ===== 模拟管理员操作 =====
  if (__VU < 10) {
    // 少量 VU 模拟管理员
    const adminHeaders = {
      headers: {
        'Authorization': `Bearer ${data.adminToken}`,
        'Content-Type': 'application/json',
      },
    };

    // 仪表盘
    let res = http.get(`${BASE_URL}/api/v1/dashboard/stats`, {
      ...adminHeaders,
      tags: { name: 'dashboard' },
    });
    check(res, { 'dashboard status 200': (r) => r.status === 200 });

    // 日志查询
    res = http.get(`${BASE_URL}/api/v1/audit-logs?page=1&size=20`, {
      ...adminHeaders,
      tags: { name: 'auditLogs' },
    });
    check(res, { 'audit logs status 200': (r) => r.status === 200 });
  }

  // ===== 模拟公开访问 =====
  // 作品列表
  let res = http.get(`${BASE_URL}/api/v1/showcase/works?page=1&size=20`, {
    tags: { name: 'workList' },
  });
  check(res, { 'work list status 200': (r) => r.status === 200 });

  // 作品详情
  res = http.get(`${BASE_URL}/api/v1/showcase/works/${data.sampleWorkId}`, {
    tags: { name: 'workDetail' },
  });
  check(res, { 'work detail status 200': (r) => r.status === 200 });

  // 评论列表
  res = http.get(`${BASE_URL}/api/v1/works/${data.sampleWorkId}/comments?page=1&size=10`, {
    tags: { name: 'comments' },
  });
  check(res, { 'comments status 200': (r) => r.status === 200 });

  // 排行榜（部分请求）
  if (__VU % 3 === 0) {
    res = http.get(`${BASE_URL}/api/v1/leaderboards?batchId=${data.sampleBatchId}&topN=20`, {
      tags: { name: 'leaderboard' },
    });
    check(res, { 'leaderboard status 200': (r) => r.status === 200 });
  }

  // 公告列表
  res = http.get(`${BASE_URL}/api/v1/notices/published?page=1&size=10`, {
    tags: { name: 'notices' },
  });
  check(res, { 'notices status 200': (r) => r.status === 200 });

  sleep(1);
}
