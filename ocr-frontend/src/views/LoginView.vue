<template>
  <div style="display: flex; justify-content: center; align-items: center; height: 100vh; background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);">
    <el-card style="width: 400px; border-radius: 12px;">
      <template #header>
        <div style="text-align: center; font-size: 22px; font-weight: bold; color: #303133;">
          OCR智能识别系统
        </div>
        <div style="text-align: center; font-size: 13px; color: #909399; margin-top: 6px;">测试DEMO</div>
      </template>
      <el-form :model="form" @submit.prevent="handleLogin" label-width="0">
        <el-form-item>
          <el-input v-model="form.username" placeholder="用户名" prefix-icon="User" size="large" />
        </el-form-item>
        <el-form-item>
          <el-input v-model="form.password" placeholder="密码" type="password" prefix-icon="Lock" size="large" show-password />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" size="large" style="width: 100%;" :loading="loading" @click="handleLogin">
            登 录
          </el-button>
        </el-form-item>
      </el-form>
      <div style="color: #909399; font-size: 12px;">
        <div>测试账号：admin / 123456 (租户T001)</div>
        <div>测试账号：admin2 / 123456 (租户T002)</div>
      </div>
    </el-card>
  </div>
</template>

<script setup>
import { reactive, ref } from 'vue'
import { useAuthStore } from '../stores/auth'
import { ElMessage } from 'element-plus'

const authStore = useAuthStore()
const loading = ref(false)
const form = reactive({ username: 'admin', password: '123456' })

const handleLogin = async () => {
  loading.value = true
  try {
    const ok = await authStore.login(form.username, form.password)
    if (ok) {
      ElMessage.success('登录成功')
    } else {
      ElMessage.error('用户名或密码错误')
    }
  } catch (e) {
    ElMessage.error('登录失败')
  } finally {
    loading.value = false
  }
}
</script>
