import { createRouter, createWebHistory } from 'vue-router'
import type { RouteRecordRaw } from 'vue-router'
import { useAuthStore } from '@/stores/auth'

const routes: RouteRecordRaw[] = [
  {
    path: '/',
    redirect: '/resume/write'
  },
  {
    path: '/login',
    name: 'Login',
    component: () => import('@/views/Login.vue'),
    meta: { requiresAuth: false }
  },
  {
    path: '/register',
    name: 'Register',
    component: () => import('@/views/Register.vue'),
    meta: { requiresAuth: false }
  },
  {
    path: '/',
    component: () => import('@/layouts/MainLayout.vue'),
    meta: { requiresAuth: true },
    children: [
      {
        path: 'resume/write',
        name: 'ResumeWriter',
        component: () => import('@/views/ResumeWriter.vue'),
        meta: { title: '简历编写' }
      },
      {
        path: 'resume/optimize',
        name: 'ResumeOptimizer',
        component: () => import('@/views/ResumeOptimizer.vue'),
        meta: { title: '简历优化' }
      },
      {
        path: 'jd-match',
        name: 'JDMatcher',
        component: () => import('@/views/JDMatcher.vue'),
        meta: { title: 'JD匹配度' }
      },
      {
        path: 'interview',
        name: 'MockInterview',
        component: () => import('@/views/MockInterview.vue'),
        meta: { title: '模拟面试' }
      },
      {
        path: 'profile',
        name: 'Profile',
        component: () => import('@/views/Profile.vue'),
        meta: { title: '用户画像' }
      }
    ]
  }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

// 路由守卫
router.beforeEach((to, _from, next) => {
  const authStore = useAuthStore()

  if (to.matched.some(record => record.meta.requiresAuth !== false)) {
    if (!authStore.isAuthenticated) {
      next({ path: '/login', query: { redirect: to.fullPath } })
      return
    }
  }

  next()
})

export default router
