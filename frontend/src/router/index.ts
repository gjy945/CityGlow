import { createRouter, createWebHistory } from 'vue-router'
import { useAuthStore } from '../stores/auth'

const routes = [
  { path: '/', name: 'home', component: () => import('../views/DarkSkyMap.vue') },
  { path: '/forecast', name: 'forecast', component: () => import('../views/ForecastPanel.vue') },
  { path: '/timeline', name: 'timeline', component: () => import('../views/AstroTimeline.vue') },
  { path: '/sky', name: 'sky', component: () => import('../views/SkyAtlas.vue') },
  {
    path: '/postcard',
    name: 'postcard',
    component: () => import('../views/StarryPostcard.vue'),
    meta: { requiresAuth: true },
  },
  { path: '/postcard/:id', name: 'postcard-detail', component: () => import('../views/StarryPostcard.vue') },
  {
    path: '/favorites',
    name: 'favorites',
    component: () => import('../views/Favorites.vue'),
    meta: { requiresAuth: true },
  },
  { path: '/login', name: 'login', component: () => import('../views/Login.vue') },
  { path: '/:pathMatch(.*)*', name: 'not-found', component: () => import('../views/NotFound.vue') },
]

const router = createRouter({
  history: createWebHistory(),
  routes,
  scrollBehavior() { return { top: 0 } },
})

// 全局前置守卫:requiresAuth 路由未登录则跳转登录页(带 redirect 回跳)
router.beforeEach((to) => {
  if (to.meta.requiresAuth) {
    const auth = useAuthStore()
    if (!auth.isLoggedIn) {
      return { name: 'login', query: { redirect: to.fullPath } }
    }
  }
  return true
})

export default router
