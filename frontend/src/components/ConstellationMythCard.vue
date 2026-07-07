<script setup lang="ts">
import { ref, watch, computed, onMounted, onUnmounted } from 'vue'
import { skyApi, type MythCard } from '../api/sky'

interface Props {
  // 星座 name(如 "orion"),用于拉取神话
  constellation: string
  // 弹窗显隐
  visible: boolean
  // 星座拉丁名 / 中文名(可选,由父组件从 skyView 传入用于头部展示)
  latin?: string
  chinese?: string
}
const props = withDefaults(defineProps<Props>(), {
  latin: '',
  chinese: '',
})

const emit = defineEmits<{
  'close': []
}>()

// 神话数据 + 加载状态
const myths = ref<MythCard[]>([])
const loading = ref(false)
const error = ref('')

// 双栏切换模式:both = 双栏 / greek = 仅希腊 / chinese = 仅中国
type ViewMode = 'both' | 'greek' | 'chinese'
const viewMode = ref<ViewMode>('both')

// 拆分希腊 / 中国神话
const greekMyth = computed(() => myths.value.find((m) => m.culture === 'greek') ?? null)
const chineseMyth = computed(() => myths.value.find((m) => m.culture === 'chinese') ?? null)

// 头部展示名:优先用传入的 latin / chinese,缺省回退到 constellation
const latinName = computed(() => props.latin || props.constellation)
const chineseName = computed(() => props.chinese || '')

async function loadMyths() {
  if (!props.constellation) return
  loading.value = true
  error.value = ''
  myths.value = []
  try {
    const result = await skyApi.getMyths(props.constellation)
    myths.value = result
  } catch (e) {
    error.value = e instanceof Error ? e.message : String(e)
  } finally {
    loading.value = false
  }
}

// 弹窗打开 或 星座切换时重新加载(合并为单个 watcher,避免初次打开时双触发)
watch(
  [() => props.constellation, () => props.visible],
  ([name, open]) => {
    if (open && name) {
      viewMode.value = 'both'
      loadMyths()
    }
  },
)

// ESC 关闭
function onKey(e: KeyboardEvent) {
  if (e.key === 'Escape' && props.visible) emit('close')
}

onMounted(() => {
  window.addEventListener('keydown', onKey)
})
onUnmounted(() => {
  window.removeEventListener('keydown', onKey)
})

// Phase 5 实现跳转,此处仅占位
function addToPostcard() {
  console.log('[ConstellationMythCard] 加入星图明信片:', props.constellation)
}
</script>

