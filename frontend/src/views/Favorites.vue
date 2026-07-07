<script setup lang="ts">
import { onMounted, computed } from 'vue'
import { RouterLink, useRouter } from 'vue-router'
import { useI18n } from 'vue-i18n'
import { useFavoritesStore } from '../stores/favorites'
import type { Favorite } from '../api/favorites'

const { t, locale } = useI18n()
const router = useRouter()
const favoritesStore = useFavoritesStore()

const list = computed(() => favoritesStore.list)
const loading = computed(() => favoritesStore.loading)
const error = computed(() => favoritesStore.error)
const isEmpty = computed(() => !loading.value && list.value.length === 0)

onMounted(() => {
  favoritesStore.fetch().catch(() => {
    // 错误已记录在 store.error 中,此处忽略
  })
})

// 格式化收藏时间
function formatCreatedAt(iso: string): string {
  const d = new Date(iso)
  if (Number.isNaN(d.getTime())) return iso
  return d.toLocaleString(locale.value, {
    year: 'numeric',
    month: '2-digit',
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit',
    hour12: false,
  })
}

// 跳转预报页
function viewForecast(item: Favorite) {
  router.push({
    path: '/forecast',
    query: { lat: item.latitude, lng: item.longitude },
  })
}

// 删除收藏(确认后执行)
async function removeFavorite(item: Favorite) {
  if (!window.confirm(t('favorites.removeConfirm'))) return
  try {
    await favoritesStore.remove(item.latitude, item.longitude)
  } catch {
    // 错误已记录在 store.error 中,此处忽略
  }
}
</script>

<template>
  <section class="favorites-page">
    <!-- 星图等高线背景装饰 -->
    <svg class="contour-deco" viewBox="0 0 800 600" preserveAspectRatio="xMidYMid slice" aria-hidden="true">
      <g fill="none" stroke="rgba(159,168,218,0.07)" stroke-width="0.8">
        <ellipse cx="400" cy="300" rx="160" ry="60" />
        <ellipse cx="400" cy="300" rx="240" ry="95" />
        <ellipse cx="400" cy="300" rx="320" ry="135" />
        <ellipse cx="400" cy="300" rx="400" ry="175" />
      </g>
      <g fill="rgba(232,234,246,0.4)">
        <circle cx="120" cy="140" r="0.9" />
        <circle cx="680" cy="100" r="1.1" />
        <circle cx="160" cy="460" r="1" />
        <circle cx="720" cy="480" r="0.9" />
        <circle cx="540" cy="160" r="0.8" />
        <circle cx="260" cy="440" r="1" />
      </g>
    </svg>

    <div class="favorites-inner">
      <header class="favorites-header text-center">
        <p class="font-mono text-[11px] tracking-[0.4em] uppercase text-dark-gold/80 mb-4">
          Section · My Collection
        </p>
        <h1 class="favorites-title font-display starlight-text">
          {{ t('favorites.title') }}
        </h1>
        <p v-if="!isEmpty" class="favorites-count font-mono">
          {{ list.length }} {{ t('nav.favorites') }}
        </p>
      </header>

      <!-- 加载中 -->
      <div v-if="loading && list.length === 0" class="favorites-state">
        <div class="spinner" />
      </div>

      <!-- 错误 -->
      <div v-else-if="error && list.length === 0" class="favorites-state">
        <p class="font-mono text-xs text-moonlight/60 tracking-wider">{{ error }}</p>
      </div>

      <!-- 空状态 -->
      <div v-else-if="isEmpty" class="favorites-state empty-state">
        <div class="empty-emblem" aria-hidden="true">
          <svg viewBox="0 0 48 48" width="56" height="56" fill="none" stroke="currentColor" stroke-width="1.1" stroke-linecap="round" stroke-linejoin="round">
            <path d="M24 8 L29 19 L41 20.5 L32 29 L34.5 41 L24 35 L13.5 41 L16 29 L7 20.5 L19 19 Z" />
          </svg>
        </div>
        <p class="font-display text-xl text-starlight/80">{{ t('favorites.empty') }}</p>
        <RouterLink to="/" class="back-link font-mono">
          ← {{ t('forecast.goToMap') }}
        </RouterLink>
      </div>

      <!-- 收藏列表 -->
      <ul v-else class="favorites-list">
        <li v-for="item in list" :key="item.id" class="favorite-card glass-panel">
          <button
            type="button"
            class="favorite-main"
            :aria-label="t('favorites.viewForecast')"
            @click="viewForecast(item)"
          >
            <span class="favorite-icon" aria-hidden="true">
              <svg viewBox="0 0 24 24" width="20" height="20" fill="currentColor">
                <path d="M12 17.3 L5.8 21 L7.6 13.5 L2 9 L9.6 8.6 L12 2 L14.4 8.6 L22 9 L16.4 13.5 L18.2 21 Z" />
              </svg>
            </span>
            <span class="favorite-info">
              <span class="favorite-name font-display">{{ item.name }}</span>
              <span class="favorite-coord font-mono">
                {{ item.latitude.toFixed(4) }}, {{ item.longitude.toFixed(4) }}
              </span>
              <span class="favorite-time font-mono">
                {{ t('favorites.addedAt') }} {{ formatCreatedAt(item.createdAt) }}
              </span>
            </span>
          </button>
          <button
            type="button"
            class="favorite-remove"
            :aria-label="t('common.delete')"
            @click="removeFavorite(item)"
          >
            <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.6" stroke-linecap="round" stroke-linejoin="round">
              <path d="M4 7h16" />
              <path d="M9 7V5a1 1 0 0 1 1-1h4a1 1 0 0 1 1 1v2" />
              <path d="M6 7l1 13a1 1 0 0 0 1 1h8a1 1 0 0 0 1-1l1-13" />
            </svg>
          </button>
        </li>
      </ul>

      <RouterLink to="/" class="back-link font-mono back-link--bottom">
        ← {{ t('common.back') }}
      </RouterLink>
    </div>
  </section>
