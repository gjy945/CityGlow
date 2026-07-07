<script setup lang="ts">
import { RouterLink } from 'vue-router'
</script>

<template>
  <section class="min-h-[calc(100vh-73px)] flex items-center justify-center px-6">
    <div class="text-center">
      <p class="font-mono text-dark-gold text-sm tracking-[0.4em] uppercase mb-6">404</p>
      <h1 class="font-display text-6xl text-starlight starlight-text mb-6">星图迷失</h1>
      <p class="font-body text-moonlight/60 mb-10">坐标超出已知星域</p>
      <RouterLink to="/"
        class="font-body text-sm text-dark-gold hover:text-starlight transition-colors uppercase tracking-wider border border-dark-gold/40 hover:border-starlight/60 px-6 py-3 rounded">
        返回首页
      </RouterLink>
    </div>
  </section>
</template>
