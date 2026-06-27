import apiClient from './client'
import type { ResumeListItem, ResumeDetail, TemplateInfo, ChatResponse } from './types'

export const resumeApi = {
  getList: () =>
    apiClient.get<any, ResumeListItem[]>('/resume/list'),
  getDetail: (id: number) =>
    apiClient.get<any, ResumeDetail>(`/resume/${id}`),
  chat: (data: { message: string; sessionId: string }) =>
    apiClient.post<any, ChatResponse>('/resume/chat', data),
  submitForm: (data: any) =>
    apiClient.post<any, any>('/resume/form', data),
  update: (id: number, data: any) =>
    apiClient.put<any, any>(`/resume/${id}`, data),
  getTemplates: () =>
    apiClient.get<any, TemplateInfo[]>('/resume/templates'),
  export: (data: { resumeId: number; format: string; templateId?: string; language?: string }) =>
    apiClient.post('/resume/export', data, { responseType: 'blob' }) as Promise<Blob>
}
