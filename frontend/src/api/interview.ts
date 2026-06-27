import apiClient from './client'

export interface InterviewStartRequest {
  resumeId: number
  jdContent: string
  mode: string
}

export interface InterviewStartResponse {
  interviewId: number
  wsUrl: string
  status: string
}

export interface InterviewMessage {
  type: 'interviewer' | 'candidate' | 'system'
  content: string
  timestamp: string
  metadata?: Record<string, any>
}

export interface InterviewReportData {
  technicalDepth: number
  communication: number
  projectUnderstanding: number
  adaptability: number
  totalScore: number
  passRate: string
  strengths: string[]
  weaknesses: string[]
  suggestions: string[]
  overallComment: string
}

export interface InterviewHistoryItem {
  id: number
  resumeId: number
  resumeTitle: string
  jdSnippet: string
  mode: string
  status: string
  totalScore: number
  startedAt: string
  completedAt: string
}

export const interviewApi = {
  start: (data: InterviewStartRequest) =>
    apiClient.post<any, InterviewStartResponse>('/interview/start', data),
  end: (id: number) =>
    apiClient.post<any, InterviewReportData>(`/interview/${id}/end`),
  getReport: (id: number) =>
    apiClient.get<any, InterviewReportData>(`/interview/${id}/report`),
  getHistory: () =>
    apiClient.get<any, InterviewHistoryItem[]>('/interview/history')
}
