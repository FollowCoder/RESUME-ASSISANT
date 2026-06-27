<script setup lang="ts">
import type { ResumeContent } from '@/api/types'

const props = defineProps<{
  resumeContent: ResumeContent
}>()

// SVG 图标
const phoneSvg = `<svg viewBox="64 64 896 896" width="1em" height="1em" fill="currentColor"><path d="M885.6 230.2L779.1 123.8a80.83 80.83 0 00-57.3-23.8c-21.7 0-42.1 8.5-57.4 23.8L549.8 238.4a80.83 80.83 0 00-23.8 57.3c0 21.7 8.5 42.1 23.8 57.4l83.8 83.8A393.82 393.82 0 01553.1 553 395.34 395.34 0 01437 633.8L353.2 550a80.83 80.83 0 00-57.3-23.8c-21.7 0-42.1 8.5-57.4 23.8L123.8 664.5a80.89 80.89 0 00-23.8 57.4c0 21.7 8.5 42.1 23.8 57.4l106.3 106.3c24.4 24.5 58.1 38.4 92.7 38.4 7.3 0 14.3-.6 21.2-1.8 134.8-22.2 268.5-93.9 376.4-201.7C828.2 612.8 899.8 479.2 922.3 344c6.8-41.3-6.9-83.8-36.7-113.8z"/></svg>`

const emailSvg = `<svg viewBox="64 64 896 896" width="1em" height="1em" fill="currentColor"><path d="M928 160H96c-17.7 0-32 14.3-32 32v640c0 17.7 14.3 32 32 32h832c17.7 0 32-14.3 32-32V192c0-17.7-14.3-32-32-32zm-80.8 108.9L531.7 514.4c-7.8 6.1-18.7 6.1-26.5 0L189.6 268.9A7.2 7.2 0 01194 256h648.8a7.2 7.2 0 014.4 12.9z"/></svg>`

const locationSvg = `<svg viewBox="64 64 896 896" width="1em" height="1em" fill="currentColor"><path d="M512 327c-29.9 0-58 11.6-79.2 32.8A111.6 111.6 0 00400 439c0 29.9 11.7 58 32.8 79.2A111.6 111.6 0 00512 551c29.9 0 58-11.7 79.2-32.8C612.4 497 624 468.9 624 439c0-29.9-11.6-58-32.8-79.2S541.9 327 512 327zm342.6-37.9a362.49 362.49 0 00-79.9-115.7 370.83 370.83 0 00-118.2-77.8C610.7 76.6 562.1 67 512 67c-50.1 0-98.7 9.6-144.5 28.5-44.3 18.3-84 44.5-118.2 77.8A363.6 363.6 0 00169.4 289c-19.5 45-29.4 92.8-29.4 142 0 70.6 16.9 140.9 50.1 208.7 26.7 54.5 64 107.6 111 158.1 80.3 86.2 164.5 138.9 188.4 153a43.9 43.9 0 0022.4 6.1c7.8 0 15.5-2 22.4-6.1 23.9-14.1 108.1-66.8 188.4-153 47-50.4 84.3-103.6 111-158.1C867.1 572 884 501.8 884 431.1c0-49.2-9.9-97-29.4-142zM512 615c-97.2 0-176-78.8-176-176s78.8-176 176-176 176 78.8 176 176-78.8 176-176 176z"/></svg>`

// 处理技能描述中的列表内容
function formatDescription(desc: string): string {
  if (!desc) return ''
  // 将换行符转换为列表项
  const lines = desc.split('\n').filter(l => l.trim())
  if (lines.length > 1) {
    return '<ol>' + lines.map(l => `<li>${l.trim()}</li>`).join('') + '</ol>'
  }
  return `<p>${desc}</p>`
}

// 处理成就列表
function formatAchievements(achievements?: string[]): string {
  if (!achievements || achievements.length === 0) return ''
  const filtered = achievements.filter(a => a.trim())
  if (filtered.length === 0) return ''
  return '<ol>' + filtered.map(a => `<li>${a}</li>`).join('') + '</ol>'
}
</script>

