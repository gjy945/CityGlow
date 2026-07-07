import { createRouter, createWebHistory } from 'vue-router'

const routes = [
  { path: '/', name: 'home', component: () => import('../views/DarkSkyMap.vue') },
  { path: '/forecast', name: 'forecast', component: () => import('../views/ForecastPanel.vue') },
  { path: '/timeline', name: 'timeline', component: () => import('../views/AstroTimeline.vue') },
  { path: '/postcard', name: 'postcard', component: () => import('../views/StarryPostcard.vue') },
  { path: '/postcard/:id', name: 'postcard-detail', component: () => import('../views/StarryPostcard.vue') },
  { path: '/:pathMatch(.*)*', name: 'not-found', component: () => import('../views/NotFound.vue') },
]

export default createRouter({
  history: createWebHistory(),
  routes,
  scrollBehavior() { return { top: 0 } },
})
