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

  // export function convertInputsToStateData(object: { [key: string]: { currentValue: any, renderType?: string } }): StateData {

  // }
</script>

<script lang="ts">
  import SelectorTab from "./SelectorTab.svelte";
  import State from "./State.svelte";

  let { name, state, hash }: { name: string; state: { [key: string]: InputType }; hash: number } =
    $props();
</script>

<SelectorTab name={name + " " + hash} overflowHeight="20rem">
  {#each Object.entries(state) as [key, value]}
    <State {key} {value} path={hash + "." + key} />
  {/each}
</SelectorTab>
