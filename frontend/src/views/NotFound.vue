<script setup lang="ts">
import { RouterLink } from 'vue-router'
</script>

<template>
  <section class="notfound-page flex items-center justify-center px-6">
    <!-- 星图等高线背景装饰 -->
    <svg class="contour-deco" viewBox="0 0 800 600" preserveAspectRatio="xMidYMid slice" aria-hidden="true">
      <g fill="none" stroke="rgba(159,168,218,0.07)" stroke-width="0.8">
        <ellipse cx="400" cy="300" rx="160" ry="60" />
        <ellipse cx="400" cy="300" rx="240" ry="95" />
        <ellipse cx="400" cy="300" rx="320" ry="135" />
        <ellipse cx="400" cy="300" rx="400" ry="175" />
      </g>
      <g fill="rgba(232,234,246,0.4)">
        <circle cx="120" cy="140" r="0.9" />
        <circle cx="680" cy="100" r="1.1" />
        <circle cx="160" cy="460" r="1" />
        <circle cx="720" cy="480" r="0.9" />
        <circle cx="540" cy="160" r="0.8" />
        <circle cx="260" cy="440" r="1" />
      </g>
    </svg>
    <div class="notfound-inner text-center">
      <p class="font-mono text-[11px] tracking-[0.4em] uppercase text-dark-gold/80 mb-6">
        Section Lost · Out of Bounds
      </p>
      <h1 class="notfound-code font-display starlight-text">404</h1>
      <h2 class="font-display text-3xl sm:text-4xl text-starlight starlight-text mt-4 mb-4">
        星图迷失
      </h2>
      <p class="font-body text-sm text-moonlight/60 mb-10 max-w-[320px] mx-auto leading-relaxed">
        坐标超出已知星域,这片夜空尚未被观测记录。
      </p>
      <RouterLink to="/"
        class="font-mono text-[11px] text-dark-gold hover:text-starlight transition-colors uppercase tracking-[0.2em] border border-dark-gold/40 hover:border-starlight/60 px-6 py-3 rounded inline-block">
        返回暗夜地图
      </RouterLink>
    </div>
  </section>
</template>

<style scoped>
.notfound-page {
  position: relative;
  min-height: calc(100vh - 64px);
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
.notfound-inner {
  position: relative;
  z-index: 1;
}
.notfound-code {
  font-size: clamp(7rem, 18vw, 11rem);
  color: #e8eaf6;
  line-height: 0.9;
  font-weight: 500;
  letter-spacing: 0.04em;
  text-shadow: 0 0 30px rgba(197, 165, 114, 0.35),
    0 0 60px rgba(159, 168, 218, 0.18);
}
</style>
