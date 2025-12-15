import { writable } from 'svelte/store'

import { registerNetworkEvent } from '../networkRegistry'

export enum NotificationType {
  INFO = 'INFO',
  WARNING = 'WARNING',
  ERROR = 'ERROR'
}

export interface NotificationData {
  title: string
  message: string
  type: NotificationType
  isModal: boolean
}

export interface Notification extends NotificationData {
  id: string
  tick: number
  timestamp: number
  duration?: number // Duration in ms for toasts
}

export const notificationStore = writable<Notification[]>([])

export function addNotification(data: NotificationData, tick: number) {
  const id = crypto.randomUUID()
  const notification: Notification = {
    ...data,
    id,
    tick,
    timestamp: Date.now(),
    duration: data.isModal ? undefined : 5000 // Default 5s for toasts
  }

  notificationStore.update((n) => [...n, notification])
}

export function removeNotification(id: string) {
  notificationStore.update((n) => n.filter((item) => item.id !== id))
}

registerNetworkEvent('notification', (data: NotificationData, tick: number) => {
  addNotification(data, tick)
})
