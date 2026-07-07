<script setup lang="ts">
import { ref, computed, onMounted, watch, onUnmounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useI18n } from 'vue-i18n'
import { useLogsStore } from '../stores/logs'

const { t } = useI18n()
const route = useRoute()
const router = useRouter()
const logsStore = useLogsStore()

const detailId = computed<number | null>(() => {
  const id = route.params.id
  if (id == null || id === '') return null
  const n = Number(id)
  return Number.isFinite(n) && n > 0 ? n : null
})

const isDetailMode = computed(() => detailId.value != null)

// ───────────────────── 详情模式 ─────────────────────
watch(
  detailId,
  async (id) => {
    if (id != null) {
      await logsStore.fetchById(id)
    }
  },
  { immediate: true },
)

const detail = computed(() => logsStore.current)
const detailLoading = computed(
  () => logsStore.loading && !detail.value && detailId.value != null,
)
const detailError = computed(() => {
  if (detailId.value != null && !logsStore.loading && !detail.value) {
    return logsStore.error || t('postcard.notFoundHint')
  }
  return null
})

// ───────────────────── 上传模式 ─────────────────────
const file = ref<File | null>(null)
const previewUrl = ref<string | null>(null)
const lat = ref<string>('')
const lng = ref<string>('')
const locationName = ref('')
const description = ref('')
const formError = ref<string | null>(null)
const locating = ref(false)
const dragging = ref(false)
const fileInput = ref<HTMLInputElement | null>(null)

function selectFile(f: File) {
  if (!['image/jpeg', 'image/png'].includes(f.type)) {
    formError.value = t('postcard.errors.unsupportedFormat')
    return
  }
  file.value = f
  if (previewUrl.value) URL.revokeObjectURL(previewUrl.value)
  previewUrl.value = URL.createObjectURL(f)
  formError.value = null
}

function onFileChange(e: Event) {
  const input = e.target as HTMLInputElement
  if (input.files && input.files.length) {
    selectFile(input.files[0])
  }
}

function onDrop(e: DragEvent) {
  dragging.value = false
  const files = e.dataTransfer?.files
  if (files && files.length) selectFile(files[0])
}

function triggerFileInput() {
  fileInput.value?.click()
}

function clearFile() {
  file.value = null
  if (previewUrl.value) URL.revokeObjectURL(previewUrl.value)
  previewUrl.value = null
  if (fileInput.value) fileInput.value.value = ''
}

function locate() {
  if (!navigator.geolocation) {
    formError.value = t('postcard.errors.noGeolocation')
    return
  }
  locating.value = true
  navigator.geolocation.getCurrentPosition(
    (pos) => {
      lat.value = pos.coords.latitude.toFixed(4)
      lng.value = pos.coords.longitude.toFixed(4)
      locating.value = false
      formError.value = null
    },
    () => {
      formError.value = t('postcard.errors.locateFailed')
      locating.value = false
    },
    { enableHighAccuracy: true, timeout: 10000 },
  )
}

async function submit() {
  formError.value = null
  if (!file.value) {
    formError.value = t('postcard.errors.noFile')
    return
  }
  const latNum = Number(lat.value)
  const lngNum = Number(lng.value)
  if (!Number.isFinite(latNum) || latNum < -90 || latNum > 90) {
    formError.value = t('postcard.errors.latRange')
    return
  }
  if (!Number.isFinite(lngNum) || lngNum < -180 || lngNum > 180) {
    formError.value = t('postcard.errors.lngRange')
    return
  }
  try {
    const result = await logsStore.upload({
      image: file.value,
      lat: latNum,
      lng: lngNum,
      locationName: locationName.value || undefined,
      description: description.value || undefined,
    })
    if (previewUrl.value) URL.revokeObjectURL(previewUrl.value)
    router.push(`/postcard/${result.logId}`)
  } catch (e) {
    formError.value = (e as Error).message || t('postcard.errors.uploadFailed')
  }
}

// ───────────────────── 画廊 ─────────────────────
onMounted(() => {
  logsStore.fetchList()
})

onUnmounted(() => {
  if (previewUrl.value) URL.revokeObjectURL(previewUrl.value)
})

const gallery = computed(() => logsStore.list)

// ───────────────────── 辅助函数 ─────────────────────
function formatDateTime(iso: string): string {
  const d = new Date(iso)
  if (Number.isNaN(d.getTime())) return '--'
  const pad = (n: number) => String(n).padStart(2, '0')
  return `${d.getFullYear()}.${pad(d.getMonth() + 1)}.${pad(
    d.getDate(),
  )} ${pad(d.getHours())}:${pad(d.getMinutes())}`
}

