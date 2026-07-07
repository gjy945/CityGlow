import { apiClient } from './client'

export interface ObservationLog {
  id: number
  userId: number | null
  locationName: string | null
  latitude: number
  longitude: number
  imageUrl: string | null
  bortleLevel: number | null
  description: string | null
  createdAt: string
}

export interface UploadResult {
  logId: number
  cardUrl: string
}

export interface UploadPayload {
  image: File
  lat: number
  lng: number
  locationName?: string
  description?: string
}

export const logsApi = {
  upload: (data: UploadPayload) => {
    const formData = new FormData()
    formData.append('image', data.image)
    formData.append('lat', String(data.lat))
    formData.append('lng', String(data.lng))
    if (data.locationName) formData.append('locationName', data.locationName)
    if (data.description) formData.append('description', data.description)
    return apiClient.post<unknown, UploadResult>('/logs/upload', formData, {
      headers: { 'Content-Type': 'multipart/form-data' },
    })
  },
  list: () => apiClient.get<unknown, ObservationLog[]>('/logs'),
  getById: (id: number) => apiClient.get<unknown, ObservationLog>(`/logs/${id}`),
}
