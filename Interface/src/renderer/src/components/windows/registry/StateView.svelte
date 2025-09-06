<script lang="ts">
  import { getCommandExecutionTimeColor } from "../../../lib/color"
  import { schedulableStore } from "../../../lib/stores/schedulableStore";
  import { update } from "../../../lib/stores/windows";
  import StateValue from "../../StateValue.svelte";
  
  let { id }: { id: string } = $props()

  let schedulable = $derived($schedulableStore.get(id))
  let state = $derived(schedulable?.state)

  let color = $derived(getCommandExecutionTimeColor(schedulable?.state.getValue('executionTime') as number ?? 0))

  update(id, { maxW: 350 })
</script>

{#if !schedulable}
  <div class="empty">No state available.</div>
{:else}
  <div class="header">
    <span class="type">{schedulable.type}</span>
    <span class="execution-time" style={`color: ${color}`}>{schedulable.state.getValue('executionTime')}ms</span>
  </div>
  <ul>
    {#each state as [key, value]}
      <StateValue label={key} value={value} />
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