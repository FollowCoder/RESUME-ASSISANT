<script setup lang="ts">
import { ref, computed } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Document, Upload, FolderOpened, Loading } from '@element-plus/icons-vue'
import FileUploader from '@/components/FileUploader.vue'
import OptimizationDiff from '@/components/OptimizationDiff.vue'
import { optimizeApi } from '@/api/optimize'
import type { OptimizationResult, OptimizationSuggestion } from '@/api/optimize'
import { resumeApi } from '@/api/resume'
import type { ResumeListItem } from '@/api/types'

// ========== 页面状态 ==========
type PageState = 'input' | 'loading' | 'result'
const pageState = ref<PageState>('input')

// ========== 输入阶段 ==========
type InputMode = 'text' | 'file' | 'existing'
const inputMode = ref<InputMode>('text')

// 文本模式
const textContent = ref('')

// 文件模式
const uploadedFile = ref<File | null>(null)
const fileUploaderRef = ref<InstanceType<typeof FileUploader> | null>(null)

// 选择已有模式
const resumeList = ref<ResumeListItem[]>([])
const selectedResumeId = ref<number | null>(null)
const loadingResumes = ref(false)

// ========== 结果阶段 ==========
const optimizationResult = ref<OptimizationResult | null>(null)
const suggestionStatuses = ref<Record<string, 'pending' | 'accepted' | 'rejected'>>({})
const applyLoading = ref(false)

// ========== 计算属性 ==========
const canStartOptimize = computed(() => {
  switch (inputMode.value) {
    case 'text':
      return textContent.value.trim().length > 0
    case 'file':
      return uploadedFile.value !== null
    case 'existing':
      return selectedResumeId.value !== null
    default:
      return false
  }
})

const acceptedCount = computed(() => {
  return Object.values(suggestionStatuses.value).filter(s => s === 'accepted').length
})

const totalCount = computed(() => {
  return optimizationResult.value?.suggestions.length || 0
})

const acceptedSuggestionIds = computed(() => {
  return Object.entries(suggestionStatuses.value)
    .filter(([_, status]) => status === 'accepted')
    .map(([id]) => id)
})

// ========== 方法 ==========

// 加载简历列表
async function loadResumeList() {
  if (resumeList.value.length > 0) return
  loadingResumes.value = true
  try {
    resumeList.value = await resumeApi.getList()
  } catch (e) {
    // 错误已由拦截器处理
  } finally {
    loadingResumes.value = false
  }
}

// 输入模式切换
function handleModeChange(mode: string | number | boolean | undefined) {
  inputMode.value = mode as InputMode
  if (mode === 'existing') {
    loadResumeList()
  }
}

// 文件上传回调
function handleFileUpload(file: File) {
  uploadedFile.value = file
}

// 开始优化
async function startOptimize() {
  pageState.value = 'loading'
  try {
    let result: OptimizationResult

    switch (inputMode.value) {
      case 'text':
        result = await optimizeApi.optimizeText(textContent.value)
        break
      case 'file':
        result = await optimizeApi.optimizeFile(uploadedFile.value!)
        break
      case 'existing':
        result = await optimizeApi.optimizeResume(selectedResumeId.value!)
        break
      default:
        return
    }

    optimizationResult.value = result
    // 初始化所有建议为 pending 状态
    const statuses: Record<string, 'pending' | 'accepted' | 'rejected'> = {}
    result.suggestions.forEach(s => {
      statuses[s.id] = 'pending'
    })
    suggestionStatuses.value = statuses
    pageState.value = 'result'
  } catch (e) {
    pageState.value = 'input'
    // 错误已由拦截器处理
  }
}

// 接受建议
function handleAccept(id: string) {
  suggestionStatuses.value[id] = 'accepted'
}

// 拒绝建议
function handleReject(id: string) {
  suggestionStatuses.value[id] = 'rejected'
}

// 一键全部接受
function acceptAll() {
  const statuses = { ...suggestionStatuses.value }
  Object.keys(statuses).forEach(id => {
    statuses[id] = 'accepted'
  })
  suggestionStatuses.value = statuses
}

// 应用选中建议
async function applySuggestions() {
  if (acceptedSuggestionIds.value.length === 0) {
    ElMessage.warning('请至少接受一条优化建议')
    return
  }

  // 仅在选择已有简历时可应用
  if (inputMode.value !== 'existing' || !selectedResumeId.value) {
    ElMessage.info('已接受的优化建议已记录')
    return
  }

  applyLoading.value = true
  try {
    await optimizeApi.applySuggestions({
      resumeId: selectedResumeId.value,
      acceptedSuggestionIds: acceptedSuggestionIds.value
    })
    ElMessage.success('优化建议已成功应用到简历')
  } catch (e) {
    // 错误已由拦截器处理
  } finally {
    applyLoading.value = false
  }
}

