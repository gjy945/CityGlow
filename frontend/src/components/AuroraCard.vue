<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { useI18n } from 'vue-i18n'
import { spaceWeatherApi, type AuroraEvent } from '../api/spaceWeather'

const { t, locale } = useI18n()

const props = withDefaults(defineProps<{ days?: number }>(), { days: 30 })

const loading = ref(false)
const error = ref<string | null>(null)
const events = ref<AuroraEvent[]>([])
const totalCount = ref(0)

async function load() {
  loading.value = true
  error.value = null
  try {
    const res = await spaceWeatherApi.getAuroraForecast(props.days)
    events.value = res?.events ?? []
    totalCount.value = res?.totalCount ?? events.value.length
  } catch (e) {
    error.value = (e as Error).message
  } finally {
    loading.value = false
  }
}

onMounted(() => {
  load()
})

// 最近 3 次事件(已按时间倒序)
const recentEvents = computed(() => events.value.slice(0, 3))

// 近 7 天内是否有事件
const hasRecentActivity = computed(() => {
  const sevenDaysAgo = Date.now() - 7 * 24 * 3600 * 1000
  return events.value.some((e) => {
    const t = new Date(e.startTime).getTime()
    return Number.isFinite(t) && t >= sevenDaysAgo
  })
})

function formatTime(iso: string): string {
  const d = new Date(iso)
  if (Number.isNaN(d.getTime())) return '--'
  return d.toLocaleDateString(locale.value, {
    month: 'short',
    day: 'numeric',
    hour: '2-digit',
    minute: '2-digit',
    hour12: false,
  })
}

function typeLabel(type: string): string {
  if (type === 'GST') return t('aurora.gst')
  if (type === 'FLR') return t('aurora.solarFlare')
  return type
}
</script>

<template>
  <div class="aurora-card glass-panel">
    <!-- 极光色顶部光带 -->
    <div class="aurora-ribbon" aria-hidden="true" />

    <header class="aurora-header">
      <div class="aurora-icon" aria-hidden="true">
        <svg viewBox="0 0 32 32" width="22" height="22" fill="none">
          <defs>
            <linearGradient id="auroraGrad" x1="0" y1="0" x2="0" y2="1">
              <stop offset="0%" stop-color="#86efac" />
              <stop offset="60%" stop-color="#4ade80" />
              <stop offset="100%" stop-color="#16a34a" />
            </linearGradient>
          </defs>
          <path
            d="M4 22 C8 14, 14 18, 16 12 C18 6, 24 10, 28 6"
            stroke="url(#auroraGrad)"
            stroke-width="2"
            stroke-linecap="round"
            fill="none"
          />
          <path
            d="M4 26 C8 20, 14 22, 16 18 C18 14, 24 16, 28 12"
            stroke="url(#auroraGrad)"
            stroke-width="1.4"
            stroke-linecap="round"
            fill="none"
            opacity="0.7"
          />
        </svg>
      </div>
      <div class="aurora-titles">
        <p class="font-mono aurora-tag">{{ t('aurora.title') }}</p>
        <p class="font-display aurora-title">{{ t('aurora.recentCount') }}</p>
      </div>
      <div class="aurora-count">
        <span class="font-mono aurora-count-num">{{ totalCount }}</span>
      </div>
    </header>

    <!-- 加载态 -->
    <div v-if="loading && !events.length" class="aurora-state">
      <div class="spinner" />
      <p class="font-mono state-text">{{ t('aurora.loading') }}</p>
    </div>

    <!-- 错误态 -->
    <div v-else-if="error && !events.length" class="aurora-state">
      <p class="font-display state-title">{{ t('aurora.error') }}</p>
      <p class="font-mono state-text">{{ error }}</p>
    </div>

    <!-- 数据展示 -->
    <div v-else class="aurora-body">
      <!-- 警告 / 无活动提示 -->
      <div
        class="aurora-alert"
        :class="{ 'aurora-alert--active': hasRecentActivity }"
      >
        <span v-if="hasRecentActivity" class="alert-icon">⚠️</span>
        <span v-else class="alert-icon alert-icon--ok">✓</span>
        <p class="font-body alert-text">
          {{ hasRecentActivity ? t('aurora.warning') : t('aurora.noActivity') }}
        </p>
      </div>

      <!-- 最近事件列表 -->
      <div v-if="recentEvents.length" class="aurora-events">
        <p class="font-mono events-label">{{ t('aurora.recent') }}</p>
        <ul class="events-list">
          <li v-for="ev in recentEvents" :key="ev.id" class="event-item">
            <span class="event-type" :class="`event-type--${ev.type}`">
              {{ typeLabel(ev.type) }}
            </span>
            <span class="event-time font-mono">{{ formatTime(ev.startTime) }}</span>
            <a
              v-if="ev.link"
              :href="ev.link"
              target="_blank"
              rel="noopener noreferrer"
              class="event-link"
              :aria-label="ev.id"
            >
              <svg width="12" height="12" viewBox="0 0 12 12" fill="none">
                <path
                  d="M3 9 L9 3 M5 3 H9 V7"
                  stroke="currentColor"
                  stroke-width="1.2"
                  stroke-linecap="round"
                  stroke-linejoin="round"
                />
              </svg>
            </a>
          </li>
        </ul>
      </div>
    </div>
  </div>
