<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { useI18n } from 'vue-i18n'
import { neoApi, type NeoApproach } from '../api/neo'

const { t, locale } = useI18n()

const props = withDefaults(defineProps<{ days?: number; topN?: number }>(), {
  days: 7,
  topN: 5,
})

const loading = ref(false)
const error = ref<string | null>(null)
const approaches = ref<NeoApproach[]>([])
const totalCount = ref(0)

async function load() {
  loading.value = true
  error.value = null
  try {
    const res = await neoApi.getNeoFeed(props.days)
    approaches.value = res?.approaches ?? []
    totalCount.value = res?.totalCount ?? approaches.value.length
  } catch (e) {
    error.value = (e as Error).message
  } finally {
    loading.value = false
  }
}

onMounted(() => {
  load()
})

// 前 N 个最大的(按 estimatedDiameterMaxMeters 降序)
const topApproaches = computed(() => {
  return [...approaches.value]
    .sort(
      (a, b) =>
        b.estimatedDiameterMaxMeters - a.estimatedDiameterMaxMeters,
    )
    .slice(0, props.topN)
})

function formatDate(iso: string): string {
  const d = new Date(iso)
  if (Number.isNaN(d.getTime())) return '--'
  return d.toLocaleDateString(locale.value, {
    year: 'numeric',
    month: 'short',
    day: 'numeric',
  })
}

// 大数字格式化(千分位)
function formatNumber(n: number, fractionDigits = 0): string {
  if (!Number.isFinite(n)) return '--'
  return n.toLocaleString(locale.value, {
    minimumFractionDigits: fractionDigits,
    maximumFractionDigits: fractionDigits,
  })
}

// 距离换算:1 LD ≈ 384400 km
function formatDistance(km: number): string {
  if (!Number.isFinite(km)) return '--'
  if (km < 1e6) return `${formatNumber(Math.round(km))} km`
  // 转换成 LD 显示
  const ld = km / 384400
  return `${ld.toFixed(2)} LD`
}
</script>

<template>
  <div class="neo-widget glass-panel">
    <!-- 橙色顶部光带 -->
    <div class="neo-ribbon" aria-hidden="true" />

    <header class="neo-header">
      <div class="neo-icon" aria-hidden="true">
        <svg viewBox="0 0 32 32" width="22" height="22" fill="none">
          <defs>
            <radialGradient id="neoGrad" cx="40%" cy="35%" r="65%">
              <stop offset="0%" stop-color="#fcd34d" />
              <stop offset="60%" stop-color="#f59e0b" />
              <stop offset="100%" stop-color="#b45309" />
            </radialGradient>
          </defs>
          <circle cx="14" cy="15" r="8" fill="url(#neoGrad)" />
          <circle cx="11" cy="12" r="1.5" fill="rgba(0,0,0,0.18)" />
          <circle cx="16" cy="17" r="1" fill="rgba(0,0,0,0.18)" />
          <circle cx="13" cy="18" r="0.7" fill="rgba(0,0,0,0.18)" />
          <ellipse
            cx="14"
            cy="15"
            rx="14"
            ry="4"
            transform="rotate(-22 14 15)"
            fill="none"
            stroke="rgba(245, 158, 11, 0.55)"
            stroke-width="0.8"
          />
        </svg>
      </div>
      <div class="neo-titles">
        <p class="font-mono neo-tag">{{ t('neo.title') }}</p>
        <p class="font-display neo-title">{{ t('neo.upcoming') }}</p>
      </div>
      <div class="neo-count">
        <span class="font-mono neo-count-num">{{ totalCount }}</span>
      </div>
    </header>

    <!-- 加载态 -->
    <div v-if="loading && !approaches.length" class="neo-state">
      <div class="spinner" />
      <p class="font-mono state-text">{{ t('neo.loading') }}</p>
    </div>

    <!-- 错误态 -->
    <div v-else-if="error && !approaches.length" class="neo-state">
      <p class="font-display state-title">{{ t('neo.error') }}</p>
      <p class="font-mono state-text">{{ error }}</p>
    </div>

    <!-- 数据列表 -->
    <div v-else-if="topApproaches.length" class="neo-list">
      <article
        v-for="item in topApproaches"
        :key="item.id"
        class="neo-item"
        :class="{ 'neo-item--hazardous': item.isPotentiallyHazardous }"
      >
        <div class="neo-item-head">
          <span class="neo-name font-display">{{ item.name }}</span>
          <span
            v-if="item.isPotentiallyHazardous"
            class="neo-hazard-tag"
            :title="t('neo.hazardous')"
          >
            <svg width="10" height="10" viewBox="0 0 10 10" fill="none">
              <path
                d="M5 1 L9 8 L1 8 Z"
                fill="currentColor"
                opacity="0.85"
              />
              <rect x="4.5" y="3.5" width="1" height="2.5" fill="#1a1f3a" />
              <rect x="4.5" y="6.5" width="1" height="1" fill="#1a1f3a" />
            </svg>
            {{ t('neo.hazardous') }}
          </span>
        </div>
        <dl class="neo-meta">
          <div class="meta-row">
            <dt class="font-mono">{{ t('neo.approachDate') }}</dt>
            <dd class="font-mono">{{ formatDate(item.approachDate) }}</dd>
          </div>
          <div class="meta-row">
            <dt class="font-mono">{{ t('neo.diameter') }}</dt>
            <dd class="font-mono">
              {{ formatNumber(item.estimatedDiameterMinMeters) }}–{{ formatNumber(item.estimatedDiameterMaxMeters) }} m
            </dd>
          </div>
          <div class="meta-row">
            <dt class="font-mono">{{ t('neo.distance') }}</dt>
            <dd class="font-mono">{{ formatDistance(item.missDistanceKm) }}</dd>
          </div>
          <div class="meta-row">
            <dt class="font-mono">{{ t('neo.velocity') }}</dt>
            <dd class="font-mono">{{ formatNumber(item.relativeVelocityKps, 2) }} km/s</dd>
          </div>
        </dl>
      </article>
    </div>

    <!-- 空态 -->
    <div v-else class="neo-state">
      <p class="font-display state-title">{{ t('neo.title') }}</p>
    </div>
  </div>