// 重新优化
function resetOptimize() {
  pageState.value = 'input'
  optimizationResult.value = null
  suggestionStatuses.value = {}
}

// 获取评分颜色
function getScoreColor(score: number): string {
  if (score >= 85) return '#67C23A'
  if (score >= 70) return '#409EFF'
  if (score >= 55) return '#E6A23C'
  return '#F56C6C'
}
</script>

<template>
  <div class="resume-optimizer">
    <!-- 输入阶段 -->
    <template v-if="pageState === 'input'">
      <div class="page-header">
        <h2>简历优化</h2>
        <p class="page-desc">AI 智能分析你的简历，提供专业优化建议</p>
      </div>

      <!-- 输入方式选择 -->
      <div class="input-section">
        <div class="section-label">选择输入方式：</div>
        <el-radio-group :model-value="inputMode" @change="handleModeChange" size="large">
          <el-radio-button value="text">
            <el-icon><Document /></el-icon>
            <span>粘贴文本</span>
          </el-radio-button>
          <el-radio-button value="file">
            <el-icon><Upload /></el-icon>
            <span>上传文件</span>
          </el-radio-button>
          <el-radio-button value="existing">
            <el-icon><FolderOpened /></el-icon>
            <span>选择已有简历</span>
          </el-radio-button>
        </el-radio-group>
      </div>

      <!-- 输入内容区 -->
      <div class="content-section">
        <!-- 粘贴文本 -->
        <div v-if="inputMode === 'text'" class="input-panel">
          <el-input
            v-model="textContent"
            type="textarea"
            :rows="12"
            placeholder="请将你的简历内容粘贴到这里..."
            resize="vertical"
          />
        </div>

        <!-- 上传文件 -->
        <div v-else-if="inputMode === 'file'" class="input-panel">
          <FileUploader ref="fileUploaderRef" @upload="handleFileUpload" />
        </div>

        <!-- 选择已有简历 -->
        <div v-else-if="inputMode === 'existing'" class="input-panel">
          <el-select
            v-model="selectedResumeId"
            placeholder="请选择一份已有的简历"
            size="large"
            style="width: 100%"
            :loading="loadingResumes"
          >
            <el-option
              v-for="resume in resumeList"
              :key="resume.id"
              :label="resume.title"
              :value="resume.id"
            >
              <div class="resume-option">
                <span class="resume-title">{{ resume.title }}</span>
                <span class="resume-date">{{ resume.updatedAt }}</span>
              </div>
            </el-option>
          </el-select>
        </div>
      </div>

      <!-- 开始优化按钮 -->
      <div class="action-section">
        <el-button
          type="primary"
          size="large"
          :disabled="!canStartOptimize"
          @click="startOptimize"
        >
          开始优化
        </el-button>
      </div>
    </template>

    <!-- Loading 阶段 -->
    <template v-else-if="pageState === 'loading'">
      <div class="loading-section">
        <el-icon class="loading-icon" :size="48">
          <Loading />
        </el-icon>
        <h3>AI 正在分析你的简历...</h3>
        <p class="loading-desc">正在从多个维度进行深度分析，预计需要 10-30 秒</p>
        <el-progress :percentage="undefined" status="success" :indeterminate="true" />
      </div>
    </template>

    <!-- 结果阶段 -->
    <template v-else-if="pageState === 'result' && optimizationResult">
      <div class="result-header">
        <div class="result-title">
          <h2>优化结果</h2>
          <div class="score-badge" :style="{ borderColor: getScoreColor(optimizationResult.overallScore) }">
            <span class="score-value" :style="{ color: getScoreColor(optimizationResult.overallScore) }">
              {{ optimizationResult.overallScore }}
            </span>
            <span class="score-label">/ 100</span>
          </div>
        </div>
        <div class="result-actions">
          <el-button @click="acceptAll" type="success" plain>
            一键全部接受
          </el-button>
          <el-button @click="resetOptimize">
            重新优化
          </el-button>
        </div>
      </div>

      <!-- 统计信息 -->
      <div class="result-stats">
        <span>共 {{ totalCount }} 条建议</span>
        <el-divider direction="vertical" />
        <span class="stat-accepted">已接受 {{ acceptedCount }} 条</span>
      </div>

      <!-- 建议列表 -->
      <div class="suggestions-list">
        <OptimizationDiff
          v-for="suggestion in optimizationResult.suggestions"
          :key="suggestion.id"
          :suggestion="suggestion"
          :status="suggestionStatuses[suggestion.id] || 'pending'"
          @accept="handleAccept"
          @reject="handleReject"
        />
      </div>

      <!-- 应用按钮 -->
      <div class="apply-section">
        <el-button
          type="primary"
          size="large"
          :loading="applyLoading"
          :disabled="acceptedCount === 0"
          @click="applySuggestions"
        >
          应用选中建议（{{ acceptedCount }} 条）
        </el-button>
      </div>
    </template>
  </div>
