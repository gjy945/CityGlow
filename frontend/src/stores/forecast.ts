import { defineStore } from 'pinia'
import { ref } from 'vue'
import { forecastApi, type ForecastResult } from '../api/forecast'

export const useForecastStore = defineStore('forecast', () => {
  const data = ref<ForecastResult | null>(null)
  const loading = ref(false)
  const error = ref<string | null>(null)

  async function fetchForecast(lat: number, lng: number) {
    loading.value = true
    error.value = null
    try {
      data.value = await forecastApi.getForecast(lat, lng)
    } catch (e) {
      error.value = (e as Error).message
    } finally {
      loading.value = false
    }
  }

  function reset() {
    data.value = null
    loading.value = false
    error.value = null
  }

  return { data, loading, error, fetchForecast, reset }
})
