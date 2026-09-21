<script setup lang="ts">
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import AppNotice from '../components/AppNotice.vue'
import { ApiError } from '../api/http'
import { useAuth } from '../composables/useAuth'
const email = ref('leader@demo.com'), password = ref('Leader@123'), error = ref('')
const router = useRouter(); const { login, state } = useAuth()
async function submit() { error.value = ''; try { const user = await login(email.value, password.value); router.push(user.role === 'LEADER' ? '/dashboard' : '/strategies') } catch (exception) { error.value = exception instanceof ApiError ? exception.message : 'Não foi possível entrar.' } }
</script>
<template><main class="login-page"><section class="login-panel"><div class="brand brand--large"><span>ÁGUIA</span> BRANCA</div><p class="eyebrow">GESTÃO ESTRATÉGICA</p><h1>Entre para acompanhar decisões que movem a operação.</h1><p class="muted">Use uma das contas de demonstração documentadas no projeto.</p><form @submit.prevent="submit"><label>E-mail<input v-model="email" type="email" autocomplete="email" required></label><label>Senha<input v-model="password" type="password" autocomplete="current-password" required></label><AppNotice tone="error" :message="error"/><button class="button button--primary" :disabled="state.loading">{{ state.loading ? 'Entrando...' : 'Entrar' }}</button></form></section></main></template>
