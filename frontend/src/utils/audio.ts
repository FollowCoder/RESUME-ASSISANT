/**
 * 音频录制器 - 负责获取麦克风权限并录制 PCM 音频数据
 */
export class AudioRecorder {
  private mediaStream: MediaStream | null = null
  private audioContext: AudioContext | null = null
  private scriptProcessor: ScriptProcessorNode | null = null
  private sourceNode: MediaStreamAudioSourceNode | null = null
  private dataCallback: ((data: ArrayBuffer) => void) | null = null
  private _isRecording = false

  get isRecording(): boolean {
    return this._isRecording
  }

  /**
   * 注册音频数据回调（PCM 16bit 格式）
   */
  onData(callback: (data: ArrayBuffer) => void): void {
    this.dataCallback = callback
  }

  /**
   * 开始录音 - 请求麦克风权限并开始采集 PCM 数据
   */
  async start(): Promise<void> {
    if (this._isRecording) return

    // 请求麦克风权限
    this.mediaStream = await navigator.mediaDevices.getUserMedia({
      audio: {
        sampleRate: 16000,
        channelCount: 1,
        echoCancellation: true,
        noiseSuppression: true
      }
    })

    // 创建 AudioContext 采集 PCM 数据
    this.audioContext = new AudioContext({ sampleRate: 16000 })
    this.sourceNode = this.audioContext.createMediaStreamSource(this.mediaStream)

    // 使用 ScriptProcessorNode 获取原始 PCM 数据
    // bufferSize=4096, inputChannels=1, outputChannels=1
    this.scriptProcessor = this.audioContext.createScriptProcessor(4096, 1, 1)

    this.scriptProcessor.onaudioprocess = (event: AudioProcessingEvent) => {
      if (!this._isRecording) return

      const inputData = event.inputBuffer.getChannelData(0)
      // 将 Float32 转为 Int16 PCM
      const pcmData = float32ToInt16(inputData)

      if (this.dataCallback) {
        this.dataCallback(pcmData.buffer as ArrayBuffer)
      }
    }

    this.sourceNode.connect(this.scriptProcessor)
    this.scriptProcessor.connect(this.audioContext.destination)

    this._isRecording = true
  }

  /**
   * 停止录音
   */
  async stop(): Promise<void> {
    this._isRecording = false

    if (this.scriptProcessor) {
      this.scriptProcessor.disconnect()
      this.scriptProcessor = null
    }

    if (this.sourceNode) {
      this.sourceNode.disconnect()
      this.sourceNode = null
    }

    if (this.audioContext) {
      await this.audioContext.close()
      this.audioContext = null
    }
  }

  /**
   * 释放麦克风资源
   */
  release(): void {
    this.stop()
    if (this.mediaStream) {
      this.mediaStream.getTracks().forEach(track => track.stop())
      this.mediaStream = null
    }
    this.dataCallback = null
  }
}

/**
 * 音频播放器 - 负责接收和播放流式音频数据
 * 支持流式累积模式：先收集所有音频 chunk，完成后再一次性解码播放
 */
export class AudioPlayer {
  private audioContext: AudioContext | null = null
  private audioQueue: AudioBuffer[] = []
  private isPlaying = false
  private nextStartTime = 0
  private currentSource: AudioBufferSourceNode | null = null
  // 流式累积缓冲区
  private pendingChunks: ArrayBuffer[] = []
  private pendingSize = 0

  constructor() {
    this.audioContext = new AudioContext({ sampleRate: 22050 })
  }

  /**
   * 将音频数据加入累积缓冲区（流式模式）
   * @param data WAV/PCM 格式的音频数据块
   */
  appendAudio(data: ArrayBuffer): void {
    if (!this.audioContext) return
    // 将 chunk 加入待处理队列
    this.pendingChunks.push(data)
    this.pendingSize += data.byteLength
  }

