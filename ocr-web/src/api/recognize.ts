import axios from 'axios'
import type { ApiResult, BatchUploadResult, RecognizeTask, RecognizeMode } from '../types/recognize'

const api = axios.create({
  baseURL: import.meta.env.VITE_API_BASE || '/api/v1',
  timeout: 60000
})

export function uploadFiles(files: File[], mode: RecognizeMode = 'AUTO'): Promise<ApiResult<BatchUploadResult>> {
  const formData = new FormData()
  files.forEach(f => formData.append('files', f))
  formData.append('mode', mode)
  return api.post('/recognize/upload', formData, {
    headers: { 'Content-Type': 'multipart/form-data' }
  }).then(r => r.data)
}

export function getTask(taskId: string): Promise<ApiResult<RecognizeTask>> {
  return api.get(`/recognize/task/${taskId}`).then(r => r.data)
}

export function getAllTasks(): Promise<ApiResult<RecognizeTask[]>> {
  return api.get('/recognize/tasks').then(r => r.data)
}

export function confirmCorrection(data: {
  taskId: string
  fields?: any[]
  tableData?: any[]
}): Promise<ApiResult<RecognizeTask>> {
  return api.post('/recognize/confirm', data).then(r => r.data)
}
