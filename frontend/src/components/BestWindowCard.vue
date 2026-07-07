<script setup lang="ts">
import { ref, computed, watch, onMounted } from 'vue'
import { useI18n } from 'vue-i18n'
import { bestWindowApi, type BestWindow } from '../api/bestWindow'

const { t } = useI18n()

const props = withDefaults(defineProps<{ lat?: number; lng?: number }>(), {
  lat: undefined,
  lng: undefined,
})

const loading = ref(false)
const error = ref<string | null>(null)
const data = ref<BestWindow | null>(null)

async function load() {
  if (props.lat == null || props.lng == null) return
  if (!Number.isFinite(props.lat) || !Number.isFinite(props.lng)) return
  loading.value = true
  error.value = null
  try {
    data.value = await bestWindowApi.getBestWindow(props.lat, props.lng)
  } catch (e) {
    error.value = (e as Error).message
  } finally {
    loading.value = false
  }
}

watch(
  () => [props.lat, props.lng],
  () => {
    load()
  },
)

onMounted(() => {
  load()
})

// 评分颜色:80+ 暗金 / 60-79 月光蓝 / 40-59 橙色 / <40 红
const scoreColor = computed(() => {
  const s = data.value?.score ?? 0
  if (s >= 80) return '#c5a572'
  if (s >= 60) return '#9fa8da'
  if (s >= 40) return '#c98850'
  return '#b85a5a'
})

const hasCoords = computed(
  () => props.lat != null && props.lng != null && Number.isFinite(props.lat) && Number.isFinite(props.lng),
)
</script>

<template>
  <div class="bw-card glass-panel">
    <!-- 暗金顶部光带 -->
    <div class="bw-ribbon" aria-hidden="true" />

    <header class="bw-header">
      <div class="bw-icon" aria-hidden="true">
        <svg viewBox="0 0 24 24" width="18" height="18" fill="none">
          <circle cx="12" cy="12" r="9" stroke="currentColor" stroke-width="1.4" />
          <path
            d="M12 7 V12 L15.5 14"
            stroke="currentColor"
            stroke-width="1.6"
            stroke-linecap="round"
            stroke-linejoin="round"
          />
        </svg>
      </div>
      <p class="font-display bw-title">{{ t('bestWindow.title') }}</p>
    </header>

    <!-- 无坐标 -->
    <div v-if="!hasCoords" class="bw-state">
      <p class="font-mono state-text">{{ t('bestWindow.error') }}</p>
    </div>

    <!-- 加载态 -->
    <div v-else-if="loading && !data" class="bw-state">
      <div class="spinner" />
      <p class="font-mono state-text">{{ t('bestWindow.loading') }}</p>
    </div>

    <!-- 错误态 -->
    <div v-else-if="error && !data" class="bw-state">
      <p class="font-display state-title">{{ t('bestWindow.error') }}</p>
      <p class="font-mono state-text">{{ error }}</p>
    </div>

    <!-- 数据展示 -->
    <div v-else-if="data" class="bw-body">
      <div class="bw-window-row">
        <div class="bw-time">
          <span class="font-mono bw-time-start">{{ data.start || '--:--' }}</span>
          <span class="bw-sep">{{ t('bestWindow.to') }}</span>
          <span class="font-mono bw-time-end">{{ data.end || '--:--' }}</span>
        </div>
        <div class="bw-score" :style="{ '--score-color': scoreColor }">
          <span class="font-mono bw-score-num">{{ data.score }}</span>
          <span class="bw-score-max">/ 100</span>
        </div>
      </div>

      <p v-if="data.message" class="font-body bw-message">{{ data.message }}</p>

      <div v-if="data.reasons?.length" class="bw-reasons">
        <p class="font-mono bw-reasons-label">{{ t('bestWindow.reasons') }}</p>
        <ul class="reasons-list">
          <li v-for="(r, idx) in data.reasons" :key="idx" class="reason-item">
            <span class="reason-check" aria-hidden="true">✓</span>
            <span class="font-body reason-text">{{ r }}</span>
          </li>
        </ul>
      </div>
    </div>
  </div>
