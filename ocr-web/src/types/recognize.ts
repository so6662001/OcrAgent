export type TaskStatus = 'QUEUED' | 'PROCESSING' | 'COMPLETED' | 'FAILED'
export type RecognizeMode = 'AUTO' | 'GENERAL' | 'HANDWRITING' | 'TEMPLATE'

export interface FieldVO {
  standardKey: string
  displayName: string
  originalKey: string
  value: any
  confidence: number
  valueType: string
}

export interface RecognizeTask {
  taskId: string
  fileName: string
  status: TaskStatus
  errorMessage?: string
  confidence: number
  ocrApi?: string
  fields?: FieldVO[]
  tableData?: Record<string, FieldVO>[]
  rawText?: string
  processTimeMs?: number
}

export interface BatchUploadResult {
  totalFiles: number
  taskIds: string[]
}

export interface ApiResult<T> {
  code: number
  message: string
  data: T
}
