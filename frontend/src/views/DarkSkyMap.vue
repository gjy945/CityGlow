<script setup lang="ts">
import { ref, computed, onMounted, nextTick } from 'vue'
import { useI18n } from 'vue-i18n'
import DarkSkyLeaflet, { type MapMarker } from '../components/DarkSkyLeaflet.vue'
import ForecastPanel from './ForecastPanel.vue'
import { useEventsStore } from '../stores/events'
import { useApodStore } from '../stores/apod'

const { t } = useI18n()

const eventsStore = useEventsStore()
const apodStore = useApodStore()

const mapRef = ref<InstanceType<typeof DarkSkyLeaflet> | null>(null)
const lastClick = ref<{ lat: number; lng: number } | null>(null)
const forecastCoords = ref<{ lat: number; lng: number } | null>(null)
const forecastOpen = ref(false)
const apodModalOpen = ref(false)
const locating = ref(false)

// 中国境内暗夜观测地(为天文事件提供地图锚点,事件实体本身无坐标)
const darkSkyLocations: { lat: number; lng: number; name: string }[] = [
  { lat: 32.5, lng: 80.0, name: '西藏阿里' },
  { lat: 38.6, lng: 93.3, name: '青海冷湖' },
  { lat: 38.3, lng: 75.0, name: '新疆慕士塔格' },
  { lat: 47.2, lng: 119.9, name: '内蒙古阿尔山' },
  { lat: 28.4, lng: 100.3, name: '四川稻城亚丁' },
  { lat: 27.8, lng: 99.7, name: '云南香格里拉' },
  { lat: 38.9, lng: 100.1, name: '甘肃张掖' },
  { lat: 53.5, lng: 122.3, name: '黑龙江漠河' },
  { lat: 30.7, lng: 90.6, name: '西藏纳木错' },
  { lat: 36.7, lng: 99.1, name: '青海茶卡盐湖' },
  { lat: 41.9, lng: 101.0, name: '内蒙古额济纳旗' },
  { lat: 37.5, lng: 105.0, name: '宁夏沙坡头' },
  { lat: 42.1, lng: 117.7, name: '河北承德围场' },
]

const markers = computed<MapMarker[]>(() => {
  const list = eventsStore.list
  if (list.length === 0) return []
  return list.map((event, idx) => {
    const loc = darkSkyLocations[idx % darkSkyLocations.length]
    return {
      lat: loc.lat,
      lng: loc.lng,
      title: `${event.title} · ${loc.name}`,
      type: event.eventType,
    }
  })
})

const apod = computed(() => apodStore.data)
const apodLoading = computed(() => apodStore.loading)
const eventsLoading = computed(() => eventsStore.loading)

// 判断是否为MP4视频
const isApodVideo = computed(() => {
  if (!apod.value) return false
  const mediaUrl = apod.value.url || ''
  const mediaType = apod.value.media_type
  return mediaType === 'video' || mediaUrl.endsWith('.mp4')
})

function handleClick({ lat, lng }: { lat: number; lng: number }) {
  lastClick.value = { lat, lng }
  forecastCoords.value = { lat, lng }
  forecastOpen.value = true
  // 面板滑入后通知地图重算尺寸(Leaflet 在容器变化时需要)
  nextTick(() => {
    setTimeout(() => mapRef.value?.invalidateSize(), 320)
  })
}

function handleReady() {
  // 地图就绪
}

function handleMarkerClick(payload: { lat: number; lng: number; title: string }) {
  lastClick.value = { lat: payload.lat, lng: payload.lng }
  forecastCoords.value = { lat: payload.lat, lng: payload.lng }
  forecastOpen.value = true
  nextTick(() => {
    setTimeout(() => mapRef.value?.invalidateSize(), 320)
  })
}

function closeForecast() {
  forecastOpen.value = false
  nextTick(() => {
    setTimeout(() => mapRef.value?.invalidateSize(), 320)
  })
}

function locate() {
  if (!navigator.geolocation) {
    return
  }
  locating.value = true
  navigator.geolocation.getCurrentPosition(
      (pos) => {
        const { latitude, longitude } = pos.coords
        mapRef.value?.flyTo(latitude, longitude, 11)
        locating.value = false
      },
      () => {
        // 定位失败:回退北京
        mapRef.value?.flyTo(39.9, 116.4, 11)
        locating.value = false
      },
      { enableHighAccuracy: true, timeout: 10000 },
  )
}

