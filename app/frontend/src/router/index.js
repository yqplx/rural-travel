import { createRouter, createWebHistory } from 'vue-router'
import { ElMessage } from 'element-plus'
import { auth } from '@/store/auth'

const routes = [
  {
    path: '/',
    component: () => import('@/layouts/MainLayout.vue'),
    children: [
      { path: '', name: 'Home', component: () => import('@/views/Home.vue') },
      { path: 'scenic', name: 'ScenicList', component: () => import('@/views/ScenicList.vue') },
      { path: 'scenic/:id', name: 'ScenicDetail', component: () => import('@/views/ScenicDetail.vue') },
      { path: 'recognize', name: 'PhotoRecognize', component: () => import('@/views/PhotoRecognize.vue') },
      // 方案三：县域文旅 AI 多模态导览
      { path: 'guide/graph', name: 'GuideGraph', component: () => import('@/views/GuideGraph.vue') },
      { path: 'guide/plan', name: 'GuidePlan', component: () => import('@/views/GuidePlan.vue') },
      { path: 'guide/live', name: 'GuideLive', component: () => import('@/views/GuideLive.vue') },
      { path: 'profile', name: 'Profile', component: () => import('@/views/Profile.vue'), meta: { requiresAuth: true } }
    ]
  },
  { path: '/login', name: 'Login', component: () => import('@/views/Login.vue') },
  { path: '/register', name: 'Register', component: () => import('@/views/Register.vue') },
  // 数据可视化大屏（独立全屏页，免登录，支持 ?screen=1|2|3 指定首屏）
  { path: '/bigscreen', name: 'BigScreen', component: () => import('@/views/bigscreen/BigScreenShell.vue') },
  {
    path: '/admin',
    component: () => import('@/layouts/AdminLayout.vue'),
    meta: { requiresAdmin: true },
    children: [
      { path: '', redirect: '/admin/analytics' },
      { path: 'analytics', name: 'AdminAnalytics', component: () => import('@/views/admin/AdminAnalytics.vue') },
      { path: 'scenic', name: 'AdminScenic', component: () => import('@/views/admin/AdminScenic.vue') },
      { path: 'dict', name: 'AdminDict', component: () => import('@/views/admin/AdminDict.vue') },
      { path: 'users', name: 'AdminUsers', component: () => import('@/views/admin/AdminUsers.vue') },
      { path: 'ratings', name: 'AdminRatings', component: () => import('@/views/admin/AdminRatings.vue') },
      { path: 'virtual', name: 'AdminVirtual', component: () => import('@/views/admin/AdminVirtual.vue') },
      { path: 'knowledge', name: 'AdminKnowledge', component: () => import('@/views/admin/AdminKnowledge.vue') },
      { path: 'content', name: 'AdminContent', component: () => import('@/views/admin/AdminContent.vue') },
      { path: 'logs', name: 'AdminLogs', component: () => import('@/views/admin/AdminLogs.vue') },
      // 方案三：县域文旅 AI 多模态导览
      { path: 'guide-kg', name: 'AdminGuideKg', component: () => import('@/views/admin/AdminGuideKg.vue') },
      { path: 'guide-content', name: 'AdminGuideContent', component: () => import('@/views/admin/AdminGuideContent.vue') },
      { path: 'guide-metrics', name: 'AdminGuideMetrics', component: () => import('@/views/admin/AdminGuideMetrics.vue') }
    ]
  },
  { path: '/:pathMatch(.*)*', redirect: '/' }
]

const router = createRouter({ history: createWebHistory(), routes })

router.beforeEach((to, from, next) => {
  if (to.meta.requiresAdmin) {
    if (!auth.isLogin) return next({ path: '/login', query: { redirect: to.fullPath } })
    if (!auth.isAdmin) {
      ElMessage.error('没有权限访问管理后台')
      return next('/')
    }
  }
  if (to.meta.requiresAuth && !auth.isLogin) {
    return next({ path: '/login', query: { redirect: to.fullPath } })
  }
  next()
})

export default router
