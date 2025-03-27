<script lang="ts">
    import Field from "../../assets/Field.svg";

    // x and y are in mm
    // r is in radians
    let { x, y, r }: { x: number, y: number, r: number } = $props();

    const fieldMM = 3657.6;
    const robotWidth = 450;  // Robot width in mm
    const robotLength = 450; // Robot length in mm
</script>

<div class="flex flex-col justify-center items-center w-full h-full lg:w-3/4 gap-2">
    <div class="relative w-full aspect-square">
        <img src={Field} alt="Field" class="w-full h-full rounded-lg object-contain" />
        <svg class="absolute inset-0 w-full h-full" viewBox={`0 0 ${fieldMM} ${fieldMM}`} style="transform: scale(1, -1)">
            <rect 
                x={x - robotWidth/2} 
                y={y - robotLength/2} 
                width={robotWidth} 
                height={robotLength}
                transform={`rotate(${-r * 180 / Math.PI}, ${x}, ${y})`}
                fill="rgb(251, 146, 60)"
                fill-opacity="0.3"
                stroke="rgb(251, 146, 60)"
                stroke-width="4"
            />
        </svg>
    </div>
    <p class="text-orange-400 text-sm select-none">
        <span class="font-bold text-orange-300">x:</span> {x} mm, <span class="font-bold text-orange-300">y:</span> {y} mm, <span class="font-bold text-orange-300">r:</span> {r * 180 / Math.PI}°
    </p>
</div>