  /**
   * 完成流式接收，将累积的数据合并解码并播放
   * 应在收到 tts_complete 信号后调用
   */
  async finishStream(): Promise<void> {
    if (!this.audioContext || this.pendingChunks.length === 0) return

    // 合并所有 chunk
    const merged = new Uint8Array(this.pendingSize)
    let offset = 0
    for (const chunk of this.pendingChunks) {
      merged.set(new Uint8Array(chunk), offset)
      offset += chunk.byteLength
    }
    this.pendingChunks = []
    this.pendingSize = 0

    try {
      // 尝试解码为标准音频格式（WAV）
      const audioBuffer = await this.decodeAudio(merged.buffer)
      if (audioBuffer) {
        this.audioQueue.push(audioBuffer)
        if (!this.isPlaying) {
          this.playNext()
        }
        return
      }
    } catch (e) {
      console.warn('[AudioPlayer] WAV 解码失败，尝试 PCM 处理', e)
    }

    // 如果 WAV 解码失败，作为原始 PCM 数据处理
    const pcmBuffer = this.pcmToAudioBuffer(merged.buffer)
    if (pcmBuffer) {
      this.audioQueue.push(pcmBuffer)
      if (!this.isPlaying) {
        this.playNext()
      }
    }
  }

  /**
   * 播放队列中下一段音频
   */
  private playNext(): void {
    if (!this.audioContext || this.audioQueue.length === 0) {
      this.isPlaying = false
      return
    }

    this.isPlaying = true
    const buffer = this.audioQueue.shift()!

    const source = this.audioContext.createBufferSource()
    source.buffer = buffer
    source.connect(this.audioContext.destination)

    // 计算开始时间，确保连续播放
    const currentTime = this.audioContext.currentTime
    const startTime = Math.max(currentTime, this.nextStartTime)
    this.nextStartTime = startTime + buffer.duration

    source.start(startTime)
    this.currentSource = source

    source.onended = () => {
      this.currentSource = null
      this.playNext()
    }
  }

  /**
   * 停止播放并清空队列
   */
  stop(): void {
    this.audioQueue = []
    this.isPlaying = false
    this.nextStartTime = 0

    if (this.currentSource) {
      try {
        this.currentSource.stop()
      } catch {
        // ignore
      }
      this.currentSource = null
    }
  }

  /**
   * 当前是否正在播放
   */
  get isCurrentlyPlaying(): boolean {
    return this.isPlaying
  }

  /**
   * 释放资源
   */
  async release(): Promise<void> {
    this.stop()
    if (this.audioContext) {
      await this.audioContext.close()
      this.audioContext = null
    }
  }

  private async decodeAudio(data: ArrayBuffer): Promise<AudioBuffer | null> {
    if (!this.audioContext) return null
    try {
      return await this.audioContext.decodeAudioData(data.slice(0))
    } catch {
      return null
    }
  }

  /**
   * 将原始 PCM 16bit 数据转换为 AudioBuffer
   */
  private pcmToAudioBuffer(data: ArrayBuffer): AudioBuffer | null {
    if (!this.audioContext) return null

    const int16Array = new Int16Array(data)
    const float32Array = new Float32Array(int16Array.length)

    for (let i = 0; i < int16Array.length; i++) {
      float32Array[i] = int16Array[i] / 32768.0
    }

    const audioBuffer = this.audioContext.createBuffer(1, float32Array.length, 22050)
    audioBuffer.getChannelData(0).set(float32Array)
    return audioBuffer
  }
}

/**
 * 将 Float32Array 转换为 Int16Array (PCM 16bit)
 */
function float32ToInt16(float32Array: Float32Array): Int16Array {
  const int16Array = new Int16Array(float32Array.length)
  for (let i = 0; i < float32Array.length; i++) {
    const s = Math.max(-1, Math.min(1, float32Array[i]))
    int16Array[i] = s < 0 ? s * 0x8000 : s * 0x7FFF
  }
  return int16Array
}
