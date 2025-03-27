<script lang="ts">
    import type { CommandData } from "./Command.svelte";
    import LinePlot from "./LinePlot.svelte";

    let { graphState, commands, startTime, isRunning, graphData = $bindable(new Map()) }: { graphState: Array<string>, commands: Array<CommandData>, startTime: number, isRunning: boolean, graphData: Map<string, Array<{x: number, y: number}>> } = $props();

    function getValueFromPath(path: string): number | undefined {
        let pathSplit = path.split(".");
        let command = commands.find(c => c.name == pathSplit[0]);
        let value = command?.state[pathSplit[1]];

        for (let i = 2; i < pathSplit.length; i++) {
            if (value?.value == undefined) {
                value = value!![pathSplit[i] as keyof typeof value];
            } else {
                value = value.value[pathSplit[i] as keyof typeof value.value];
            }
        }

        return value?.value as number | undefined;
    }

    let graphVariables = $derived(graphState.map(s => getValueFromPath(s)));
    // svelte-ignore state_referenced_locally
    let lastGraphVariables = graphVariables;

    $effect(() => {
        if (lastGraphVariables == graphVariables || !isRunning) return;
        lastGraphVariables = graphVariables;

        graphVariables.forEach((value, index) => {
            if (value == undefined) return;

            let data = graphData.get(graphState[index]);
            if (data == undefined) {
                data = [];
            }

            data.push({x: Date.now() - startTime, y: value});
            graphData.set(graphState[index], data);
            graphData = new Map(graphData);
        })
    })
</script>

<div class="flex flex-col w-full h-full p-2">
    <h1 class="text-orange-300 text-lg font-bold select-none text-center">
        Graph
    </h1>
    
    <div class="w-full h-full bg-neutral-800/40 mt-3 rounded-lg p-2 backdrop-blur-lg">
        <LinePlot bind:data={graphData} />
    </div>
</div>
