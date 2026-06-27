<script setup lang="ts">
import { reactive, ref } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { useAuthStore } from '@/stores/auth'
import { ElMessage } from 'element-plus'
import type { FormInstance, FormRules } from 'element-plus'
import { User, Lock } from '@element-plus/icons-vue'

const router = useRouter()
const route = useRoute()
const authStore = useAuthStore()

const formRef = ref<FormInstance>()
const loading = ref(false)

const form = reactive({
  username: '',
  password: ''
})

const rules: FormRules = {
  username: [
    { required: true, message: '请输入用户名', trigger: 'blur' }
  ],
  password: [
    { required: true, message: '请输入密码', trigger: 'blur' }
  ]
}

async function handleLogin() {
  const valid = await formRef.value?.validate().catch(() => false)
  if (!valid) return

  loading.value = true
  try {
    await authStore.login(form.username, form.password)
    ElMessage.success('登录成功')
    const redirect = (route.query.redirect as string) || '/'
    router.push(redirect)
  } catch (error: any) {
    // 错误已由 API 拦截器处理
  } finally {
    loading.value = false
  }
}
</script>

<template>
  <div class="login-page">
    <!-- Animated background orbs -->
    <div class="bg-orbs">
      <div class="orb orb-1"></div>
      <div class="orb orb-2"></div>
      <div class="orb orb-3"></div>
    </div>

    <!-- Floating geometric shapes -->
    <div class="bg-shapes">
      <div class="shape shape-1"></div>
      <div class="shape shape-2"></div>
      <div class="shape shape-3"></div>
      <div class="shape shape-4"></div>
    </div>

    <div class="login-card">
      <!-- Brand -->
      <div class="login-brand">
        <div class="brand-icon">
          <svg width="32" height="32" viewBox="0 0 28 28" fill="none">
            <rect x="2" y="4" width="18" height="22" rx="3" stroke="currentColor" stroke-width="1.5" fill="none"/>
            <line x1="7" y1="10" x2="15" y2="10" stroke="currentColor" stroke-width="1.5" stroke-linecap="round"/>
            <line x1="7" y1="14" x2="15" y2="14" stroke="currentColor" stroke-width="1.5" stroke-linecap="round"/>
            <line x1="7" y1="18" x2="12" y2="18" stroke="currentColor" stroke-width="1.5" stroke-linecap="round"/>
            <circle cx="20" cy="20" r="7" fill="currentColor" opacity="0.9"/>
            <path d="M18 20h4M20 18v4" stroke="#1a1f3d" stroke-width="1.8" stroke-linecap="round"/>
          </svg>
        </div>
        <h2 class="brand-title">简历助手</h2>
        <p class="brand-subtitle">AI 驱动的智能简历工具</p>
      </div>

      <!-- Form -->
      <el-form
        ref="formRef"
        :model="form"
        :rules="rules"
        size="large"
        @keyup.enter="handleLogin"
        class="login-form"
      >
        <el-form-item prop="username">
          <el-input
            v-model="form.username"
            placeholder="用户名"
            :prefix-icon="User"
            class="glass-input"
          />
        </el-form-item>

        <el-form-item prop="password">
          <el-input
            v-model="form.password"
            type="password"
            placeholder="密码"
            show-password
            :prefix-icon="Lock"
            class="glass-input"
          />
        </el-form-item>

        <el-form-item>
          <el-button
            :loading="loading"
            class="login-btn"
            @click="handleLogin"
          >
            <span v-if="!loading">登 录</span>
            <span v-else>登录中...</span>
          </el-button>
        </el-form-item>
      </el-form>

      <div class="login-footer">
        <span>还没有账号？</span>
        <router-link to="/register" class="footer-link">立即注册</router-link>
      </div>
    </div>
  </div>
</template>

<style lang="scss" scoped>
.login-page {
  display: flex;
  align-items: center;
  justify-content: center;
  min-height: 100vh;
  background: var(--color-primary-dark);
  position: relative;
  overflow: hidden;
}

// ===== Background Effects =====
.bg-orbs {
  position: absolute;
  inset: 0;
  pointer-events: none;

  .orb {
    position: absolute;
    border-radius: 50%;
    filter: blur(80px);
    opacity: 0.5;
  }

  .orb-1 {
    width: 500px;
    height: 500px;
    background: radial-gradient(circle, rgba(232, 148, 58, 0.35) 0%, transparent 70%);
    top: -10%;
    right: -5%;
    animation: floatOrb1 15s ease-in-out infinite;
  }

  .orb-2 {
    width: 400px;
    height: 400px;
    background: radial-gradient(circle, rgba(45, 53, 97, 0.6) 0%, transparent 70%);
    bottom: -10%;
    left: -5%;
    animation: floatOrb2 18s ease-in-out infinite;
  }

  .orb-3 {
    width: 300px;
    height: 300px;
    background: radial-gradient(circle, rgba(147, 112, 219, 0.25) 0%, transparent 70%);
    top: 50%;
    left: 50%;
    transform: translate(-50%, -50%);
    animation: floatOrb3 12s ease-in-out infinite;
  }
}

.bg-shapes {
  position: absolute;
  inset: 0;
  pointer-events: none;

  .shape {
    position: absolute;
    border: 1px solid rgba(255, 255, 255, 0.06);
    border-radius: 12px;
  }

  .shape-1 {
    width: 80px;
    height: 80px;
    top: 15%;
    left: 10%;
    transform: rotate(15deg);
    animation: floatShape 20s ease-in-out infinite;
  }

  .shape-2 {
    width: 50px;
    height: 50px;
    top: 70%;
    right: 15%;
    border-radius: 50%;
    animation: floatShape 16s ease-in-out infinite reverse;
  }

  .shape-3 {
    width: 120px;
    height: 120px;
    bottom: 20%;
    left: 20%;
    transform: rotate(-10deg);
    animation: floatShape 22s ease-in-out infinite;
  }

  .shape-4 {
    width: 60px;
    height: 60px;
    top: 25%;
    right: 25%;
    border-radius: 50%;
    animation: floatShape 14s ease-in-out infinite reverse;
  }
}