function formatDateShort(iso: string): string {
  const d = new Date(iso)
  if (Number.isNaN(d.getTime())) return '--.--.--'
  const pad = (n: number) => String(n).padStart(2, '0')
  return `${d.getFullYear()}.${pad(d.getMonth() + 1)}.${pad(d.getDate())}`
}

function formatCoord(v: number | null | undefined, suffix: string): string {
  if (v == null || !Number.isFinite(v)) return '--'
  return `${v.toFixed(4)}°${suffix}`
}

function bortleColor(level: number | null): string {
  if (level == null) return '#9fa8da'
  if (level <= 2) return '#c5a572'
  if (level <= 4) return '#9fa8da'
  if (level <= 6) return '#c98850'
  return '#b85a5a'
}

function bortleLabel(level: number | null): string {
  if (level == null) return t('postcard.bortleLabels.unrated')
  if (level <= 2) return t('postcard.bortleLabels.excellent')
  if (level <= 4) return t('postcard.bortleLabels.good')
  if (level <= 6) return t('postcard.bortleLabels.moderate')
  return t('postcard.bortleLabels.severe')
}
</script>

<template>
  <section class="postcard-page">
    <!-- 星图等高线背景装饰 -->
    <svg class="contour-deco" viewBox="0 0 1200 700" preserveAspectRatio="xMidYMid slice" aria-hidden="true">
      <g fill="none" stroke="rgba(159,168,218,0.07)" stroke-width="0.8">
        <ellipse cx="150" cy="600" rx="200" ry="80" />
        <ellipse cx="150" cy="600" rx="270" ry="115" />
        <ellipse cx="150" cy="600" rx="340" ry="150" />
        <ellipse cx="1050" cy="100" rx="180" ry="65" />
        <ellipse cx="1050" cy="100" rx="250" ry="95" />
      </g>
      <g fill="rgba(232,234,246,0.35)">
        <circle cx="90" cy="180" r="0.9" />
        <circle cx="280" cy="80" r="1" />
        <circle cx="540" cy="220" r="0.8" />
        <circle cx="760" cy="60" r="1.1" />
        <circle cx="980" cy="280" r="0.9" />
        <circle cx="1120" cy="460" r="1" />
        <circle cx="380" cy="560" r="0.9" />
        <circle cx="680" cy="640" r="0.8" />
      </g>
    </svg>

    <div class="postcard-inner">
      <!-- ════════════ 详情模式 ════════════ -->
      <template v-if="isDetailMode">
        <button class="back-link" @click="router.push('/postcard')">
          <svg width="14" height="14" viewBox="0 0 14 14" fill="none">
            <path d="M9 2 L4 7 L9 12" stroke="currentColor" stroke-width="1.5" stroke-linecap="round" stroke-linejoin="round" />
          </svg>
          {{ t('postcard.backToGallery') }}
        </button>

        <!-- 加载中 -->
        <div v-if="detailLoading" class="detail-state">
          <div class="spinner"></div>
          <p class="font-mono text-xs text-moonlight/60 mt-4 tracking-wider">
            {{ t('postcard.retrieving') }}
          </p>
        </div>

        <!-- 错误 -->
        <div v-else-if="detailError" class="detail-state">
          <p class="font-display text-2xl text-starlight/70">{{ t('postcard.notFound') }}</p>
          <p class="font-mono text-[10px] text-moonlight/50 mt-3">{{ detailError }}</p>
          <button class="back-link back-link--cta mt-6" @click="router.push('/postcard')">
            {{ t('postcard.backToGallery') }}
          </button>
        </div>

        <!-- 明信片详情 -->
        <article v-else-if="detail" class="postcard-detail">
          <div class="postcard-frame">
            <!-- 邮票装饰 -->
            <div class="postage-stamp" aria-hidden="true">
              <svg viewBox="0 0 24 24" width="20" height="20" fill="none" stroke="currentColor" stroke-width="1.3">
                <path d="M12 2 L14.4 9 L22 9 L15.8 13.5 L18.2 21 L12 16.5 L5.8 21 L8.2 13.5 L2 9 L9.6 9 Z" fill="currentColor" opacity="0.18" />
                <path d="M12 2 L14.4 9 L22 9 L15.8 13.5 L18.2 21 L12 16.5 L5.8 21 L8.2 13.5 L2 9 L9.6 9 Z" />
              </svg>
              <p class="stamp-value font-mono">CITYGLOW</p>
              <p class="stamp-sub font-mono">2026</p>
            </div>

            <!-- 照片 -->
            <div class="postcard-photo">
              <img
                v-if="detail.imageUrl"
                :src="detail.imageUrl"
                :alt="detail.locationName || t('postcard.unnamed')"
              />
              <div v-else class="postcard-photo-empty">
                <svg viewBox="0 0 48 48" width="40" height="40" fill="none" stroke="currentColor" stroke-width="1">
                  <circle cx="24" cy="24" r="17" />
                  <path d="M17 24 L24 31 L31 17" stroke-linecap="round" stroke-linejoin="round" />
                </svg>
                <p class="font-mono text-[10px] text-moonlight/40 mt-2">{{ t('postcard.noImage') }}</p>
              </div>
            </div>

            <!-- 邮戳装饰 -->
            <div class="postmark" aria-hidden="true">
              <svg viewBox="0 0 100 100" width="86" height="86">
                <circle cx="50" cy="50" r="47" fill="none" stroke="currentColor" stroke-width="0.7" />
                <circle cx="50" cy="50" r="40" fill="none" stroke="currentColor" stroke-width="0.5" stroke-dasharray="1.5 2" />
                <g stroke="currentColor" stroke-width="0.8" stroke-linecap="round">
                  <line x1="50" y1="3" x2="50" y2="9" />
                  <line x1="50" y1="91" x2="50" y2="97" />
                  <line x1="3" y1="50" x2="9" y2="50" />
                  <line x1="91" y1="50" x2="97" y2="50" />
                  <line x1="16.3" y1="16.3" x2="20.5" y2="20.5" />
                  <line x1="79.5" y1="79.5" x2="83.7" y2="83.7" />
                  <line x1="83.7" y1="16.3" x2="79.5" y2="20.5" />
                  <line x1="20.5" y1="79.5" x2="16.3" y2="83.7" />
                </g>
              </svg>
              <div class="postmark-inner">
                <p class="postmark-line postmark-place">CITYGLOW</p>
                <p class="postmark-line postmark-date font-mono">{{ formatDateShort(detail.createdAt) }}</p>
                <p class="postmark-line postmark-sub">DARK SKY</p>
              </div>
            </div>

            <!-- 题字区 -->
            <div class="postcard-caption">
              <p class="caption-tag font-mono">Starry Postcard · No.{{ detail.id }}</p>
              <h2 class="caption-place font-display starlight-text">
                {{ detail.locationName || t('postcard.unnamed') }}
              </h2>
              <div class="caption-meta">
                <div class="meta-row">
                  <span class="meta-label">{{ t('postcard.coord') }}</span>
                  <span class="meta-value font-mono">
                    {{ formatCoord(detail.latitude, 'N') }} · {{ formatCoord(detail.longitude, 'E') }}
                  </span>
                </div>
                <div class="meta-row">
                  <span class="meta-label">{{ t('postcard.bortle') }}</span>
                  <span class="meta-value font-mono" :style="{ color: bortleColor(detail.bortleLevel) }">
                    {{ detail.bortleLevel ?? '--' }} {{ t('common.levelUnit') }}
                    <span class="meta-sub">{{ bortleLabel(detail.bortleLevel) }}</span>
                  </span>
                </div>
                <div class="meta-row">
                  <span class="meta-label">{{ t('postcard.moment') }}</span>
                  <span class="meta-value font-mono">{{ formatDateTime(detail.createdAt) }}</span>
                </div>
              </div>
              <p v-if="detail.description" class="caption-desc">
                {{ detail.description }}
              </p>
            </div>
          </div>
        </article>
      </template>

      <!-- ════════════ 上传 + 画廊模式 ════════════ -->
      <template v-else>
        <header class="page-header">
          <p class="section-tag">{{ t('postcard.sectionTag') }}</p>
          <h1 class="page-title font-display starlight-text">{{ t('postcard.title') }}</h1>
          <p class="page-sub">{{ t('postcard.subtitle') }}</p>
        </header>

        <!-- 上传表单 -->
        <div class="upload-grid">
          <!-- 拖拽上传区 -->
          <div
            class="dropzone"
            :class="{ 'dropzone--active': dragging, 'dropzone--filled': !!previewUrl }"
            @click="triggerFileInput"
            @dragover.prevent="dragging = true"
            @dragleave.prevent="dragging = false"
            @drop.prevent="onDrop"
          >
            <input
              ref="fileInput"
              type="file"
              accept="image/jpeg,image/png"
              class="file-input"
              @change="onFileChange"
            />
            <div v-if="!previewUrl" class="dropzone-empty">
              <svg viewBox="0 0 48 48" width="40" height="40" fill="none" stroke="currentColor" stroke-width="1.1" stroke-linecap="round">
                <circle cx="24" cy="24" r="18" />
                <path d="M24 16 V32 M16 24 H32" />
              </svg>
              <p class="dz-title font-display">{{ t('postcard.uploadPhoto') }}</p>
              <p class="dz-hint font-mono">{{ t('postcard.dropHint') }}</p>
              <p class="dz-formats font-mono">{{ t('postcard.formats') }}</p>
            </div>
            <div v-else class="dropzone-preview">
              <img :src="previewUrl" :alt="t('postcard.preview')" />
              <button class="dz-clear" @click.stop="clearFile" :aria-label="t('postcard.removeImage')">
                <svg width="12" height="12" viewBox="0 0 14 14" fill="none">
                  <path d="M2 2 L12 12 M12 2 L2 12" stroke="currentColor" stroke-width="1.5" stroke-linecap="round" />
                </svg>
              </button>
              <p class="dz-filename font-mono">{{ file?.name }}</p>
            </div>
          </div>

          <!-- 表单字段 -->
          <div class="form-panel glass-panel">
            <p class="form-tag font-mono">{{ t('postcard.obsInfo') }}</p>

            <div class="field">
              <label class="field-label">{{ t('postcard.locationName') }}</label>
              <input
                v-model="locationName"
                type="text"
                class="field-input"
                :placeholder="t('postcard.locationPlaceholder')"
                maxlength="100"
              />
            </div>

            <div class="field-row">
              <div class="field">
                <label class="field-label">{{ t('postcard.latitude') }}</label>
                <input
                  v-model="lat"
                  type="text"
                  inputmode="decimal"
                  class="field-input field-input--mono"
                  placeholder="-90 ~ 90"
                />
              </div>
              <div class="field">
                <label class="field-label">{{ t('postcard.longitude') }}</label>
                <input
                  v-model="lng"
                  type="text"
                  inputmode="decimal"
                  class="field-input field-input--mono"
                  placeholder="-180 ~ 180"
                />
              </div>
            </div>

            <button
              class="locate-btn"
              :disabled="locating"
              @click="locate"
            >
              <div v-if="locating" class="spinner spinner--sm"></div>
              <svg v-else width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.6" stroke-linecap="round">
                <circle cx="12" cy="12" r="3.2" />
                <path d="M12 2v3M12 19v3M2 12h3M19 12h3" />
              </svg>
              {{ locating ? t('postcard.locating') : t('postcard.locateBtn') }}
            </button>

            <div class="field field--grow">
              <label class="field-label">{{ t('postcard.description') }}</label>
              <textarea
                v-model="description"
                class="field-input field-textarea"
                :placeholder="t('postcard.descPlaceholder')"
                maxlength="500"
                rows="3"
              ></textarea>
            </div>

            <p v-if="formError" class="form-error font-mono">{{ formError }}</p>

            <button
              class="submit-btn"
              :disabled="logsStore.uploading"
              @click="submit"
            >
              <div v-if="logsStore.uploading" class="spinner spinner--sm"></div>
              <span>{{ logsStore.uploading ? t('postcard.generatingPostcard') : t('postcard.generate') }}</span>
            </button>
          </div>
        </div>

        <!-- 画廊 -->
        <section class="gallery-section">
          <div class="gallery-head">
            <p class="gallery-tag font-mono">{{ t('postcard.galleryTitle') }}</p>
            <p v-if="gallery.length" class="gallery-count font-mono">
              {{ t('postcard.count', { n: gallery.length }) }}
            </p>
          </div>

          <div v-if="logsStore.loading && !gallery.length" class="gallery-state">
            <div class="spinner spinner--sm"></div>
            <p class="font-mono text-xs text-moonlight/50 mt-3 tracking-wider">
              {{ t('postcard.collecting') }}
            </p>
          </div>

          <div v-else-if="gallery.length" class="gallery-grid">
            <button
              v-for="log in gallery"
              :key="log.id"
              class="gallery-card"
              @click="router.push(`/postcard/${log.id}`)"
            >
              <div class="gallery-card-img">
                <img
                  v-if="log.imageUrl"
                  :src="log.imageUrl"
                  :alt="log.locationName || t('postcard.unnamed')"
                  loading="lazy"
                />
                <div v-else class="gallery-card-placeholder">
                  <svg viewBox="0 0 24 24" width="20" height="20" fill="none" stroke="currentColor" stroke-width="1">
                    <circle cx="12" cy="12" r="9" />
                    <path d="M12 7 V12 L15 14" stroke-linecap="round" />
                  </svg>
                </div>
              </div>
              <div class="gallery-card-info">
                <p class="gallery-card-place font-display">
                  {{ log.locationName || t('postcard.unnamed') }}
                </p>
                <div class="gallery-card-foot">
                  <span class="gallery-card-date font-mono">
                    {{ formatDateShort(log.createdAt) }}
                  </span>
                  <span
                    v-if="log.bortleLevel"
                    class="gallery-card-bortle font-mono"
                    :style="{ color: bortleColor(log.bortleLevel) }"
                  >
                    B{{ log.bortleLevel }}
                  </span>
                </div>
              </div>
            </button>
          </div>

          <div v-else class="gallery-state">
            <p class="font-display text-xl text-starlight/50">{{ t('postcard.emptyGallery') }}</p>
            <p class="font-mono text-[10px] text-moonlight/40 mt-3">
              {{ t('postcard.emptyHint') }}
            </p>
          </div>
        </section>
      </template>
    </div>
  </section>
