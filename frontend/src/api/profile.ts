import apiClient from './client'
import type { ProfileForm, ProfileResponse } from './types'

export const profileApi = {
  getProfile: () => apiClient.get<any, ProfileResponse>('/profile'),
  updateProfile: (data: ProfileForm) => apiClient.put<any, ProfileResponse>('/profile', data)
}
