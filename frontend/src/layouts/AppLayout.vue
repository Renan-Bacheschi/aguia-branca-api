<script setup lang="ts">
import { computed, ref } from 'vue'
import { RouterLink, RouterView, useRouter } from 'vue-router'
import { useAuth } from '../composables/useAuth'
import type { Role } from '../types/api'
const router = useRouter(); const { state, logout } = useAuth(); const menuOpen = ref(false)
const links = computed(() => {
  const role = state.user?.role as Role | undefined
  return [
    ...(role === 'LEADER' ? [{ to: '/dashboard', label: 'Dashboard' }] : []),
    { to: '/strategies', label: 'Estratégias' },
    ...(role === 'OPERATOR' || role === 'MANAGER' ? [{ to: '/ideas', label: 'Ideias' }] : []),
    ...(role === 'MANAGER' || role === 'LEADER' ? [{ to: '/projects', label: 'Projetos' }] : []),
  ]
})
function signOut() { logout(); router.push('/login') }
</script>
<template>
  <div class="app-shell" :class="{ 'menu-open': menuOpen }">
    <aside class="sidebar"><RouterLink class="brand" to="/strategies"><span>ÁGUIA</span> BRANCA</RouterLink>
      <nav><RouterLink v-for="link in links" :key="link.to" :to="link.to" @click="menuOpen = false">{{ link.label }}</RouterLink></nav>
      <div class="account"><strong>{{ state.user?.name }}</strong><span>{{ state.user?.role }}</span><button class="button button--quiet" @click="signOut">Sair</button></div>
    </aside>
    <main><header class="mobile-header"><button class="menu-button" aria-label="Abrir menu" @click="menuOpen = !menuOpen">Menu</button><span>Águia Branca</span></header><RouterView /></main>
  </div>
</template>
