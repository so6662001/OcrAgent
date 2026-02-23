<template>
  <el-card>
    <template #header>
      <span style="font-size: 18px; font-weight: bold;">文件识别</span>
    </template>

    <el-form :model="form" label-width="100px">
      <el-form-item label="单据类型" required>
        <el-select v-model="form.docTypeCode" placeholder="请选择单据类型" style="width: 300px;">
          <el-option v-for="t in docTypes" :key="t.typeCode" :label="t.typeName" :value="t.typeCode" />
        </el-select>
      </el-form-item>
      <el-form-item label="回调地址" required>
        <el-input v-model="form.callbackUrl" placeholder="https://your-erp.com/api/callback" style="width: 500px;" />
      </el-form-item>
      <el-form-item label="上传文件">
        <el-upload
          ref="uploadRef"
          drag
          multiple
          :auto-upload="false"
          :file-list="fileList"
          :on-change="handleChange"
          :on-remove="handleRemove"
          accept=".pdf,.jpg,.jpeg,.png"
        >
          <el-icon style="font-size: 40px; color: #909399;"><Upload /></el-icon>
          <div style="color: #606266;">拖拽文件到此处或点击上传</div>
          <div style="color: #909399; font-size: 12px;">支持 PDF / JPG / PNG，单次最多50个文件</div>
        </el-upload>
      </el-form-item>
      <el-form-item>
        <div style="color: #909399; font-size: 13px; margin-bottom: 10px;">
          供应商无需手动选择，系统将从识别内容中自动匹配
        </div>
        <el-button type="primary" size="large" :loading="submitting" @click="startRecognize">
          开始识别
        </el-button>
      </el-form-item>
    </el-form>

    <el-result v-if="result" icon="success" title="提交成功" :sub-title="`任务编号: ${result.taskNo}，共${result.totalFiles}个文件`">
      <template #extra>
        <el-button type="primary" @click="$router.push(`/task/${result.taskId}/files`)">查看进度</el-button>
        <el-button @click="resetForm">继续上传</el-button>
      </template>
    </el-result>
  </el-card>
</template>

<script setup>
import { ref, onMounted, reactive } from 'vue'
import { ocrApi } from '../../api/ocr'
import { ElMessage } from 'element-plus'

const docTypes = ref([])
const fileList = ref([])
const submitting = ref(false)
const result = ref(null)
const uploadRef = ref(null)

const form = reactive({
  docTypeCode: '',
  callbackUrl: 'http://localhost:8080/api/demo/callback/receive'
})

onMounted(async () => {
  const res = await ocrApi.getDocTypes()
  docTypes.value = res.data.data || []
})

const handleChange = (file, list) => { fileList.value = list }
const handleRemove = (file, list) => { fileList.value = list }

const startRecognize = async () => {
  if (!form.docTypeCode) return ElMessage.warning('请选择单据类型')
  if (!form.callbackUrl) return ElMessage.warning('请输入回调地址')
  if (fileList.value.length === 0) return ElMessage.warning('请上传文件')

  submitting.value = true
  try {
    const fd = new FormData()
    fd.append('docTypeCode', form.docTypeCode)
    fd.append('callbackUrl', form.callbackUrl)
    fileList.value.forEach(f => fd.append('files', f.raw))

    const res = await ocrApi.recognize(fd)
    result.value = res.data.data
    ElMessage.success('提交成功')
  } catch (e) {
    ElMessage.error('提交失败')
  } finally {
    submitting.value = false
  }
}

const resetForm = () => {
  result.value = null
  fileList.value = []
  form.docTypeCode = ''
}
</script>
