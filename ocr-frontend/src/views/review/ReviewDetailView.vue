<template>
  <el-card v-loading="loading">
    <template #header>
      <div style="display: flex; justify-content: space-between; align-items: center;">
        <span style="font-size: 18px; font-weight: bold;">人工审核 — {{ file?.fileName }}</span>
        <div>
          <el-button @click="$router.push('/review')">返回列表</el-button>
        </div>
      </div>
    </template>

    <el-row :gutter="20" v-if="file">
      <el-col :span="10">
        <el-card shadow="never">
          <template #header>原始文件预览</template>
          <div style="text-align: center; color: #909399; padding: 60px 0;">
            <el-icon style="font-size: 48px;"><Picture /></el-icon>
            <div style="margin-top: 10px;">{{ file.fileName }}</div>
            <div style="font-size: 12px; margin-top: 5px;">文件预览需要MinIO服务支持</div>
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
              <el-input v-model="f.editValue" :class="confClass(f.confidence)"
                :style="{ borderColor: confBorderColor(f.confidence) }" size="small" />
              <el-tag :type="confTagType(f.confidence)" size="small" style="margin-left: 8px;">
                {{ f.confidence }}%
              </el-tag>
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
                    {{ row[col.fieldCode].confidence }}%
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
            | 当前阈值: {{ file.thresholdUsed }}%
          </div>
          <el-button type="primary" size="large" @click="submitConfirm" :loading="submitting">确认提交</el-button>
          <el-button size="large" @click="$router.push('/review')">跳过</el-button>
        </div>
      </el-col>
    </el-row>
  </el-card>
</template>

<script setup>
import { ref, onMounted, reactive, computed } from 'vue'
import { useRouter } from 'vue-router'
import { ocrApi } from '../../api/ocr'
import { ElMessage } from 'element-plus'

const props = defineProps({ fileId: String })
const router = useRouter()
const file = ref(null)
const loading = ref(false)
const submitting = ref(false)
const suppliers = ref([])
const confirmData = reactive({ supplierId: null })

const headerFields = ref([])
const bodyRows = ref([])
const bodyColumns = ref([])

onMounted(async () => {
  loading.value = true
  const [fileRes, supplierRes] = await Promise.all([
    ocrApi.getFile(props.fileId),
    ocrApi.getSuppliers('')
  ])
  file.value = fileRes.data.data
  suppliers.value = supplierRes.data.data || []

  if (file.value?.resultJson) {
    const result = typeof file.value.resultJson === 'string' ? JSON.parse(file.value.resultJson) : file.value.resultJson
    if (result.header) {
      headerFields.value = result.header.map(h => ({ ...h, editValue: h.value }))
    }
    if (result.body) {
      const colSet = new Map()
      result.body.forEach(row => {
        row.cells?.forEach(c => {
          if (!colSet.has(c.fieldCode)) colSet.set(c.fieldCode, c.fieldName)
        })
      })
      bodyColumns.value = [...colSet.entries()].map(([fieldCode, fieldName]) => ({ fieldCode, fieldName }))

      bodyRows.value = result.body.map(row => {
        const mapped = {}
        row.cells?.forEach(c => { mapped[c.fieldCode] = { ...c, editValue: c.value } })
        return mapped
      })
    }
  }
  loading.value = false
})

const confBorderColor = (conf) => {
  if (!conf) return '#dcdfe6'
  const t = file.value?.thresholdUsed || 95
  if (conf >= t) return '#67c23a'
  if (conf >= 60) return '#e6a23c'
  return '#f56c6c'
}

const confClass = (conf) => {
  const t = file.value?.thresholdUsed || 95
  if (conf >= t) return ''
  if (conf >= 60) return ''
  return ''
}

const confTagType = (conf) => {
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
    router.push('/review')
  } catch (e) {
    ElMessage.error('提交失败')
  } finally {
    submitting.value = false
  }
}
</script>
