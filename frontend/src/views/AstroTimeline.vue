<script setup lang="ts">
import { ref, computed, onMounted, onUnmounted, nextTick } from 'vue'
import { useEventsStore } from '../stores/events'
import type { AstroEvent } from '../api/events'

const eventsStore = useEventsStore()

const now = ref(Date.now())
let timer: number | undefined

onMounted(() => {
  eventsStore.fetchList()
  timer = window.setInterval(() => {
    now.value = Date.now()
  }, 1000)
})

onUnmounted(() => {
  if (timer) window.clearInterval(timer)
})

const sortedEvents = computed(() =>
  [...eventsStore.list].sort(
    (a, b) => new Date(a.eventTime).getTime() - new Date(b.eventTime).getTime(),
  ),
)

const nextEvent = computed(() => {
  const t = now.value
  return (
    sortedEvents.value.find((e) => new Date(e.eventTime).getTime() > t) ?? null
  )
})

interface CountdownParts {
  days: number
  hours: number
  minutes: number
  seconds: number
}

const countdown = computed<CountdownParts | null>(() => {
  if (!nextEvent.value) return null
  const diff = new Date(nextEvent.value.eventTime).getTime() - now.value
  if (diff <= 0) return { days: 0, hours: 0, minutes: 0, seconds: 0 }
  return {
    days: Math.floor(diff / 86400000),
    hours: Math.floor((diff % 86400000) / 3600000),
    minutes: Math.floor((diff % 3600000) / 60000),
    seconds: Math.floor((diff % 60000) / 1000),
  }
})

interface TypeMeta {
  label: string
  color: string
  glow: string
  tip: string
}

const typeMeta: Record<string, TypeMeta> = {
  METEOR: {
    label: '流星雨',
    color: '#c5a572',
    glow: 'rgba(197, 165, 114, 0.55)',
    tip: '后半夜辐射点升至最高时观测最佳。远离城市光污染,让双眼适应黑暗约 20 分钟,无需望远镜即可肉眼追随。',
  },
  ECLIPSE: {
    label: '日月食',
    color: '#9fa8da',
    glow: 'rgba(159, 168, 218, 0.55)',
    tip: '日食须佩戴专业巴德膜滤光镜直视,严禁裸眼;月食肉眼全程可见,望远镜下可观察月面铜红色调变化。',
  },
  PLANET: {
    label: '行星动态',
    color: '#e8eaf6',
    glow: 'rgba(232, 234, 246, 0.55)',
    tip: '冲日前后行星最亮且整夜可见,中小型望远镜可分辨行星盘面、土星环系或木星伽利略卫星。',
  },
}

function typeOf(t: string): TypeMeta {
  return typeMeta[t] ?? typeMeta.PLANET
}

function typeClass(t: string): string {
  return `type-${(t || 'planet').toLowerCase()}`
}

function formatDate(iso: string): string {
  const d = new Date(iso)
  if (Number.isNaN(d.getTime())) return '--'
  return `${d.getMonth() + 1}月${d.getDate()}日`
}

function formatMonth(iso: string): string {
  const d = new Date(iso)
  if (Number.isNaN(d.getTime())) return ''
  return `${d.getFullYear()} · ${String(d.getMonth() + 1).padStart(2, '0')}`
}

function formatFull(iso: string): string {
  const d = new Date(iso)
  if (Number.isNaN(d.getTime())) return '--'
  const pad = (n: number) => String(n).padStart(2, '0')
  return `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())} ${pad(
    d.getHours(),
  )}:${pad(d.getMinutes())}`
}

function pad2(n: number): string {
  return String(n).padStart(2, '0')
}

function isNext(event: AstroEvent): boolean {
  return nextEvent.value?.id === event.id
}

// Modal
const selected = ref<AstroEvent | null>(null)
const modalOpen = ref(false)

function openDetail(event: AstroEvent) {
  selected.value = event
  modalOpen.value = true
}

function closeDetail() {
  modalOpen.value = false
}

function onKey(e: KeyboardEvent) {
  if (e.key === 'Escape' && modalOpen.value) closeDetail()
}

onMounted(() => {
  window.addEventListener('keydown', onKey)
})
onUnmounted(() => {
  window.removeEventListener('keydown', onKey)
})

