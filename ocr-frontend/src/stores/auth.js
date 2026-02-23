import { defineStore } from 'pinia'
import { ref } from 'vue'
import api from '../api/http'

export const useAuthStore = defineStore('auth', () => {
  const token = ref(localStorage.getItem('ocr_token') || '')
  const tenantId = ref(localStorage.getItem('ocr_tenantId') || '')
  const userId = ref(localStorage.getItem('ocr_userId') || '')
  const userName = ref(localStorage.getItem('ocr_userName') || '')

  async function login(username, password) {
    const res = await api.post('/api/auth/login', { username, password })
    if (res.data.code === 200) {
      const d = res.data.data
      token.value = d.token
      tenantId.value = d.tenantId
      userId.value = d.userId
      userName.value = d.userName
      localStorage.setItem('ocr_token', d.token)
      localStorage.setItem('ocr_tenantId', d.tenantId)
      localStorage.setItem('ocr_userId', d.userId)
      localStorage.setItem('ocr_userName', d.userName)
      return true
    }
    return false
  }

  function logout() {
    token.value = ''
    tenantId.value = ''
    userId.value = ''
    userName.value = ''
    localStorage.removeItem('ocr_token')
    localStorage.removeItem('ocr_tenantId')
    localStorage.removeItem('ocr_userId')
    localStorage.removeItem('ocr_userName')
  }

  return { token, tenantId, userId, userName, login, logout }
})
