import { ElectronAPI } from '@electron-toolkit/preload';

declare global {
  interface Window {
    electron: ElectronAPI
    axiomAPI: {
      onData: (callback: (data: unknown) => void) => void
      send: (data: string | ArrayBufferLike | Blob | ArrayBufferView) => void
    }
  }
}
