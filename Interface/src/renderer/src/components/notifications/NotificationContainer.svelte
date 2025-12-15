<script lang="ts">
  import { notificationStore } from '../../lib/stores/notificationStore'
  import NotificationModal from './NotificationModal.svelte'
  import NotificationToast from './NotificationToast.svelte'
  import { flip } from 'svelte/animate'

  // Separate toasts and modals
  let toasts = $derived($notificationStore.filter((n) => !n.isModal))
  let modals = $derived($notificationStore.filter((n) => n.isModal))
</script>

<div class="toast-container">
  {#each toasts as toast (toast.id)}
    <div animate:flip={{ duration: 300 }}>
      <NotificationToast notification={toast} />
    </div>
  {/each}
</div>

<!-- Display modals on top of everything. If multiple, we stack them (render all),
     but functionally only the top one is interactable usually.
     Or just render the last one. Let's render the last one for focus. -->
{#if modals.length > 0}
  <NotificationModal notification={modals[modals.length - 1]} />
{/if}

<style>
  .toast-container {
    position: fixed;
    bottom: 20px;
    right: 20px;
    display: flex;
    flex-direction: column;
    gap: 10px;
    z-index: 9999;
    pointer-events: none; /* Allows clicking through the container */
    max-height: 100vh;
    overflow: hidden;
  }
</style>
