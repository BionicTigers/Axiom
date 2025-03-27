<script lang="ts" module>
  export type InputType =
    | { value: string | number | boolean; readOnly: boolean }
    | { [key: string]: InputType };
  export class CommandData {
    name: string;
    hash: number;
    state: { [key: string]: InputType };

    constructor(
      name: string,
      state: { [key: string]: InputType },
      hash: number
    ) {
      this.name = name;
      this.hash = hash;
      this.state = state;
    }
  }

  export interface StateData {
    name: string;
    value: any;
    render?: string;
  }

  //deltaTime
  export function filterInternalValues(state: { [key: string]: InputType }) {
    return Object.fromEntries(
      Object.entries(state).filter(([key, value]) => !key.startsWith("deltaTime"))
    );
  }
</script>

<script lang="ts">
  import SelectorTab from "../components/SelectorTab.svelte";
  import State from "../components/State.svelte";
  import { generateGradient, rgbToString } from "../utils/Color";

  let { name, state, hash }: { name: string; state: { [key: string]: InputType }; hash: number } = $props();

  let rightSide = $derived(state.deltaTime as unknown as number * 1000)
  let rightSideColor = $derived(rgbToString(generateGradient([{color: {r: 255, g: 0, b: 0}, position: 20}, {color: {r: 0, g: 255, b: 0}, position: 0}], rightSide)))
  let rightSideString = $derived((rightSide).toFixed(2) + " ms")
</script>

<SelectorTab {name} {rightSideColor} rightSide={rightSideString} overflowHeight="20rem">
  {#each Object.entries(filterInternalValues(state)) as [key, value]}
    <State {key} {value} path={hash + "." + key} />
  {/each}
</SelectorTab>
