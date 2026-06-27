import apiClient from './client'

export interface LoginData {
  username: string
  password: string
}

export interface RegisterData {
  username: string
  password: string
}

export interface AuthResponseData {
  token: string
  userId: number
  username: string
  remainingCredits: number
}

export const authApi = {
  login: (data: LoginData): Promise<AuthResponseData> =>
    apiClient.post('/auth/login', data),

  register: (data: RegisterData): Promise<AuthResponseData> =>
    apiClient.post('/auth/register', data),

  getMe: (): Promise<AuthResponseData> =>
    apiClient.get('/auth/me')
}
