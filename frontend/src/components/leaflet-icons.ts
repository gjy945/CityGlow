import L from 'leaflet'

export type EventIconType = 'METEOR' | 'ECLIPSE' | 'PLANET' | string

const COLORS: Record<string, string> = {
  METEOR: '#c5a572', // 暗金
  ECLIPSE: '#9fa8da', // 月光蓝
  PLANET: '#e8eaf6', // 星辉银
}

export function eventColor(type?: string): string {
  if (!type) return '#e8eaf6'
  return COLORS[type] ?? '#e8eaf6'
}

// 不同事件类型的彩色圆点 marker,带光晕效果
export function createEventIcon(type?: EventIconType): L.DivIcon {
  const color = eventColor(type)
  return L.divIcon({
    className: 'cityglow-marker',
    html: `<div style="width:12px;height:12px;border-radius:50%;background:${color};box-shadow:0 0 10px ${color},0 0 4px ${color};border:1px solid rgba(232,234,246,0.5);"></div>`,
    iconSize: [12, 12],
    iconAnchor: [6, 6],
    popupAnchor: [0, -8],
  })
}

// 默认选址标记(地图点击后的临时标记,星辉银外圈)
export function createPinIcon(): L.DivIcon {
  return L.divIcon({
    className: 'cityglow-pin',
    html: `<div style="width:16px;height:16px;border-radius:50%;background:rgba(197,165,114,0.85);box-shadow:0 0 14px rgba(197,165,114,0.7);border:2px solid rgba(232,234,246,0.7);"></div>`,
    iconSize: [16, 16],
    iconAnchor: [8, 8],
  })
}
