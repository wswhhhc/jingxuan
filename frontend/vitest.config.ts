import { defineConfig } from 'vitest/config'
import vue from '@vitejs/plugin-vue'
import path from 'path'

export default defineConfig({
  plugins: [vue()],
  resolve: {
    alias: {
      '@': path.resolve(__dirname, 'src'),
    },
  },
  test: {
    environment: 'jsdom',
    globals: true,
    include: ['src/**/*.{test,spec}.{ts,tsx}'],
    coverage: {
      provider: 'v8',
      reportsDirectory: 'coverage',
      reporter: ['text', 'json-summary', 'html', 'lcov'],
      reportOnFailure: true,
      include: ['src/**/*.{ts,vue}'],
      exclude: ['src/**/*.d.ts', 'src/**/__tests__/**', 'src/**/*.{test,spec}.ts'],
      // 阶段 0 先锁住现有可信基线，后续每个 v2 模块只允许提高阈值。
      // 发布候选门禁仍以整体 80%/70%、核心模块 90% 为最终目标。
      thresholds: {
        statements: 24,
        branches: 25,
        functions: 17,
        lines: 25,
      },
    },
  },
})
