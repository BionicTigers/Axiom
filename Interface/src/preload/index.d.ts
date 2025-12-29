import { ElectronAPI } from '@electron-toolkit/preload'

export type UpdateInfo = { version: string }
export type UpdateProgress = { percent: number }
export type UpdateError = { message: string }

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
      // Auto-update API
      checkForUpdates: () => Promise<unknown>
      downloadUpdate: () => Promise<boolean>
      installUpdate: () => void
      onUpdateAvailable: (callback: (info: UpdateInfo) => void) => () => void
      onUpdateProgress: (callback: (progress: UpdateProgress) => void) => () => void
      onUpdateDownloaded: (callback: (info: UpdateInfo) => void) => () => void
      onUpdateError: (callback: (error: UpdateError) => void) => () => void
      // ADB status API
      onAdbNotAvailable: (callback: () => void) => () => void
      onAdbNoDevice: (callback: () => void) => () => void
      onAdbForwardingSuccess: (callback: () => void) => () => void
      onAdbRetryFailed: (callback: () => void) => () => void
      setAdbTestMode: (
        mode: 'normal' | 'simulate-not-installed' | 'simulate-no-device'
      ) => Promise<boolean>
      refreshAdb: () => Promise<boolean>
    }
  }
}