// ===== Login Card =====
.login-card {
  width: 420px;
  padding: 44px 40px 36px;
  background: rgba(255, 255, 255, 0.07);
  backdrop-filter: blur(24px);
  -webkit-backdrop-filter: blur(24px);
  border: 1px solid rgba(255, 255, 255, 0.12);
  border-radius: var(--radius-xl);
  position: relative;
  z-index: 10;
  animation: scaleIn 0.5s ease both;
  box-shadow: 0 24px 80px rgba(0, 0, 0, 0.3);
}

// ===== Brand =====
.login-brand {
  text-align: center;
  margin-bottom: 36px;

  .brand-icon {
    width: 60px;
    height: 60px;
    margin: 0 auto 16px;
    border-radius: 16px;
    background: linear-gradient(135deg, var(--color-accent) 0%, var(--color-accent-light) 100%);
    display: flex;
    align-items: center;
    justify-content: center;
    color: var(--color-primary-dark);
    box-shadow: 0 8px 24px rgba(232, 148, 58, 0.3);
    animation: fadeInUp 0.6s ease 0.1s both;
  }

  .brand-title {
    font-size: 32px;
    font-weight: 700;
    color: #ffffff;
    margin: 0 0 8px;
    font-family: var(--font-heading);
    letter-spacing: 1px;
    animation: fadeInUp 0.6s ease 0.2s both;
  }

  .brand-subtitle {
    font-size: 14px;
    color: rgba(255, 255, 255, 0.5);
    margin: 0;
    font-weight: 400;
    animation: fadeInUp 0.6s ease 0.3s both;
  }
}

// ===== Form =====
.login-form {
  animation: fadeInUp 0.6s ease 0.35s both;

  :deep(.el-form-item) {
    margin-bottom: 20px;
  }

  :deep(.el-input__wrapper) {
    background: rgba(255, 255, 255, 0.08);
    border: 1px solid rgba(255, 255, 255, 0.1);
    box-shadow: none;
    border-radius: var(--radius-base);
    padding: 4px 12px;
    transition: all var(--transition-smooth);

    &:hover {
      border-color: rgba(255, 255, 255, 0.2);
      background: rgba(255, 255, 255, 0.1);
    }

    &.is-focus {
      border-color: var(--color-accent);
      background: rgba(255, 255, 255, 0.12);
      box-shadow: 0 0 0 3px rgba(232, 148, 58, 0.15);
    }
  }

  :deep(.el-input__inner) {
    color: #ffffff;
    font-size: 14px;

    &::placeholder {
      color: rgba(255, 255, 255, 0.35);
    }
  }

  :deep(.el-input__prefix) {
    color: rgba(255, 255, 255, 0.45);
  }

  :deep(.el-form-item__error) {
    color: #f0a85c;
  }
}

// ===== Login Button =====
.login-btn {
  width: 100%;
  height: 48px;
  font-size: 15px;
  font-weight: 600;
  letter-spacing: 2px;
  border: none;
  border-radius: var(--radius-base);
  background: linear-gradient(135deg, var(--color-accent) 0%, var(--color-accent-dark) 100%);
  color: #ffffff;
  transition: all var(--transition-smooth);
  position: relative;
  overflow: hidden;

  &::before {
    content: '';
    position: absolute;
    inset: 0;
    background: linear-gradient(135deg, var(--color-accent-light) 0%, var(--color-accent) 100%);
    opacity: 0;
    transition: opacity var(--transition-smooth);
  }

  &:hover {
    transform: translateY(-1px);
    box-shadow: 0 8px 24px rgba(232, 148, 58, 0.35);

    &::before {
      opacity: 1;
    }
  }

  &:active {
    transform: translateY(0);
  }

  span {
    position: relative;
    z-index: 1;
  }
}

// ===== Footer =====
.login-footer {
  text-align: center;
  margin-top: 24px;
  font-size: 13px;
  color: rgba(255, 255, 255, 0.45);
  animation: fadeInUp 0.6s ease 0.45s both;

  .footer-link {
    color: var(--color-accent-light);
    margin-left: 6px;
    font-weight: 500;
    transition: color var(--transition-fast);

    &:hover {
      color: var(--color-accent);
      text-decoration: underline;
    }
  }
}

// ===== Animations =====
@keyframes floatOrb1 {
  0%, 100% { transform: translate(0, 0) scale(1); }
  33% { transform: translate(-30px, 40px) scale(1.05); }
  66% { transform: translate(20px, -20px) scale(0.95); }
}

@keyframes floatOrb2 {
  0%, 100% { transform: translate(0, 0) scale(1); }
  33% { transform: translate(40px, -30px) scale(1.08); }
  66% { transform: translate(-20px, 20px) scale(0.92); }
}

@keyframes floatOrb3 {
  0%, 100% { transform: translate(-50%, -50%) scale(1); }
  50% { transform: translate(-50%, -50%) scale(1.15); }
}

@keyframes floatShape {
  0%, 100% { transform: translateY(0) rotate(0deg); }
  50% { transform: translateY(-20px) rotate(5deg); }
}

// ===== Responsive =====
@media (max-width: 480px) {
  .login-card {
    width: calc(100% - 32px);
    padding: 32px 24px 28px;
  }
}
</style>
