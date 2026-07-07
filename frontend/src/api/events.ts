import { apiClient } from './client'

export interface AstroEvent {
  id: number
  title: string
  eventTime: string // ISO 字符串
  description: string
  eventType: 'METEOR' | 'ECLIPSE' | 'PLANET' | string
}

export interface EventListParams {
  type?: string
  after?: string
}

export const eventsApi = {
  list: (params?: EventListParams) =>
    apiClient.get<unknown, AstroEvent[]>('/events', { params }),
  getById: (id: number) => apiClient.get<unknown, AstroEvent>(`/events/${id}`),
}
