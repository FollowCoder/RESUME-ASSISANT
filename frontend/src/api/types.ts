export interface EducationInfo {
  degree: string
  school: string
  major: string
}

export interface ProfileForm {
  workYears: string
  techDirection: string[]
  targetPosition: string
  targetIndustry: string[]
  salaryRange: string
  education: EducationInfo[]
  coreSkills: string[]
}

export interface ProfileResponse extends ProfileForm {
  id: number
  updatedAt: string
  profileCompleted: boolean
}

// ========== 简历相关类型 ==========

export interface ResumeListItem {
  id: number
  title: string
  language: string
  templateId: string
  updatedAt: string
}

export interface ResumeDetail {
  id: number
  title: string
  content: string
  language: string
  templateId: string
  updatedAt: string
}

// ========== 简历编写相关类型 ==========

export interface ChatResponse {
  reply: string
  stage: string
  completed: boolean
  resumeContent?: ResumeContent
  resumeId?: number
}

export interface ResumeContent {
  basicInfo?: {
    name?: string
    phone?: string
    email?: string
    location?: string
    website?: string
  }
  education?: EducationItem[]
  workExperience?: WorkExperienceItem[]
  projectExperience?: ProjectExperienceItem[]
  skills?: string[]
  summary?: string
}

export interface EducationItem {
  school: string
  degree: string
  major: string
  startDate: string
  endDate: string
  description?: string
}

export interface WorkExperienceItem {
  company: string
  position: string
  startDate: string
  endDate: string
  description?: string
  achievements?: string[]
}

export interface ProjectExperienceItem {
  name: string
  role: string
  startDate: string
  endDate: string
  description?: string
  techStack?: string[]
  highlights?: string[]
}

export interface TemplateInfo {
  id: string
  name: string
  description: string
  previewUrl?: string
}

export interface ResumeFormData {
  basicInfo: {
    name: string
    phone: string
    email: string
    location: string
    website: string
  }
  education: EducationItem[]
  workExperience: WorkExperienceItem[]
  projectExperience: ProjectExperienceItem[]
  skills: string[]
  summary: string
  title: string
  language: string
  templateId: string
}

// ========== JD 匹配相关类型 ==========

export interface MatchDimensions {
  skillMatch: number
  experienceMatch: number
  educationMatch: number
  keywordCoverage: number
}

export interface MatchGapItem {
  category: string
  requirement: string
  suggestion: string
}

export interface MatchAnalysisResult {
  totalScore: number
  level: string
  dimensions: MatchDimensions
  gaps: MatchGapItem[]
  improvements: string[]
}

export interface MatchHistoryItem {
  id: number
  resumeId: number
  resumeTitle: string
  jdSnippet: string
  matchScore: number
  level: string
  createdAt: string
}
