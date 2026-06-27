import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import { authApi } from '@/api/auth'
import type { AuthResponseData } from '@/api/auth'

export const useAuthStore = defineStore('auth', () => {
  const token = ref<string>(localStorage.getItem('token') || '')
  const user = ref<{ id: number; username: string; remainingCredits: number } | null>(null)

  const isAuthenticated = computed(() => !!token.value)

  async function login(username: string, password: string) {
    const res = await authApi.login({ username, password })
    if (res) {
      setAuth(res)
    }
  }

  async function register(username: string, password: string) {
    const res = await authApi.register({ username, password })
    if (res) {
      setAuth(res)
    }
  }

  async function fetchUser() {
    try {
      const res = await authApi.getMe()
      if (res) {
        user.value = {
          id: res.userId,
          username: res.username,
          remainingCredits: res.remainingCredits
        }
      }
    } catch {
      logout()
    }
  }

  function logout() {
    token.value = ''
    user.value = null
    localStorage.removeItem('token')
  }

  function setAuth(data: AuthResponseData) {
    token.value = data.token
    localStorage.setItem('token', data.token)
    user.value = {
      id: data.userId,
      username: data.username,
      remainingCredits: data.remainingCredits
    }
  }

  return { token, user, isAuthenticated, login, register, fetchUser, logout }
})
