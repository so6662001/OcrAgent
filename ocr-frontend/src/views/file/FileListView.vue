<template>
  <el-card>
    <template #header>
      <div style="display: flex; justify-content: space-between; align-items: center;">
        <span style="font-size: 18px; font-weight: bold;">任务文件列表 — {{ taskId }}</span>
        <el-button @click="$router.push('/tasks')">返回任务列表</el-button>
      </div>
    </template>

    <el-form :inline="true" style="margin-bottom: 16px;">
      <el-form-item label="状态">
        <el-select v-model="query.status" placeholder="全部" clearable style="width: 140px;">
          <el-option label="成功" value="SUCCESS" />
          <el-option label="待审核" value="NEED_REVIEW" />
          <el-option label="已确认" value="CONFIRMED" />
          <el-option label="失败" value="FAILED" />
        </el-select>
      </el-form-item>
      <el-form-item>
        <el-button type="primary" @click="loadFiles">查询</el-button>
      </el-form-item>
    </el-form>

    <el-table :data="files" stripe v-loading="loading">
      <el-table-column prop="fileName" label="文件名" min-width="180" />
      <el-table-column label="供应商" width="160">
        <template #default="{ row }">
          <span v-if="row.supplierMatched === 1" style="color: #67c23a;">{{ row.supplierId ? '已匹配' : '--' }}</span>
          <el-tag v-else type="danger" size="small">未匹配</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="置信度" width="100" align="center">
        <template #default="{ row }">
          <span :style="{ color: confColor(row.overallConf, row.thresholdUsed) }">
            {{ row.overallConf ? row.overallConf + '%' : '--' }}
          </span>
        </template>
      </el-table-column>
      <el-table-column prop="status" label="状态" width="110">
        <template #default="{ row }">
          <el-tag :type="fileStatusType(row.status)" size="small">{{ fileStatusText(row.status) }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="callbackStatus" label="回调" width="90">
        <template #default="{ row }">
          <el-tag :type="row.callbackStatus === 'SUCCESS' ? 'success' : 'info'" size="small">
            {{ row.callbackStatus || '--' }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="processTime" label="耗时" width="80" align="center">
        <template #default="{ row }">{{ row.processTime ? row.processTime + 'ms' : '--' }}</template>
      </el-table-column>
      <el-table-column label="操作" fixed="right" width="180">
        <template #default="{ row }">
          <el-button v-if="row.status === 'NEED_REVIEW'" type="warning" link @click="$router.push(`/review/${row.id}`)">审核</el-button>
          <el-button type="primary" link @click="showJson(row)">JSON</el-button>
          <el-button type="info" link @click="showDetail(row)">详情</el-button>
        </template>
      </el-table-column>
    </el-table>

    <el-pagination style="margin-top: 16px; justify-content: flex-end;"
      v-model:current-page="query.page" v-model:page-size="query.size"
      :total="total" layout="total, sizes, prev, pager, next" @change="loadFiles" />

    <el-dialog v-model="jsonVisible" title="识别结果JSON" width="70%">
      <el-tabs v-model="jsonTab">
        <el-tab-pane label="结构化结果" name="result">
          <vue-json-pretty :data="currentJson.result" :deep="3" />
        </el-tab-pane>
        <el-tab-pane label="百度OCR原始返回" name="raw">
          <vue-json-pretty :data="currentJson.raw" :deep="3" />
        </el-tab-pane>
      </el-tabs>
    </el-dialog>
  </el-card>
</template>

<script setup>
import { ref, onMounted, reactive } from 'vue'
import { ocrApi } from '../../api/ocr'
import VueJsonPretty from 'vue-json-pretty'
import 'vue-json-pretty/lib/styles.css'

const props = defineProps({ taskId: String })
const files = ref([])
const total = ref(0)
const loading = ref(false)
const query = reactive({ page: 1, size: 20, status: '' })
const jsonVisible = ref(false)
const jsonTab = ref('result')
const currentJson = reactive({ result: null, raw: null })

const loadFiles = async () => {
  loading.value = true
  const res = await ocrApi.getTaskFiles(props.taskId, { page: query.page, size: query.size, status: query.status || undefined })
  files.value = res.data.data?.list || []
  total.value = res.data.data?.total || 0
  loading.value = false
}

const showJson = async (row) => {
  const res = await ocrApi.getFile(row.id)
  currentJson.result = res.data.data?.resultJson
  currentJson.raw = res.data.data?.ocrRawJson
  jsonTab.value = 'result'
  jsonVisible.value = true
}

const showDetail = async (row) => {
  const res = await ocrApi.getFile(row.id)
  currentJson.result = res.data.data
  currentJson.raw = null
  jsonTab.value = 'result'
  jsonVisible.value = true
}

const confColor = (conf, threshold) => {
  if (!conf) return '#909399'
  if (conf >= (threshold || 95)) return '#67c23a'
  if (conf >= 60) return '#e6a23c'
  return '#f56c6c'
}

const fileStatusType = (s) => ({ SUCCESS: 'success', NEED_REVIEW: 'warning', CONFIRMED: 'success', FAILED: 'danger', PENDING: 'info', RECOGNIZING: 'warning' }[s] || 'info')
const fileStatusText = (s) => ({ PENDING: '等待中', PREPROCESSING: '预处理', RECOGNIZING: '识别中', SUCCESS: '已完成', NEED_REVIEW: '待审核', CONFIRMED: '已确认', FAILED: '失败' }[s] || s)

onMounted(loadFiles)
</script>
