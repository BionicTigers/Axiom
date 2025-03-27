<script lang="ts">
  import Command, { CommandData, type InputType } from "./lib/components/Command.svelte";
  import Modal from "./lib/components/Modal.svelte";
  import System from "./lib/components/System.svelte";
  import Field from "./lib/components/Field.svelte";
  import Selector from "./lib/components/Selector.svelte";
  import { onMount } from "svelte";
  import StateSelector from "./lib/components/StateSelector.svelte";
  import Graph from "./lib/components/Graph.svelte";
  import Drivetrain from "./lib/components/Drivetrain.svelte";
  import LoadingGear from "./lib/components/LoadingGear.svelte";
  import { Websocket } from "./lib/components/Networking";
  import { get } from "svelte/store";
  import Gamepad, { GamepadData } from "./lib/components/Gamepad.svelte";

  type CommandJson = {
    name: string;
    state: { [key: string]: InputType };
    hash: number;
  };

  let drivetrainPowers: number[] | undefined = $state(undefined);

  let commands = $state<CommandData[]>([]);
  let teleops = $state<String[]>([]);
  let autos = $state<String[]>([]);

  let selectedOpMode = $state("NONE");
  // Selected, Init, Running
  let opModeState = $state("Selected");

  let telemetry = $state<String[]>([])

  Websocket.on("cycle", (data) => {
    drivetrainPowers = data.drivetrain;

    // Handle command updates
    let newCommands: CommandData[] = [];

    for (let command of data.commands as CommandJson[]) {
      newCommands.push(
        new CommandData(command.name, command.state, command.hash)
      );
    }

    commands = newCommands;
  });

  Websocket.on("opModeList", (data) => {
    teleops = data.teleops;
    autos = data.autos;
  });

  Websocket.on("opMode", (data) => {
    selectedOpMode = data.selected;
    opModeState = data.state;
  });

  onMount(() => {
    Websocket.create();
    let heartbeatInterval = setInterval(() => {
      if (get(Websocket.state) === Websocket.State.Connected) {
        Websocket.Functions.ping();
      }
    }, 500);
    return () => {
      Websocket.close();
      clearInterval(heartbeatInterval);
    };
  });

  let WebsocketState = Websocket.state;

  let gamepadData = $state(new GamepadData());

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
  let graphData = $state(new Map<string, Array<{ x: number; y: number }>>());
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
  <div class="flex flex-col items-center">
    <select
      class="bg-neutral-800/40 ring-2 text-orange-400 ring-neutral-900 rounded-lg p-2 w-1/5 mt-5 backdrop-blur-lg hover:bg-neutral-800/50 transition-colors"
      bind:value={selectedOpMode}
    >
      <optgroup label="Teleop">
        {#each teleops as teleop}
          <option value={teleop}>{teleop}</option>
        {/each}
      </optgroup>
      <optgroup label="Autos">
        {#each autos as auto}
          <option value={auto}>{auto}</option>
        {/each}
      </optgroup>
    </select>
	<div class="bg-neutral-800 h-72 w-96 rounded-xl flex flex-col">
		{#each telemetry as line}
			<p>line</p>
		{/each}
	</div>
    <button
      class="bg-neutral-800/40 ring-2 text-orange-400 ring-neutral-900 rounded-lg p-2 w-1/5 mt-5 backdrop-blur-lg hover:bg-neutral-800/50 transition-colors disabled:opacity-50 disabled:cursor-not-allowed"
      disabled={selectedOpMode === "NONE"}
      onclick={() => {
        if (selectedOpMode === "NONE") return;
        if (opModeState === "Selected") {
          opModeState = "Init";
          Websocket.Functions.opModeUpdate(selectedOpMode, "Init");
        } else if (opModeState === "Init") {
          opModeState = "Running";
          Websocket.Functions.opModeUpdate(selectedOpMode, "Start");
        } else if (opModeState === "Running") {
          opModeState = "Selected";
          Websocket.Functions.opModeUpdate(selectedOpMode, "Stop");
        }
      }}
    >
      {selectedOpMode === "NONE"
        ? "Select Op Mode"
        : opModeState === "Selected"
          ? "Init"
          : opModeState === "Init"
            ? "Start"
            : "Stop"}
    </button>
  </div>
</Modal>

<Modal enabled={graphModal} {closeModal}>
  <h1 class="text-orange-500 text-lg font-black text-center select-none">
    Graph
  </h1>

  <div class="flex w-full p-2 h-[40vh] min-h-[18.5rem]">
    <StateSelector bind:state={graphState} {commands} />
    <Graph
      {graphState}
      {commands}
      startTime={graphStartTime}
      isRunning={graphIsRunning}
      bind:graphData
    />
  </div>

  <div class="flex w-full justify-center gap-4">
    {#if !graphIsRunning}
      <button
        class="bg-neutral-800/40 ring-2 text-orange-400 ring-neutral-900 rounded-lg p-2 w-1/5 mt-5 backdrop-blur-lg hover:bg-neutral-800/50 transition-colors"
        onclick={() => {
          graphStartTime = Date.now();
          graphIsRunning = true;
          graphData = new Map();
        }}
      >
        Start
      </button>
    {:else}
      <button
        class="bg-neutral-800/40 ring-2 text-orange-400 ring-neutral-900 rounded-lg p-2 w-1/5 mt-5 backdrop-blur-lg hover:bg-neutral-800/50 transition-colors"
        onclick={() => {
          graphIsRunning = false;
        }}
      >
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
      <Selector
        options={["FIELD", "GAMEPAD", "DRIVETRAIN"]}
        bind:selected={leftSideSelected}
      />
    </div>
    {#if leftSideSelected === "FIELD"}
      <Field x={1000} y={1000} r={0} />
    {:else if leftSideSelected === "GAMEPAD"}
      <Gamepad bind:gamepad={gamepadData} />
    {:else if leftSideSelected === "DRIVETRAIN"}
      {#if drivetrainPowers}
        <Drivetrain powers={drivetrainPowers} />
      {:else}
        <div class="flex flex-col items-center gap-2">
          <LoadingGear />
          <p class="text-orange-200 text-lg select-none">
            Waiting for drivetrain data...
          </p>
          <p class="text-orange-200 text-sm select-none">
            Please send drivetrain powers using <code
              class="text-orange-400 font-medium">WebData</code
            >
          </p>
        </div>
      {/if}
    {/if}
  </div>
  <div
    class="flex flex-col w-full h-fit ring-2 ring-neutral-900 rounded-lg p-2"
  >
    <h1 class="text-orange-400 text-lg font-bold text-center select-none">
      SCHEDULER
    </h1>
    <div class="flex flex-col items-center justify-center w-full mt-3">
      {#if commands.length > 0}
        <System>
          {#each commands as command}
            <Command
              name={command.name}
              state={command.state}
              hash={command.hash}
            />
          {/each}
        </System>
      {:else}
        <div class="flex flex-col items-center gap-2 h-48">
          <LoadingGear />
          <p
            class="text-orange-200 text-lg font-medium select-none h-8 w-fit max-w-1/2"
          >
            No commands recieved
          </p>
        </div>
      {/if}
      {#if $WebsocketState === Websocket.State.Connecting}
        <p
          class="text-orange-200 text-lg font-medium select-none h-8 w-fit mt-5"
        >
          Websocket Disconnected
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
