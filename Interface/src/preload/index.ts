import { electronAPI } from '@electron-toolkit/preload';
import { contextBridge, ipcRenderer } from 'electron';

import { AutoWebsocket } from './AutoWebsocket';

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
  getVersion: async (): Promise<string> => ipcRenderer.invoke('app:getVersion')
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
