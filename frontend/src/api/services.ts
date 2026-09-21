import { request, requestWithEtag } from './http'
import type { Analysis, Idea, PageResponse, Project, ProjectResult, Strategy, StrategyHistory, SummaryReport } from '../types/api'

const query = (params: Record<string, string | number | undefined>) => {
  const value = new URLSearchParams(); Object.entries(params).forEach(([key, item]) => item !== undefined && value.set(key, String(item)))
  return value.toString() ? `?${value}` : ''
}
export const strategiesApi = {
  list: (params = {}) => request<PageResponse<Strategy>>(`/api/v1/strategies${query(params)}`),
  get: (id: string) => requestWithEtag<Strategy>(`/api/v1/strategies/${id}`),
  history: (id: string) => request<StrategyHistory[]>(`/api/v1/strategies/${id}/history`),
  create: (payload: object) => request<Strategy>('/api/v1/strategies', { method: 'POST', body: JSON.stringify(payload) }),
  update: (id: string, payload: object, etag: string) => request<Strategy>(`/api/v1/strategies/${id}`, { method: 'PUT', headers: { 'If-Match': etag }, body: JSON.stringify(payload) }),
  archive: (id: string, etag: string) => request<void>(`/api/v1/strategies/${id}`, { method: 'DELETE', headers: { 'If-Match': etag } }),
}
export const ideasApi = {
  list: (params = {}) => request<PageResponse<Idea>>(`/api/v1/ideas${query(params)}`), get: (id: string) => request<Idea>(`/api/v1/ideas/${id}`),
  create: (payload: object) => request<Idea>('/api/v1/ideas', { method: 'POST', body: JSON.stringify(payload) }), update: (id: string, payload: object) => request<Idea>(`/api/v1/ideas/${id}`, { method: 'PUT', body: JSON.stringify(payload) }), delete: (id: string) => request<void>(`/api/v1/ideas/${id}`, { method: 'DELETE' }), submit: (id: string) => request<Idea>(`/api/v1/ideas/${id}/submit`, { method: 'POST' }), review: (id: string, payload: object) => request<Idea>(`/api/v1/ideas/${id}/review`, { method: 'PATCH', body: JSON.stringify(payload) }), analysis: (id: string) => request<Analysis>(`/api/v1/ideas/${id}/analysis`), analyze: (id: string) => request<Analysis>(`/api/v1/ideas/${id}/analysis`, { method: 'POST' }),
}
export const projectsApi = {
  list: (params = {}) => request<PageResponse<Project>>(`/api/v1/projects${query(params)}`), get: (id: string) => request<Project>(`/api/v1/projects/${id}`), create: (payload: object) => request<Project>('/api/v1/projects', { method: 'POST', body: JSON.stringify(payload) }), update: (id: string, payload: object) => request<Project>(`/api/v1/projects/${id}`, { method: 'PUT', body: JSON.stringify(payload) }), progress: (id: string, payload: object) => request<Project>(`/api/v1/projects/${id}/progress`, { method: 'PATCH', body: JSON.stringify(payload) }), archive: (id: string) => request<void>(`/api/v1/projects/${id}`, { method: 'DELETE' }), results: (id: string) => request<PageResponse<ProjectResult>>(`/api/v1/projects/${id}/results`), createResult: (id: string, payload: object) => request<ProjectResult>(`/api/v1/projects/${id}/results`, { method: 'POST', body: JSON.stringify(payload) }), updateResult: (projectId: string, id: string, payload: object) => request<ProjectResult>(`/api/v1/projects/${projectId}/results/${id}`, { method: 'PUT', body: JSON.stringify(payload) }), deleteResult: (projectId: string, id: string) => request<void>(`/api/v1/projects/${projectId}/results/${id}`, { method: 'DELETE' }),
}
export const reportsApi = { summary: () => request<SummaryReport>('/api/v1/reports/summary'), strategy: (id: string) => request(`/api/v1/reports/strategies/${id}`), project: (id: string) => request(`/api/v1/reports/projects/${id}`) }
