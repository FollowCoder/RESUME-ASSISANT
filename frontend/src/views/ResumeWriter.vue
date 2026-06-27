<script setup lang="ts">
import { ref, onMounted, computed } from 'vue'
import { useResumeStore } from '@/stores/resume'
import { resumeApi } from '@/api/resume'
import type { ResumeFormData, ResumeContent, ResumeListItem } from '@/api/types'
import { ElMessage } from 'element-plus'
import ResumeForm from '@/components/ResumeForm.vue'
import ResumePreview from '@/components/ResumePreview.vue'
import ModuleSelector from '@/components/ModuleSelector.vue'
import type { ModuleItem } from '@/components/ModuleSelector.vue'

const resumeStore = useResumeStore()

const selectedTemplate = ref('modern')
const resumeList = ref<ResumeListItem[]>([])
const formResumeContent = ref<ResumeContent | null>(null)
const formResumeId = ref<number | null>(null)
const exportLoading = ref(false)
const showResumeList = ref(false)

// 模块配置
const modules = ref<ModuleItem[]>([
  { id: 'basicInfo', title: '基本信息', icon: 'User', visible: true },
  { id: 'education', title: '教育背景', icon: 'School', visible: true },
  { id: 'workExperience', title: '工作经历', icon: 'Briefcase', visible: true },
  { id: 'projectExperience', title: '项目经验', icon: 'Folder', visible: true },
  { id: 'skills', title: '技能特长', icon: 'Star', visible: true },
  { id: 'summary', title: '个人总结', icon: 'Document', visible: true }
])

// 当前激活模块
const activeModule = ref<string>('basicInfo')

// 预览缩放
const previewZoom = ref(85)

// 当前预览内容
const previewContent = computed(() => formResumeContent.value)

const currentResumeId = computed(() => formResumeId.value)

// 加载模板和简历列表
onMounted(async () => {
  await resumeStore.loadTemplates()
  loadResumeList()
})

async function loadResumeList() {
  try {
    const list = await resumeApi.getList()
    resumeList.value = list
  } catch (e) {
    // Error handled by interceptor
  }
}

// 表单模式：提交表单
async function handleFormSubmit(data: ResumeFormData) {
  try {
    // 将前端平铺数据转换为后端期望的嵌套结构
    const requestData = {
      title: data.title,
      language: data.language,
      templateId: data.templateId,
      content: {
        basicInfo: data.basicInfo,
        education: data.education,
        workExperience: data.workExperience,
        projectExperience: data.projectExperience.map(proj => ({
          ...proj,
          techStack: Array.isArray(proj.techStack) ? proj.techStack.join(', ') : proj.techStack
        })),
        skills: data.skills,
        summary: data.summary
      }
    }
    const res = await resumeApi.submitForm(requestData)
    formResumeId.value = res.id
    // 构建预览内容
    formResumeContent.value = {
      basicInfo: data.basicInfo,
      education: data.education,
      workExperience: data.workExperience,
      projectExperience: data.projectExperience,
      skills: data.skills,
      summary: data.summary
    }
    selectedTemplate.value = data.templateId
    ElMessage.success('简历生成成功！')
    loadResumeList()
  } catch (e) {
    // Error handled by interceptor
  }
}

// 导出简历
async function handleExport(format: 'pdf' | 'docx') {
  const resumeId = currentResumeId.value
  if (!resumeId) {
    ElMessage.warning('请先完成简历生成')
    return
  }
  exportLoading.value = true
  try {
    await resumeStore.exportResume(resumeId, format, selectedTemplate.value)
  } finally {
    exportLoading.value = false
  }
}

// 查看已有简历
async function handleViewResume(id: number) {
  try {
    const detail = await resumeApi.getDetail(id)
    const content = typeof detail.content === 'string' ? JSON.parse(detail.content) : detail.content
    formResumeContent.value = content
    formResumeId.value = detail.id
    selectedTemplate.value = detail.templateId || 'modern'
    ElMessage.success('已加载简历')
  } catch (e) {
    // Error handled by interceptor
  }
}

// 实时预览更新（表单输入时触发）
function handlePreviewUpdate(content: ResumeContent) {
  formResumeContent.value = content
}
</script>

