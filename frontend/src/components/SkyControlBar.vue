<script setup lang="ts">
import { ref, watch, computed, onMounted } from 'vue'
import { useI18n } from 'vue-i18n'
import { useFavoritesStore } from '../stores/favorites'

interface Props {
  date: string
  hour: number
  lat: number
  lng: number
}
const props = defineProps<Props>()

const emit = defineEmits<{
  'update:date': [value: string]
  'update:hour': [value: number]
  'locate': []
  'select-location': [lat: number, lng: number]
}>()

const { t } = useI18n()

// ───────────────────── 时间滑块(防抖 200ms)─────────────────────
// 本地 hour 值,实时响应滑块拖动(显示用)
const localHour = ref(props.hour)
// 防抖 timer
let debounceTimer: ReturnType<typeof setTimeout> | null = null

// 监听 props.hour 变化(外部修改时同步本地)
watch(() => props.hour, (newVal) => {
  localHour.value = newVal
})

// 把 0-23 的小时数格式化为 HH:00(用 localHour 实时更新显示)
const hourLabel = computed(() => {
  const h = Math.max(0, Math.min(23, Math.round(localHour.value)))
  return `${String(h).padStart(2, '0')}:00`
})

// 坐标显示(保留 4 位小数)
const coordLabel = computed(() => {
  return `${props.lat.toFixed(4)}, ${props.lng.toFixed(4)}`
})

function onDateInput(e: Event) {
  const target = e.target as HTMLInputElement
  if (target.value) {
    emit('update:date', target.value)
  }
}

// 滑块拖动时:更新本地值,防抖 emit(200ms)
function onHourInput(e: Event) {
  const val = Number((e.target as HTMLInputElement).value)
  localHour.value = val
  if (debounceTimer) clearTimeout(debounceTimer)
  debounceTimer = setTimeout(() => {
    emit('update:hour', val)
  }, 200)
}

function onLocate() {
  emit('locate')
}

// ───────────────────── 收藏点下拉 ─────────────────────
const favoritesStore = useFavoritesStore()
const showFavoritesDropdown = ref(false)

onMounted(() => {
  if (favoritesStore.list.length === 0) {
    favoritesStore.fetch().catch(() => {})
  }
})

function toggleFavoritesDropdown() {
  showFavoritesDropdown.value = !showFavoritesDropdown.value
}

function closeFavoritesDropdown() {
  showFavoritesDropdown.value = false
}

function selectFavorite(fav: { name: string; latitude: number; longitude: number }) {
  emit('select-location', fav.latitude, fav.longitude)
  showFavoritesDropdown.value = false
}
</script>