</template>

<style scoped>
.postcard-page {
  position: relative;
  min-height: calc(100vh - 64px);
  padding: 48px 0 72px;
  overflow: hidden;
}

.contour-deco {
  position: absolute;
  inset: 0;
  width: 100%;
  height: 100%;
  pointer-events: none;
  z-index: 0;
}

.postcard-inner {
  position: relative;
  z-index: 1;
  max-width: 1180px;
  margin: 0 auto;
  padding: 0 32px;
}

/* 返回链接 */
.back-link {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  font-family: 'JetBrains Mono', monospace;
  font-size: 11px;
  letter-spacing: 0.18em;
  text-transform: uppercase;
  color: rgba(159, 168, 218, 0.75);
  background: none;
  border: none;
  cursor: pointer;
  padding: 0;
  margin-bottom: 28px;
  transition: color 0.25s ease;
}
.back-link:hover {
  color: #c5a572;
}
.back-link--cta {
  border: 1px solid rgba(197, 165, 114, 0.4);
  padding: 10px 22px;
  border-radius: 4px;
  color: #c5a572;
}
.back-link--cta:hover {
  border-color: rgba(197, 165, 114, 0.8);
  color: #e8eaf6;
}

/* ════════════ 标题区 ════════════ */
.page-header {
  text-align: center;
  margin-bottom: 44px;
}
.section-tag {
  font-family: 'JetBrains Mono', monospace;
  font-size: 11px;
  letter-spacing: 0.32em;
  text-transform: uppercase;
  color: rgba(197, 165, 114, 0.85);
  margin-bottom: 14px;
}
.page-title {
  font-size: clamp(2.6rem, 5vw, 3.6rem);
  color: #e8eaf6;
  line-height: 1.05;
  font-weight: 500;
  letter-spacing: 0.02em;
}
.page-sub {
  font-family: 'Manrope', sans-serif;
  font-size: 13px;
  letter-spacing: 0.15em;
  color: rgba(159, 168, 218, 0.6);
  margin-top: 14px;
}

