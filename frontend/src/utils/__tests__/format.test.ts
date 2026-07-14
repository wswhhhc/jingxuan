import { describe, expect, it } from 'vitest'
import { rewardTagType } from '../format'

describe('rewardTagType', () => {
  it('always returns an Element Plus supported tag type', () => {
    expect(rewardTagType('一等奖')).toBe('danger')
    expect(rewardTagType('二等奖')).toBe('warning')
    expect(rewardTagType('三等奖')).toBe('primary')
    expect(rewardTagType('未知奖项')).toBe('info')
  })
})
