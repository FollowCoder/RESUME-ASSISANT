<script setup lang="ts">
import type { MatchDimensions } from '@/api/types'

const props = defineProps<{
  dimensions: MatchDimensions
}>()

const dimensionList = computed(() => [
  { key: 'skillMatch', label: '技能匹配', score: props.dimensions.skillMatch },
  { key: 'experienceMatch', label: '经验匹配', score: props.dimensions.experienceMatch },
  { key: 'educationMatch', label: '教育匹配', score: props.dimensions.educationMatch },
  { key: 'keywordCoverage', label: '关键词覆盖', score: props.dimensions.keywordCoverage }
])

function getColor(score: number): string {
  if (score >= 85) return '#67C23A'
  if (score >= 70) return '#409EFF'
  if (score >= 55) return '#E6A23C'
  return '#F56C6C'
}

import { computed } from 'vue'
</script>

<template>
  <div class="score-radar">
    <h4 class="radar-title">维度分析</h4>
    <div class="dimension-scores">
      <div v-for="item in dimensionList" :key="item.key" class="dimension-item">
        <div class="dimension-header">
          <span class="label">{{ item.label }}</span>
          <span class="score" :style="{ color: getColor(item.score) }">{{ item.score }}分</span>
        </div>
        <el-progress
          :percentage="item.score"
          :color="getColor(item.score)"
          :stroke-width="12"
          :show-text="false"
        />
      </div>
    </div>
  </div>
</template>

<style lang="scss" scoped>
.score-radar {
  .radar-title {
    margin: 0 0 20px 0;
    font-size: 20px;
    font-weight: 600;
    color: var(--color-text-primary);
    font-family: var(--font-heading);
    padding-bottom: 12px;
    border-bottom: 2px solid var(--color-accent);
    position: relative;

    &::after {
      content: '';
      position: absolute;
      bottom: -2px;
      left: 0;
      width: 40px;
      height: 2px;
      background: var(--color-primary);
    }
  }

  .dimension-scores {
    display: flex;
    flex-direction: column;
    gap: 20px;
  }

  .dimension-item {
    padding: 16px;
    background: var(--color-bg-warm);
    border-radius: var(--radius-base);
    transition: all var(--transition-smooth);

    &:hover {
      background: var(--color-surface);
      box-shadow: var(--shadow-sm);
      transform: translateX(4px);
    }

    .dimension-header {
      display: flex;
      justify-content: space-between;
      align-items: center;
      margin-bottom: 10px;

      .label {
        font-size: 15px;
        color: var(--color-text-regular);
        font-weight: 500;
      }

      .score {
        font-size: 16px;
        font-weight: 700;
        font-family: var(--font-heading);
      }
    }

    :deep(.el-progress-bar__outer) {
      border-radius: var(--radius-full);
      height: 12px !important;
    }

    :deep(.el-progress-bar__inner) {
      border-radius: var(--radius-full);
    }
  }
}
</style>
