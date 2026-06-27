<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { useAuthStore } from '@/stores/auth'
import { profileApi } from '@/api/profile'
import { ElMessage } from 'element-plus'
import type { ProfileForm } from '@/api/types'
import CreditBadge from '@/components/CreditBadge.vue'

const router = useRouter()
const authStore = useAuthStore()

const menuItems = [
  { index: '/resume/write', title: '简历编写', icon: 'Edit' },
  { index: '/resume/optimize', title: '简历优化', icon: 'MagicStick' },
  { index: '/jd-match', title: 'JD匹配度', icon: 'Connection' },
  { index: '/interview', title: '模拟面试', icon: 'Microphone' },
  { index: '/profile', title: '个人画像', icon: 'User' }
]

const handleMenuSelect = (index: string) => {
  router.push(index)
}

const handleLogout = () => {
  authStore.logout()
  router.push('/login')
}

// 首次登录引导
const showGuideDialog = ref(false)
const guideSaving = ref(false)
const guideForm = reactive({
  workYears: '',
  techDirection: [] as string[],
  targetPosition: ''
})

const workYearsOptions = ['应届', '1-3年', '3-5年', '5-10年', '10年+']
const techDirectionOptions = ['Java', 'Python', '前端', '大数据', 'AI', 'Go', 'C++', '移动端']

const checkProfileCompleted = async () => {
  try {
    const res = await profileApi.getProfile()
    if (res.success && res.data && !res.data.profileCompleted) {
      showGuideDialog.value = true
    }
  } catch (e) {
    // 静默失败
  }
}

const handleGuideSave = async () => {
  if (!guideForm.workYears || !guideForm.targetPosition) {
    ElMessage.warning('请至少填写工作年限和目标岗位')
    return
  }
  guideSaving.value = true
  try {
    const data: ProfileForm = {
      workYears: guideForm.workYears,
      techDirection: guideForm.techDirection,
      targetPosition: guideForm.targetPosition,
      targetIndustry: [],
      salaryRange: '',
      education: [{ degree: '', school: '', major: '' }],
      coreSkills: []
    }
    const res = await profileApi.updateProfile(data)
    if (res.success) {
      ElMessage.success('画像已保存，前往完善更多信息')
      showGuideDialog.value = false
      router.push('/profile')
    }
  } catch (e) {
    // 错误已在拦截器中处理
  } finally {
    guideSaving.value = false
  }
}

const handleGuideSkip = () => {
  showGuideDialog.value = false
}

onMounted(() => {
  if (authStore.isAuthenticated) {
    checkProfileCompleted()
  }
})
</script>

