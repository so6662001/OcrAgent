import axios from 'axios'
import { ElMessage } from 'element-plus'

const http = axios.create({
  timeout: 60000
})

http.interceptors.request.use(config => {
  const token = localStorage.getItem('ocr_token')
  if (token) {
    config.headers.Authorization = `Bearer ${token}`
  }
  return config
})

http.interceptors.response.use(
  response => response,
  error => {
    if (error.response?.status === 401 || error.response?.status === 403) {
      localStorage.removeItem('ocr_token')
      localStorage.removeItem('ocr_tenantId')
      localStorage.removeItem('ocr_userId')
      localStorage.removeItem('ocr_userName')
      ElMessage.error('登录已过期，请重新登录')
      setTimeout(() => window.location.reload(), 1000)
    } else {
      const msg = error.response?.data?.message || error.message || '请求失败'
      ElMessage.error(msg)
    }
    return Promise.reject(error)
  }
)

export default http
