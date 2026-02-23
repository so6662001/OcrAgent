<template>
  <div id="app">
    <template v-if="authStore.token">
      <el-container style="height: 100vh">
        <el-aside width="220px" style="background: #304156">
          <div style="height: 60px; display: flex; align-items: center; justify-content: center; color: #fff; font-size: 18px; font-weight: bold;">
            OCR智能识别
          </div>
          <el-menu
            :default-active="$route.path"
            router
            background-color="#304156"
            text-color="#bfcbd9"
            active-text-color="#409eff"
          >
            <el-menu-item index="/upload">
              <el-icon><Upload /></el-icon>
              <span>文件识别</span>
            </el-menu-item>
            <el-menu-item index="/tasks">
              <el-icon><List /></el-icon>
              <span>任务查询</span>
            </el-menu-item>
            <el-menu-item index="/records">
              <el-icon><Search /></el-icon>
              <span>识别记录</span>
            </el-menu-item>
            <el-menu-item index="/review">
              <el-icon><Edit /></el-icon>
              <span>人工审核</span>
            </el-menu-item>
            <el-menu-item index="/config">
              <el-icon><Setting /></el-icon>
              <span>系统配置</span>
            </el-menu-item>
          </el-menu>
        </el-aside>
        <el-container>
          <el-header style="display: flex; align-items: center; justify-content: space-between; border-bottom: 1px solid #eee;">
            <span style="font-size: 14px; color: #666;">{{ authStore.userName }} ({{ authStore.tenantId }})</span>
            <el-button type="danger" size="small" @click="logout">退出登录</el-button>
          </el-header>
          <el-main style="background: #f5f7fa; padding: 20px;">
            <router-view />
          </el-main>
        </el-container>
      </el-container>
    </template>
    <template v-else>
      <LoginView />
    </template>
  </div>
</template>

<script setup>
import { useAuthStore } from './stores/auth'
import LoginView from './views/LoginView.vue'

const authStore = useAuthStore()

const logout = () => {
  authStore.logout()
}
</script>

<style>
body { margin: 0; font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif; }
</style>
