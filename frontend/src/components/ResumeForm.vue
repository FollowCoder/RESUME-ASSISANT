<script setup lang="ts">
import { ref, reactive, watch } from 'vue'
import type { ResumeFormData, ResumeContent, EducationItem, WorkExperienceItem, ProjectExperienceItem, TemplateInfo } from '@/api/types'
import type { FormInstance } from 'element-plus'

const props = defineProps<{
  templates: TemplateInfo[]
  activeModule?: string
}>()

const emit = defineEmits<{
  submit: [data: ResumeFormData]
  'update:preview': [content: ResumeContent]
}>()

const activeCollapse = ref<string[]>(['basicInfo'])
const formRef = ref<FormInstance>()
const submitting = ref(false)

// 监听外部激活模块变化
watch(() => props.activeModule, (newModule) => {
  if (newModule) {
    activeCollapse.value = [newModule]
  }
})

const formData = reactive<ResumeFormData>({
  basicInfo: {
    name: '',
    phone: '',
    email: '',
    location: '',
    website: ''
  },
  education: [{ school: '', degree: '', major: '', startDate: '', endDate: '', description: '' }],
  workExperience: [{ company: '', position: '', startDate: '', endDate: '', description: '', achievements: [''] }],
  projectExperience: [{ name: '', role: '', startDate: '', endDate: '', description: '', techStack: [], highlights: [] }],
  skills: [],
  summary: '',
  title: '',
  language: 'zh',
  templateId: 'modern'
})

const skillInput = ref('')

const modules = [
  { id: 'basicInfo', title: '基本信息', icon: 'User' },
  { id: 'education', title: '教育背景', icon: 'School' },
  { id: 'workExperience', title: '工作经历', icon: 'Briefcase' },
  { id: 'projectExperience', title: '项目经验', icon: 'Folder' },
  { id: 'skills', title: '技能特长', icon: 'Star' },
  { id: 'summary', title: '个人总结', icon: 'Document' },
  { id: 'meta', title: '元信息', icon: 'Setting' }
]

function addEducation() {
  formData.education.push({ school: '', degree: '', major: '', startDate: '', endDate: '', description: '' })
}

function removeEducation(index: number) {
  if (formData.education.length > 1) {
    formData.education.splice(index, 1)
  }
}

function addWorkExperience() {
  formData.workExperience.push({ company: '', position: '', startDate: '', endDate: '', description: '', achievements: [''] })
}

function removeWorkExperience(index: number) {
  if (formData.workExperience.length > 1) {
    formData.workExperience.splice(index, 1)
  }
}

function addAchievement(workIndex: number) {
  formData.workExperience[workIndex].achievements!.push('')
}

function removeAchievement(workIndex: number, achIndex: number) {
  formData.workExperience[workIndex].achievements!.splice(achIndex, 1)
}

function addProjectExperience() {
  formData.projectExperience.push({ name: '', role: '', startDate: '', endDate: '', description: '', techStack: [], highlights: [] })
}

function removeProjectExperience(index: number) {
  if (formData.projectExperience.length > 1) {
    formData.projectExperience.splice(index, 1)
  }
}

function addHighlight(projIndex: number) {
  formData.projectExperience[projIndex].highlights!.push('')
}

function removeHighlight(projIndex: number, hlIndex: number) {
  formData.projectExperience[projIndex].highlights!.splice(hlIndex, 1)
}

function addSkill() {
  const skill = skillInput.value.trim()
  if (skill && !formData.skills.includes(skill)) {
    formData.skills.push(skill)
    skillInput.value = ''
  }
}

function removeSkill(index: number) {
  formData.skills.splice(index, 1)
}

function handleSkillKeydown(e: KeyboardEvent | Event) {
  if ((e as KeyboardEvent).key === 'Enter') {
    e.preventDefault()
    addSkill()
  }
}

function handleAiOptimize(field: string) {
  // AI 优化功能占位
  console.log('AI optimize:', field)
}

// 防抖更新预览内容（150ms）
let debounceTimer: ReturnType<typeof setTimeout> | null = null

