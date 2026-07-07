import { apiClient } from './client'

// 今晚最佳观测时段
export interface BestWindow {
  // 时段起始时间(HH:mm,本地时)
  start: string
  // 时段结束时间(HH:mm,本地时)
  end: string
  // 综合评分(0-100)
  score: number
  // 综合消息
  message: string
  // 推荐原因列表(每条带 ✓ 标记展示)
  reasons: string[]
}

export const bestWindowApi = {
  getBestWindow: (lat: number, lng: number) =>
    apiClient.get<unknown, BestWindow>('/best-window', {
      params: { lat, lng },
    }),
}
