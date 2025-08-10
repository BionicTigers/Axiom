import { mount } from 'svelte';

import './assets/main.css';
import App from './App.svelte';

const app = mount(App, {
  target: document.getElementById('app')!
})

export default app

// Forward renderer console to main via IPC for better visibility in terminal
;(() => {
  const ipc = window.electron?.ipcRenderer
  type ConsoleLevel = 'log' | 'info' | 'warn' | 'error' | 'debug'
  const levels: ConsoleLevel[] = ['log', 'info', 'warn', 'error', 'debug']
  const con = console as unknown as Record<ConsoleLevel, (...args: unknown[]) => void>
  for (const level of levels) {
    const original = con[level]
    con[level] = (...args: unknown[]) => {
      try {
        ipc?.send('renderer-console', { level, args: args.map(String) })
      } catch {}
      original(...args)
    }
  }

  // Also forward uncaught errors and unhandled rejections
  window.addEventListener('error', (e) => {
    try {
      ipc?.send('renderer-console', {
        level: 'error',
        args: [String(e.error || e.message || 'Unknown error')]
      })
    } catch {}
  })
  window.addEventListener('unhandledrejection', (e) => {
    try {
      const reason =
        (e.reason && (e.reason.stack || e.reason.message || String(e.reason))) ||
        'Unhandled rejection'
      ipc?.send('renderer-console', { level: 'error', args: [String(reason)] })
    } catch {}
  })
})()
