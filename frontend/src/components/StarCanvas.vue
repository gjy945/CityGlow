<script setup lang="ts">
import { ref, watch, onMounted, onUnmounted, nextTick } from 'vue'
import type { SkyViewResult, StarPoint, ConstellationView } from '../api/sky'

interface Props {
  skyView: SkyViewResult | null
  hoveredConstellation?: string | null
}
const props = withDefaults(defineProps<Props>(), {
  hoveredConstellation: null,
})

// 鼠标交互:悬停高亮 + 点击选中
const emit = defineEmits<{
  'hover': [value: string | null]
  'select': [value: string]
}>()

const canvas = ref<HTMLCanvasElement>()
const containerRef = ref<HTMLDivElement>()

// 光标状态:悬停在星座上时显示 pointer
const cursorPointer = ref(false)
// 记录上次悬停的星座,避免 mousemove 高频重复 emit
let lastHovered: string | null = null

// 数据更新时的整体淡入动画触发(通过 class toggle 重启 CSS animation)
const fadeActive = ref(false)

// Canvas 显示尺寸(CSS 像素),由 ResizeObserver 更新
let cssWidth = 0
let cssHeight = 0
let resizeObserver: ResizeObserver | null = null

// 颜色常量(遵循项目视觉规范)
const COLOR_BG = '#050811' // 比项目主背景 #0a0e1a 更深的星图背景
const COLOR_STAR = '#fef3c7' // 暖白星点
const COLOR_LINE = '#c5a572' // 暗金连线
const COLOR_HORIZON = 'rgba(197, 165, 114, 0.3)' // 地平线环暗金 30%
const COLOR_LABEL = 'rgba(197, 165, 114, 0.5)' // 星座标签暗金 50%
const COLOR_DIR = 'rgba(197, 165, 114, 0.85)' // 方位标暗金

// 把方位角(0=北,90=东)与高度角(0=地平,90=天顶)投影到 Canvas 坐标
function project(az: number, alt: number, cx: number, cy: number, maxR: number) {
  // 半径:越靠近天顶(alt 大)越靠近中心
  const r = ((90 - alt) / 90) * maxR
  const azRad = (az * Math.PI) / 180
  // 0=北=上方,90=东=右方,180=南=下方,270=西=左方
  const x = cx + r * Math.sin(azRad)
  const y = cy - r * Math.cos(azRad)
  return { x, y }
}

// 计算星座中心(所有 stars 平均投影位置)
function constellationCenter(
  stars: StarPoint[],
  cx: number,
  cy: number,
  maxR: number,
) {
  if (stars.length === 0) return { x: cx, y: cy }
  let sx = 0
  let sy = 0
  for (const s of stars) {
    const p = project(s.az, s.alt, cx, cy, maxR)
    sx += p.x
    sy += p.y
  }
  return { x: sx / stars.length, y: sy / stars.length }
}

// 画单颗星:大小由视星等决定(mag 越小越亮越大),颜色暖白
function drawStar(
  ctx: CanvasRenderingContext2D,
  x: number,
  y: number,
  mag: number,
) {
  // mag 越小越亮越大;下限 1px
  const size = Math.max(1, 6 - mag)
  ctx.fillStyle = COLOR_STAR
  ctx.beginPath()
  ctx.arc(x, y, size, 0, Math.PI * 2)
  ctx.fill()
  // 轻微辉光,让亮星更醒目
  if (size >= 3) {
    ctx.save()
    ctx.globalAlpha = 0.25
    ctx.beginPath()
    ctx.arc(x, y, size * 2.2, 0, Math.PI * 2)
    ctx.fill()
    ctx.restore()
  }
}

