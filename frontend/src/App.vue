<script setup lang="ts">
import { RouterLink, RouterView } from 'vue-router'

const links = [
  { to: '/', label: '暗夜地图' },
  { to: '/forecast', label: '观星指数' },
  { to: '/timeline', label: '天文事件' },
  { to: '/postcard', label: '星空明信片' },
]
</script>

<template>
  <div class="min-h-screen flex flex-col">
    <header class="glass-panel sticky top-0 z-50 border-b border-starlight/10">
      <nav class="max-w-7xl mx-auto px-6 py-4 flex items-center justify-between">
        <RouterLink to="/" class="font-display text-2xl text-starlight starlight-text">
          城市光影 <span class="text-dark-gold">·</span> <span class="text-moonlight text-lg">CityGlow</span>
        </RouterLink>
        <div class="flex gap-6 font-body text-sm">
          <RouterLink v-for="link in links" :key="link.to" :to="link.to"
            class="text-starlight/70 hover:text-dark-gold transition-colors uppercase tracking-wider"
            active-class="text-dark-gold">
            {{ link.label }}
          </RouterLink>
        </div>
      </nav>
    </header>
    <main class="flex-1">
      <RouterView />
    </main>
  </div>
</template>