<template>
  <div class="laoyu-resume">
    <!-- ==================== 基本信息 ==================== -->
    <div class="profile" v-if="resumeContent.basicInfo">
      <div class="profile0">
        <div class="profileContent">
          <div class="profileNoImageArea">
            <div class="profile-0">
              <div class="name">{{ resumeContent.basicInfo.name || '姓名' }}</div>
              <div class="telephone" v-if="resumeContent.basicInfo.phone">
                <span v-html="phoneSvg"></span>
                {{ resumeContent.basicInfo.phone }}
              </div>
              <div class="email" v-if="resumeContent.basicInfo.email">
                <span v-html="emailSvg"></span>
                {{ resumeContent.basicInfo.email }}
              </div>
            </div>
            <div class="profile-1">
              <div class="workPlace" v-if="resumeContent.basicInfo.location">
                <span v-html="locationSvg"></span>
                {{ resumeContent.basicInfo.location }}
              </div>
              <div class="website" v-if="resumeContent.basicInfo.website">
                🔗 {{ resumeContent.basicInfo.website }}
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>

    <!-- ==================== 个人总结 ==================== -->
    <div class="module-section" v-if="resumeContent.summary">
      <div class="icon-title">
        <div class="title">
          <span>个人总结</span>
        </div>
      </div>
      <div class="summaryContent">
        <p>{{ resumeContent.summary }}</p>
      </div>
    </div>

    <!-- ==================== 教育经历 ==================== -->
    <div class="module-section" v-if="resumeContent.education?.length">
      <div class="icon-title">
        <div class="title">
          <span>教育经历</span>
        </div>
      </div>
      <div class="educationItem" v-for="(edu, index) in resumeContent.education" :key="index">
        <div class="educationHeader">
          <div class="school">{{ edu.school }}</div>
          <div class="major">{{ edu.major }}</div>
          <div class="academicDegree">{{ edu.degree }}</div>
          <div class="startToEndTime">{{ edu.startDate }} ~ {{ edu.endDate }}</div>
        </div>
        <div class="educationDesc" v-if="edu.description">
          <p>{{ edu.description }}</p>
        </div>
      </div>
    </div>

    <!-- ==================== 专业技能 ==================== -->
    <div class="module-section" v-if="resumeContent.skills?.length">
      <div class="icon-title">
        <div class="title">
          <span>专业技能</span>
        </div>
      </div>
      <div class="skillItem">
        <div class="skillHeader">
          <div class="skillTags">
            <span v-for="(skill, index) in resumeContent.skills" :key="index" class="skillTag">
              {{ skill }}
            </span>
          </div>
        </div>
      </div>
    </div>

    <!-- ==================== 工作经历 ==================== -->
    <div class="module-section" v-if="resumeContent.workExperience?.length">
      <div class="icon-title">
        <div class="title">
          <span>工作经历</span>
        </div>
      </div>
      <div class="jobItem" v-for="(work, index) in resumeContent.workExperience" :key="index">
        <div class="jobHeader1">
          <div class="jobCompanyName">{{ work.company }}</div>
          <div class="jobTime">{{ work.startDate }} ~ {{ work.endDate }}</div>
        </div>
        <div class="jobHeader2">
          <div class="jobName">{{ work.position }}</div>
        </div>
        <div class="jobDetail" v-if="work.description || work.achievements?.length">
          <div class="rich-text-viewer">
            <p v-if="work.description">{{ work.description }}</p>
            <ol v-if="work.achievements?.length">
              <li v-for="(ach, aIdx) in work.achievements" :key="aIdx">
                <strong>{{ ach }}</strong>
              </li>
            </ol>
          </div>
        </div>
      </div>
    </div>

    <!-- ==================== 项目经历 ==================== -->
    <div class="module-section" v-if="resumeContent.projectExperience?.length">
      <div class="icon-title">
        <div class="title">
          <span>项目经历</span>
        </div>
      </div>
      <div class="projectItem" v-for="(proj, index) in resumeContent.projectExperience" :key="index">
        <div class="projectHeader1">
          <div class="projectName">{{ proj.name }}</div>
          <div class="projectTime">{{ proj.startDate }} ~ {{ proj.endDate }}</div>
        </div>
        <div class="projectHeader2">
          <div class="projectRole">{{ proj.role }}</div>
          <div class="projectTechStack" v-if="proj.techStack?.length">
            <span v-for="(tech, tIdx) in proj.techStack" :key="tIdx" class="techBadge">
              {{ tech }}
            </span>
          </div>
        </div>
        <div class="projectDetail" v-if="proj.description || proj.highlights?.length">
          <div class="rich-text-viewer">
            <p v-if="proj.description">{{ proj.description }}</p>
            <ol v-if="proj.highlights?.length">
              <li v-for="(hl, hIdx) in proj.highlights" :key="hIdx">
                <strong>{{ hl }}</strong>
              </li>
            </ol>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<style scoped>