<template>
  <el-container class="main-layout">
    <!-- 左侧导航 -->
    <el-aside width="240px" class="sidebar">
      <div class="logo">
        <div class="logo-icon">
          <svg width="28" height="28" viewBox="0 0 28 28" fill="none">
            <rect x="2" y="4" width="18" height="22" rx="3" stroke="currentColor" stroke-width="1.5" fill="none"/>
            <line x1="7" y1="10" x2="15" y2="10" stroke="currentColor" stroke-width="1.5" stroke-linecap="round"/>
            <line x1="7" y1="14" x2="15" y2="14" stroke="currentColor" stroke-width="1.5" stroke-linecap="round"/>
            <line x1="7" y1="18" x2="12" y2="18" stroke="currentColor" stroke-width="1.5" stroke-linecap="round"/>
            <circle cx="20" cy="20" r="7" fill="currentColor" opacity="0.9"/>
            <path d="M18 20h4M20 18v4" stroke="#1a1f3d" stroke-width="1.8" stroke-linecap="round"/>
          </svg>
        </div>
        <div class="logo-text">
          <h2>简历助手</h2>
          <span class="logo-sub">AI Resume Studio</span>
        </div>
      </div>

      <nav class="sidebar-nav">
        <el-menu
          :default-active="$route.path"
          class="sidebar-menu"
          @select="handleMenuSelect"
          background-color="transparent"
          text-color="rgba(255,255,255,0.65)"
          active-text-color="#ffffff"
        >
          <el-menu-item
            v-for="item in menuItems"
            :key="item.index"
            :index="item.index"
            class="nav-item"
          >
            <div class="nav-indicator"></div>
            <el-icon :size="18">
              <component :is="item.icon" />
            </el-icon>
            <span>{{ item.title }}</span>
          </el-menu-item>
        </el-menu>
      </nav>

      <div class="sidebar-footer">
        <div class="sidebar-brand">
          <span class="brand-dot"></span>
          Powered by AI
        </div>
      </div>
    </el-aside>

    <el-container class="right-container">
      <!-- 顶部栏 -->
      <el-header class="header" height="72px">
        <div class="header-left">
          <h1 class="page-title">{{ $route.meta.title }}</h1>
        </div>
        <div class="header-right">
          <CreditBadge />
          <div class="user-pill">
            <div class="user-avatar">
              {{ (authStore.user?.username || 'U')[0].toUpperCase() }}
            </div>
            <span class="user-name">{{ authStore.user?.username || '用户' }}</span>
          </div>
          <el-button text class="logout-btn" @click="handleLogout">
            退出
          </el-button>
        </div>
      </el-header>

      <!-- 主内容区 -->
      <el-main class="main-content">
        <router-view v-slot="{ Component }">
          <transition name="page-fade" mode="out-in">
            <component :is="Component" />
          </transition>
        </router-view>
      </el-main>
    </el-container>

    <!-- 首次登录引导弹窗 -->
    <el-dialog
      v-model="showGuideDialog"
      title="完善个人画像"
      width="520px"
      :close-on-click-modal="false"
      class="guide-dialog"
    >
      <div class="guide-content">
        <div class="guide-icon">
          <svg width="48" height="48" viewBox="0 0 48 48" fill="none">
            <circle cx="24" cy="24" r="22" stroke="var(--color-accent)" stroke-width="2" fill="none" opacity="0.3"/>
            <circle cx="24" cy="24" r="16" stroke="var(--color-accent)" stroke-width="2" fill="none" opacity="0.6"/>
            <circle cx="24" cy="24" r="6" fill="var(--color-accent)"/>
          </svg>
        </div>
        <p class="guide-desc">欢迎使用简历助手！请先完善基本信息，以便我们为您提供更精准的服务。</p>
        <el-form :model="guideForm" label-width="90px">
          <el-form-item label="工作年限" required>
            <el-select v-model="guideForm.workYears" placeholder="请选择" style="width: 100%">
              <el-option v-for="item in workYearsOptions" :key="item" :label="item" :value="item" />
            </el-select>
          </el-form-item>
          <el-form-item label="技术方向">
            <el-select v-model="guideForm.techDirection" multiple placeholder="请选择（可多选）" style="width: 100%">
              <el-option v-for="item in techDirectionOptions" :key="item" :label="item" :value="item" />
            </el-select>
          </el-form-item>
          <el-form-item label="目标岗位" required>
            <el-input v-model="guideForm.targetPosition" placeholder="如：高级Java工程师" />
          </el-form-item>
        </el-form>
      </div>
      <template #footer>
        <div class="guide-footer">
          <el-button @click="handleGuideSkip" class="guide-skip-btn">稍后再说</el-button>
          <el-button type="primary" :loading="guideSaving" @click="handleGuideSave" class="guide-save-btn">
            保存并完善
          </el-button>
        </div>
      </template>
    </el-dialog>
  </el-container>
</template>

<style lang="scss" scoped>
.main-layout {
  height: 100vh;
  overflow: hidden;
}

