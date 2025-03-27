<script lang="ts">
    let { children, name, overflowHeight = "13rem", rightSide, rightSideColor = "rgb(253 186 116 / var(--tw-text-opacity, 1))"}: { children: any, name?: string, overflowHeight?: string, rightSide?: string, rightSideColor?: string } = $props();

    let isOpen = $state(false);

    console.log(rightSide)

    function toggleOpen() {
        isOpen = !isOpen;
    }
</script>

<div class="flex flex-col w-full">
    <button class="flex justify-between w-full p-2" onclick={toggleOpen}>
        <h3 class="text-orange-300 text-lg font-bold">{name}</h3>
        <div class="flex">
            {#if rightSide}
                <p class="text-lg" style="color: {rightSideColor}">{rightSide}</p>
            {/if}
            <p class="text-orange-300 text-lg">{isOpen ? '-' : '+'}</p>
        </div>
    </button>
    <div class="flex flex-col overflow-y-auto bg-neutral-900 rounded-lg p-2" class:hidden={!isOpen} style="max-height: {overflowHeight}">
        {@render children()}
    </div>
</div>