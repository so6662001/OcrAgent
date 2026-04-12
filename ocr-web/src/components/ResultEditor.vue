<script setup lang="ts">
import { ref, computed, watch } from 'vue'
import type { RecognizeTask, FieldVO } from '../types/recognize'
import { confirmCorrection } from '../api/recognize'

const props = defineProps<{
  task: RecognizeTask
}>()

const emit = defineEmits<{
  (e: 'confirmed', json: any): void
}>()

const editFields = ref<FieldVO[]>([])
const editTable = ref<Record<string, FieldVO>[]>([])
const confirmed = ref(false)
const confirmedJson = ref<any>(null)
const submitting = ref(false)

watch(() => props.task, (t) => {
  if (t.fields) {
    editFields.value = JSON.parse(JSON.stringify(t.fields))
  }
  if (t.tableData) {
    editTable.value = JSON.parse(JSON.stringify(t.tableData))
  }
  confirmed.value = false
  confirmedJson.value = null
}, { immediate: true, deep: true })

const hasTableData = computed(() => editTable.value.length > 0)

const tableHeaders = computed(() => {
  if (!editTable.value.length) return []
  return Object.keys(editTable.value[0])
})

async function handleConfirm() {
  submitting.value = true
  try {
    const payload = {
      taskId: props.task.taskId,
      fields: editFields.value,
      tableData: editTable.value
    }
    const res = await confirmCorrection(payload)
    if (res.code === 200) {
      confirmed.value = true
      confirmedJson.value = buildOutputJson()
      emit('confirmed', confirmedJson.value)
    }
  } catch (err) {
    console.warn('提交纠错请求失败，使用本地数据', err)
    confirmed.value = true
    confirmedJson.value = buildOutputJson()
    emit('confirmed', confirmedJson.value)
  } finally {
    submitting.value = false
  }
}

function buildOutputJson() {
  const result: any = {
    taskId: props.task.taskId,
    fileName: props.task.fileName,
    confidence: props.task.confidence,
    ocrApi: props.task.ocrApi,
    fields: {}
  }

  for (const f of editFields.value) {
    result.fields[f.standardKey] = {
      displayName: f.displayName,
      value: f.value,
      originalKey: f.originalKey,
      confidence: f.confidence,
      valueType: f.valueType
    }
  }

  if (editTable.value.length) {
    result.tableData = editTable.value.map((row, i) => {
      const rowData: any = { rowIndex: i + 1 }
      for (const [key, cell] of Object.entries(row)) {
        rowData[key] = {
          displayName: cell.displayName,
          value: cell.value,
          originalKey: cell.originalKey
        }
      }
      return rowData
    })
  }

  return result
}

const copySuccess = ref(false)

function copyJson() {
  const text = JSON.stringify(confirmedJson.value, null, 2)
  if (navigator.clipboard && window.isSecureContext) {
    navigator.clipboard.writeText(text).then(() => showCopyTip())
  } else {
    const textarea = document.createElement('textarea')
    textarea.value = text
    textarea.style.position = 'fixed'
    textarea.style.opacity = '0'
    document.body.appendChild(textarea)
    textarea.select()
    try {
      document.execCommand('copy')
      showCopyTip()
    } catch { /* ignore */ }
    document.body.removeChild(textarea)
  }
}

function showCopyTip() {
  copySuccess.value = true
  setTimeout(() => { copySuccess.value = false }, 1500)
}
</script>

<template>
  <div class="editor" v-if="task.status === 'COMPLETED'">
    <!-- 字段编辑 -->
    <div v-if="!confirmed" class="section">
      <h3 class="section-title">识别字段</h3>
      <div class="field-list">
        <div v-for="(field, idx) in editFields" :key="idx" class="field-row">
          <label class="field-label">
            <span class="field-original" v-if="field.originalKey !== field.displayName">
              {{ field.originalKey }} →
            </span>
            {{ field.displayName }}
          </label>
          <input
            class="field-input"
            v-model="field.value"
            :placeholder="field.displayName"
          />
          <span class="field-conf" :class="{ low: field.confidence < 0.8 }">
            {{ (field.confidence * 100).toFixed(0) }}%
          </span>
        </div>
      </div>

      <!-- 表格数据编辑 -->
      <div v-if="hasTableData" class="table-section">
        <h3 class="section-title">表格数据</h3>
        <div class="table-wrap">
          <table class="data-table">
            <thead>
              <tr>
                <th v-for="h in tableHeaders" :key="h">
                  {{ editTable[0][h]?.displayName || h }}
                </th>
              </tr>
            </thead>
            <tbody>
              <tr v-for="(row, ri) in editTable" :key="ri">
                <td v-for="h in tableHeaders" :key="h">
                  <input
                    class="cell-input"
                    v-model="row[h].value"
                  />
                </td>
              </tr>
            </tbody>
          </table>
        </div>
      </div>

      <!-- 原始文本 -->
      <details class="raw-section" v-if="task.rawText">
        <summary class="raw-toggle">查看原始文本</summary>
        <pre class="raw-text">{{ task.rawText }}</pre>
      </details>

      <button class="btn-confirm" @click="handleConfirm" :disabled="submitting">
        {{ submitting ? '提交中...' : '确认无误' }}
      </button>
    </div>

    <!-- 确认后显示JSON -->
    <div v-else class="section">
      <div class="json-header">
        <h3 class="section-title">结构化JSON结果</h3>
        <button class="btn-copy" @click="copyJson">{{ copySuccess ? '已复制' : '复制' }}</button>
      </div>
      <pre class="json-output">{{ JSON.stringify(confirmedJson, null, 2) }}</pre>
    </div>
  </div>

  <div v-else-if="task.status === 'PROCESSING'" class="editor-placeholder">
    <div class="spinner"></div>
    <p>正在识别中，请稍候...</p>
  </div>

  <div v-else-if="task.status === 'QUEUED'" class="editor-placeholder">
    <p>等待处理中...</p>
  </div>

  <div v-else-if="task.status === 'FAILED'" class="editor-placeholder error">
    <p>识别失败</p>
    <p class="err-msg">{{ task.errorMessage }}</p>
  </div>