// Horizontal scroll by wheel (vertical wheel → horizontal)
const scrollEl = ref<HTMLElement | null>(null)
function onWheel(e: WheelEvent) {
  const el = scrollEl.value
  if (!el) return
  if (Math.abs(e.deltaY) > Math.abs(e.deltaX)) {
    el.scrollLeft += e.deltaY
    e.preventDefault()
  }
}

// Scroll to next event on mount
onMounted(() => {
  nextTick(() => {
    setTimeout(() => {
      const el = scrollEl.value
      if (!el) return
      const target = el.querySelector<HTMLElement>('.timeline-item--next')
      if (target) {
        el.scrollTo({
          left: target.offsetLeft - el.clientWidth / 2 + target.offsetWidth / 2,
          behavior: 'smooth',
        })
      }
    }, 400)
  })
})
</script>

<template>
  <section class="timeline-page">
    <!-- 星图等高线背景装饰 -->
    <svg class="contour-deco" viewBox="0 0 1200 600" preserveAspectRatio="xMidYMid slice" aria-hidden="true">
      <defs>
        <radialGradient id="contourFade" cx="50%" cy="50%" r="50%">
          <stop offset="0%" stop-color="rgba(159,168,218,0.10)" />
          <stop offset="100%" stop-color="rgba(159,168,218,0)" />
        </radialGradient>
      </defs>
      <g fill="none" stroke="rgba(159,168,218,0.08)" stroke-width="0.8">
        <ellipse cx="200" cy="120" rx="180" ry="70" />
        <ellipse cx="200" cy="120" rx="240" ry="100" />
        <ellipse cx="200" cy="120" rx="300" ry="135" />
        <ellipse cx="200" cy="120" rx="360" ry="170" />
        <ellipse cx="980" cy="480" rx="160" ry="60" />
        <ellipse cx="980" cy="480" rx="220" ry="90" />
        <ellipse cx="980" cy="480" rx="290" ry="125" />
      </g>
      <g fill="rgba(232,234,246,0.4)">
        <circle cx="120" cy="80" r="0.9" />
        <circle cx="340" cy="200" r="1.1" />
        <circle cx="620" cy="60" r="0.8" />
        <circle cx="880" cy="160" r="1" />
        <circle cx="1080" cy="320" r="0.9" />
        <circle cx="160" cy="440" r="1" />
        <circle cx="520" cy="520" r="0.8" />
        <circle cx="760" cy="380" r="1.1" />
        <circle cx="1040" cy="540" r="0.9" />
      </g>
    </svg>

    <div class="timeline-inner">
      <!-- 标题区 -->
      <header class="timeline-header">
        <p class="section-tag">Section 03 · Cosmic Chronicle</p>
        <h1 class="timeline-title font-display starlight-text">天文事件时间轴</h1>
        <p class="timeline-sub">2026 · 暗夜守护者天文志</p>
      </header>

      <!-- 倒计时区 -->
      <div class="countdown-block glass-panel">
        <div class="countdown-left">
          <p class="cd-label">距下一事件</p>
          <p v-if="nextEvent" class="cd-event-title font-display">
            {{ nextEvent.title }}
          </p>
          <p v-else class="cd-event-title font-display cd-event-empty">
            本年度星历已尽
          </p>
          <p v-if="nextEvent" class="cd-event-date font-mono">
            {{ formatFull(nextEvent.eventTime) }} · {{ typeOf(nextEvent.eventType).label }}
          </p>
        </div>
        <div class="countdown-right">
          <template v-if="countdown">
            <div class="cd-segment cd-segment--days">
              <span class="cd-num font-mono">{{ countdown.days }}</span>
              <span class="cd-unit">天</span>
            </div>
            <span class="cd-colon">·</span>
            <div class="cd-segment">
              <span class="cd-num font-mono">{{ pad2(countdown.hours) }}</span>
            </div>
            <span class="cd-colon">:</span>
            <div class="cd-segment">
              <span class="cd-num font-mono">{{ pad2(countdown.minutes) }}</span>
            </div>
            <span class="cd-colon">:</span>
            <div class="cd-segment">
              <span class="cd-num font-mono">{{ pad2(countdown.seconds) }}</span>
            </div>
          </template>
          <p v-else class="cd-num font-mono cd-num--end">—— · ——</p>
        </div>
      </div>

      <!-- 时间轴 -->
      <div
        v-if="sortedEvents.length"
        ref="scrollEl"
        class="timeline-scroll"
        @wheel="onWheel"
      >
        <div class="timeline-track">
          <div class="timeline-line" aria-hidden="true">
            <span class="line-cap line-cap--left"></span>
            <span class="line-cap line-cap--right"></span>
          </div>
          <article
            v-for="event in sortedEvents"
            :key="event.id"
            class="timeline-item"
            :class="[typeClass(event.eventType), { 'timeline-item--next': isNext(event) }]"
            @click="openDetail(event)"
          >
            <div class="timeline-node">
              <span class="node-pulse"></span>
              <span class="node-dot"></span>
            </div>
            <div class="timeline-card glass-panel">
              <div class="card-top">
                <span class="card-icon" :style="{ color: typeOf(event.eventType).color }">
                  <!-- METEOR: 流星轨迹 -->
                  <svg v-if="event.eventType === 'METEOR'" viewBox="0 0 24 24" width="18" height="18" fill="none" stroke="currentColor" stroke-width="1.4" stroke-linecap="round">
                    <path d="M3 21 L13 11" />
                    <path d="M14 10 L16 8" stroke-width="1.6" />
                    <circle cx="17" cy="7" r="1.3" fill="currentColor" stroke="none" />
                    <circle cx="19.5" cy="4.5" r="0.8" fill="currentColor" stroke="none" />
                    <circle cx="21" cy="3" r="0.5" fill="currentColor" stroke="none" />
                  </svg>
                  <!-- ECLIPSE: 月相圆 -->
                  <svg v-else-if="event.eventType === 'ECLIPSE'" viewBox="0 0 24 24" width="18" height="18">
                    <circle cx="12" cy="12" r="8.5" fill="none" stroke="currentColor" stroke-width="1.4" />
                    <path d="M12 3.5 A8.5 8.5 0 0 1 12 20.5 Z" fill="currentColor" opacity="0.55" />
                  </svg>
                  <!-- PLANET: 行星环 -->
                  <svg v-else viewBox="0 0 24 24" width="18" height="18" fill="none" stroke="currentColor" stroke-width="1.4">
                    <circle cx="12" cy="12" r="4.6" />
                    <ellipse cx="12" cy="12" rx="10" ry="3.6" transform="rotate(-22 12 12)" />
                  </svg>
                </span>
                <span class="card-type" :style="{ color: typeOf(event.eventType).color }">
                  {{ typeOf(event.eventType).label }}
                </span>
                <span v-if="isNext(event)" class="card-next-tag font-mono">NEXT</span>
              </div>
              <h3 class="card-title font-display">{{ event.title }}</h3>
              <div class="card-foot">
                <span class="card-date font-mono">{{ formatDate(event.eventTime) }}</span>
                <span class="card-month font-mono">{{ formatMonth(event.eventTime) }}</span>
              </div>
            </div>
          </article>
        </div>
      </div>

      <!-- 加载态 -->
      <div v-else-if="eventsStore.loading" class="timeline-state">
        <div class="spinner"></div>
        <p class="font-mono text-xs text-moonlight/60 mt-4 tracking-wider">
          校准星历中…
        </p>
      </div>

      <!-- 空态/错误 -->
      <div v-else class="timeline-state">
        <p class="font-display text-2xl text-starlight/70">星历暂缺</p>
        <p class="font-mono text-[10px] text-moonlight/50 mt-3">
          {{ eventsStore.error || '未能加载天文事件' }}
        </p>
      </div>

      <!-- 滚动提示 -->
      <p v-if="sortedEvents.length" class="scroll-hint font-mono">
        ← 横向滚动浏览全年星历 →
      </p>
    </div>

    <!-- 详情 Modal -->
    <Teleport to="body">
      <Transition name="modal">
        <div
          v-if="modalOpen && selected"
          class="modal-overlay"
          @click.self="closeDetail"
        >
          <div
            class="modal-panel glass-panel moon-glow"
            :class="typeClass(selected.eventType)"
            :style="{ '--accent': typeOf(selected.eventType).color, '--accent-glow': typeOf(selected.eventType).glow }"
          >
            <button class="modal-close" @click="closeDetail" aria-label="关闭">
              <svg width="14" height="14" viewBox="0 0 14 14" fill="none">
                <path d="M2 2 L12 12 M12 2 L2 12" stroke="currentColor" stroke-width="1.5" stroke-linecap="round" />
              </svg>
            </button>

            <div class="modal-icon" :style="{ color: typeOf(selected.eventType).color }">
              <svg v-if="selected.eventType === 'METEOR'" viewBox="0 0 48 48" width="40" height="40" fill="none" stroke="currentColor" stroke-width="1.2" stroke-linecap="round">
                <path d="M6 42 L26 22" />
                <path d="M28 20 L32 16" stroke-width="1.4" />
                <circle cx="34" cy="14" r="2.2" fill="currentColor" stroke="none" />
                <circle cx="39" cy="9" r="1.4" fill="currentColor" stroke="none" />
                <circle cx="42" cy="6" r="0.9" fill="currentColor" stroke="none" />
              </svg>
              <svg v-else-if="selected.eventType === 'ECLIPSE'" viewBox="0 0 48 48" width="40" height="40">
                <circle cx="24" cy="24" r="17" fill="none" stroke="currentColor" stroke-width="1.2" />
                <path d="M24 7 A17 17 0 0 1 24 41 Z" fill="currentColor" opacity="0.55" />
              </svg>
              <svg v-else viewBox="0 0 48 48" width="40" height="40" fill="none" stroke="currentColor" stroke-width="1.2">
                <circle cx="24" cy="24" r="9" />
                <ellipse cx="24" cy="24" rx="20" ry="7.2" transform="rotate(-22 24 24)" />
              </svg>
            </div>

            <p class="modal-type font-mono" :style="{ color: typeOf(selected.eventType).color }">
              {{ typeOf(selected.eventType).label }}
            </p>

            <h2 class="modal-title font-display starlight-text">
              {{ selected.title }}
            </h2>

            <p class="modal-time font-mono">
              {{ formatFull(selected.eventTime) }}
            </p>

            <div class="modal-divider"></div>

            <div class="modal-section">
              <p class="modal-section-label font-mono">事件详注</p>
              <p class="modal-desc">{{ selected.description }}</p>
            </div>

            <div class="modal-section">
              <p class="modal-section-label font-mono">观测建议</p>
              <p class="modal-tip">{{ typeOf(selected.eventType).tip }}</p>
            </div>
          </div>
        </div>
      </Transition>
    </Teleport>
  </section>
