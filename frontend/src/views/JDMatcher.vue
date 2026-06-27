<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { matchApi } from '@/api/match'
import { resumeApi } from '@/api/resume'
import type { ResumeListItem, MatchAnalysisResult, MatchHistoryItem } from '@/api/types'
import MatchReport from '@/components/MatchReport.vue'

// 表单数据
const selectedResumeId = ref<number | undefined>(undefined)
const jdContent = ref('')

// 状态
const analyzing = ref(false)
const loadingResumes = ref(false)
const loadingHistory = ref(false)
const loadingDetail = ref(false)

// 数据
const resumeList = ref<ResumeListItem[]>([])
const analysisResult = ref<MatchAnalysisResult | null>(null)
const historyList = ref<MatchHistoryItem[]>([])

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

// 加载匹配历史
async function loadHistory() {
  loadingHistory.value = true
  try {
    const data = await matchApi.getHistory()
    historyList.value = data
  } catch {
    // 错误已在拦截器中处理
  } finally {
    loadingHistory.value = false
  }
}

// 开始分析
async function handleAnalyze() {
  if (!selectedResumeId.value) {
    ElMessage.warning('请选择一份简历')
    return
  }
  if (!jdContent.value.trim()) {
    ElMessage.warning('请输入 JD 内容')
    return
  }

  analyzing.value = true
  analysisResult.value = null
  try {
    const data = await matchApi.analyze({
      resumeId: selectedResumeId.value,
      jdContent: jdContent.value.trim()
    })
    analysisResult.value = data
    ElMessage.success('分析完成')
    // 刷新历史记录
    loadHistory()
  } catch {
    // 错误已在拦截器中处理
  } finally {
    analyzing.value = false
  }
}

// 查看历史详情
async function handleViewDetail(row: MatchHistoryItem) {
  loadingDetail.value = true
  try {
    const data = await matchApi.getDetail(row.id)
    analysisResult.value = data
  } catch {
    // 错误已在拦截器中处理
  } finally {
    loadingDetail.value = false
  }
}

// 获取分数对应颜色
function getScoreColor(score: number): string {
  if (score >= 85) return '#67C23A'
  if (score >= 70) return '#409EFF'
  if (score >= 55) return '#E6A23C'
  return '#F56C6C'
}

// 获取等级标签类型
function getLevelType(level: string): 'success' | 'warning' | 'danger' | 'info' {
  if (level === '极佳') return 'success'
  if (level === '良好') return 'info'
  if (level === '一般') return 'warning'
  return 'danger'
}

// 格式化时间
function formatTime(dateStr: string): string {
  if (!dateStr) return ''
  const date = new Date(dateStr)
  return date.toLocaleString('zh-CN', {
    year: 'numeric',
    month: '2-digit',
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit'
  })
}

onMounted(() => {
  loadResumes()
  loadHistory()
})
</script>

