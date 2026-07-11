import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'
import path from 'path'
import AutoImport from 'unplugin-auto-import/vite'
import Components from 'unplugin-vue-components/vite'
import { ElementPlusResolver } from 'unplugin-vue-components/resolvers'
import viteCompression from 'vite-plugin-compression'

export default defineConfig({
  base: '/jingxuan/',
  plugins: [
    vue(),

    // Element Plus 按需导入
    AutoImport({
      resolvers: [ElementPlusResolver({ importStyle: 'css' })],
    }),
    Components({
      resolvers: [ElementPlusResolver({ importStyle: 'css' })],
    }),

    // gzip 预压缩（需部署环境开启静态 .gz 优先服务）
    viteCompression({
      algorithm: 'gzip',
      ext: '.gz',
      threshold: 10240,
      deleteOriginFile: false,
    }),
  ],
  resolve: {
    alias: {
      '@': path.resolve(__dirname, 'src'),
    },
  },
  server: {
    port: 5173,
    proxy: {
      '/api/file': {
        target: 'http://localhost:8080',
        changeOrigin: true,
      },
      '/api': {
        target: 'http://localhost:8080',
        changeOrigin: true,
        rewrite: (path) => path.replace(/^\/api/, ''),
      },
      '/uploads': {
        target: 'http://localhost:8080',
        changeOrigin: true,
      },
    },
  },
  build: {
    chunkSizeWarningLimit: 750,
    rollupOptions: {
      output: {
        manualChunks(id: string) {
          // Vue 生态核心（vue / pinia / vue-router / axios）
          if (
            id.includes('node_modules/vue') ||
            id.includes('node_modules/pinia') ||
            id.includes('node_modules/vue-router') ||
            id.includes('node_modules/axios')
          ) {
            return 'vendor-vue'
          }
          // Element Plus（按需导入后仍集中放置，避免碎片化）
          if (id.includes('node_modules/element-plus')) {
            return 'vendor-element'
          }
          // ECharts（仅在管理端控制台使用，独立 chunk）
          if (id.includes('node_modules/echarts')) {
            return 'vendor-echarts'
          }
        },
      },
    },
  },
})