function emitPreviewUpdate() {
  if (debounceTimer) {
    clearTimeout(debounceTimer)
  }
  debounceTimer = setTimeout(() => {
    const previewContent: ResumeContent = {
      basicInfo: { ...formData.basicInfo },
      education: formData.education.map(e => ({ ...e })),
      workExperience: formData.workExperience.map(w => ({ ...w })),
      projectExperience: formData.projectExperience.map(p => ({
        ...p,
        techStack: Array.isArray(p.techStack) ? [...p.techStack] : p.techStack
      })),
      skills: [...formData.skills],
      summary: formData.summary
    }
    emit('update:preview', previewContent)
  }, 150)
}

// 监听 formData 变化，实时更新预览
watch(
  () => formData,
  () => {
    emitPreviewUpdate()
  },
  { deep: true }
)

function handleSubmit() {
  if (!formData.title) {
    formData.title = `${formData.basicInfo.name}的简历`
  }
  submitting.value = true
  emit('submit', { ...formData })
  setTimeout(() => { submitting.value = false }, 1000)
}
</script>

<template>
  <div class="resume-form">
    <div class="form-header">
      <h3>简历编辑</h3>
      <span class="hint">点击模块标题展开编辑</span>
    </div>

    <el-scrollbar class="form-body">
      <el-collapse v-model="activeCollapse" accordion>
        <!-- 基本信息 -->
        <el-collapse-item name="basicInfo">
          <template #title>
            <div class="collapse-title">
              <span class="title-icon">👤</span>
              <span>基本信息</span>
            </div>
          </template>
          <el-form :model="formData.basicInfo" label-width="80px">
            <el-form-item label="姓名" required>
              <el-input v-model="formData.basicInfo.name" placeholder="请输入姓名" />
            </el-form-item>
            <el-form-item label="电话" required>
              <el-input v-model="formData.basicInfo.phone" placeholder="请输入电话号码" />
            </el-form-item>
            <el-form-item label="邮箱" required>
              <el-input v-model="formData.basicInfo.email" placeholder="请输入邮箱地址" />
            </el-form-item>
            <el-form-item label="所在地">
              <el-input v-model="formData.basicInfo.location" placeholder="如：北京市" />
            </el-form-item>
            <el-form-item label="个人网站">
              <el-input v-model="formData.basicInfo.website" placeholder="如：https://github.com/xxx" />
            </el-form-item>
          </el-form>
        </el-collapse-item>

        <!-- 教育背景 -->
        <el-collapse-item name="education">
          <template #title>
            <div class="collapse-title">
              <span class="title-icon">🎓</span>
              <span>教育背景</span>
            </div>
          </template>
          <div v-for="(edu, index) in formData.education" :key="index" class="dynamic-item">
            <div class="item-header">
              <span>教育经历 {{ index + 1 }}</span>
              <el-button v-if="formData.education.length > 1" type="danger" text size="small" @click="removeEducation(index)">
                <el-icon><Delete /></el-icon> 删除
              </el-button>
            </div>
            <el-form label-width="80px">
              <el-row :gutter="16">
                <el-col :span="12">
                  <el-form-item label="学校">
                    <el-input v-model="edu.school" placeholder="学校名称" />
                  </el-form-item>
                </el-col>
                <el-col :span="12">
                  <el-form-item label="学位">
                    <el-select v-model="edu.degree" placeholder="选择学位" style="width: 100%">
                      <el-option label="大专" value="大专" />
                      <el-option label="本科" value="本科" />
                      <el-option label="硕士" value="硕士" />
                      <el-option label="博士" value="博士" />
                    </el-select>
                  </el-form-item>
                </el-col>
              </el-row>
              <el-form-item label="专业">
                <el-input v-model="edu.major" placeholder="所学专业" />
              </el-form-item>
              <el-row :gutter="16">
                <el-col :span="12">
                  <el-form-item label="开始时间">
                    <el-input v-model="edu.startDate" placeholder="如：2018-09" />
                  </el-form-item>
                </el-col>
                <el-col :span="12">
                  <el-form-item label="结束时间">
                    <el-input v-model="edu.endDate" placeholder="如：2022-06" />
                  </el-form-item>
                </el-col>
              </el-row>
              <el-form-item label="描述">
                <el-input v-model="edu.description" type="textarea" :rows="2" placeholder="相关描述（选填）" />
              </el-form-item>
            </el-form>
          </div>
          <el-button type="primary" plain @click="addEducation" class="add-btn">
            <el-icon><Plus /></el-icon> 添加教育经历
          </el-button>
        </el-collapse-item>

        <!-- 工作经历 -->
        <el-collapse-item name="workExperience">
          <template #title>
            <div class="collapse-title">
              <span class="title-icon">💼</span>
              <span>工作经历</span>
            </div>
          </template>
          <div v-for="(work, wIndex) in formData.workExperience" :key="wIndex" class="dynamic-item">
            <div class="item-header">
              <span>工作经历 {{ wIndex + 1 }}</span>
              <el-button v-if="formData.workExperience.length > 1" type="danger" text size="small" @click="removeWorkExperience(wIndex)">
                <el-icon><Delete /></el-icon> 删除
              </el-button>
            </div>
            <el-form label-width="80px">
              <el-row :gutter="16">
                <el-col :span="12">
                  <el-form-item label="公司">
                    <el-input v-model="work.company" placeholder="公司名称" />
                  </el-form-item>
                </el-col>
                <el-col :span="12">
                  <el-form-item label="职位">
                    <el-input v-model="work.position" placeholder="岗位名称" />
                  </el-form-item>
                </el-col>
              </el-row>
              <el-row :gutter="16">
                <el-col :span="12">
                  <el-form-item label="开始时间">
                    <el-input v-model="work.startDate" placeholder="如：2022-07" />
                  </el-form-item>
                </el-col>
                <el-col :span="12">
                  <el-form-item label="结束时间">
                    <el-input v-model="work.endDate" placeholder="如：至今" />
                  </el-form-item>
                </el-col>
              </el-row>
              <el-form-item label="工作描述">
                <div class="textarea-with-ai">
                  <el-input v-model="work.description" type="textarea" :rows="3" placeholder="描述工作内容和职责" />
                  <el-button class="ai-optimize-btn" type="primary" plain size="small" @click.stop="handleAiOptimize('workDescription')">
                    AI 优化
                  </el-button>
                </div>
              </el-form-item>
              <el-form-item label="工作成果">
                <div class="achievements-list">
                  <div v-for="(_, aIndex) in work.achievements" :key="aIndex" class="achievement-row">
                    <el-input v-model="work.achievements![aIndex]" placeholder="描述具体成果" />
                    <el-button type="danger" text @click="removeAchievement(wIndex, aIndex)" :disabled="work.achievements!.length <= 1">
                      <el-icon><Minus /></el-icon>
                    </el-button>
                  </div>
                  <el-button type="primary" text size="small" @click="addAchievement(wIndex)">
                    <el-icon><Plus /></el-icon> 添加成果
                  </el-button>
                </div>
              </el-form-item>
            </el-form>
          </div>
          <el-button type="primary" plain @click="addWorkExperience" class="add-btn">
            <el-icon><Plus /></el-icon> 添加工作经历
          </el-button>
        </el-collapse-item>

        <!-- 项目经验 -->
        <el-collapse-item name="projectExperience">
          <template #title>
            <div class="collapse-title">
              <span class="title-icon">📁</span>
              <span>项目经验</span>
            </div>
          </template>
          <div v-for="(proj, pIndex) in formData.projectExperience" :key="pIndex" class="dynamic-item">
            <div class="item-header">
              <span>项目经验 {{ pIndex + 1 }}</span>
              <el-button v-if="formData.projectExperience.length > 1" type="danger" text size="small" @click="removeProjectExperience(pIndex)">
                <el-icon><Delete /></el-icon> 删除
              </el-button>
            </div>
            <el-form label-width="80px">
              <el-row :gutter="16">
                <el-col :span="12">
                  <el-form-item label="项目名称">
                    <el-input v-model="proj.name" placeholder="项目名称" />
                  </el-form-item>
                </el-col>
                <el-col :span="12">
                  <el-form-item label="角色">
                    <el-input v-model="proj.role" placeholder="如：技术负责人" />
                  </el-form-item>
                </el-col>
              </el-row>
              <el-row :gutter="16">
                <el-col :span="12">
                  <el-form-item label="开始时间">
                    <el-input v-model="proj.startDate" placeholder="如：2023-01" />
                  </el-form-item>
                </el-col>
                <el-col :span="12">
                  <el-form-item label="结束时间">
                    <el-input v-model="proj.endDate" placeholder="如：2023-12" />
                  </el-form-item>
                </el-col>
              </el-row>
              <el-form-item label="项目描述">
                <div class="textarea-with-ai">
                  <el-input v-model="proj.description" type="textarea" :rows="3" placeholder="描述项目背景和你的工作" />
                  <el-button class="ai-optimize-btn" type="primary" plain size="small" @click.stop="handleAiOptimize('projectDescription')">
                    AI 优化
                  </el-button>
                </div>
              </el-form-item>
              <el-form-item label="技术栈">
                <el-select v-model="proj.techStack" multiple filterable allow-create placeholder="输入技术栈后回车" style="width: 100%" />
              </el-form-item>
              <el-form-item label="项目亮点">
                <div class="achievements-list">
                  <div v-for="(_, hIndex) in proj.highlights" :key="hIndex" class="achievement-row">
                    <el-input v-model="proj.highlights![hIndex]" placeholder="描述项目亮点" />
                    <el-button type="danger" text @click="removeHighlight(pIndex, hIndex)">
                      <el-icon><Minus /></el-icon>
                    </el-button>
                  </div>
                  <el-button type="primary" text size="small" @click="addHighlight(pIndex)">
                    <el-icon><Plus /></el-icon> 添加亮点
                  </el-button>
                </div>
              </el-form-item>
            </el-form>
          </div>
          <el-button type="primary" plain @click="addProjectExperience" class="add-btn">
            <el-icon><Plus /></el-icon> 添加项目经验
          </el-button>
        </el-collapse-item>

        <!-- 技能特长 -->
        <el-collapse-item name="skills">
          <template #title>
            <div class="collapse-title">
              <span class="title-icon">⭐</span>
              <span>技能特长</span>
            </div>
          </template>
          <div class="skills-section">
            <div class="skill-input-row">
              <el-input
                v-model="skillInput"
                placeholder="输入技能后按 Enter 添加"
                @keydown="handleSkillKeydown"
              />
              <el-button type="primary" @click="addSkill">添加</el-button>
            </div>
            <div class="skills-tags">
              <el-tag
                v-for="(skill, index) in formData.skills"
                :key="index"
                closable
                type="primary"
                @close="removeSkill(index)"
                class="skill-tag"
              >
                {{ skill }}
              </el-tag>
              <el-empty v-if="!formData.skills.length" description="暂未添加技能" :image-size="60" />
            </div>
          </div>
        </el-collapse-item>

        <!-- 个人总结 -->
        <el-collapse-item name="summary">
          <template #title>
            <div class="collapse-title">
              <span class="title-icon">📄</span>
              <span>个人总结</span>
            </div>
          </template>
          <el-form label-width="80px">
            <el-form-item label="个人总结">
              <div class="textarea-with-ai">
                <el-input
                  v-model="formData.summary"
                  type="textarea"
                  :rows="6"
                  placeholder="简短总结您的职业经历、核心能力和求职意向（建议 100-200 字）"
                />
                <el-button class="ai-optimize-btn" type="primary" plain size="small" @click.stop="handleAiOptimize('summary')">
                  AI 优化
                </el-button>
              </div>
            </el-form-item>
          </el-form>
        </el-collapse-item>

        <!-- 元信息 -->
        <el-collapse-item name="meta">
          <template #title>
            <div class="collapse-title">
              <span class="title-icon">⚙️</span>
              <span>元信息</span>
            </div>
          </template>
          <el-form label-width="80px">
            <el-form-item label="简历标题" required>
              <el-input v-model="formData.title" placeholder="如：张三-高级Java工程师" />
            </el-form-item>
            <el-form-item label="语言">
              <el-radio-group v-model="formData.language">
                <el-radio-button value="zh">中文</el-radio-button>
                <el-radio-button value="en">English</el-radio-button>
              </el-radio-group>
            </el-form-item>
            <el-form-item label="选择模板">
              <el-radio-group v-model="formData.templateId">
                <el-radio-button v-for="tpl in templates" :key="tpl.id" :value="tpl.id">
                  {{ tpl.name }}
                </el-radio-button>
              </el-radio-group>
            </el-form-item>
          </el-form>
        </el-collapse-item>
      </el-collapse>
    </el-scrollbar>

    <div class="form-footer">
      <el-button type="success" :loading="submitting" @click="handleSubmit" class="submit-btn">
        <el-icon><Check /></el-icon> 生成简历
      </el-button>
    </div>
  </div>
