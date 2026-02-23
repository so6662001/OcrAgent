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
    if (error.response?.status === 401) {
      localStorage.removeItem('ocr_token')
      window.location.reload()
    } else {
      ElMessage.error(error.response?.data?.message || '请求失败')
    }
    return Promise.reject(error)
  }
)

export default http
