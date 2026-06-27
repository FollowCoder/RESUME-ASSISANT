import { defineStore } from 'pinia'
import { ref } from 'vue'
import { resumeApi } from '@/api/resume'
import type { TemplateInfo, ResumeContent } from '@/api/types'
import { ElMessage } from 'element-plus'

function generateSessionId(): string {
  return 'xxxxxxxx-xxxx-4xxx-yxxx-xxxxxxxxxxxx'.replace(/[xy]/g, (c) => {
    const r = (Math.random() * 16) | 0
    const v = c === 'x' ? r : (r & 0x3) | 0x8
    return v.toString(16)
  })
}

export interface ChatMessageItem {
  role: 'user' | 'assistant'
  content: string
}

export const useResumeStore = defineStore('resume', () => {
  const currentSessionId = ref(generateSessionId())
  const chatMessages = ref<ChatMessageItem[]>([])
  const currentStage = ref('GREETING')
  const isCompleted = ref(false)
  const generatedResumeId = ref<number | null>(null)
  const resumeContent = ref<ResumeContent | null>(null)
  const templates = ref<TemplateInfo[]>([])
  const chatLoading = ref(false)

  async function sendChatMessage(message: string) {
    chatMessages.value.push({ role: 'user', content: message })
    chatLoading.value = true
    try {
      const res = await resumeApi.chat({
        message,
        sessionId: currentSessionId.value
      })
      chatMessages.value.push({ role: 'assistant', content: res.reply })
      currentStage.value = res.stage
      isCompleted.value = res.completed
      if (res.resumeContent) {
        resumeContent.value = res.resumeContent
      }
      if (res.resumeId) {
        generatedResumeId.value = res.resumeId
      }
    } catch (e) {
      // Error already handled by interceptor
    } finally {
      chatLoading.value = false
    }
  }

  async function loadTemplates() {
    try {
      const res = await resumeApi.getTemplates()
      templates.value = res
    } catch (e) {
      // Error already handled by interceptor
    }
  }

  async function exportResume(resumeId: number, format: string, templateId?: string) {
    try {
      const blob = await resumeApi.export({ resumeId, format, templateId })
      const url = URL.createObjectURL(blob)
      const link = document.createElement('a')
      link.href = url
      link.download = `resume.${format}`
      link.click()
      URL.revokeObjectURL(url)
      ElMessage.success('导出成功')
    } catch (e) {
      // Error already handled by interceptor
    }
  }

  function resetChat() {
    currentSessionId.value = generateSessionId()
    chatMessages.value = []
    currentStage.value = 'GREETING'
    isCompleted.value = false
    generatedResumeId.value = null
    resumeContent.value = null
  }

  return {
    currentSessionId,
    chatMessages,
    currentStage,
    isCompleted,
    generatedResumeId,
    resumeContent,
    templates,
    chatLoading,
    sendChatMessage,
    loadTemplates,
    exportResume,
    resetChat
  }
})
