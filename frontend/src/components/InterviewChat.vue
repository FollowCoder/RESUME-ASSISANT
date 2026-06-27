<script setup lang="ts">
import { ref, nextTick, watch } from 'vue'
import type { InterviewMessage } from '@/api/interview'

const props = defineProps<{
  messages: InterviewMessage[]
}>()

const emit = defineEmits<{
  send: [content: string]
}>()

const inputContent = ref('')
const chatContainer = ref<HTMLElement>()

// 监听消息变化自动滚动
watch(
  () => props.messages.length,
  () => {
    nextTick(() => {
      scrollToBottom()
    })
  }
)

function scrollToBottom() {
  if (chatContainer.value) {
    chatContainer.value.scrollTop = chatContainer.value.scrollHeight
  }
}

function handleSend() {
  const content = inputContent.value.trim()
  if (!content) return
  emit('send', content)
  inputContent.value = ''
}

function handleKeydown(e: KeyboardEvent | Event) {
  const ke = e as KeyboardEvent
  if (ke.key === 'Enter' && !ke.shiftKey) {
    ke.preventDefault()
    handleSend()
  }
}

function formatTime(timestamp: string): string {
  if (!timestamp) return ''
  const date = new Date(timestamp)
  return date.toLocaleTimeString('zh-CN', {
    hour: '2-digit',
    minute: '2-digit'
  })
}
</script>

<template>
  <div class="interview-chat">
    <!-- 消息列表 -->
    <div ref="chatContainer" class="chat-messages">
      <div
        v-for="(msg, index) in messages"
        :key="index"
        class="message-item"
        :class="`message-${msg.type}`"
      >
        <!-- 系统消息 -->
        <div v-if="msg.type === 'system'" class="system-message">
          <el-tag type="info" size="small" effect="plain">
            {{ msg.content }}
          </el-tag>
        </div>

        <!-- 面试官消息 -->
        <div v-else-if="msg.type === 'interviewer'" class="bubble-wrapper bubble-left">
          <div class="avatar">
            <el-icon :size="20"><Monitor /></el-icon>
          </div>
          <div class="bubble-content">
            <div class="bubble interviewer-bubble">
              {{ msg.content }}
            </div>
            <span class="msg-time">{{ formatTime(msg.timestamp) }}</span>
          </div>
        </div>

        <!-- 候选人消息 -->
        <div v-else class="bubble-wrapper bubble-right">
          <div class="bubble-content">
            <div class="bubble candidate-bubble">
              {{ msg.content }}
            </div>
            <span class="msg-time">{{ formatTime(msg.timestamp) }}</span>
          </div>
          <div class="avatar avatar-candidate">
            <el-icon :size="20"><User /></el-icon>
          </div>
        </div>
      </div>

      <div v-if="messages.length === 0" class="empty-chat">
        <el-empty description="等待面试官提问..." :image-size="80" />
      </div>
    </div>

    <!-- 输入区 -->
    <div class="chat-input">
      <el-input
        v-model="inputContent"
        type="textarea"
        :rows="2"
        placeholder="输入你的回答... (Enter 发送, Shift+Enter 换行)"
        resize="none"
        @keydown="handleKeydown"
      />
      <el-button
        type="primary"
        :disabled="!inputContent.trim()"
        @click="handleSend"
      >
        发送
      </el-button>
    </div>
  </div>
</template>

<style lang="scss" scoped>
.interview-chat {
  display: flex;
  flex-direction: column;
  height: 100%;
  min-height: 0;
  overflow: hidden;

  .chat-messages {
    flex: 1;
    min-height: 0;
    overflow-y: auto;
    padding: 20px;
    display: flex;
    flex-direction: column;
    gap: 16px;
    background: var(--color-bg-warm);

    .message-item {
      &.message-system {
        display: flex;
        justify-content: center;
      }
      animation: fadeInUp 0.3s ease both;
    }

    .system-message {
      text-align: center;
    }

    .bubble-wrapper {
      display: flex;
      gap: 10px;
      max-width: 80%;

      &.bubble-left {
        align-self: flex-start;
      }

      &.bubble-right {
        align-self: flex-end;
      }

      .avatar {
        width: 40px;
        height: 40px;
        border-radius: 50%;
        display: flex;
        align-items: center;
        justify-content: center;
        color: #fff;
        flex-shrink: 0;
        background: linear-gradient(135deg, var(--color-primary) 0%, var(--color-primary-light) 100%);
        box-shadow: 0 4px 12px rgba(45, 53, 97, 0.3);

        &.avatar-candidate {
          background: linear-gradient(135deg, var(--color-accent) 0%, var(--color-accent-light) 100%);
          box-shadow: 0 4px 12px rgba(232, 148, 58, 0.3);
        }
      }

      .bubble-content {
        display: flex;
        flex-direction: column;
        gap: 4px;

        .bubble {
          padding: 12px 16px;
          border-radius: 14px;
          font-size: 14px;
          line-height: 1.7;
          word-break: break-word;
          white-space: pre-wrap;
        }

        .interviewer-bubble {
          background: linear-gradient(135deg, var(--color-primary) 0%, var(--color-primary-light) 100%);
          color: #ffffff;
          border-top-left-radius: 4px;
          box-shadow: var(--shadow-sm);
        }

        .candidate-bubble {
          background: linear-gradient(135deg, var(--color-accent) 0%, var(--color-accent-dark) 100%);
          color: #ffffff;
          border-top-right-radius: 4px;
          box-shadow: 0 4px 12px rgba(232, 148, 58, 0.25);
        }

        .msg-time {
          font-size: 11px;
          color: var(--color-text-tertiary);
        }
      }
    }

    .empty-chat {
      flex: 1;
      display: flex;
      align-items: center;
      justify-content: center;

      :deep(.el-empty__image) {
        opacity: 0.6;
      }

      :deep(.el-empty__description p) {
        color: var(--color-text-secondary);
        font-family: var(--font-heading);
        font-size: 16px;
      }
    }
  }

  .chat-input {
    padding: 14px 20px;
    border-top: 1px solid var(--color-border-light);
    display: flex;
    gap: 12px;
    align-items: flex-end;
    background: var(--color-surface);

    .el-textarea {
      flex: 1;
    }

    .el-button {
      height: 42px;
      min-width: 72px;
      font-weight: 600;
    }
  }
}
</style>
