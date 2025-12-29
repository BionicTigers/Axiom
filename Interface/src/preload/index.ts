import { electronAPI } from '@electron-toolkit/preload'
import { contextBridge, ipcRenderer } from 'electron'

import { AutoWebsocket } from './AutoWebsocket'

const AXIOM_URLS = ['ws://localhost:10464', 'ws://192.168.43.1:10464']

let ws: AutoWebsocket | null = null

function setupEventHandlers(websocket: AutoWebsocket): void {
  websocket.on('open', () => {
    ipcRenderer.send('axiom-connected')
  })

  websocket.on('close', () => {
    ipcRenderer.send('axiom-disconnected')
  })

  websocket.on('message', (data) => {
    try {
      const parsed = JSON.parse(data)
      ipcRenderer.send('axiom-data', parsed)
    } catch (error) {
      console.error(error)
    }
  })
}

function tryConnect(urls: string[]): void {
  const testConnections: AutoWebsocket[] = []
  let resolved = false

  // Try all URLs in parallel
  for (const url of urls) {
    const testWs = new AutoWebsocket(url)

    testWs.on('open', () => {
      if (!resolved) {
        resolved = true
        // Clean up other connections
        testConnections.forEach((conn) => {
          if (conn !== testWs) {
            conn.removeAllListeners()
            conn.stop()
          }
        })
        ws = testWs
        setupEventHandlers(ws)
      }
    })

    testConnections.push(testWs)
    testWs.start()
  }

  // If none connect within a reasonable time, use the last one as fallback
  setTimeout(() => {
    if (!resolved && ws === null) {
      resolved = true
      testConnections.forEach((conn) => {
        conn.removeAllListeners()
        conn.stop()
      })
      ws = new AutoWebsocket(urls[urls.length - 1])
      setupEventHandlers(ws)
      ws.start()
    }
  }, 5000)
}

tryConnect(AXIOM_URLS)

export type UpdateInfo = { version: string }
export type UpdateProgress = { percent: number }
export type UpdateError = { message: string }

const axiomAPI = {
  isConnected: () => ws?.isConnected ?? false,
  onConnected: (callback: () => void): (() => void) => {
    const handler = () => callback()
    ipcRenderer.on('axiom-connected', handler)
    return () => ipcRenderer.removeListener('axiom-connected', handler)
  },
  onDisconnected: (callback: () => void): (() => void) => {
    const handler = () => callback()
    ipcRenderer.on('axiom-disconnected', handler)
    return () => ipcRenderer.removeListener('axiom-disconnected', handler)
  },
  onData: (callback: (data: unknown) => void): (() => void) => {
    const handler = (_: unknown, data: unknown) => callback(data)
    ipcRenderer.on('axiom-data', handler)
    return () => ipcRenderer.removeListener('axiom-data', handler)
  },
  send: (data: string | ArrayBufferLike | Blob | ArrayBufferView) => {
    if (ws) {
      ws.send(data)
    }
  },
  getVersion: async (): Promise<string> => ipcRenderer.invoke('app:getVersion'),

  // Auto-update API
  checkForUpdates: async (): Promise<unknown> => ipcRenderer.invoke('update:check'),
  downloadUpdate: async (): Promise<boolean> => ipcRenderer.invoke('update:download'),
  installUpdate: (): void => ipcRenderer.send('update:install'),
  onUpdateAvailable: (callback: (info: UpdateInfo) => void): (() => void) => {
    const handler = (_: unknown, info: UpdateInfo) => callback(info)
    ipcRenderer.on('update-available', handler)
    return () => ipcRenderer.removeListener('update-available', handler)
  },
  onUpdateProgress: (callback: (progress: UpdateProgress) => void): (() => void) => {
    const handler = (_: unknown, progress: UpdateProgress) => callback(progress)
    ipcRenderer.on('update-progress', handler)
    return () => ipcRenderer.removeListener('update-progress', handler)
  },
  onUpdateDownloaded: (callback: (info: UpdateInfo) => void): (() => void) => {
    const handler = (_: unknown, info: UpdateInfo) => callback(info)
    ipcRenderer.on('update-downloaded', handler)
    return () => ipcRenderer.removeListener('update-downloaded', handler)
  },
  onUpdateError: (callback: (error: UpdateError) => void): (() => void) => {
    const handler = (_: unknown, error: UpdateError) => callback(error)
    ipcRenderer.on('update-error', handler)
    return () => ipcRenderer.removeListener('update-error', handler)
  },

  // ADB status events
  onAdbNotAvailable: (callback: () => void): (() => void) => {
    const handler = () => callback()
    ipcRenderer.on('adb-not-available', handler)
    return () => ipcRenderer.removeListener('adb-not-available', handler)
  },
  onAdbNoDevice: (callback: () => void): (() => void) => {
    const handler = () => callback()
    ipcRenderer.on('adb-no-device', handler)
    return () => ipcRenderer.removeListener('adb-no-device', handler)
  },
  onAdbForwardingSuccess: (callback: () => void): (() => void) => {
    const handler = () => callback()
    ipcRenderer.on('adb-forwarding-success', handler)
    return () => ipcRenderer.removeListener('adb-forwarding-success', handler)
  },
  onAdbRetryFailed: (callback: () => void): (() => void) => {
    const handler = () => callback()
    ipcRenderer.on('adb-retry-failed', handler)
    return () => ipcRenderer.removeListener('adb-retry-failed', handler)
  },
  // ADB test mode
  setAdbTestMode: async (
    mode: 'normal' | 'simulate-not-installed' | 'simulate-no-device'
  ): Promise<boolean> => ipcRenderer.invoke('adb:setTestMode', mode),
  refreshAdb: async (): Promise<boolean> => ipcRenderer.invoke('adb:refresh')
}

if (process.contextIsolated) {
  try {
    contextBridge.exposeInMainWorld('electron', electronAPI)
    contextBridge.exposeInMainWorld('axiomAPI', axiomAPI)
  } catch (error) {
    console.error('[preload] expose failed', error)
  }
} else {
  // @ts-ignore (define in dts)
  window.electron = electronAPI
  // @ts-ignore (define in dts)
  window.axiomAPI = axiomAPI
}