function drawConstellationLines(
  ctx: CanvasRenderingContext2D,
  constellation: ConstellationView,
  cx: number,
  cy: number,
  maxR: number,
  highlight: boolean,
  dimmed: boolean,
) {
  if (constellation.stars.length === 0) return
  // lines 中的数字是 HIP 编号,建立 hip -> StarPoint 索引以便查找
  const hipMap = new Map<number, StarPoint>()
  for (const s of constellation.stars) {
    hipMap.set(s.hip, s)
  }
  ctx.save()
  ctx.strokeStyle = COLOR_LINE
  ctx.lineWidth = 1
  if (highlight) {
    ctx.globalAlpha = 1
    ctx.shadowColor = COLOR_LINE
    ctx.shadowBlur = 10
  } else if (dimmed) {
    ctx.globalAlpha = 0.3
  } else {
    ctx.globalAlpha = 0.6
  }
  ctx.beginPath()
  for (const [a, b] of constellation.lines) {
    // 优先按 HIP 查找;若找不到(可能 lines 是下标),回退按下标取
    const s1 = hipMap.get(a) ?? constellation.stars[a]
    const s2 = hipMap.get(b) ?? constellation.stars[b]
    if (!s1 || !s2) continue
    const p1 = project(s1.az, s1.alt, cx, cy, maxR)
    const p2 = project(s2.az, s2.alt, cx, cy, maxR)
    ctx.moveTo(p1.x, p1.y)
    ctx.lineTo(p2.x, p2.y)
  }
  ctx.stroke()
  ctx.restore()
}

function drawConstellationLabel(
  ctx: CanvasRenderingContext2D,
  constellation: ConstellationView,
  cx: number,
  cy: number,
  maxR: number,
  dimmed: boolean,
) {
  if (constellation.stars.length === 0) return
  const center = constellationCenter(constellation.stars, cx, cy, maxR)
  ctx.save()
  ctx.font = '13px "Cormorant Garamond", serif'
  ctx.fillStyle = dimmed ? 'rgba(197, 165, 114, 0.2)' : COLOR_LABEL
  ctx.textAlign = 'center'
  ctx.textBaseline = 'middle'
  ctx.fillText(constellation.latin, center.x, center.y)
  ctx.restore()
}

// 地平线环:主环 + 16 方位刻度 + 主方位标签(N/E/S/W)
function drawHorizonRing(ctx: CanvasRenderingContext2D, cx: number, cy: number, maxR: number) {
  ctx.save()

  // 主环:暗金 30% 透明,1px
  ctx.strokeStyle = COLOR_HORIZON
  ctx.lineWidth = 1
  ctx.beginPath()
  ctx.arc(cx, cy, maxR, 0, Math.PI * 2)
  ctx.stroke()

  // 16 方位(每 22.5°):主方位画标签,其他画小刻度线
  const directions = ['N', 'NNE', 'NE', 'ENE', 'E', 'ESE', 'SE', 'SSE',
    'S', 'SSW', 'SW', 'WSW', 'W', 'WNW', 'NW', 'NNW']

  for (let i = 0; i < 16; i++) {
    const angle = (i * 22.5 * Math.PI) / 180
    if (i % 4 === 0) {
      // 主方位(N/E/S/W):衬线字体标签
      ctx.font = '14px "Cormorant Garamond", serif'
      ctx.fillStyle = COLOR_DIR
      ctx.textAlign = 'center'
      ctx.textBaseline = 'middle'
      const offset = maxR + 18
      const x = cx + offset * Math.sin(angle)
      const y = cy - offset * Math.cos(angle)
      ctx.fillText(directions[i], x, y)
    } else {
      // 其他方位:小刻度线
      ctx.strokeStyle = 'rgba(197, 165, 114, 0.35)'
      ctx.lineWidth = 0.8
      const x1 = cx + maxR * Math.sin(angle)
      const y1 = cy - maxR * Math.cos(angle)
      const x2 = cx + (maxR + 5) * Math.sin(angle)
      const y2 = cy - (maxR + 5) * Math.cos(angle)
      ctx.beginPath()
      ctx.moveTo(x1, y1)
      ctx.lineTo(x2, y2)
      ctx.stroke()
    }
  }

  ctx.restore()
}

