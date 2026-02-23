import { createRouter, createWebHistory } from 'vue-router'

const routes = [
  { path: '/', redirect: '/upload' },
  { path: '/upload', component: () => import('../views/upload/UploadView.vue') },
  { path: '/tasks', component: () => import('../views/task/TaskListView.vue') },
  { path: '/task/:taskId/files', component: () => import('../views/file/FileListView.vue'), props: true },
  { path: '/records', component: () => import('../views/record/RecordListView.vue') },
  { path: '/review', component: () => import('../views/review/ReviewListView.vue') },
  { path: '/review/:fileId', component: () => import('../views/review/ReviewDetailView.vue'), props: true },
  { path: '/config', component: () => import('../views/config/ConfigView.vue') }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

export default router
