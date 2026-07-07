import { defineStore } from 'pinia'
import { ref } from 'vue'
import { apodApi, type NasaApodResponse } from '../api/apod'

export const useApodStore = defineStore('apod', () => {
  const data = ref<NasaApodResponse | null>(null)
  const loading = ref(false)
  const error = ref<string | null>(null)

  async function fetchApod() {
    loading.value = true
    error.value = null
    try {
      data.value = await apodApi.get()
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

  return { data, loading, error, fetchApod, reset }
})
