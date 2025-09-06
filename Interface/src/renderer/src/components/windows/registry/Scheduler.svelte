<script lang="ts">
  import { schedulableOrderStore, schedulableStore } from '../../../lib/stores/schedulableStore'
  import { update } from '../../../lib/stores/windows'
  import SchedulableRow from './SchedulableRow.svelte'

  let { id }: { id: string } = $props()
  update(id, { maxW: 350 })
</script>

<div>
  <ul>
    <!-- <li class="schedulable">
      <h3>Scheduler</h3>
      <div class="info">
        <p>Execution Time: {schedulerExecutionTime ?? "??"}ms</p>
      </div>
    </li> -->
    <li>
      <h3>Execution Order</h3>
      <hr />
    </li>
    {#each $schedulableOrderStore as id (id)}
      {@const schedulable = $schedulableStore.get(id)}
      {#if schedulable}
        <SchedulableRow {id} {schedulable} />
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
