import { apiClient } from './client'

// 单次地磁暴 / 太阳耀斑事件
export interface AuroraEvent {
  // 事件唯一标识
  id: string
  // NASA DONKI 事件类型:'GST'(地磁暴)/ 'FLR'(太阳耀斑)等
  type: 'GST' | 'FLR' | string
  // ISO 时间字符串
  startTime: string
  // ISO 时间字符串(可选)
  endTime?: string
  // 事件来源链接(NASA DONKI 详情页)
  link?: string
  // 备注 / 强度等附加信息
  note?: string
}

export interface AuroraForecastResult {
  // 近 N 天事件总数
  totalCount: number
  // 按时间倒序排列的事件列表(最近的在前)
  events: AuroraEvent[]
}

export const spaceWeatherApi = {
  getAuroraForecast: (days = 30) =>
    apiClient.get<unknown, AuroraForecastResult>('/space-weather/aurora', {
      params: { days },
    }),
}