</template>

<style lang="scss" scoped>
.resume-form {
  height: 100%;
  display: flex;
  flex-direction: column;
  background: var(--color-surface);
  border-radius: var(--radius-lg);
  box-shadow: var(--shadow-card);
  border: 1px solid var(--color-border-light);
  overflow: hidden;
}

.form-header {
  padding: 18px 20px;
  border-bottom: 1px solid #E8E8E8;
  background: #E8EEFD;
  flex-shrink: 0;

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

.form-body {
  flex: 1;
  padding: 16px;
}

.collapse-title {
  display: flex;
  align-items: center;
  gap: 10px;
  font-weight: 500;
  color: #333333;

  .title-icon {
    font-size: 18px;
  }
}

:deep(.el-collapse) {
  border: none;
}

:deep(.el-collapse-item__header) {
  background: #F5F7FA;
  border-radius: var(--radius-base);
  margin-bottom: 8px;
  padding: 0 16px;
  border: 1px solid #E8E8E8;
  transition: all var(--transition-smooth);

  &:hover {
    border-color: #4A7CFF;
    background: #F0F5FF;
  }

  &.is-active {
    border-color: #4A7CFF;
    background: #E8EEFD;
  }
}

:deep(.el-collapse-item__wrap) {
  border: none;
  background: transparent;
}

:deep(.el-collapse-item__content) {
  padding: 16px 0;
}

.dynamic-item {
  border: 1px solid #E8E8E8;
  border-radius: var(--radius-base);
  padding: 16px;
  margin-bottom: 12px;
  background: #FAFBFC;
  transition: all var(--transition-smooth);

  &:hover {
    border-color: #4A7CFF;
  }

  .item-header {
    display: flex;
    justify-content: space-between;
    align-items: center;
    margin-bottom: 12px;
    font-weight: 600;
    color: #333333;
    font-size: 14px;
    padding-bottom: 8px;
    border-bottom: 2px solid #4A7CFF;
  }
}

.textarea-with-ai {
  position: relative;
  width: 100%;

  .ai-optimize-btn {
    position: absolute;
    top: 8px;
    right: 8px;
    z-index: 10;
    font-size: 12px;
    padding: 4px 10px;
    border-color: #4A7CFF;
    color: #4A7CFF;
    background: rgba(255, 255, 255, 0.9);
    backdrop-filter: blur(4px);

    &:hover {
      background: #4A7CFF;
      color: #FFFFFF;
    }
  }
}

.add-btn {
  width: 100%;
  border-style: dashed;
  border-color: #DCDFE6;
  color: #666666;
  font-weight: 500;
  transition: all var(--transition-smooth);

  &:hover {
    border-color: #4A7CFF;
    color: #4A7CFF;
    background: rgba(74, 124, 255, 0.04);
  }
}

.achievements-list {
  width: 100%;

  .achievement-row {
    display: flex;
    gap: 10px;
    margin-bottom: 10px;
    align-items: center;
  }
}

.skills-section {
  .skill-input-row {
    display: flex;
    gap: 10px;
    margin-bottom: 12px;
  }

  .skills-tags {
    display: flex;
    flex-wrap: wrap;
    gap: 10px;
    min-height: 60px;
    padding: 12px;
    border: 2px dashed #DCDFE6;
    border-radius: var(--radius-base);
    background: #FAFBFC;
    transition: all var(--transition-smooth);

    &:focus-within {
      border-color: #4A7CFF;
    }

    .skill-tag {
      height: 30px;
      font-size: 13px;
      background: #4A7CFF;
      border: none;
      color: #FFFFFF;
      border-radius: var(--radius-full);
      transition: all var(--transition-smooth);

      &:hover {
        transform: translateY(-2px);
        box-shadow: 0 4px 12px rgba(74, 124, 255, 0.3);
      }
    }
  }
}

.form-footer {
  padding: 16px 20px;
  border-top: 1px solid #E8E8E8;
  flex-shrink: 0;
  background: #FAFBFC;

  .submit-btn {
    width: 100%;
    padding: 14px;
    font-weight: 600;
    font-size: 15px;
    background: linear-gradient(135deg, #4A7CFF 0%, #6B93FF 100%);
    border: none;
    border-radius: var(--radius-base);
    transition: all var(--transition-smooth);

    &:hover {
      transform: translateY(-2px);
      box-shadow: 0 6px 20px rgba(74, 124, 255, 0.35);
    }
  }
}
</style>
