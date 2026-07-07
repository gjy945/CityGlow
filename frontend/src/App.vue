<script setup lang="ts">
import { ref, onMounted, onUnmounted, computed } from 'vue'
import { RouterLink, RouterView, useRouter } from 'vue-router'
import { useI18n } from 'vue-i18n'
import LanguageSwitcher from './components/LanguageSwitcher.vue'
import { useAuthStore } from './stores/auth'

const { t } = useI18n()
const router = useRouter()
const authStore = useAuthStore()

const links = [
  { to: '/', key: 'nav.darkSky' },
  { to: '/forecast', key: 'nav.forecast' },
  { to: '/timeline', key: 'nav.timeline' },
  { to: '/postcard', key: 'nav.postcard' },
]

const mobileOpen = ref(false)
const userMenuOpen = ref(false)
const userMenuRef = ref<HTMLElement | null>(null)

const displayName = computed(() => authStore.user?.username ?? '')

function closeMobile() {
  mobileOpen.value = false
}

function closeUserMenu() {
  userMenuOpen.value = false
}

function onDocClick(e: MouseEvent) {
  if (userMenuRef.value && !userMenuRef.value.contains(e.target as Node)) {
    userMenuOpen.value = false
  }
}

function onKey(e: KeyboardEvent) {
  if (e.key === 'Escape') userMenuOpen.value = false
}

onMounted(() => {
  document.addEventListener('click', onDocClick)
  document.addEventListener('keydown', onKey)
  // 若本地存有 token,拉取用户信息以填充导航栏用户菜单
  if (authStore.isLoggedIn) {
    authStore.fetchMe().catch(() => {
      // token 无效已在拦截器/store 中处理,此处忽略
    })
  }
})
onUnmounted(() => {
  document.removeEventListener('click', onDocClick)
  document.removeEventListener('keydown', onKey)
})

function logout() {
  authStore.logout()
  closeUserMenu()
  closeMobile()
  router.push('/')
}
</script>

<template>
  <div class="min-h-screen flex flex-col">
    <header class="glass-panel sticky top-0 z-50 border-b border-starlight/10">
      <nav class="max-w-7xl mx-auto px-4 sm:px-6 py-4 flex items-center justify-between gap-4">
        <RouterLink to="/" class="font-display text-2xl text-starlight starlight-text shrink-0" @click="closeMobile">
          城市光影 <span class="text-dark-gold">·</span> <span class="text-moonlight text-lg">CityGlow</span>
        </RouterLink>
        <!-- 桌面导航 -->
        <div class="hidden md:flex items-center gap-6 font-body text-sm">
          <RouterLink v-for="link in links" :key="link.to" :to="link.to"
            class="text-starlight/70 hover:text-dark-gold transition-colors uppercase tracking-wider"
            active-class="text-dark-gold">
            {{ t(link.key) }}
          </RouterLink>
        </div>
        <!-- 右侧操作区:语言切换 + 用户菜单 -->
        <div class="flex items-center gap-2 sm:gap-3">
          <LanguageSwitcher class="hidden sm:block" />

          <!-- 未登录:登录按钮 -->
          <RouterLink v-if="!authStore.isLoggedIn" to="/login"
            class="hidden md:inline-flex items-center font-body text-sm text-dark-gold hover:text-starlight transition-colors uppercase tracking-wider border border-dark-gold/40 hover:border-starlight/60 px-4 py-1.5 rounded">
            {{ t('nav.login') }}
          </RouterLink>

          <!-- 已登录:用户菜单 -->
          <div v-else ref="userMenuRef" class="hidden md:block relative">
            <button
              type="button"
              class="user-trigger"
              :aria-expanded="userMenuOpen"
              aria-haspopup="menu"
              @click="userMenuOpen = !userMenuOpen"
            >
              <span class="user-avatar" aria-hidden="true">
                {{ displayName.charAt(0).toUpperCase() }}
              </span>
              <span class="user-name">{{ displayName }}</span>
              <svg
                class="user-caret"
                :class="{ 'user-caret--open': userMenuOpen }"
                width="10" height="10" viewBox="0 0 12 12" fill="none"
                stroke="currentColor" stroke-width="1.5" stroke-linecap="round" stroke-linejoin="round"
                aria-hidden="true"
              >
                <path d="M3 4.5 L6 7.5 L9 4.5" />
              </svg>
            </button>
            <Transition name="user-dropdown">
              <div v-if="userMenuOpen" class="user-menu glass-panel" role="menu">
                <RouterLink to="/favorites" class="user-menu-item" role="menuitem" @click="closeUserMenu">
                  <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true">
                    <path d="M12 17.3 L5.8 21 L7.6 13.5 L2 9 L9.6 8.6 L12 2 L14.4 8.6 L22 9 L16.4 13.5 L18.2 21 Z" />
                  </svg>
                  {{ t('nav.favorites') }}
                </RouterLink>
                <button type="button" class="user-menu-item user-menu-item--danger" role="menuitem" @click="logout">
                  <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true">
                    <path d="M15 12H4" />
                    <path d="M8 8 L4 12 L8 16" />
                    <path d="M10 4 H19 V20 H10" />
                  </svg>
                  {{ t('nav.logout') }}
                </button>
              </div>
            </Transition>
          </div>

          <!-- 移动端汉堡按钮 -->
          <button
            class="md:hidden flex items-center justify-center w-10 h-10 rounded text-moonlight hover:text-dark-gold transition-colors"
            :aria-expanded="mobileOpen"
            aria-label="Menu"
            @click="mobileOpen = !mobileOpen"
          >
            <svg v-if="!mobileOpen" width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.6" stroke-linecap="round">
              <path d="M4 7h16M4 12h16M4 17h16" />
            </svg>
            <svg v-else width="18" height="18" viewBox="0 0 14 14" fill="none">
              <path d="M2 2 L12 12 M12 2 L2 12" stroke="currentColor" stroke-width="1.5" stroke-linecap="round" />
            </svg>
          </button>
        </div>
      </nav>
      <!-- 移动端下拉菜单 -->
      <Transition name="dropdown">
        <div v-if="mobileOpen" class="md:hidden border-t border-starlight/10 px-4 py-3 flex flex-col gap-1">
          <RouterLink v-for="link in links" :key="link.to" :to="link.to"
            class="font-body text-sm text-starlight/70 hover:text-dark-gold transition-colors uppercase tracking-wider py-2.5 px-2 rounded"
            active-class="text-dark-gold bg-starlight/5"
            @click="closeMobile">
            {{ t(link.key) }}
          </RouterLink>
          <div class="my-2 h-px bg-starlight/10"></div>
          <LanguageSwitcher class="sm:hidden" />
          <RouterLink v-if="!authStore.isLoggedIn" to="/login"
            class="font-body text-sm text-dark-gold hover:text-starlight transition-colors uppercase tracking-wider py-2.5 px-2 rounded"
            @click="closeMobile">
            {{ t('nav.login') }}
          </RouterLink>
          <template v-else>
            <div class="flex items-center gap-3 py-2 px-2">
              <span class="user-avatar user-avatar--sm" aria-hidden="true">
                {{ displayName.charAt(0).toUpperCase() }}
              </span>
              <span class="font-body text-sm text-starlight">{{ displayName }}</span>
            </div>
            <RouterLink to="/favorites"
              class="font-body text-sm text-starlight/70 hover:text-dark-gold transition-colors uppercase tracking-wider py-2.5 px-2 rounded"
              @click="closeMobile">
              {{ t('nav.favorites') }}
            </RouterLink>
            <button type="button"
              class="text-left font-body text-sm text-starlight/70 hover:text-dark-gold transition-colors uppercase tracking-wider py-2.5 px-2 rounded"
              @click="logout">
              {{ t('nav.logout') }}
            </button>
          </template>
        </div>
      </Transition>
    </header>
    <main class="flex-1">
      <RouterView />
    </main>
  </div>
