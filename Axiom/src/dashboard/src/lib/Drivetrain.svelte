<script lang="ts">
    let { powers }: { powers: number[] } = $props();

    interface ChassisVelocity {
        vx: number;    // Forward velocity
        vy: number;    // Strafe velocity
        omega: number; // Angular velocity
    }

    function mecanumVelocityKinematics(powers: number[]): ChassisVelocity {
        // Assuming powers are in order: front left, front right, back left, back right
        // And normalized between -1 and 1
        const [fl, fr, bl, br] = powers;
        
        // Mecanum inverse kinematics matrix
        // [vx]   = 1/4 * [ 1  1  1  1] * [fl]
        // [vy]          [ 1 -1 -1  1] * [fr]
        // [ω ]          [-1  1 -1  1] * [bl]
        //                              [br]
        
        const vx = (fl + fr + bl + br) / 4.0;
        const vy = (fl - fr - bl + br) / 4.0;
        const omega = (-fl + fr - bl + br) / 4.0;

        return { vx, vy, omega };
    }

    let velocity = $derived(mecanumVelocityKinematics(powers));

    // Calculate the resultant velocity vector
    let resultantMagnitude = $derived(Math.sqrt(velocity.vx * velocity.vx + velocity.vy * velocity.vy));
    let resultantAngle = $derived(Math.atan2(-velocity.vx, velocity.vy));

    // Calculate rotation arc parameters
    function getRotationArc(omega: number) {
        const radius = 12;
        const maxAngle = Math.PI; // 180 degrees max sweep
        const sweepAngle = Math.min(Math.abs(omega), 1) * maxAngle;
        const startAngle = -sweepAngle / 2;
        const endAngle = sweepAngle / 2;
        
        const startX = 50 + radius * Math.sin(startAngle);
        const startY = 50 - radius * Math.cos(startAngle);
        const endX = 50 + radius * Math.sin(endAngle);
        const endY = 50 - radius * Math.cos(endAngle);

        return `M ${startX} ${startY} A ${radius} ${radius} 0 ${sweepAngle > Math.PI ? 1 : 0} ${omega < 0 ? 1 : 0} ${endX} ${endY}`;
    }

    const DEADZONE = 0.1; // 10% deadzone
    const ARROW_SCALE = 25; // Scale factor for arrow length
</script>

<div class="flex flex-col items-center gap-4">
    <div class="flex relative justify-center items-center w-52 h-52">
        <svg class="w-[100%] h-[100%] absolute top-1/2 left-1/2 transform -translate-x-1/2 -translate-y-1/2" viewBox="0 0 100 100">
            <!-- Background chassis shape -->
            <line x1="20" y1="10" x2="20" y2="90" stroke="rgb(55, 55, 55)" stroke-width="4" />
            <line x1="80" y1="10" x2="80" y2="90" stroke="rgb(55, 55, 55)" stroke-width="4" />
            <line x1="20" y1="50" x2="80" y2="50" stroke="rgb(55, 55, 55)" stroke-width="4" />
            
            <!-- Power indicator lines -->
            <!-- Top left -->
            <line x1="10" y1="5" x2="10" y2="35" stroke="rgb(40, 40, 40)" stroke-width="3" />
            <line x1="10" y1="20" x2="10" y2="{20 - powers[0] * 15}" stroke="rgb(251, 146, 60)" stroke-width="3" />
            
            <!-- Top right -->
            <line x1="90" y1="5" x2="90" y2="35" stroke="rgb(40, 40, 40)" stroke-width="3" />
            <line x1="90" y1="20" x2="90" y2="{20 - powers[1] * 15}" stroke="rgb(251, 146, 60)" stroke-width="3" />
            
            <!-- Bottom left -->
            <line x1="10" y1="65" x2="10" y2="95" stroke="rgb(40, 40, 40)" stroke-width="3" />
            <line x1="10" y1="80" x2="10" y2="{80 - powers[2] * 15}" stroke="rgb(251, 146, 60)" stroke-width="3" />
            
            <!-- Bottom right -->
            <line x1="90" y1="65" x2="90" y2="95" stroke="rgb(40, 40, 40)" stroke-width="3" />
            <line x1="90" y1="80" x2="90" y2="{80 - powers[3] * 15}" stroke="rgb(251, 146, 60)" stroke-width="3" />

            <!-- Velocity indicators -->
            {#if velocity}
                <!-- Translational velocity vector -->
                {#if resultantMagnitude > DEADZONE}
                    <line 
                        x1="50" y1="50" 
                        x2="{50 + Math.cos(resultantAngle) * Math.max(0, resultantMagnitude - DEADZONE) * ARROW_SCALE}" 
                        y2="{50 + Math.sin(resultantAngle) * Math.max(0, resultantMagnitude - DEADZONE) * ARROW_SCALE}" 
                        stroke="rgb(251, 146, 60)" 
                        stroke-width="2"
                        marker-end="url(#translationArrow)"
                        class="transition-all duration-150"
                    />
                {/if}

                <!-- Rotation indicator -->
                {#if Math.abs(velocity.omega) > DEADZONE}
                    <g class="transition-all duration-150" style="opacity: {Math.min(Math.abs(velocity.omega), 1)}">
                        <path 
                            d={getRotationArc(velocity.omega)}
                            fill="none"
                            stroke="rgb(251, 146, 60)"
                            stroke-width="2"
                            marker-end="url(#rotationArrow)"
                        />
                    </g>
                {/if}
            {/if}

            <!-- Arrow marker definitions -->
            <defs>
                <!-- Translation arrow -->
                <marker
                    id="translationArrow"
                    markerWidth="5"
                    markerHeight="5"
                    refX="1"
                    refY="2.5"
                    orient="auto">
                    <path 
                        d="M0,0 L5,2.5 L0,5 L1,2.5 Z"
                        fill="rgb(251, 146, 60)"
                    />
                </marker>

                <!-- Rotation arrow -->
                <marker
                    id="rotationArrow"
                    markerWidth="5"
                    markerHeight="5"
                    refX="1"
                    refY="2.5"
                    orient="auto">
                    <path 
                        d="M0,0 L5,2.5 L0,5 L1,2.5 Z"
                        fill="rgb(251, 146, 60)"
                    />
                </marker>
            </defs>
        </svg>
    </div>

    <div class="text-orange-300 text-sm space-y-1">
        <div>Forward: {velocity.vx.toFixed(2)}</div>
        <div>Strafe: {velocity.vy.toFixed(2)}</div>
        <div>Rotation: {velocity.omega.toFixed(2)}</div>
    </div>
</div>