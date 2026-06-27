<script setup lang="ts">
import { computed } from 'vue'
import type { ResumeContent, TemplateInfo } from '@/api/types'
import type { ModuleItem } from './ModuleSelector.vue'
import LaoyuTemplate from './templates/LaoyuTemplate.vue'

const props = defineProps<{
  resumeContent: ResumeContent | null
  templateId: string
  templates: TemplateInfo[]
  modules?: ModuleItem[]
  zoom?: number
}>()

// 计算可见模块（按顺序）
const visibleSections = computed(() => {
  if (!props.modules) {
    // 如果没有传入模块配置，显示所有
    return [
      { id: 'basicInfo', visible: true },
      { id: 'education', visible: true },
      { id: 'workExperience', visible: true },
      { id: 'projectExperience', visible: true },
      { id: 'skills', visible: true },
      { id: 'summary', visible: true }
    ]
  }
  return props.modules.filter(m => m.visible)
})

function getTemplateName(id: string): string {
  const tpl = props.templates.find(t => t.id === id)
  return tpl?.name || id
}

function isSectionVisible(sectionId: string): boolean {
  return visibleSections.value.some(m => m.id === sectionId)
}

// 判断是否使用老鱼模板
const isLaoyuTemplate = computed(() => {
  return props.templateId === 'laoyu' || props.templateId === 'laoyujianli'
})
</script>