<template>
  <div class="resume-writer">
    <!-- 顶部工具栏 -->
    <div class="top-toolbar">
      <div class="toolbar-left">
        <h2 class="page-title">简历编写</h2>
      </div>
      <div class="toolbar-right">
        <el-button text @click="showResumeList = !showResumeList">
          <el-icon><List /></el-icon> 我的简历
        </el-button>
      </div>
    </div>

    <!-- 三栏主内容区 -->
    <div class="main-content">
      <!-- 左侧：模块选择栏 -->
      <div class="left-sidebar">
        <ModuleSelector
          v-model:modules="modules"
          v-model:activeModule="activeModule"
        />
      </div>

      <!-- 中间：编辑区 -->
      <div class="center-panel">
        <ResumeForm
          :templates="resumeStore.templates"
          :activeModule="activeModule"
          @submit="handleFormSubmit"
          @update:preview="handlePreviewUpdate"
        />
      </div>

      <!-- 右侧：预览区 -->
      <div class="right-panel">
        <!-- 模板和缩放控制 -->
        <div class="preview-controls">
          <div class="control-row">
            <span class="control-label">模板</span>
            <el-select v-model="selectedTemplate" size="small" style="width: 120px">
              <el-option
                v-for="tpl in resumeStore.templates"
                :key="tpl.id"
                :label="tpl.name"
                :value="tpl.id"
              />
            </el-select>
          </div>
          <div class="control-row">
            <span class="control-label">缩放</span>
            <el-slider
              v-model="previewZoom"
              :min="60"
              :max="120"
              :step="5"
              size="small"
              style="width: 100px"
            />
            <span class="zoom-value">{{ previewZoom }}%</span>
          </div>
        </div>

        <!-- 预览区域 -->
        <div class="preview-wrapper">
          <ResumePreview
            :resume-content="previewContent"
            :template-id="selectedTemplate"
            :templates="resumeStore.templates"
            :modules="modules"
            :zoom="previewZoom"
          />
        </div>

        <!-- 导出按钮 -->
        <div class="export-actions">
          <el-button
            type="primary"
            :loading="exportLoading"
            :disabled="!currentResumeId"
            @click="handleExport('pdf')"
          >
            <el-icon><Download /></el-icon> 导出 PDF
          </el-button>
          <el-button
            type="success"
            :loading="exportLoading"
            :disabled="!currentResumeId"
            @click="handleExport('docx')"
          >
            <el-icon><Download /></el-icon> 导出 Word
          </el-button>
        </div>
      </div>
    </div>

    <!-- 简历列表抽屉 -->
    <el-drawer
      v-model="showResumeList"
      title="我的简历"
      direction="rtl"
      size="360px"
    >
      <div class="resume-list-drawer">
        <el-card
          v-for="item in resumeList"
          :key="item.id"
          shadow="hover"
          class="resume-card"
          @click="handleViewResume(item.id)"
        >
          <div class="card-content">
            <div class="card-title">{{ item.title }}</div>
            <div class="card-meta">
              <el-tag size="small" type="info">{{ item.language === 'zh' ? '中文' : 'English' }}</el-tag>
              <span class="update-time">{{ item.updatedAt }}</span>
            </div>
          </div>
        </el-card>
        <el-empty v-if="!resumeList.length" description="暂无简历" />
      </div>
    </el-drawer>
  </div>
</template>

<style lang="scss" scoped>
.resume-writer {
  height: 100%;
  display: flex;
  flex-direction: column;
  overflow: hidden;
  animation: fadeInUp 0.4s ease both;
}

// 顶部工具栏
.top-toolbar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 14px 20px;
  background: var(--color-surface);
  border-radius: var(--radius-lg);
  margin-bottom: 12px;
  flex-shrink: 0;
  box-shadow: var(--shadow-card);
  border: 1px solid var(--color-border-light);

  .toolbar-left {
    display: flex;
    align-items: center;
    gap: 20px;

    .page-title {
      margin: 0;
      font-size: 20px;
      font-weight: 600;
      color: #333333;
    }
  }

  .toolbar-right {
    display: flex;
    align-items: center;
    gap: 8px;
  }
}

// 三栏布局
.main-content {
  display: flex;
  gap: 12px;
  flex: 1;
  min-height: 0;
  overflow: hidden;
}

// 左侧模块选择栏
.left-sidebar {
  width: 240px;
  flex-shrink: 0;
}

// 中间编辑区
.center-panel {
  flex: 1;
  min-width: 0;
  display: flex;
  flex-direction: column;
}

// 右侧预览区
.right-panel {
  width: 480px;
  flex-shrink: 0;
  display: flex;
  flex-direction: column;
  gap: 12px;
  min-height: 0;
}

// 预览控制栏
.preview-controls {
  background: var(--color-surface);
  border-radius: var(--radius-lg);
  padding: 12px 16px;
  display: flex;
  flex-direction: column;
  gap: 10px;
  flex-shrink: 0;
  box-shadow: var(--shadow-card);
  border: 1px solid var(--color-border-light);

  .control-row {
    display: flex;
    align-items: center;
    gap: 10px;

    .control-label {
      font-size: 12px;
      color: #666666;
      white-space: nowrap;
      width: 32px;
    }

    .zoom-value {
      font-size: 12px;
      color: #999999;
      width: 36px;
      text-align: right;
    }
  }
}

// 预览包装器
.preview-wrapper {
  flex: 1;
  min-height: 0;
  overflow: hidden;
}

// 导出按钮
.export-actions {
  display: flex;
  gap: 12px;
  background: var(--color-surface);
  border-radius: var(--radius-lg);
  padding: 12px 16px;
  flex-shrink: 0;
  box-shadow: var(--shadow-card);
  border: 1px solid var(--color-border-light);

  .el-button {
    flex: 1;
    font-weight: 600;
  }
}

// 简历列表抽屉
.resume-list-drawer {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.resume-card {
  cursor: pointer;
  transition: all var(--transition-smooth);
  border: 1px solid var(--color-border-light);

  &:hover {
    transform: translateY(-2px);
    box-shadow: var(--shadow-elevated);
    border-color: #4A7CFF;
  }

  .card-content {
    .card-title {
      font-size: 14px;
      font-weight: 600;
      color: #333333;
      margin-bottom: 8px;
    }

    .card-meta {
      display: flex;
      align-items: center;
      gap: 8px;

      .update-time {
        font-size: 12px;
        color: #999999;
      }
    }
  }
}

// 响应式调整
@media (max-width: 1400px) {
  .left-sidebar {
    width: 200px;
  }

  .right-panel {
    width: 400px;
  }
}

@media (max-width: 1200px) {
  .left-sidebar {
    width: 180px;
  }

  .right-panel {
    width: 360px;
  }
}
</style>
