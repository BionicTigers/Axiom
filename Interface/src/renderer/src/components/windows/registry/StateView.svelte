<script lang="ts">
  import { getCommandExecutionTimeColor } from '../../../lib/color'
  import { schedulableStore } from '../../../lib/stores/schedulableStore'
  import { update } from '../../../lib/stores/windows'
  import StateValue from '../../StateValue.svelte'

  let { id }: { id: string } = $props()

  let schedulable = $schedulableStore.get(id)
  let scheduableState = schedulable.state

  let color = $derived(
    getCommandExecutionTimeColor(($scheduableState.getValue('executionTime') as number) ?? 0)
  )

  update(id, { maxW: 425 })
</script>

{#if !schedulable}
  <div class="empty">No state available.</div>
{:else}
  <div class="header">
    <span class="type">{schedulable.type}</span>
    <span class="execution-time" style={`color: ${color}`}
      >{($scheduableState.getValue('executionTime') as number) ?? 0}ms</span
    >
  </div>
  <ul>
    {#each $scheduableState as [key, value] (key)}
      <StateValue
        label={key}
        {value}
        path={`${schedulable.type}.${id}.${key}`}
        stateMap={$scheduableState}
      />
    {/each}
  </ul>
{/if}

<style>
  .header {
    display: flex;
    justify-content: center;
    align-items: center;
    gap: 8px;
  }

  .header span {
    font-size: 0.85rem;
    opacity: 0.8;
  }

  ul {
    list-style: none;
    padding: 0;
    display: flex;
    flex-direction: column;
    gap: 8px;
  }

  .empty {
    opacity: 0.7;
  }
</style>
