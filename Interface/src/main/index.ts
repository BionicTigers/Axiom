import { electronApp, is, optimizer } from '@electron-toolkit/utils'
import { app, BrowserWindow, ipcMain, shell } from 'electron'
import { autoUpdater } from 'electron-updater'
import { join } from 'path'

import icon from '../../resources/icon.ico?asset'
import iconOther from '../../resources/icon.png?asset'

const version = app.getVersion()

// Configure auto-updater
autoUpdater.autoDownload = false
autoUpdater.autoInstallOnAppQuit = true

// Set application user model ID for Windows (for proper taskbar grouping and notifications)
electronApp.setAppUserModelId('com.bionictigers.seek')

// Track renderer readiness and queue messages arriving before listeners are attached
let rendererReady = false
let mainWebContents: Electron.WebContents | null = null
type UpdateChannel = 'update-available' | 'update-downloaded' | 'update-error' | 'update-progress'
type AxiomChannel = 'axiom-connected' | 'axiom-disconnected' | 'axiom-data'

const pendingAxiomMessages: Array<{
  channel: AxiomChannel
  payload?: unknown
}> = []

function sendToRenderer(channel: AxiomChannel | UpdateChannel, payload?: unknown): void {
  if (rendererReady && mainWebContents) {
    mainWebContents.send(channel, payload)
  } else if (channel.startsWith('axiom-')) {
    // Only queue Axiom messages; update messages are sent after renderer is ready
    pendingAxiomMessages.push({ channel: channel as AxiomChannel, payload })
  }
}

function createWindow(): void {
  // Create the browser window.
  const mainWindow = new BrowserWindow({
    width: 900,
    height: 670,
    show: false,
    icon: icon,
    title: `Seek (${version})`,
    autoHideMenuBar: true,
    ...(process.platform !== 'win32' ? { icon: iconOther } : {}),
    webPreferences: {
      preload: join(__dirname, '../preload/index.js'),
      sandbox: false
    }
  })

  mainWindow.on('ready-to-show', () => {
    mainWindow.show()
  })

  // Remember the webContents for later sends
  mainWebContents = mainWindow.webContents

  // During reloads/navigations, ensure we queue messages until renderer reattaches listeners
  mainWindow.webContents.on('will-navigate', () => {
    rendererReady = false
    console.log('[main] will-navigate -> rendererReady=false')
  })
  mainWindow.webContents.on('did-start-navigation', () => {
    rendererReady = false
    console.log('[main] did-start-navigation -> rendererReady=false')
  })
  mainWindow.webContents.on('did-start-loading', () => {
    rendererReady = false
    console.log('[main] did-start-loading -> rendererReady=false')
  })

  mainWindow.webContents.setWindowOpenHandler((details) => {
    shell.openExternal(details.url)
    return { action: 'deny' }
  })

  // HMR for renderer base on electron-vite cli.
  // Load the remote URL for development or the local html file for production.
  if (is.dev && process.env['ELECTRON_RENDERER_URL']) {
    mainWindow.loadURL(process.env['ELECTRON_RENDERER_URL'])
  } else {
    mainWindow.loadFile(join(__dirname, '../renderer/index.html'))
  }
}

// This method will be called when Electron has finished
// initialization and is ready to create browser windows.
// Some APIs can only be used after this event occurs.
app.whenReady().then(() => {
  // App user model id already set above for auto-updater

  // Default open or close DevTools by F12 in development
  // and ignore CommandOrControl + R in production.
  // see https://github.com/alex8088/electron-toolkit/tree/master/packages/utils
  app.on('browser-window-created', (_, window) => {
    optimizer.watchWindowShortcuts(window)
  })

  // IPC test
  ipcMain.on('ping', () => console.log('pong'))

  // Receive forwarded renderer console messages
  ipcMain.on(
    'renderer-console',
    (_event, payload: { level: 'log' | 'info' | 'warn' | 'error' | 'debug'; args: string[] }) => {
      const { level, args } = payload || { level: 'log', args: [] }
      switch (level) {
        case 'error':
          console.error('[renderer]', ...args)
          break
        case 'warn':
          console.warn('[renderer]', ...args)
          break
        case 'info':
          console.info('[renderer]', ...args)
          break
        case 'debug':
          console.debug('[renderer]', ...args)
          break
        default:
          console.log('[renderer]', ...args)
      }
    }
  )

  // Expose app version to renderer via IPC
  ipcMain.handle('app:getVersion', () => app.getVersion())

  // Auto-updater events
  autoUpdater.on('update-available', (info) => {
    console.log('[main] Update available:', info.version)
    sendToRenderer('update-available', { version: info.version })
  })

  autoUpdater.on('update-not-available', () => {
    console.log('[main] No updates available')
  })

  autoUpdater.on('download-progress', (progress) => {
    sendToRenderer('update-progress', { percent: progress.percent })
  })

  autoUpdater.on('update-downloaded', (info) => {
    console.log('[main] Update downloaded:', info.version)
    sendToRenderer('update-downloaded', { version: info.version })
  })

  autoUpdater.on('error', (err) => {
    console.error('[main] Auto-updater error:', err)
    sendToRenderer('update-error', { message: err.message })
  })

  // IPC handlers for update actions
  ipcMain.handle('update:check', async () => {
    if (is.dev) {
      console.log('[main] Skipping update check in dev mode')
      return null
    }
    try {
      const result = await autoUpdater.checkForUpdates()
      return result?.updateInfo ?? null
    } catch (err) {
      console.error('[main] Update check failed:', err)
      return null
    }
  })

  ipcMain.handle('update:download', async () => {
    try {
      await autoUpdater.downloadUpdate()
      return true
    } catch (err) {
      console.error('[main] Download failed:', err)
      return false
    }
  })

  ipcMain.on('update:install', () => {
    autoUpdater.quitAndInstall(false, true)
  })

  // Axiom connection handlers
  ipcMain.on('axiom-connected', () => {
    sendToRenderer('axiom-connected')
  })

  ipcMain.on('axiom-disconnected', () => {
    sendToRenderer('axiom-disconnected')
  })

  ipcMain.on('axiom-data', (_event, data) => {
    sendToRenderer('axiom-data', data)
  })

  // Renderer signals when it has registered listeners
  ipcMain.on('renderer-ready', (event) => {
    rendererReady = true
    // Prefer the sender that announced readiness
    mainWebContents = event.sender
    console.log('[main] renderer-ready; flushing', pendingAxiomMessages.length, 'queued messages')
    // Flush any queued messages in order
    for (const { channel, payload } of pendingAxiomMessages) {
      event.sender.send(channel, payload)
    }
    pendingAxiomMessages.length = 0
  })

  createWindow()

  // Check for updates after window is ready (production only)
  if (!is.dev) {
    setTimeout(() => {
      autoUpdater.checkForUpdates().catch((err) => {
        console.error('[main] Initial update check failed:', err)
      })
    }, 3000)
  }

  app.on('activate', function () {
    // On macOS it's common to re-create a window in the app when the
    // dock icon is clicked and there are no other windows open.
    if (BrowserWindow.getAllWindows().length === 0) createWindow()
  })
})

// Quit when all windows are closed, except on macOS. There, it's common
// for applications and their menu bar to stay active until the user quits
// explicitly with Cmd + Q.
app.on('window-all-closed', () => {
  if (process.platform !== 'darwin') {
    app.quit()
  }
})

// In this file you can include the rest of your app's specific main process
// code. You can also put them in separate files and require them here.
