<script setup lang="ts">
import { computed, watch, onMounted } from 'vue'
import { useRoute, useRouter, RouterLink } from 'vue-router'
import { useI18n } from 'vue-i18n'
import { useForecastStore } from '../stores/forecast'
import { useFavoritesStore } from '../stores/favorites'
import { useAuthStore } from '../stores/auth'
import MoonPhaseCanvas from '../components/MoonPhaseCanvas.vue'

const { t, locale } = useI18n()

interface Props {
  lat?: number
  lng?: number
  embedded?: boolean
}
const props = withDefaults(defineProps<Props>(), {
  lat: undefined,
  lng: undefined,
  embedded: false,
})
const emit = defineEmits<{ (e: 'close'): void }>()

const route = useRoute()
const router = useRouter()
const forecastStore = useForecastStore()
const favoritesStore = useFavoritesStore()
const authStore = useAuthStore()

// 优先 props,否则回退路由 query
const resolvedLat = computed<number | undefined>(() => {
  if (props.lat != null) return props.lat
  const q = route.query.lat
  if (q == null) return undefined
  const n = Number(q)
  return Number.isFinite(n) ? n : undefined
})
const resolvedLng = computed<number | undefined>(() => {
  if (props.lng != null) return props.lng
  const q = route.query.lng
  if (q == null) return undefined
  const n = Number(q)
  return Number.isFinite(n) ? n : undefined
})

const hasCoords = computed(
  () => resolvedLat.value != null && resolvedLng.value != null,
)

const data = computed(() => forecastStore.data)
const loading = computed(() => forecastStore.loading)
const error = computed(() => forecastStore.error)
const isRefreshing = computed(() => loading.value && data.value != null)

async function load() {
  if (!hasCoords.value) return
  await forecastStore.fetchForecast(resolvedLat.value!, resolvedLng.value!)
}

watch([resolvedLat, resolvedLng], () => {
  load()
})
onMounted(() => {
  load()
  // 已登录时预加载收藏列表,以正确显示星标状态
  if (authStore.isLoggedIn && favoritesStore.list.length === 0) {
    favoritesStore.fetch().catch(() => {
      // 错误已记录在 store.error 中,此处忽略
    })
  }
})

// 指数颜色映射:80+ 暗金 / 60-79 月光蓝 / 40-59 橙色 / <40 红
function scoreColor(score: number): string {
  if (score >= 80) return '#c5a572'
  if (score >= 60) return '#9fa8da'
  if (score >= 40) return '#c98850'
  return '#b85a5a'
}
const scoreColorHex = computed(() => scoreColor(data.value?.score ?? 0))

// SVG 环形进度
const ringRadius = 70
const ringCircumference = 2 * Math.PI * ringRadius
const ringOffset = computed(() => {
  const score = data.value?.score ?? 0
  return ringCircumference * (1 - Math.max(0, Math.min(100, score)) / 100)
})

const coordLabel = computed(() => {
  if (!hasCoords.value) return ''
  return `lat ${resolvedLat.value!.toFixed(4)}  ·  lng ${resolvedLng.value!.toFixed(4)}`
})

// unix 秒 -> HH:mm
function formatTime(unixSeconds: number): string {
  if (!unixSeconds) return '--:--'
  const d = new Date(unixSeconds * 1000)
  if (Number.isNaN(d.getTime())) return '--:--'
  return d.toLocaleTimeString(locale.value, {
    hour: '2-digit',
    minute: '2-digit',
    hour12: false,
  })
}

// 天文昏影终(估算法:日落后约 90 分钟,中纬度近似)
function estimateTwilightEnd(): string {
  if (!data.value?.sunset) return '--:--'
  return formatTime(data.value.sunset + 90 * 60)
}

// 收藏状态:当前坐标是否已收藏
const favorited = computed(() => {
  if (!hasCoords.value) return false
  return favoritesStore.isFavorite(resolvedLat.value!, resolvedLng.value!)
})

