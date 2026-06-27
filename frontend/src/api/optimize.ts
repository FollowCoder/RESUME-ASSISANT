import apiClient from './client'

export interface OptimizationSuggestion {
  id: string
  dimension: string
  original: string
  optimized: string
  reason: string
  severity: string
}

export interface OptimizationResult {
  overallScore: number
  suggestions: OptimizationSuggestion[]
  optimizedContent: string
}

export const optimizeApi = {
  optimizeText: (content: string) =>
    apiClient.post<any, OptimizationResult>('/optimize/text', { content }),

  optimizeFile: (file: File) => {
    const formData = new FormData()
    formData.append('file', file)
    return apiClient.post<any, OptimizationResult>('/optimize/file', formData, {
      headers: { 'Content-Type': 'multipart/form-data' }
    })
  },

  optimizeResume: (resumeId: number) =>
    apiClient.post<any, OptimizationResult>(`/optimize/resume/${resumeId}`),

  applySuggestions: (data: { resumeId: number; acceptedSuggestionIds: string[] }) =>
    apiClient.post<any, any>('/optimize/apply', data)
}
