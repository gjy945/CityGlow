<script setup lang="ts">
import { computed } from 'vue'
import { useI18n } from 'vue-i18n'

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
}>()

const { t } = useI18n()

// 把 0-23 的小时数格式化为 HH:00
const hourLabel = computed(() => {
  const h = Math.max(0, Math.min(23, Math.round(props.hour)))
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

function onHourInput(e: Event) {
  const target = e.target as HTMLInputElement
  emit('update:hour', Number(target.value))
}

function onLocate() {
  emit('locate')
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
        :value="hour"
        class="hour-slider"
        @input="onHourInput"
      />
    </div>

    <!-- 位置显示 -->
    <div class="control-group">
      <label class="control-label">{{ t('skyAtlas.location') }}</label>
      <span class="coord-display">{{ coordLabel }}</span>
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