<template>
  <Teleport to="body">
    <Transition name="myth">
      <div
        v-if="visible"
        class="myth-overlay"
        @click.self="emit('close')"
      >
        <div class="myth-panel" role="dialog" aria-modal="true">
          <!-- 关闭按钮 -->
          <button
            class="myth-close"
            :aria-label="'关闭'"
            @click="emit('close')"
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

          <!-- 头部:拉丁名 | 中文名 -->
          <header class="myth-header">
            <div class="myth-header-left">
              <p class="myth-eyebrow font-mono">CONSTELLATION</p>
              <h2 class="myth-latin font-display">{{ latinName }}</h2>
            </div>
            <div class="myth-header-right">
              <p class="myth-eyebrow font-mono">星 宿</p>
              <h2 class="myth-chinese font-display">{{ chineseName }}</h2>
            </div>
          </header>

          <!-- 装饰分隔线 -->
          <div class="myth-divider">
            <span class="myth-divider-glyph">✦</span>
          </div>

          <!-- 主体:加载 / 错误 / 神话双栏 -->
          <div class="myth-body">
            <!-- 加载中 -->
            <div v-if="loading" class="myth-state">
              <div class="myth-spinner" />
              <p class="font-mono myth-state-text">翻阅古老星卷…</p>
            </div>

            <!-- 加载失败 -->
            <div v-else-if="error" class="myth-state">
              <p class="font-display myth-state-title">星卷遗失</p>
              <p class="font-mono myth-state-text break-all">{{ error }}</p>
            </div>

            <!-- 暂无神话 -->
            <div v-else-if="!greekMyth && !chineseMyth" class="myth-state">
              <p class="font-display myth-state-title">此星卷尚空</p>
              <p class="font-mono myth-state-text">该星座暂未收录神话故事</p>
            </div>

            <!-- 双栏神话 -->
            <div
              v-else
              class="myth-grid"
              :class="{
                'myth-grid--both': viewMode === 'both',
                'myth-grid--single': viewMode !== 'both',
              }"
            >
              <!-- 希腊栏 -->
              <article
                v-if="greekMyth && viewMode !== 'chinese'"
                class="myth-col myth-col--greek"
              >
                <div class="myth-col-head">
                  <span class="myth-culture-tag font-mono">GREEK MYTH</span>
                  <span class="myth-culture-zh font-display">希腊神话</span>
                </div>
                <h3 class="myth-story-title font-display">{{ greekMyth.title }}</h3>
                <p class="myth-story font-body">{{ greekMyth.story }}</p>
              </article>

              <!-- 中间分隔线(仅双栏模式) -->
              <div v-if="viewMode === 'both' && greekMyth && chineseMyth" class="myth-col-divider" />

              <!-- 中国栏 -->
              <article
                v-if="chineseMyth && viewMode !== 'greek'"
                class="myth-col myth-col--chinese"
              >
                <div class="myth-col-head">
                  <span class="myth-culture-tag font-mono">CHINESE SKY</span>
                  <span class="myth-culture-zh font-display">中国星宿</span>
                </div>
                <h3 class="myth-story-title font-display">{{ chineseMyth.title }}</h3>
                <p class="myth-story font-body">{{ chineseMyth.story }}</p>
              </article>
            </div>
          </div>

          <!-- 底部:视图切换 + 明信片按钮 -->
          <footer class="myth-footer">
            <div class="myth-view-toggle">
              <button
                class="myth-toggle-btn font-mono"
                :class="{ 'myth-toggle-btn--active': viewMode === 'both' }"
                @click="viewMode = 'both'"
              >
                双栏
              </button>
              <button
                class="myth-toggle-btn font-mono"
                :class="{ 'myth-toggle-btn--active': viewMode === 'greek' }"
                @click="viewMode = 'greek'"
              >
                希腊
              </button>
              <button
                class="myth-toggle-btn font-mono"
                :class="{ 'myth-toggle-btn--active': viewMode === 'chinese' }"
                @click="viewMode = 'chinese'"
              >
                中国
              </button>
            </div>
            <button class="myth-postcard-btn font-mono" @click="addToPostcard">
              <span class="myth-postcard-glyph">✧</span>
              加入星图明信片
            </button>
          </footer>
        </div>
      </div>
    </Transition>
  </Teleport>
</template>

<style scoped>
/* 遮罩:深空半透明 + 模糊 */
.myth-overlay {
  position: fixed;
  inset: 0;
  z-index: 2100;
  background: rgba(5, 8, 17, 0.85);
  backdrop-filter: blur(14px);
  -webkit-backdrop-filter: blur(14px);
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 32px 24px;
}

/* 卡片:羊皮纸纹理 + 暗金边框 + 辉光 */
.myth-panel {
  position: relative;
  width: 100%;
  max-width: 880px;
  max-height: 88vh;
  display: flex;
  flex-direction: column;
  padding: 36px 40px 28px;
  border-radius: 14px;
  border: 1px solid #c5a572;
  box-shadow: 0 0 40px rgba(197, 165, 114, 0.3),
    0 28px 90px rgba(0, 0, 0, 0.7);
  /* 羊皮纸底色 + 暖色径向渐变 + 噪点纹理 */
  background-color: rgba(245, 230, 200, 0.05);
  background-image:
    radial-gradient(ellipse at top left, rgba(197, 165, 114, 0.08) 0%, transparent 55%),
    radial-gradient(ellipse at bottom right, rgba(159, 168, 218, 0.05) 0%, transparent 55%),
    url("data:image/svg+xml,%3Csvg xmlns='http://www.w3.org/2000/svg' width='220' height='220'%3E%3Cfilter id='n'%3E%3CfeTurbulence type='fractalNoise' baseFrequency='0.85' numOctaves='2'/%3E%3CfeColorMatrix values='0 0 0 0 0.77 0 0 0 0 0.65 0 0 0 0 0.45 0 0 0 0.06 0'/%3E%3C/filter%3E%3Crect width='100%25' height='100%25' filter='url(%23n)'/%3E%3C/svg%3E");
  overflow: hidden;
}