<template>
  <div class="sky-control-bar glass-panel">
    <!-- 日期选择器 -->
    <div class="control-group">
      <label class="control-label">{{ t('skyAtlas.date') }}</label>
      <input
        type="date"
        :value="date"
        class="control-input date-input"
        @input="onDateInput"
      />
    </div>

    <!-- 时间滑块 -->
    <div class="control-group control-group--grow">
      <label class="control-label">
        {{ t('skyAtlas.hour') }}
        <span class="hour-value">{{ hourLabel }}</span>
      </label>
      <input
        type="range"
        min="0"
        max="23"
        step="1"
        :value="localHour"
        class="hour-slider"
        @input="onHourInput"
      />
    </div>

    <!-- 位置显示 + 收藏点下拉 -->
    <div class="control-group">
      <label class="control-label">{{ t('skyAtlas.location') }}</label>
      <div class="location-row">
        <span class="coord-display">{{ coordLabel }}</span>
        <div class="favorites-dropdown">
          <button
            type="button"
            class="favorites-btn"
            :class="{ 'favorites-btn--active': showFavoritesDropdown }"
            aria-label="收藏点"
            @click="toggleFavoritesDropdown"
          >
            <svg width="12" height="12" viewBox="0 0 12 12" fill="none"
              stroke="currentColor" stroke-width="1.2" stroke-linecap="round" stroke-linejoin="round">
              <path d="M6 1 L7.5 4.5 L11 5 L8.5 7.5 L9 11 L6 9 L3 11 L3.5 7.5 L1 5 L4.5 4.5 Z" />
            </svg>
          </button>
          <Transition name="dropdown">
            <div v-if="showFavoritesDropdown" class="favorites-list-wrapper">
              <div class="favorites-backdrop" @click="closeFavoritesDropdown" />
              <ul class="favorites-list">
                <li v-if="favoritesStore.list.length === 0" class="favorites-empty">
                  暂无收藏点
                </li>
                <li v-for="fav in favoritesStore.list" :key="fav.id">
                  <button
                    type="button"
                    class="favorites-item"
                    @click="selectFavorite(fav)"
                  >
                    <span class="favorites-item-name">{{ fav.name }}</span>
                    <span class="favorites-item-coord">
                      {{ fav.latitude.toFixed(2) }}, {{ fav.longitude.toFixed(2) }}
                    </span>
                  </button>
                </li>
              </ul>
            </div>
          </Transition>
        </div>
      </div>
    </div>

    <!-- 定位按钮 -->
    <button type="button" class="locate-btn" @click="onLocate">
      <svg width="12" height="12" viewBox="0 0 12 12" fill="none"
        stroke="currentColor" stroke-width="1.2" stroke-linecap="round" stroke-linejoin="round">
        <circle cx="6" cy="6" r="2.2" />
        <path d="M6 1 V3 M6 9 V11 M1 6 H3 M9 6 H11" />
      </svg>
      {{ t('skyAtlas.locate') }}
    </button>
  </div>
</template>

<style scoped>
.sky-control-bar {
  display: flex;
  align-items: center;
  gap: 20px;
  padding: 12px 18px;
  border-radius: 10px;
  border: 1px solid rgba(197, 165, 114, 0.22);
  background: rgba(10, 14, 26, 0.55);
  backdrop-filter: blur(12px);
  -webkit-backdrop-filter: blur(12px);
}

.control-group {
  display: flex;
  flex-direction: column;
  gap: 4px;
}
.control-group--grow {
  flex: 1;
  min-width: 160px;
}

.control-label {
  font-family: 'JetBrains Mono', monospace;
  font-size: 10px;
  letter-spacing: 0.18em;
  text-transform: uppercase;
  color: rgba(197, 165, 114, 0.75);
  display: inline-flex;
  align-items: center;
  gap: 8px;
}

.hour-value {
  font-family: 'JetBrains Mono', monospace;
  color: #e8eaf6;
  font-size: 11px;
  letter-spacing: 0.05em;
}

.control-input {
  font-family: 'JetBrains Mono', monospace;
  font-size: 12px;
  color: #e8eaf6;
  background: rgba(10, 14, 26, 0.6);
  border: 1px solid rgba(197, 165, 114, 0.35);
  border-radius: 6px;
  padding: 5px 8px;
  outline: none;
  transition: border-color 0.25s ease, background 0.25s ease;
}
.control-input:focus {
  border-color: rgba(197, 165, 114, 0.75);
  background: rgba(26, 31, 58, 0.7);
}
.date-input {
  color-scheme: dark;
}

.coord-display {
  font-family: 'JetBrains Mono', monospace;
  font-size: 12px;
  color: rgba(232, 234, 246, 0.85);
  letter-spacing: 0.05em;
  padding: 5px 8px;
  border: 1px solid rgba(232, 234, 246, 0.12);
  border-radius: 6px;
  background: rgba(10, 14, 26, 0.4);
}

/* 位置行:坐标 + 收藏按钮 */
.location-row {
  display: flex;
  align-items: center;
  gap: 6px;
}

/* 收藏点下拉 */
.favorites-dropdown {
  position: relative;
}
.favorites-btn {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 28px;
  height: 28px;
  color: rgba(197, 165, 114, 0.7);
  background: rgba(197, 165, 114, 0.06);
  border: 1px solid rgba(197, 165, 114, 0.3);
  border-radius: 6px;
  cursor: pointer;
  transition: all 0.25s ease;
}
.favorites-btn:hover,
.favorites-btn--active {
  color: #c5a572;
  border-color: rgba(197, 165, 114, 0.7);
  background: rgba(197, 165, 114, 0.14);
}

