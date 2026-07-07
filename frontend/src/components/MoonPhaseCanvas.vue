<script setup lang="ts">
import { ref, watch, onMounted } from 'vue'

interface Props {
  // 月相循环位置:0=新月,0.25=上弦月,0.5=满月,0.75=下弦月,1=新月
  phase: number
  // Canvas 尺寸(px)
  size?: number
}
const props = withDefaults(defineProps<Props>(), { size: 80 })

const canvas = ref<HTMLCanvasElement>()
let currentFraction = 0.5
let currentWaxing = true
let rafId: number | null = null

// 将循环位置 (0-1) 转为照亮比例 (0=全暗,1=全亮) 与盈亏方向
function phaseToFraction(phase: number): number {
  const p = ((phase % 1) + 1) % 1
  if (p <= 0.5) return p * 2 // 0→0, 0.25→0.5, 0.5→1
  return (1 - p) * 2 // 0.5→1, 0.75→0.5, 1→0
}
function phaseIsWaxing(phase: number): boolean {
  const p = ((phase % 1) + 1) % 1
  // 0-0.5 为盈(右亮),0.5-1 为亏(左亮)
  return p < 0.5
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
  glow.addColorStop(0, 'rgba(245, 230, 200, 0.32)')
  glow.addColorStop(1, 'rgba(245, 230, 200, 0)')
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
    // 亮面渐变:左上偏亮,右下偏暗,模拟立体感(月光色 #f5e6c8)
    const litGrad = ctx.createRadialGradient(
      cx - r * 0.3,
      cy - r * 0.3,
      r * 0.1,
      cx,
      cy,
      r,
    )
    litGrad.addColorStop(0, '#fbf3df')
    litGrad.addColorStop(0.55, '#f5e6c8')
    litGrad.addColorStop(1, '#d8c195')
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
  () => props.phase,
  (next) => {
    if (next == null || Number.isNaN(next)) return
    animateTo(phaseToFraction(next), phaseIsWaxing(next))
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
  currentFraction = phaseToFraction(props.phase)
  currentWaxing = phaseIsWaxing(props.phase)
  draw(currentFraction, currentWaxing)
})
</script>

<template>
  <canvas ref="canvas" class="moon-phase-canvas" />
</template>

<style scoped>
.moon-phase-canvas {
  display: block;
  filter: drop-shadow(0 0 18px rgba(245, 230, 200, 0.28));
}
</style>