</template>

<style scoped>
.favorites-page {
  position: relative;
  min-height: calc(100vh - 64px);
  display: flex;
  align-items: flex-start;
  justify-content: center;
  overflow: hidden;
  padding: 48px 24px;
}
.contour-deco {
  position: absolute;
  inset: 0;
  width: 100%;
  height: 100%;
  pointer-events: none;
  z-index: 0;
}
.favorites-inner {
  position: relative;
  z-index: 1;
  width: 100%;
  max-width: 640px;
}

.favorites-header {
  margin-bottom: 32px;
}
.favorites-title {
  font-size: clamp(2.2rem, 4.5vw, 2.8rem);
  color: #e8eaf6;
  line-height: 1.05;
  font-weight: 500;
  letter-spacing: 0.02em;
}
.favorites-count {
  margin-top: 10px;
  font-size: 11px;
  letter-spacing: 0.2em;
  text-transform: uppercase;
  color: rgba(159, 168, 218, 0.6);
}

.favorites-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  text-align: center;
  padding: 60px 16px;
  min-height: 240px;
}
.empty-state {
  gap: 18px;
}
.empty-emblem {
  color: rgba(197, 165, 114, 0.4);
  animation: glowPulse 4s ease-in-out infinite;
}
@keyframes glowPulse {
  0%, 100% { opacity: 0.4; }
  50% { opacity: 0.8; }
}

.favorites-list {
  list-style: none;
  margin: 0;
  padding: 0;
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.favorite-card {
  display: flex;
  align-items: stretch;
  border-radius: 10px;
  overflow: hidden;
  transition: border-color 0.25s ease, transform 0.25s ease;
}
.favorite-card:hover {
  border-color: rgba(197, 165, 114, 0.35);
}

.favorite-main {
  flex: 1;
  display: flex;
  align-items: center;
  gap: 14px;
  padding: 16px 18px;
  border: none;
  background: transparent;
  color: inherit;
  cursor: pointer;
  text-align: left;
  transition: background 0.25s ease;
}
.favorite-main:hover {
  background: rgba(197, 165, 114, 0.06);
}

.favorite-icon {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 38px;
  height: 38px;
  border-radius: 50%;
  background: rgba(197, 165, 114, 0.12);
  color: #c5a572;
  flex-shrink: 0;
}

.favorite-info {
  display: flex;
  flex-direction: column;
  gap: 3px;
  min-width: 0;
}
.favorite-name {
  font-size: 1.15rem;
  color: #e8eaf6;
  font-weight: 500;
  line-height: 1.2;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}
.favorite-coord {
  font-size: 11px;
  color: rgba(159, 168, 218, 0.7);
  letter-spacing: 0.05em;
}
.favorite-time {
  font-size: 10px;
  color: rgba(159, 168, 218, 0.45);
  letter-spacing: 0.04em;
}

.favorite-remove {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 48px;
  border: none;
  border-left: 1px solid rgba(232, 234, 246, 0.06);
  background: transparent;
  color: rgba(184, 90, 90, 0.7);
  cursor: pointer;
  transition: all 0.25s ease;
}
.favorite-remove:hover {
  background: rgba(184, 90, 90, 0.12);
  color: #b85a5a;
}

.back-link {
  display: inline-block;
  font-size: 11px;
  letter-spacing: 0.2em;
  text-transform: uppercase;
  color: rgba(159, 168, 218, 0.7);
  border: 1px solid rgba(197, 165, 114, 0.35);
  padding: 11px 22px;
  border-radius: 4px;
  transition: all 0.25s ease;
}
.back-link:hover {
  color: #c5a572;
  border-color: rgba(197, 165, 114, 0.7);
}
.back-link--bottom {
  margin-top: 32px;
}

.spinner {
  width: 22px;
  height: 22px;
  border: 1.5px solid rgba(197, 165, 114, 0.18);
  border-top-color: #c5a572;
  border-radius: 50%;
  animation: spin 0.85s linear infinite;
}
@keyframes spin {
  to { transform: rotate(360deg); }
}

@media (max-width: 600px) {
  .favorites-page {
    padding: 24px 14px;
  }
  .favorite-main {
    padding: 14px 14px;
    gap: 12px;
  }
  .favorite-icon {
    width: 34px;
    height: 34px;
  }
  .favorite-name {
    font-size: 1.05rem;
  }
}
</style>