</template>

<style scoped>
.editor {
  width: 100%;
}

.section {
  background: var(--card);
  border-radius: var(--radius);
  padding: 16px;
}

.section-title {
  font-size: 15px;
  font-weight: 600;
  margin-bottom: 12px;
  color: var(--text);
}

.field-list {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.field-row {
  display: flex;
  align-items: center;
  gap: 8px;
}

.field-label {
  min-width: 80px;
  font-size: 13px;
  color: var(--text-secondary);
  flex-shrink: 0;
}

.field-original {
  color: #9ca3af;
  font-size: 11px;
}

.field-input {
  flex: 1;
  border: 1px solid var(--border);
  border-radius: 6px;
  padding: 6px 10px;
  font-size: 14px;
  background: #fafafa;
  transition: border-color 0.15s;
}

.field-input:focus {
  border-color: var(--primary);
  background: #fff;
}

.field-conf {
  font-size: 11px;
  color: var(--success);
  min-width: 32px;
  text-align: right;
}

.field-conf.low {
  color: var(--warning);
}

.table-section {
  margin-top: 16px;
}

.table-wrap {
  overflow-x: auto;
  -webkit-overflow-scrolling: touch;
}

.data-table {
  width: 100%;
  border-collapse: collapse;
  font-size: 13px;
}

.data-table th {
  background: #f9fafb;
  padding: 8px 6px;
  text-align: left;
  font-weight: 500;
  color: var(--text-secondary);
  border-bottom: 1px solid var(--border);
  white-space: nowrap;
}

.data-table td {
  padding: 4px;
  border-bottom: 1px solid #f3f4f6;
}

.cell-input {
  width: 100%;
  border: 1px solid transparent;
  border-radius: 4px;
  padding: 4px 6px;
  font-size: 13px;
  background: transparent;
  transition: all 0.15s;
}

.cell-input:focus {
  border-color: var(--primary);
  background: #fff;
}

.raw-section {
  margin-top: 16px;
}

.raw-toggle {
  font-size: 13px;
  color: var(--text-secondary);
  cursor: pointer;
  padding: 4px 0;
}

.raw-text {
  margin-top: 8px;
  padding: 12px;
  background: #f9fafb;
  border-radius: 6px;
  font-size: 12px;
  line-height: 1.5;
  white-space: pre-wrap;
  word-break: break-all;
  max-height: 200px;
  overflow-y: auto;
}

.btn-confirm {
  display: block;
  width: 100%;
  margin-top: 20px;
  padding: 12px;
  background: var(--primary);
  color: #fff;
  border-radius: var(--radius-sm);
  font-size: 15px;
  font-weight: 500;
  transition: opacity 0.15s;
}

.btn-confirm:active {
  opacity: 0.85;
}

.btn-confirm:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}

.json-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 12px;
}

.json-header .section-title {
  margin-bottom: 0;
}

.btn-copy {
  padding: 4px 12px;
  font-size: 13px;
  color: var(--primary);
  background: var(--primary-light);
  border-radius: 6px;
  font-weight: 500;
}

.btn-copy:active {
  opacity: 0.8;
}

.json-output {
  padding: 14px;
  background: #1e1e2e;
  color: #a6e3a1;
  border-radius: var(--radius-sm);
  font-size: 12px;
  line-height: 1.6;
  overflow-x: auto;
  -webkit-overflow-scrolling: touch;
  white-space: pre-wrap;
  word-break: break-all;
  max-height: 50vh;
  overflow-y: auto;
}

.editor-placeholder {
  text-align: center;
  padding: 40px 16px;
  color: var(--text-secondary);
  font-size: 14px;
}

.editor-placeholder.error {
  color: var(--danger);
}

.err-msg {
  margin-top: 4px;
  font-size: 12px;
  opacity: 0.8;
}

.spinner {
  width: 28px;
  height: 28px;
  border: 3px solid var(--border);
  border-top-color: var(--primary);
  border-radius: 50%;
  animation: spin 0.8s linear infinite;
  margin: 0 auto 12px;
}

@keyframes spin {
  to { transform: rotate(360deg); }
}
</style>
