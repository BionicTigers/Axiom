<script lang="ts">
  import type { Component } from 'svelte'
  import { schedulableStore } from '../../../lib/stores/schedulableStore'
  import { add, bringToFront, update } from '../../../lib/stores/windows'
  import StateView from './StateView.svelte'

  let { id }: { id: string } = $props()
  update(id, { maxW: 350, minW: 280, minH: 300 })

  let persistentExpanded = $state(false)
  let currentExpanded = $state(true)

  function openCommandState(id: string) {
    let schedulable = $schedulableStore.get(id)
    if (!schedulable) return
    add({ id: id, title: schedulable.name, component: StateView as Component })
    bringToFront(schedulable.name)
  }

  let search = $state('')

  let filteredStates = $derived(
    Array.from($schedulableStore).filter(([_, schedulable]) => {
      if (search === '') return true
      return schedulable.name.toLowerCase().includes(search.toLowerCase())
    })
  )

  let commandCount = $derived(filteredStates.filter(([_, s]) => s.type === 'Command').length)
  let systemCount = $derived(filteredStates.filter(([_, s]) => s.type === 'System').length)
</script>

<div class="states-container">
  <div class="search-box">
    <input type="text" placeholder="Search schedulables..." bind:value={search} />
  </div>

  <div class="sections">
    <section>
      <button class="section-header" onclick={() => (persistentExpanded = !persistentExpanded)}>
        <span class="chevron">{persistentExpanded ? '▾' : '▸'}</span>
        <span class="section-title">Persistent</span>
        <span class="count">0</span>
      </button>
      {#if persistentExpanded}
        <ul class="items">
          <li class="empty-hint">No persistent states</li>
        </ul>
      {/if}
    </section>

    <section>
      <button class="section-header" onclick={() => (currentExpanded = !currentExpanded)}>
        <span class="chevron">{currentExpanded ? '▾' : '▸'}</span>
        <span class="section-title">Current</span>
        <span class="count">{filteredStates.length}</span>
      </button>
      {#if currentExpanded}
        <ul class="items">
          {#each filteredStates as [itemId, schedulable] (itemId)}
            <li>
              <button class="item-btn" onclick={() => openCommandState(itemId)}>
                <span class="item-name">{schedulable.name}</span>
                <span class="item-type" class:command={schedulable.type === 'Command'}>
                  {schedulable.type}
                </span>
              </button>
            </li>
          {:else}
            <li class="empty-hint">
              {#if search}
                No matching schedulables
              {:else}
                No schedulables available
              {/if}
            </li>
          {/each}
        </ul>
      {/if}
    </section>
  </div>

  <div class="footer">
    <span>{commandCount} Commands</span>
    <span class="separator">•</span>
    <span>{systemCount} Systems</span>
  </div>
</div>

<style>
  .states-container {
    display: flex;
    flex-direction: column;
    height: 100%;
    gap: 12px;
  }

  .search-box input {
    width: 100%;
    padding: 10px 14px;
    border-radius: 8px;
    border: 1px solid rgb(43, 52, 70);
    background-color: rgb(33, 42, 60);
    color: rgb(230, 233, 239);
    font-size: 0.9rem;
    transition: border-color 0.15s;
  }

  .search-box input:focus {
    outline: none;
    border-color: rgb(77, 171, 247);
  }

  .search-box input::placeholder {
    color: rgb(120, 130, 150);
  }

  .sections {
    flex: 1;
    overflow-y: auto;
    display: flex;
    flex-direction: column;
    gap: 16px;
  }

  section {
    display: flex;
    flex-direction: column;
  }

  .section-header {
    display: flex;
    align-items: center;
    gap: 8px;
    padding: 8px 4px;
    background: transparent;
    border: none;
    cursor: pointer;
    color: rgb(230, 233, 239);
    border-bottom: 1px solid rgb(60, 70, 90);
  }

  .section-header:hover {
    background-color: rgba(255, 255, 255, 0.03);
  }

  .chevron {
    width: 16px;
    font-size: 0.9rem;
    color: rgb(150, 160, 180);
  }

  .section-title {
    font-weight: 600;
    font-size: 0.95rem;
  }

  .count {
    margin-left: auto;
    font-size: 0.8rem;
    color: rgb(150, 160, 180);
    background-color: rgb(43, 52, 70);
    padding: 2px 8px;
    border-radius: 10px;
  }

  .items {
    list-style: none;
    padding: 0;
    margin: 0;
    display: flex;
    flex-direction: column;
    gap: 4px;
    padding-top: 8px;
  }

  .items li {
    border-radius: 8px;
  }

  .item-btn {
    width: 100%;
    display: flex;
    align-items: center;
    gap: 8px;
    padding: 10px 12px;
    background-color: rgb(33, 42, 60);
    border: 1px solid rgb(43, 52, 70);
    border-radius: 8px;
    cursor: pointer;
    color: rgb(230, 233, 239);
    text-align: left;
    transition:
      background-color 0.15s,
      border-color 0.15s;
  }

  .item-btn:hover {
    background-color: rgb(43, 52, 70);
    border-color: rgb(53, 62, 80);
  }

  .item-name {
    flex: 1;
    overflow: hidden;
    text-overflow: ellipsis;
    white-space: nowrap;
    font-size: 0.9rem;
  }

  .item-type {
    font-size: 0.7rem;
    color: rgb(150, 160, 180);
    background-color: rgb(50, 60, 80);
    padding: 3px 8px;
    border-radius: 4px;
    text-transform: uppercase;
    letter-spacing: 0.5px;
  }

  .item-type.command {
    background-color: rgba(77, 171, 247, 0.15);
    color: rgb(120, 190, 255);
  }

  .empty-hint {
    padding: 12px;
    color: rgb(120, 130, 150);
    font-size: 0.85rem;
    text-align: center;
    font-style: italic;
  }

  .footer {
    display: flex;
    justify-content: center;
    gap: 8px;
    padding-top: 8px;
    border-top: 1px solid rgb(60, 70, 90);
    font-size: 0.8rem;
    color: rgb(150, 160, 180);
  }

  .separator {
    opacity: 0.5;
  }
</style>