// 切换收藏/取消收藏
async function toggleFavorite() {
  if (!hasCoords.value) return
  const lat = resolvedLat.value!
  const lng = resolvedLng.value!
  // 未登录 → 跳转登录页(带回跳地址)
  if (!authStore.isLoggedIn) {
    router.push({ name: 'login', query: { redirect: route.fullPath } })
    return
  }
  try {
    if (favoritesStore.isFavorite(lat, lng)) {
      await favoritesStore.remove(lat, lng)
    } else {
      // 默认名称用坐标,便于在收藏列表中识别
      await favoritesStore.add(`${lat.toFixed(4)}, ${lng.toFixed(4)}`, lat, lng)
    }
  } catch {
    // 错误已记录在 store.error 中,此处忽略
  }
}
</script>

<template>
  <component :is="embedded ? 'aside' : 'section'" :class="embedded ? 'forecast-embedded' : 'forecast-page'">
    <div :class="embedded ? 'forecast-panel glass-panel moon-glow forecast-panel--embedded' : 'forecast-panel glass-panel moon-glow forecast-panel--standalone'">
      <!-- 关闭 / 返回 -->
      <button
        v-if="embedded"
        class="forecast-close"
        @click="emit('close')"
        :aria-label="t('forecast.close')"
      >
        <svg width="14" height="14" viewBox="0 0 14 14" fill="none">
          <path
            d="M2 2 L12 12 M12 2 L2 12"
            stroke="currentColor"
            stroke-width="1.5"
            stroke-linecap="round"
          />
        </svg>
      </button>
      <RouterLink v-else to="/" class="forecast-back">
        <svg width="14" height="14" viewBox="0 0 14 14" fill="none">
          <path
            d="M9 2 L4 7 L9 12"
            stroke="currentColor"
            stroke-width="1.5"
            stroke-linecap="round"
            stroke-linejoin="round"
          />
        </svg>
        {{ t('forecast.backToMap') }}
      </RouterLink>

      <!-- 加载中(无数据) -->
      <div v-if="loading && !data" class="forecast-state">
        <div class="spinner" />
        <p class="font-mono text-xs text-moonlight/60 mt-4 tracking-wider">
          {{ t('forecast.calculating') }}
        </p>
      </div>

      <!-- 错误(无数据) -->
      <div v-else-if="error && !data" class="forecast-state">
        <p class="font-display text-xl text-starlight/80">{{ t('forecast.noData') }}</p>
        <p class="font-mono text-[10px] text-moonlight/50 mt-3 break-all">
          {{ error }}
        </p>
      </div>

      <!-- 无坐标 -->
      <div v-else-if="!hasCoords" class="forecast-state">
        <p class="font-mono text-[10px] uppercase tracking-[0.3em] text-dark-gold/80">
          {{ t('forecast.noCoords') }}
        </p>
        <p class="font-display text-2xl text-starlight starlight-text mt-3">
          {{ t('forecast.selectLocation') }}
        </p>
        <p class="font-body text-sm text-moonlight/60 mt-3 leading-relaxed max-w-[280px]">
          {{ t('forecast.selectLocationHint') }}
        </p>
        <RouterLink
          to="/"
          class="mt-6 inline-block font-mono text-[11px] uppercase tracking-[0.2em] text-dark-gold hover:text-starlight transition-colors border border-dark-gold/40 hover:border-starlight/60 px-5 py-2 rounded"
        >
          {{ t('forecast.goToMap') }}
        </RouterLink>
      </div>

      <!-- 数据展示 -->
      <div v-else-if="data" class="forecast-content">
        <!-- 刷新条 -->
        <div v-if="isRefreshing" class="refresh-bar">
          <div class="refresh-bar-inner" />
        </div>

        <!-- 标题 -->
        <header class="forecast-header">
          <div class="forecast-header-row">
            <p class="font-mono text-[10px] uppercase tracking-[0.3em] text-dark-gold/80">
              {{ t('forecast.sectionTag') }}
            </p>
            <button
              v-if="hasCoords"
              type="button"
              class="favorite-toggle"
              :class="{ 'favorite-toggle--active': favorited }"
              :aria-label="favorited ? t('forecast.removeFromFavorites') : t('forecast.addToFavorites')"
              :title="favorited ? t('forecast.removeFromFavorites') : t('forecast.addToFavorites')"
              @click="toggleFavorite"
            >
              <svg viewBox="0 0 24 24" width="18" height="18"
                :fill="favorited ? 'currentColor' : 'none'"
                stroke="currentColor" stroke-width="1.5" stroke-linecap="round" stroke-linejoin="round">
                <path d="M12 17.3 L5.8 21 L7.6 13.5 L2 9 L9.6 8.6 L12 2 L14.4 8.6 L22 9 L16.4 13.5 L18.2 21 Z" />
              </svg>
            </button>
          </div>
          <h2 class="font-display text-2xl text-starlight starlight-text mt-1">
            {{ t('forecast.score') }}
          </h2>
          <p
            v-if="coordLabel"
            class="font-mono text-[10px] text-moonlight/50 mt-1 tracking-wider"
          >
            {{ coordLabel }}
          </p>
        </header>

        <!-- 指数环 -->
        <div class="forecast-score">
          <svg
            width="180"
            height="180"
            viewBox="0 0 180 180"
            class="score-ring"
          >
            <defs>
              <filter
                id="scoreGlow"
                x="-50%"
                y="-50%"
                width="200%"
                height="200%"
              >
                <feGaussianBlur stdDeviation="2.8" result="blur" />
                <feMerge>
                  <feMergeNode in="blur" />
                  <feMergeNode in="SourceGraphic" />
                </feMerge>
              </filter>
            </defs>
            <circle
              cx="90"
              cy="90"
              r="70"
              fill="none"
              stroke="rgba(232,234,246,0.08)"
              stroke-width="5"
            />
            <circle
              cx="90"
              cy="90"
              r="70"
              fill="none"
              :stroke="scoreColorHex"
              stroke-width="5"
              stroke-linecap="round"
              :stroke-dasharray="ringCircumference"
              :stroke-dashoffset="ringOffset"
              transform="rotate(-90 90 90)"
              filter="url(#scoreGlow)"
              class="score-progress"
            />
            <text
              x="90"
              y="86"
              text-anchor="middle"
              font-family="JetBrains Mono, monospace"
              font-size="48"
              font-weight="700"
              :fill="scoreColorHex"
            >
              {{ data.score }}
            </text>
            <text
              x="90"
              y="112"
              text-anchor="middle"
              font-family="Manrope, sans-serif"
              font-size="9"
              letter-spacing="2"
              fill="rgba(232,234,246,0.5)"
            >
              / 100
            </text>
          </svg>
          <p class="font-display text-xl text-starlight starlight-text mt-3 text-center">
            {{ data.message }}
          </p>
        </div>

        <!-- 月相 -->
        <div class="forecast-section forecast-moon">
          <MoonPhaseCanvas :moon-phase="data.moonPhase" :size="100" />
          <p class="font-display text-base text-moonlight mt-2">
            {{ data.moonPhase }}
          </p>
        </div>

        <!-- 数据行 -->
        <div class="forecast-section forecast-data">
          <div class="data-row">
            <span class="data-label">{{ t('forecast.cloudCover') }}</span>
            <span class="data-value">
              {{ Math.round(data.cloudCover) }}<span class="data-unit">%</span>
            </span>
          </div>
          <div class="data-row">
            <span class="data-label">{{ t('forecast.bortle') }}</span>
            <span class="data-value">
              {{ data.bortleLevel }}<span class="data-unit">{{ t('common.levelUnit') }}</span>
            </span>
          </div>
        </div>

        <!-- 时间轴 -->
        <div class="forecast-section forecast-timeline">
          <p class="timeline-label">{{ t('forecast.nightTimeline') }}</p>
          <div class="data-row">
            <span class="data-label">{{ t('forecast.sunset') }}</span>
            <span class="data-value">{{ formatTime(data.sunset) }}</span>
          </div>
          <div class="data-row">
            <span class="data-label">{{ t('forecast.twilightEnd') }}</span>
            <span class="data-value">
              {{ estimateTwilightEnd() }}<span class="data-unit">~</span>
            </span>
          </div>
          <div class="data-row">
            <span class="data-label">{{ t('forecast.sunrise') }}</span>
            <span class="data-value">{{ formatTime(data.sunrise) }}</span>
          </div>
        </div>
      </div>
    </div>
  </component>
