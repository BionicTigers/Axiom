<script lang="ts">
  import { getCommandExecutionTimeColor } from '../../../lib/color'
  import { schedulableStore, type Schedulable } from '../../../lib/stores/schedulableStore'

  let { id, schedulable }: { id: string; schedulable: Schedulable } = $props()

  let stateStore = schedulable.state
  const executionTime = $derived(
    (() => {
      const raw = $stateStore.getValue('executionTime') as unknown
      const num = typeof raw === 'string' ? parseInt(raw) : ((raw as number) ?? 0)
      return Number.isFinite(num) ? num : 0
    })()
  )
</script>

<li class="schedulable">
  {#if schedulable.parent}
    {@const parent = $schedulableStore.get(schedulable.parent)}
    {#if parent}
      <div class="info">
        <p class="gray">{parent.name}</p>
      </div>
    {/if}
  {/if}
  <p>{schedulable.name}</p>
  <div class="info">
    <p class="gray">{id}</p>
    <p class="right" style={`color: ${getCommandExecutionTimeColor(executionTime)}`}>
      {executionTime}ms
    </p>
  </div>
</li>

<style>
  .schedulable {
    background-color: rgb(33, 42, 60);
    padding: 10px;
    border-radius: 10px;
    display: flex;
    flex-direction: column;
  }
  .info {
    display: flex;
  }
  .info p {
    font-size: 12px;
  }
  .right {
    margin-left: auto;
  }
  .gray {
    color: rgb(186, 186, 186);
  }
</style>