</template>

<style scoped>
.neo-widget {
  position: relative;
  border-radius: 12px;
  padding: 18px 18px 16px;
  overflow: hidden;
}

.neo-ribbon {
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  height: 2px;
  background: linear-gradient(
    90deg,
    transparent 0%,
    rgba(245, 158, 11, 0.6) 30%,
    rgba(252, 211, 77, 0.9) 50%,
    rgba(245, 158, 11, 0.6) 70%,
    transparent 100%
  );
}

.neo-header {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 14px;
}

.neo-icon {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 36px;
  height: 36px;
  border-radius: 50%;
  background: rgba(245, 158, 11, 0.1);
  border: 1px solid rgba(245, 158, 11, 0.32);
  flex-shrink: 0;
}

.neo-titles {
  flex: 1;
  min-width: 0;
}

.neo-tag {
  font-size: 9px;
  letter-spacing: 0.28em;
  text-transform: uppercase;
  color: rgba(245, 158, 11, 0.85);
}

.neo-title {
  font-size: 1.05rem;
  color: #e8eaf6;
  margin-top: 2px;
  line-height: 1.2;
}

.neo-count {
  flex-shrink: 0;
}

.neo-count-num {
  font-size: 1.8rem;
  font-weight: 700;
  color: #f59e0b;
  text-shadow: 0 0 12px rgba(245, 158, 11, 0.5);
  line-height: 1;
}

.neo-state {
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

.neo-list {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.neo-item {
  padding: 10px 12px;
  border-radius: 8px;
  background: rgba(10, 14, 26, 0.45);
  border: 1px solid rgba(232, 234, 246, 0.05);
  transition: border-color 0.25s ease;
}

.neo-item:hover {
  border-color: rgba(245, 158, 11, 0.25);
}

.neo-item--hazardous {
  border-color: rgba(220, 70, 70, 0.4);
  background: rgba(70, 20, 20, 0.18);
  box-shadow: inset 2px 0 0 #dc4646;
}

.neo-item-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
  margin-bottom: 6px;
}

.neo-name {
  font-size: 1rem;
  color: #e8eaf6;
  line-height: 1.2;
  font-weight: 500;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.neo-hazard-tag {
  display: inline-flex;
  align-items: center;
  gap: 3px;
  font-family: 'Manrope', sans-serif;
  font-size: 9px;
  letter-spacing: 0.12em;
  text-transform: uppercase;
  padding: 2px 6px;
  border-radius: 3px;
  background: rgba(220, 70, 70, 0.18);
  color: #f87171;
  border: 1px solid rgba(220, 70, 70, 0.4);
  flex-shrink: 0;
}

.neo-meta {
  margin: 0;
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 4px 12px;
}

.meta-row {
  display: flex;
  align-items: baseline;
  justify-content: space-between;
  gap: 8px;
  font-size: 11px;
}

.meta-row dt {
  font-size: 9px;
  letter-spacing: 0.18em;
  text-transform: uppercase;
  color: rgba(159, 168, 218, 0.6);
  font-weight: 400;
}

.meta-row dd {
  margin: 0;
  font-size: 11px;
  color: rgba(232, 234, 246, 0.85);
  text-align: right;
}

.spinner {
  width: 18px;
  height: 18px;
  border: 1.5px solid rgba(245, 158, 11, 0.22);
  border-top-color: #f59e0b;
  border-radius: 50%;
  animation: spin 0.85s linear infinite;
}

@keyframes spin {
  to {
    transform: rotate(360deg);
  }
}

@media (max-width: 480px) {
  .neo-meta {
    grid-template-columns: 1fr;
  }
}
</style>
