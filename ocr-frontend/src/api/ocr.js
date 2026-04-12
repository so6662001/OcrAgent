import http from './http'

export const ocrApi = {
  recognize: (formData) => http.post('/api/ocr/recognize', formData, {
    headers: { 'Content-Type': 'multipart/form-data' }
  }),

  getTasks: (params) => http.get('/api/ocr/tasks', { params }),
  getTask: (taskId) => http.get(`/api/ocr/task/${taskId}`),
  getTaskFiles: (taskId, params) => http.get(`/api/ocr/task/${taskId}/files`, { params }),

  getFiles: (params) => http.get('/api/ocr/files', { params }),
  getFile: (fileId) => http.get(`/api/ocr/file/${fileId}`),
  getRawJson: (fileId) => http.get(`/api/ocr/file/${fileId}/raw-json`),
  getReview: (fileId) => http.get(`/api/ocr/file/${fileId}/review`),
  confirmReview: (fileId, data) => http.post(`/api/ocr/file/${fileId}/confirm`, data),

  getDocTypes: () => http.get('/api/ocr/config/doc-types'),
  createDocType: (data) => http.post('/api/ocr/config/doc-type', data),
  updateDocType: (id, data) => http.put(`/api/ocr/config/doc-type/${id}`, data),
  deleteDocType: (id) => http.delete(`/api/ocr/config/doc-type/${id}`),
  getFields: (docTypeId) => http.get(`/api/ocr/config/doc-type/${docTypeId}/fields`),
  createField: (data) => http.post('/api/ocr/config/field', data),
  updateField: (id, data) => http.put(`/api/ocr/config/field/${id}`, data),
  deleteField: (id) => http.delete(`/api/ocr/config/field/${id}`),

  getThresholds: () => http.get('/api/ocr/config/threshold'),
  setThreshold: (docTypeId, threshold) => http.put(`/api/ocr/config/threshold/${docTypeId}`, { threshold }),
  resetThreshold: (docTypeId) => http.delete(`/api/ocr/config/threshold/${docTypeId}`),

  getSuppliers: (keyword) => http.get('/api/ocr/suppliers', { params: { keyword } }),
  syncSuppliers: (data) => http.post('/api/ocr/supplier/sync', data)
}
