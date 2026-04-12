<template>
  <el-card v-loading="loading">
    <template #header>
      <div style="display: flex; justify-content: space-between; align-items: center;">
        <span style="font-size: 18px; font-weight: bold;">人工审核 — {{ file?.fileName }}</span>
        <div>
          <el-button :disabled="!hasPrev" @click="goPrev">上一个</el-button>
          <el-button :disabled="!hasNext" @click="goNext">下一个</el-button>
          <el-button @click="$router.push('/review')">返回列表</el-button>
        </div>
      </div>
    </template>

    <el-row :gutter="20" v-if="file">
      <el-col :span="10">
        <el-card shadow="never">
          <template #header>原始文件预览</template>
          <div v-if="file.fileUrl && isImage(file.fileType)" style="text-align: center;">
            <img :src="file.fileUrl" style="max-width: 100%; max-height: 500px;" alt="原始文件" />
          </div>
          <div v-else style="text-align: center; color: #909399; padding: 60px 0;">
            <el-icon style="font-size: 48px;"><Document /></el-icon>
            <div style="margin-top: 10px;">{{ file.fileName }}</div>
            <div style="font-size: 12px; margin-top: 5px;">{{ file.fileUrl ? '点击下载查看' : '文件预览需要MinIO服务' }}</div>
            <el-button v-if="file.fileUrl" type="primary" link style="margin-top: 8px;"
              @click="window.open(file.fileUrl)">下载文件</el-button>
          </div>
        </el-card>
      </el-col>

      <el-col :span="14">
        <el-alert v-if="file.supplierMatched !== 1" type="warning" title="供应商未匹配" :closable="false" show-icon style="margin-bottom: 16px;">
          <template #default>
            <div>请选择供应商：
              <el-select v-model="confirmData.supplierId" placeholder="选择供应商" style="width: 280px; margin-left: 10px;" filterable>
                <el-option v-for="s in suppliers" :key="s.id" :label="s.supplierName" :value="s.id" />
              </el-select>
            </div>
          </template>
        </el-alert>

        <el-card shadow="never" style="margin-bottom: 16px;">
          <template #header>表头信息</template>
          <el-descriptions :column="2" border v-if="headerFields.length">
            <el-descriptions-item v-for="(f, i) in headerFields" :key="i" :label="f.fieldName">
              <div style="display: flex; align-items: center; gap: 8px;">
                <el-input v-model="f.editValue" size="small"
                  :style="{ borderColor: confBorderColor(f.confidence) }" />
                <el-tag :type="confTagType(f.confidence)" size="small" style="flex-shrink: 0;">
                  {{ f.confidence != null ? f.confidence + '%' : '--' }}
                </el-tag>
              </div>
            </el-descriptions-item>
          </el-descriptions>
          <el-empty v-else description="无表头数据" :image-size="40" />
        </el-card>

        <el-card shadow="never">
          <template #header>表体信息</template>
          <el-table :data="bodyRows" border size="small" v-if="bodyRows.length">
            <el-table-column v-for="col in bodyColumns" :key="col.fieldCode" :label="col.fieldName" min-width="120">
              <template #default="{ row }">
                <div v-if="row[col.fieldCode]">
                  <el-input v-model="row[col.fieldCode].editValue" size="small"
                    :style="{ borderColor: confBorderColor(row[col.fieldCode].confidence) }" />
                  <el-tag :type="confTagType(row[col.fieldCode].confidence)" size="small" style="margin-top: 4px;">
                    {{ row[col.fieldCode].confidence != null ? row[col.fieldCode].confidence + '%' : '--' }}
                  </el-tag>
                </div>
              </template>
            </el-table-column>
          </el-table>
          <el-empty v-else description="无表体数据" :image-size="40" />
        </el-card>

        <div style="margin-top: 16px; display: flex; gap: 12px;">
          <div style="flex: 1; color: #909399; font-size: 12px;">
            颜色说明：
            <span style="color: #67c23a;">绿色(≥阈值)</span>
            <span style="color: #e6a23c; margin-left: 8px;">橙色(60%~阈值)</span>
            <span style="color: #f56c6c; margin-left: 8px;">红色(&lt;60%)</span>
            | 当前阈值: {{ file.thresholdUsed || 95 }}%
          </div>
          <el-button type="primary" size="large" @click="submitConfirm" :loading="submitting">确认提交</el-button>
          <el-button size="large" @click="goNext" :disabled="!hasNext">跳过</el-button>
        </div>
      </el-col>
    </el-row>
  </el-card>
</template>

