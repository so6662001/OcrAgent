<template>
  <el-card>
    <el-tabs v-model="activeTab">
      <el-tab-pane label="单据类型管理" name="docType">
        <el-button type="primary" style="margin-bottom: 16px;" @click="showAddDocType">新增单据类型</el-button>
        <el-table :data="docTypes" stripe>
          <el-table-column prop="typeCode" label="编码" width="180" />
          <el-table-column prop="typeName" label="名称" width="160" />
          <el-table-column prop="description" label="描述" min-width="200" />
          <el-table-column prop="defaultThreshold" label="默认阈值" width="100" align="center">
            <template #default="{ row }">{{ row.defaultThreshold }}%</template>
          </el-table-column>
          <el-table-column label="操作" width="200">
            <template #default="{ row }">
              <el-button type="primary" link @click="showFields(row)">字段配置</el-button>
              <el-button type="danger" link @click="handleDeleteDocType(row)">删除</el-button>
            </template>
          </el-table-column>
        </el-table>
      </el-tab-pane>

      <el-tab-pane label="阈值设置" name="threshold">
        <el-table :data="thresholdList" stripe>
          <el-table-column prop="typeName" label="单据类型" width="200" />
          <el-table-column prop="defaultThreshold" label="系统默认" width="120" align="center">
            <template #default="{ row }">{{ row.defaultThreshold }}%</template>
          </el-table-column>
          <el-table-column label="本租户自定义" width="200">
            <template #default="{ row }">
              <el-input-number v-model="row.customThreshold" :min="50" :max="100" :step="1" size="small" />
            </template>
          </el-table-column>
          <el-table-column label="操作" width="160">
            <template #default="{ row }">
              <el-button type="primary" size="small" @click="saveThreshold(row)">保存</el-button>
              <el-button size="small" @click="resetThreshold(row)">重置</el-button>
            </template>
          </el-table-column>
        </el-table>
      </el-tab-pane>
    </el-tabs>

    <el-dialog v-model="docTypeDialogVisible" title="新增单据类型" width="500px">
      <el-form :model="newDocType" label-width="100px">
        <el-form-item label="类型编码"><el-input v-model="newDocType.typeCode" /></el-form-item>
        <el-form-item label="类型名称"><el-input v-model="newDocType.typeName" /></el-form-item>
        <el-form-item label="描述"><el-input v-model="newDocType.description" type="textarea" /></el-form-item>
        <el-form-item label="默认阈值"><el-input-number v-model="newDocType.defaultThreshold" :min="50" :max="100" /></el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="docTypeDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="createDocType">确定</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="fieldDialogVisible" :title="`字段配置 — ${currentDocType?.typeName}`" width="70%">
      <el-button type="primary" size="small" style="margin-bottom: 12px;" @click="showAddField">新增字段</el-button>
      <el-table :data="fields" stripe size="small">
        <el-table-column prop="fieldCode" label="编码" width="140" />
        <el-table-column prop="fieldName" label="名称" width="120" />
        <el-table-column prop="fieldType" label="类型" width="100" />
        <el-table-column prop="position" label="位置" width="80">
          <template #default="{ row }">
            <el-tag :type="row.position === 'HEADER' ? '' : 'success'" size="small">{{ row.position }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="isRequired" label="必填" width="60" align="center">
          <template #default="{ row }">{{ row.isRequired ? '是' : '否' }}</template>
        </el-table-column>
        <el-table-column prop="sortOrder" label="排序" width="60" align="center" />
        <el-table-column label="操作" width="80">
          <template #default="{ row }">
            <el-button type="danger" link size="small" @click="handleDeleteField(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-dialog>
  </el-card>
</template>

<script setup>
import { ref, onMounted, reactive } from 'vue'
import { ocrApi } from '../../api/ocr'
import { ElMessage, ElMessageBox } from 'element-plus'

const activeTab = ref('docType')
const docTypes = ref([])
const thresholdList = ref([])
const docTypeDialogVisible = ref(false)
const fieldDialogVisible = ref(false)
const currentDocType = ref(null)
const fields = ref([])

const newDocType = reactive({ typeCode: '', typeName: '', description: '', defaultThreshold: 95 })

const loadDocTypes = async () => {
  const res = await ocrApi.getDocTypes()
  docTypes.value = res.data.data || []
  thresholdList.value = docTypes.value.map(dt => ({
    ...dt,
    customThreshold: dt.defaultThreshold
  }))
  const tRes = await ocrApi.getThresholds()
  const customs = tRes.data.data || []
  customs.forEach(c => {
    const item = thresholdList.value.find(t => t.id === c.docTypeId)
    if (item) item.customThreshold = c.threshold
  })
}

const showAddDocType = () => { Object.assign(newDocType, { typeCode: '', typeName: '', description: '', defaultThreshold: 95 }); docTypeDialogVisible.value = true }

const createDocType = async () => {
  await ocrApi.createDocType(newDocType)
  ElMessage.success('创建成功')
  docTypeDialogVisible.value = false
  loadDocTypes()
}

const handleDeleteDocType = async (row) => {
  await ElMessageBox.confirm('确定删除该单据类型？')
  await ocrApi.deleteDocType(row.id)
  ElMessage.success('删除成功')
  loadDocTypes()
}

const showFields = async (row) => {
  currentDocType.value = row
  const res = await ocrApi.getFields(row.id)
  fields.value = res.data.data || []
  fieldDialogVisible.value = true
}

const showAddField = () => { ElMessage.info('字段新增功能开发中') }

const handleDeleteField = async (row) => {
  await ocrApi.deleteField(row.id)
  ElMessage.success('删除成功')
  showFields(currentDocType.value)
}

const saveThreshold = async (row) => {
  await ocrApi.setThreshold(row.id, row.customThreshold)
  ElMessage.success('保存成功')
}

const resetThreshold = async (row) => {
  await ocrApi.resetThreshold(row.id)
  row.customThreshold = row.defaultThreshold
  ElMessage.success('已重置')
}

onMounted(loadDocTypes)
</script>
