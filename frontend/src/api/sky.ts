import { apiClient } from './client'

export interface StarPoint {
  hip: number
  mag: number
  az: number
  alt: number
}

export interface ConstellationView {
  name: string
  latin: string
  chinese: string
  stars: StarPoint[]
  lines: number[][]
}

export interface SkyViewResult {
  visibleStars: StarPoint[]
  constellations: ConstellationView[]
  observerLat: string
  observerLng: string
  date: string
  hour: number
}

export interface MythCard {
  constellation: string
  culture: string  // "greek" | "chinese"
  title: string
  story: string
}

export const skyApi = {
  getSkyView: (lat: number, lng: number, date?: string, hour: number = 22) =>
    apiClient.get<unknown, SkyViewResult>('/sky/constellation-view', {
      params: { lat, lng, date, hour },
    }),
  getMyths: (constellation: string) =>
    apiClient.get<unknown, MythCard[]>(`/sky/myths/${constellation}`),
}
