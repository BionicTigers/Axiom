<script lang="ts">
  import { getCommandExecutionTimeColor } from '../../../lib/color'
  import { schedulableStore, type Schedulable } from '../../../lib/stores/schedulableStore'

  let {
    itemId,
    schedulable,
    index
  }: { itemId: string; schedulable: Schedulable; index: number } = $props()

  let stateStore = schedulable.state
  const executionTime = $derived(
    (() => {
      const raw = $stateStore.getValue('executionTime') as unknown
      const num = typeof raw === 'string' ? parseInt(raw) : ((raw as number) ?? 0)
      return Number.isFinite(num) ? num : 0
    })()
  )

  const parent = $derived(
    schedulable.parent ? $schedulableStore.get(schedulable.parent) : null
  )
</script>

<li class="row">
  <div class="index">{index + 1}</div>
  <div class="content">
    <div class="main-row">
      <span class="name">{schedulable.name}</span>
      <span class="type" class:command={schedulable.type === 'Command'}>
        {schedulable.type}
      </span>
    </div>
    <div class="meta-row">
      <span class="id">{itemId}</span>
      {#if parent}
        <span class="parent">← {parent.name}</span>
      {/if}
      <span class="exec-time" style="color: {getCommandExecutionTimeColor(executionTime)}">
        {executionTime}ms
      </span>
    </div>
  </div>
</li>

<style>
  .row {
    display: flex;
    align-items: stretch;
    background-color: rgb(33, 42, 60);
    border: 1px solid rgb(43, 52, 70);
    border-radius: 8px;
    overflow: hidden;
    transition:
      background-color 0.15s,
      border-color 0.15s;
  }

  .row:hover {
    background-color: rgb(38, 47, 65);
    border-color: rgb(53, 62, 80);
  }

  .index {
    display: flex;
    align-items: center;
    justify-content: center;
    min-width: 32px;
    padding: 8px;
    background-color: rgba(0, 0, 0, 0.15);
    font-size: 0.8rem;
    font-weight: 600;
    color: rgb(120, 130, 150);
  }

  .content {
    flex: 1;
    display: flex;
    flex-direction: column;
    gap: 4px;
    padding: 10px 12px;
    min-width: 0;
  }

  .main-row {
    display: flex;
    align-items: center;
    gap: 8px;
  }

  .name {
    flex: 1;
    font-size: 0.9rem;
    font-weight: 500;
    color: rgb(230, 233, 239);
    overflow: hidden;
    text-overflow: ellipsis;
    white-space: nowrap;
  }

  .type {
    font-size: 0.65rem;
    color: rgb(150, 160, 180);
    background-color: rgb(50, 60, 80);
    padding: 2px 6px;
    border-radius: 4px;
    text-transform: uppercase;
    letter-spacing: 0.5px;
    flex-shrink: 0;
  }

  .type.command {
    background-color: rgba(77, 171, 247, 0.15);
    color: rgb(120, 190, 255);
  }

  .meta-row {
    display: flex;
    align-items: center;
    gap: 8px;
    font-size: 0.75rem;
  }

  .id {
    color: rgb(100, 110, 130);
    font-family: ui-monospace, SFMono-Regular, Menlo, Monaco, Consolas, monospace;
    font-size: 0.7rem;
    max-width: 80px;
    overflow: hidden;
    text-overflow: ellipsis;
    white-space: nowrap;
  }

  .parent {
    color: rgb(120, 130, 150);
    overflow: hidden;
    text-overflow: ellipsis;
    white-space: nowrap;
  }

  .exec-time {
    margin-left: auto;
    font-weight: 500;
    font-family: ui-monospace, SFMono-Regular, Menlo, Monaco, Consolas, monospace;
  }
</style>
