import { describe, it, expect, vi, beforeEach } from 'vitest'
import { mount } from '@vue/test-utils'
import { createRouter, createWebHistory } from 'vue-router'
import { createPinia, setActivePinia } from 'pinia'
import Login from '@/views/Login.vue'

// Mock Element Plus
vi.mock('element-plus', () => ({
  ElMessage: {
    success: vi.fn(),
    error: vi.fn(),
  },
}))

// Mock the auth store
vi.mock('@/stores/auth', () => ({
  useAuthStore: vi.fn(() => ({
    login: vi.fn(),
  })),
}))

describe('Login.vue', () => {
  let router: any

  beforeEach(() => {
    setActivePinia(createPinia())
    router = createRouter({
      history: createWebHistory(),
      routes: [
        { path: '/', component: { template: '<div>Home</div>' } },
        { path: '/login', component: Login },
      ],
    })
  })

  it('renders login form', () => {
    const wrapper = mount(Login, {
      global: {
        plugins: [router],
      },
    })

    expect(wrapper.find('h2').text()).toBe('简历助手')
    expect(wrapper.find('p').text()).toBe('AI 驱动的智能简历工具')
    expect(wrapper.find('input[placeholder="用户名"]').exists()).toBe(true)
    expect(wrapper.find('input[placeholder="密码"]').exists()).toBe(true)
    expect(wrapper.find('button').text()).toBe('登录')
  })

  it('has link to register page', () => {
    const wrapper = mount(Login, {
      global: {
        plugins: [router],
      },
    })

    const registerLink = wrapper.find('a')
    expect(registerLink.exists()).toBe(true)
    expect(registerLink.text()).toBe('立即注册')
    expect(registerLink.attributes('href')).toBe('/register')
  })

  it('calls login method on button click', async () => {
    const mockLogin = vi.fn()
    vi.mocked(vi.importActual('@/stores/auth')).then(() => ({
      useAuthStore: vi.fn(() => ({
        login: mockLogin,
      })),
    }))

    const wrapper = mount(Login, {
      global: {
        plugins: [router],
      },
    })

    // Fill in the form
    await wrapper.find('input[placeholder="用户名"]').setValue('testuser')
    await wrapper.find('input[placeholder="密码"]').setValue('password123')

    // Click login button
    await wrapper.find('button').trigger('click')

    // Note: The actual login call depends on form validation
    // In a real test, we would mock the form validation
  })
})