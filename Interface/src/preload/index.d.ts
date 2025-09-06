import { ElectronAPI } from '@electron-toolkit/preload';

declare global {
  interface Window {
    electron: ElectronAPI
    axiomAPI: {
      isConnected: () => boolean
      onConnected: (callback: () => void) => () => void
      onDisconnected: (callback: () => void) => () => void
      onData: (callback: (data: unknown) => void) => () => void
      send: (data: string | ArrayBufferLike | Blob | ArrayBufferView) => void
      getVersion: () => Promise<string>
    }
  }
}