// 四角装饰角花:简洁 L 形线条 + 角点(铜版画风格简化版)
function drawCornerOrnaments(ctx: CanvasRenderingContext2D, w: number, h: number) {
  const size = 30
  const margin = 16
  const color = 'rgba(197, 165, 114, 0.4)'
  ctx.save()
  ctx.strokeStyle = color
  ctx.lineWidth = 1

  // 左上 L 形
  ctx.beginPath()
  ctx.moveTo(margin, margin + size)
  ctx.lineTo(margin, margin)
  ctx.lineTo(margin + size, margin)
  ctx.stroke()

  // 右上 L 形
  ctx.beginPath()
  ctx.moveTo(w - margin - size, margin)
  ctx.lineTo(w - margin, margin)
  ctx.lineTo(w - margin, margin + size)
  ctx.stroke()

  // 左下 L 形
  ctx.beginPath()
  ctx.moveTo(margin, h - margin - size)
  ctx.lineTo(margin, h - margin)
  ctx.lineTo(margin + size, h - margin)
  ctx.stroke()

  // 右下 L 形
  ctx.beginPath()
  ctx.moveTo(w - margin - size, h - margin)
  ctx.lineTo(w - margin, h - margin)
  ctx.lineTo(w - margin, h - margin - size)
  ctx.stroke()

  // 四角小圆点装饰
  ctx.fillStyle = color
  const dotR = 2
  ctx.beginPath(); ctx.arc(margin, margin, dotR, 0, Math.PI * 2); ctx.fill()
  ctx.beginPath(); ctx.arc(w - margin, margin, dotR, 0, Math.PI * 2); ctx.fill()
  ctx.beginPath(); ctx.arc(margin, h - margin, dotR, 0, Math.PI * 2); ctx.fill()
  ctx.beginPath(); ctx.arc(w - margin, h - margin, dotR, 0, Math.PI * 2); ctx.fill()

  ctx.restore()
}

function setupCanvas() {
  const cvs = canvas.value
  const container = containerRef.value
  if (!cvs || !container) return
  const rect = container.getBoundingClientRect()
  cssWidth = Math.max(1, rect.width)
  cssHeight = Math.max(1, rect.height)
  const dpr = window.devicePixelRatio || 1
  cvs.width = Math.floor(cssWidth * dpr)
  cvs.height = Math.floor(cssHeight * dpr)
  cvs.style.width = `${cssWidth}px`
  cvs.style.height = `${cssHeight}px`
  const ctx = cvs.getContext('2d')
  if (ctx) {
    ctx.setTransform(dpr, 0, 0, dpr, 0, 0)
  }
}

function redraw() {
  const ctx = canvas.value?.getContext('2d')
  if (!ctx) return
  if (cssWidth === 0 || cssHeight === 0) return

  // 背景:深空黑
  ctx.fillStyle = COLOR_BG
  ctx.fillRect(0, 0, cssWidth, cssHeight)

  const cx = cssWidth / 2
  const cy = cssHeight / 2
  // 半径取短边的一半再 *0.9,留出方位标空间
  const maxR = (Math.min(cssWidth, cssHeight) / 2) * 0.9

  // 地平线环(含主环 + 16 方位刻度 + 主方位标签)
  drawHorizonRing(ctx, cx, cy, maxR)

  // 四角装饰角花
  drawCornerOrnaments(ctx, cssWidth, cssHeight)

  const sv = props.skyView
  if (!sv) return

  // 判断当前悬停状态以决定高亮 / 调暗
  const hovered = props.hoveredConstellation
  const hasHover = !!hovered

  // 先画所有星座的连线(悬停的最后画,确保辉光不被遮挡)
  for (const c of sv.constellations) {
    const isHovered = hasHover && c.name === hovered
    if (isHovered) continue
    drawConstellationLines(ctx, c, cx, cy, maxR, false, hasHover)
  }
  if (hasHover) {
    const target = sv.constellations.find((c) => c.name === hovered)
    if (target) {
      drawConstellationLines(ctx, target, cx, cy, maxR, true, false)
    }
  }

  // 星点:可见星 + 各星座内的星(星座 stars 也属于 visibleStars,统一画一遍即可)
  for (const s of sv.visibleStars) {
    if (s.alt < 0) continue // 地平线以下不画
    const p = project(s.az, s.alt, cx, cy, maxR)
    drawStar(ctx, p.x, p.y, s.mag)
  }

  // 星座标签(悬停的其他星座调暗)
  for (const c of sv.constellations) {
    const isHovered = hasHover && c.name === hovered
    drawConstellationLabel(ctx, c, cx, cy, maxR, hasHover && !isHovered)
  }
}