</template>

<style scoped>
/* 用户菜单触发器 */
.user-trigger {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  padding: 5px 10px 5px 5px;
  border-radius: 999px;
  border: 1px solid rgba(197, 165, 114, 0.32);
  background: rgba(26, 31, 58, 0.55);
  backdrop-filter: blur(12px);
  -webkit-backdrop-filter: blur(12px);
  color: rgba(232, 234, 246, 0.85);
  cursor: pointer;
  transition: all 0.25s ease;
}
.user-trigger:hover {
  border-color: rgba(197, 165, 114, 0.7);
  color: #c5a572;
}

.user-avatar {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 28px;
  height: 28px;
  border-radius: 50%;
  background: linear-gradient(135deg, #c5a572 0%, #9fa8da 100%);
  color: #0a0e1a;
  font-family: 'Cormorant Garamond', serif;
  font-size: 14px;
  font-weight: 600;
  flex-shrink: 0;
}
.user-avatar--sm {
  width: 24px;
  height: 24px;
  font-size: 12px;
}

.user-name {
  font-family: 'Manrope', sans-serif;
  font-size: 13px;
  max-width: 120px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.user-caret {
  transition: transform 0.25s ease;
  opacity: 0.7;
}
.user-caret--open {
  transform: rotate(180deg);
}

.user-menu {
  position: absolute;
  top: calc(100% + 8px);
  right: 0;
  min-width: 172px;
  padding: 6px;
  border-radius: 10px;
  border: 1px solid rgba(197, 165, 114, 0.28);
  box-shadow: 0 12px 36px rgba(0, 0, 0, 0.45);
  z-index: 60;
}
.user-menu-item {
  display: flex;
  align-items: center;
  gap: 10px;
  width: 100%;
  padding: 9px 11px;
  border: none;
  background: transparent;
  border-radius: 6px;
  color: rgba(232, 234, 246, 0.72);
  cursor: pointer;
  transition: all 0.2s ease;
  text-align: left;
  font-family: 'Manrope', sans-serif;
  font-size: 13px;
}
.user-menu-item:hover {
  background: rgba(197, 165, 114, 0.1);
  color: #e8eaf6;
}
.user-menu-item--danger:hover {
  background: rgba(184, 90, 90, 0.1);
  color: #b85a5a;
}

.user-dropdown-enter-active,
.user-dropdown-leave-active {
  transition: opacity 0.22s ease, transform 0.22s ease;
  transform-origin: top right;
}
.user-dropdown-enter-from,
.user-dropdown-leave-to {
  opacity: 0;
  transform: scale(0.94) translateY(-4px);
}

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
