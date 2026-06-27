<script setup lang="ts">
import { ref, computed, onMounted, onUnmounted } from 'vue'
import { creditsApi } from '@/api/credits'
import type { TransactionItem } from '@/api/credits'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Coin } from '@element-plus/icons-vue'

const balance = ref(0)
const recentTransactions = ref<TransactionItem[]>([])
const loading = ref(false)

// 充值对话框
const purchaseDialogVisible = ref(false)
const selectedAmount = ref(10)
const purchasing = ref(false)

const purchaseOptions = [
  { value: 10, label: '10 次' },
  { value: 30, label: '30 次' },
  { value: 50, label: '50 次' },
  { value: 100, label: '100 次' }
]

const badgeType = computed(() => {
  if (balance.value === 0) return 'danger'
  if (balance.value <= 3) return 'warning'
  return 'primary'
})

const buttonType = computed(() => {
  if (balance.value === 0) return 'danger'
  if (balance.value <= 3) return 'warning'
  return 'primary'
})

const fetchBalance = async () => {
  try {
    const res = await creditsApi.getBalance()
    if (res) {
      balance.value = res.balance
    }
  } catch {
    // 静默失败
  }
}

const fetchTransactions = async () => {
  try {
    loading.value = true
    const res = await creditsApi.getTransactions()
    if (res) {
      recentTransactions.value = res.slice(0, 5)
    }
  } catch {
    // 静默失败
  } finally {
    loading.value = false
  }
}

const handlePurchase = () => {
  purchaseDialogVisible.value = true
}

const confirmPurchase = async () => {
  purchasing.value = true
  try {
    const res = await creditsApi.purchase(selectedAmount.value)
    if (res) {
      balance.value = res.balance
      ElMessage.success(`充值成功！当前余额: ${res.balance} 次`)
      purchaseDialogVisible.value = false
      fetchTransactions()
    }
  } catch {
    // 错误已在拦截器处理
  } finally {
    purchasing.value = false
  }
}

const formatTime = (dateStr: string) => {
  if (!dateStr) return ''
  const date = new Date(dateStr)
  const month = (date.getMonth() + 1).toString().padStart(2, '0')
  const day = date.getDate().toString().padStart(2, '0')
  const hours = date.getHours().toString().padStart(2, '0')
  const minutes = date.getMinutes().toString().padStart(2, '0')
  return `${month}-${day} ${hours}:${minutes}`
}

const handlePopoverShow = () => {
  fetchTransactions()
}

const openPurchaseFromEvent = () => {
  purchaseDialogVisible.value = true
}

onMounted(() => {
  fetchBalance()
  window.addEventListener('open-purchase-dialog', openPurchaseFromEvent)
})

onUnmounted(() => {
  window.removeEventListener('open-purchase-dialog', openPurchaseFromEvent)
})

// 暴露刷新方法供外部调用
defineExpose({ fetchBalance })
</script>

<template>
  <el-popover placement="bottom" :width="340" trigger="click" @show="handlePopoverShow">
    <template #reference>
      <el-button :type="buttonType" size="small" plain class="credit-btn">
        <el-icon><Coin /></el-icon>
        <span>剩余 {{ balance }} 次</span>
      </el-button>
    </template>

    <!-- 弹出内容 -->
    <div class="credit-popover">
      <div class="credit-header">
        <span class="credit-title">使用额度</span>
        <el-button type="primary" size="small" @click="handlePurchase">充值</el-button>
      </div>
      <el-divider style="margin: 12px 0" />
      <div class="price-list">
        <div class="price-item"><span>简历编写</span><span class="price-cost">2次/份</span></div>
        <div class="price-item"><span>简历优化</span><span class="price-cost">1次/次</span></div>
        <div class="price-item"><span>JD匹配</span><span class="price-cost">1次/次</span></div>
        <div class="price-item"><span>模拟面试</span><span class="price-cost">3次/场</span></div>
      </div>
      <el-divider style="margin: 12px 0" />
      <div class="recent-title">最近使用记录</div>
      <div v-if="recentTransactions.length === 0" class="empty-text">暂无使用记录</div>
      <div v-else class="recent-transactions">
        <div v-for="t in recentTransactions" :key="t.id" class="transaction-item">
          <span class="t-module">{{ t.moduleName }}</span>
          <span class="t-cost">-{{ t.creditsUsed }}</span>
          <span class="t-time">{{ formatTime(t.createdAt) }}</span>
        </div>
      </div>
    </div>
  </el-popover>

  <!-- 充值对话框 -->
  <el-dialog v-model="purchaseDialogVisible" title="充值额度" width="400px">
    <div class="purchase-content">
      <p class="purchase-desc">选择充值数量：</p>
      <el-radio-group v-model="selectedAmount" class="purchase-options">
        <el-radio-button v-for="opt in purchaseOptions" :key="opt.value" :value="opt.value">
          {{ opt.label }}
        </el-radio-button>
      </el-radio-group>
      <p class="purchase-info">当前余额: {{ balance }} 次，充值后: {{ balance + selectedAmount }} 次</p>
    </div>
    <template #footer>
      <el-button @click="purchaseDialogVisible = false">取消</el-button>
      <el-button type="primary" :loading="purchasing" @click="confirmPurchase">确认充值</el-button>
    </template>
  </el-dialog>
