<script setup lang="ts">
import { onMounted, onUnmounted } from 'vue'
import { useI18n } from 'vue-i18n'

const { t } = useI18n()

defineProps<{ open: boolean }>()
const emit = defineEmits<{ (e: 'close'): void }>()

const levels: number[] = [1, 2, 3, 4, 5, 6, 7, 8, 9]

// 每级 Bortle 的肉眼极限星等范围
const magLimits: Record<number, string> = {
  1: '7.6–8.0',
  2: '7.1–7.5',
  3: '6.6–7.0',
  4: '6.1–6.5',
  5: '5.6–6.0',
  6: '5.1–5.5',
  7: '4.6–5.0',
  8: '4.1–4.5',
  9: '≤ 4.0',
}

// 等级颜色:1-2 暗金(极佳)/3-4 月光蓝 /5-6 黄 /7-9 红
function levelColor(level: number): string {
  if (level <= 2) return '#c5a572'
  if (level <= 4) return '#9fa8da'
  if (level <= 6) return '#f59e0b'
  return '#dc4646'
}

function onKey(e: KeyboardEvent) {
  if (e.key === 'Escape') emit('close')
}

onMounted(() => {
  window.addEventListener('keydown', onKey)
})
onUnmounted(() => {
  window.removeEventListener('keydown', onKey)
})
</script>

<template>
  <Teleport to="body">
    <Transition name="bim">
      <div
        v-if="open"
        class="bim-overlay"
        @click.self="emit('close')"
      >
        <div class="bim-panel glass-panel moon-glow" role="dialog" aria-modal="true">
          <button
            class="bim-close"
            @click="emit('close')"
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

          <header class="bim-header">
            <p class="font-mono bim-tag">BORTLE SCALE</p>
            <h2 class="font-display bim-title starlight-text">
              {{ t('bortleInfo.title') }}
            </h2>
          </header>

          <div class="bim-table">
            <div class="bim-row bim-row--head">
              <span class="font-mono bim-cell bim-cell--level">{{ t('bortleInfo.level') }}</span>
              <span class="font-mono bim-cell bim-cell--desc">{{ t('bortleInfo.description') }}</span>
              <span class="font-mono bim-cell bim-cell--mag">{{ t('bortleInfo.magLimit') }}</span>
              <span class="font-mono bim-cell bim-cell--env">{{ t('bortleInfo.environment') }}</span>
            </div>
            <div
              v-for="lvl in levels"
              :key="lvl"
              class="bim-row"
              :style="{ '--lvl-color': levelColor(lvl) }"
            >
              <span class="bim-cell bim-cell--level">
                <span class="lvl-badge font-mono">{{ lvl }}</span>
              </span>
              <span class="bim-cell bim-cell--desc font-body">
                {{ t(`bortleInfo.levels.${lvl}.desc`) }}
              </span>
              <span class="bim-cell bim-cell--mag font-mono">
                {{ magLimits[lvl] }}
              </span>
              <span class="bim-cell bim-cell--env font-body">
                {{ t(`bortleInfo.levels.${lvl}.env`) }}
              </span>
            </div>
          </div>
        </div>
      </div>
    </Transition>
  </Teleport>
</template>

<style scoped>
.bim-overlay {
  position: fixed;
  inset: 0;
  z-index: 2000;
  background: rgba(5, 8, 17, 0.82);
  backdrop-filter: blur(14px);
  -webkit-backdrop-filter: blur(14px);
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 32px 24px;
}

.bim-panel {
  position: relative;
  width: 100%;
  max-width: 720px;
  max-height: 88vh;
  overflow-y: auto;
  padding: 36px 32px 40px;
  border-radius: 14px;
  border: 1px solid rgba(232, 234, 246, 0.1);
  box-shadow: 0 28px 90px rgba(0, 0, 0, 0.65),
    0 0 0 1px rgba(197, 165, 114, 0.25);
}

.bim-close {
  position: absolute;
  top: 16px;
  right: 16px;
  z-index: 5;
  width: 32px;
  height: 32px;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: 50%;
  background: rgba(10, 14, 26, 0.6);
  color: #9fa8da;
  border: 1px solid rgba(232, 234, 246, 0.12);
  cursor: pointer;
  transition: all 0.25s ease;
}

.bim-close:hover {
  color: #c5a572;
  border-color: rgba(197, 165, 114, 0.5);
  background: rgba(197, 165, 114, 0.12);
  transform: rotate(90deg);
}

.bim-header {
  text-align: center;
  margin-bottom: 24px;
}

.bim-tag {
  font-size: 10px;
  letter-spacing: 0.32em;
  text-transform: uppercase;
  color: rgba(197, 165, 114, 0.85);
  margin-bottom: 8px;
}

.bim-title {
  font-size: 1.8rem;
  color: #e8eaf6;
  line-height: 1.15;
  font-weight: 500;
}

.bim-table {
  display: flex;
  flex-direction: column;
  gap: 2px;
}

.bim-row {
  display: grid;
  grid-template-columns: 56px 1.4fr 1fr 1.2fr;
  align-items: center;
  gap: 12px;
  padding: 10px 12px;
  border-radius: 6px;
  background: rgba(10, 14, 26, 0.4);
  border: 1px solid rgba(232, 234, 246, 0.04);
  border-left: 2px solid var(--lvl-color, rgba(197, 165, 114, 0.5));
}

.bim-row--head {
  background: transparent;
  border: none;
  border-bottom: 1px solid rgba(232, 234, 246, 0.1);
  border-radius: 0;
  padding: 8px 12px;
}

.bim-row--head .bim-cell {
  font-size: 9px;
  letter-spacing: 0.24em;
  text-transform: uppercase;
  color: rgba(197, 165, 114, 0.75);
}

.bim-cell {
  font-size: 12px;
  color: rgba(232, 234, 246, 0.82);
  line-height: 1.4;
}

.bim-cell--level {
  display: flex;
  align-items: center;
  justify-content: center;
}

.bim-cell--mag {
  font-size: 11px;
  color: rgba(159, 168, 218, 0.85);
  letter-spacing: 0.04em;
}

.lvl-badge {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 26px;
  height: 26px;
  border-radius: 50%;
  background: rgba(10, 14, 26, 0.6);
  border: 1px solid var(--lvl-color, rgba(197, 165, 114, 0.5));
  color: var(--lvl-color, #c5a572);
  font-size: 13px;
  font-weight: 700;
  text-shadow: 0 0 8px currentColor;
}

.bim-enter-active,
.bim-leave-active {
  transition: opacity 0.3s ease;
}

.bim-enter-active .bim-panel,
.bim-leave-active .bim-panel {
  transition: transform 0.4s cubic-bezier(0.4, 0, 0.2, 1),
    opacity 0.3s ease;
}

.bim-enter-from,
.bim-leave-to {
  opacity: 0;
}

.bim-enter-from .bim-panel,
.bim-leave-to .bim-panel {
  transform: translateY(20px) scale(0.96);
  opacity: 0;
}

@media (max-width: 768px) {
  .bim-panel {
    padding: 28px 18px 32px;
  }

  .bim-row {
    grid-template-columns: 40px 1fr 1fr;
    gap: 8px;
    padding: 8px 10px;
  }

  .bim-cell--env {
    grid-column: 1 / -1;
    padding-top: 4px;
    border-top: 1px dashed rgba(232, 234, 246, 0.06);
  }

  .bim-title {
    font-size: 1.4rem;
  }
}
</style>