</template>

<style scoped>
.timeline-page {
  position: relative;
  min-height: calc(100vh - 64px);
  padding: 56px 0 72px;
  overflow: hidden;
}

.contour-deco {
  position: absolute;
  inset: 0;
  width: 100%;
  height: 100%;
  pointer-events: none;
  z-index: 0;
}

.timeline-inner {
  position: relative;
  z-index: 1;
  max-width: 1280px;
  margin: 0 auto;
  padding: 0 32px;
}

/* 标题区 */
.timeline-header {
  text-align: center;
  margin-bottom: 44px;
}
.section-tag {
  font-family: 'JetBrains Mono', monospace;
  font-size: 11px;
  letter-spacing: 0.32em;
  text-transform: uppercase;
  color: rgba(197, 165, 114, 0.85);
  margin-bottom: 14px;
}
.timeline-title {
  font-size: clamp(2.6rem, 5vw, 3.8rem);
  color: #e8eaf6;
  line-height: 1.05;
  letter-spacing: 0.02em;
  font-weight: 500;
}
.timeline-sub {
  font-family: 'JetBrains Mono', monospace;
  font-size: 12px;
  letter-spacing: 0.22em;
  color: rgba(159, 168, 218, 0.6);
  margin-top: 12px;
  text-transform: uppercase;
}

