import { setupServer } from 'msw/node'
import { handlers } from './handlers'

/** MSW 测试用 server：在单元测试中拦截 HTTP 请求 */
export const server = setupServer(...handlers)
