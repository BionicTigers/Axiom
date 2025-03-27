<script lang="ts">
    import { line, curveLinear, scaleLinear } from 'd3';
    import { onMount } from 'svelte';

    let { data = $bindable(new Map()) } = $props<{
        data: Map<string, Array<{x: number, y: number}>>
    }>();
    
    const colors = ['#F50057','#42A5F5','#26A69A','#9575CD', '#FF8A65', '#FFD54F', '#66BB6A', '#FF7043', '#7986CB', '#EF5350'];
    const marginTop = 20;
    const marginRight = 20;
    const marginBottom = 30;
    const marginLeft = 60;

    let container: HTMLDivElement;
    let width = $state(600);
    let height = $state(300);
    let tooltip = $state<{x: number, y: number, value: {x: number, y: number}, series: string} | null>(null);
    let hoveredPoint = $state<{point: {x: number, y: number}, series: string} | null>(null);

    function updateDimensions() {
        if (!container) return;
        const rect = container.getBoundingClientRect();
        width = rect.width;
        height = rect.height;
    }

    onMount(() => {
        updateDimensions();
        window.addEventListener('resize', updateDimensions);
        return () => window.removeEventListener('resize', updateDimensions);
    });

    $effect(() => {
        updateDimensions();
    });

    let allPoints = $derived([...data.values()].flat());
    let xRange = $derived([marginLeft, width - marginRight]);
    let yRange = $derived([height - marginBottom, marginTop]);

    let xScale = $derived(scaleLinear()
        .domain([
            Math.max(0, Math.max(...allPoints.map(d => d.x)) - 5000),
            Math.max(1000, Math.max(...allPoints.map(d => d.x)))
        ])
        .range(xRange));

    let yScale = $derived(scaleLinear()
        .domain([
            Math.min(0, Math.min(...allPoints.map(d => d.y))),
            Math.max(1, Math.max(...allPoints.map(d => d.y)) * 1.1)
        ])
        .range(yRange));

    let chartLine = $derived(line<{x: number, y: number}>()
        .x(d => xScale(d.x))
        .y(d => yScale(d.y))
        .curve(curveLinear));
    
    let xTicks = $derived(xScale.ticks(10));
    let yTicks = $derived(yScale.ticks(5));

    function findClosestPoint(mouseX: number, mouseY: number) {
        let closestPoint = null;
        let closestDistance = Infinity;
        let closestSeries = '';

        for (const [series, points] of data.entries()) {
            for (const point of points) {
                const dx = xScale(point.x) - mouseX;
                const dy = yScale(point.y) - mouseY;
                const distance = Math.sqrt(dx * dx + dy * dy);
                
                if (distance < closestDistance) {
                    closestDistance = distance;
                    closestPoint = point;
                    closestSeries = series;
                }
            }
        }

        return closestPoint && closestDistance < 50 ? { point: closestPoint, series: closestSeries } : null;
    }

    function handleMouseMove(e: MouseEvent) {
        if (!container) return;
        
        const rect = container.getBoundingClientRect();
        const mouseX = (e.clientX - rect.left) * (width / rect.width);
        const mouseY = (e.clientY - rect.top) * (height / rect.height);
        
        const closest = findClosestPoint(mouseX, mouseY);
        hoveredPoint = closest;
        
        if (closest) {
            tooltip = {
                x: e.clientX - rect.left,
                y: e.clientY - rect.top,
                value: closest.point,
                series: closest.series
            };
        } else {
            tooltip = null;
        }
    }

    function handleMouseLeave() {
        tooltip = null;
        hoveredPoint = null;
    }

    function clipNumber(value: number) {
        if (value > 1000000000) return (value / 1000000000) + 'B';
        if (value > 1000000) return (value / 1000000) + 'M';
        return value
    }
</script>

