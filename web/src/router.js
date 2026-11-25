import { createRouter, createWebHashHistory } from 'vue-router'
import { restoreSession, session } from './session'
import HomeView from './views/HomeView.vue'

const router = createRouter({
  history: createWebHashHistory(),
  scrollBehavior(to, from, savedPosition) {
    if (savedPosition) return savedPosition
    if (to.hash) return { el: to.hash, behavior: 'smooth' }
    return { top: 0 }
  },
  routes: [
    {
      path: '/',
      name: 'home',
      component: HomeView
    },
    {
      path: '/goods/:id',
      name: 'goods-detail',
      component: () => import('./views/GoodsDetailView.vue')
    },
    {
      path: '/login',
      name: 'login',
      component: () => import('./views/AuthView.vue')
    },
    {
      path: '/publish',
      name: 'publish',
      component: () => import('./views/PublishView.vue'),
      meta: { requiresAuth: true }
    },
    {
      path: '/orders',
      name: 'orders',
      component: () => import('./views/OrdersView.vue'),
      meta: { requiresAuth: true }
    },
    {
      path: '/my-goods',
      name: 'my-goods',
      component: () => import('./views/MyGoodsView.vue'),
      meta: { requiresAuth: true }
    },
    {
      path: '/:pathMatch(.*)*',
      name: 'not-found',
      component: () => import('./views/NotFoundView.vue')
    }
  ]
})

router.beforeEach(async (to) => {
  await restoreSession()
  if (to.meta.requiresAuth && !session.user) {
    return {
      name: 'login',
      query: { redirect: to.fullPath }
    }
  }
  return true
})

export default router