/* ════════════ 上传表单 ════════════ */
.upload-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 28px;
  margin-bottom: 56px;
}

.dropzone {
  position: relative;
  min-height: 340px;
  border: 1.5px dashed rgba(197, 165, 114, 0.35);
  border-radius: 12px;
  background: rgba(26, 31, 58, 0.4);
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  transition: all 0.3s ease;
  overflow: hidden;
  padding: 24px;
}
.dropzone:hover {
  border-color: rgba(197, 165, 114, 0.6);
  background: rgba(26, 31, 58, 0.55);
}
.dropzone--active {
  border-color: #c5a572;
  background: rgba(197, 165, 114, 0.08);
  box-shadow: inset 0 0 30px rgba(197, 165, 114, 0.12);
}
.dropzone--filled {
  border-style: solid;
  border-color: rgba(197, 165, 114, 0.5);
  padding: 16px;
}
.file-input {
  position: absolute;
  width: 1px;
  height: 1px;
  opacity: 0;
  overflow: hidden;
  pointer-events: none;
}

.dropzone-empty {
  text-align: center;
  color: rgba(159, 168, 218, 0.6);
}
.dropzone-empty svg {
  color: rgba(197, 165, 114, 0.6);
  margin-bottom: 18px;
  transition: color 0.3s ease;
}
.dropzone:hover .dropzone-empty svg {
  color: #c5a572;
}
.dz-title {
  font-size: 1.4rem;
  color: #e8eaf6;
  font-weight: 500;
  margin-bottom: 8px;
}
.dz-hint {
  font-size: 11px;
  letter-spacing: 0.18em;
  color: rgba(159, 168, 218, 0.6);
  text-transform: uppercase;
}
.dz-formats {
  font-size: 10px;
  letter-spacing: 0.2em;
  color: rgba(197, 165, 114, 0.55);
  margin-top: 6px;
}

