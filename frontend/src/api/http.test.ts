import { afterEach, describe, expect, it, vi } from 'vitest'
import { ApiError, clearToken, request, setToken, setUnauthorizedHandler } from './http'

describe('http client', () => {
  afterEach(() => { vi.unstubAllGlobals(); clearToken() })

  it('sends the bearer token and turns ProblemDetail into an ApiError', async () => {
    setToken('jwt-token')
    const fetchMock = vi.fn().mockResolvedValue(new Response(JSON.stringify({ title: 'Inválido', detail: 'Campo obrigatório.' }), { status: 400, headers: { 'content-type': 'application/problem+json' } }))
    vi.stubGlobal('fetch', fetchMock)
    await expect(request('/api/v1/ideas')).rejects.toMatchObject({ status: 400, message: 'Campo obrigatório.' } satisfies Partial<ApiError>)
    expect(fetchMock.mock.calls[0][1].headers.get('Authorization')).toBe('Bearer jwt-token')
  })

  it('calls the unauthorized handler for a 401 response', async () => {
    const handler = vi.fn(); setUnauthorizedHandler(handler)
    vi.stubGlobal('fetch', vi.fn().mockResolvedValue(new Response(JSON.stringify({ detail: 'Sessão expirada.' }), { status: 401, headers: { 'content-type': 'application/problem+json' } })))
    await expect(request('/api/v1/auth/me')).rejects.toMatchObject({ status: 401 })
    expect(handler).toHaveBeenCalledOnce()
  })
})