// 容器尺寸变化时,重设 Canvas 并重绘
function handleResize() {
  setupCanvas()
  redraw()
}

// 命中检测:找到距离鼠标最近的星座(点到星座几何中心的距离)
// 阈值 80px 内则认为命中,返回星座 name;否则返回 null
const HOVER_THRESHOLD = 80
function findConstellationAt(mouseX: number, mouseY: number): string | null {
  const sv = props.skyView
  if (!sv || sv.constellations.length === 0) return null
  if (cssWidth === 0 || cssHeight === 0) return null
  const cx = cssWidth / 2
  const cy = cssHeight / 2
  const maxR = (Math.min(cssWidth, cssHeight) / 2) * 0.9
  let nearest: string | null = null
  let minDist = HOVER_THRESHOLD
  for (const c of sv.constellations) {
    if (c.stars.length === 0) continue
    const center = constellationCenter(c.stars, cx, cy, maxR)
    const d = Math.hypot(center.x - mouseX, center.y - mouseY)
    if (d < minDist) {
      minDist = d
      nearest = c.name
    }
  }
  return nearest
}

// 鼠标移动:检测命中并 emit hover(仅在变化时触发,避免高频抖动)
function onMouseMove(e: MouseEvent) {
  const cvs = canvas.value
  if (!cvs) return
  const rect = cvs.getBoundingClientRect()
  const x = e.clientX - rect.left
  const y = e.clientY - rect.top
  const hit = findConstellationAt(x, y)
  if (hit !== lastHovered) {
    lastHovered = hit
    cursorPointer.value = !!hit
    emit('hover', hit)
  }
}

// 点击:命中则 emit select
function onClick(e: MouseEvent) {
  const cvs = canvas.value
  if (!cvs) return
  const rect = cvs.getBoundingClientRect()
  const x = e.clientX - rect.left
  const y = e.clientY - rect.top
  const hit = findConstellationAt(x, y)
  if (hit) emit('select', hit)
}

// 鼠标离开 Canvas:清除悬停
function onMouseLeave() {
  if (lastHovered !== null) {
    lastHovered = null
    cursorPointer.value = false
    emit('hover', null)
  }
}

watch(() => props.skyView, (newVal) => {
  if (!newVal) return
  // 数据刷新后清除过期的悬停状态
  if (lastHovered !== null) {
    lastHovered = null
    cursorPointer.value = false
    emit('hover', null)
  }
  // 触发整体淡入动画:先移除 class,下一帧再添加,以重启 CSS animation
  fadeActive.value = false
  nextTick(() => {
    redraw()
    requestAnimationFrame(() => {
      requestAnimationFrame(() => {
        fadeActive.value = true
      })
    })
  })
}, { deep: false })

watch(() => props.hoveredConstellation, () => {
  redraw()
})

onMounted(() => {
  setupCanvas()
  redraw()
  // 初始淡入
  requestAnimationFrame(() => {
    requestAnimationFrame(() => {
      fadeActive.value = true
    })
  })
  if (containerRef.value) {
    resizeObserver = new ResizeObserver(() => {
      handleResize()
    })
    resizeObserver.observe(containerRef.value)
  }
})

onUnmounted(() => {
  if (resizeObserver) {
    resizeObserver.disconnect()
    resizeObserver = null
  }
})
</script>

<template>
  <div ref="containerRef" class="star-canvas-container">
    <canvas
      ref="canvas"
      class="star-canvas"
      :class="{ 'star-canvas--fade-in': fadeActive }"
      :style="{ cursor: cursorPointer ? 'pointer' : 'default' }"
      @mousemove="onMouseMove"
      @click="onClick"
      @mouseleave="onMouseLeave"
    />
  </div>
</template>

<style scoped>
.star-canvas-container {
  position: relative;
  width: 100%;
  height: 100%;
  background: #050811;
  overflow: hidden;
}
.star-canvas {
  display: block;
  width: 100%;
  height: 100%;
}
/* 数据更新时的整体淡入动画(400ms) */
.star-canvas--fade-in {
  animation: starFadeIn 400ms ease-out;
}
@keyframes starFadeIn {
  from { opacity: 0; }
  to { opacity: 1; }
}
</style>