</template>

<style scoped>
.bw-card {
  position: relative;
  border-radius: 12px;
  padding: 18px 18px 16px;
  overflow: hidden;
}

.bw-ribbon {
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  height: 2px;
  background: linear-gradient(
    90deg,
    transparent 0%,
    rgba(197, 165, 114, 0.6) 30%,
    rgba(232, 234, 246, 0.85) 50%,
    rgba(197, 165, 114, 0.6) 70%,
    transparent 100%
  );
}

.bw-header {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-bottom: 14px;
}

.bw-icon {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 30px;
  height: 30px;
  border-radius: 50%;
  background: rgba(197, 165, 114, 0.1);
  border: 1px solid rgba(197, 165, 114, 0.32);
  color: #c5a572;
  flex-shrink: 0;
}

.bw-title {
  font-size: 1.05rem;
  color: #e8eaf6;
  line-height: 1.2;
  font-weight: 500;
}

.bw-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: 22px 12px;
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

.bw-body {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.bw-window-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  padding: 6px 4px 4px;
}

.bw-time {
  display: flex;
  align-items: baseline;
  gap: 8px;
}

.bw-time-start,
.bw-time-end {
  font-size: 1.7rem;
  font-weight: 700;
  color: #c5a572;
  text-shadow: 0 0 14px rgba(197, 165, 114, 0.45);
  letter-spacing: 0.04em;
  line-height: 1;
}

.bw-sep {
  font-family: 'JetBrains Mono', monospace;
  font-size: 0.95rem;
  color: rgba(197, 165, 114, 0.55);
  letter-spacing: 0.1em;
}

.bw-score {
  display: flex;
  align-items: baseline;
  gap: 3px;
  padding: 4px 10px;
  border-radius: 8px;
  background: rgba(10, 14, 26, 0.4);
  border: 1px solid rgba(232, 234, 246, 0.08);
}

.bw-score-num {
  font-size: 1.4rem;
  font-weight: 700;
  color: var(--score-color, #c5a572);
  line-height: 1;
}

.bw-score-max {
  font-family: 'Manrope', sans-serif;
  font-size: 10px;
  letter-spacing: 0.12em;
  color: rgba(232, 234, 246, 0.45);
}

.bw-message {
  font-size: 12.5px;
  line-height: 1.55;
  color: rgba(232, 234, 246, 0.78);
  padding: 0 4px;
}

.bw-reasons {
  display: flex;
  flex-direction: column;
  gap: 6px;
  padding-top: 6px;
  border-top: 1px solid rgba(232, 234, 246, 0.06);
}

.bw-reasons-label {
  font-size: 9px;
  letter-spacing: 0.28em;
  text-transform: uppercase;
  color: rgba(197, 165, 114, 0.7);
  margin-bottom: 2px;
}

.reasons-list {
  list-style: none;
  padding: 0;
  margin: 0;
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.reason-item {
  display: flex;
  align-items: flex-start;
  gap: 8px;
}

.reason-check {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 14px;
  height: 14px;
  border-radius: 50%;
  background: rgba(74, 222, 128, 0.15);
  color: #4ade80;
  font-size: 9px;
  font-weight: 700;
  flex-shrink: 0;
  margin-top: 2px;
}

.reason-text {
  font-size: 12.5px;
  line-height: 1.5;
  color: rgba(232, 234, 246, 0.82);
}

.spinner {
  width: 18px;
  height: 18px;
  border: 1.5px solid rgba(197, 165, 114, 0.22);
  border-top-color: #c5a572;
  border-radius: 50%;
  animation: spin 0.85s linear infinite;
}

@keyframes spin {
  to {
    transform: rotate(360deg);
  }
}
</style>
