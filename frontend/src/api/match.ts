import apiClient from './client'
import type { MatchAnalysisResult, MatchHistoryItem } from './types'

export const matchApi = {
  analyze: (data: { resumeId: number; jdContent: string }) =>
    apiClient.post<any, MatchAnalysisResult>('/match/analyze', data),
  getHistory: () =>
    apiClient.get<any, MatchHistoryItem[]>('/match/history'),
  getDetail: (id: number) =>
    apiClient.get<any, MatchAnalysisResult>(`/match/${id}`)
}