<template>
  <div class="jd-matcher">
    <h2 class="page-title">JD 匹配度分析</h2>

    <!-- 主内容区：左侧输入 + 右侧结果 -->
    <el-row :gutter="24" class="main-content">
      <!-- 左侧输入区 -->
      <el-col :xs="24" :sm="24" :md="10" :lg="9">
        <el-card shadow="never" class="input-card">
          <template #header>
            <span class="card-title">输入信息</span>
          </template>

          <el-form label-position="top">
            <el-form-item label="选择简历">
              <el-select
                v-model="selectedResumeId"
                placeholder="请选择一份简历"
                :loading="loadingResumes"
                style="width: 100%"
              >
                <el-option
                  v-for="resume in resumeList"
                  :key="resume.id"
                  :label="resume.title"
                  :value="resume.id"
                />
              </el-select>
            </el-form-item>

            <el-form-item label="JD 内容">
              <el-input
                v-model="jdContent"
                type="textarea"
                :rows="10"
                placeholder="请粘贴职位描述（JD）内容..."
                resize="vertical"
              />
            </el-form-item>

            <el-form-item>
              <el-button
                type="primary"
                :loading="analyzing"
                :disabled="!selectedResumeId || !jdContent.trim()"
                style="width: 100%"
                size="large"
                @click="handleAnalyze"
              >
                {{ analyzing ? '正在分析中...' : '开始分析' }}
              </el-button>
            </el-form-item>
          </el-form>

          <el-alert
            v-if="analyzing"
            title="AI 正在深度分析简历与 JD 的匹配程度，请稍候..."
            type="info"
            :closable="false"
            show-icon
          />
        </el-card>
      </el-col>

      <!-- 右侧结果区 -->
      <el-col :xs="24" :sm="24" :md="14" :lg="15">
        <el-card
          v-loading="loadingDetail"
          shadow="never"
          class="result-card"
        >
          <template #header>
            <span class="card-title">分析结果</span>
          </template>

          <div v-if="analysisResult">
            <MatchReport :result="analysisResult" />
          </div>

          <el-empty
            v-else
            description="请选择简历并输入 JD 内容后开始分析"
            :image-size="120"
          />
        </el-card>
      </el-col>
    </el-row>

    <!-- 历史记录 -->
    <el-card shadow="never" class="history-card">
      <template #header>
        <span class="card-title">匹配历史记录</span>
      </template>

      <el-table
        v-loading="loadingHistory"
        :data="historyList"
        stripe
        style="width: 100%"
        @row-click="handleViewDetail"
        class="history-table"
      >
        <el-table-column label="简历标题" prop="resumeTitle" min-width="150" />
        <el-table-column label="JD 摘要" prop="jdSnippet" min-width="200" show-overflow-tooltip />
        <el-table-column label="匹配分数" prop="matchScore" width="120" align="center">
          <template #default="{ row }">
            <span
              class="score-text"
              :style="{ color: getScoreColor(row.matchScore) }"
            >
              {{ row.matchScore }}
            </span>
          </template>
        </el-table-column>
        <el-table-column label="等级" prop="level" width="100" align="center">
          <template #default="{ row }">
            <el-tag :type="getLevelType(row.level)" size="small">
              {{ row.level }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="分析时间" prop="createdAt" width="180" align="center">
          <template #default="{ row }">
            {{ formatTime(row.createdAt) }}
          </template>
        </el-table-column>
      </el-table>

      <el-empty
        v-if="!loadingHistory && historyList.length === 0"
        description="暂无匹配历史"
        :image-size="60"
      />
    </el-card>
  </div>
</template>

<style lang="scss" scoped>
.jd-matcher {
  padding: 24px;
  max-width: 1400px;
  margin: 0 auto;
  animation: fadeInUp 0.4s ease both;

  .page-title {
    margin: 0 0 24px 0;
    font-size: 28px;
    font-weight: 600;
    color: var(--color-text-primary);
    font-family: var(--font-heading);
  }

  .card-title {
    font-size: 16px;
    font-weight: 600;
    color: var(--color-text-primary);
    font-family: var(--font-heading);
    font-size: 18px;
  }

  .main-content {
    margin-bottom: 24px;
  }

  .input-card,
  .result-card {
    height: 100%;
    min-height: 500px;
    border-radius: var(--radius-lg);
    border: 1px solid var(--color-border-light);
  }

  .result-card {
    :deep(.el-card__body) {
      max-height: 600px;
      overflow-y: auto;
    }
  }

  .history-card {
    border-radius: var(--radius-lg);
    border: 1px solid var(--color-border-light);

    .history-table {
      cursor: pointer;

      .score-text {
        font-size: 16px;
        font-weight: 700;
        font-family: var(--font-heading);
      }
    }
  }
}

// 响应式适配
@media (max-width: 768px) {
  .jd-matcher {
    padding: 16px;

    .main-content {
      .el-col {
        margin-bottom: 16px;
      }
    }

    .input-card,
    .result-card {
      min-height: auto;
    }
  }
}
</style>
