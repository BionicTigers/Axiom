import { electronAPI } from '@electron-toolkit/preload'
import { contextBridge, ipcRenderer } from 'electron'

import { AutoWebsocket } from './AutoWebsocket'

const AXIOM_URL = 'ws://localhost:10464'

const ws = new AutoWebsocket(AXIOM_URL)

ws.on('open', () => {
  ipcRenderer.send('axiom-connected')
})

ws.on('close', () => {
  ipcRenderer.send('axiom-disconnected')
})

ws.on('message', (data) => {
  try {
    const parsed = JSON.parse(data)

    ipcRenderer.send('axiom-data', parsed)
  } catch (error) {
    console.error(error)
  }
})

ws.start()

export type UpdateInfo = { version: string }
export type UpdateProgress = { percent: number }
export type UpdateError = { message: string }

const axiomAPI = {
  isConnected: () => ws.isConnected,
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
    ws.send(data)
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
  }
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