</template>

<style scoped>
.forecast-embedded {
  width: 100%;
  height: 100%;
  display: flex;
}

.forecast-page {
  min-height: calc(100vh - 64px);
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 32px 24px;
}

.forecast-panel {
  position: relative;
  border-radius: 14px;
  overflow: hidden;
}
.forecast-panel--embedded {
  flex: 1;
  display: flex;
  flex-direction: column;
  padding: 28px 26px 32px;
  overflow-y: auto;
}
.forecast-panel--standalone {
  width: 100%;
  max-width: 460px;
  padding: 28px 26px 32px;
}

.forecast-close {
  position: absolute;
  top: 14px;
  right: 14px;
  z-index: 5;
  width: 32px;
  height: 32px;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: 50%;
  background: rgba(10, 14, 26, 0.5);
  border: 1px solid rgba(232, 234, 246, 0.12);
  color: #9fa8da;
  cursor: pointer;
  transition: all 0.25s ease;
}
.forecast-close:hover {
  color: #c5a572;
  border-color: rgba(197, 165, 114, 0.5);
  background: rgba(197, 165, 114, 0.12);
  transform: rotate(90deg);
}

.forecast-back {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  font-family: 'JetBrains Mono', monospace;
  font-size: 11px;
  letter-spacing: 0.15em;
  text-transform: uppercase;
  color: rgba(159, 168, 218, 0.7);
  margin-bottom: 18px;
  transition: color 0.25s ease;
}
.forecast-back:hover {
  color: #c5a572;
}