/* 倒计时区 */
.countdown-block {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 24px;
  padding: 26px 32px;
  border-radius: 12px;
  margin-bottom: 48px;
  flex-wrap: wrap;
}
.countdown-left {
  flex: 1 1 280px;
  min-width: 240px;
}
.cd-label {
  font-family: 'JetBrains Mono', monospace;
  font-size: 10px;
  letter-spacing: 0.3em;
  text-transform: uppercase;
  color: rgba(197, 165, 114, 0.75);
  margin-bottom: 8px;
}
.cd-event-title {
  font-size: 1.6rem;
  color: #e8eaf6;
  line-height: 1.2;
  font-weight: 500;
}
.cd-event-empty {
  color: rgba(232, 234, 246, 0.5);
}
.cd-event-date {
  font-size: 11px;
  letter-spacing: 0.12em;
  color: rgba(159, 168, 218, 0.7);
  margin-top: 6px;
}

.countdown-right {
  display: flex;
  align-items: flex-end;
  gap: 4px;
  flex-wrap: nowrap;
}
.cd-segment {
  display: flex;
  flex-direction: column;
  align-items: center;
  min-width: 38px;
}
.cd-segment--days {
  min-width: 60px;
}
.cd-num {
  font-size: clamp(1.8rem, 3.4vw, 2.6rem);
  font-weight: 700;
  color: #c5a572;
  text-shadow: 0 0 14px rgba(197, 165, 114, 0.45);
  line-height: 1;
  font-variant-numeric: tabular-nums;
}
.cd-num--end {
  font-size: 2rem;
  color: rgba(159, 168, 218, 0.4);
  text-shadow: none;
}
.cd-segment--days .cd-num {
  font-size: clamp(2.4rem, 4.6vw, 3.4rem);
}
.cd-unit {
  font-family: 'Manrope', sans-serif;
  font-size: 10px;
  letter-spacing: 0.2em;
  color: rgba(197, 165, 114, 0.6);
  margin-top: 4px;
}
.cd-colon {
  font-family: 'JetBrains Mono', monospace;
  font-size: 1.8rem;
  color: rgba(197, 165, 114, 0.4);
  padding-bottom: 4px;
}

