<script setup lang="ts">
import { ref, nextTick, watch } from 'vue'
import { marked } from 'marked'

interface ChatMessage {
  role: 'user' | 'assistant'
  content: string
}

const props = defineProps<{
  messages: ChatMessage[]
  loading: boolean
  stage: string
}>()

const emit = defineEmits<{
  send: [message: string]
}>()

const inputText = ref('')
const messagesContainer = ref<HTMLElement>()

const stageMap: Record<string, string> = {
  GREETING: '开始',
  BASIC_INFO: '基本信息',
  EDUCATION: '教育背景',
  WORK: '工作经历',
  PROJECT: '项目经验',
  SKILLS: '技能特长',
  SUMMARY: '个人总结',
  COMPLETE: '完成'
}

const stageList = ['GREETING', 'BASIC_INFO', 'EDUCATION', 'WORK', 'PROJECT', 'SKILLS', 'SUMMARY', 'COMPLETE']

function getStageIndex(stage: string): number {
  return stageList.indexOf(stage)
}

function renderMarkdown(content: string): string {
  return marked.parse(content, { async: false }) as string
}

function handleSend() {
  const message = inputText.value.trim()
  if (!message || props.loading) return
  emit('send', message)
  inputText.value = ''
}

function handleKeydown(e: KeyboardEvent | Event) {
  const ke = e as KeyboardEvent
  if (ke.key === 'Enter' && !ke.shiftKey) {
    ke.preventDefault()
    handleSend()
  }
}

function scrollToBottom() {
  nextTick(() => {
    if (messagesContainer.value) {
      messagesContainer.value.scrollTop = messagesContainer.value.scrollHeight
    }
  })
}

watch(
  () => props.messages.length,
  () => scrollToBottom()
)

watch(
  () => props.loading,
  () => scrollToBottom()
)
</script>

<template>
  <div class="chat-panel">
    <!-- 阶段进度指示器 -->
    <div class="stage-progress">
      <div
        v-for="(s, index) in stageList"
        :key="s"
        class="stage-item"
        :class="{
          active: s === stage,
          completed: getStageIndex(stage) > index
        }"
      >
        <div class="stage-dot">
          <el-icon v-if="getStageIndex(stage) > index"><Check /></el-icon>
          <span v-else>{{ index + 1 }}</span>
        </div>
        <span class="stage-label">{{ stageMap[s] }}</span>
      </div>
    </div>

    <!-- 消息列表 -->
    <div ref="messagesContainer" class="messages-container">
      <div
        v-for="(msg, index) in messages"
        :key="index"
        class="message-item"
        :class="msg.role"
      >
        <div class="message-avatar">
          <el-icon v-if="msg.role === 'assistant'" :size="20"><Service /></el-icon>
          <el-icon v-else :size="20"><User /></el-icon>
        </div>
        <div class="message-bubble">
          <div
            v-if="msg.role === 'assistant'"
            class="message-content markdown-body"
            v-html="renderMarkdown(msg.content)"
          />
          <div v-else class="message-content">{{ msg.content }}</div>
        </div>
      </div>

      <!-- 打字指示器 -->
      <div v-if="loading" class="message-item assistant">
        <div class="message-avatar">
          <el-icon :size="20"><Service /></el-icon>
        </div>
        <div class="message-bubble">
          <div class="typing-indicator">
            <span></span>
            <span></span>
            <span></span>
            <span></span>
          </div>
        </div>
      </div>
    </div>

    <!-- 输入区域 -->
    <div class="input-area">
      <el-input
        v-model="inputText"
        type="textarea"
        :rows="2"
        placeholder="输入消息，Enter 发送，Shift+Enter 换行"
        :disabled="loading"
        @keydown="handleKeydown"
        resize="none"
      />
      <el-button
        type="primary"
        :disabled="!inputText.trim() || loading"
        @click="handleSend"
        class="send-btn"
      >
        <el-icon><Promotion /></el-icon>
      </el-button>
    </div>
  </div>
</template>

<style lang="scss" scoped>
.chat-panel {
  display: flex;
  flex-direction: column;
  height: 100%;
  background: var(--color-surface);
  border-radius: var(--radius-lg);
  overflow: hidden;
}

