import { describe, expect, it } from 'vitest'
import router from './index'

describe('route protection', () => {
  it('marks login as public and role routes with their permitted roles', () => {
    expect(router.resolve('/login').meta.public).toBe(true)
    expect(router.resolve('/dashboard').meta.roles).toEqual(['LEADER'])
    expect(router.resolve('/ideas').meta.roles).toEqual(['OPERATOR', 'MANAGER'])
    expect(router.resolve('/projects').meta.roles).toEqual(['MANAGER', 'LEADER'])
  })
})