.favorites-list-wrapper {
  position: relative;
}
.favorites-backdrop {
  position: fixed;
  inset: 0;
  z-index: 90;
}
.favorites-list {
  position: absolute;
  top: calc(100% + 6px);
  right: 0;
  z-index: 100;
  min-width: 220px;
  max-height: 280px;
  overflow-y: auto;
  list-style: none;
  margin: 0;
  padding: 6px;
  background: rgba(10, 14, 26, 0.92);
  border: 1px solid rgba(197, 165, 114, 0.35);
  border-radius: 8px;
  backdrop-filter: blur(12px);
  -webkit-backdrop-filter: blur(12px);
  box-shadow: 0 12px 32px rgba(0, 0, 0, 0.5);
}
.favorites-item {
  display: flex;
  flex-direction: column;
  gap: 2px;
  width: 100%;
  text-align: left;
  padding: 8px 10px;
  background: none;
  border: none;
  border-radius: 4px;
  cursor: pointer;
  transition: background 0.2s ease;
}
.favorites-item:hover {
  background: rgba(197, 165, 114, 0.12);
}
.favorites-item-name {
  font-family: 'Manrope', sans-serif;
  font-size: 12px;
  color: #e8eaf6;
}
.favorites-item-coord {
  font-family: 'JetBrains Mono', monospace;
  font-size: 10px;
  color: rgba(197, 165, 114, 0.6);
  letter-spacing: 0.05em;
}
.favorites-empty {
  padding: 12px 10px;
  font-family: 'Manrope', sans-serif;
  font-size: 11px;
  color: rgba(159, 168, 218, 0.5);
  text-align: center;
}

/* 下拉动画 */
.dropdown-enter-active,
.dropdown-leave-active {
  transition: opacity 0.2s ease, transform 0.2s ease;
}
.dropdown-enter-from,
.dropdown-leave-to {
  opacity: 0;
  transform: translateY(-4px);
}

/* 时间滑块 */
.hour-slider {
  -webkit-appearance: none;
  appearance: none;
  width: 100%;
  height: 2px;
  background: rgba(197, 165, 114, 0.3);
  border-radius: 1px;
  outline: none;
  cursor: pointer;
}
.hour-slider::-webkit-slider-thumb {
  -webkit-appearance: none;
  appearance: none;
  width: 12px;
  height: 12px;
  border-radius: 50%;
  background: #c5a572;
  border: none;
  box-shadow: 0 0 8px rgba(197, 165, 114, 0.6);
  transition: transform 0.2s ease;
}
.hour-slider::-webkit-slider-thumb:hover {
  transform: scale(1.2);
}
.hour-slider::-moz-range-thumb {
  width: 12px;
  height: 12px;
  border-radius: 50%;
  background: #c5a572;
  border: none;
  box-shadow: 0 0 8px rgba(197, 165, 114, 0.6);
  cursor: pointer;
}

.locate-btn {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  font-family: 'Manrope', sans-serif;
  font-size: 11px;
  letter-spacing: 0.18em;
  text-transform: uppercase;
  color: #c5a572;
  background: rgba(197, 165, 114, 0.08);
  border: 1px solid rgba(197, 165, 114, 0.4);
  border-radius: 6px;
  padding: 8px 14px;
  cursor: pointer;
  transition: all 0.25s ease;
}
.locate-btn:hover {
  color: #e8eaf6;
  border-color: rgba(232, 234, 246, 0.6);
  background: rgba(197, 165, 114, 0.18);
}

@media (max-width: 768px) {
  .sky-control-bar {
    flex-wrap: wrap;
    gap: 12px;
    padding: 10px 12px;
  }
  .control-group--grow {
    flex: 1 1 100%;
  }
}
</style>
