<script lang="ts">  
  import Command, { CommandData, type InputType } from "./lib/Command.svelte";
  import Modal from "./lib/Modal.svelte";
  import System from "./lib/System.svelte";
  import Field from "./assets/Field.svg";
  import Selector from "./lib/Selector.svelte";
  import { onMount } from "svelte";
  import StateSelector from "./lib/StateSelector.svelte";
  import Graph from "./lib/Graph.svelte";

  type CommandJson = {
    name: string
    state: { [key: string]: InputType }
  }

  let socket: WebSocket | undefined = $state(undefined);
  let commands = $state<CommandData[]>([]);

  function createWebsocket() {
    if (socket) socket.close()
    socket = new WebSocket("ws://localhost:10464/updates")
    socket.onmessage = (event) => {
      let data = JSON.parse(event.data)
      if (data.type === "cycle") {
        let newCommands: CommandData[] = [];
        for (let command of data.commands as CommandJson[]) {
          newCommands.push(new CommandData(command.name, command.state))
        }
        commands = newCommands
      }
    };
    socket.onclose = (event) => {
      createWebsocket()
    }
  }

  onMount(() => {
    createWebsocket()
    return () => {
      if (socket) socket.close()
    }
  })

  let isPaused = $state(false);
  let graphModal = $state(false);
  let opModeModal = $state(false);

  let leftSideSelected = $state("FIELD");

  let closeModal = () => {
    isPaused = false;
    graphModal = false;
    opModeModal = false;
  };

  let graphState = $state<Array<string>>([]);
  let graphStartTime = $state(0);
  let graphIsRunning = $state(false);
  let graphData = $state(new Map<string, Array<{x: number, y: number}>>());
</script>

<div
  class="bg-neutral-800/40 ring-2 ring-neutral-900 rounded-lg p-4 w-1/3 mx-auto mt-5 backdrop-blur-lg hover:bg-neutral-800/50 transition-colors"
>
  <h1 class="text-orange-500 text-lg font-black text-center select-none">
    AXIOM
  </h1>
</div>

<div class="flex w-full justify-center gap-4">
  <button
    class="bg-neutral-800/40 ring-2 text-orange-400 ring-neutral-900 rounded-lg p-2 w-1/5 mt-5 backdrop-blur-lg hover:bg-neutral-800/50 transition-colors"
    onclick={() => (opModeModal = !opModeModal)}
  >
    Op Mode
  </button>
  <button
    class="bg-neutral-800/40 ring-2 text-orange-400 ring-neutral-900 rounded-lg p-2 w-1/5 mt-5 backdrop-blur-lg hover:bg-neutral-800/50 transition-colors"
    onclick={() => (graphModal = !graphModal)}
  >
    Graph
  </button>
  <button
    class="bg-neutral-800/40 ring-2 text-orange-400 ring-neutral-900 rounded-lg p-2 w-1/5 mt-5 backdrop-blur-lg hover:bg-neutral-800/50 transition-colors"
    onclick={() => (isPaused = !isPaused)}
  >
    {isPaused ? "Resume" : "Pause"}
  </button>
</div>

<Modal enabled={opModeModal} {closeModal}>
  <h1 class="text-orange-500 text-lg font-black text-center select-none">
    Op Mode
  </h1>
</Modal>

<Modal enabled={graphModal} {closeModal}>
  <h1 class="text-orange-500 text-lg font-black text-center select-none">
    Graph
  </h1>

  <div class="flex w-full h-fit ring-2 ring-neutral-900 rounded-lg p-2">
    <StateSelector bind:state={graphState} commands={commands} />
    <Graph graphState={graphState} commands={commands} startTime={graphStartTime} isRunning={graphIsRunning} bind:graphData={graphData} />
  </div>

  <div class="flex w-full justify-center gap-4">
    {#if !graphIsRunning}
      <button class="bg-neutral-800/40 ring-2 text-orange-400 ring-neutral-900 rounded-lg p-2 w-1/5 mt-5 backdrop-blur-lg hover:bg-neutral-800/50 transition-colors" onclick={() => {
        graphStartTime = Date.now();
        graphIsRunning = true;
        graphData = new Map();
      }}>
        Start
      </button>
    {:else}
      <button class="bg-neutral-800/40 ring-2 text-orange-400 ring-neutral-900 rounded-lg p-2 w-1/5 mt-5 backdrop-blur-lg hover:bg-neutral-800/50 transition-colors" onclick={() => {
        graphIsRunning = false;
      }}>
        Stop
      </button>
    {/if}
    <!-- <input type="number" placeholder="time (ms)" value="1000" class="bg-neutral-800/40 ring-2 text-orange-400 ring-neutral-900 rounded-lg p-2 w-28 mt-5 backdrop-blur-lg hover:bg-neutral-800/50 transition-colors" /> -->
  </div>
</Modal>

<main class="flex justify-around h-fit mt-4 p-4 gap-4">
  <div
    class="flex flex-col w-full h-fit ring-2 ring-neutral-900 rounded-lg p-2 justify-center items-center"
  >
    <div class="flex items-center justify-center w-full mb-3">
      <Selector options={["FIELD", "GAMEPAD", "DRIVETRAIN"]} bind:selected={leftSideSelected} />
    </div>
    {#if leftSideSelected === "FIELD"}
      <img src={Field} alt="Field" class="w-full h-full rounded-lg lg:w-3/4" />
    {:else if leftSideSelected === "GAMEPAD"}
      <!-- <img src={Gamepad} alt="Gamepad" class="w-full h-full rounded-lg lg:w-3/4" /> -->
    {:else if leftSideSelected === "DRIVETRAIN"}
      <!-- <img src={Drivetrain} alt="Drivetrain" class="w-full h-full rounded-lg lg:w-3/4" /> -->
    {/if}
  </div>
  <div
    class="flex flex-col w-full h-fit ring-2 ring-neutral-900 rounded-lg p-2"
  >
    <h1 class="text-orange-400 text-lg font-bold text-center select-none">
      SCHEDULER
    </h1>
    <div class="flex flex-col items-center justify-center w-full mt-3">
      <System>
        {#each commands as command}
          <Command name={command.name} state={command.state} />
        {/each}
      </System>
      {#if socket == undefined || socket.CONNECTING}
        <p class="text-orange-200 text-lg font-medium select-none h-8 w-fit max-w-1/2">
          Attempting to connect...
        </p>
      {/if}
    </div>
  </div>
</main>

<style lang="postcss">
  :global(body) {
    @apply bg-black;

    font-family: "Inter", sans-serif;
  }
</style>
