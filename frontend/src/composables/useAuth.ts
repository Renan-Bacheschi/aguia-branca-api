import { computed, reactive } from 'vue'
import { clearToken, request, setToken, setUnauthorizedHandler } from '../api/http'
import type { LoginResponse, User } from '../types/api'

const state = reactive<{ user: User | null; loading: boolean }>({ user: null, loading: false })

export function useAuth() {
  const isAuthenticated = computed(() => Boolean(state.user))
  async function login(email: string, password: string) {
    state.loading = true
    try {
      const result = await request<LoginResponse>('/api/v1/auth/login', { method: 'POST', body: JSON.stringify({ email, password }) })
      setToken(result.accessToken); state.user = result.user
      return result.user
    } finally { state.loading = false }
  }
  async function restore() {
    if (!sessionStorage.getItem('aguia-branca-token')) return null
    state.loading = true
    try { state.user = await request<User>('/api/v1/auth/me'); return state.user }
    catch { logout(); return null } finally { state.loading = false }
  }
  function logout() { clearToken(); state.user = null }
  setUnauthorizedHandler(logout)
  return { state, isAuthenticated, login, restore, logout }
}
