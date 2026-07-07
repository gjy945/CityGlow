<script setup lang="ts">
import { onMounted, onUnmounted, ref, watch } from 'vue'
import L from 'leaflet'
import { createEventIcon } from './leaflet-icons'

export interface MapMarker {
  lat: number
  lng: number
  title: string
  type?: string
}

interface Props {
  center?: [number, number]
  zoom?: number
  markers?: MapMarker[]
}

interface Emits {
  (e: 'map-click', payload: { lat: number; lng: number }): void
  (e: 'ready', map: L.Map): void
  (e: 'marker-click', payload: { lat: number; lng: number; title: string }): void
}

const props = withDefaults(defineProps<Props>(), {
  center: () => [35.0, 105.0], // 中国中心
  zoom: 4,
  markers: () => [],
})

const emit = defineEmits<Emits>()
const mapContainer = ref<HTMLDivElement>()
let map: L.Map | null = null
let markerLayer: L.LayerGroup | null = null

function renderMarkers() {
  if (!map || !markerLayer) return
  markerLayer.clearLayers()
  for (const m of props.markers) {
    const marker = L.marker([m.lat, m.lng], {
      icon: createEventIcon(m.type),
      title: m.title,
    })
    marker.bindPopup(
      `<div class="cityglow-popup"><strong>${m.title}</strong></div>`,
    )
    marker.on('click', () => {
      emit('marker-click', { lat: m.lat, lng: m.lng, title: m.title })
    })
    markerLayer.addLayer(marker)
  }
}

onMounted(() => {
  if (!mapContainer.value) return
  map = L.map(mapContainer.value).setView(props.center, props.zoom)

  // CartoDB Dark Matter 暗色瓦片(无 key)
  L.tileLayer('https://{s}.basemaps.cartocdn.com/dark_all/{z}/{x}/{y}{r}.png', {
    attribution: '© OpenStreetMap © CARTO',
    maxZoom: 19,
    subdomains: 'abcd',
  }).addTo(map)

  markerLayer = L.layerGroup().addTo(map)
  renderMarkers()

  map.on('click', (e: L.LeafletMouseEvent) => {
    emit('map-click', { lat: e.latlng.lat, lng: e.latlng.lng })
  })

  emit('ready', map)
})

watch(
  () => props.markers,
  () => {
    renderMarkers()
  },
  { deep: true },
)

watch(
  () => props.center,
  (next) => {
    if (!map || !next) return
    map.setView(next, map.getZoom())
  },
)

watch(
  () => props.zoom,
  (next) => {
    if (!map || next == null) return
    map.setZoom(next)
  },
)

onUnmounted(() => {
  if (map) {
    map.remove()
    map = null
    markerLayer = null
  }
})

function flyTo(lat: number, lng: number, zoom?: number) {
  map?.flyTo([lat, lng], zoom ?? map.getZoom(), { duration: 1.5 })
}

function setView(lat: number, lng: number, zoom?: number) {
  map?.setView([lat, lng], zoom ?? map.getZoom())
}

function invalidateSize() {
  map?.invalidateSize()
}

defineExpose({ flyTo, setView, invalidateSize })
</script>

<template>
  <div ref="mapContainer" class="w-full h-full" />
</template>

<style>
/* Leaflet popup 深色主题(全局,不能 scoped 因为 popup 在 Leaflet 内部 DOM) */
.leaflet-popup-content-wrapper {
  background: rgba(10, 14, 26, 0.95);
  color: #e8eaf6;
  border: 1px solid rgba(197, 165, 114, 0.35);
  border-radius: 8px;
  box-shadow: 0 8px 32px rgba(0, 0, 0, 0.5);
}
.leaflet-popup-tip {
  background: rgba(10, 14, 26, 0.95);
  border: 1px solid rgba(197, 165, 114, 0.35);
}
.leaflet-popup-content {
  margin: 12px 16px;
}
.cityglow-popup {
  font-family: 'Manrope', sans-serif;
}
.cityglow-popup strong {
  color: #c5a572;
  font-weight: 600;
  font-size: 13px;
  letter-spacing: 0.02em;
}
.leaflet-container a.leaflet-popup-close-button {
  color: #e8eaf6;
}
.leaflet-container a.leaflet-popup-close-button:hover {
  color: #c5a572;
}
</style>
