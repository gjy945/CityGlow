<script setup lang="ts">
import { ref, computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useI18n } from 'vue-i18n'
import { useAuthStore } from '../stores/auth'

const route = useRoute()
const router = useRouter()
const { t } = useI18n()
const authStore = useAuthStore()

type Mode = 'login' | 'register'

const mode = ref<Mode>('login')
const username = ref('')
const password = ref('')
const confirmPassword = ref('')
const errorMsg = ref<string | null>(null)
const submitting = ref(false)

const isRegister = computed(() => mode.value === 'register')
const titleKey = computed(() =>
  isRegister.value ? 'auth.registerTitle' : 'auth.loginTitle',
)

function switchMode(next: Mode) {
  mode.value = next
  errorMsg.value = null
  confirmPassword.value = ''
}

function resolveRedirect(): string {
  const r = route.query.redirect
  if (typeof r === 'string' && r.startsWith('/') && !r.startsWith('//')) {
    return r
  }
  return '/'
}

// 将后端 / 客户端错误映射为 i18n 文案
function mapError(err: unknown): string {
  const msg = (err as Error)?.message ?? ''
  const lower = msg.toLowerCase()
  if (lower.includes('exist') || msg.includes('已存在')) {
    return t('auth.usernameExists')
  }
  if (lower.includes('credential') || lower.includes('invalid') || msg.includes('错误')) {
    return t('auth.invalidCredentials')
  }
  return msg || t('common.error')
}

async function submit() {
  errorMsg.value = null

  const u = username.value.trim()
  if (!u || !password.value) {
    errorMsg.value = t('auth.invalidCredentials')
    return
  }
  if (isRegister.value && password.value !== confirmPassword.value) {
    errorMsg.value = t('auth.passwordMismatch')
    return
  }

  submitting.value = true
  try {
    if (isRegister.value) {
      await authStore.register({ username: u, password: password.value })
    } else {
      await authStore.login({ username: u, password: password.value })
    }
    router.push(resolveRedirect())
  } catch (e) {
    errorMsg.value = mapError(e)
  } finally {
    submitting.value = false
  }
}
</script>

<template>
  <section class="login-page">
    <!-- 星图等高线背景装饰 -->
    <svg class="contour-deco" viewBox="0 0 800 600" preserveAspectRatio="xMidYMid slice" aria-hidden="true">
      <g fill="none" stroke="rgba(159,168,218,0.07)" stroke-width="0.8">
        <ellipse cx="400" cy="300" rx="160" ry="60" />
        <ellipse cx="400" cy="300" rx="240" ry="95" />
        <ellipse cx="400" cy="300" rx="320" ry="135" />
        <ellipse cx="400" cy="300" rx="400" ry="175" />
      </g>
      <g fill="rgba(232,234,246,0.4)">
        <circle cx="120" cy="140" r="0.9" />
        <circle cx="680" cy="100" r="1.1" />
        <circle cx="160" cy="460" r="1" />
        <circle cx="720" cy="480" r="0.9" />
        <circle cx="540" cy="160" r="0.8" />
        <circle cx="260" cy="440" r="1" />
      </g>
    </svg>

    <div class="login-card glass-panel moon-glow">
      <header class="login-header">
        <p class="login-tag font-mono">Section · Gate of Stars</p>
        <h1 class="login-title font-display starlight-text">
          {{ t(titleKey) }}
        </h1>
      </header>

      <!-- 模式切换 -->
      <div class="mode-tabs" role="tablist">
        <button
          type="button"
          role="tab"
          :aria-selected="!isRegister"
          class="mode-tab"
          :class="{ 'mode-tab--active': !isRegister }"
          @click="switchMode('login')"
        >
          {{ t('auth.loginBtn') }}
        </button>
        <button
          type="button"
          role="tab"
          :aria-selected="isRegister"
          class="mode-tab"
          :class="{ 'mode-tab--active': isRegister }"
          @click="switchMode('register')"
        >
          {{ t('auth.registerBtn') }}
        </button>
      </div>

      <form class="login-form" @submit.prevent="submit">
        <div class="field">
          <label class="field-label">{{ t('auth.username') }}</label>
          <input
            v-model="username"
            type="text"
            autocomplete="username"
            class="field-input"
            maxlength="32"
          />
        </div>

        <div class="field">
          <label class="field-label">{{ t('auth.password') }}</label>
          <input
            v-model="password"
            type="password"
            :autocomplete="isRegister ? 'new-password' : 'current-password'"
            class="field-input"
            maxlength="64"
          />
        </div>

        <Transition name="expand">
          <div v-if="isRegister" class="field">
            <label class="field-label">{{ t('auth.confirmPassword') }}</label>
            <input
              v-model="confirmPassword"
              type="password"
              autocomplete="new-password"
              class="field-input"
              maxlength="64"
            />
          </div>
        </Transition>

        <p v-if="errorMsg" class="form-error font-mono">{{ errorMsg }}</p>

        <button
          type="submit"
          class="submit-btn"
          :disabled="submitting"
        >
          <div v-if="submitting" class="spinner spinner--sm"></div>
          <span>{{ isRegister ? t('auth.registerBtn') : t('auth.loginBtn') }}</span>
        </button>
      </form>

      <button
        type="button"
        class="switch-link"
        @click="switchMode(isRegister ? 'login' : 'register')"
      >
        {{ isRegister ? t('auth.hasAccount') : t('auth.noAccount') }}
      </button>
    </div>
  </section>
