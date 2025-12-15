<script lang="ts">
  import { schedulableOrderStore, schedulableStore } from '../../../lib/stores/schedulableStore'
  import { schedulerDetails } from '../../../lib/stores/schedulerDetails'
  import { update } from '../../../lib/stores/windows'
  import SchedulableRow from './SchedulableRow.svelte'

  let { id }: { id: string } = $props()
  update(id, { maxW: 380, minW: 300 })
</script>

<div class="scheduler-container">
  <div class="header-info">
    <div class="stat">
      <span class="stat-label">Tick</span>
      <span class="stat-value">{$schedulerDetails.tick}</span>
    </div>
    <div class="stat">
      <span class="stat-label">Exec Time</span>
      <span class="stat-value">{$schedulerDetails.executionTime.toFixed(1)}ms</span>
    </div>
  </div>

  <div class="section">
    <div class="section-header">
      <span class="section-title">Execution Order</span>
      <span class="count">{$schedulableOrderStore.length}</span>
    </div>

    <ul class="order-list">
      {#each $schedulableOrderStore as itemId, index (itemId)}
        {@const schedulable = $schedulableStore.get(itemId)}
        {#if schedulable}
          <SchedulableRow {itemId} {schedulable} {index} />
        {/if}
      {:else}
        <li class="empty">No schedulables in execution order</li>
      {/each}
    </ul>
  </div>
</div>

<style>
  .scheduler-container {
    display: flex;
    flex-direction: column;
    height: 100%;
    gap: 16px;
  }

  .header-info {
    display: flex;
    gap: 12px;
    padding-bottom: 12px;
    border-bottom: 1px solid rgb(60, 70, 90);
  }

  .stat {
    flex: 1;
    display: flex;
    flex-direction: column;
    align-items: center;
    gap: 4px;
    padding: 10px;
    background-color: rgb(33, 42, 60);
    border-radius: 8px;
    border: 1px solid rgb(43, 52, 70);
  }

  .stat-label {
    font-size: 0.75rem;
    color: rgb(150, 160, 180);
    text-transform: uppercase;
    letter-spacing: 0.5px;
  }

  .stat-value {
    font-size: 1.2rem;
    font-weight: 600;
    color: rgb(230, 233, 239);
  }

  .section {
    flex: 1;
    display: flex;
    flex-direction: column;
    min-height: 0;
  }

  .section-header {
    display: flex;
    align-items: center;
    gap: 8px;
    padding-bottom: 8px;
    margin-bottom: 8px;
    border-bottom: 1px solid rgb(60, 70, 90);
  }

  .section-title {
    font-weight: 600;
    font-size: 0.95rem;
    color: rgb(230, 233, 239);
  }

  .count {
    margin-left: auto;
    font-size: 0.8rem;
    color: rgb(150, 160, 180);
    background-color: rgb(43, 52, 70);
    padding: 2px 8px;
    border-radius: 10px;
  }

  .order-list {
    list-style: none;
    padding: 0;
    margin: 0;
    display: flex;
    flex-direction: column;
    gap: 6px;
    overflow-y: auto;
    flex: 1;
  }

  .empty {
    padding: 16px;
    text-align: center;
    color: rgb(120, 130, 150);
    font-size: 0.85rem;
    font-style: italic;
  }
</style>
