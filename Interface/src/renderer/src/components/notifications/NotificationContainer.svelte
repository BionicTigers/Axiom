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
    <div class="toast-wrapper" animate:flip={{ duration: 250 }}>
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
    bottom: 100px;
    right: 24px;
    display: flex;
    flex-direction: column-reverse;
    gap: 12px;
    z-index: 9999;
    pointer-events: none;
    max-height: calc(100vh - 140px);
    overflow: hidden;
  }

  .toast-wrapper {
    display: flex;
    justify-content: flex-end;
  }
</style>
