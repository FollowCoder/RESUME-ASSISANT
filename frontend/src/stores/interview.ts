import { defineStore } from 'pinia'
import { ref } from 'vue'
import { interviewApi } from '@/api/interview'
import type { InterviewMessage, InterviewReportData } from '@/api/interview'
import { WebSocketClient } from '@/utils/websocket'
import { AudioRecorder, AudioPlayer } from '@/utils/audio'
import { ElMessage } from 'element-plus'

export const useInterviewStore = defineStore('interview', () => {
  const currentInterviewId = ref<number | null>(null)
  const messages = ref<InterviewMessage[]>([])
  const isConnected = ref(false)
  const isInterviewing = ref(false)
  const report = ref<InterviewReportData | null>(null)
  const loading = ref(false)

  // 语音模式相关状态
  const isVoiceMode = ref(false)
  const isRecording = ref(false)
  const isTtsPlaying = ref(false)
  const currentTranscript = ref('')

  let wsClient: WebSocketClient | null = null
  let voiceWs: WebSocket | null = null
  let audioRecorder: AudioRecorder | null = null
  let audioPlayer: AudioPlayer | null = null

  async function startInterview(resumeId: number, jdContent: string, mode: string = 'text') {
    loading.value = true
    try {
      const res = await interviewApi.start({
        resumeId,
        jdContent,
        mode
      })
      currentInterviewId.value = res.interviewId
      messages.value = []
      report.value = null
      isInterviewing.value = true
      isVoiceMode.value = mode === 'voice'

      const wsProtocol = window.location.protocol === 'https:' ? 'wss:' : 'ws:'
      const wsHost = window.location.host
      const token = localStorage.getItem('token')

      if (mode === 'voice') {
        // 语音模式：使用原生 WebSocket 处理 binary frame
        const voiceWsUrl = `${wsProtocol}//${wsHost}/ws/interview/${res.interviewId}/voice?token=${token}`
        connectVoiceWebSocket(voiceWsUrl)
      } else {
        // 文字模式：使用封装的 WebSocketClient
        const wsUrl = `${wsProtocol}//${wsHost}/ws/interview/${res.interviewId}`
        wsClient = new WebSocketClient({
          url: wsUrl,
          maxReconnectAttempts: 5,
          onOpen: () => {
            isConnected.value = true
          },
          onClose: () => {
            isConnected.value = false
          },
          onError: () => {
            ElMessage.error('WebSocket 连接异常')
          }
        })

        wsClient.on('*', (data: InterviewMessage) => {
          messages.value.push({
            type: data.type,
            content: data.content,
            timestamp: data.timestamp || new Date().toISOString(),
            metadata: data.metadata
          })
        })

        wsClient.connect()
      }

      return true
    } catch {
      isInterviewing.value = false
      return false
    } finally {
      loading.value = false
    }
  }

  function connectVoiceWebSocket(url: string) {
    voiceWs = new WebSocket(url)
    voiceWs.binaryType = 'arraybuffer'

    // 初始化音频播放器
    audioPlayer = new AudioPlayer()

    voiceWs.onopen = () => {
      isConnected.value = true
    }

    voiceWs.onmessage = (event: MessageEvent) => {
      if (event.data instanceof ArrayBuffer) {
        // Binary frame: TTS 音频数据 - 累积到缓冲区
        audioPlayer?.appendAudio(event.data)
      } else {
        // Text frame: JSON 消息
        try {
          const msg = JSON.parse(event.data)
          handleVoiceMessage(msg)
        } catch (e) {
          console.warn('[VoiceWS] 解析消息失败', e)
        }
      }
    }

    voiceWs.onclose = () => {
      isConnected.value = false
    }

    voiceWs.onerror = () => {
      ElMessage.error('语音 WebSocket 连接异常')
    }
  }

  function handleVoiceMessage(msg: { type: string; content: string }) {
    switch (msg.type) {
      case 'candidate_text':
        // 语音转写结果 — 添加为候选人消息
        currentTranscript.value = ''
        messages.value.push({
          type: 'candidate',
          content: msg.content,
          timestamp: new Date().toISOString()
        })
        break
      case 'interviewer_text':
        // 面试官文本回复
        messages.value.push({
          type: 'interviewer',
          content: msg.content,
          timestamp: new Date().toISOString()
        })
        break
      case 'system':
        messages.value.push({
          type: 'system',
          content: msg.content,
          timestamp: new Date().toISOString()
        })
        break
      case 'tts_complete':
        // TTS 数据接收完成，开始解码播放累积的音频
        isTtsPlaying.value = true
        audioPlayer?.finishStream().then(() => {
          isTtsPlaying.value = false
        })
        break
    }
  }

  // 语音模式：开始录音
  async function startRecording() {
    if (!voiceWs || voiceWs.readyState !== WebSocket.OPEN) {
      ElMessage.warning('连接未就绪，请稍后再试')
      return
    }

    // 停止当前 TTS 播放
    audioPlayer?.stop()
    isTtsPlaying.value = false

    // 初始化录音器
    audioRecorder = new AudioRecorder()
    audioRecorder.onData((data: ArrayBuffer) => {
      // 将 PCM 数据通过 WebSocket 发送
      if (voiceWs && voiceWs.readyState === WebSocket.OPEN) {
        voiceWs.send(data)
      }
    })

    try {
      await audioRecorder.start()
      isRecording.value = true
      // 通知后端开始录音
      voiceWs.send(JSON.stringify({ type: 'start_recording' }))
    } catch (e) {
      ElMessage.error('无法访问麦克风，请检查权限设置')
      console.error('[录音] 启动失败', e)
    }
  }

  // 语音模式：停止录音
  async function stopRecording() {
    if (!audioRecorder) return

    await audioRecorder.stop()
    audioRecorder.release()
    audioRecorder = null
    isRecording.value = false

    // 通知后端停止录音
    if (voiceWs && voiceWs.readyState === WebSocket.OPEN) {
      voiceWs.send(JSON.stringify({ type: 'stop_recording' }))
    }
  }

  function sendMessage(content: string) {
    if (isVoiceMode.value) {
      // 语音模式下也支持文本发送（fallback）
      if (!voiceWs || voiceWs.readyState !== WebSocket.OPEN) {
        ElMessage.warning('连接未就绪，请稍后再试')
        return
      }
      voiceWs.send(JSON.stringify({ type: 'text', content }))
      messages.value.push({
        type: 'candidate',
        content,
        timestamp: new Date().toISOString()
      })
    } else {
      if (!wsClient || !isConnected.value) {
        ElMessage.warning('连接未就绪，请稍后再试')
        return
      }
      const msg: InterviewMessage = {
        type: 'candidate',
        content,
        timestamp: new Date().toISOString()
      }
      wsClient.send(msg)
      messages.value.push(msg)
    }
  }

  async function endInterview() {
    if (!currentInterviewId.value) return null
    loading.value = true
    try {
      const data = await interviewApi.end(currentInterviewId.value)
      report.value = data
      disconnect()
      isInterviewing.value = false
      return report.value
    } catch {
      return null
    } finally {
      loading.value = false
    }
  }

  async function loadReport(interviewId: number) {
    loading.value = true
    try {
      const data = await interviewApi.getReport(interviewId)
      report.value = data
      return report.value
    } catch {
      return null
    } finally {
      loading.value = false
    }
  }

  function disconnect() {
    // 关闭文字模式 WebSocket
    if (wsClient) {
      wsClient.close()
      wsClient = null
    }
    // 关闭语音模式 WebSocket
    if (voiceWs) {
      voiceWs.close()
      voiceWs = null
    }
    // 释放音频资源
    if (audioRecorder) {
      audioRecorder.release()
      audioRecorder = null
    }
    if (audioPlayer) {
      audioPlayer.release()
      audioPlayer = null
    }
    isConnected.value = false
    isRecording.value = false
    isTtsPlaying.value = false
  }

  function reset() {
    disconnect()
    currentInterviewId.value = null
    messages.value = []
    isInterviewing.value = false
    isVoiceMode.value = false
    report.value = null
    loading.value = false
    currentTranscript.value = ''
  }

  return {
    currentInterviewId,
    messages,
    isConnected,
    isInterviewing,
    isVoiceMode,
    isRecording,
    isTtsPlaying,
    currentTranscript,
    report,
    loading,
    startInterview,
    startRecording,
    stopRecording,
    sendMessage,
    endInterview,
    loadReport,
    disconnect,
    reset
  }
})
