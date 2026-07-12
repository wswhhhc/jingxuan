import { test, expect } from '@playwright/test'

test.describe('菁选 v2 核心 E2E 冒烟', () => {
  test('公开页面：展廊列表可加载', async ({ page }) => {
    await page.goto('/jingxuan/')
    await expect(page.locator('h1')).toContainText(/学院作品展廊|菁选/)
  })

  test('公开发布作品列表正常', async ({ page }) => {
    await page.goto('/jingxuan/works')
    await expect(page.locator('.workspace-page')).toBeVisible()
  })

  test('公开排行榜可加载', async ({ page }) => {
    await page.goto('/jingxuan/ranking')
    await expect(page.locator('.workspace-page')).toBeVisible()
  })
})

test.describe('认证流程', () => {
  test('登录页渲染', async ({ page }) => {
    await page.goto('/jingxuan/login')
    await expect(page.locator('button')).toContainText(/登录|登入/)
  })

  test('未认证重定向到登录', async ({ page }) => {
    await page.goto('/jingxuan/student/home')
    await page.waitForURL('/jingxuan/login')
  })
})

test.describe('管理员端', () => {
  test.beforeEach(async ({ page }) => {
    // 直接注入 token 跳过登录
    await page.goto('/jingxuan/login')
    await page.evaluate(() => {
      localStorage.setItem('token', 'test-admin-token')
      localStorage.setItem('userInfo', JSON.stringify({
        id: 1, username: 'admin', realName: '管理员', roleId: 3, roleCode: 'ADMIN',
      }))
    })
  })

  test('控制台可加载', async ({ page }) => {
    await page.goto('/jingxuan/admin/dashboard')
    await expect(page.locator('h1')).toContainText('控制台')
  })

  test('审核页可加载', async ({ page }) => {
    await page.goto('/jingxuan/admin/audit')
    await expect(page.locator('.workspace-page')).toBeVisible()
  })

  test('用户管理页可加载', async ({ page }) => {
    await page.goto('/jingxuan/admin/users')
    await expect(page.locator('.workspace-page')).toBeVisible()
  })

  test('公告管理页可加载', async ({ page }) => {
    await page.goto('/jingxuan/admin/notice')
    await expect(page.locator('.workspace-page')).toBeVisible()
  })
})

test.describe('教师端', () => {
  test.beforeEach(async ({ page }) => {
    await page.goto('/jingxuan/login')
    await page.evaluate(() => {
      localStorage.setItem('token', 'test-teacher-token')
      localStorage.setItem('userInfo', JSON.stringify({
        id: 2, username: 't001', realName: '教师', roleId: 2, roleCode: 'TEACHER',
      }))
    })
  })

  test('评分页可加载', async ({ page }) => {
    await page.goto('/jingxuan/teacher/score')
    await expect(page.locator('.workspace-page')).toBeVisible()
  })

  test('排行榜页可加载', async ({ page }) => {
    await page.goto('/jingxuan/teacher/ranking')
    await expect(page.locator('.workspace-page')).toBeVisible()
  })
})

test.describe('学生端', () => {
  test.beforeEach(async ({ page }) => {
    await page.goto('/jingxuan/login')
    await page.evaluate(() => {
      localStorage.setItem('token', 'test-student-token')
      localStorage.setItem('userInfo', JSON.stringify({
        id: 3, username: '2022001', realName: '学生', roleId: 1, roleCode: 'STUDENT',
      }))
    })
  })

  test('首页可加载', async ({ page }) => {
    await page.goto('/jingxuan/student/home')
    await expect(page.locator('.workspace-page')).toBeVisible()
  })

  test('我的作品可加载', async ({ page }) => {
    await page.goto('/jingxuan/student/works')
    await expect(page.locator('.workspace-page')).toBeVisible()
  })

  test('待办页可加载', async ({ page }) => {
    await page.goto('/jingxuan/student/todos')
    await expect(page.locator('.workspace-page')).toBeVisible()
  })
})
