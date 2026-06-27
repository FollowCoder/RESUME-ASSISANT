import axios from 'axios'
import type { AxiosInstance, InternalAxiosRequestConfig, AxiosResponse } from 'axios'
import { ElMessage, ElMessageBox } from 'element-plus'
import { useAuthStore } from '@/stores/auth'
import router from '@/router'

const apiClient: AxiosInstance = axios.create({
  baseURL: '/api',
  timeout: 30000,
  headers: {
    'Content-Type': 'application/json'
  }
})

// 请求拦截器：添加 JWT token
apiClient.interceptors.request.use(
  (config: InternalAxiosRequestConfig) => {
    const token = localStorage.getItem('token')
    if (token) {
      config.headers.Authorization = `Bearer ${token}`
    }
    return config
  },
  (error) => {
    return Promise.reject(error)
  }
)

// 响应拦截器：统一错误处理 + ApiResponse 自动解包
apiClient.interceptors.response.use(
  (response: AxiosResponse) => {
    const body = response.data
    // 自动解包 ApiResponse 包装：{ success, data, message? }
    if (body && typeof body === 'object' && 'success' in body && 'data' in body) {
      return body.data
    }
    return body
  },
  (error) => {
    if (error.response) {
      const { status, data } = error.response

      switch (status) {
        case 401:
          // 未认证，跳转登录
          {
            const authStore = useAuthStore()
            authStore.logout()
            router.push({ path: '/login', query: { redirect: router.currentRoute.value.fullPath } })
            ElMessage.error('登录已过期，请重新登录')
          }
          break
        case 402:
          // 余额不足
          ElMessageBox.confirm(
            data?.message || '余额不足，请充值后继续使用',
            '余额不足',
            {
              confirmButtonText: '去充值',
              cancelButtonText: '取消',
              type: 'warning'
            }
          ).then(() => {
            // 触发全局事件打开充值弹窗
            window.dispatchEvent(new CustomEvent('open-purchase-dialog'))
          }).catch(() => {
            // 用户取消
          })
          break
        case 403:
          ElMessage.error('没有权限执行此操作')
          break
        case 429:
          ElMessage.error('请求过于频繁，请稍后再试')
          break
        case 500:
          ElMessage.error(data?.message || '服务器错误，请稍后重试')
          break
        default:
          ElMessage.error(data?.message || '请求失败，请稍后重试')
      }
    } else {
      ElMessage.error('网络连接异常，请检查网络')
    }

    return Promise.reject(error)
  }
)

export default apiClient
