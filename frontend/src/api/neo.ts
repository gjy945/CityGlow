import { apiClient } from './client'

// 单颗近地天体接近事件
export interface NeoApproach {
  // NEO 唯一标识(NASA neo_reference_id)
  id: string
  // 名称(如 "(2024 XX1)")
  name: string
  // 接近日期 ISO 字符串
  approachDate: string
  // 估计最大直径(米)
  estimatedDiameterMaxMeters: number
  // 估计最小直径(米)
  estimatedDiameterMinMeters: number
  // 与地球最接近距离(公里)
  missDistanceKm: number
  // 相对速度(km/s)
  relativeVelocityKps: number
  // 是否潜在危险
  isPotentiallyHazardous: boolean
}

export interface NeoFeedResult {
  // 未来 N 天接近事件总数
  totalCount: number
  // 按直径降序排列的接近事件列表
  approaches: NeoApproach[]
}

export const neoApi = {
  getNeoFeed: (days = 7) =>
    apiClient.get<unknown, NeoFeedResult>('/neo/feed', { params: { days } }),
}
