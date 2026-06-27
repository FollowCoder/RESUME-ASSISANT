<script setup lang="ts">
import { computed } from 'vue'
import type { InterviewReportData } from '@/api/interview'

const props = defineProps<{
  report: InterviewReportData
}>()

const emit = defineEmits<{
  restart: []
  back: []
}>()

const scoreColor = computed(() => {
  const score = props.report.totalScore
  if (score >= 80) return '#67C23A'
  if (score >= 60) return '#409EFF'
  if (score >= 40) return '#E6A23C'
  return '#F56C6C'
})

const passRateType = computed(() => {
  const rate = props.report.passRate
  if (rate === '高' || rate === 'high') return 'success'
  if (rate === '中' || rate === 'medium') return 'warning'
  return 'danger'
})

const passRateLabel = computed(() => {
  const rate = props.report.passRate
  if (rate === 'high') return '高'
  if (rate === 'medium') return '中'
  if (rate === 'low') return '低'
  return rate
})

const dimensions = computed(() => [
  { label: '技术深度', value: props.report.technicalDepth, color: '#409EFF' },
  { label: '表达能力', value: props.report.communication, color: '#67C23A' },
  { label: '项目理解', value: props.report.projectUnderstanding, color: '#E6A23C' },
  { label: '应变能力', value: props.report.adaptability, color: '#9B59B6' }
])
</script>

<template>
  <div class="interview-report">
    <!-- 综合评分 -->
    <div class="score-section">
      <el-progress
        type="circle"
        :percentage="report.totalScore"
        :width="160"
        :stroke-width="12"
        :color="scoreColor"
      >
        <template #default>
          <div class="score-inner">
            <span class="score-number">{{ report.totalScore }}</span>
            <span class="score-label">综合评分</span>
          </div>
        </template>
      </el-progress>
      <div class="pass-rate">
        <span class="pass-rate-label">通过概率：</span>
        <el-tag :type="passRateType" size="large" effect="dark">
          {{ passRateLabel }}
        </el-tag>
      </div>
    </div>

    <!-- 各维度分数 -->
    <el-divider content-position="left">维度评分</el-divider>
    <div class="dimensions-section">
      <div
        v-for="dim in dimensions"
        :key="dim.label"
        class="dimension-item"
      >
        <div class="dim-header">
          <span class="dim-label">{{ dim.label }}</span>
          <span class="dim-value" :style="{ color: dim.color }">{{ dim.value }}/100</span>
        </div>
        <el-progress
          :percentage="dim.value"
          :color="dim.color"
          :stroke-width="10"
          :show-text="false"
        />
      </div>
    </div>

    <!-- 优势列表 -->
    <el-divider content-position="left">优势亮点</el-divider>
    <div class="list-section strengths-section">
      <div
        v-for="(item, index) in report.strengths"
        :key="index"
        class="list-item strength-item"
      >
        <el-icon color="#67C23A"><CircleCheck /></el-icon>
        <span>{{ item }}</span>
      </div>
      <el-empty v-if="!report.strengths?.length" description="暂无数据" :image-size="40" />
    </div>

    <!-- 不足列表 -->
    <el-divider content-position="left">待改进项</el-divider>
    <div class="list-section weaknesses-section">
      <div
        v-for="(item, index) in report.weaknesses"
        :key="index"
        class="list-item weakness-item"
      >
        <el-icon color="#F56C6C"><CircleClose /></el-icon>
        <span>{{ item }}</span>
      </div>
      <el-empty v-if="!report.weaknesses?.length" description="暂无数据" :image-size="40" />
    </div>

    <!-- 改进建议 -->
    <el-divider content-position="left">改进建议</el-divider>
    <div class="list-section suggestions-section">
      <div
        v-for="(item, index) in report.suggestions"
        :key="index"
        class="list-item suggestion-item"
      >
        <el-icon color="#E6A23C"><InfoFilled /></el-icon>
        <span>{{ item }}</span>
      </div>
      <el-empty v-if="!report.suggestions?.length" description="暂无建议" :image-size="40" />
    </div>

    <!-- 总体评语 -->
    <el-divider content-position="left">总体评语</el-divider>
    <div class="comment-section">
      <el-alert
        :title="report.overallComment"
        type="info"
        :closable="false"
        show-icon
      />
    </div>

    <!-- 操作按钮 -->
    <div class="action-buttons">
      <el-button size="large" @click="emit('back')">
        返回首页
      </el-button>
      <el-button type="primary" size="large" @click="emit('restart')">
        再来一次
      </el-button>
    </div>
  </div>