/* 时间轴滚动 */
.timeline-scroll {
  overflow-x: auto;
  overflow-y: visible;
  padding: 8px 0 24px;
  cursor: grab;
  scrollbar-width: thin;
}
.timeline-track {
  position: relative;
  display: flex;
  gap: 24px;
  align-items: flex-start;
  padding: 56px 24px 28px;
  min-width: max-content;
}

/* 时间轴线 */
.timeline-line {
  position: absolute;
  top: 63px;
  left: 24px;
  right: 24px;
  height: 1px;
  background: linear-gradient(
    90deg,
    transparent 0%,
    rgba(197, 165, 114, 0.15) 4%,
    rgba(197, 165, 114, 0.55) 50%,
    rgba(197, 165, 114, 0.15) 96%,
    transparent 100%
  );
}
.line-cap {
  position: absolute;
  top: 50%;
  width: 7px;
  height: 7px;
  border-radius: 50%;
  background: rgba(197, 165, 114, 0.5);
  transform: translateY(-50%);
}
.line-cap--left {
  left: -3px;
}
.line-cap--right {
  right: -3px;
  background: transparent;
  border: 1px solid rgba(197, 165, 114, 0.6);
}

/* 事件列 */
.timeline-item {
  position: relative;
  flex: 0 0 244px;
  display: flex;
  flex-direction: column;
  align-items: center;
  cursor: pointer;
  --type-color: #9fa8da;
  --type-glow: rgba(159, 168, 218, 0.5);
}
.timeline-item.type-meteor {
  --type-color: #c5a572;
  --type-glow: rgba(197, 165, 114, 0.5);
}
.timeline-item.type-eclipse {
  --type-color: #9fa8da;
  --type-glow: rgba(159, 168, 218, 0.5);
}
.timeline-item.type-planet {
  --type-color: #e8eaf6;
  --type-glow: rgba(232, 234, 246, 0.45);
}

