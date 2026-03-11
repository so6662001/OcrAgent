import { createRouter, createWebHistory } from 'vue-router'

const routes = [
  { path: '/', redirect: '/upload' },
  { path: '/upload', component: () => import('../views/upload/UploadView.vue'), meta: { auth: true } },
  { path: '/tasks', component: () => import('../views/task/TaskListView.vue'), meta: { auth: true } },
  { path: '/task/:taskId/files', component: () => import('../views/file/FileListView.vue'), props: true, meta: { auth: true } },
  { path: '/records', component: () => import('../views/record/RecordListView.vue'), meta: { auth: true } },
  { path: '/review', component: () => import('../views/review/ReviewListView.vue'), meta: { auth: true } },
  { path: '/review/:fileId', component: () => import('../views/review/ReviewDetailView.vue'), props: true, meta: { auth: true } },
  { path: '/config', component: () => import('../views/config/ConfigView.vue'), meta: { auth: true } }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

router.beforeEach((to, from, next) => {
  const token = localStorage.getItem('ocr_token')
  if (to.meta.auth && !token) {
    next('/')
    return
  }
  next()
})

export default router
