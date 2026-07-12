import { QueryClient } from '@tanstack/vue-query'
import { describe, expect, it } from 'vitest'
import { queryClient } from '../queryClient'

describe('queryClient', () => {
  it('provides shared defaults for generated Vue Query hooks', () => {
    expect(queryClient).toBeInstanceOf(QueryClient)
    expect(queryClient.getDefaultOptions()).toEqual({
      queries: {
        refetchOnWindowFocus: false,
        retry: 1,
        staleTime: 30_000,
      },
      mutations: {
        retry: 0,
      },
    })
  })
})
