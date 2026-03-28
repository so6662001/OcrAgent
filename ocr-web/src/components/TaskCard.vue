<script setup lang="ts">
import { computed } from 'vue'
import type { RecognizeTask } from '../types/recognize'

const props = defineProps<{
  task: RecognizeTask
  active: boolean
}>()

const emit = defineEmits<{
  (e: 'select'): void
}>()

const statusInfo = computed(() => {
  switch (props.task.status) {
    case 'QUEUED': return { label: '排队中', cls: 'queued' }
    case 'PROCESSING': return { label: '识别中', cls: 'processing' }
    case 'COMPLETED': return { label: '已完成', cls: 'completed' }
    case 'FAILED': return { label: '失败', cls: 'failed' }
    default: return { label: '', cls: '' }
  }
})
</script>

<template>
  <div class="task-card" :class="{ active }" @click="emit('select')">
    <div class="task-info">
      <span class="task-name">{{ task.fileName }}</span>
      <span class="task-badge" :class="statusInfo.cls">{{ statusInfo.label }}</span>
    </div>
    <div v-if="task.status === 'PROCESSING'" class="progress-bar">
      <div class="progress-inner"></div>
    </div>
    <div v-if="task.status === 'COMPLETED'" class="task-meta">
      置信度 {{ (task.confidence * 100).toFixed(1) }}%
      <span v-if="task.processTimeMs"> · {{ task.processTimeMs }}ms</span>
    </div>
    <div v-if="task.status === 'FAILED'" class="task-error">
      {{ task.errorMessage }}
    </div>
  </div>
</template>

<style scoped>
.task-card {
  padding: 12px 14px;
  background: var(--card);
  border-radius: var(--radius-sm);
  border: 1.5px solid var(--border);
  cursor: pointer;
  transition: all 0.15s;
}

.task-card.active {
  border-color: var(--primary);
  box-shadow: 0 0 0 2px var(--primary-light);
}

.task-info {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 8px;
}

.task-name {
  font-size: 14px;
  font-weight: 500;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  flex: 1;
}

.task-badge {
  font-size: 12px;
  padding: 2px 8px;
  border-radius: 10px;
  white-space: nowrap;
  font-weight: 500;
}

.task-badge.queued { background: #f3f4f6; color: #6b7280; }
.task-badge.processing { background: #fef3c7; color: #d97706; }
.task-badge.completed { background: #d1fae5; color: #059669; }
.task-badge.failed { background: #fee2e2; color: #dc2626; }

.progress-bar {
  margin-top: 8px;
  height: 3px;
  background: #e5e7eb;
  border-radius: 2px;
  overflow: hidden;
}

.progress-inner {
  height: 100%;
  width: 60%;
  background: var(--primary);
  border-radius: 2px;
  animation: loading 1.5s ease-in-out infinite;
}

@keyframes loading {
  0% { width: 10%; margin-left: 0; }
  50% { width: 60%; margin-left: 20%; }
  100% { width: 10%; margin-left: 90%; }
}

.task-meta {
  margin-top: 6px;
  font-size: 12px;
  color: var(--text-secondary);
}

.task-error {
  margin-top: 6px;
  font-size: 12px;
  color: var(--danger);
}
</style>