/* ========== 全局重置 ========== */
.laoyu-resume {
  font-family: "Microsoft YaHei", -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, "Helvetica Neue", Arial, "Noto Sans", sans-serif;
  font-size: 14px;
  line-height: 1.6;
  color: rgba(0, 0, 0, 0.85);
  padding: 30px;
}

/* ========== 基本信息模块 ========== */
.profile {
  padding: 0 0 16px;
  border-bottom: 1px solid #000;
  margin-bottom: 16px;
}

.profile0 {
  padding-bottom: 8px;
}

.profileContent {
  display: flex;
  justify-content: space-between;
}

.profileNoImageArea {
  display: block;
  flex: 1 1 0%;
}

.profile-0 {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 4px;
  margin-bottom: 8px;
}

.profile-1 {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 4px;
  margin-top: 4px;
}

.name {
  word-break: break-word;
  overflow-wrap: break-word;
  margin: 0 16px 0 0;
  display: inline-block;
  font-weight: bold;
  font-size: 22px;
  text-align: left;
}

.telephone, .email, .workPlace, .website {
  word-break: break-word;
  overflow-wrap: break-word;
  margin-right: 12px;
  display: inline-flex;
  align-items: center;
  font-size: 13px;
  color: rgba(0, 0, 0, 0.65);
}

.telephone :deep(svg), .email :deep(svg), .workPlace :deep(svg) {
  margin-right: 4px;
  color: #000;
  width: 1em;
  height: 1em;
}

/* ========== 模块通用样式 ========== */
.module-section {
  padding: 4px 0 0;
  margin-bottom: 16px;
}

.icon-title {
  margin-top: 4px;
}

.title {
  margin: 0 0 12px;
  background: transparent;
  width: 100%;
  position: relative;
  padding: 4px 0;
  font-weight: bold;
  font-size: 18px;
  border-bottom: 1px solid #000;
}

.title span {
  background: transparent;
  color: #000;
}

/* ========== 教育经历 ========== */
.educationItem {
  padding-bottom: 8px;
}

.educationHeader {
  display: flex;
  justify-content: flex-start;
  align-items: baseline;
  width: 100%;
  flex-wrap: wrap;
}

.school {
  word-break: break-word;
  overflow-wrap: break-word;
  margin: 0 8px;
  font-weight: bold;
  font-size: 15px;
}

.major, .academicDegree {
  word-break: break-word;
  overflow-wrap: break-word;
  margin: 0 8px;
  color: rgba(0, 0, 0, 0.45);
}

.startToEndTime {
  word-break: break-word;
  overflow-wrap: break-word;
  margin: 0 8px 0 auto;
  color: rgba(0, 0, 0, 0.45);
}