/* 关闭按钮 */
.myth-close {
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
  background: rgba(10, 14, 26, 0.5);
  color: #e8eaf6;
  border: 1px solid rgba(232, 234, 246, 0.14);
  cursor: pointer;
  transition: all 0.25s ease;
}
.myth-close:hover {
  color: #c5a572;
  border-color: rgba(197, 165, 114, 0.6);
  background: rgba(197, 165, 114, 0.14);
  transform: rotate(90deg);
}

/* 头部:左右双语名 */
.myth-header {
  display: flex;
  align-items: flex-end;
  justify-content: space-between;
  gap: 24px;
  padding-right: 40px;
}
.myth-header-right {
  text-align: right;
}
.myth-eyebrow {
  font-size: 9px;
  letter-spacing: 0.32em;
  text-transform: uppercase;
  color: rgba(197, 165, 114, 0.7);
  margin-bottom: 6px;
}
.myth-latin {
  font-size: 2.1rem;
  font-weight: 500;
  line-height: 1.1;
  color: #c5a572;
  text-shadow: 0 0 16px rgba(197, 165, 114, 0.35);
}
.myth-chinese {
  font-size: 1.6rem;
  font-weight: 500;
  line-height: 1.1;
  color: #e8eaf6;
  letter-spacing: 0.08em;
}

/* 装饰分隔线 */
.myth-divider {
  display: flex;
  align-items: center;
  justify-content: center;
  margin: 18px 0 22px;
}
.myth-divider::before,
.myth-divider::after {
  content: '';
  flex: 1;
  height: 1px;
  background: linear-gradient(
    to right,
    transparent,
    rgba(197, 165, 114, 0.5),
    transparent
  );
}
.myth-divider-glyph {
  margin: 0 14px;
  color: rgba(197, 165, 114, 0.7);
  font-size: 12px;
  text-shadow: 0 0 8px rgba(197, 165, 114, 0.5);
}

/* 主体区域 */
.myth-body {
  flex: 1;
  min-height: 0;
  overflow-y: auto;
  padding-right: 4px;
}

/* 状态:加载 / 错误 / 空 */
.myth-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  text-align: center;
  padding: 48px 16px;
  gap: 14px;
}
.myth-spinner {
  width: 26px;
  height: 26px;
  border: 1.5px solid rgba(197, 165, 114, 0.2);
  border-top-color: #c5a572;
  border-radius: 50%;
  animation: myth-spin 0.85s linear infinite;
}
@keyframes myth-spin {
  to { transform: rotate(360deg); }
}
.myth-state-title {
  font-size: 1.3rem;
  color: rgba(232, 234, 246, 0.8);
}
.myth-state-text {
  font-size: 11px;
  letter-spacing: 0.18em;
  text-transform: uppercase;
  color: rgba(159, 168, 218, 0.6);
}

/* 双栏网格 */
.myth-grid {
  display: grid;
  gap: 28px;
}
.myth-grid--both {
  grid-template-columns: 1fr 1px 1fr;
}
.myth-grid--single {
  grid-template-columns: 1fr;
}

