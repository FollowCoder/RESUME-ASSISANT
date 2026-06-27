<script setup lang="ts">
import { computed } from 'vue'
import { useRouter } from 'vue-router'
import type { MatchAnalysisResult } from '@/api/types'
import ScoreRadar from './ScoreRadar.vue'

const props = defineProps<{
  result: MatchAnalysisResult
}>()

const router = useRouter()

const scoreColor = computed(() => {
  const score = props.result.totalScore
  if (score >= 85) return '#67C23A'
  if (score >= 70) return '#409EFF'
  if (score >= 55) return '#E6A23C'
  return '#F56C6C'
})

const levelType = computed(() => {
  const level = props.result.level
  if (level === '极佳') return 'success' as const
  if (level === '良好') return 'info' as const
  if (level === '一般') return 'warning' as const
  return 'danger' as const
})

function goOptimize() {
  router.push('/resume-optimizer')
}
</script>

<template>
  <div class="match-report">
    <!-- 总分区域 -->
    <div class="score-section">
      <div class="score-circle">
        <el-progress
          type="circle"
          :percentage="result.totalScore"
          :width="140"
          :stroke-width="10"
          :color="scoreColor"
        >
          <template #default>
            <div class="score-inner">
              <span class="score-number">{{ result.totalScore }}</span>
              <span class="score-label">匹配分</span>
            </div>
          </template>
        </el-progress>
      </div>
      <el-tag :type="levelType" size="large" class="level-tag">
        {{ result.level }}
      </el-tag>
    </div>

    <!-- 维度分析 -->
    <el-divider />
    <ScoreRadar :dimensions="result.dimensions" />

    <!-- 差距分析 -->
    <el-divider />
    <div class="gaps-section">
      <h4 class="section-title">差距分析</h4>
      <div v-if="result.gaps && result.gaps.length > 0" class="gaps-list">
        <el-card
          v-for="(gap, index) in result.gaps"
          :key="index"
          shadow="never"
          class="gap-card"
        >
          <div class="gap-header">
            <el-tag size="small" type="info">{{ gap.category }}</el-tag>
          </div>
          <div class="gap-requirement">
            <strong>要求：</strong>{{ gap.requirement }}
          </div>
          <div class="gap-suggestion">
            <strong>建议：</strong>{{ gap.suggestion }}
          </div>
        </el-card>
      </div>
      <el-empty v-else description="无明显差距，表现优秀！" :image-size="60" />
    </div>

    <!-- 改进建议 -->
    <el-divider />
    <div class="improvements-section">
      <h4 class="section-title">改进建议</h4>
      <div v-if="result.improvements && result.improvements.length > 0">
        <el-timeline>
          <el-timeline-item
            v-for="(item, index) in result.improvements"
            :key="index"
            :timestamp="`建议 ${index + 1}`"
            placement="top"
          >
            <div class="improvement-item">
              <span class="improvement-text">{{ item }}</span>
              <el-button type="primary" link size="small" @click="goOptimize">
                去优化
              </el-button>
            </div>
          </el-timeline-item>
        </el-timeline>
      </div>
      <el-empty v-else description="暂无改进建议" :image-size="60" />
    </div>
  </div>
</template>

<style lang="scss" scoped>
.match-report {
  .score-section {
    display: flex;
    flex-direction: column;
    align-items: center;
    gap: 14px;
    padding: 24px 0;

    .score-circle {
      position: relative;

      :deep(.el-progress-circle) {
        filter: drop-shadow(0 4px 12px rgba(0, 0, 0, 0.1));
      }

      .score-inner {
        display: flex;
        flex-direction: column;
        align-items: center;

        .score-number {
          font-size: 42px;
          font-weight: 700;
          background: linear-gradient(135deg, var(--color-primary) 0%, var(--color-accent) 100%);
          -webkit-background-clip: text;
          -webkit-text-fill-color: transparent;
          background-clip: text;
          font-family: var(--font-heading);
          line-height: 1;
        }

        .score-label {
          font-size: 13px;
          color: var(--color-text-secondary);
          font-weight: 500;
          margin-top: 4px;
        }
      }
    }

    .level-tag {
      font-size: 15px;
      font-weight: 600;
      padding: 8px 20px;
      border-radius: var(--radius-full);
      letter-spacing: 0.5px;
    }
  }

  .section-title {
    margin: 0 0 16px 0;
    font-size: 18px;
    font-weight: 600;
    color: var(--color-text-primary);
    font-family: var(--font-heading);
  }

  .gaps-list {
    display: flex;
    flex-direction: column;
    gap: 14px;

    .gap-card {
      border-left: 4px solid var(--color-accent);
      border-radius: var(--radius-base);
      transition: all var(--transition-smooth);
      background: var(--color-surface);

      &:hover {
        transform: translateX(4px);
        box-shadow: var(--shadow-md);
      }

      :deep(.el-card__body) {
        padding: 16px 20px;
      }

      .gap-header {
        margin-bottom: 10px;
      }

      .gap-requirement,
      .gap-suggestion {
        font-size: 14px;
        color: var(--color-text-regular);
        line-height: 1.7;
        margin-top: 6px;
      }
    }
  }

  .improvements-section {
    :deep(.el-timeline) {
      padding-left: 8px;
    }

    :deep(.el-timeline-item__node) {
      background: linear-gradient(135deg, var(--color-accent) 0%, var(--color-accent-light) 100%);
      border: none;
    }

    :deep(.el-timeline-item__tail) {
      border-left: 2px dashed var(--color-border);
    }

    .improvement-item {
      display: flex;
      align-items: center;
      justify-content: space-between;
      gap: 12px;
      padding: 12px 16px;
      background: var(--color-bg-warm);
      border-radius: var(--radius-base);
      transition: all var(--transition-smooth);

      &:hover {
        background: var(--color-surface);
        box-shadow: var(--shadow-sm);
      }

      .improvement-text {
        font-size: 14px;
        color: var(--color-text-regular);
        flex: 1;
        line-height: 1.6;
      }
    }
  }
}
</style>
