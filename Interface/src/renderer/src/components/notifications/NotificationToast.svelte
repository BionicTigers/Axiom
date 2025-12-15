<script lang="ts">
  import { onMount } from 'svelte'
  import {
    NotificationType,
    removeNotification,
    type Notification
  } from '../../lib/stores/notificationStore'

  let { notification }: { notification: Notification } = $props()

  let typeColor = $derived(
    notification.type === NotificationType.WARNING
      ? 'rgb(255, 204, 0)'
      : notification.type === NotificationType.ERROR
        ? 'rgb(255, 80, 80)'
        : 'rgb(230, 233, 239)'
  )

  let duration = notification.duration || 5000

  onMount(() => {
    const timer = setTimeout(() => {
      removeNotification(notification.id)
    }, duration)
    return () => clearTimeout(timer)
  })

  function close() {
    removeNotification(notification.id)
  }
</script>

<button class="toast" onclick={close} style="--color: {typeColor}">
  <div class="content">
    <h4 style="color: var(--color)">{notification.title}</h4>
    <p>{notification.message}</p>
  </div>
  <div class="progress-bar">
    <div
      class="fill"
      style="animation-duration: {duration}ms; background-color: var(--color)"
    ></div>
  </div>
</button>

<style>
  .toast {
    background-color: rgb(33, 42, 60);
    border: 1px solid rgb(43, 52, 70);
    border-left: 4px solid var(--color);
    border-radius: 6px;
    padding: 0;
    width: 300px;
    box-shadow: 0 4px 12px rgba(0, 0, 0, 0.3);
    cursor: pointer;
    overflow: hidden;
    display: flex;
    flex-direction: column;
    text-align: left;
    position: relative;
    pointer-events: auto;
    transition:
      box-shadow 0.2s,
      border-color 0.2s;
  }

  .toast:hover {
    box-shadow: 0 6px 16px rgba(0, 0, 0, 0.4);
    border-color: rgb(53, 62, 80);
  }

  .toast:active {
    box-shadow: 0 2px 8px rgba(0, 0, 0, 0.3);
  }

  .content {
    padding: 12px;
  }

  h4 {
    margin: 0 0 4px 0;
    font-size: 0.95rem;
    font-weight: 600;
  }

  p {
    margin: 0;
    color: rgb(200, 203, 210);
    font-size: 0.85rem;
    line-height: 1.4;
    word-break: break-word;
    white-space: pre-wrap;
  }

  .progress-bar {
    height: 3px;
    width: 100%;
    background-color: rgba(0, 0, 0, 0.2);
  }

  .fill {
    height: 100%;
    width: 100%;
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
