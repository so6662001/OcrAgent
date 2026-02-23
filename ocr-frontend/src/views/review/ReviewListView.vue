<template>
  <el-card>
    <template #header>
      <span style="font-size: 18px; font-weight: bold;">人工审核</span>
    </template>

    <el-table :data="files" stripe v-loading="loading">
      <el-table-column prop="fileName" label="文件名" min-width="200" />
      <el-table-column prop="taskId" label="任务ID" width="100" />
      <el-table-column label="置信度" width="100" align="center">
        <template #default="{ row }">
          <span style="color: #e6a23c; font-weight: bold;">{{ row.overallConf }}%</span>
        </template>
      </el-table-column>
      <el-table-column label="供应商" width="120">
        <template #default="{ row }">
          <el-tag v-if="row.supplierMatched !== 1" type="danger" size="small">未匹配</el-tag>
          <span v-else>已匹配</span>
        </template>
      </el-table-column>
      <el-table-column prop="createdAt" label="创建时间" width="170" />
      <el-table-column label="操作" fixed="right" width="120">
        <template #default="{ row }">
          <el-button type="primary" @click="$router.push(`/review/${row.id}`)">去审核</el-button>
        </template>
      </el-table-column>
    </el-table>

    <el-pagination style="margin-top: 16px; justify-content: flex-end;"
      v-model:current-page="query.page" v-model:page-size="query.size"
      :total="total" layout="total, sizes, prev, pager, next" @change="loadFiles" />
  </el-card>
</template>

<script setup>
import { ref, onMounted, reactive } from 'vue'
import { ocrApi } from '../../api/ocr'

const files = ref([])
const total = ref(0)
const loading = ref(false)
const query = reactive({ page: 1, size: 20 })

const loadFiles = async () => {
  loading.value = true
  const res = await ocrApi.getFiles({ page: query.page, size: query.size, status: 'NEED_REVIEW' })
  files.value = res.data.data?.list || []
  total.value = res.data.data?.total || 0
  loading.value = false
}

onMounted(loadFiles)
</script>