</template>

<style scoped>
.login-page {
  position: relative;
  min-height: calc(100vh - 64px);
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 48px 24px;
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

.login-card {
  position: relative;
  z-index: 1;
  width: 100%;
  max-width: 420px;
  padding: 36px 34px 32px;
  border-radius: 14px;
  border: 1px solid rgba(197, 165, 114, 0.22);
  box-shadow: 0 24px 70px rgba(0, 0, 0, 0.5);
}

.login-header {
  text-align: center;
  margin-bottom: 26px;
}
.login-tag {
  font-size: 10px;
  letter-spacing: 0.32em;
  text-transform: uppercase;
  color: rgba(197, 165, 114, 0.8);
  margin-bottom: 12px;
}
.login-title {
  font-size: 2.2rem;
  color: #e8eaf6;
  font-weight: 500;
  line-height: 1.1;
}

/* 模式切换 */
.mode-tabs {
  display: flex;
  gap: 4px;
  padding: 4px;
  background: rgba(10, 14, 26, 0.5);
  border: 1px solid rgba(232, 234, 246, 0.08);
  border-radius: 8px;
  margin-bottom: 24px;
}
.mode-tab {
  flex: 1;
  padding: 9px 12px;
  border: none;
  background: transparent;
  border-radius: 6px;
  font-family: 'Manrope', sans-serif;
  font-size: 12px;
  letter-spacing: 0.12em;
  text-transform: uppercase;
  color: rgba(232, 234, 246, 0.55);
  cursor: pointer;
  transition: all 0.25s ease;
}
.mode-tab:hover {
  color: #e8eaf6;
}
.mode-tab--active {
  background: rgba(197, 165, 114, 0.14);
  color: #c5a572;
  box-shadow: inset 0 0 0 1px rgba(197, 165, 114, 0.4);
}

/* 表单 */
.login-form {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.field {
  display: flex;
  flex-direction: column;
  gap: 6px;
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
  padding: 11px 13px;
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

.form-error {
  font-size: 11px;
  letter-spacing: 0.06em;
  color: #b85a5a;
  padding: 9px 12px;
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
  margin-top: 6px;
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

.switch-link {
  display: block;
  width: 100%;
  margin-top: 20px;
  background: none;
  border: none;
  cursor: pointer;
  font-family: 'Manrope', sans-serif;
  font-size: 12px;
  letter-spacing: 0.08em;
  color: rgba(159, 168, 218, 0.7);
  text-align: center;
  transition: color 0.25s ease;
}
.switch-link:hover {
  color: #c5a572;
}

/* 确认密码展开动画 */
.expand-enter-active,
.expand-leave-active {
  transition: opacity 0.25s ease, transform 0.25s ease;
  overflow: hidden;
}
.expand-enter-from,
.expand-leave-to {
  opacity: 0;
  transform: translateY(-6px);
}

/* Spinner */
.spinner {
  width: 16px;
  height: 16px;
  border: 1.5px solid rgba(10, 14, 26, 0.25);
  border-top-color: #0a0e1a;
  border-radius: 50%;
  animation: spin 0.85s linear infinite;
}
.spinner--sm {
  width: 14px;
  height: 14px;
  border-width: 1.4px;
}
@keyframes spin {
  to { transform: rotate(360deg); }
}

@media (max-width: 480px) {
  .login-card {
    padding: 28px 22px 24px;
  }
  .login-title {
    font-size: 1.8rem;
  }
}
</style>