.dropzone-preview {
  position: relative;
  width: 100%;
  height: 100%;
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 10px;
}
.dropzone-preview img {
  max-width: 100%;
  max-height: 280px;
  object-fit: contain;
  border-radius: 6px;
  box-shadow: 0 6px 24px rgba(0, 0, 0, 0.4);
}
.dz-clear {
  position: absolute;
  top: 0;
  right: 0;
  width: 28px;
  height: 28px;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: 50%;
  background: rgba(10, 14, 26, 0.85);
  color: #9fa8da;
  border: 1px solid rgba(232, 234, 246, 0.15);
  cursor: pointer;
  transition: all 0.2s ease;
}
.dz-clear:hover {
  color: #c5a572;
  border-color: rgba(197, 165, 114, 0.6);
}
.dz-filename {
  font-size: 10px;
  letter-spacing: 0.1em;
  color: rgba(159, 168, 218, 0.5);
  max-width: 100%;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

/* 表单面板 */
.form-panel {
  padding: 26px 28px;
  border-radius: 12px;
  display: flex;
  flex-direction: column;
  gap: 16px;
}
.form-tag {
  font-size: 10px;
  letter-spacing: 0.3em;
  text-transform: uppercase;
  color: rgba(197, 165, 114, 0.8);
  margin-bottom: 4px;
}

.field {
  display: flex;
  flex-direction: column;
  gap: 6px;
}
.field--grow {
  flex: 1;
}
.field-row {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 14px;
}
.field-label {
  font-family: 'Manrope', sans-serif;
  font-size: 11px;
  letter-spacing: 0.16em;
  text-transform: uppercase;
  color: rgba(232, 234, 246, 0.55);
}
.field-input {
  font-family: 'Manrope', sans-serif;
  font-size: 14px;
  color: #e8eaf6;
  background: rgba(10, 14, 26, 0.5);
  border: 1px solid rgba(232, 234, 246, 0.1);
  border-radius: 6px;
  padding: 10px 12px;
  outline: none;
  transition: border-color 0.25s ease, background 0.25s ease;
  width: 100%;
  box-sizing: border-box;
}
.field-input::placeholder {
  color: rgba(159, 168, 218, 0.35);
}
.field-input:focus {
  border-color: rgba(197, 165, 114, 0.6);
  background: rgba(10, 14, 26, 0.7);
}
.field-input--mono {
  font-family: 'JetBrains Mono', monospace;
  font-size: 13px;
}
.field-textarea {
  resize: vertical;
  min-height: 72px;
  font-family: 'Manrope', sans-serif;
  line-height: 1.6;
}

.locate-btn {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  align-self: flex-start;
  font-family: 'JetBrains Mono', monospace;
  font-size: 11px;
  letter-spacing: 0.16em;
  text-transform: uppercase;
  color: rgba(159, 168, 218, 0.8);
  background: rgba(159, 168, 218, 0.06);
  border: 1px solid rgba(159, 168, 218, 0.2);
  border-radius: 4px;
  padding: 8px 14px;
  cursor: pointer;
  transition: all 0.25s ease;
}
.locate-btn:hover:not(:disabled) {
  color: #c5a572;
  border-color: rgba(197, 165, 114, 0.5);
  background: rgba(197, 165, 114, 0.08);
}
.locate-btn:disabled {
  opacity: 0.6;
  cursor: wait;
}

.form-error {
  font-size: 11px;
  letter-spacing: 0.06em;
  color: #b85a5a;
  padding: 8px 12px;
  background: rgba(184, 90, 90, 0.08);
  border-left: 2px solid rgba(184, 90, 90, 0.6);
  border-radius: 0 4px 4px 0;
}

.submit-btn {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: 10px;
  font-family: 'Manrope', sans-serif;
  font-size: 13px;
  letter-spacing: 0.18em;
  text-transform: uppercase;
  font-weight: 600;
  color: #0a0e1a;
  background: linear-gradient(135deg, #c5a572 0%, #d4b888 100%);
  border: none;
  border-radius: 6px;
  padding: 13px 24px;
  cursor: pointer;
  transition: all 0.28s ease;
  margin-top: 4px;
  box-shadow: 0 4px 16px rgba(197, 165, 114, 0.25);
}
.submit-btn:hover:not(:disabled) {
  transform: translateY(-2px);
  box-shadow: 0 8px 26px rgba(197, 165, 114, 0.4);
}
.submit-btn:disabled {
  opacity: 0.75;
  cursor: wait;
}

/* ════════════ 画廊 ════════════ */
.gallery-section {
  border-top: 1px solid rgba(232, 234, 246, 0.08);
  padding-top: 40px;
}
.gallery-head {
  display: flex;
  align-items: baseline;
  justify-content: space-between;
  margin-bottom: 24px;
}
.gallery-tag {
  font-size: 11px;
  letter-spacing: 0.28em;
  text-transform: uppercase;
  color: rgba(197, 165, 114, 0.85);
}
.gallery-count {
  font-size: 11px;
  letter-spacing: 0.12em;
  color: rgba(159, 168, 218, 0.55);
}

.gallery-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 20px;
}

.gallery-card {
  display: flex;
  flex-direction: column;
  text-align: left;
  background: none;
  border: none;
  padding: 0;
  cursor: pointer;
  transition: transform 0.3s cubic-bezier(0.4, 0, 0.2, 1);
}
.gallery-card:hover {
  transform: translateY(-5px);
}
.gallery-card-img {
  position: relative;
  aspect-ratio: 4 / 3;
  border: 1.5px solid rgba(197, 165, 114, 0.25);
  border-radius: 6px;
  overflow: hidden;
  background: rgba(10, 14, 26, 0.6);
  transition: border-color 0.3s ease, box-shadow 0.3s ease;
}
.gallery-card-img img {
  width: 100%;
  height: 100%;
  object-fit: cover;
  display: block;
  transition: transform 0.5s ease, filter 0.3s ease;
  filter: brightness(0.92);
}
.gallery-card:hover .gallery-card-img {
  border-color: rgba(197, 165, 114, 0.65);
  box-shadow: 0 8px 24px rgba(0, 0, 0, 0.4),
    0 0 0 1px rgba(197, 165, 114, 0.3);
}
.gallery-card:hover .gallery-card-img img {
  transform: scale(1.06);
  filter: brightness(1);
}
.gallery-card-placeholder {
  width: 100%;
  height: 100%;
  display: flex;
  align-items: center;
  justify-content: center;
  color: rgba(159, 168, 218, 0.3);
}
.gallery-card-info {
  padding: 10px 4px 4px;
}
.gallery-card-place {
  font-size: 1rem;
  color: #e8eaf6;
  font-weight: 500;
  line-height: 1.3;
  margin-bottom: 6px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.gallery-card-foot {
  display: flex;
  align-items: center;
  justify-content: space-between;
}
.gallery-card-date {
  font-size: 10px;
  letter-spacing: 0.1em;
  color: rgba(159, 168, 218, 0.5);
}
.gallery-card-bortle {
  font-size: 10px;
  font-weight: 600;
  letter-spacing: 0.06em;
  padding: 1px 5px;
  border: 1px solid currentColor;
  border-radius: 3px;
  opacity: 0.85;
}

.gallery-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  text-align: center;
  padding: 56px 16px;
}

/* ════════════ 明信片详情 ════════════ */
.detail-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  text-align: center;
  padding: 80px 16px;
}

