<script lang="ts">
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

  function close() {
    removeNotification(notification.id)
  }
</script>

<div class="overlay">
  <div class="modal" style="--color: {typeColor}">
    <h2 style="color: var(--color); font-weight: 800">{notification.title}</h2>
    <p>{notification.message}</p>
    <button class="close-btn" onclick={close}>Close</button>
  </div>
</div>

<style>
  .overlay {
    position: fixed;
    top: 0;
    left: 0;
    width: 100vw;
    height: 100vh;
    background-color: rgba(0, 0, 0, 0.5);
    backdrop-filter: blur(4px);
    z-index: 1000;
    display: flex;
    justify-content: center;
    align-items: center;
  }

  .modal {
    background-color: rgba(25, 30, 40, 0.6);
    backdrop-filter: blur(12px);
    border: 1px solid rgb(43, 52, 70);
    border-top: 4px solid var(--color);
    padding: 2rem;
    border-radius: 12px;
    max-width: 500px;
    width: 90%;
    box-shadow: 0 10px 25px rgba(0, 0, 0, 0.5);
    display: flex;
    flex-direction: column;
    gap: 1.5rem;
    text-align: center;
  }

  h2 {
    margin: 0;
    font-size: 1.5rem;
  }

  p {
    margin: 0;
    color: rgb(230, 233, 239);
    line-height: 1.6;
    font-size: 1.1rem;
    white-space: pre-wrap;
  }

  .close-btn {
    align-self: center;
    padding: 0.6rem 2rem;
    background-color: rgb(43, 52, 70);
    color: white;
    border: 1px solid rgb(63, 72, 90);
    border-radius: 6px;
    font-size: 1rem;
    cursor: pointer;
    transition: background-color 0.2s;
  }

  .close-btn:hover {
    background-color: rgb(53, 62, 80);
  }
</style>
