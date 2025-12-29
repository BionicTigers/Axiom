<script lang="ts">
  import { onMount } from 'svelte'
  import {
    NotificationType,
    removeNotification,
    type Notification
  } from '../../lib/stores/notificationStore'
  import WarningIcon from '~icons/material-symbols/warning-rounded'
  import ErrorIcon from '~icons/material-symbols/error-rounded'
  import InfoIcon from '~icons/material-symbols/info-rounded'

  let { notification }: { notification: Notification } = $props()

  let visible = $state(false)
  let exiting = $state(false)

  let typeColor = $derived(
    notification.type === NotificationType.WARNING
      ? '#f59e0b'
      : notification.type === NotificationType.ERROR
        ? '#ef4444'
        : '#3b82f6'
  )

  let bgTint = $derived(
    notification.type === NotificationType.WARNING
      ? 'rgba(245, 158, 11, 0.08)'
      : notification.type === NotificationType.ERROR
        ? 'rgba(239, 68, 68, 0.08)'
        : 'rgba(59, 130, 246, 0.08)'
  )

  let duration = $derived(notification.duration || 5000)

  onMount(() => {
    // Trigger enter animation
    requestAnimationFrame(() => {
      visible = true
    })

    const timer = setTimeout(() => {
      close()
    }, duration)

    return () => clearTimeout(timer)
  })

  function close() {
    exiting = true
    setTimeout(() => {
      removeNotification(notification.id)
    }, 200)
  }
</script>

<button
  class="toast"
  class:visible
  class:exiting
  style="--color: {typeColor}; --bg-tint: {bgTint}"
  onclick={close}
>
  <div class="icon-wrapper">
    {#if notification.type === NotificationType.WARNING}
      <WarningIcon />
    {:else if notification.type === NotificationType.ERROR}
      <ErrorIcon />
    {:else}
      <InfoIcon />
    {/if}
  </div>

  <div class="content">
    <h4>{notification.title}</h4>
    <p>{notification.message}</p>
  </div>

  <div class="progress-bar">
    <div class="fill" style="animation-duration: {duration}ms"></div>
  </div>
</button>

<style>
  .toast {
    border: 1px solid rgba(255, 255, 255, 0.05);
    background-color: rgba(0, 0, 0, 0.2);
    backdrop-filter: blur(10px);
    border-radius: 12px;
    width: 340px;
    overflow: hidden;
    display: grid;
    grid-template-columns: auto 1fr auto;
    grid-template-rows: 1fr auto;
    align-items: start;
    position: relative;
    pointer-events: auto;
    transform: translateX(120%);
    opacity: 0;
    transition:
      transform 0.3s cubic-bezier(0.16, 1, 0.3, 1),
      opacity 0.3s ease;
    cursor: pointer;
  }

  .toast:hover {
    background-color: rgba(0, 0, 0, 0.3);
  }

  .toast.visible {
    transform: translateX(0);
    opacity: 1;
  }

  .toast.exiting {
    transform: translateX(120%);
    opacity: 0;
    transition:
      transform 0.2s ease-in,
      opacity 0.2s ease;
  }

  .icon-wrapper {
    grid-row: 1;
    grid-column: 1;
    display: flex;
    align-items: center;
    justify-content: center;
    padding: 16px 0 16px 16px;
    color: var(--color);
    font-size: 22px;
  }

  .content {
    grid-row: 1;
    grid-column: 2;
    padding: 14px 12px;
    min-width: 0;
  }

  h4 {
    margin: 0 0 4px 0;
    font-size: 0.9rem;
    font-weight: 600;
    color: rgb(240, 242, 245);
    letter-spacing: -0.01em;
  }

  p {
    margin: 0;
    color: rgb(156, 163, 175);
    font-size: 0.8rem;
    line-height: 1.45;
    word-break: break-word;
  }

  .close-btn {
    grid-row: 1;
    grid-column: 3;
    display: flex;
    align-items: center;
    justify-content: center;
    background: transparent;
    border: none;
    padding: 8px;
    margin: 8px 8px 0 0;
    cursor: pointer;
    color: rgb(100, 110, 130);
    font-size: 16px;
    border-radius: 6px;
    transition:
      background-color 0.15s,
      color 0.15s;
  }

  .close-btn:hover {
    background-color: rgba(255, 255, 255, 0.08);
    color: rgb(180, 185, 195);
  }

  .progress-bar {
    grid-row: 2;
    grid-column: 1 / -1;
    height: 3px;
    width: 100%;
    background-color: rgba(255, 255, 255, 0.05);
  }

  .fill {
    height: 100%;
    width: 100%;
    background: linear-gradient(90deg, var(--color), color-mix(in srgb, var(--color) 60%, white));
    transform-origin: left;
    animation-name: shrink;
    animation-timing-function: linear;
    animation-fill-mode: forwards;
  }

  @keyframes shrink {
    from {
      transform: scaleX(1);
    }
    to {
      transform: scaleX(0);
    }
  }
</style>