.postcard-detail {
  display: flex;
  justify-content: center;
}

.postcard-frame {
  position: relative;
  width: 100%;
  max-width: 680px;
  background: rgba(26, 31, 58, 0.55);
  backdrop-filter: blur(12px);
  -webkit-backdrop-filter: blur(12px);
  border: 2px solid #c5a572;
  border-radius: 4px;
  padding: 18px;
  box-shadow: 0 24px 70px rgba(0, 0, 0, 0.55),
    0 0 0 1px rgba(197, 165, 114, 0.25),
    inset 0 0 0 1px rgba(197, 165, 114, 0.1);
}

/* 邮票 */
.postage-stamp {
  position: absolute;
  top: 30px;
  right: 30px;
  z-index: 4;
  width: 62px;
  padding: 8px 6px 6px;
  background: rgba(197, 165, 114, 0.1);
  border: 1.5px dashed rgba(197, 165, 114, 0.65);
  border-radius: 2px;
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 3px;
  color: #c5a572;
  transform: rotate(4deg);
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.3);
}
.postage-stamp svg {
  margin-bottom: 2px;
}
.stamp-value {
  font-size: 8px;
  letter-spacing: 0.12em;
  font-weight: 700;
  color: rgba(197, 165, 114, 0.85);
}
.stamp-sub {
  font-size: 7px;
  letter-spacing: 0.18em;
  color: rgba(197, 165, 114, 0.6);
}

