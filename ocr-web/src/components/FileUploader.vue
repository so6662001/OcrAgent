<script setup lang="ts">
import { ref } from 'vue'

const emit = defineEmits<{
  (e: 'files-selected', files: File[]): void
}>()

const isDragging = ref(false)
const fileInput = ref<HTMLInputElement>()

function handleDrop(e: DragEvent) {
  isDragging.value = false
  const files = Array.from(e.dataTransfer?.files || [])
  if (files.length) emit('files-selected', filterFiles(files))
}

function handleSelect(e: Event) {
  const input = e.target as HTMLInputElement
  const files = Array.from(input.files || [])
  if (files.length) emit('files-selected', filterFiles(files))
  input.value = ''
}

const MAX_FILE_SIZE = 20 * 1024 * 1024 // 20MB
const MAX_FILES = 20

function filterFiles(files: File[]) {
  const allowed = ['image/jpeg', 'image/png', 'image/bmp', 'image/tiff', 'application/pdf']
  const filtered = files.filter(f => {
    if (f.size > MAX_FILE_SIZE) return false
    if (allowed.includes(f.type)) return true
    const ext = f.name.toLowerCase().split('.').pop()
    return ['jpg', 'jpeg', 'png', 'bmp', 'tiff', 'pdf'].includes(ext || '')
  })
  if (filtered.length > MAX_FILES) {
    return filtered.slice(0, MAX_FILES)
  }
  return filtered
}

function openPicker() {
  fileInput.value?.click()
}
</script>

<template>
  <div
    class="uploader"
    :class="{ dragging: isDragging }"
    @dragover.prevent="isDragging = true"
    @dragleave.prevent="isDragging = false"
    @drop.prevent="handleDrop"
    @click="openPicker"
  >
    <input
      ref="fileInput"
      type="file"
      multiple
      accept="image/*,.pdf"
      style="display: none"
      @change="handleSelect"
    />
    <div class="uploader-icon">
      <svg width="40" height="40" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5">
        <path d="M21 15v4a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2v-4"/>
        <polyline points="17 8 12 3 7 8"/>
        <line x1="12" y1="3" x2="12" y2="15"/>
      </svg>
    </div>
    <p class="uploader-text">点击或拖拽文件到此处</p>
    <p class="uploader-hint">支持 JPG / PNG / PDF，可多选</p>
  </div>
</template>

<style scoped>
.uploader {
  border: 2px dashed var(--border);
  border-radius: var(--radius);
  padding: 32px 16px;
  text-align: center;
  cursor: pointer;
  transition: all 0.2s;
  background: var(--card);
}

.uploader:active,
.uploader.dragging {
  border-color: var(--primary);
  background: var(--primary-light);
}

@media (hover: hover) {
  .uploader:hover {
    border-color: var(--primary);
    background: var(--primary-light);
  }
}

.uploader-icon {
  color: var(--text-secondary);
  margin-bottom: 8px;
}

.dragging .uploader-icon,
.uploader:hover .uploader-icon {
  color: var(--primary);
}

.uploader-text {
  font-size: 15px;
  font-weight: 500;
  color: var(--text);
  margin-bottom: 4px;
}

.uploader-hint {
  font-size: 13px;
  color: var(--text-secondary);
}
</style>