<div class="flex flex-col h-full w-full">
    <div class="chart-container flex-1 h-full w-full" bind:this={container} 
        onmousemove={handleMouseMove}
        onmouseleave={handleMouseLeave}
        aria-label="Line Plot"
        role="region">
        <svg viewBox="0 0 {width} {height}" preserveAspectRatio="xMidYMid meet">
            <defs>
                <clipPath id="chart-area">
                    <rect 
                        x={marginLeft} 
                        y={marginTop} 
                        width={width - marginLeft - marginRight} 
                        height={height - marginTop - marginBottom} 
                    />
                </clipPath>
            </defs>

            <!-- Y-axis and grid -->
            <g class="y-axis" transform="translate({marginLeft}, 0)">
                <path class="domain" stroke="currentColor" d="M0,{marginTop} V{height - marginBottom}" />
                {#each yTicks as tick}
                    <g class="tick" transform="translate(0,{yScale(tick)})">
                        <line stroke="currentColor" x2={width - marginLeft - marginRight} stroke-opacity="0.1" />
                        <text fill="currentColor" x="-10" dy="0.32em" text-anchor="end">{clipNumber(tick)}</text>
                    </g>
                {/each}
            </g>

            <!-- X-axis and grid -->
            <g class="x-axis" transform="translate(0,{height - marginBottom})">
                <path class="domain" stroke="currentColor" d="M{marginLeft},0H{width - marginRight}" />
                {#each xTicks as tick}
                    <g class="tick" transform="translate({xScale(tick)},0)">
                        <line stroke="currentColor" y2={-height + marginTop + marginBottom} stroke-opacity="0.1" />
                        <text fill="currentColor" y="20" text-anchor="middle">{clipNumber(tick)}</text>
                    </g>
                {/each}
            </g>

            <!-- Lines and Points -->
            <g clip-path="url(#chart-area)">
                {#each [...data.entries()] as [key, points], i}
                    <!-- Line -->
                    <path 
                        d={chartLine(points)}
                        fill="none"
                        stroke={colors[i % colors.length]}
                        stroke-width="2"
                        stroke-linecap="round"
                        stroke-linejoin="round"
                    />
                {/each}

                <!-- Vertical line at hovered point -->
                {#if hoveredPoint}
                    <line
                        x1={xScale(hoveredPoint.point.x)}
                        x2={xScale(hoveredPoint.point.x)}
                        y1={marginTop}
                        y2={height - marginBottom}
                        stroke="currentColor"
                        stroke-opacity="0.2"
                        stroke-dasharray="4,4"
                    />
                {/if}
            </g>
        </svg>

        {#if tooltip}
            <div 
                class="tooltip"
                style="left: {tooltip.x + 10}px; top: {tooltip.y - 10}px;"
            >
                <div class="font-medium">{tooltip.series}</div>
                <div>Time: {(tooltip.value.x / 1000).toFixed(2)}s</div>
                <div>Value: {tooltip.value.y.toFixed(3)}</div>
            </div>
        {/if}
    </div>

    <!-- Legend -->
    <div class="flex justify-center gap-4 mt-2 px-2 overflow-x-auto w-full">
        {#each [...data.entries()] as [key], i}
            <div class="flex items-center gap-2 w-fit">
                <div class="w-3 h-3 rounded-full" style="background-color: {colors[i % colors.length]}"></div>
                <span class="text-orange-300 text-sm">{key}</span>
            </div>
        {/each}
    </div>
</div>

<style lang="postcss">
    .chart-container {
        @apply w-full relative;
    }
    
    svg {
        @apply absolute inset-0 w-full h-full;
    }

    .tick text {
        @apply text-orange-300 text-xs;
    }

    .domain {
        @apply text-orange-300;
    }

    line {
        @apply text-orange-300;
    }

    .tooltip {
        @apply absolute z-10 bg-neutral-800/90 text-orange-300 px-3 py-2 rounded-lg text-sm pointer-events-none ring-1 ring-orange-500/20;
    }
</style>
