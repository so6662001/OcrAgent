<template>
  <el-card>
    <template #header>
      <span style="font-size: 18px; font-weight: bold;">识别记录（跨任务查询）</span>
    </template>

    <el-form :inline="true" :model="query" style="margin-bottom: 16px;">
      <el-form-item label="供应商">
        <el-input v-model="query.supplierName" placeholder="供应商名称" clearable style="width: 180px;" />
      </el-form-item>
      <el-form-item label="文件名">
        <el-input v-model="query.fileName" placeholder="文件名" clearable style="width: 160px;" />
      </el-form-item>
      <el-form-item label="状态">
        <el-select v-model="query.status" placeholder="全部" clearable style="width: 120px;">
          <el-option label="成功" value="SUCCESS" />
          <el-option label="待审核" value="NEED_REVIEW" />
          <el-option label="已确认" value="CONFIRMED" />
          <el-option label="失败" value="FAILED" />
        </el-select>
      </el-form-item>
      <el-form-item label="供应商匹配">
        <el-select v-model="query.supplierMatched" placeholder="全部" clearable style="width: 120px;">
          <el-option label="已匹配" :value="true" />
          <el-option label="未匹配" :value="false" />
        </el-select>
      </el-form-item>
      <el-form-item label="创建时间">
        <el-date-picker v-model="query.dateRange" type="daterange" range-separator="~"
          start-placeholder="开始" end-placeholder="结束" value-format="YYYY-MM-DD" />
      </el-form-item>
      <el-form-item>
        <el-button type="primary" @click="loadRecords">查询</el-button>
        <el-button @click="resetQuery">重置</el-button>
      </el-form-item>
    </el-form>

    <el-table :data="records" stripe v-loading="loading">
      <el-table-column prop="fileName" label="文件名" min-width="180" />
      <el-table-column prop="taskId" label="所属任务" width="100" />
      <el-table-column label="供应商" width="160">
        <template #default="{ row }">
          <span v-if="row.supplierMatched === 1" style="color: #67c23a;">已匹配</span>
          <el-tag v-else type="danger" size="small">未匹配</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="置信度" width="100" align="center">
        <template #default="{ row }">
          <span :style="{ color: confColor(row.overallConf) }">
            {{ row.overallConf ? row.overallConf + '%' : '--' }}
          </span>
        </template>
      </el-table-column>
      <el-table-column prop="status" label="状态" width="110">
        <template #default="{ row }">
          <el-tag :type="statusType(row.status)" size="small">{{ statusText(row.status) }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="createdAt" label="创建时间" width="170" />
      <el-table-column label="操作" fixed="right" width="160">
        <template #default="{ row }">
          <el-button v-if="row.status === 'NEED_REVIEW'" type="warning" link @click="$router.push(`/review/${row.id}`)">审核</el-button>
          <el-button type="primary" link @click="showJson(row)">JSON</el-button>
        </template>
      </el-table-column>
    </el-table>

    <el-pagination style="margin-top: 16px; justify-content: flex-end;"
      v-model:current-page="query.page" v-model:page-size="query.size"
      :total="total" :page-sizes="[10, 20, 50]" layout="total, sizes, prev, pager, next"
      @change="loadRecords" />

    <el-dialog v-model="jsonVisible" title="识别结果JSON" width="70%">
      <vue-json-pretty :data="currentJson" :deep="3" />
    </el-dialog>
  </el-card>
</template>

<script setup>
import { ref, onMounted, reactive } from 'vue'
import { ocrApi } from '../../api/ocr'
import VueJsonPretty from 'vue-json-pretty'
import 'vue-json-pretty/lib/styles.css'

const records = ref([])
const total = ref(0)
const loading = ref(false)
const jsonVisible = ref(false)
const currentJson = ref(null)
const query = reactive({ page: 1, size: 20, supplierName: '', fileName: '', status: '', supplierMatched: null, dateRange: null })

const loadRecords = async () => {
  loading.value = true
  const params = {
    page: query.page, size: query.size,
    supplierName: query.supplierName || undefined,
    fileName: query.fileName || undefined,
    status: query.status || undefined,
    supplierMatched: query.supplierMatched !== null ? query.supplierMatched : undefined
  }
  if (query.dateRange) {
    params.startDate = query.dateRange[0]
    params.endDate = query.dateRange[1]
  }
  const res = await ocrApi.getFiles(params)
  records.value = res.data.data?.list || []
  total.value = res.data.data?.total || 0
  loading.value = false
}

const resetQuery = () => {
  Object.assign(query, { page: 1, supplierName: '', fileName: '', status: '', supplierMatched: null, dateRange: null })
  loadRecords()
}

const showJson = async (row) => {
  const res = await ocrApi.getFile(row.id)
  currentJson.value = res.data.data
  jsonVisible.value = true
}

const confColor = (conf) => {
  if (!conf) return '#909399'
  if (conf >= 95) return '#67c23a'
  if (conf >= 60) return '#e6a23c'
  return '#f56c6c'
}

const statusType = (s) => ({ SUCCESS: 'success', NEED_REVIEW: 'warning', CONFIRMED: 'success', FAILED: 'danger' }[s] || 'info')
const statusText = (s) => ({ PENDING: '等待中', RECOGNIZING: '识别中', SUCCESS: '已完成', NEED_REVIEW: '待审核', CONFIRMED: '已确认', FAILED: '失败' }[s] || s)

onMounted(loadRecords)
</script>
