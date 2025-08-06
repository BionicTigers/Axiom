import { electronAPI } from '@electron-toolkit/preload';
import { contextBridge, ipcRenderer } from 'electron';

import { AutoWebsocket } from './AutoWebsocket';

const AXIOM_URL = 'ws://localhost:10464'

const ws = new AutoWebsocket(AXIOM_URL)

ws.on('open', () => {
  console.log('Axiom connected')
  ipcRenderer.send('axiom-connected')
})

ws.on('close', () => {
  console.log('Axiom disconnected')
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

const axiomAPI = {
  isConnected: () => ws.isConnected,
  onData: (callback: (data: unknown) => void) => {
    ipcRenderer.on('axiom-data', (_, data) => callback(data))
  },
  send: (data: string | ArrayBufferLike | Blob | ArrayBufferView) => {
    ws.send(data)
  }
}

if (process.contextIsolated) {
  try {
    contextBridge.exposeInMainWorld('electron', electronAPI)
    contextBridge.exposeInMainWorld('axiomAPI', axiomAPI)
  } catch (error) {
    console.error(error)
  }
} else {
  // @ts-ignore (define in dts)
  window.electron = electronAPI
  // @ts-ignore (define in dts)
  window.axiomAPI = axiomAPI
}
