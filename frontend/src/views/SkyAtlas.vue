<script setup lang="ts">
import { ref, computed, watch, onMounted } from 'vue'
import { useI18n } from 'vue-i18n'
import { skyApi, type SkyViewResult } from '../api/sky'
import StarCanvas from '../components/StarCanvas.vue'
import SkyControlBar from '../components/SkyControlBar.vue'
import ConstellationMythCard from '../components/ConstellationMythCard.vue'

const { t } = useI18n()

// 默认坐标:北京
const DEFAULT_LAT = 39.9
const DEFAULT_LNG = 116.4

// 取今日 YYYY-MM-DD(本地时区)
function todayStr(): string {
  const d = new Date()
  const y = d.getFullYear()
  const m = String(d.getMonth() + 1).padStart(2, '0')
  const day = String(d.getDate()).padStart(2, '0')
  return `${y}-${m}-${day}`
}

const lat = ref<number>(DEFAULT_LAT)
const lng = ref<number>(DEFAULT_LNG)
const date = ref<string>(todayStr())
const hour = ref<number>(22)

const skyView = ref<SkyViewResult | null>(null)
const loading = ref(false)
const error = ref<string>('')

// 悬停的星座名(传给 StarCanvas 控制高亮)
const hoveredConstellation = ref<string | null>(null)
// 选中的星座名(点击后弹出神话卡)
const selectedConstellation = ref<string>('')
// 神话卡显隐
const mythCardVisible = ref(false)

// 选中星座的拉丁名 / 中文名(从 skyView 查表传给神话卡头部)
const selectedLatin = computed(() => {
  const c = skyView.value?.constellations.find((x) => x.name === selectedConstellation.value)
  return c?.latin ?? ''
})
const selectedChinese = computed(() => {
  const c = skyView.value?.constellations.find((x) => x.name === selectedConstellation.value)
  return c?.chinese ?? ''
})

// StarCanvas 悬停:更新 hoveredConstellation(回传给 StarCanvas 作高亮 prop)
function onHover(name: string | null) {
  hoveredConstellation.value = name
}

// StarCanvas 点击:设置选中星座并打开神话卡
function onSelect(name: string) {
  selectedConstellation.value = name
  mythCardVisible.value = true
}

// 关闭神话卡
function closeMythCard() {
  mythCardVisible.value = false
}

async function loadSkyView() {
  loading.value = true
  error.value = ''
  try {
    const result = await skyApi.getSkyView(lat.value, lng.value, date.value, hour.value)
    skyView.value = result
  } catch (e) {
    error.value = e instanceof Error ? e.message : String(e)
    skyView.value = null
  } finally {
    loading.value = false
  }
}

// 浏览器定位
function locateMe() {
  if (!('geolocation' in navigator)) return
  navigator.geolocation.getCurrentPosition(
    (pos) => {
      lat.value = Number(pos.coords.latitude.toFixed(4))
      lng.value = Number(pos.coords.longitude.toFixed(4))
      loadSkyView()
    },
    () => {
      // 定位失败保持默认坐标
      loadSkyView()
    },
    { enableHighAccuracy: false, timeout: 8000, maximumAge: 600000 },
  )
}

onMounted(() => {
  if ('geolocation' in navigator) {
    navigator.geolocation.getCurrentPosition(
      (pos) => {
        lat.value = Number(pos.coords.latitude.toFixed(4))
        lng.value = Number(pos.coords.longitude.toFixed(4))
        loadSkyView()
      },
      () => {
        // 定位失败用默认坐标
        loadSkyView()
      },
      { enableHighAccuracy: false, timeout: 8000, maximumAge: 600000 },
    )
  } else {
    loadSkyView()
  }
})

// 日期 / 时间变化触发重载
watch([date, hour], () => {
  loadSkyView()
})

const subtitle = computed(() => t('skyAtlas.subtitle'))
</script>

<template>
  <section class="sky-atlas-page">
    <!-- 页面标题 -->
    <header class="sky-atlas-header">
      <div class="sky-atlas-header-inner">
        <p class="font-mono text-[10px] uppercase tracking-[0.3em] text-dark-gold/80">
          Section 05 · Sky Atlas
        </p>
        <h1 class="font-display text-3xl text-dark-gold mt-1">
          {{ t('skyAtlas.title') }}
        </h1>
        <p class="font-body text-sm text-moonlight/70 mt-1">
          {{ subtitle }}
        </p>
      </div>
    </header>

    <!-- 控制条 -->
    <SkyControlBar
      :date="date"
      :hour="hour"
      :lat="lat"
      :lng="lng"
      @update:date="date = $event"
      @update:hour="hour = $event"
      @locate="locateMe"
    />

    <!-- 星图主体 -->
    <div class="sky-atlas-canvas-wrap">
      <!-- 加载中 -->
      <div v-if="loading && !skyView" class="sky-atlas-state">
        <div class="spinner" />
        <p class="font-mono text-xs text-moonlight/60 mt-4 tracking-wider">
          {{ t('skyAtlas.loading') }}
        </p>
      </div>

      <!-- 错误 -->
      <div v-else-if="error && !skyView" class="sky-atlas-state">
        <p class="font-display text-xl text-starlight/80">{{ t('skyAtlas.error') }}</p>
        <p class="font-mono text-[10px] text-moonlight/50 mt-3 break-all">
          {{ error }}
        </p>
      </div>

      <!-- Canvas -->
      <StarCanvas
        v-else
        :sky-view="skyView"
        :hovered-constellation="hoveredConstellation"
        @hover="onHover"
        @select="onSelect"
      />

      <!-- 数据加载中覆盖层(已有数据时刷新) -->
      <div v-if="loading && skyView" class="refresh-overlay">
        <div class="spinner spinner--sm" />
      </div>
    </div>

    <!-- 神话卡弹窗 -->
    <ConstellationMythCard
      :constellation="selectedConstellation"
      :visible="mythCardVisible"
      :latin="selectedLatin"
      :chinese="selectedChinese"
      @close="closeMythCard"
    />
  </section>
</template>

<style scoped>
.sky-atlas-page {
  display: flex;
  flex-direction: column;
  height: calc(100vh - 64px);
  background: #050811;
  padding: 20px 24px 24px;
  gap: 16px;
}

.sky-atlas-header {
  flex-shrink: 0;
}
.sky-atlas-header-inner {
  display: flex;
  flex-direction: column;
  gap: 2px;
}

.sky-atlas-canvas-wrap {
  position: relative;
  flex: 1;
  min-height: 0;
  border-radius: 12px;
  overflow: hidden;
  border: 1px solid rgba(197, 165, 114, 0.18);
  background: #050811;
}

.sky-atlas-state {
  position: absolute;
  inset: 0;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  text-align: center;
  padding: 24px;
}

.refresh-overlay {
  position: absolute;
  top: 14px;
  right: 14px;
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 6px 10px;
  border-radius: 999px;
  background: rgba(10, 14, 26, 0.55);
  border: 1px solid rgba(197, 165, 114, 0.28);
  backdrop-filter: blur(8px);
  -webkit-backdrop-filter: blur(8px);
}

.spinner {
  width: 22px;
  height: 22px;
  border: 1.5px solid rgba(197, 165, 114, 0.18);
  border-top-color: #c5a572;
  border-radius: 50%;
  animation: spin 0.85s linear infinite;
}
.spinner--sm {
  width: 14px;
  height: 14px;
  border-width: 1.2px;
}
@keyframes spin {
  to { transform: rotate(360deg); }
}

@media (max-width: 768px) {
  .sky-atlas-page {
    padding: 12px 12px 16px;
    gap: 12px;
  }
}
</style>
