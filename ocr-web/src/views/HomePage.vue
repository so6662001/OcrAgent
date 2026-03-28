<script setup lang="ts">
import { ref, computed } from 'vue'
import FileUploader from '../components/FileUploader.vue'
import TaskCard from '../components/TaskCard.vue'
import ResultEditor from '../components/ResultEditor.vue'
import { useTaskStore } from '../stores/taskStore'
import type { RecognizeMode } from '../types/recognize'

const store = useTaskStore()
const mode = ref<RecognizeMode>('AUTO')
const activeTaskId = ref<string | null>(null)

const activeTask = computed(() => {
  if (!activeTaskId.value) return null
  return store.tasks.find(t => t.taskId === activeTaskId.value) || null
})

const showPanel = ref(false)

async function onFilesSelected(files: File[]) {
  if (!files.length) return
  await store.submitFiles(files, mode.value)
  if (store.tasks.length && !activeTaskId.value) {
    activeTaskId.value = store.tasks[store.tasks.length - 1].taskId
  }
}

function selectTask(taskId: string) {
  activeTaskId.value = taskId
  showPanel.value = true
}

function backToList() {
  showPanel.value = false
}
</script>

<template>
  <div class="page">
    <!-- Header -->
    <header class="header">
      <h1 class="title">单据识别</h1>
      <div class="header-right">
        <select v-model="mode" class="mode-select">
          <option value="AUTO">自动识别</option>
          <option value="GENERAL">印刷体</option>
          <option value="HANDWRITING">手写体</option>
        </select>
      </div>
    </header>

    <!-- Desktop layout -->
    <div class="desktop-layout">
      <aside class="sidebar">
        <FileUploader @files-selected="onFilesSelected" />

        <div v-if="store.tasks.length" class="task-summary">
          <span>{{ store.completedCount }}/{{ store.tasks.length }} 已完成</span>
          <button v-if="store.tasks.length" class="btn-clear" @click="store.clearTasks(); activeTaskId = null">
            清空
          </button>
        </div>

        <div class="task-list">
          <TaskCard
            v-for="task in store.tasks"
            :key="task.taskId"
            :task="task"
            :active="task.taskId === activeTaskId"
            @select="selectTask(task.taskId)"
          />
        </div>
      </aside>

      <main class="main-content">
        <ResultEditor
          v-if="activeTask"
          :task="activeTask"
          @confirmed="() => {}"
        />
        <div v-else class="empty-state">
          <svg width="48" height="48" viewBox="0 0 24 24" fill="none" stroke="#d1d5db" stroke-width="1.5">
            <path d="M14 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V8z"/>
            <polyline points="14 2 14 8 20 8"/>
            <line x1="16" y1="13" x2="8" y2="13"/>
            <line x1="16" y1="17" x2="8" y2="17"/>
            <polyline points="10 9 9 9 8 9"/>
          </svg>
          <p>上传单据文件开始识别</p>
        </div>
      </main>
    </div>

    <!-- Mobile layout -->
    <div class="mobile-layout">
      <div v-show="!showPanel" class="mobile-main">
        <FileUploader @files-selected="onFilesSelected" />

        <div v-if="store.tasks.length" class="task-summary">
          <span>{{ store.completedCount }}/{{ store.tasks.length }} 已完成</span>
          <button class="btn-clear" @click="store.clearTasks(); activeTaskId = null">清空</button>
        </div>

        <div class="task-list">
          <TaskCard
            v-for="task in store.tasks"
            :key="task.taskId"
            :task="task"
            :active="task.taskId === activeTaskId"
            @select="selectTask(task.taskId)"
          />
        </div>
      </div>

      <div v-show="showPanel" class="mobile-panel">
        <button class="btn-back" @click="backToList">
          <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
            <polyline points="15 18 9 12 15 6"/>
          </svg>
          返回列表
        </button>
        <ResultEditor
          v-if="activeTask"
          :task="activeTask"
          @confirmed="() => {}"
        />
      </div>
    </div>
  </div>
</template>

<style scoped>
.page {
  min-height: 100vh;
  max-width: 1200px;
  margin: 0 auto;
}

.header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 12px 16px;
  background: var(--card);
  border-bottom: 1px solid var(--border);
  position: sticky;
  top: 0;
  z-index: 100;
}

.title {
  font-size: 18px;
  font-weight: 600;
}

.mode-select {
  padding: 6px 10px;
  border: 1px solid var(--border);
  border-radius: 6px;
  font-size: 13px;
  background: var(--card);
  color: var(--text);
}

/* Desktop */
.desktop-layout {
  display: none;
  gap: 16px;
  padding: 16px;
  align-items: flex-start;
}

.sidebar {
  width: 320px;
  flex-shrink: 0;
  display: flex;
  flex-direction: column;
  gap: 12px;
  position: sticky;
  top: 60px;
  max-height: calc(100vh - 76px);
  overflow-y: auto;
}

.main-content {
  flex: 1;
  min-width: 0;
}

/* Mobile */
.mobile-layout {
  display: block;
  padding: 12px;
}

.mobile-main {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.mobile-panel {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.btn-back {
  display: flex;
  align-items: center;
  gap: 4px;
  padding: 8px 0;
  background: none;
  color: var(--primary);
  font-size: 14px;
  font-weight: 500;
}

/* Shared */
.task-summary {
  display: flex;
  justify-content: space-between;
  align-items: center;
  font-size: 13px;
  color: var(--text-secondary);
  padding: 0 2px;
}

.btn-clear {
  padding: 4px 10px;
  font-size: 12px;
  color: var(--danger);
  background: #fff0f0;
  border-radius: 6px;
}

.task-list {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.empty-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 60px 20px;
  color: #d1d5db;
  gap: 12px;
  font-size: 14px;
}

/* Responsive breakpoint */
@media (min-width: 768px) {
  .desktop-layout {
    display: flex;
  }
  .mobile-layout {
    display: none;
  }
}
</style>
