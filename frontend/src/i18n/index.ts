import { createI18n } from 'vue-i18n'
import zh from './locales/zh'
import en from './locales/en'
import ja from './locales/ja'

export type MessageSchema = typeof zh

export const SUPPORTED_LOCALES = ['zh', 'en', 'ja'] as const
export type AppLocale = (typeof SUPPORTED_LOCALES)[number]

const STORAGE_KEY = 'cityglow-lang'

function detectLocale(): AppLocale {
  const saved = localStorage.getItem(STORAGE_KEY)
  if (saved && (SUPPORTED_LOCALES as readonly string[]).includes(saved)) {
    return saved as AppLocale
  }
  return 'zh'
}

const i18n = createI18n<false>({
  legacy: false,
  locale: detectLocale(),
  fallbackLocale: 'zh',
  messages: {
    zh,
    en,
    ja,
  },
})

export function setLocale(locale: AppLocale): void {
  i18n.global.locale.value = locale
  localStorage.setItem(STORAGE_KEY, locale)
  document.documentElement.setAttribute('lang', locale)
}

// 同步 <html lang> 属性,便于辅助技术与后端内容协商
document.documentElement.setAttribute('lang', i18n.global.locale.value)

export default i18n
