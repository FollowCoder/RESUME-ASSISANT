<script setup lang="ts">
import type { OptimizationSuggestion } from '@/api/optimize'
import { Check, Close, InfoFilled } from '@element-plus/icons-vue'

interface Props {
  suggestion: OptimizationSuggestion
  status: 'pending' | 'accepted' | 'rejected'
}

defineProps<Props>()

const emit = defineEmits<{
  accept: [id: string]
  reject: [id: string]
}>()

const dimensionMap: Record<string, string> = {
  summary: '个人总结',
  work: '工作经历',
  project: '项目经验',
  skills: '技能清单',
  education: '教育背景',
  structure: '整体结构'
}

const severityType: Record<string, string> = {
  high: 'danger',
  medium: 'warning',
  low: 'info'
}

const severityLabel: Record<string, string> = {
  high: '高优先',
  medium: '中优先',
  low: '低优先'
}

function getDimensionLabel(dimension: string): string {
  return dimensionMap[dimension] || dimension
}

function getSeverityType(severity: string): string {
  return severityType[severity] || 'info'
}

function getSeverityLabel(severity: string): string {
  return severityLabel[severity] || severity
}
</script>

<template>
  <div
    class="optimization-diff"
    :class="{
      'is-accepted': status === 'accepted',
      'is-rejected': status === 'rejected'
    }"
  >
    <!-- 头部信息 -->
    <div class="diff-header">
      <div class="diff-tags">
        <el-tag :type="(getSeverityType(suggestion.severity) as any)" size="small" effect="dark">
          {{ getSeverityLabel(suggestion.severity) }}
        </el-tag>
        <el-tag size="small">
          {{ getDimensionLabel(suggestion.dimension) }}
        </el-tag>
      </div>
      <div class="diff-status" v-if="status !== 'pending'">
        <el-tag v-if="status === 'accepted'" type="success" size="small" effect="plain">
          已接受
        </el-tag>
        <el-tag v-else type="info" size="small" effect="plain">
          已拒绝
        </el-tag>
      </div>
    </div>

    <!-- 优化理由 -->
    <div class="diff-reason">
      <el-icon><InfoFilled /></el-icon>
      <span>{{ suggestion.reason }}</span>
    </div>

    <!-- 对比区域 -->
    <div class="diff-content">
      <div class="diff-column diff-original">
        <div class="diff-column-title">原文</div>
        <div class="diff-text">{{ suggestion.original }}</div>
      </div>
      <div class="diff-arrow">→</div>
      <div class="diff-column diff-optimized">
        <div class="diff-column-title">优化后</div>
        <div class="diff-text">{{ suggestion.optimized }}</div>
      </div>
    </div>

    <!-- 操作按钮 -->
    <div class="diff-actions" v-if="status === 'pending'">
      <el-button type="success" size="small" @click="emit('accept', suggestion.id)">
        <el-icon><Check /></el-icon>
        <span>接受</span>
      </el-button>
      <el-button type="info" size="small" @click="emit('reject', suggestion.id)">
        <el-icon><Close /></el-icon>
        <span>拒绝</span>
      </el-button>
    </div>
  </div>
</template>

<style lang="scss" scoped>
.optimization-diff {
  border: 1px solid var(--color-border);
  border-radius: var(--radius-lg);
  padding: 24px;
  transition: all var(--transition-smooth);
  background: var(--color-surface);
  box-shadow: var(--shadow-sm);

  &:hover {
    box-shadow: var(--shadow-md);
  }

  &.is-accepted {
    border-color: var(--color-success);
    background: linear-gradient(135deg, rgba(103, 194, 58, 0.05) 0%, rgba(103, 194, 58, 0.02) 100%);
    box-shadow: 0 0 0 1px rgba(103, 194, 58, 0.2);
  }

  &.is-rejected {
    border-color: var(--color-border-light);
    opacity: 0.6;
    background: var(--color-bg);
    transform: scale(0.98);
  }

  .diff-header {
    display: flex;
    justify-content: space-between;
    align-items: center;
    margin-bottom: 14px;

    .diff-tags {
      display: flex;
      gap: 8px;

      :deep(.el-tag) {
        border-radius: var(--radius-full);
        font-weight: 500;
      }
    }
  }

  .diff-reason {
    display: flex;
    align-items: flex-start;
    gap: 10px;
    margin-bottom: 18px;
    padding: 14px 16px;
    background: var(--color-bg-warm);
    border-radius: var(--radius-base);
    font-size: 14px;
    color: var(--color-text-regular);
    line-height: 1.7;
    border-left: 4px solid var(--color-accent);

    .el-icon {
      margin-top: 3px;
      color: var(--color-accent);
      flex-shrink: 0;
      font-size: 16px;
    }
  }

  .diff-content {
    display: flex;
    gap: 12px;
    align-items: stretch;
    margin-bottom: 16px;

    .diff-arrow {
      display: flex;
      align-items: center;
      font-size: 20px;
      color: var(--color-text-tertiary);
      flex-shrink: 0;
    }

    .diff-column {
      flex: 1;
      border-radius: var(--radius-base);
      padding: 14px;
      min-height: 60px;

      .diff-column-title {
        font-size: 11px;
        font-weight: 700;
        margin-bottom: 8px;
        text-transform: uppercase;
        letter-spacing: 0.5px;
      }

      .diff-text {
        font-size: 14px;
        line-height: 1.7;
        white-space: pre-wrap;
        word-break: break-word;
      }
    }

    .diff-original {
      background: linear-gradient(135deg, rgba(199, 84, 84, 0.06) 0%, rgba(199, 84, 84, 0.02) 100%);
      border: 1px solid rgba(199, 84, 84, 0.2);

      .diff-column-title {
        color: var(--color-danger);
      }

      .diff-text {
        color: #a04444;
        text-decoration: line-through;
        text-decoration-color: rgba(160, 68, 68, 0.4);
        text-decoration-thickness: 2px;
      }
    }

    .diff-optimized {
      background: linear-gradient(135deg, rgba(91, 167, 112, 0.06) 0%, rgba(91, 167, 112, 0.02) 100%);
      border: 1px solid rgba(91, 167, 112, 0.2);

      .diff-column-title {
        color: var(--color-success);
      }

      .diff-text {
        color: #3d7a4c;
      }
    }
  }

  .diff-actions {
    display: flex;
    justify-content: flex-end;
    gap: 10px;

    .el-button {
      font-weight: 500;
      transition: all var(--transition-smooth);

      &:hover {
        transform: translateY(-1px);
        box-shadow: var(--shadow-sm);
      }
    }
  }
}
</style>
