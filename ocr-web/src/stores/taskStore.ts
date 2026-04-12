import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import type { RecognizeTask, RecognizeMode } from '../types/recognize'
import { uploadFiles, getTask } from '../api/recognize'

export const useTaskStore = defineStore('task', () => {
  const tasks = ref<RecognizeTask[]>([])
  const pollingTimers = ref<Record<string, number>>({})

  const pendingCount = computed(() =>
    tasks.value.filter(t => t.status === 'QUEUED' || t.status === 'PROCESSING').length
  )

  const completedCount = computed(() =>
    tasks.value.filter(t => t.status === 'COMPLETED').length
  )

  async function submitFiles(files: File[], mode: RecognizeMode) {
    const res = await uploadFiles(files, mode)
    if (res.code === 200 && res.data) {
      for (let i = 0; i < res.data.taskIds.length; i++) {
        const task: RecognizeTask = {
          taskId: res.data.taskIds[i],
          fileName: files[i]?.name || `file_${i}`,
          status: 'QUEUED',
          confidence: 0
        }
        tasks.value.push(task)
        startPolling(task.taskId)
      }
    }
    return res
  }

  function startPolling(taskId: string) {
    if (pollingTimers.value[taskId]) return
    const timer = window.setInterval(async () => {
      try {
        const res = await getTask(taskId)
        if (res.code === 200 && res.data) {
          const idx = tasks.value.findIndex(t => t.taskId === taskId)
          if (idx >= 0) {
            tasks.value[idx] = res.data
          }
          if (res.data.status === 'COMPLETED' || res.data.status === 'FAILED') {
            stopPolling(taskId)
          }
        }
      } catch {
        // retry on next interval
      }
    }, 1500)
    pollingTimers.value[taskId] = timer
  }

  function stopPolling(taskId: string) {
    if (pollingTimers.value[taskId]) {
      clearInterval(pollingTimers.value[taskId])
      delete pollingTimers.value[taskId]
    }
  }

  function clearTasks() {
    Object.keys(pollingTimers.value).forEach(stopPolling)
    tasks.value = []
  }

  return { tasks, pendingCount, completedCount, submitFiles, clearTasks }
})