</template>

<style scoped>
.aurora-card {
  position: relative;
  border-radius: 12px;
  padding: 18px 18px 16px;
  overflow: hidden;
}

.aurora-ribbon {
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  height: 2px;
  background: linear-gradient(
    90deg,
    transparent 0%,
    rgba(74, 222, 128, 0.6) 30%,
    rgba(134, 239, 172, 0.9) 50%,
    rgba(74, 222, 128, 0.6) 70%,
    transparent 100%
  );
  filter: blur(0.4px);
}

.aurora-header {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 14px;
}

.aurora-icon {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 36px;
  height: 36px;
  border-radius: 50%;
  background: rgba(74, 222, 128, 0.1);
  border: 1px solid rgba(74, 222, 128, 0.32);
  flex-shrink: 0;
}

.aurora-titles {
  flex: 1;
  min-width: 0;
}

.aurora-tag {
  font-size: 9px;
  letter-spacing: 0.28em;
  text-transform: uppercase;
  color: rgba(74, 222, 128, 0.85);
}

.aurora-title {
  font-size: 1.05rem;
  color: #e8eaf6;
  margin-top: 2px;
  line-height: 1.2;
}

.aurora-count {
  flex-shrink: 0;
  display: flex;
  align-items: baseline;
  gap: 4px;
}

.aurora-count-num {
  font-size: 1.8rem;
  font-weight: 700;
  color: #4ade80;
  text-shadow: 0 0 12px rgba(74, 222, 128, 0.5);
  line-height: 1;
}

.aurora-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: 24px 12px;
  gap: 10px;
}

.state-title {
  font-size: 1rem;
  color: rgba(232, 234, 246, 0.7);
}

.state-text {
  font-size: 10px;
  letter-spacing: 0.18em;
  color: rgba(159, 168, 218, 0.55);
  text-align: center;
  word-break: break-all;
}

.aurora-body {
  display: flex;
  flex-direction: column;
  gap: 14px;
}

.aurora-alert {
  display: flex;
  align-items: flex-start;
  gap: 8px;
  padding: 10px 12px;
  border-radius: 8px;
  background: rgba(74, 222, 128, 0.06);
  border: 1px solid rgba(74, 222, 128, 0.18);
}

.aurora-alert--active {
  background: rgba(74, 222, 128, 0.12);
  border-color: rgba(74, 222, 128, 0.4);
  box-shadow: 0 0 18px rgba(74, 222, 128, 0.12);
}

.alert-icon {
  font-size: 14px;
  line-height: 1.4;
  flex-shrink: 0;
}

.alert-icon--ok {
  color: #4ade80;
}

.alert-text {
  font-size: 12.5px;
  line-height: 1.5;
  color: rgba(232, 234, 246, 0.82);
}

.aurora-events {
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.events-label {
  font-size: 9px;
  letter-spacing: 0.28em;
  text-transform: uppercase;
  color: rgba(197, 165, 114, 0.7);
  margin-bottom: 4px;
}

.events-list {
  list-style: none;
  padding: 0;
  margin: 0;
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.event-item {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 8px 10px;
  border-radius: 6px;
  background: rgba(10, 14, 26, 0.45);
  border: 1px solid rgba(232, 234, 246, 0.05);
  transition: border-color 0.25s ease;
}

.event-item:hover {
  border-color: rgba(74, 222, 128, 0.25);
}

.event-type {
  font-family: 'Manrope', sans-serif;
  font-size: 10px;
  letter-spacing: 0.14em;
  text-transform: uppercase;
  padding: 2px 7px;
  border-radius: 3px;
  background: rgba(74, 222, 128, 0.1);
  color: #4ade80;
  border: 1px solid rgba(74, 222, 128, 0.25);
  flex-shrink: 0;
}

.event-type--FLR {
  background: rgba(245, 158, 11, 0.1);
  color: #f59e0b;
  border-color: rgba(245, 158, 11, 0.25);
}

.event-time {
  flex: 1;
  font-size: 11px;
  color: rgba(232, 234, 246, 0.78);
  letter-spacing: 0.04em;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.event-link {
  display: inline-flex;
  align-items: center;
  color: rgba(159, 168, 218, 0.65);
  transition: color 0.2s ease;
  flex-shrink: 0;
}

.event-link:hover {
  color: #4ade80;
}

.spinner {
  width: 18px;
  height: 18px;
  border: 1.5px solid rgba(74, 222, 128, 0.22);
  border-top-color: #4ade80;
  border-radius: 50%;
  animation: spin 0.85s linear infinite;
}

@keyframes spin {
  to {
    transform: rotate(360deg);
  }
}
</style>
