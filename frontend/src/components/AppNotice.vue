<script setup lang="ts">
defineProps<{ tone?: 'error' | 'success' | 'info'; message: string }>()
</script>
<template><p v-if="message" class="notice" :class="`notice--${tone || 'info'}`">{{ message }}</p></template>
