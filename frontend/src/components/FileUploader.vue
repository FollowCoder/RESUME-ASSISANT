<script setup lang="ts">
import { ref } from 'vue'
import { UploadFilled } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import type { UploadFile, UploadFiles } from 'element-plus'

interface Props {
  accept?: string
}

const props = withDefaults(defineProps<Props>(), {
  accept: '.pdf,.doc,.docx'
})

const emit = defineEmits<{
  upload: [file: File]
}>()

const selectedFile = ref<File | null>(null)
const fileList = ref<UploadFiles>([])

const MAX_SIZE = 10 * 1024 * 1024 // 10MB

function handleChange(uploadFile: UploadFile, uploadFiles: UploadFiles) {
  const rawFile = uploadFile.raw
  if (!rawFile) return

  // 文件类型校验
  const allowedTypes = [
    'application/pdf',
    'application/msword',
    'application/vnd.openxmlformats-officedocument.wordprocessingml.document'
  ]
  if (!allowedTypes.includes(rawFile.type)) {
    ElMessage.warning('仅支持 PDF、Word 格式文件')
    fileList.value = []
    selectedFile.value = null
    return
  }

  // 文件大小校验
  if (rawFile.size > MAX_SIZE) {
    ElMessage.warning('文件大小不能超过 10MB')
    fileList.value = []
    selectedFile.value = null
    return
  }

  selectedFile.value = rawFile
  fileList.value = [uploadFile]
  emit('upload', rawFile)
}

function handleExceed() {
  ElMessage.warning('只能上传一个文件，请先移除已选文件')
}

function clearFile() {
  selectedFile.value = null
  fileList.value = []
}

defineExpose({ selectedFile, clearFile })
</script>

<template>
  <div class="file-uploader">
    <el-upload
      drag
      :auto-upload="false"
      :accept="props.accept"
      :limit="1"
      :file-list="fileList"
      :on-change="handleChange"
      :on-exceed="handleExceed"
    >
      <el-icon class="el-icon--upload" :size="48">
        <UploadFilled />
      </el-icon>
      <div class="el-upload__text">
        拖拽文件到此处，或 <em>点击上传</em>
      </div>
      <template #tip>
        <div class="el-upload__tip">
          支持 PDF、Word 格式，文件大小不超过 10MB
        </div>
      </template>
    </el-upload>
  </div>
</template>

<style lang="scss" scoped>
.file-uploader {
  width: 100%;

  :deep(.el-upload-dragger) {
    padding: 56px 32px;
    border-radius: var(--radius-lg);
    border: 2px dashed var(--color-border);
    background: linear-gradient(135deg, var(--color-bg-warm) 0%, rgba(232, 148, 58, 0.02) 100%);
    transition: all var(--transition-smooth);
    position: relative;
    overflow: hidden;

    &::before {
      content: '';
      position: absolute;
      top: 0;
      left: 0;
      right: 0;
      bottom: 0;
      background: radial-gradient(circle at center, rgba(232, 148, 58, 0.05) 0%, transparent 70%);
      opacity: 0;
      transition: opacity var(--transition-smooth);
    }

    &:hover {
      border-color: var(--color-accent);
      background: linear-gradient(135deg, rgba(232, 148, 58, 0.06) 0%, rgba(232, 148, 58, 0.02) 100%);
      transform: translateY(-2px);
      box-shadow: var(--shadow-md);

      &::before {
        opacity: 1;
      }
    }
  }

  .el-icon--upload {
    color: var(--color-accent);
    margin-bottom: 16px;
    transition: all var(--transition-smooth);
  }

  :deep(.el-upload__text) {
    color: var(--color-text-primary);
    font-size: 16px;
    font-weight: 500;

    em {
      color: var(--color-accent);
      font-style: normal;
      font-weight: 600;
      text-decoration: underline;
      text-underline-offset: 4px;
    }
  }

  :deep(.el-upload__tip) {
    color: var(--color-text-secondary);
    font-size: 14px;
    margin-top: 12px;
  }
}
</style>