<script setup>
import { ref, onMounted, reactive } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { ocrApi } from '../../api/ocr'
import { ElMessage } from 'element-plus'

const props = defineProps({ fileId: String })
const router = useRouter()
const route = useRoute()
const file = ref(null)
const loading = ref(false)
const submitting = ref(false)
const suppliers = ref([])
const confirmData = reactive({ supplierId: null })

const headerFields = ref([])
const bodyRows = ref([])
const bodyColumns = ref([])

const reviewList = ref([])
const currentIndex = ref(-1)
const hasPrev = ref(false)
const hasNext = ref(false)

const loadReviewList = async () => {
  try {
    const res = await ocrApi.getFiles({ page: 1, size: 200, status: 'NEED_REVIEW' })
    reviewList.value = res.data.data?.list || []
    currentIndex.value = reviewList.value.findIndex(f => String(f.id) === String(props.fileId))
    hasPrev.value = currentIndex.value > 0
    hasNext.value = currentIndex.value < reviewList.value.length - 1
  } catch { /* ignore */ }
}

const goPrev = () => {
  if (currentIndex.value > 0) {
    router.push(`/review/${reviewList.value[currentIndex.value - 1].id}`)
  }
}

const goNext = () => {
  if (currentIndex.value < reviewList.value.length - 1) {
    router.push(`/review/${reviewList.value[currentIndex.value + 1].id}`)
  }
}

const isImage = (type) => ['jpg', 'jpeg', 'png'].includes((type || '').toLowerCase())

const loadFileData = async () => {
  loading.value = true
  try {
    const [fileRes, supplierRes] = await Promise.all([
      ocrApi.getReview(props.fileId),
      ocrApi.getSuppliers('')
    ])
    file.value = fileRes.data.data
    suppliers.value = supplierRes.data.data || []

    parseResult()
  } catch (e) {
    ElMessage.error('加载审核数据失败')
  } finally {
    loading.value = false
  }
}

const parseResult = () => {
  headerFields.value = []
  bodyRows.value = []
  bodyColumns.value = []

  if (!file.value?.resultJson) return

  let result
  try {
    result = typeof file.value.resultJson === 'string' ? JSON.parse(file.value.resultJson) : file.value.resultJson
  } catch (e) {
    ElMessage.warning('识别结果JSON解析失败')
    return
  }

  if (result.header && Array.isArray(result.header)) {
    headerFields.value = result.header.map(h => ({ ...h, editValue: h.value || '' }))
  }
  if (result.body && Array.isArray(result.body)) {
    const colSet = new Map()
    result.body.forEach(row => {
      if (row.cells && Array.isArray(row.cells)) {
        row.cells.forEach(c => {
          if (c.fieldCode && !colSet.has(c.fieldCode)) colSet.set(c.fieldCode, c.fieldName || c.fieldCode)
        })
      }
    })
    bodyColumns.value = [...colSet.entries()].map(([fieldCode, fieldName]) => ({ fieldCode, fieldName }))

    bodyRows.value = result.body.map(row => {
      const mapped = {}
      if (row.cells && Array.isArray(row.cells)) {
        row.cells.forEach(c => { if (c.fieldCode) mapped[c.fieldCode] = { ...c, editValue: c.value || '' } })
      }
      return mapped
    })
  }
}

const confBorderColor = (conf) => {
  if (conf == null) return '#dcdfe6'
  const t = file.value?.thresholdUsed || 95
  if (conf >= t) return '#67c23a'
  if (conf >= 60) return '#e6a23c'
  return '#f56c6c'
}

const confTagType = (conf) => {
  if (conf == null) return 'info'
  const t = file.value?.thresholdUsed || 95
  if (conf >= t) return 'success'
  if (conf >= 60) return 'warning'
  return 'danger'
}

const submitConfirm = async () => {
  submitting.value = true
  try {
    const data = {
      supplierId: confirmData.supplierId,
      header: headerFields.value.map(f => ({ fieldCode: f.fieldCode, value: f.editValue })),
      body: bodyRows.value.map((row, i) => ({
        rowIndex: i,
        cells: Object.entries(row).map(([code, cell]) => ({ fieldCode: code, value: cell.editValue }))
      }))
    }
    await ocrApi.confirmReview(props.fileId, data)
    ElMessage.success('审核确认成功')
    if (hasNext.value) {
      goNext()
    } else {
      router.push('/review')
    }
  } catch (e) {
    ElMessage.error(e.response?.data?.message || '提交失败')
  } finally {
    submitting.value = false
  }
}

onMounted(() => {
  loadReviewList()
  loadFileData()
})
</script>