</template>

<style scoped>
.credit-btn {
  display: flex;
  align-items: center;
  gap: 6px;
  border-radius: var(--radius-full);
  font-weight: 600;
  transition: all var(--transition-smooth);

  &:hover {
    transform: translateY(-1px);
    box-shadow: var(--shadow-md);
  }
}

.credit-popover {
  .credit-header {
    display: flex;
    justify-content: space-between;
    align-items: center;
  }

  .credit-title {
    font-weight: 600;
    font-size: 16px;
    font-family: var(--font-heading);
    color: var(--color-text-primary);
  }

  .price-list {
    .price-item {
      display: flex;
      justify-content: space-between;
      padding: 8px 0;
      font-size: 14px;
      color: var(--color-text-regular);
      border-bottom: 1px dashed var(--color-border-light);

      &:last-child {
        border-bottom: none;
      }

      .price-cost {
        color: var(--color-accent);
        font-weight: 600;
      }
    }
  }

  .recent-title {
    font-size: 14px;
    font-weight: 600;
    margin-bottom: 10px;
    color: var(--color-text-primary);
    font-family: var(--font-heading);
  }

  .empty-text {
    font-size: 13px;
    color: var(--color-text-tertiary);
    text-align: center;
    padding: 12px 0;
  }

  .recent-transactions {
    .transaction-item {
      display: flex;
      align-items: center;
      padding: 8px 0;
      font-size: 13px;
      border-bottom: 1px dashed var(--color-border-light);
      transition: all var(--transition-smooth);

      &:last-child {
        border-bottom: none;
      }

      &:hover {
        background: var(--color-bg-warm);
        border-radius: var(--radius-base);
        padding-left: 8px;
        padding-right: 8px;
      }

      .t-module {
        flex: 1;
        color: var(--color-text-primary);
        font-weight: 500;
      }

      .t-cost {
        color: var(--color-danger);
        margin-right: 12px;
        font-weight: 600;
      }

      .t-time {
        color: var(--color-text-tertiary);
        font-size: 12px;
      }
    }
  }
}

.purchase-content {
  .purchase-desc {
    margin-bottom: 20px;
    color: var(--color-text-regular);
    font-size: 15px;
  }

  .purchase-options {
    display: flex;
    gap: 10px;
    margin-bottom: 20px;

    :deep(.el-radio-button__inner) {
      border-radius: var(--radius-base);
      border: 1px solid var(--color-border);
      transition: all var(--transition-smooth);

      &:hover {
        border-color: var(--color-accent);
        color: var(--color-accent);
      }
    }

    :deep(.el-radio-button__original-radio:checked + .el-radio-button__inner) {
      background: linear-gradient(135deg, var(--color-accent) 0%, var(--color-accent-dark) 100%);
      border-color: var(--color-accent);
      box-shadow: -1px 0 0 0 var(--color-accent);
    }
  }

  .purchase-info {
    color: var(--color-text-secondary);
    font-size: 14px;
    padding: 12px 16px;
    background: var(--color-bg-warm);
    border-radius: var(--radius-base);
    border-left: 3px solid var(--color-accent);
  }
}
</style>