onMounted(() => {
  eventsStore.fetchList()
  apodStore.fetchApod()
})
</script>

<template>
  <section class="relative h-[calc(100vh-64px)] w-full overflow-hidden">
    <DarkSkyLeaflet
        ref="mapRef"
        :markers="markers"
        class="map-fade-in"
        @map-click="handleClick"
        @ready="handleReady"
        @marker-click="handleMarkerClick"
    />

    <!-- 左上:定位按钮 -->
    <button
        class="map-btn map-btn--locate glass-panel fade-in-delay-1"
        :class="{ 'is-busy': locating }"
        :disabled="locating"
        @click="locate"
        :aria-label="t('map.locate')"
        :title="t('map.locate')"
    >
      <svg
          v-if="!locating"
          width="18"
          height="18"
          viewBox="0 0 24 24"
          fill="none"
          stroke="currentColor"
          stroke-width="1.6"
          stroke-linecap="round"
          stroke-linejoin="round"
      >
        <circle cx="12" cy="12" r="3.2" />
        <path d="M12 2v3M12 19v3M2 12h3M19 12h3" />
      </svg>
      <div v-else class="spinner" />
    </button>

    <!-- 右上:APOD 缩略图（区分图片/MP4视频） -->
    <button
        v-if="apod"
        class="map-btn map-btn--apod glass-panel"
        :class="{ 'is-image': apod.media_type === 'image' }"
        @click="apodModalOpen = true"
        :title="apod.title"
        :aria-label="t('map.viewApod')"
    >
      <!-- 图片类型缩略图 -->
      <img
          v-if="apod.media_type === 'image'"
          :src="apod.url"
          :alt="apod.title"
          class="apod-thumb-img"
      />
      <!-- MP4视频占位缩略图（播放图标） -->
      <div v-else-if="isApodVideo" class="apod-thumb-video">
        <svg width="24" height="24" viewBox="0 0 24 24" fill="#c5a572">
          <path d="M8 5v14l11-7z" />
        </svg>
      </div>
      <!-- 其他媒体兜底占位 -->
      <div v-else class="apod-thumb-placeholder">
        <span class="font-display text-lg text-dark-gold">APOD</span>
      </div>

      <div class="apod-thumb-overlay">
        <p class="font-mono text-[9px] uppercase tracking-[0.2em] text-dark-gold">
          NASA · APOD
        </p>
        <p class="font-display text-xs text-starlight truncate">{{ apod.title }}</p>
      </div>
    </button>
    <div
        v-else-if="apodLoading"
        class="map-btn map-btn--apod map-btn--loading glass-panel"
    >
      <div class="spinner" />
    </div>

    <!-- 左下:最近点击坐标 -->
    <div
        v-if="lastClick"
        class="map-info map-info--coords glass-panel"
    >
      <p class="font-mono text-[9px] uppercase tracking-[0.25em] text-dark-gold/80">
        {{ t('map.lastClick') }}
      </p>
      <p class="font-mono text-xs text-moonlight/85 mt-1">
        lat {{ lastClick.lat.toFixed(4) }} · lng {{ lastClick.lng.toFixed(4) }}
      </p>
    </div>

    <!-- 右下:章节标识 -->
    <div class="map-info map-info--section glass-panel fade-in-delay-2">
      <p class="font-mono text-[9px] uppercase tracking-[0.25em] text-dark-gold/80">
        {{ t('map.section') }}
      </p>
      <p class="font-display text-base text-starlight mt-1">{{ t('map.title') }}</p>
      <p class="font-body text-[10px] text-moonlight/50 mt-1.5 max-w-[200px] leading-snug">
        {{ t('map.subtitle') }}
      </p>
    </div>

    <!-- 事件加载指示器(右下角章节标识附近) -->
    <div
        v-if="eventsLoading"
        class="absolute bottom-6 left-1/2 -translate-x-1/2 z-[1000] glass-panel rounded-full px-4 py-2 flex items-center gap-2"
    >
      <div class="spinner spinner--sm" />
      <span class="font-mono text-[10px] text-moonlight/70 tracking-wider">
        {{ t('map.loadingEvents') }}
      </span>
    </div>

    <!-- 右侧滑出 ForecastPanel -->
    <Transition name="slide">
      <ForecastPanel
          v-if="forecastOpen && forecastCoords"
          :lat="forecastCoords.lat"
          :lng="forecastCoords.lng"
          :embedded="true"
          class="forecast-slide"
          @close="closeForecast"
      />
    </Transition>

    <!-- APOD 全屏 Modal（图片 + MP4视频双适配） -->
    <Teleport to="body">
      <Transition name="fade">
        <div
            v-if="apodModalOpen && apod"
            class="apod-modal-overlay"
            @click.self="apodModalOpen = false"
        >
          <div class="apod-modal glass-panel">
            <button
                class="apod-modal-close"
                @click="apodModalOpen = false"
                :aria-label="t('common.close')"
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
            <div class="apod-modal-media">
              <!-- 图片媒体 -->
              <img
                  v-if="apod.media_type === 'image'"
                  :src="apod.hdurl || apod.url"
                  :alt="apod.title"
              />
              <!-- MP4视频媒体：原生video标签，带控制栏、静音预加载 -->
              <video
                  v-else-if="isApodVideo"
                  class="apod-modal-video"
                  :src="apod.url"
                  autoplay
                  muted
                  controls
                  preload="auto"
              />
              <!-- 其他外链视频兜底iframe（油管等） -->
              <iframe
                  v-else
                  :src="apod.url"
                  class="apod-modal-iframe"
                  frameborder="0"
                  allowfullscreen
                  allow="autoplay; fullscreen"
              />
            </div>
            <div class="apod-modal-info">
              <p class="font-mono text-[10px] uppercase tracking-[0.3em] text-dark-gold mb-2">
                NASA · Astronomy Picture of the Day
              </p>
              <h2 class="font-display text-3xl text-starlight starlight-text mb-3 leading-tight">
                {{ apod.title }}
              </h2>
              <p class="font-body text-sm text-moonlight/80 leading-relaxed">
                {{ apod.explanation }}
              </p>
              <p v-if="apod.copyright" class="font-mono text-[10px] text-moonlight/40 mt-5">
                © {{ apod.copyright }} · {{ apod.date }}
              </p>
            </div>
          </div>
        </div>
      </Transition>
    </Teleport>
  </section>
