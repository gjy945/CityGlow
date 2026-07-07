<script setup lang="ts">
import { ref, watch, onMounted } from 'vue'

interface Props {
  moonPhase: string
  size?: number
}
const props = withDefaults(defineProps<Props>(), { size: 120 })

const canvas = ref<HTMLCanvasElement>()
let currentFraction = 0.5
let currentWaxing = true
let rafId: number | null = null

function phaseToFraction(phase: string): number {
  switch (phase) {
    case 'New Moon':
      return 0
    case 'Full Moon':
      return 1
    case 'First Quarter':
    case 'Last Quarter':
      return 0.5
    case 'Waxing Crescent':
    case 'Waning Crescent':
      return 0.25
    case 'Waxing Gibbous':
    case 'Waning Gibbous':
      return 0.75
    default:
      return 0.5
  }
}

function phaseIsWaxing(phase: string): boolean {
  switch (phase) {
    case 'First Quarter':
    case 'Waxing Crescent':
    case 'Waxing Gibbous':
    case 'New Moon':
    case 'Full Moon':
      return true
    case 'Last Quarter':
    case 'Waning Gibbous':
    case 'Waning Crescent':
      return false
    default:
      return true
  }
}

function setupCanvas() {
  const cvs = canvas.value
  if (!cvs) return
  const dpr = window.devicePixelRatio || 1
  cvs.width = props.size * dpr
  cvs.height = props.size * dpr
  cvs.style.width = `${props.size}px`
  cvs.style.height = `${props.size}px`
  const ctx = cvs.getContext('2d')
  if (ctx) {
    ctx.setTransform(dpr, 0, 0, dpr, 0, 0)
  }
}

function draw(fraction: number, waxing: boolean) {
  const ctx = canvas.value?.getContext('2d')
  if (!ctx) return
  const size = props.size
  ctx.clearRect(0, 0, size, size)

  const r = size / 2 - 6
  const cx = size / 2
  const cy = size / 2

  // 外层月光光晕
  const glow = ctx.createRadialGradient(cx, cy, r * 0.85, cx, cy, r * 1.7)
  glow.addColorStop(0, 'rgba(159, 168, 218, 0.32)')
  glow.addColorStop(1, 'rgba(159, 168, 218, 0)')
  ctx.fillStyle = glow
  ctx.beginPath()
  ctx.arc(cx, cy, r * 1.7, 0, Math.PI * 2)
  ctx.fill()

  // 暗面底色
  ctx.fillStyle = '#1a1f3a'
  ctx.beginPath()
  ctx.arc(cx, cy, r, 0, Math.PI * 2)
  ctx.fill()

  // 亮面区域
  const f = Math.max(0, Math.min(1, fraction))
  if (f > 0.001) {
    // 亮面渐变:左上偏亮,右下偏暗,模拟立体感
    const litGrad = ctx.createRadialGradient(
      cx - r * 0.3,
      cy - r * 0.3,
      r * 0.1,
      cx,
      cy,
      r,
    )
    litGrad.addColorStop(0, '#f4f5fb')
    litGrad.addColorStop(0.6, '#e8eaf6')
    litGrad.addColorStop(1, '#c3c8e8')
    ctx.fillStyle = litGrad
    drawLitRegion(ctx, cx, cy, r, f, waxing)
  }

  // 月面边缘细描边
  ctx.strokeStyle = 'rgba(232, 234, 246, 0.18)'
  ctx.lineWidth = 0.6
  ctx.beginPath()
  ctx.arc(cx, cy, r, 0, Math.PI * 2)
  ctx.stroke()
}

function drawLitRegion(
  ctx: CanvasRenderingContext2D,
  cx: number,
  cy: number,
  r: number,
  f: number,
  waxing: boolean,
) {
  // 椭圆水平半径:r * |1 - 2f|
  // f=0 -> r (terminator 在边缘,无亮面)
  // f=0.5 -> 0 (terminator 在中线,半弦月)
  // f=1 -> r (terminator 在对侧边缘,满月)
  const ellipseX = r * Math.abs(1 - 2 * f)
  ctx.beginPath()
  if (waxing) {
    // 亮面在右:右半圆 top -> right -> bottom
    ctx.arc(cx, cy, r, -Math.PI / 2, Math.PI / 2, false)
    if (f < 0.5) {
      // 蛾眉月:从中线向右收(右半椭圆回程,逆时针视觉)
      ctx.ellipse(cx, cy, ellipseX, r, 0, Math.PI / 2, -Math.PI / 2, true)
    } else {
      // 盈凸月:延伸到左侧(左半椭圆回程,顺时针视觉)
      ctx.ellipse(cx, cy, ellipseX, r, 0, Math.PI / 2, -Math.PI / 2, false)
    }
  } else {
    // 亮面在左:左半圆 top -> left -> bottom
    ctx.arc(cx, cy, r, -Math.PI / 2, Math.PI / 2, true)
    if (f < 0.5) {
      // 残月:左半椭圆回程,顺时针视觉
      ctx.ellipse(cx, cy, ellipseX, r, 0, Math.PI / 2, -Math.PI / 2, false)
    } else {
      // 亏凸月:右半椭圆回程,逆时针视觉
      ctx.ellipse(cx, cy, ellipseX, r, 0, Math.PI / 2, -Math.PI / 2, true)
    }
  }
  ctx.closePath()
  ctx.fill()
}

function animateTo(targetFraction: number, targetWaxing: boolean) {
  if (rafId != null) {
    cancelAnimationFrame(rafId)
    rafId = null
  }
  const startFraction = currentFraction
  const startTime = performance.now()
  const duration = 450
  // 盈亏方向离散切换(实际月相变更罕见,瞬间切换可接受)
  currentWaxing = targetWaxing

  function step(now: number) {
    const t = Math.min(1, (now - startTime) / duration)
    // ease-in-out cubic
    const eased = t < 0.5 ? 4 * t * t * t : 1 - Math.pow(-2 * t + 2, 3) / 2
    currentFraction = startFraction + (targetFraction - startFraction) * eased
    draw(currentFraction, currentWaxing)
    if (t < 1) {
      rafId = requestAnimationFrame(step)
    } else {
      rafId = null
    }
  }
  rafId = requestAnimationFrame(step)
}

watch(
  () => props.moonPhase,
  (next) => {
    if (next) {
      animateTo(phaseToFraction(next), phaseIsWaxing(next))
    }
  },
)

watch(
  () => props.size,
  () => {
    setupCanvas()
    draw(currentFraction, currentWaxing)
  },
)

onMounted(() => {
  setupCanvas()
  currentFraction = phaseToFraction(props.moonPhase)
  currentWaxing = phaseIsWaxing(props.moonPhase)
  draw(currentFraction, currentWaxing)
})
</script>

<template>
  <canvas ref="canvas" class="moon-phase-canvas" />
</template>

<style scoped>
.moon-phase-canvas {
  display: block;
  filter: drop-shadow(0 0 18px rgba(159, 168, 218, 0.28));
}
</style>
