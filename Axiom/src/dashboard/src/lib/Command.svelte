<script lang="ts" module>
    export type InputType = { value: string | number | boolean, readOnly: boolean } | { [key: string]: InputType }
    export class CommandData {
        name: string;
        state: { [key: string]: InputType };

        constructor(name: string, state: { [key: string]: InputType }) {
            this.name = name;
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
    import State from "./State.svelte";

    let { name, state }: { name: string, state: { [key: string]: InputType } } = $props();
</script>

<div class="flex w-full h-fit ring-2 ring-neutral-900 rounded-lg p-2 gap-4">
    <div class="flex flex-col">
        <h1 class="text-orange-200 text-lg font-medium select-none">
            {name}
        </h1>
    </div>

    <div class="flex flex-col mt-8 gap-2">
        {#each Object.entries(state) as [key, value]}
            <State key={key} value={value} />
        {/each}
    </div>
</div>