.forecast-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  text-align: center;
  padding: 60px 16px;
  min-height: 280px;
}

.forecast-content {
  position: relative;
}

.refresh-bar {
  position: absolute;
  top: -28px;
  left: -26px;
  right: -26px;
  height: 2px;
  background: rgba(197, 165, 114, 0.1);
  overflow: hidden;
}
.refresh-bar-inner {
  height: 100%;
  width: 40%;
  background: linear-gradient(90deg, transparent, #c5a572, transparent);
  animation: refreshSlide 1.4s ease-in-out infinite;
}
@keyframes refreshSlide {
  0% { transform: translateX(-100%); }
  100% { transform: translateX(350%); }
}

.forecast-header {
  margin-bottom: 22px;
}
.forecast-header-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
}

/* 收藏切换按钮:未收藏空心(月光蓝),已收藏填充暗金 */
.favorite-toggle {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 32px;
  height: 32px;
  border-radius: 50%;
  background: rgba(10, 14, 26, 0.4);
  border: 1px solid rgba(232, 234, 246, 0.12);
  color: rgba(159, 168, 218, 0.7);
  cursor: pointer;
  transition: all 0.25s ease;
}
.favorite-toggle:hover {
  color: #c5a572;
  border-color: rgba(197, 165, 114, 0.5);
  background: rgba(197, 165, 114, 0.1);
}
.favorite-toggle--active {
  color: #c5a572;
  border-color: rgba(197, 165, 114, 0.55);
  background: rgba(197, 165, 114, 0.12);
}

.forecast-score {
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: 8px 0 20px;
}

.score-ring {
  display: block;
}
.score-progress {
  transition: stroke-dashoffset 0.9s cubic-bezier(0.4, 0, 0.2, 1),
    stroke 0.4s ease;
}

.forecast-section {
  border-top: 1px solid rgba(232, 234, 246, 0.08);
  padding: 20px 0;
}

.forecast-moon {
  display: flex;
  flex-direction: column;
  align-items: center;
}

.forecast-data,
.forecast-timeline {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.timeline-label {
  font-family: 'JetBrains Mono', monospace;
  font-size: 10px;
  letter-spacing: 0.25em;
  text-transform: uppercase;
  color: rgba(197, 165, 114, 0.7);
  margin-bottom: 4px;
}

.data-row {
  display: flex;
  align-items: baseline;
  justify-content: space-between;
}
.data-label {
  font-family: 'Manrope', sans-serif;
  font-size: 13px;
  color: rgba(232, 234, 246, 0.55);
  letter-spacing: 0.05em;
}
.data-value {
  font-family: 'JetBrains Mono', monospace;
  font-size: 18px;
  font-weight: 500;
  color: #e8eaf6;
}
.data-unit {
  font-size: 11px;
  color: rgba(232, 234, 246, 0.45);
  margin-left: 2px;
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

@media (max-width: 768px) {
  .forecast-page {
    padding: 16px 12px;
  }
}
</style>
