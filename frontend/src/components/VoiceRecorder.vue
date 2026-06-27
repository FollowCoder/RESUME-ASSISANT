<script setup lang="ts">
import { ref, onBeforeUnmount } from 'vue'
import { Microphone } from '@element-plus/icons-vue'

const props = defineProps<{
  disabled?: boolean
}>()

const emit = defineEmits<{
  startRecording: []
  stopRecording: []
  audioData: [data: ArrayBuffer]
}>()

const isRecording = ref(false)
const duration = ref(0)
let durationTimer: ReturnType<typeof setInterval> | null = null

function startRecord() {
  if (props.disabled || isRecording.value) return
  isRecording.value = true
  duration.value = 0
  durationTimer = setInterval(() => {
    duration.value++
  }, 1000)
  emit('startRecording')
}

function stopRecord() {
  if (!isRecording.value) return
  isRecording.value = false
  if (durationTimer) {
    clearInterval(durationTimer)
    durationTimer = null
  }
  emit('stopRecording')
}

function formatDuration(seconds: number): string {
  const m = Math.floor(seconds / 60)
  const s = seconds % 60
  return `${m.toString().padStart(2, '0')}:${s.toString().padStart(2, '0')}`
}

onBeforeUnmount(() => {
  if (durationTimer) {
    clearInterval(durationTimer)
  }
})
</script>

<template>
  <div class="voice-recorder">
    <el-button
      :type="isRecording ? 'danger' : 'primary'"
      circle
      size="large"
      :disabled="disabled"
      @mousedown.prevent="startRecord"
      @mouseup.prevent="stopRecord"
      @mouseleave="isRecording && stopRecord()"
      @touchstart.prevent="startRecord"
      @touchend.prevent="stopRecord"
    >
      <el-icon :size="24"><Microphone /></el-icon>
    </el-button>

    <div v-if="isRecording" class="recording-indicator">
      <span class="pulse-dot"></span>
      <span class="recording-text">录音中... {{ formatDuration(duration) }}</span>
    </div>

    <div v-else class="hint-text">
      {{ disabled ? '等待面试官回复...' : '按住说话' }}
    </div>
  </div>
</template>

<style scoped>
.voice-recorder {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 16px;
  padding: 28px;
}

.voice-recorder .el-button.is-circle {
  width: 72px;
  height: 72px;
  box-shadow: 0 4px 20px rgba(45, 53, 97, 0.25);
  transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
  position: relative;
  z-index: 1;

  &::before,
  &::after {
    content: '';
    position: absolute;
    top: 50%;
    left: 50%;
    transform: translate(-50%, -50%);
    border-radius: 50%;
    background: linear-gradient(135deg, var(--color-accent) 0%, var(--color-accent-dark) 100%);
    z-index: -1;
    opacity: 0;
    transition: all 0.3s ease;
  }

  &::before {
    width: 80px;
    height: 80px;
  }

  &::after {
    width: 88px;
    height: 88px;
  }
}

.voice-recorder .el-button.is-circle:hover {
  transform: scale(1.08);
  box-shadow: 0 6px 28px rgba(45, 53, 97, 0.35);

  &::before {
    opacity: 0.2;
  }

  &::after {
    opacity: 0.1;
  }
}

.voice-recorder .el-button.is-circle:active {
  transform: scale(0.95);
}

.recording-indicator {
  display: flex;
  align-items: center;
  gap: 10px;
}

.pulse-dot {
  width: 12px;
  height: 12px;
  border-radius: 50%;
  background: linear-gradient(135deg, var(--color-danger) 0%, #e74c3c 100%);
  animation: pulse 1.2s infinite;
  box-shadow: 0 0 12px rgba(199, 84, 84, 0.5);
}

.recording-text {
  font-size: 15px;
  color: var(--color-danger);
  font-weight: 600;
  font-family: var(--font-heading);
}

.hint-text {
  font-size: 14px;
  color: var(--color-text-secondary);
  font-weight: 500;
}

@keyframes pulse {
  0% {
    transform: scale(1);
    opacity: 1;
    box-shadow: 0 0 0 0 rgba(199, 84, 84, 0.5);
  }
  50% {
    transform: scale(1.6);
    opacity: 0.6;
    box-shadow: 0 0 0 12px rgba(199, 84, 84, 0);
  }
  100% {
    transform: scale(1);
    opacity: 1;
    box-shadow: 0 0 0 0 rgba(199, 84, 84, 0.5);
  }
}
</style>
