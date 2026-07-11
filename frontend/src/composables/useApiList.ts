import { ref } from 'vue'

interface PagePayload {
  records?: unknown
  total?: unknown
}

function getResponseData(response: unknown): unknown {
  if (typeof response !== 'object' || response === null || !('data' in response)) {
    return undefined
  }
  return (response as { data?: unknown }).data
}

function isPagePayload(value: unknown): value is PagePayload {
  return typeof value === 'object' && value !== null && 'records' in value
}

/**
 * API 分页列表查询 composable
 *
 * 统一管理 loading / list / total 状态和 try-finally 模板代码。
 * page/size 由各视图自行管理（ref 或 query 对象），通过 params 传入 loadList。
 *
 * @example
 *   const { loading, list, total, loadList } = useApiList<Item, ListQuery>(getList)
 *   const reload = () => loadList({ page: page.value, size: size.value, keyword: keyword.value })
 *   onMounted(reload)
 */
export function useApiList<T, TParams extends object, TData = unknown>(
  fetchFn: (params: TParams) => Promise<unknown>,
  mapResponse?: (data: TData | undefined) => { records: T[]; total: number },
) {
  const loading = ref(false)
  const list = ref<T[]>([])
  const total = ref(0)

  const loadList = async (params?: TParams) => {
    loading.value = true
    try {
      const res = await fetchFn((params ?? {}) as TParams)
      const data = getResponseData(res)
      if (mapResponse) {
        const mapped = mapResponse(data as TData | undefined)
        list.value = mapped.records
        total.value = mapped.total
      } else if (isPagePayload(data)) {
        list.value = Array.isArray(data.records) ? (data.records as T[]) : []
        total.value = typeof data.total === 'number' ? data.total : 0
      } else if (Array.isArray(data)) {
        list.value = data as T[]
        total.value = data.length
      } else {
        list.value = []
        total.value = 0
      }
    } finally {
      loading.value = false
    }
  }

  return { loading, list, total, loadList }
}