</template>

<style lang="scss" scoped>
.interview-report {
  padding: 28px;
  max-width: 800px;
  margin: 0 auto;

  .score-section {
    display: flex;
    flex-direction: column;
    align-items: center;
    gap: 20px;
    padding: 32px 0;
    background: linear-gradient(135deg, rgba(45, 53, 97, 0.03) 0%, rgba(232, 148, 58, 0.03) 100%);
    border-radius: var(--radius-lg);
    margin-bottom: 24px;

    :deep(.el-progress-circle) {
      filter: drop-shadow(0 6px 16px rgba(0, 0, 0, 0.12));
    }

    .score-inner {
      display: flex;
      flex-direction: column;
      align-items: center;

      .score-number {
        font-size: 48px;
        font-weight: 700;
        background: linear-gradient(135deg, var(--color-primary) 0%, var(--color-accent) 100%);
        -webkit-background-clip: text;
        -webkit-text-fill-color: transparent;
        background-clip: text;
        font-family: var(--font-heading);
        line-height: 1;
      }

      .score-label {
        font-size: 14px;
        color: var(--color-text-secondary);
        font-weight: 500;
        margin-top: 6px;
      }
    }

    .pass-rate {
      display: flex;
      align-items: center;
      gap: 10px;

      .pass-rate-label {
        font-size: 15px;
        color: var(--color-text-regular);
        font-weight: 500;
      }

      :deep(.el-tag) {
        padding: 8px 16px;
        font-size: 14px;
        border-radius: var(--radius-full);
      }
    }
  }

  .dimensions-section {
    display: flex;
    flex-direction: column;
    gap: 20px;
    padding: 12px 0;

    .dimension-item {
      padding: 16px;
      background: var(--color-bg-warm);
      border-radius: var(--radius-base);
      transition: all var(--transition-smooth);

      &:hover {
        background: var(--color-surface);
        box-shadow: var(--shadow-sm);
      }

      .dim-header {
        display: flex;
        justify-content: space-between;
        margin-bottom: 10px;

        .dim-label {
          font-size: 15px;
          color: var(--color-text-regular);
          font-weight: 500;
        }

        .dim-value {
          font-size: 15px;
          font-weight: 700;
          font-family: var(--font-heading);
        }
      }

      :deep(.el-progress-bar__outer) {
        border-radius: var(--radius-full);
      }

      :deep(.el-progress-bar__inner) {
        border-radius: var(--radius-full);
      }
    }
  }

  .list-section {
    display: flex;
    flex-direction: column;
    gap: 12px;
    padding: 12px 0;

    .list-item {
      display: flex;
      align-items: flex-start;
      gap: 12px;
      font-size: 14px;
      color: var(--color-text-regular);
      line-height: 1.7;
      padding: 14px 16px;
      background: var(--color-bg-warm);
      border-radius: var(--radius-base);
      transition: all var(--transition-smooth);
      border-left: 3px solid transparent;

      &:hover {
        background: var(--color-surface);
        box-shadow: var(--shadow-sm);
        transform: translateX(4px);
      }

      .el-icon {
        margin-top: 3px;
        flex-shrink: 0;
      }
    }

    &.strengths-section .list-item {
      border-left-color: var(--color-success);
    }

    &.weaknesses-section .list-item {
      border-left-color: var(--color-danger);
    }

    &.suggestions-section .list-item {
      border-left-color: var(--color-accent);
    }
  }

  .comment-section {
    padding: 8px 0;
  }

  .action-buttons {
    display: flex;
    justify-content: center;
    gap: 16px;
    padding: 32px 0 8px;

    .el-button {
      padding: 12px 28px;
      font-weight: 600;
      border-radius: var(--radius-base);
      transition: all var(--transition-smooth);

      &:hover {
        transform: translateY(-2px);
        box-shadow: var(--shadow-md);
      }

      &.el-button--primary {
        background: linear-gradient(135deg, var(--color-accent) 0%, var(--color-accent-dark) 100%);
        border: none;
      }
    }
  }
}
</style>