</template>

<style scoped>
/* 地图容器淡入 */
.map-fade-in {
  animation: mapFadeIn 1s ease-out both;
}
@keyframes mapFadeIn {
  from { opacity: 0; }
  to { opacity: 1; }
}
@media (prefers-reduced-motion: reduce) {
  .map-fade-in { animation: none; }
}

/* 通用按钮 */
.map-btn {
  position: absolute;
  z-index: 1000;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #c5a572;
  cursor: pointer;
  transition: all 0.28s cubic-bezier(0.4, 0, 0.2, 1);
  border-radius: 12px;
}
.map-btn:hover {
  color: #e8eaf6;
}
.map-btn:disabled {
  cursor: wait;
  opacity: 0.7;
}
.map-btn.is-busy {
  opacity: 0.7;
}

.map-btn--locate {
  top: 90px;
  left: 3px;
  width: 48px;
  height: 48px;
  border-radius: 50%;
  box-shadow: 0 4px 16px rgba(0, 0, 0, 0.35);
}
.map-btn--locate:hover {
  transform: scale(1.06);
  box-shadow: 0 6px 22px rgba(197, 165, 114, 0.25);
}

.map-btn--apod {
  top: 20px;
  right: 20px;
  width: 64px;
  height: 64px;
  overflow: hidden;
  padding: 0;
  border-radius: 10px;
  box-shadow: 0 4px 16px rgba(0, 0, 0, 0.35);
}
.map-btn--apod:hover {
  transform: scale(1.08);
  box-shadow: 0 6px 26px rgba(197, 165, 114, 0.3);
}
.map-btn--loading {
  pointer-events: none;
}

