<script lang="ts">
    import { filterInternalValues, type CommandData } from "./Command.svelte";
    import SelectorTab from "../components/SelectorTab.svelte";
    import StateSelectorOption from "./StateSelectorOption.svelte";

    let { state = $bindable<Array<string>>([]), commands }: { state: Array<string>, commands: Array<CommandData> } = $props();
</script>

<div class="flex flex-col w-1/3 h-full p-2">
    <h1 class="text-orange-300 text-lg font-bold select-none text-center">
        State Selector
    </h1>

    <div class="flex flex-col items-center w-full mt-3 overflow-y-auto h-full pr-3">
        {#each commands.filter(command => Object.keys(command.state).length > 0) as command}
            <div class="flex flex-col items-center justify-center w-full">
                <SelectorTab name={command.name}>
                    {#each Object.entries(filterInternalValues(command.state)) as [key, value]}
                        <StateSelectorOption bind:state={state} key={key} value={value} path={command.name} />
                    {/each}
                </SelectorTab>
            </div>
        {/each}
    </div>
</div>