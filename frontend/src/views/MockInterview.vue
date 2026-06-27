<script setup lang="ts">
import { ref, onMounted, onBeforeUnmount } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { resumeApi } from '@/api/resume'
import { interviewApi } from '@/api/interview'
import type { InterviewHistoryItem, InterviewReportData } from '@/api/interview'
import type { ResumeListItem } from '@/api/types'
import { useInterviewStore } from '@/stores/interview'
import InterviewChat from '@/components/InterviewChat.vue'
import InterviewReport from '@/components/InterviewReport.vue'
import VoiceRecorder from '@/components/VoiceRecorder.vue'

// 页面状态: prepare | interviewing | report
const pageState = ref<'prepare' | 'interviewing' | 'report'>('prepare')

// 表单数据
const selectedResumeId = ref<number | undefined>(undefined)
const jdContent = ref('')
const interviewMode = ref<'text' | 'voice'>('text')

// 加载状态
const loadingResumes = ref(false)
const loadingHistory = ref(false)
const starting = ref(false)

// 数据
const resumeList = ref<ResumeListItem[]>([])
const historyList = ref<InterviewHistoryItem[]>([])
const currentReport = ref<InterviewReportData | null>(null)

const interviewStore = useInterviewStore()

// 加载简历列表
async function loadResumes() {
  loadingResumes.value = true
  try {
    const data = await resumeApi.getList()
    resumeList.value = data
  } catch {
    // 错误已在拦截器中处理
  } finally {
    loadingResumes.value = false
  }
}

// 加载面试历史
async function loadHistory() {
  loadingHistory.value = true
  try {
    const data = await interviewApi.getHistory()
    historyList.value = data
  } catch {
    // 错误已在拦截器中处理
  } finally {
    loadingHistory.value = false
  }
}

// 开始面试
async function handleStart() {
  if (!selectedResumeId.value) {
    ElMessage.warning('请选择一份简历')
    return
  }
  if (!jdContent.value.trim()) {
    ElMessage.warning('请输入目标 JD 内容')
    return
  }

  starting.value = true
  try {
    const success = await interviewStore.startInterview(
      selectedResumeId.value,
      jdContent.value.trim(),
      interviewMode.value
    )
    if (success) {
      pageState.value = 'interviewing'
      ElMessage.success(interviewMode.value === 'voice' 
        ? '语音面试已开始，请等待面试官提问后按住按钮说话' 
        : '面试已开始，请等待面试官提问'
      )
    }
  } finally {
    starting.value = false
  }
}

// 发送消息
function handleSendMessage(content: string) {
  interviewStore.sendMessage(content)
}

// 语音模式：开始录音
function handleStartRecording() {
  interviewStore.startRecording()
}

// 语音模式：停止录音
function handleStopRecording() {
  interviewStore.stopRecording()
}

// 结束面试
async function handleEndInterview() {
  try {
    await ElMessageBox.confirm(
      '确定要结束本次面试吗？结束后将生成评估报告。',
      '结束面试',
      { confirmButtonText: '确认结束', cancelButtonText: '继续面试', type: 'warning' }
    )
  } catch {
    return
  }

  const report = await interviewStore.endInterview()
  if (report) {
    currentReport.value = report
    pageState.value = 'report'
  }
}

// 查看历史报告
async function handleViewReport(row: InterviewHistoryItem) {
  if (row.status !== 'completed') {
    ElMessage.info('该面试尚未完成，无法查看报告')
    return
  }
  const report = await interviewStore.loadReport(row.id)
  if (report) {
    currentReport.value = report
    pageState.value = 'report'
  }
}

// 返回首页
function handleBack() {
  interviewStore.reset()
  currentReport.value = null
  pageState.value = 'prepare'
  loadHistory()
}

// 再来一次
function handleRestart() {
  interviewStore.reset()
  currentReport.value = null
  pageState.value = 'prepare'
}

// 格式化时间
function formatTime(dateStr: string): string {
  if (!dateStr) return '-'
  const date = new Date(dateStr)
  return date.toLocaleString('zh-CN', {
    year: 'numeric',
    month: '2-digit',
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit'
  })
}

// 获取状态标签类型
function getStatusType(status: string): 'success' | 'warning' | 'info' {
  if (status === 'completed') return 'success'
  if (status === 'in_progress') return 'warning'
  return 'info'
}

