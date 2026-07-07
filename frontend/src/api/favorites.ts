import { apiClient } from './client'

// 收藏观测点响应
export interface Favorite {
  id: number
  name: string
  latitude: number
  longitude: number
  createdAt: string
}

// 添加收藏请求体
export interface FavoritePayload {
  name: string
  latitude: number
  longitude: number
}

export const favoritesApi = {
  // 获取当前用户的全部收藏
  getFavorites: () => apiClient.get<unknown, Favorite[]>('/favorites'),
  // 添加收藏(幂等:相同坐标已存在则返回已有记录)
  addFavorite: (data: FavoritePayload) =>
    apiClient.post<unknown, Favorite>('/favorites', data),
  // 按坐标删除收藏
  removeFavorite: (latitude: number, longitude: number) =>
    apiClient.delete<unknown, void>('/favorites', {
      params: { latitude, longitude },
    }),
}
