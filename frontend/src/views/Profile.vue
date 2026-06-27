<script setup lang="ts">
import { ref, reactive, onMounted, nextTick } from 'vue'
import { ElNotification, ElInput } from 'element-plus'
import { Delete } from '@element-plus/icons-vue'
import { profileApi } from '@/api/profile'
import type { ProfileForm } from '@/api/types'

const loading = ref(false)
const saving = ref(false)

const form = reactive<ProfileForm>({
  workYears: '',
  techDirection: [],
  targetPosition: '',
  targetIndustry: [],
  salaryRange: '',
  education: [
    { degree: '', school: '', major: '' }
  ],
  coreSkills: []
})

// 添加教育经历
const addEducation = () => {
  form.education.push({ degree: '', school: '', major: '' })
}

// 删除教育经历
const removeEducation = (index: number) => {
  if (form.education.length > 1) {
    form.education.splice(index, 1)
  }
}

// 选项配置
const workYearsOptions = ['应届', '1-3年', '3-5年', '5-10年', '10年+']
const techDirectionOptions = [
  // IT技术类
  'Java后端', 'Python', '前端开发', '大数据', 'AI/机器学习', 'Go', 'C/C++', '移动端开发', '云计算/DevOps', '网络安全', '测试/QA', '数据工程', '全栈开发', '嵌入式开发',
  // 金融科技
  '金融分析', '风控建模', '量化交易', '精算', '区块链/Web3',
  // 医疗健康
  '医疗信息化', '临床研究', '药物研发', '生物信息学', '医疗器械',
  // 教育培训
  '教育科技', '在线教育', '课程设计', '培训管理',
  // 制造工程
  '工业工程', '自动化', '质量管理', '供应链管理', '机械设计',
  // 营销运营
  '数字营销', '品牌管理', '市场研究', '内容营销', '增长黑客', '用户运营',
  // 设计创意
  'UI/UX设计', '产品设计', '视觉设计', '创意策划',
  // 产品管理
  '产品管理', '项目管理', '战略咨询', '数据分析',
  // 其他
  '法务合规', '人力资源', '财务会计', '行政管理'
]
const targetIndustryOptions = ['互联网', '金融', '游戏', '电商', '教育', '医疗', '制造']
const salaryRangeOptions = ['10k以下', '10k-15k', '15k-20k', '20k-30k', '30k-50k', '50k+']
const degreeOptions = ['大专', '本科', '硕士', '博士']

// 技能标签相关
const skillInputVisible = ref(false)
const skillInputValue = ref('')
const skillInputRef = ref<InstanceType<typeof ElInput> | null>(null)

const handleSkillClose = (skill: string) => {
  form.coreSkills.splice(form.coreSkills.indexOf(skill), 1)
}

const showSkillInput = () => {
  skillInputVisible.value = true
  nextTick(() => {
    skillInputRef.value?.input?.focus()
  })
}

const handleSkillInputConfirm = () => {
  if (skillInputValue.value && !form.coreSkills.includes(skillInputValue.value)) {
    form.coreSkills.push(skillInputValue.value)
  }
  skillInputVisible.value = false
  skillInputValue.value = ''
}

// 加载画像
const loadProfile = async () => {
  loading.value = true
  try {
    const res = await profileApi.getProfile()
    if (res) {
      form.workYears = res.workYears || ''
      form.techDirection = res.techDirection || []
      form.targetPosition = res.targetPosition || ''
      form.targetIndustry = res.targetIndustry || []
      form.salaryRange = res.salaryRange || ''
      form.education = res.education?.length ? res.education : [{ degree: '', school: '', major: '' }]
      form.coreSkills = res.coreSkills || []
    }
  } catch (e) {
    // 错误已在拦截器中处理
  } finally {
    loading.value = false
  }
}

// 保存画像
const handleSave = async () => {
  saving.value = true
  try {
    await profileApi.updateProfile(form)
    ElNotification({
      title: '成功',
      message: '画像保存成功',
      type: 'success',
      duration: 3000
    })
  } catch (e: any) {
    console.error('Save error:', e)
    // 优先提取服务器返回的具体错误信息，其次判断是否有 HTTP 响应
    const serverMsg = e?.response?.data?.message
    const errorMsg = serverMsg
      || (e?.response ? '服务器处理失败，请稍后重试' : '网络连接异常，请检查网络')
    ElNotification({
      title: '保存失败',
      message: errorMsg,
      type: 'error',
      duration: 5000
    })
  } finally {
    saving.value = false
  }
}

onMounted(() => {
  loadProfile()
})
</script>