/* 照片 */
.postcard-photo {
  position: relative;
  width: 100%;
  aspect-ratio: 3 / 2;
  background: #050811;
  border: 1px solid rgba(197, 165, 114, 0.3);
  border-radius: 2px;
  overflow: hidden;
  display: flex;
  align-items: center;
  justify-content: center;
}
.postcard-photo img {
  width: 100%;
  height: 100%;
  object-fit: cover;
  display: block;
}
.postcard-photo-empty {
  display: flex;
  flex-direction: column;
  align-items: center;
  color: rgba(159, 168, 218, 0.3);
}

/* 邮戳 */
.postmark {
  position: absolute;
  bottom: 28px;
  right: 28px;
  z-index: 4;
  width: 86px;
  height: 86px;
  color: rgba(197, 165, 114, 0.55);
  transform: rotate(-12deg);
  pointer-events: none;
}
.postmark svg {
  position: absolute;
  inset: 0;
}
.postmark-inner {
  position: absolute;
  inset: 0;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  text-align: center;
  gap: 1px;
}
.postmark-line {
  letter-spacing: 0.08em;
  text-transform: uppercase;
}
.postmark-place {
  font-family: 'Manrope', sans-serif;
  font-size: 8px;
  font-weight: 700;
  color: rgba(197, 165, 114, 0.7);
}
.postmark-date {
  font-size: 9px;
  font-weight: 500;
  color: rgba(197, 165, 114, 0.65);
}
.postmark-sub {
  font-family: 'Manrope', sans-serif;
  font-size: 6.5px;
  color: rgba(197, 165, 114, 0.5);
}

