<script lang="ts">
  import { schedulableStore } from "../../../lib/stores/schedulableStore"
  import { update } from "../../../lib/stores/windows"

  let { id }: { id: string } = $props()
  update(id, { maxW: 350 })

  let persistentExpanded = $state(true)
  let currentExpanded = $state(true)
</script>

<ul>
  <li>
    <button class="header" onclick={() => persistentExpanded = !persistentExpanded}>
      <h3>Persistent</h3>
      <p>{persistentExpanded ? "-" : "+"}</p>
    </button>
    <hr>
    <ul class="commands" class:hidden={!persistentExpanded}>
      <li>Command 1</li>
      <li>Command 2</li>
      <li>Command 3</li>
    </ul>
  </li>
  <li class="current">
    <button class="header" onclick={() => currentExpanded = !currentExpanded}>
    <h3>Current</h3>
      <p>{currentExpanded ? "-" : "+"}</p>
    </button>
    <hr>
    <ul class="commands" class:hidden={!currentExpanded}>
      {#each $schedulableStore as [_, schedulable]}
        <li>{schedulable.name}</li>
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
    padding: 10px;
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
</style>