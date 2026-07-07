import { defineStore } from 'pinia'
import { ref } from 'vue'
import { eventsApi, type AstroEvent, type EventListParams } from '../api/events'

export const useEventsStore = defineStore('events', () => {
  const list = ref<AstroEvent[]>([])
  const current = ref<AstroEvent | null>(null)
  const loading = ref(false)
  const error = ref<string | null>(null)

  async function fetchList(params?: EventListParams) {
    loading.value = true
    error.value = null
    try {
      list.value = await eventsApi.list(params)
    } catch (e) {
      error.value = (e as Error).message
    } finally {
      loading.value = false
    }
  }

  async function fetchById(id: number) {
    loading.value = true
    error.value = null
    try {
      current.value = await eventsApi.getById(id)
    } catch (e) {
      error.value = (e as Error).message
    } finally {
      loading.value = false
    }
  }

  function setCurrent(event: AstroEvent | null) {
    current.value = event
  }

  function reset() {
    list.value = []
    current.value = null
    loading.value = false
    error.value = null
  }

  return { list, current, loading, error, fetchList, fetchById, setCurrent, reset }
})