/* 题字区 */
.postcard-caption {
  padding: 20px 6px 6px;
}
.caption-tag {
  font-size: 9px;
  letter-spacing: 0.28em;
  text-transform: uppercase;
  color: rgba(197, 165, 114, 0.65);
  margin-bottom: 10px;
}
.caption-place {
  font-size: clamp(1.8rem, 3.4vw, 2.4rem);
  color: #e8eaf6;
  line-height: 1.15;
  font-weight: 500;
  letter-spacing: 0.01em;
  margin-bottom: 18px;
}

.caption-meta {
  display: flex;
  flex-direction: column;
  gap: 8px;
  padding: 14px 0;
  border-top: 1px solid rgba(197, 165, 114, 0.18);
  border-bottom: 1px solid rgba(197, 165, 114, 0.18);
  margin-bottom: 16px;
}
.meta-row {
  display: flex;
  align-items: baseline;
  gap: 16px;
}
.meta-label {
  font-family: 'Manrope', sans-serif;
  font-size: 10px;
  letter-spacing: 0.22em;
  text-transform: uppercase;
  color: rgba(159, 168, 218, 0.55);
  min-width: 56px;
}
.meta-value {
  font-size: 13px;
  color: #e8eaf6;
  font-weight: 500;
}
.meta-sub {
  font-family: 'Manrope', sans-serif;
  font-size: 11px;
  font-weight: 400;
  color: rgba(232, 234, 246, 0.5);
  margin-left: 8px;
}

.caption-desc {
  font-family: 'Manrope', sans-serif;
  font-size: 14px;
  line-height: 1.8;
  color: rgba(232, 234, 246, 0.75);
  font-style: italic;
}

/* ════════════ 通用 ════════════ */
.spinner {
  width: 22px;
  height: 22px;
  border: 1.5px solid rgba(197, 165, 114, 0.18);
  border-top-color: #c5a572;
  border-radius: 50%;
  animation: spin 0.85s linear infinite;
}
.spinner--sm {
  width: 14px;
  height: 14px;
  border-width: 1.2px;
}
@keyframes spin {
  to { transform: rotate(360deg); }
}

/* ════════════ 响应式 ════════════ */
@media (max-width: 900px) {
  .upload-grid {
    grid-template-columns: 1fr;
  }
  .gallery-grid {
    grid-template-columns: repeat(3, 1fr);
  }
}

@media (max-width: 640px) {
  .postcard-page {
    padding: 32px 0 48px;
  }
  .postcard-inner {
    padding: 0 16px;
  }
  .gallery-grid {
    grid-template-columns: repeat(2, 1fr);
    gap: 14px;
  }
  .field-row {
    grid-template-columns: 1fr;
  }
  .form-panel {
    padding: 22px 20px;
  }
  .dropzone {
    min-height: 260px;
  }
  .postage-stamp {
    top: 24px;
    right: 24px;
    width: 52px;
  }
  .postmark {
    bottom: 22px;
    right: 22px;
    width: 70px;
    height: 70px;
  }
  .postmark svg {
    width: 70px;
    height: 70px;
  }
  .postcard-frame {
    padding: 12px;
  }
  .caption-place {
    font-size: 1.6rem;
  }
}
</style>