// ===== SIDEBAR =====
.sidebar {
  background: linear-gradient(180deg, var(--sidebar-bg-start) 0%, var(--sidebar-bg-end) 100%);
  overflow: hidden;
  display: flex;
  flex-direction: column;
  position: relative;

  // Subtle noise texture overlay
  &::before {
    content: '';
    position: absolute;
    inset: 0;
    background-image: url("data:image/svg+xml,%3Csvg viewBox='0 0 256 256' xmlns='http://www.w3.org/2000/svg'%3E%3Cfilter id='n'%3E%3CfeTurbulence type='fractalNoise' baseFrequency='0.9' numOctaves='4' stitchTiles='stitch'/%3E%3C/filter%3E%3Crect width='100%25' height='100%25' filter='url(%23n)' opacity='0.03'/%3E%3C/svg%3E");
    pointer-events: none;
    z-index: 0;
  }

  // Gradient accent line at top
  &::after {
    content: '';
    position: absolute;
    top: 0;
    left: 0;
    right: 0;
    height: 3px;
    background: linear-gradient(90deg, var(--color-accent), var(--color-accent-light), var(--color-accent));
    z-index: 1;
  }

  .logo {
    position: relative;
    z-index: 1;
    padding: 24px 20px 20px;
    display: flex;
    align-items: center;
    gap: 12px;

    .logo-icon {
      width: 44px;
      height: 44px;
      border-radius: 12px;
      background: linear-gradient(135deg, var(--color-accent) 0%, var(--color-accent-light) 100%);
      display: flex;
      align-items: center;
      justify-content: center;
      color: #1a1f3d;
      flex-shrink: 0;
      box-shadow: 0 4px 12px rgba(232, 148, 58, 0.3);
    }

    .logo-text {
      h2 {
        margin: 0;
        font-size: 20px;
        font-weight: 700;
        color: #ffffff;
        font-family: var(--font-heading);
        letter-spacing: 0.5px;
        line-height: 1.2;
      }

      .logo-sub {
        font-size: 10px;
        color: rgba(255, 255, 255, 0.4);
        text-transform: uppercase;
        letter-spacing: 1.5px;
        font-family: var(--font-body);
        font-weight: 500;
      }
    }
  }
}

.sidebar-nav {
  flex: 1;
  position: relative;
  z-index: 1;
  padding: 8px 12px;
  overflow-y: auto;

  .sidebar-menu {
    border-right: none;
  }

  .nav-item {
    position: relative;
    height: 46px;
    line-height: 46px;
    margin-bottom: 4px;
    border-radius: 10px;
    transition: all var(--transition-smooth);
    padding-left: 16px !important;

    .nav-indicator {
      position: absolute;
      left: 0;
      top: 50%;
      transform: translateY(-50%);
      width: 3px;
      height: 0;
      background: var(--sidebar-accent);
      border-radius: 0 3px 3px 0;
      transition: height var(--transition-bounce);
    }

    &:hover {
      background: rgba(255, 255, 255, 0.06);

      .nav-indicator {
        height: 20px;
      }
    }

    &.is-active {
      background: rgba(255, 255, 255, 0.1);
      color: #ffffff !important;

      .nav-indicator {
        height: 28px;
        background: var(--sidebar-accent);
      }
    }

    .el-icon {
      margin-right: 10px;
    }

    span {
      font-size: 14px;
      font-weight: 500;
    }
  }
}

.sidebar-footer {
  position: relative;
  z-index: 1;
  padding: 16px 20px 20px;
  border-top: 1px solid rgba(255, 255, 255, 0.06);

  .sidebar-brand {
    display: flex;
    align-items: center;
    gap: 8px;
    font-size: 11px;
    color: rgba(255, 255, 255, 0.3);
    text-transform: uppercase;
    letter-spacing: 1px;
    font-weight: 500;

    .brand-dot {
      width: 6px;
      height: 6px;
      border-radius: 50%;
      background: var(--color-success);
      animation: pulseGlow 2s infinite;
      box-shadow: 0 0 0 0 rgba(91, 167, 112, 0.4);
    }
  }
}

