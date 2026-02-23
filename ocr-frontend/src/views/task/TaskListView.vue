<template>
  <el-card>
    <template #header>
      <span style="font-size: 18px; font-weight: bold;">任务查询</span>
    </template>

    <el-form :inline="true" :model="query" style="margin-bottom: 16px;">
      <el-form-item label="任务编号">
        <el-input v-model="query.taskNo" placeholder="任务编号" clearable />
      </el-form-item>
      <el-form-item label="状态">
        <el-select v-model="query.status" placeholder="全部" clearable style="width: 140px;">
          <el-option label="处理中" value="PROCESSING" />
          <el-option label="部分完成" value="PARTIAL_DONE" />
          <el-option label="已完成" value="DONE" />
        </el-select>
      </el-form-item>
      <el-form-item label="创建时间">
        <el-date-picker v-model="query.dateRange" type="daterange" range-separator="~"
          start-placeholder="开始" end-placeholder="结束" value-format="YYYY-MM-DD" />
      </el-form-item>
      <el-form-item>
        <el-button type="primary" @click="loadTasks">查询</el-button>
        <el-button @click="resetQuery">重置</el-button>
      </el-form-item>
    </el-form>

    <el-table :data="tasks" stripe v-loading="loading">
      <el-table-column prop="taskNo" label="任务编号" width="200" />
      <el-table-column label="单据类型" width="120">
        <template #default="{ row }">{{ row.docTypeId }}</template>
      </el-table-column>
      <el-table-column prop="totalFiles" label="文件数" width="80" align="center" />
      <el-table-column prop="successCount" label="成功" width="70" align="center" />
      <el-table-column prop="reviewCount" label="待审核" width="80" align="center" />
      <el-table-column prop="failedCount" label="失败" width="70" align="center" />
      <el-table-column prop="status" label="状态" width="120">
        <template #default="{ row }">
          <el-tag :type="statusType(row.status)">{{ statusText(row.status) }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="createdBy" label="创建人" width="100" />
      <el-table-column prop="createdAt" label="创建时间" width="170" />
      <el-table-column label="操作" fixed="right" width="100">
        <template #default="{ row }">
          <el-button type="primary" link @click="$router.push(`/task/${row.id}/files`)">查看文件</el-button>
        </template>
      </el-table-column>
    </el-table>

    <el-pagination style="margin-top: 16px; justify-content: flex-end;"
      v-model:current-page="query.page" v-model:page-size="query.size"
      :total="total" :page-sizes="[10, 20, 50]" layout="total, sizes, prev, pager, next"
      @change="loadTasks" />
  </el-card>
</template>

<script setup>
import { ref, onMounted, reactive } from 'vue'
import { ocrApi } from '../../api/ocr'

const tasks = ref([])
const total = ref(0)
const loading = ref(false)
const query = reactive({ page: 1, size: 20, taskNo: '', status: '', dateRange: null })

const loadTasks = async () => {
  loading.value = true
  const params = { page: query.page, size: query.size, status: query.status || undefined }
  if (query.dateRange) {
    params.startDate = query.dateRange[0]
    params.endDate = query.dateRange[1]
  }
  const res = await ocrApi.getTasks(params)
  tasks.value = res.data.data?.list || []
  total.value = res.data.data?.total || 0
  loading.value = false
}

const resetQuery = () => {
  query.page = 1; query.taskNo = ''; query.status = ''; query.dateRange = null
  loadTasks()
}

const statusType = (s) => ({ PROCESSING: 'warning', PARTIAL_DONE: 'warning', DONE: 'success', ALL_CALLBACK: 'success' }[s] || 'info')
const statusText = (s) => ({ UPLOADING: '上传中', PROCESSING: '处理中', PARTIAL_DONE: '部分完成', DONE: '已完成', ALL_CALLBACK: '全部回调' }[s] || s)

onMounted(loadTasks)
</script>