</template>

<style lang="scss" scoped>
.resume-optimizer {
  max-width: 900px;
  margin: 0 auto;
  padding: 24px;
  animation: fadeInUp 0.4s ease both;

  .page-header {
    margin-bottom: 32px;

    h2 {
      margin: 0 0 8px 0;
      font-size: 28px;
      font-weight: 600;
      color: var(--color-text-primary);
      font-family: var(--font-heading);
    }

    .page-desc {
      margin: 0;
      font-size: 14px;
      color: var(--color-text-secondary);
    }
  }

  .input-section {
    margin-bottom: 24px;

    .section-label {
      margin-bottom: 12px;
      font-size: 14px;
      font-weight: 600;
      color: var(--color-text-regular);
    }

    .el-radio-button {
      :deep(.el-radio-button__inner) {
        display: flex;
        align-items: center;
        gap: 6px;
      }
    }
  }

  .content-section {
    margin-bottom: 24px;

    .input-panel {
      min-height: 200px;
    }
  }

  .action-section {
    display: flex;
    justify-content: center;
    padding-top: 16px;
  }

  // Loading 状态
  .loading-section {
    display: flex;
    flex-direction: column;
    align-items: center;
    justify-content: center;
    min-height: 400px;
    text-align: center;

    .loading-icon {
      color: var(--color-accent);
      animation: spin 1.5s linear infinite;
      margin-bottom: 24px;
    }

    h3 {
      margin: 0 0 12px 0;
      font-size: 22px;
      color: var(--color-text-primary);
      font-family: var(--font-heading);
    }

    .loading-desc {
      margin: 0 0 32px 0;
      font-size: 14px;
      color: var(--color-text-secondary);
    }

    .el-progress {
      width: 300px;
    }
  }

  // 结果阶段
  .result-header {
    display: flex;
    justify-content: space-between;
    align-items: center;
    margin-bottom: 24px;
    flex-wrap: wrap;
    gap: 16px;
    animation: fadeInUp 0.4s ease both;

    .result-title {
      display: flex;
      align-items: center;
      gap: 16px;

      h2 {
        margin: 0;
        font-size: 28px;
        font-weight: 600;
        color: var(--color-text-primary);
        font-family: var(--font-heading);
      }

      .score-badge {
        display: flex;
        align-items: baseline;
        padding: 8px 18px;
        border: 2px solid;
        border-radius: var(--radius-full);
        background: var(--color-surface);
        box-shadow: var(--shadow-sm);

        .score-value {
          font-size: 26px;
          font-weight: 700;
          font-family: var(--font-heading);
        }

        .score-label {
          font-size: 13px;
          color: var(--color-text-tertiary);
          margin-left: 2px;
        }
      }
    }

    .result-actions {
      display: flex;
      gap: 8px;
    }
  }

  .result-stats {
    margin-bottom: 20px;
    font-size: 14px;
    color: var(--color-text-secondary);

    .stat-accepted {
      color: var(--color-success);
      font-weight: 600;
    }
  }

  .suggestions-list {
    display: flex;
    flex-direction: column;
    gap: 16px;
    margin-bottom: 32px;
  }

  .apply-section {
    display: flex;
    justify-content: center;
    padding: 24px 0;
    border-top: 1px solid var(--color-border-light);
  }

  // 简历选择选项
  .resume-option {
    display: flex;
    justify-content: space-between;
    align-items: center;
    width: 100%;

    .resume-title {
      font-size: 14px;
      font-weight: 500;
    }

    .resume-date {
      font-size: 12px;
      color: var(--color-text-tertiary);
    }
  }
}

@keyframes spin {
  from { transform: rotate(0deg); }
  to { transform: rotate(360deg); }
}
</style>
