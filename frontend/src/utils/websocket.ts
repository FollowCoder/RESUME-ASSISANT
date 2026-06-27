type EventCallback = (data: any) => void

interface WebSocketOptions {
  url: string
  maxReconnectAttempts?: number
  onOpen?: () => void
  onClose?: () => void
  onError?: (error: Event) => void
}

export class WebSocketClient {
  private ws: WebSocket | null = null
  private url: string
  private maxReconnectAttempts: number
  private reconnectAttempts: number = 0
  private eventHandlers: Map<string, Set<EventCallback>> = new Map()
  private options: WebSocketOptions
  private isManualClose: boolean = false

  constructor(options: WebSocketOptions) {
    this.options = options
    this.url = options.url
    this.maxReconnectAttempts = options.maxReconnectAttempts ?? 5
  }

  connect(): void {
    const token = localStorage.getItem('token')
    const url = token ? `${this.url}?token=${token}` : this.url

    this.ws = new WebSocket(url)
    this.isManualClose = false

    this.ws.onopen = () => {
      this.reconnectAttempts = 0
      this.options.onOpen?.()
    }

    this.ws.onmessage = (event: MessageEvent) => {
      try {
        const data = JSON.parse(event.data)
        const type = data.type || 'message'
        const handlers = this.eventHandlers.get(type)
        if (handlers) {
          handlers.forEach(handler => handler(data))
        }
        // 同时触发通用 message 事件
        const allHandlers = this.eventHandlers.get('*')
        if (allHandlers) {
          allHandlers.forEach(handler => handler(data))
        }
      } catch {
        // 非 JSON 格式消息
        const handlers = this.eventHandlers.get('raw')
        if (handlers) {
          handlers.forEach(handler => handler(event.data))
        }
      }
    }

    this.ws.onclose = () => {
      this.options.onClose?.()
      if (!this.isManualClose) {
        this.tryReconnect()
      }
    }

    this.ws.onerror = (error: Event) => {
      this.options.onError?.(error)
    }
  }

  private tryReconnect(): void {
    if (this.reconnectAttempts >= this.maxReconnectAttempts) {
      console.warn('[WebSocket] 达到最大重连次数，停止重连')
      return
    }

    this.reconnectAttempts++
    // 指数退避：1s, 2s, 4s, 8s, 16s
    const delay = Math.pow(2, this.reconnectAttempts - 1) * 1000

    console.log(`[WebSocket] 第 ${this.reconnectAttempts} 次重连，${delay}ms 后执行`)
    setTimeout(() => {
      this.connect()
    }, delay)
  }

  on(event: string, callback: EventCallback): void {
    if (!this.eventHandlers.has(event)) {
      this.eventHandlers.set(event, new Set())
    }
    this.eventHandlers.get(event)!.add(callback)
  }

  off(event: string, callback: EventCallback): void {
    const handlers = this.eventHandlers.get(event)
    if (handlers) {
      handlers.delete(callback)
    }
  }

  send(data: object | string): void {
    if (this.ws && this.ws.readyState === WebSocket.OPEN) {
      const message = typeof data === 'string' ? data : JSON.stringify(data)
      this.ws.send(message)
    } else {
      console.warn('[WebSocket] 连接未就绪，无法发送消息')
    }
  }

  close(): void {
    this.isManualClose = true
    if (this.ws) {
      this.ws.close()
      this.ws = null
    }
    this.eventHandlers.clear()
  }

  get isConnected(): boolean {
    return this.ws?.readyState === WebSocket.OPEN
  }
}