<template>
  <div class="resume-preview-container">
    <!-- 预览工具栏 -->
    <div class="preview-toolbar">
      <span class="toolbar-label">简历预览</span>
      <div class="toolbar-actions">
        <span class="zoom-info">{{ zoom || 100 }}%</span>
      </div>
    </div>

    <!-- A4 纸张预览区 -->
    <div class="preview-scroll-area">
      <div
        class="a4-paper"
        :style="{ transform: `scale(${(zoom || 100) / 100})` }"
      >
        <div v-if="resumeContent" class="paper-content">
          <!-- 老鱼模板 -->
          <LaoyuTemplate
            v-if="isLaoyuTemplate"
            :resumeContent="resumeContent"
          />

          <!-- 默认模板 -->
          <template v-else>
            <!-- 基本信息 -->
            <div v-if="isSectionVisible('basicInfo') && resumeContent.basicInfo" class="resume-header">
              <h1 class="candidate-name">{{ resumeContent.basicInfo.name || '姓名' }}</h1>
              <div class="contact-row">
                <span v-if="resumeContent.basicInfo.phone" class="contact-item">
                  <span class="contact-icon">📱</span>
                  {{ resumeContent.basicInfo.phone }}
                </span>
                <span v-if="resumeContent.basicInfo.email" class="contact-item">
                  <span class="contact-icon">✉️</span>
                  {{ resumeContent.basicInfo.email }}
                </span>
                <span v-if="resumeContent.basicInfo.location" class="contact-item">
                  <span class="contact-icon">📍</span>
                  {{ resumeContent.basicInfo.location }}
                </span>
                <span v-if="resumeContent.basicInfo.website" class="contact-item">
                  <span class="contact-icon">🔗</span>
                  {{ resumeContent.basicInfo.website }}
                </span>
              </div>
            </div>

            <!-- 个人总结 -->
            <div v-if="isSectionVisible('summary') && resumeContent.summary" class="resume-section">
              <div class="section-header">
                <div class="section-indicator"></div>
                <h2 class="section-title">个人总结</h2>
              </div>
              <p class="summary-text">{{ resumeContent.summary }}</p>
            </div>

            <!-- 工作经历 -->
            <div v-if="isSectionVisible('workExperience') && resumeContent.workExperience?.length" class="resume-section">
              <div class="section-header">
                <div class="section-indicator"></div>
                <h2 class="section-title">工作经历</h2>
              </div>
              <div v-for="(work, index) in resumeContent.workExperience" :key="index" class="experience-item">
                <div class="item-header">
                  <div class="item-left">
                    <strong class="company-name">{{ work.company }}</strong>
                    <span class="position-name">{{ work.position }}</span>
                  </div>
                  <span class="date-range">{{ work.startDate }} - {{ work.endDate }}</span>
                </div>
                <p v-if="work.description" class="item-description">{{ work.description }}</p>
                <ul v-if="work.achievements?.length" class="achievements-list">
                  <li v-for="(ach, aIdx) in work.achievements" :key="aIdx">{{ ach }}</li>
                </ul>
              </div>
            </div>

            <!-- 项目经验 -->
            <div v-if="isSectionVisible('projectExperience') && resumeContent.projectExperience?.length" class="resume-section">
              <div class="section-header">
                <div class="section-indicator"></div>
                <h2 class="section-title">项目经验</h2>
              </div>
              <div v-for="(proj, index) in resumeContent.projectExperience" :key="index" class="experience-item">
                <div class="item-header">
                  <div class="item-left">
                    <strong class="company-name">{{ proj.name }}</strong>
                    <span class="position-name">{{ proj.role }}</span>
                  </div>
                  <span class="date-range">{{ proj.startDate }} - {{ proj.endDate }}</span>
                </div>
                <p v-if="proj.description" class="item-description">{{ proj.description }}</p>
                <div v-if="proj.techStack?.length" class="tech-stack">
                  <el-tag v-for="tech in proj.techStack" :key="tech" size="small" type="info" class="tech-tag">{{ tech }}</el-tag>
                </div>
                <ul v-if="proj.highlights?.length" class="achievements-list">
                  <li v-for="(hl, hIdx) in proj.highlights" :key="hIdx">{{ hl }}</li>
                </ul>
              </div>
            </div>

            <!-- 教育背景 -->
            <div v-if="isSectionVisible('education') && resumeContent.education?.length" class="resume-section">
              <div class="section-header">
                <div class="section-indicator"></div>
                <h2 class="section-title">教育背景</h2>
              </div>
              <div v-for="(edu, index) in resumeContent.education" :key="index" class="experience-item">
                <div class="item-header">
                  <div class="item-left">
                    <strong class="company-name">{{ edu.school }}</strong>
                    <span class="position-name">{{ edu.degree }} · {{ edu.major }}</span>
                  </div>
                  <span class="date-range">{{ edu.startDate }} - {{ edu.endDate }}</span>
                </div>
                <p v-if="edu.description" class="item-description">{{ edu.description }}</p>
              </div>
            </div>

            <!-- 技能特长 -->
            <div v-if="isSectionVisible('skills') && resumeContent.skills?.length" class="resume-section">
              <div class="section-header">
                <div class="section-indicator"></div>
                <h2 class="section-title">技能特长</h2>
              </div>
              <div class="skills-container">
                <span v-for="skill in resumeContent.skills" :key="skill" class="skill-badge">
                  {{ skill }}
                </span>
              </div>
            </div>
          </template>

          <!-- 分页提示线 -->
          <div class="page-break-hint">
            <div class="break-line"></div>
            <span class="break-text">分页区域 - 可通过调整内容解决</span>
            <div class="break-line"></div>
          </div>

          <!-- 模板信息 -->
          <div class="template-footer">
            <span class="template-name">模板：{{ getTemplateName(templateId) }}</span>
          </div>
        </div>

        <!-- 空状态 -->
        <div v-else class="preview-empty">
          <div class="empty-icon">📄</div>
          <h3>暂无预览内容</h3>
          <p>通过对话或表单生成简历后</p>
          <p>预览将在此处展示</p>
        </div>
      </div>
    </div>
  </div>
</template>

<style lang="scss" scoped>
.resume-preview-container {
  height: 100%;
  display: flex;
  flex-direction: column;
  background: var(--color-surface);
  border-radius: var(--radius-lg);
  box-shadow: var(--shadow-card);
  border: 1px solid var(--color-border-light);
  overflow: hidden;
}

.preview-toolbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 12px 20px;
  border-bottom: 1px solid #E8E8E8;
  background: #E8EEFD;
  flex-shrink: 0;

  .toolbar-label {
    font-size: 14px;
    font-weight: 600;
    color: #333333;
  }

  .toolbar-actions {
    display: flex;
    align-items: center;
    gap: 12px;

    .zoom-info {
      font-size: 12px;
      color: #666666;
      background: #FFFFFF;
      padding: 4px 8px;
      border-radius: 4px;
      border: 1px solid #E8E8E8;
    }
  }
}

.preview-scroll-area {
  flex: 1;
  overflow: auto;
  padding: 24px;
  background: #F5F5F5;
  display: flex;
  justify-content: center;
}

// A4 纸张效果
.a4-paper {
  width: 689px; // 与老鱼模板一致
  min-height: 975px; // A4 高度
  background: #FFFFFF;
  border-radius: 4px;
  box-shadow:
    0 1px 3px rgba(0, 0, 0, 0.08),
    0 4px 12px rgba(0, 0, 0, 0.06),
    0 8px 24px rgba(0, 0, 0, 0.04);
  transform-origin: top center;
  transition: transform 0.3s ease;
  position: relative;
}

