<script setup lang="ts">
import { ref } from 'vue'
import DarkSkyLeaflet, { type MapMarker } from '../components/DarkSkyLeaflet.vue'

const lastClick = ref<{ lat: number; lng: number } | null>(null)

const sampleMarkers: MapMarker[] = [
  { lat: 39.9, lng: 116.4, title: '北京 · 流星雨观测点', type: 'METEOR' },
  { lat: 31.2, lng: 121.5, title: '上海 · 月食记录', type: 'ECLIPSE' },
  { lat: 30.6, lng: 104.0, title: '成都 · 行星合月', type: 'PLANET' },
]

function handleClick({ lat, lng }: { lat: number; lng: number }) {
  lastClick.value = { lat, lng }
  // eslint-disable-next-line no-console
  console.log('Map clicked:', lat, lng)
}

function handleReady() {
  // eslint-disable-next-line no-console
  console.log('DarkSkyLeaflet ready')
}
</script>

<template>
  <section class="relative h-[calc(100vh-64px)] w-full">
    <DarkSkyLeaflet
      :markers="sampleMarkers"
      @map-click="handleClick"
      @ready="handleReady"
    />
    <div
      v-if="lastClick"
      class="glass-panel pointer-events-none absolute bottom-6 left-6 z-[1000] rounded-md px-4 py-3 font-mono text-xs text-moonlight/80"
    >
      <p class="uppercase tracking-[0.2em] text-dark-gold/80">last click</p>
      <p class="mt-1">lat {{ lastClick.lat.toFixed(4) }} · lng {{ lastClick.lng.toFixed(4) }}</p>
    </div>
    <div
      class="glass-panel pointer-events-none absolute right-6 top-6 z-[1000] rounded-md px-4 py-3 font-mono text-xs text-moonlight/80"
    >
      <p class="uppercase tracking-[0.2em] text-dark-gold/80">section 01</p>
      <p class="mt-1 font-display text-base text-starlight">暗夜地图 · Dark Sky Map</p>
    </div>
  </section>
</template>
