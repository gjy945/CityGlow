import { defineStore } from 'pinia'
import { ref } from 'vue'
import {
  logsApi,
  type ObservationLog,
  type UploadResult,
  type UploadPayload,
} from '../api/logs'

export const useLogsStore = defineStore('logs', () => {
  const list = ref<ObservationLog[]>([])
  const current = ref<ObservationLog | null>(null)
  const loading = ref(false)
  const uploading = ref(false)
  const lastUpload = ref<UploadResult | null>(null)
  const error = ref<string | null>(null)

  async function fetchList() {
    loading.value = true
    error.value = null
    try {
      list.value = await logsApi.list()
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
      current.value = await logsApi.getById(id)
    } catch (e) {
      error.value = (e as Error).message
    } finally {
      loading.value = false
    }
  }

  async function upload(payload: UploadPayload) {
    uploading.value = true
    error.value = null
    try {
      const result = await logsApi.upload(payload)
      lastUpload.value = result
      return result
    } catch (e) {
      error.value = (e as Error).message
      throw e
    } finally {
      uploading.value = false
    }
  }

  function reset() {
    list.value = []
    current.value = null
    loading.value = false
    uploading.value = false
    lastUpload.value = null
    error.value = null
  }

  return {
    list,
    current,
    loading,
    uploading,
    lastUpload,
    error,
    fetchList,
    fetchById,
    upload,
    reset,
  }
})
