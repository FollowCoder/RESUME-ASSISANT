<script setup lang="ts">
import { ref, watch } from 'vue'

export interface ModuleItem {
  id: string
  title: string
  icon: string
  visible: boolean
}

const props = defineProps<{
  modules: ModuleItem[]
  activeModule?: string
}>()

const emit = defineEmits<{
  'update:modules': [modules: ModuleItem[]]
  'update:activeModule': [id: string]
}>()

// 拖拽相关状态
const draggedIndex = ref<number | null>(null)
const dragOverIndex = ref<number | null>(null)

// 切换模块显隐
function toggleModule(moduleId: string) {
  const updatedModules = props.modules.map(m =>
    m.id === moduleId ? { ...m, visible: !m.visible } : m
  )
  emit('update:modules', updatedModules)
}

// 设置激活模块
function setActiveModule(moduleId: string) {
  emit('update:activeModule', moduleId)
}

// 拖拽开始
function onDragStart(index: number, event: DragEvent) {
  draggedIndex.value = index
  if (event.dataTransfer) {
    event.dataTransfer.effectAllowed = 'move'
    event.dataTransfer.setData('text/plain', index.toString())
  }
}

// 拖拽经过
function onDragOver(index: number, event: DragEvent) {
  event.preventDefault()
  dragOverIndex.value = index
}

// 拖拽放下
function onDrop(index: number) {
  if (draggedIndex.value === null || draggedIndex.value === index) {
    draggedIndex.value = null
    dragOverIndex.value = null
    return
  }

  const updatedModules = [...props.modules]
  const [draggedItem] = updatedModules.splice(draggedIndex.value, 1)
  updatedModules.splice(index, 0, draggedItem)

  emit('update:modules', updatedModules)
  draggedIndex.value = null
  dragOverIndex.value = null
}

// 拖拽结束
function onDragEnd() {
  draggedIndex.value = null
  dragOverIndex.value = null
}

// 图标映射
const iconMap: Record<string, string> = {
  'User': '👤',
  'School': '🎓',
  'Briefcase': '💼',
  'Folder': '📁',
  'Star': '⭐',
  'Document': '📄'
}

function getIcon(iconName: string): string {
  return iconMap[iconName] || '📋'
}
</script>

<template>
  <div class="module-selector">
    <div class="selector-header">
      <h3>模块管理</h3>
      <span class="hint">拖拽调整顺序</span>
    </div>

    <div class="module-list">
      <div
        v-for="(module, index) in modules"
        :key="module.id"
        class="module-item"
        :class="{
          'is-active': activeModule === module.id,
          'is-hidden': !module.visible,
          'is-drag-over': dragOverIndex === index
        }"
        draggable="true"
        @dragstart="onDragStart(index, $event)"
        @dragover="onDragOver(index, $event)"
        @drop="onDrop(index)"
        @dragend="onDragEnd"
        @click="setActiveModule(module.id)"
      >
        <div class="module-indicator"></div>
        <div class="module-icon">{{ getIcon(module.icon) }}</div>
        <div class="module-info">
          <span class="module-title">{{ module.title }}</span>
          <span class="module-status">{{ module.visible ? '显示' : '隐藏' }}</span>
        </div>
        <el-switch
          :model-value="module.visible"
          @click.stop
          @change="toggleModule(module.id)"
          size="small"
          class="module-switch"
        />
      </div>
    </div>
  </div>
</template>

<style lang="scss" scoped>
.module-selector {
  height: 100%;
  display: flex;
  flex-direction: column;
  background: var(--color-surface);
  border-radius: var(--radius-lg);
  box-shadow: var(--shadow-card);
  border: 1px solid var(--color-border-light);
  overflow: hidden;
}

.selector-header {
  padding: 18px 20px;
  border-bottom: 1px solid #E8E8E8;
  background: #E8EEFD;

  h3 {
    margin: 0 0 4px 0;
    font-size: 16px;
    font-weight: 600;
    color: #333333;
  }

  .hint {
    font-size: 12px;
    color: #999999;
  }
}

.module-list {
  flex: 1;
  overflow-y: auto;
  padding: 12px;
}

.module-item {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 14px 16px;
  margin-bottom: 8px;
  background: #FFFFFF;
  border: 1px solid #E8E8E8;
  border-radius: var(--radius-base);
  cursor: pointer;
  transition: all var(--transition-smooth);
  user-select: none;

  &:hover {
    border-color: #4A7CFF;
    box-shadow: 0 2px 8px rgba(74, 124, 255, 0.15);
  }

  &.is-active {
    border-color: #4A7CFF;
    background: #F0F5FF;
    box-shadow: 0 2px 12px rgba(74, 124, 255, 0.2);

    .module-indicator {
      background: #4A7CFF;
    }
  }

  &.is-hidden {
    opacity: 0.6;

    .module-indicator {
      background: #CCCCCC;
    }
  }

  &.is-drag-over {
    border-color: #4A7CFF;
    border-style: dashed;
    background: #F8FAFF;
  }

  &.is-dragging {
    opacity: 0.5;
    transform: scale(0.98);
  }
}

.module-indicator {
  width: 4px;
  height: 32px;
  background: #4A7CFF;
  border-radius: 2px;
  flex-shrink: 0;
  transition: background var(--transition-smooth);
}

.module-icon {
  font-size: 20px;
  flex-shrink: 0;
  width: 32px;
  height: 32px;
  display: flex;
  align-items: center;
  justify-content: center;
  background: #F5F5F5;
  border-radius: var(--radius-base);
}

.module-info {
  flex: 1;
  min-width: 0;
  display: flex;
  flex-direction: column;
  gap: 2px;
}

.module-title {
  font-size: 14px;
  font-weight: 500;
  color: #333333;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.module-status {
  font-size: 12px;
  color: #999999;
}

.module-switch {
  flex-shrink: 0;

  :deep(.el-switch__core) {
    background-color: #DCDFE6;

    &::after {
      background-color: #FFFFFF;
    }
  }

  :deep(.el-switch.is-checked .el-switch__core) {
    background-color: #4A7CFF;
  }
}
</style>
