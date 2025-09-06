<script lang="ts">
  import type { Component } from 'svelte'
  import { schedulableStore } from '../../../lib/stores/schedulableStore'
  import { add, bringToFront, update } from '../../../lib/stores/windows'
  import StateView from './StateView.svelte'

  let { id }: { id: string } = $props()
  update(id, { maxW: 350 })

  let persistentExpanded = $state(true)
  let currentExpanded = $state(true)

  function openCommandState(id: string) {
    let schedulable = $schedulableStore.get(id)
    if (!schedulable) return
    add({ id: id, title: schedulable.name, component: StateView as Component })
    bringToFront(schedulable.name)
  }

  let search = $state('')
  let store = $state($schedulableStore)

  let filteredStates = $derived(
    Array.from(store).filter(([_, schedulable]) => {
      if (search === '') return true
      if (schedulable.name.toLowerCase().includes(search.toLowerCase())) {
        return true
      }
      return false
    })
  )
</script>

<ul>
  <li class="search">
    <input type="text" placeholder="Search" bind:value={search} />
  </li>
  <li>
    <button class="header" onclick={() => (persistentExpanded = !persistentExpanded)}>
      <h3>Persistent</h3>
      <p>{persistentExpanded ? '-' : '+'}</p>
    </button>
    <hr />
    <ul class="commands" class:hidden={!persistentExpanded}>
      <!-- {#each $schedulableStore as [id, schedulable]}
        <li>
          <button onclick={() => openCommandState(id)}>{schedulable.name}</button>
        </li>
      {/each} -->
    </ul>
  </li>
  <li class="current">
    <button class="header" onclick={() => (currentExpanded = !currentExpanded)}>
      <h3>Current</h3>
      <p>{currentExpanded ? '-' : '+'}</p>
    </button>
    <hr />
    <ul class="commands" class:hidden={!currentExpanded}>
      {#each filteredStates as [id, schedulable] (id)}
        <li>
          <button onclick={() => openCommandState(id)}>{schedulable.name}</button>
        </li>
      {/each}
    </ul>
  </li>
</ul>

<style>
  ul {
    list-style: none;
    padding: 0;
  }

  hr {
    border: 1px solid rgb(100, 100, 100);
  }

  .current {
    margin-top: 10px;
  }

  .commands {
    display: flex;
    flex-direction: column;
    padding: 5px 0;
    gap: 5px;
  }

  .commands.hidden {
    display: none;
  }

  .commands li {
    background-color: rgb(33, 42, 60);
    border-radius: 10px;
    cursor: pointer;
    text-overflow: ellipsis;
    overflow: hidden;
    white-space: nowrap;
    max-width: 100%;
    transition: background-color 0.1s ease-in-out;
  }

  .commands li:hover {
    background-color: rgb(43, 52, 70);
  }

  .commands button {
    background-color: rgba(33, 42, 60, 0);
    padding: 15px;
    border-radius: 10px;
    cursor: pointer;
    text-overflow: ellipsis;
    overflow: hidden;
    white-space: nowrap;
    width: 100%;
    border: none;
    text-align: left;
    transition: background-color 0.1s ease-in-out;
    color: inherit;
  }

  .commands button:hover {
    background-color: rgb(43, 52, 70);
  }

  .header {
    display: flex;
    width: 100%;
    justify-content: space-between;
    align-items: center;
    background-color: transparent;
    border: none;
    cursor: pointer;
    padding: 0;
    padding-bottom: 3px;
    margin: 0;
    font-size: 1.5rem;
  }

  .header h3 {
    margin: 0;
    color: rgb(230, 233, 239);
    font-size: 1.2rem;
  }

  .header p {
    margin: 0;
    color: rgb(230, 233, 239);
    font-size: 1.2rem;
  }

  .search {
    border: none;
    background-color: transparent;
    width: 100%;
    margin-bottom: 10px;
    display: flex;
    justify-content: center;
  }

  .search input {
    border-radius: 10px;
    border: 1px solid rgb(33, 42, 60);
    background-color: rgb(33, 42, 60);
    color: rgb(230, 233, 239);
    font-size: 1rem;
    width: 80%;
    padding: 10px;
    text-align: center;
  }
</style>
