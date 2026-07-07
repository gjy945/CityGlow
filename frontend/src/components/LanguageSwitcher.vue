<script setup lang="ts">
import { ref, computed, onMounted, onUnmounted } from 'vue'
import { useI18n } from 'vue-i18n'
import { setLocale, type AppLocale } from '../i18n'

const { locale } = useI18n()

const open = ref(false)
const rootRef = ref<HTMLElement | null>(null)

interface Option {
  code: AppLocale
  label: string
  short: string
}

const options: Option[] = [
  { code: 'zh', label: '中文', short: 'ZH' },
  { code: 'en', label: 'English', short: 'EN' },
  { code: 'ja', label: '日本語', short: 'JA' },
]

const current = computed(
  () => options.find((o) => o.code === locale.value) ?? options[0],
)

function pick(code: AppLocale) {
  if (code !== locale.value) {
    setLocale(code)
  }
  open.value = false
}

function onDocClick(e: MouseEvent) {
  if (rootRef.value && !rootRef.value.contains(e.target as Node)) {
    open.value = false
  }
}

function onKey(e: KeyboardEvent) {
  if (e.key === 'Escape') open.value = false
}

onMounted(() => {
  document.addEventListener('click', onDocClick)
  document.addEventListener('keydown', onKey)
})
onUnmounted(() => {
  document.removeEventListener('click', onDocClick)
  document.removeEventListener('keydown', onKey)
})
</script>

<template>
  <div ref="rootRef" class="lang-switcher">
    <button
      type="button"
      class="lang-trigger"
      :aria-expanded="open"
      aria-haspopup="listbox"
      @click="open = !open"
    >
      <svg
        width="16"
        height="16"
        viewBox="0 0 24 24"
        fill="none"
        stroke="currentColor"
        stroke-width="1.5"
        stroke-linecap="round"
        stroke-linejoin="round"
        aria-hidden="true"
      >
        <circle cx="12" cy="12" r="9" />
        <path d="M3 12h18" />
        <path d="M12 3a14 14 0 0 1 0 18a14 14 0 0 1 0 -18" />
      </svg>
      <span class="lang-code font-mono">{{ current.short }}</span>
      <svg
        class="lang-caret"
        :class="{ 'lang-caret--open': open }"
        width="10"
        height="10"
        viewBox="0 0 12 12"
        fill="none"
        stroke="currentColor"
        stroke-width="1.5"
        stroke-linecap="round"
        stroke-linejoin="round"
        aria-hidden="true"
      >
        <path d="M3 4.5 L6 7.5 L9 4.5" />
      </svg>
    </button>

    <Transition name="lang-dropdown">
      <ul v-if="open" class="lang-menu glass-panel" role="listbox">
        <li
          v-for="opt in options"
          :key="opt.code"
          role="option"
          :aria-selected="opt.code === locale"
        >
          <button
            type="button"
            class="lang-option"
            :class="{ 'lang-option--active': opt.code === locale }"
            @click="pick(opt.code)"
          >
            <span class="lang-option-label">{{ opt.label }}</span>
            <span class="lang-option-code font-mono">{{ opt.short }}</span>
            <svg
              v-if="opt.code === locale"
              class="lang-option-check"
              width="12"
              height="12"
              viewBox="0 0 12 12"
              fill="none"
              stroke="currentColor"
              stroke-width="1.6"
              stroke-linecap="round"
              stroke-linejoin="round"
              aria-hidden="true"
            >
              <path d="M2.5 6.5 L5 9 L9.5 3.5" />
            </svg>
          </button>
        </li>
      </ul>
    </Transition>
  </div>
</template>

<style scoped>
.lang-switcher {
  position: relative;
  display: inline-block;
}

.lang-trigger {
  display: inline-flex;
  align-items: center;
  gap: 7px;
  padding: 7px 11px;
  border-radius: 8px;
  border: 1px solid rgba(197, 165, 114, 0.32);
  background: rgba(26, 31, 58, 0.55);
  backdrop-filter: blur(12px);
  -webkit-backdrop-filter: blur(12px);
  color: rgba(232, 234, 246, 0.82);
  cursor: pointer;
  transition: all 0.25s ease;
}
.lang-trigger:hover {
  border-color: rgba(197, 165, 114, 0.7);
  color: #c5a572;
  background: rgba(197, 165, 114, 0.1);
}

.lang-code {
  font-size: 11px;
  font-weight: 600;
  letter-spacing: 0.12em;
}

.lang-caret {
  transition: transform 0.25s ease;
  opacity: 0.7;
}
.lang-caret--open {
  transform: rotate(180deg);
}

.lang-menu {
  position: absolute;
  top: calc(100% + 8px);
  right: 0;
  min-width: 168px;
  padding: 6px;
  border-radius: 10px;
  border: 1px solid rgba(197, 165, 114, 0.28);
  box-shadow: 0 12px 36px rgba(0, 0, 0, 0.45);
  z-index: 60;
  list-style: none;
  margin: 0;
}
.lang-menu li {
  list-style: none;
}

.lang-option {
  display: flex;
  align-items: center;
  gap: 10px;
  width: 100%;
  padding: 9px 11px;
  border: none;
  background: transparent;
  border-radius: 6px;
  color: rgba(232, 234, 246, 0.72);
  cursor: pointer;
  transition: all 0.2s ease;
  text-align: left;
}
.lang-option:hover {
  background: rgba(197, 165, 114, 0.1);
  color: #e8eaf6;
}
.lang-option--active {
  color: #c5a572;
}
.lang-option--active:hover {
  color: #c5a572;
}

.lang-option-label {
  flex: 1;
  font-family: 'Manrope', sans-serif;
  font-size: 13px;
}

.lang-option-code {
  font-size: 10px;
  letter-spacing: 0.12em;
  color: rgba(159, 168, 218, 0.55);
}
.lang-option--active .lang-option-code {
  color: rgba(197, 165, 114, 0.7);
}

.lang-option-check {
  color: #c5a572;
}

.lang-dropdown-enter-active,
.lang-dropdown-leave-active {
  transition: opacity 0.22s ease, transform 0.22s ease;
  transform-origin: top right;
}
.lang-dropdown-enter-from,
.lang-dropdown-leave-to {
  opacity: 0;
  transform: scale(0.94) translateY(-4px);
}
</style>
