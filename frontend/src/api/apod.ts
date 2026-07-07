import { apiClient } from './client'

export interface NasaApodResponse {
  title: string
  explanation: string
  url: string
  hdurl?: string
  media_type: string
  date: string
  copyright?: string
}

export const apodApi = {
  get: () => apiClient.get<unknown, NasaApodResponse>('/apod'),
}
