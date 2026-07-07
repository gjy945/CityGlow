import { defineStore } from 'pinia'
import { ref } from 'vue'
import { favoritesApi, type Favorite } from '../api/favorites'

export const useFavoritesStore = defineStore('favorites', () => {
  const list = ref<Favorite[]>([])
  const loading = ref(false)
  const error = ref<string | null>(null)

  // 判断某坐标是否已收藏(经纬度完全匹配)
  function isFavorite(lat: number, lng: number): boolean {
    return list.value.some(
      (f) => f.latitude === lat && f.longitude === lng,
    )
  }

  // 经纬度匹配的收藏记录(用于获取 id/createdAt 等)
  function findFavorite(lat: number, lng: number): Favorite | undefined {
    return list.value.find((f) => f.latitude === lat && f.longitude === lng)
  }

  async function fetch() {
    loading.value = true
    error.value = null
    try {
      list.value = await favoritesApi.getFavorites()
    } catch (e) {
      error.value = (e as Error).message
    } finally {
      loading.value = false
    }
  }

  async function add(name: string, lat: number, lng: number) {
    error.value = null
    try {
      const added = await favoritesApi.addFavorite({
        name,
        latitude: lat,
        longitude: lng,
      })
      // 幂等:若列表中已存在相同坐标则替换,否则追加
      const idx = list.value.findIndex(
        (f) => f.latitude === lat && f.longitude === lng,
      )
      if (idx >= 0) {
        list.value[idx] = added
      } else {
        list.value.unshift(added)
      }
      return added
    } catch (e) {
      error.value = (e as Error).message
      throw e
    }
  }

  async function remove(lat: number, lng: number) {
    error.value = null
    try {
      await favoritesApi.removeFavorite(lat, lng)
      list.value = list.value.filter(
        (f) => !(f.latitude === lat && f.longitude === lng),
      )
    } catch (e) {
      error.value = (e as Error).message
      throw e
    }
  }

  function reset() {
    list.value = []
    loading.value = false
    error.value = null
  }

  return {
    list,
    loading,
    error,
    isFavorite,
    findFavorite,
    fetch,
    add,
    remove,
    reset,
  }
})
