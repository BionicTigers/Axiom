<script lang="ts">
  import { schedulableOrderStore, schedulableStore } from '../../../lib/stores/schedulableStore'
  import { update } from '../../../lib/stores/windows'

  let { id }: { id: string } = $props()
  update(id, { maxW: 350 })

  function getColorForSchedulerTime(executionTime: number) {
    if (executionTime < 20) return 'rgb(100, 240, 100)'
    if (executionTime < 40) return 'rgb(240, 240, 100)'
    return 'rgb(240, 100, 100)'
  }

  function getColorForCommandTime(executionTime: number) {
    if (executionTime < 6) return 'rgb(100, 240, 100)'
    if (executionTime < 12) return 'rgb(240, 240, 100)'
    return 'rgb(240, 100, 100)'
  }
</script>

<div>
  <ul>
    <li class="schedulable">
      <h3>Scheduler</h3>
      <div class="info">
        <!-- <p>Execution Time: {schedulerExecutionTime ?? "??"}ms</p> -->
      </div>
    </li>
    <li>
      <h3>Execution Order</h3>
      <hr>
    </li>
    {#each $schedulableOrderStore as id}
      {@const schedulable = $schedulableStore.get(id)}
      {#if (schedulable)}
        {@const executionTime = schedulable.state.getValue('executionTime') as string ?? "0"}
        <li class="schedulable">
          <p>{schedulable.name}</p>
          <div class="info">
            {#if schedulable.parent}
              {@const parent = $schedulableStore.get(schedulable.parent)}
              {#if parent}
                <p class="gray">{parent.name}</p>
              {/if}
            {/if}
            <p class="right" style={`color: ${getColorForCommandTime(parseInt(executionTime))}`}>{executionTime ?? "??"}ms</p>
          </div>
        </li>
      {/if}
    {/each}
  </ul>
</div>

<style>
  ul {
    list-style: none;
    padding: 0;
    display: flex;
    flex-direction: column;
    gap: 10px;
  }

  hr {
    border: 1px solid rgb(100, 100, 100);
  }

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