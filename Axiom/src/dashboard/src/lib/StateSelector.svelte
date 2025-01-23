<script lang="ts">
    import type { CommandData } from "./Command.svelte";
    import StateSelectorOption from "./StateSelectorOption.svelte";

    let { state = $bindable<Array<string>>([]), commands }: { state: Array<string>, commands: Array<CommandData> } = $props();
</script>

<div class="flex flex-col w-1/3 h-fit ring-2 ring-neutral-900 rounded-lg p-2">
    <h1 class="text-orange-300 text-lg font-bold select-none text-center">
        State Selector
    </h1>

    <div class="flex flex-col items-center justify-center w-full mt-3">
        {#each commands as command}
            <div class="flex flex-col items-center justify-center w-full">
                <h3 class="text-orange-300 text-lg font-bold select-none text-center">
                    {command.name}
                </h3>
                {#each Object.entries(command.state) as [key, value]}
                    <StateSelectorOption bind:state={state} key={key} value={value} path={command.name} />
                {/each}
            </div>
        {/each}
    </div>
</div>