<script lang="ts">
  import { getCommandExecutionTimeColor } from '../../../lib/color'
  import { schedulableStore } from '../../../lib/stores/schedulableStore'
  import { update } from '../../../lib/stores/windows'
  import StateValue from '../../StateValue.svelte'

  let { id }: { id: string } = $props()

  let schedulable = $derived($schedulableStore.get(id))
  let scheduableState = $derived(schedulable?.state)

  let executionTime = $derived(
    scheduableState ? (($scheduableState?.getValue('executionTime') as number) ?? 0) : 0
  )
  let color = $derived(getCommandExecutionTimeColor(executionTime))

  let stateCount = $derived(scheduableState ? $scheduableState.size : 0)

  update(id, { maxW: 450, minW: 320 })
</script>

{#if !schedulable || !scheduableState}
  <div class="empty-state">
    <p>No state available</p>
    <span>This schedulable may have been removed</span>
  </div>
{:else}
  <div class="state-view">
    <div class="header">
      <div class="header-left">
        <span class="type-badge" class:command={schedulable.type === 'Command'}>
          {schedulable.type}
        </span>
        <span class="state-count">{stateCount} fields</span>
      </div>
      <div class="header-right">
        <span class="exec-label">Exec Time</span>
        <span class="exec-time" style="color: {color}">{executionTime}ms</span>
      </div>
    </div>

    <ul class="state-list">
      {#each $scheduableState as [key, value] (key)}
        <StateValue
          label={key}
          {value}
          path={`${schedulable.type}.${id}.${key}`}
          stateMap={$scheduableState}
        />
      {/each}
    </ul>
  </div>
{/if}

<style>
  .state-view {
    display: flex;
    flex-direction: column;
    height: 100%;
    gap: 12px;
  }

  .header {
    display: flex;
    justify-content: space-between;
    align-items: center;
    padding: 10px 12px;
    background-color: rgb(33, 42, 60);
    border: 1px solid rgb(43, 52, 70);
    border-radius: 8px;
  }

  .header-left {
    display: flex;
    align-items: center;
    gap: 10px;
  }

  .header-right {
    display: flex;
    align-items: center;
    gap: 8px;
  }

  .type-badge {
    font-size: 0.7rem;
    color: rgb(150, 160, 180);
    background-color: rgb(50, 60, 80);
    padding: 4px 10px;
    border-radius: 4px;
    text-transform: uppercase;
    letter-spacing: 0.5px;
    font-weight: 500;
  }

  .type-badge.command {
    background-color: rgba(77, 171, 247, 0.15);
    color: rgb(120, 190, 255);
  }

  .state-count {
    font-size: 0.8rem;
    color: rgb(150, 160, 180);
  }

  .exec-label {
    font-size: 0.75rem;
    color: rgb(120, 130, 150);
    text-transform: uppercase;
    letter-spacing: 0.3px;
  }

  .exec-time {
    font-size: 0.95rem;
    font-weight: 600;
    font-family: ui-monospace, SFMono-Regular, Menlo, Monaco, Consolas, monospace;
  }

  .state-list {
    list-style: none;
    padding: 0;
    margin: 0;
    display: flex;
    flex-direction: column;
    gap: 6px;
    overflow-y: auto;
    flex: 1;
  }

  .empty-state {
    display: flex;
    flex-direction: column;
    align-items: center;
    justify-content: center;
    height: 100%;
    gap: 8px;
    color: rgb(150, 160, 180);
  }

  .empty-state p {
    margin: 0;
    font-size: 1rem;
    font-weight: 500;
  }

  .empty-state span {
    font-size: 0.85rem;
    opacity: 0.7;
  }
</style>