// 获取状态文本
function getStatusText(status: string): string {
  if (status === 'completed') return '已完成'
  if (status === 'in_progress') return '进行中'
  return status
}

// 获取分数颜色
function getScoreColor(score: number): string {
  if (score >= 80) return '#67C23A'
  if (score >= 60) return '#409EFF'
  if (score >= 40) return '#E6A23C'
  return '#F56C6C'
}

onMounted(() => {
  loadResumes()
  loadHistory()
})

onBeforeUnmount(() => {
  // 如果面试中离开页面，断开连接
  if (interviewStore.isInterviewing) {
    interviewStore.disconnect()
  }
})
</script>

<template>
  <div class="mock-interview">
    <!-- ===== 准备阶段 ===== -->
    <template v-if="pageState === 'prepare'">
      <h2 class="page-title">模拟面试</h2>

      <el-card shadow="never" class="prepare-card">
        <template #header>
          <span class="card-title">面试准备</span>
        </template>

        <el-form label-position="top" class="prepare-form">
          <el-form-item label="选择简历" required>
            <el-select
              v-model="selectedResumeId"
              placeholder="请选择一份简历"
              :loading="loadingResumes"
              style="width: 100%"
              size="large"
            >
              <el-option
                v-for="resume in resumeList"
                :key="resume.id"
                :label="resume.title"
                :value="resume.id"
              />
            </el-select>
          </el-form-item>

          <el-form-item label="目标 JD（职位描述）" required>
            <el-input
              v-model="jdContent"
              type="textarea"
              :rows="6"
              placeholder="请粘贴目标职位的 JD 内容，AI 面试官将据此进行提问..."
              resize="vertical"
            />
          </el-form-item>

          <el-form-item label="面试模式">
            <el-radio-group v-model="interviewMode">
              <el-radio value="text">文字模式</el-radio>
              <el-radio value="voice">语音模式</el-radio>
            </el-radio-group>
            <div v-if="interviewMode === 'voice'" class="mode-hint">
              语音模式需要麦克风权限，面试官将通过语音回复
            </div>
          </el-form-item>

          <el-form-item>
            <el-button
              type="primary"
              size="large"
              :loading="starting"
              :disabled="!selectedResumeId || !jdContent.trim()"
              style="width: 100%"
              @click="handleStart"
            >
              {{ starting ? '正在准备面试...' : '开始面试' }}
            </el-button>
          </el-form-item>
        </el-form>
      </el-card>

      <!-- 面试历史 -->
      <el-card shadow="never" class="history-card">
        <template #header>
          <span class="card-title">面试历史记录</span>
        </template>

        <el-table
          v-loading="loadingHistory"
          :data="historyList"
          stripe
          style="width: 100%"
          class="history-table"
        >
          <el-table-column label="简历" prop="resumeTitle" min-width="120" show-overflow-tooltip />
          <el-table-column label="JD 摘要" prop="jdSnippet" min-width="180" show-overflow-tooltip />
          <el-table-column label="状态" width="100" align="center">
            <template #default="{ row }">
              <el-tag :type="getStatusType(row.status)" size="small">
                {{ getStatusText(row.status) }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column label="评分" width="80" align="center">
            <template #default="{ row }">
              <span
                v-if="row.totalScore"
                class="score-text"
                :style="{ color: getScoreColor(row.totalScore) }"
              >
                {{ row.totalScore }}
              </span>
              <span v-else class="score-text" style="color: #909399">-</span>
            </template>
          </el-table-column>
          <el-table-column label="面试时间" width="170" align="center">
            <template #default="{ row }">
              {{ formatTime(row.startedAt) }}
            </template>
          </el-table-column>
          <el-table-column label="操作" width="100" align="center">
            <template #default="{ row }">
              <el-button
                type="primary"
                link
                size="small"
                :disabled="row.status !== 'completed'"
                @click="handleViewReport(row)"
              >
                查看报告
              </el-button>
            </template>
          </el-table-column>
        </el-table>

        <el-empty
          v-if="!loadingHistory && historyList.length === 0"
          description="暂无面试记录"
          :image-size="60"
        />
      </el-card>
    </template>

    <!-- ===== 面试进行中 ===== -->
    <template v-if="pageState === 'interviewing'">
      <div class="interview-header">
        <div class="header-left">
          <h2 class="page-title" style="margin: 0">面试进行中</h2>
          <el-tag
            :type="interviewStore.isConnected ? 'success' : 'danger'"
            size="small"
            effect="dark"
          >
            {{ interviewStore.isConnected ? '已连接' : '连接断开' }}
          </el-tag>
        </div>
        <el-button
          type="danger"
          :loading="interviewStore.loading"
          @click="handleEndInterview"
        >
          结束面试
        </el-button>
      </div>

      <div class="chat-area">
        <InterviewChat
          :messages="interviewStore.messages"
          @send="handleSendMessage"
        />

        <!-- 语音模式：录音控制区域 -->
        <div v-if="interviewStore.isVoiceMode" class="voice-control-area">
          <!-- TTS 播放状态 -->
          <div v-if="interviewStore.isTtsPlaying" class="tts-playing-hint">
            <span class="sound-wave"></span>
            面试官正在说话...
          </div>

          <VoiceRecorder
            :disabled="interviewStore.isTtsPlaying"
            @start-recording="handleStartRecording"
            @stop-recording="handleStopRecording"
          />
        </div>
      </div>
    </template>

    <!-- ===== 报告阶段 ===== -->
    <template v-if="pageState === 'report'">
      <h2 class="page-title">面试评估报告</h2>
      <el-card shadow="never" class="report-card">
        <InterviewReport
          v-if="currentReport"
          :report="currentReport"
          @back="handleBack"
          @restart="handleRestart"
        />
      </el-card>
    </template>
  </div>
</template>

<style lang="scss" scoped>
.mock-interview {
  padding: 24px;
  max-width: 1200px;
  margin: 0 auto;
  height: 100%;
  display: flex;
  flex-direction: column;
  animation: fadeInUp 0.4s ease both;

  .page-title {
    margin: 0 0 20px 0;
    font-size: 28px;
    font-weight: 600;
    color: var(--color-text-primary);
    font-family: var(--font-heading);
  }

  .card-title {
    font-size: 18px;
    font-weight: 600;
    color: var(--color-text-primary);
    font-family: var(--font-heading);
  }

  .prepare-card {
    margin-bottom: 24px;
    border-radius: var(--radius-lg);
    border: 1px solid var(--color-border-light);

    .prepare-form {
      max-width: 600px;
    }
  }

  .history-card {
    border-radius: var(--radius-lg);
    border: 1px solid var(--color-border-light);

    .history-table {
      .score-text {
        font-size: 16px;
        font-weight: 700;
        font-family: var(--font-heading);
      }
    }
  }

  // 面试进行中
  .interview-header {
    display: flex;
    align-items: center;
    justify-content: space-between;
    margin-bottom: 16px;

    .header-left {
      display: flex;
      align-items: center;
      gap: 12px;
    }
  }

  .chat-area {
    flex: 1;
    min-height: 0;
    border: 1px solid var(--color-border-light);
    border-radius: var(--radius-lg);
    overflow: hidden;
    display: flex;
    flex-direction: column;
    height: calc(100vh - 200px);
    max-height: calc(100vh - 200px);
    box-shadow: var(--shadow-card);
  }

  .voice-control-area {
    border-top: 1px solid var(--color-border-light);
    padding: 14px 20px;
    background: var(--color-bg-warm);
    flex-shrink: 0;

    .tts-playing-hint {
      display: flex;
      align-items: center;
      gap: 8px;
      padding: 10px 14px;
      margin-bottom: 8px;
      color: var(--color-success);
      font-size: 13px;
      font-weight: 500;

      .sound-wave {
        width: 12px;
        height: 12px;
        border-radius: 50%;
        background: var(--color-success);
        animation: pulse-green 1s infinite;
      }
    }
  }

  .mode-hint {
    margin-top: 6px;
    font-size: 12px;
    color: var(--color-text-tertiary);
  }

  // 报告阶段
  .report-card {
    border-radius: var(--radius-lg);
    border: 1px solid var(--color-border-light);

    :deep(.el-card__body) {
      padding: 0;
    }
  }
}

@media (max-width: 768px) {
  .mock-interview {
    padding: 16px;

    .chat-area {
      height: calc(100vh - 180px);
    }
  }
}

@keyframes pulse-green {
  0% { transform: scale(1); opacity: 1; }
  50% { transform: scale(1.4); opacity: 0.5; }
  100% { transform: scale(1); opacity: 1; }
}
</style>
