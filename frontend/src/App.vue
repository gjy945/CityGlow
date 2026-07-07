<script setup lang="ts">
import { ref } from 'vue'
import { RouterLink, RouterView } from 'vue-router'

const links = [
  { to: '/', label: '暗夜地图' },
  { to: '/forecast', label: '观星指数' },
  { to: '/timeline', label: '天文事件' },
  { to: '/postcard', label: '星空明信片' },
]

const mobileOpen = ref(false)

function closeMobile() {
  mobileOpen.value = false
}
</script>

<template>
  <div class="min-h-screen flex flex-col">
    <header class="glass-panel sticky top-0 z-50 border-b border-starlight/10">
      <nav class="max-w-7xl mx-auto px-4 sm:px-6 py-4 flex items-center justify-between">
        <RouterLink to="/" class="font-display text-2xl text-starlight starlight-text" @click="closeMobile">
          城市光影 <span class="text-dark-gold">·</span> <span class="text-moonlight text-lg">CityGlow</span>
        </RouterLink>
        <!-- 桌面导航 -->
        <div class="hidden md:flex gap-6 font-body text-sm">
          <RouterLink v-for="link in links" :key="link.to" :to="link.to"
            class="text-starlight/70 hover:text-dark-gold transition-colors uppercase tracking-wider"
            active-class="text-dark-gold">
            {{ link.label }}
          </RouterLink>
        </div>
        <!-- 移动端汉堡按钮 -->
        <button
          class="md:hidden flex items-center justify-center w-10 h-10 rounded text-moonlight hover:text-dark-gold transition-colors"
          :aria-expanded="mobileOpen"
          aria-label="切换菜单"
          @click="mobileOpen = !mobileOpen"
        >
          <svg v-if="!mobileOpen" width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.6" stroke-linecap="round">
            <path d="M4 7h16M4 12h16M4 17h16" />
          </svg>
          <svg v-else width="18" height="18" viewBox="0 0 14 14" fill="none">
            <path d="M2 2 L12 12 M12 2 L2 12" stroke="currentColor" stroke-width="1.5" stroke-linecap="round" />
          </svg>
        </button>
      </nav>
      <!-- 移动端下拉菜单 -->
      <Transition name="dropdown">
        <div v-if="mobileOpen" class="md:hidden border-t border-starlight/10 px-4 py-3 flex flex-col gap-1">
          <RouterLink v-for="link in links" :key="link.to" :to="link.to"
            class="font-body text-sm text-starlight/70 hover:text-dark-gold transition-colors uppercase tracking-wider py-2.5 px-2 rounded"
            active-class="text-dark-gold bg-starlight/5"
            @click="closeMobile">
            {{ link.label }}
          </RouterLink>
        </div>
      </Transition>
    </header>
    <main class="flex-1">
      <RouterView />
    </main>
  </div>
</template>

<style scoped>
.dropdown-enter-active,
.dropdown-leave-active {
  transition: opacity 0.25s ease, transform 0.25s ease;
  transform-origin: top;
}
.dropdown-enter-from,
.dropdown-leave-to {
  opacity: 0;
  transform: scaleY(0.92) translateY(-4px);
}
</style>