<template>
  <div class="profile-page" v-loading="loading">
    <el-card class="profile-card">
      <template #header>
        <div class="card-header">
          <span>个人画像</span>
          <span class="sub-title">完善个人画像，获得更精准的简历与面试服务</span>
        </div>
      </template>

      <el-form :model="form" label-width="100px" label-position="right" size="large">
        <!-- 工作年限 -->
        <el-form-item label="工作年限">
          <el-select v-model="form.workYears" placeholder="请选择工作年限" style="width: 100%">
            <el-option v-for="item in workYearsOptions" :key="item" :label="item" :value="item" />
          </el-select>
        </el-form-item>

        <!-- 技术方向 -->
        <el-form-item label="技术方向">
          <el-select
            v-model="form.techDirection"
            multiple
            placeholder="请选择技术方向（可多选）"
            style="width: 100%"
          >
            <el-option v-for="item in techDirectionOptions" :key="item" :label="item" :value="item" />
          </el-select>
        </el-form-item>

        <!-- 目标岗位 -->
        <el-form-item label="目标岗位">
          <el-input v-model="form.targetPosition" placeholder="如：高级Java工程师" />
        </el-form-item>

        <!-- 目标行业 -->
        <el-form-item label="目标行业">
          <el-select
            v-model="form.targetIndustry"
            multiple
            placeholder="请选择目标行业（可多选）"
            style="width: 100%"
          >
            <el-option v-for="item in targetIndustryOptions" :key="item" :label="item" :value="item" />
          </el-select>
        </el-form-item>

        <!-- 期望薪资 -->
        <el-form-item label="期望薪资">
          <el-select v-model="form.salaryRange" placeholder="请选择期望薪资范围" style="width: 100%">
            <el-option v-for="item in salaryRangeOptions" :key="item" :label="item" :value="item" />
          </el-select>
        </el-form-item>

        <!-- 教育背景 -->
        <el-form-item label="教育背景">
          <div class="education-list">
            <div v-for="(edu, index) in form.education" :key="index" class="education-group">
              <el-select v-model="edu.degree" placeholder="学历" style="width: 120px">
                <el-option v-for="item in degreeOptions" :key="item" :label="item" :value="item" />
              </el-select>
              <el-input v-model="edu.school" placeholder="学校名称" style="flex: 1" />
              <el-input v-model="edu.major" placeholder="专业" style="flex: 1" />
              <el-button
                v-if="form.education.length > 1"
                type="danger"
                :icon="Delete"
                circle
                size="small"
                @click="removeEducation(index)"
              />
            </div>
            <el-button size="small" @click="addEducation" style="margin-top: 8px">
              + 添加教育经历
            </el-button>
          </div>
        </el-form-item>

        <!-- 核心技能 -->
        <el-form-item label="核心技能">
          <div class="skill-tags">
            <el-tag
              v-for="skill in form.coreSkills"
              :key="skill"
              closable
              :disable-transitions="false"
              @close="handleSkillClose(skill)"
              style="margin-right: 8px; margin-bottom: 8px"
            >
              {{ skill }}
            </el-tag>
            <el-input
              v-if="skillInputVisible"
              ref="skillInputRef"
              v-model="skillInputValue"
              size="small"
              style="width: 120px; margin-bottom: 8px"
              @keyup.enter="handleSkillInputConfirm"
              @blur="handleSkillInputConfirm"
            />
            <el-button
              v-else
              size="small"
              @click="showSkillInput"
              style="margin-bottom: 8px"
            >
              + 添加技能
            </el-button>
          </div>
        </el-form-item>

        <!-- 保存按钮 -->
        <el-form-item>
          <el-button type="primary" :loading="saving" @click="handleSave" size="large">
            保存画像
          </el-button>
        </el-form-item>
      </el-form>
    </el-card>
  </div>
</template>

<style lang="scss" scoped>
.profile-page {
  padding: 24px;
  max-width: 800px;
  margin: 0 auto;
  animation: fadeInUp 0.4s ease both;
}

.profile-card {
  border-radius: var(--radius-lg);
  border: 1px solid var(--color-border-light);
  overflow: hidden;
  position: relative;

  // Top gradient accent bar
  &::before {
    content: '';
    position: absolute;
    top: 0;
    left: 0;
    right: 0;
    height: 4px;
    background: linear-gradient(90deg, var(--color-primary), var(--color-accent));
  }

  .card-header {
    display: flex;
    align-items: baseline;
    gap: 12px;

    span:first-child {
      font-size: 20px;
      font-weight: 600;
      font-family: var(--font-heading);
    }

    .sub-title {
      font-size: 13px;
      color: var(--color-text-tertiary);
    }
  }
}

.education-list {
  width: 100%;
}

.education-group {
  display: flex;
  gap: 12px;
  width: 100%;
  margin-bottom: 12px;
  align-items: center;

  &:last-of-type {
    margin-bottom: 0;
  }
}

.skill-tags {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
}
</style>
