import { EventEmitter } from 'events'

export class AutoWebsocket extends EventEmitter {
  private ws: WebSocket | null = null
  private stopped = false

  constructor(private readonly url: string) {
    super()
  }

  private cleanup(): void {
    if (!this.ws) return
    this.ws.close()
    this.ws = null
  }

  private connect(): void {
    if (this.stopped) return
    this.cleanup()

    this.ws = new WebSocket(this.url)

    this.ws.onopen = () => {
      this.emit('open')
    }

    this.ws.onmessage = (event) => {
      this.emit('message', event.data)
    }

    this.ws.onclose = () => {
      this.emit('close')
      if (!this.stopped) {
        this.connect()
      }
    }

    this.ws.onerror = (event) => {
      this.emit('error', event)
    }
  }

  public send(data: string | ArrayBufferLike | Blob | ArrayBufferView): void {
    if (!this.ws) return
    this.ws.send(data)
  }

  public start(): void {
    this.stopped = false
    this.connect()
  }

  public stop(): void {
    this.stopped = true
    this.cleanup()
  }

  public get isConnected(): boolean {
    return this.ws?.readyState === WebSocket.OPEN
  }
}