/* 单栏 */
.myth-col {
  display: flex;
  flex-direction: column;
  gap: 12px;
}
.myth-col-head {
  display: flex;
  align-items: baseline;
  gap: 10px;
  padding-bottom: 8px;
  border-bottom: 1px dashed rgba(197, 165, 114, 0.28);
}
.myth-culture-tag {
  font-size: 9px;
  letter-spacing: 0.28em;
  text-transform: uppercase;
  color: #c5a572;
  position: relative;
}
.myth-culture-tag::after {
  content: '';
  display: inline-block;
  width: 18px;
  height: 1px;
  margin-left: 8px;
  vertical-align: middle;
  background: rgba(197, 165, 114, 0.6);
}
.myth-culture-zh {
  font-size: 0.95rem;
  color: rgba(232, 234, 246, 0.65);
  letter-spacing: 0.1em;
}
.myth-story-title {
  font-size: 1.35rem;
  font-weight: 500;
  line-height: 1.25;
  color: #e8eaf6;
  text-shadow: 0 0 10px rgba(232, 234, 246, 0.25);
}
.myth-story {
  font-size: 13.5px;
  line-height: 1.75;
  color: rgba(232, 234, 246, 0.8);
  text-align: justify;
}

/* 中间分隔线 */
.myth-col-divider {
  width: 1px;
  background: linear-gradient(
    to bottom,
    transparent,
    rgba(197, 165, 114, 0.4),
    transparent
  );
}

/* 底部 */
.myth-footer {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  margin-top: 24px;
  padding-top: 18px;
  border-top: 1px solid rgba(197, 165, 114, 0.18);
  flex-shrink: 0;
}

/* 视图切换按钮组 */
.myth-view-toggle {
  display: flex;
  gap: 4px;
  padding: 3px;
  border-radius: 999px;
  background: rgba(10, 14, 26, 0.5);
  border: 1px solid rgba(197, 165, 114, 0.18);
}
.myth-toggle-btn {
  padding: 5px 14px;
  font-size: 10px;
  letter-spacing: 0.18em;
  text-transform: uppercase;
  color: rgba(159, 168, 218, 0.7);
  background: transparent;
  border: none;
  border-radius: 999px;
  cursor: pointer;
  transition: all 0.22s ease;
}
.myth-toggle-btn:hover {
  color: #e8eaf6;
}
.myth-toggle-btn--active {
  color: #c5a572;
  background: rgba(197, 165, 114, 0.14);
  box-shadow: inset 0 0 0 1px rgba(197, 165, 114, 0.4);
}

/* 明信片按钮:暗金描边 */
.myth-postcard-btn {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  padding: 8px 18px;
  font-size: 10px;
  letter-spacing: 0.22em;
  text-transform: uppercase;
  color: #c5a572;
  background: rgba(197, 165, 114, 0.06);
  border: 1px solid rgba(197, 165, 114, 0.5);
  border-radius: 999px;
  cursor: pointer;
  transition: all 0.25s ease;
}
.myth-postcard-btn:hover {
  color: #050811;
  background: #c5a572;
  box-shadow: 0 0 20px rgba(197, 165, 114, 0.45);
}
.myth-postcard-glyph {
  font-size: 12px;
}

/* 进出动画:scale + opacity */
.myth-enter-active,
.myth-leave-active {
  transition: opacity 0.3s ease;
}
.myth-enter-active .myth-panel,
.myth-leave-active .myth-panel {
  transition: transform 0.3s cubic-bezier(0.4, 0, 0.2, 1),
    opacity 0.3s ease;
}
.myth-enter-from,
.myth-leave-to {
  opacity: 0;
}
.myth-enter-from .myth-panel,
.myth-leave-to .myth-panel {
  transform: scale(0.9);
  opacity: 0;
}

/* 响应式:窄屏单栏堆叠 */
@media (max-width: 768px) {
  .myth-panel {
    padding: 28px 20px 22px;
    max-height: 90vh;
  }
  .myth-header {
    flex-direction: column;
    align-items: flex-start;
    gap: 10px;
    padding-right: 40px;
  }
  .myth-header-right {
    text-align: left;
  }
  .myth-latin {
    font-size: 1.6rem;
  }
  .myth-chinese {
    font-size: 1.3rem;
  }
  .myth-grid--both {
    grid-template-columns: 1fr;
  }
  .myth-col-divider {
    display: none;
  }
  .myth-footer {
    flex-direction: column;
    align-items: stretch;
    gap: 12px;
  }
  .myth-view-toggle {
    justify-content: center;
  }
  .myth-postcard-btn {
    justify-content: center;
  }
}
</style>