.timeline-node {
  position: relative;
  width: 14px;
  height: 14px;
  margin-bottom: 22px;
  z-index: 2;
}
.node-dot {
  position: absolute;
  inset: 0;
  border-radius: 50%;
  background: var(--type-color);
  box-shadow: 0 0 10px var(--type-glow);
  transition: transform 0.3s ease, box-shadow 0.3s ease;
}
.node-pulse {
  position: absolute;
  inset: -4px;
  border-radius: 50%;
  border: 1px solid var(--type-color);
  opacity: 0;
  transition: opacity 0.3s ease;
}
.timeline-item--next .node-pulse {
  opacity: 0.6;
  animation: nodePulse 2.4s ease-out infinite;
}
@keyframes nodePulse {
  0% { transform: scale(0.8); opacity: 0.6; }
  100% { transform: scale(2.4); opacity: 0; }
}
.timeline-item:hover .node-dot {
  transform: scale(1.25);
  box-shadow: 0 0 16px var(--type-color);
}

/* 事件卡片 */
.timeline-card {
  width: 100%;
  padding: 18px 18px 16px;
  border-radius: 10px;
  border: 1px solid rgba(232, 234, 246, 0.08);
  transition: transform 0.32s cubic-bezier(0.4, 0, 0.2, 1),
    border-color 0.32s ease, box-shadow 0.32s ease;
  position: relative;
  overflow: hidden;
}
.timeline-card::before {
  content: '';
  position: absolute;
  inset: 0;
  border-radius: 10px;
  padding: 1px;
  background: linear-gradient(
    160deg,
    var(--type-glow) 0%,
    transparent 40%,
    transparent 60%,
    var(--type-glow) 100%
  );
  -webkit-mask: linear-gradient(#000 0 0) content-box, linear-gradient(#000 0 0);
  -webkit-mask-composite: xor;
  mask-composite: exclude;
  opacity: 0;
  transition: opacity 0.32s ease;
  pointer-events: none;
}
.timeline-item:hover .timeline-card {
  transform: translateY(-6px);
  border-color: rgba(232, 234, 246, 0.16);
  box-shadow: 0 12px 36px rgba(0, 0, 0, 0.4),
    0 0 0 1px var(--type-glow);
}
.timeline-item:hover .timeline-card::before {
  opacity: 1;
}
.timeline-item--next .timeline-card {
  border-color: var(--type-glow);
  box-shadow: 0 0 0 1px var(--type-glow), 0 8px 28px rgba(0, 0, 0, 0.35);
}

.card-top {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 14px;
}
.card-icon {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  line-height: 0;
}
.card-type {
  font-family: 'Manrope', sans-serif;
  font-size: 11px;
  letter-spacing: 0.15em;
  text-transform: uppercase;
  font-weight: 500;
}
.card-next-tag {
  margin-left: auto;
  font-size: 9px;
  letter-spacing: 0.2em;
  color: #c5a572;
  border: 1px solid rgba(197, 165, 114, 0.5);
  padding: 2px 6px;
  border-radius: 3px;
  background: rgba(197, 165, 114, 0.08);
}
.card-title {
  font-size: 1.18rem;
  color: #e8eaf6;
  line-height: 1.25;
  font-weight: 500;
  min-height: 2.5em;
  margin-bottom: 12px;
}
.card-foot {
  display: flex;
  align-items: baseline;
  justify-content: space-between;
  padding-top: 10px;
  border-top: 1px solid rgba(232, 234, 246, 0.06);
}
.card-date {
  font-size: 14px;
  color: #e8eaf6;
  font-weight: 500;
}
.card-month {
  font-size: 9px;
  letter-spacing: 0.18em;
  color: rgba(159, 168, 218, 0.5);
  text-transform: uppercase;
}

/* 滚动提示 */
.scroll-hint {
  text-align: center;
  font-size: 10px;
  letter-spacing: 0.28em;
  color: rgba(159, 168, 218, 0.35);
  margin-top: 18px;
  text-transform: uppercase;
}

/* 状态态 */
.timeline-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  text-align: center;
  padding: 80px 16px;
}
.spinner {
  width: 24px;
  height: 24px;
  border: 1.5px solid rgba(197, 165, 114, 0.18);
  border-top-color: #c5a572;
  border-radius: 50%;
  animation: spin 0.85s linear infinite;
}
@keyframes spin {
  to { transform: rotate(360deg); }
}