.paper-content {
  padding: 0;
}

// 默认模板样式
.resume-header {
  text-align: center;
  margin-bottom: 24px;
  padding-bottom: 20px;
  border-bottom: 2px solid #4A7CFF;

  .candidate-name {
    font-size: 26px;
    font-weight: 700;
    color: #333333;
    margin: 0 0 12px 0;
    letter-spacing: 2px;
  }

  .contact-row {
    display: flex;
    flex-wrap: wrap;
    justify-content: center;
    gap: 20px;
    font-size: 13px;
    color: #666666;

    .contact-item {
      display: flex;
      align-items: center;
      gap: 6px;

      .contact-icon {
        font-size: 14px;
      }
    }
  }
}

.resume-section {
  margin-bottom: 24px;

  &:last-of-type {
    margin-bottom: 0;
  }
}

.section-header {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 16px;
  padding-bottom: 10px;
  border-bottom: 1px solid #E8E8E8;

  .section-indicator {
    width: 4px;
    height: 20px;
    background: #4A7CFF;
    border-radius: 2px;
    flex-shrink: 0;
  }

  .section-title {
    font-size: 16px;
    font-weight: 600;
    color: #333333;
    margin: 0;
  }
}

.summary-text {
  font-size: 13px;
  color: #666666;
  line-height: 1.8;
  margin: 0;
  text-align: justify;
}

.experience-item {
  margin-bottom: 16px;
  padding-bottom: 16px;
  border-bottom: 1px dashed #E8E8E8;

  &:last-child {
    border-bottom: none;
    margin-bottom: 0;
    padding-bottom: 0;
  }

  .item-header {
    display: flex;
    justify-content: space-between;
    align-items: flex-start;
    margin-bottom: 8px;

    .item-left {
      display: flex;
      align-items: baseline;
      gap: 12px;
      flex-wrap: wrap;

      .company-name {
        font-size: 14px;
        font-weight: 600;
        color: #333333;
      }

      .position-name {
        font-size: 13px;
        color: #666666;
      }
    }

    .date-range {
      font-size: 12px;
      color: #999999;
      white-space: nowrap;
      flex-shrink: 0;
    }
  }

  .item-description {
    font-size: 13px;
    color: #666666;
    line-height: 1.7;
    margin: 0 0 8px 0;
  }
}

.achievements-list {
  margin: 0;
  padding-left: 20px;
  font-size: 13px;
  color: #666666;

  li {
    margin-bottom: 6px;
    line-height: 1.6;

    &:last-child {
      margin-bottom: 0;
    }
  }
}

.tech-stack {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
  margin: 8px 0;

  .tech-tag {
    font-size: 11px;
    background: #E8EEFD;
    color: #4A7CFF;
    border: none;
  }
}

.skills-container {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;

  .skill-badge {
    display: inline-block;
    padding: 6px 14px;
    background: #4A7CFF;
    color: #FFFFFF;
    font-size: 12px;
    font-weight: 500;
    border-radius: 16px;
    transition: all 0.2s ease;

    &:hover {
      background: #6B93FF;
      transform: translateY(-1px);
    }
  }
}

// 分页提示
.page-break-hint {
  display: flex;
  align-items: center;
  gap: 12px;
  margin: 32px 16px 16px 16px;

  .break-line {
    flex: 1;
    height: 0;
    border-top: 1px dashed #CCCCCC;
  }

  .break-text {
    font-size: 11px;
    color: #999999;
    white-space: nowrap;
  }
}

.template-footer {
  text-align: center;
  margin: 16px;
  padding-top: 16px;
  border-top: 1px solid #E8E8E8;

  .template-name {
    font-size: 11px;
    color: #999999;
    background: #F5F5F5;
    padding: 4px 12px;
    border-radius: 4px;
  }
}

// 空状态
.preview-empty {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  height: 100%;
  min-height: 400px;
  color: #999999;

  .empty-icon {
    font-size: 48px;
    margin-bottom: 16px;
    opacity: 0.5;
  }

  h3 {
    font-size: 16px;
    color: #666666;
    margin: 0 0 12px 0;
  }

  p {
    font-size: 13px;
    color: #999999;
    margin: 4px 0;
  }
}
</style>
