import { ElectronAPI } from '@electron-toolkit/preload';

declare global {
  interface Window {
    electron: ElectronAPI
    axiomAPI: {
      isConnected: () => boolean
      onData: (callback: (data: unknown) => void) => void
      send: (data: string | ArrayBufferLike | Blob | ArrayBufferView) => void
    }
  }
}
