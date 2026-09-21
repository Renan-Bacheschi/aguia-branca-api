import type { ProblemDetail } from '../types/api'

const API_BASE_URL = import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080'
const TOKEN_KEY = 'aguia-branca-token'
let unauthorizedHandler: (() => void) | undefined

export class ApiError extends Error {
  readonly problem: ProblemDetail
  readonly status: number

  constructor(problem: ProblemDetail, status: number) {
    super(problem.detail || problem.title || 'Não foi possível concluir a operação.')
    this.problem = problem
    this.status = status
  }
}

export function setUnauthorizedHandler(handler: () => void) { unauthorizedHandler = handler }
export function getToken() { return sessionStorage.getItem(TOKEN_KEY) }
export function setToken(token: string) { sessionStorage.setItem(TOKEN_KEY, token) }
export function clearToken() { sessionStorage.removeItem(TOKEN_KEY) }

export async function request<T>(path: string, options: RequestInit = {}): Promise<T> {
  const headers = new Headers(options.headers)
  headers.set('Accept', 'application/json')
  if (options.body && !headers.has('Content-Type')) headers.set('Content-Type', 'application/json')
  const token = getToken()
  if (token) headers.set('Authorization', `Bearer ${token}`)
  const response = await fetch(`${API_BASE_URL}${path}`, { ...options, headers })
  if (response.status === 401) unauthorizedHandler?.()
  if (!response.ok) {
    const problem = response.headers.get('content-type')?.includes('json')
      ? await response.json() as ProblemDetail : { detail: 'Não foi possível concluir a operação.' }
    throw new ApiError(problem, response.status)
  }
  if (response.status === 204) return undefined as T
  return response.json() as Promise<T>
}

export async function requestWithEtag<T>(path: string): Promise<{ data: T; etag: string | null }> {
  const headers = new Headers({ Accept: 'application/json' }); const token = getToken()
  if (token) headers.set('Authorization', `Bearer ${token}`)
  const response = await fetch(`${API_BASE_URL}${path}`, { headers })
  if (!response.ok) throw new ApiError(await response.json() as ProblemDetail, response.status)
  return { data: await response.json() as T, etag: response.headers.get('ETag') }
}
