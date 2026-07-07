import { apiClient } from './client'

export interface ForecastResult {
  score: number
  cloudCover: number
  moonPhase: string
  bortleLevel: number
  message: string
  sunrise: number
  sunset: number
}

export const forecastApi = {
  getForecast: (lat: number, lng: number) =>
    apiClient.get<unknown, ForecastResult>('/astro/forecast', { params: { lat, lng } }),
}