/* Modal */
.modal-overlay {
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
.modal-panel {
  position: relative;
  width: 100%;
  max-width: 560px;
  max-height: 88vh;
  overflow-y: auto;
  padding: 40px 38px 44px;
  border-radius: 14px;
  border: 1px solid rgba(232, 234, 246, 0.1);
  box-shadow: 0 28px 90px rgba(0, 0, 0, 0.65),
    0 0 0 1px var(--accent-glow, rgba(197, 165, 114, 0.3));
}
.modal-panel::after {
  content: '';
  position: absolute;
  inset: 0;
  border-radius: 14px;
  pointer-events: none;
  background: radial-gradient(
    ellipse at top,
    var(--accent-glow, rgba(197, 165, 114, 0.18)) 0%,
    transparent 55%
  );
  opacity: 0.5;
}
.modal-close {
  position: absolute;
  top: 16px;
  right: 16px;
  z-index: 5;
  width: 34px;
  height: 34px;
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
.modal-close:hover {
  color: var(--accent, #c5a572);
  border-color: var(--accent-glow, rgba(197, 165, 114, 0.5));
  transform: rotate(90deg);
}
.modal-icon {
  margin-bottom: 18px;
  line-height: 0;
  position: relative;
  z-index: 1;
}
.modal-type {
  font-size: 11px;
  letter-spacing: 0.32em;
  text-transform: uppercase;
  margin-bottom: 10px;
  position: relative;
  z-index: 1;
}
.modal-title {
  font-size: 2.1rem;
  color: #e8eaf6;
  line-height: 1.15;
  font-weight: 500;
  letter-spacing: 0.01em;
  margin-bottom: 12px;
  position: relative;
  z-index: 1;
}
.modal-time {
  font-size: 13px;
  letter-spacing: 0.1em;
  color: rgba(159, 168, 218, 0.75);
  position: relative;
  z-index: 1;
}
.modal-divider {
  height: 1px;
  background: linear-gradient(
    90deg,
    var(--accent-glow, rgba(197, 165, 114, 0.4)),
    transparent
  );
  margin: 26px 0 24px;
  position: relative;
  z-index: 1;
}
.modal-section {
  margin-bottom: 22px;
  position: relative;
  z-index: 1;
}
.modal-section:last-child {
  margin-bottom: 0;
}
.modal-section-label {
  font-size: 10px;
  letter-spacing: 0.28em;
  text-transform: uppercase;
  color: rgba(197, 165, 114, 0.7);
  margin-bottom: 10px;
}
.modal-desc {
  font-family: 'Manrope', sans-serif;
  font-size: 14px;
  line-height: 1.75;
  color: rgba(232, 234, 246, 0.82);
}
.modal-tip {
  font-family: 'Manrope', sans-serif;
  font-size: 13.5px;
  line-height: 1.75;
  color: rgba(232, 234, 246, 0.7);
  padding: 14px 16px;
  border-left: 2px solid var(--accent, #c5a572);
  background: rgba(232, 234, 246, 0.03);
  border-radius: 0 6px 6px 0;
}

.modal-enter-active,
.modal-leave-active {
  transition: opacity 0.3s ease;
}
.modal-enter-active .modal-panel,
.modal-leave-active .modal-panel {
  transition: transform 0.4s cubic-bezier(0.4, 0, 0.2, 1),
    opacity 0.3s ease;
}
.modal-enter-from,
.modal-leave-to {
  opacity: 0;
}
.modal-enter-from .modal-panel,
.modal-leave-to .modal-panel {
  transform: translateY(20px) scale(0.96);
  opacity: 0;
}

/* 响应式 */
@media (max-width: 768px) {
  .timeline-page {
    padding: 32px 0 48px;
  }
  .timeline-inner {
    padding: 0 16px;
  }
  .countdown-block {
    padding: 20px 22px;
    gap: 16px;
  }
  .cd-event-title {
    font-size: 1.3rem;
  }
  .countdown-right {
    width: 100%;
    justify-content: center;
  }
  .timeline-track {
    gap: 18px;
    padding: 56px 16px 24px;
  }
  .timeline-item {
    flex: 0 0 220px;
  }
  .modal-panel {
    padding: 32px 24px 36px;
  }
  .modal-title {
    font-size: 1.6rem;
  }
}
</style>