// ===== RIGHT CONTAINER =====
.right-container {
  display: flex;
  flex-direction: column;
  overflow: hidden;
}

// ===== HEADER =====
.header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  background: var(--color-surface);
  padding: 0 28px;
  position: relative;
  flex-shrink: 0;

  // Gradient bottom border
  &::after {
    content: '';
    position: absolute;
    bottom: 0;
    left: 28px;
    right: 28px;
    height: 1px;
    background: linear-gradient(90deg, var(--color-border) 0%, var(--color-border-light) 50%, transparent 100%);
  }

  .header-left {
    .page-title {
      font-size: 26px;
      font-weight: 600;
      color: var(--color-text-primary);
      font-family: var(--font-heading);
      margin: 0;
      letter-spacing: -0.3px;
    }
  }

  .header-right {
    display: flex;
    align-items: center;
    gap: 16px;

    .user-pill {
      display: flex;
      align-items: center;
      gap: 10px;
      padding: 6px 14px 6px 6px;
      background: var(--color-bg);
      border-radius: var(--radius-full);
      border: 1px solid var(--color-border-light);
      transition: all var(--transition-smooth);

      &:hover {
        border-color: var(--color-border);
        box-shadow: var(--shadow-xs);
      }

      .user-avatar {
        width: 30px;
        height: 30px;
        border-radius: 50%;
        background: linear-gradient(135deg, var(--color-primary) 0%, var(--color-primary-light) 100%);
        color: #fff;
        display: flex;
        align-items: center;
        justify-content: center;
        font-size: 13px;
        font-weight: 600;
        font-family: var(--font-heading);
      }

      .user-name {
        font-size: 13px;
        font-weight: 500;
        color: var(--color-text-regular);
      }
    }

    .logout-btn {
      font-size: 13px;
      color: var(--color-text-secondary);
      padding: 6px 12px;

      &:hover {
        color: var(--color-danger);
      }
    }
  }
}

// ===== MAIN CONTENT =====
.main-content {
  background: var(--color-bg);
  overflow-y: auto;
  padding: 24px 28px;
}

// ===== PAGE TRANSITION =====
.page-fade-enter-active {
  animation: fadeInUp 0.35s ease both;
}

.page-fade-leave-active {
  animation: fadeIn 0.15s ease reverse both;
}

// ===== GUIDE DIALOG =====
.guide-dialog {
  :deep(.el-dialog__header) {
    padding: 24px 28px 0;
  }

  :deep(.el-dialog__body) {
    padding: 20px 28px;
  }

  :deep(.el-dialog__footer) {
    padding: 0 28px 24px;
  }
}

.guide-content {
  .guide-icon {
    display: flex;
    justify-content: center;
    margin-bottom: 16px;
  }

  .guide-desc {
    color: var(--color-text-secondary);
    margin-bottom: 24px;
    line-height: 1.7;
    text-align: center;
    font-size: 14px;
  }
}

.guide-footer {
  display: flex;
  justify-content: flex-end;
  gap: 12px;

  .guide-skip-btn {
    border-radius: var(--radius-base);
  }

  .guide-save-btn {
    background: linear-gradient(135deg, var(--color-accent) 0%, var(--color-accent-dark) 100%);
    border: none;
    border-radius: var(--radius-base);
    font-weight: 600;

    &:hover {
      background: linear-gradient(135deg, var(--color-accent-light) 0%, var(--color-accent) 100%);
      box-shadow: var(--shadow-glow-accent);
    }
  }
}

// ===== RESPONSIVE =====
@media (max-width: 768px) {
  .sidebar {
    width: 64px !important;

    .logo {
      padding: 16px 10px;
      justify-content: center;

      .logo-text {
        display: none;
      }
    }

    .nav-item span {
      display: none;
    }
  }

  .header {
    padding: 0 16px;

    .header-left .page-title {
      font-size: 20px;
    }
  }

  .main-content {
    padding: 16px;
  }
}
</style>