.stage-progress {
  display: flex;
  align-items: center;
  padding: 14px 20px;
  border-bottom: 1px solid var(--color-border-light);
  overflow-x: auto;
  gap: 0;
  position: relative;

  // Connecting line
  &::before {
    content: '';
    position: absolute;
    top: 50%;
    left: 40px;
    right: 40px;
    height: 2px;
    background: var(--color-border);
    transform: translateY(-50%);
    z-index: 0;
    margin-top: -6px;
  }

  .stage-item {
    display: flex;
    flex-direction: column;
    align-items: center;
    min-width: 56px;
    gap: 6px;
    position: relative;
    z-index: 1;

    .stage-dot {
      width: 26px;
      height: 26px;
      border-radius: 50%;
      background: var(--color-surface);
      border: 2px solid var(--color-border);
      color: var(--color-text-tertiary);
      display: flex;
      align-items: center;
      justify-content: center;
      font-size: 11px;
      font-weight: 600;
      transition: all var(--transition-bounce);
    }

    .stage-label {
      font-size: 10px;
      color: var(--color-text-tertiary);
      white-space: nowrap;
      font-weight: 500;
      transition: color var(--transition-smooth);
    }

    &.active {
      .stage-dot {
        background: var(--color-accent);
        border-color: var(--color-accent);
        color: #fff;
        box-shadow: 0 0 0 4px rgba(232, 148, 58, 0.2);
      }
      .stage-label {
        color: var(--color-accent);
        font-weight: 600;
      }
    }

    &.completed {
      .stage-dot {
        background: var(--color-success);
        border-color: var(--color-success);
        color: #fff;
      }
      .stage-label {
        color: var(--color-success);
      }
    }
  }
}

.messages-container {
  flex: 1;
  overflow-y: auto;
  padding: 20px;
  display: flex;
  flex-direction: column;
  gap: 16px;
  background: var(--color-bg-warm);
}

.message-item {
  display: flex;
  gap: 10px;
  max-width: 85%;
  animation: fadeInUp 0.3s ease both;

  &.user {
    align-self: flex-end;
    flex-direction: row-reverse;

    .message-bubble {
      background: linear-gradient(135deg, var(--color-primary) 0%, var(--color-primary-light) 100%);
      color: #fff;
      border-radius: 14px 4px 14px 14px;
    }
  }

  &.assistant {
    align-self: flex-start;

    .message-bubble {
      background: var(--color-surface);
      color: var(--color-text-primary);
      border-radius: 4px 14px 14px 14px;
      border: 1px solid var(--color-border-light);
      box-shadow: var(--shadow-xs);
    }
  }

  .message-avatar {
    width: 34px;
    height: 34px;
    border-radius: 50%;
    display: flex;
    align-items: center;
    justify-content: center;
    flex-shrink: 0;
  }

  &.user .message-avatar {
    background: linear-gradient(135deg, var(--color-primary) 0%, var(--color-primary-light) 100%);
    color: #fff;
  }

  &.assistant .message-avatar {
    background: linear-gradient(135deg, var(--color-accent) 0%, var(--color-accent-light) 100%);
    color: #fff;
  }

  .message-bubble {
    padding: 12px 16px;
    max-width: 100%;
    word-break: break-word;

    .message-content {
      line-height: 1.7;
      font-size: 14px;

      :deep(p) {
        margin: 0 0 8px 0;
        &:last-child {
          margin-bottom: 0;
        }
      }

      :deep(ul), :deep(ol) {
        padding-left: 20px;
        margin: 4px 0;
      }

      :deep(code) {
        background: rgba(45, 53, 97, 0.08);
        padding: 2px 6px;
        border-radius: 4px;
        font-size: 13px;
        font-family: 'JetBrains Mono', monospace;
      }

      :deep(pre) {
        background: var(--color-bg);
        padding: 14px;
        border-radius: var(--radius-base);
        overflow-x: auto;
        code {
          background: none;
          padding: 0;
        }
      }
    }
  }
}

.typing-indicator {
  display: flex;
  gap: 4px;
  padding: 8px 0;
  align-items: center;

  span {
    display: inline-block;
    width: 8px;
    height: 8px;
    background: linear-gradient(135deg, var(--color-accent) 0%, var(--color-accent-light) 100%);
    border-radius: 50%;
    animation: wave 1.4s infinite ease-in-out;

    &:nth-child(2) { animation-delay: 0.16s; }
    &:nth-child(3) { animation-delay: 0.32s; }
    &:nth-child(4) { animation-delay: 0.48s; }
  }
}

@keyframes wave {
  0%, 60%, 100% {
    transform: translateY(0);
    opacity: 0.5;
  }
  30% {
    transform: translateY(-10px);
    opacity: 1;
  }
}

.input-area {
  display: flex;
  gap: 10px;
  padding: 16px 20px;
  border-top: 1px solid var(--color-border-light);
  align-items: flex-end;
  background: var(--color-surface);
  border-radius: 0 0 var(--radius-lg) var(--radius-lg);
  box-shadow: 0 -4px 12px rgba(0, 0, 0, 0.03);

  .el-textarea {
    flex: 1;

    :deep(.el-textarea__inner) {
      border-radius: var(--radius-base);
      transition: all var(--transition-smooth);

      &:focus {
        box-shadow: 0 0 0 3px rgba(232, 148, 58, 0.15);
      }
    }
  }

  .send-btn {
    height: 42px;
    width: 42px;
    padding: 0;
    border-radius: var(--radius-base);
    background: linear-gradient(135deg, var(--color-accent) 0%, var(--color-accent-dark) 100%);
    border: none;
    box-shadow: var(--shadow-sm);
    transition: all var(--transition-smooth);

    &:hover {
      box-shadow: var(--shadow-glow-accent);
      transform: translateY(-1px);
    }

    &:active {
      transform: translateY(0);
    }
  }
}
</style>