.apod-thumb-img {
  width: 100%;
  height: 100%;
  object-fit: cover;
  display: block;
}
/* MP4视频缩略图占位 */
.apod-thumb-video {
  width: 100%;
  height: 100%;
  display: flex;
  align-items: center;
  justify-content: center;
  background: linear-gradient(135deg, #1a1f3a, #0a0e1a);
}
.apod-thumb-placeholder {
  width: 100%;
  height: 100%;
  display: flex;
  align-items: center;
  justify-content: center;
  background: linear-gradient(135deg, #1a1f3a, #0a0e1a);
}
.apod-thumb-overlay {
  position: absolute;
  inset: 0;
  display: flex;
  flex-direction: column;
  justify-content: flex-end;
  padding: 6px;
  background: linear-gradient(
      to top,
      rgba(10, 14, 26, 0.95) 0%,
      transparent 65%
  );
  opacity: 0;
  transition: opacity 0.28s ease;
}
.map-btn--apod:hover .apod-thumb-overlay {
  opacity: 1;
}

/* 信息卡片 */
.map-info {
  position: absolute;
  z-index: 1000;
  border-radius: 8px;
  padding: 10px 14px;
  pointer-events: none;
  box-shadow: 0 4px 16px rgba(0, 0, 0, 0.3);
}
.map-info--coords {
  bottom: 24px;
  left: 24px;
}
.map-info--section {
  bottom: 24px;
  right: 24px;
  max-width: 240px;
}

/* ForecastPanel 滑出容器 */
.forecast-slide {
  position: absolute;
  top: 16px;
  right: 16px;
  bottom: 16px;
  width: 380px;
  max-width: calc(100vw - 32px);
  z-index: 1100;
}
.slide-enter-active,
.slide-leave-active {
  transition: transform 0.42s cubic-bezier(0.4, 0, 0.2, 1),
  opacity 0.3s ease;
}
.slide-enter-from,
.slide-leave-to {
  transform: translateX(calc(100% + 24px));
  opacity: 0;
}

/* APOD Modal */
.apod-modal-overlay {
  position: fixed;
  inset: 0;
  z-index: 2000;
  background: rgba(10, 14, 26, 0.86);
  backdrop-filter: blur(10px);
  -webkit-backdrop-filter: blur(10px);
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 32px;
}
.apod-modal {
  position: relative;
  width: 100%;
  max-width: 1100px;
  max-height: 90vh;
  overflow: hidden;
  display: grid;
  grid-template-columns: 1.4fr 1fr;
  border-radius: 14px;
  box-shadow: 0 24px 80px rgba(0, 0, 0, 0.6);
}
.apod-modal-close {
  position: absolute;
  top: 14px;
  right: 14px;
  z-index: 10;
  width: 34px;
  height: 34px;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: 50%;
  background: rgba(10, 14, 26, 0.75);
  color: #e8eaf6;
  border: 1px solid rgba(232, 234, 246, 0.18);
  cursor: pointer;
  transition: all 0.2s ease;
}
.apod-modal-close:hover {
  color: #c5a572;
  border-color: rgba(197, 165, 114, 0.6);
  background: rgba(197, 165, 114, 0.18);
  transform: rotate(90deg);
}
.apod-modal-media {
  background: #050811;
  display: flex;
  align-items: center;
  justify-content: center;
  overflow: hidden;
  max-height: 90vh;
}
.apod-modal-media img {
  width: 100%;
  height: 100%;
  object-fit: contain;
  display: block;
}
/* MP4原生视频样式 */
.apod-modal-video {
  width: 100%;
  height: 100%;
  max-height: 90vh;
  object-fit: contain;
  background: #000;
}
/* 油管等第三方iframe视频 */
.apod-modal-iframe {
  width: 100%;
  height: 100%;
  min-height: 320px;
}
.apod-modal-info {
  padding: 32px;
  overflow-y: auto;
  max-height: 90vh;
}

.fade-enter-active,
.fade-leave-active {
  transition: opacity 0.3s ease;
}
.fade-enter-from,
.fade-leave-to {
  opacity: 0;
}

/* Spinner */
.spinner {
  width: 18px;
  height: 18px;
  border: 1.5px solid rgba(197, 165, 114, 0.22);
  border-top-color: #c5a572;
  border-radius: 50%;
  animation: spin 0.85s linear infinite;
}
.spinner--sm {
  width: 12px;
  height: 12px;
  border-width: 1.2px;
}
@keyframes spin {
  to { transform: rotate(360deg); }
}

@media (max-width: 768px) {
  .map-btn--locate {
    top: 90px;
    left: 6px;
    width: 42px;
    height: 42px;
  }
  .map-btn--apod {
    top: 12px;
    right: 12px;
    width: 52px;
    height: 52px;
  }
  .map-info--coords {
    bottom: 16px;
    left: 16px;
  }
  .map-info--section {
    display: none;
  }
  .forecast-slide {
    top: 8px;
    right: 8px;
    bottom: 8px;
    left: 8px;
    width: auto;
    max-width: none;
  }
  .apod-modal {
    grid-template-columns: 1fr;
    max-height: 95vh;
  }
  .apod-modal-media {
    max-height: 45vh;
  }
  .apod-modal-video {
    max-height: 45vh;
  }
  .apod-modal-info {
    padding: 20px;
  }
}
</style>