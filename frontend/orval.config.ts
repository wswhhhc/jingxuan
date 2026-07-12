import { defineConfig } from 'orval'

export default defineConfig({
  jingxuan: {
    input: {
      target: '../openapi/jingxuan-v1.yaml',
    },
    output: {
      mode: 'tags-split',
      target: 'src/shared/api/generated/endpoints.ts',
      schemas: 'src/shared/api/generated/models',
      client: 'vue-query',
      httpClient: 'axios',
      clean: true,
      prettier: true,
      override: {
        mutator: {
          path: 'src/shared/api/http.ts',
          name: 'apiRequest',
        },
      },
    },
  },
})