.educationDesc {
  margin-top: 4px;
  font-size: 13px;
  color: rgba(0, 0, 0, 0.65);
}

.educationDesc p {
  margin: 2px 0;
}

/* ========== 专业技能 ========== */
.skillItem {
  padding-bottom: 8px;
}

.skillHeader {
  display: flex;
  flex-wrap: wrap;
  align-items: flex-start;
}

.skillTags {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.skillTag {
  display: inline-block;
  padding: 4px 12px;
  background: #f0f0f0;
  border-radius: 4px;
  font-size: 13px;
  color: rgba(0, 0, 0, 0.75);
}

/* ========== 工作经历 ========== */
.jobItem {
  padding-bottom: 12px;
  margin-bottom: 8px;
  border-bottom: 1px dashed #e8e8e8;
}

.jobItem:last-child {
  border-bottom: none;
  margin-bottom: 0;
  padding-bottom: 0;
}

.jobHeader1 {
  display: flex;
  justify-content: flex-start;
  align-items: baseline;
}

.jobCompanyName {
  word-break: break-word;
  overflow-wrap: break-word;
  margin: 0;
  font-weight: bold;
  font-size: 15px;
}

.jobTime {
  word-break: break-word;
  overflow-wrap: break-word;
  margin: 0 0 0 auto;
  color: rgba(0, 0, 0, 0.45);
  font-size: 13px;
}

.jobHeader2 {
  display: flex;
  justify-content: flex-start;
  align-items: center;
  margin-top: 2px;
}

.jobName {
  word-break: break-word;
  overflow-wrap: break-word;
  margin: 0;
  color: rgba(0, 0, 0, 0.45);
  font-size: 13px;
}

.jobDetail {
  margin-top: 8px;
  word-break: break-word;
  overflow-wrap: break-word;
}

.rich-text-viewer p {
  margin: 4px 0;
  font-size: 13px;
  color: rgba(0, 0, 0, 0.65);
}

.rich-text-viewer ol, .rich-text-viewer ul {
  padding-left: 20px;
  margin: 4px 0;
}

.rich-text-viewer li {
  margin: 2px 0;
  font-size: 13px;
  color: rgba(0, 0, 0, 0.65);
}

.rich-text-viewer strong {
  font-weight: bold;
}

/* ========== 项目经历 ========== */
.projectItem {
  padding-bottom: 12px;
  margin-bottom: 8px;
  border-bottom: 1px dashed #e8e8e8;
}

.projectItem:last-child {
  border-bottom: none;
  margin-bottom: 0;
  padding-bottom: 0;
}

.projectHeader1 {
  display: flex;
  justify-content: flex-start;
  align-items: baseline;
}

.projectName {
  word-break: break-word;
  overflow-wrap: break-word;
  font-weight: bold;
  font-size: 15px;
}

.projectTime {
  word-break: break-word;
  overflow-wrap: break-word;
  color: rgba(0, 0, 0, 0.45);
  margin-left: auto;
  font-size: 13px;
}

.projectHeader2 {
  display: flex;
  justify-content: flex-start;
  align-items: center;
  margin-top: 2px;
  gap: 8px;
}

.projectRole {
  word-break: break-word;
  overflow-wrap: break-word;
  color: rgba(0, 0, 0, 0.45);
  font-size: 13px;
}

.projectTechStack {
  display: flex;
  flex-wrap: wrap;
  gap: 4px;
  margin-left: auto;
}

.techBadge {
  display: inline-block;
  padding: 2px 8px;
  background: #f0f0f0;
  border-radius: 3px;
  font-size: 11px;
  color: rgba(0, 0, 0, 0.65);
}

.projectDetail {
  margin-top: 8px;
  word-break: break-word;
  overflow-wrap: break-word;
}

/* ========== 总结 ========== */
.summaryContent p {
  font-size: 13px;
  color: rgba(0, 0, 0, 0.65);
  line-height: 1.8;
  margin: 0;
}
</style>
