<script lang="ts" module>
    export class GamepadData {
        leftStick: {
            x: number;
            y: number;
        } = { x: 0, y: 0 };
        rightStick: {
            x: number;
            y: number;
        } = { x: 0, y: 0 };
        buttons: {
            triangle: boolean;
            circle: boolean;
            cross: boolean;
            square: boolean;
        } = { triangle: false, circle: false, cross: false, square: false };
        dpad: {
            up: boolean;
            down: boolean;
            left: boolean;
            right: boolean;
        } = { up: false, down: false, left: false, right: false };
        triggers: {
            l2: number;
            r2: number;
        } = { l2: 0, r2: 0 };
        bumpers: {
            l1: boolean;
            r1: boolean;
        } = { l1: false, r1: false };
        ps: boolean = false;
    }
</script>

<script lang="ts">
    import GamepadSVG from "../../assets/Gamepad.svg";
    import { onMount } from 'svelte';

    let { gamepad = $bindable(new GamepadData()) }: { gamepad: GamepadData } = $props();

    // Calculate positions for stick overlays
    let leftStickX = $derived(230 + gamepad.leftStick.x * 20);
    let leftStickY = $derived(170 - gamepad.leftStick.y * 20);
    let rightStickX = $derived(570 + gamepad.rightStick.x * 20);
    let rightStickY = $derived(170 - gamepad.rightStick.y * 20);

    // Controller element that is currently being interacted with
    type ControllerElement = 
        | 'leftStick' | 'rightStick'
        | 'dpadUp' | 'dpadDown' | 'dpadLeft' | 'dpadRight'
        | 'triangle' | 'circle' | 'cross' | 'square'
        | 'l1' | 'r1' | 'l2' | 'r2'
        | 'ps'
        | null;
    
    let activeElement = $state<ControllerElement>(null);
    let container: HTMLElement;
    let containerRect = $state<DOMRect | null>(null);

    let leftStickElement: SVGCircleElement;
    let rightStickElement: SVGCircleElement;
    let dpadUpElement: SVGPathElement;
    let dpadDownElement: SVGPathElement;
    let dpadLeftElement: SVGPathElement;
    let dpadRightElement: SVGPathElement;
    let triangleElement: SVGCircleElement;
    let circleElement: SVGCircleElement;
    let crossElement: SVGCircleElement;
    let squareElement: SVGCircleElement;
    let l1Element: SVGPathElement;
    let r1Element: SVGPathElement;
    let l2Element: SVGPathElement;
    let r2Element: SVGPathElement;
    let psElement: SVGPathElement;

    // Register interactive areas for mouse input
    const interactiveAreas = {
        leftStick: { centerX: 230, centerY: 170, radius: 20 },
        rightStick: { centerX: 570, centerY: 170, radius: 20 },
        dpadUp: { x: 225, y: 220, width: 10, height: 20 },
        dpadDown: { x: 225, y: 240, width: 10, height: 20 },
        dpadLeft: { x: 210, y: 235, width: 20, height: 10 },
        dpadRight: { x: 230, y: 235, width: 20, height: 10 },
        triangle: { centerX: 570, centerY: 220, radius: 10 },
        circle: { centerX: 590, centerY: 240, radius: 10 },
        cross: { centerX: 570, centerY: 260, radius: 10 },
        square: { centerX: 550, centerY: 240, radius: 10 },
        l1: { points: "180,75 200,70 220,65 240,65 235,80 235,90 240,95 220,95 200,95 180,95 170,90 170,80" },
        r1: { points: "620,75 600,70 580,65 560,65 565,80 565,90 560,95 580,95 600,95 620,95 630,90 630,80" },
        l2: { points: "180,55 200,50 220,45 240,45 245,60 245,65 240,70 220,70 200,70 180,70 175,65 175,60" },
        r2: { points: "620,55 600,50 580,45 560,45 555,60 555,65 560,70 580,70 600,70 620,70 625,65 625,60" },
        ps: { centerX: 400, centerY: 190, radius: 12 }
    };

    // Handle mouse interactions
    function handleMouseDown(event: MouseEvent): void {
        if (!containerRect) return;
        
        const rect = containerRect;
        const scaleX = 800 / rect.width;
        const scaleY = 400 / rect.height;
        
        const x = (event.clientX - rect.left) * scaleX;
        const y = (event.clientY - rect.top) * scaleY;
        
        // Detect which element was clicked
        activeElement = getClickedElement(x, y);
        if (activeElement) {
            event.preventDefault();
            updateControllerState(x, y);
        }
    }

    function handleMouseMove(event: MouseEvent): void {
        if (!activeElement || !containerRect) return;
        
        event.preventDefault();
        
        const rect = containerRect;
        const scaleX = 800 / rect.width;
        const scaleY = 400 / rect.height;
        
        const x = (event.clientX - rect.left) * scaleX;
        const y = (event.clientY - rect.top) * scaleY;
        
        updateControllerState(x, y);
    }

    function handleMouseUp(): void {
        if (!activeElement) return;

        // Reset specific elements
        if (activeElement === 'leftStick' || activeElement === 'rightStick') {
            // Reset sticks to center
            if (activeElement === 'leftStick') {
                gamepad.leftStick.x = 0;
                gamepad.leftStick.y = 0;
            } else {
                gamepad.rightStick.x = 0;
                gamepad.rightStick.y = 0;
            }
        } else if (activeElement.startsWith('dpad')) {
            // Reset D-pad
            switch (activeElement) {
                case 'dpadUp': gamepad.dpad.up = false; break;
                case 'dpadDown': gamepad.dpad.down = false; break;
                case 'dpadLeft': gamepad.dpad.left = false; break;
                case 'dpadRight': gamepad.dpad.right = false; break;
            }
        } else if (['triangle', 'circle', 'cross', 'square'].includes(activeElement)) {
            // Reset face buttons
            gamepad.buttons[activeElement] = false;
        } else if (activeElement === 'l1' || activeElement === 'r1') {
            // Reset bumpers
            if (activeElement === 'l1') gamepad.bumpers.l1 = false;
            else gamepad.bumpers.r1 = false;
        } else if (activeElement === 'ps') {
            gamepad.ps = false;
        }
        // Triggers (l2, r2) don't reset to allow continuous adjustment

        activeElement = null;
    }

    function getClickedElement(x: number, y: number): ControllerElement {
        // Check each interactive area
        for (const [element, area] of Object.entries(interactiveAreas)) {
            if ('centerX' in area) {
                // Circle check
                const distanceSquared = Math.pow(x - area.centerX, 2) + Math.pow(y - area.centerY, 2);
                if (distanceSquared <= Math.pow(area.radius, 2)) {
                    return element as ControllerElement;
                }
            } else if ('width' in area) {
                // Rectangle check
                if (x >= area.x && x <= area.x + area.width && 
                    y >= area.y && y <= area.y + area.height) {
                    return element as ControllerElement;
                }
            } else if ('points' in area) {
                // Polygon path - simplified with bounding box for performance
                // We'd need a more complex point-in-polygon test for better precision
                // This is just an approximation for the bumpers/triggers
                if ((element === 'l1' || element === 'l2') && x >= 170 && x <= 240 && 
                    y >= (element === 'l1' ? 65 : 45) && y <= (element === 'l1' ? 95 : 70)) {
                    return element as ControllerElement;
                }
                if ((element === 'r1' || element === 'r2') && x >= 560 && x <= 630 && 
                    y >= (element === 'r1' ? 65 : 45) && y <= (element === 'r1' ? 95 : 70)) {
                    return element as ControllerElement;
                }
            }
        }
        return null;
    }

    function updateControllerState(x: number, y: number): void {
        if (!activeElement) return;
        
        switch (activeElement) {
            case 'leftStick':
                const leftDeltaX = (x - interactiveAreas.leftStick.centerX) / 20;
                const leftDeltaY = (interactiveAreas.leftStick.centerY - y) / 20;
                applyStickConstraints(leftDeltaX, leftDeltaY, 'leftStick');
                break;
            
            case 'rightStick':
                const rightDeltaX = (x - interactiveAreas.rightStick.centerX) / 20;
                const rightDeltaY = (interactiveAreas.rightStick.centerY - y) / 20;
                applyStickConstraints(rightDeltaX, rightDeltaY, 'rightStick');
                break;
            
            case 'dpadUp': gamepad.dpad.up = true; break;
            case 'dpadDown': gamepad.dpad.down = true; break;
            case 'dpadLeft': gamepad.dpad.left = true; break;
            case 'dpadRight': gamepad.dpad.right = true; break;
            
            case 'triangle': gamepad.buttons.triangle = true; break;
            case 'circle': gamepad.buttons.circle = true; break;
            case 'cross': gamepad.buttons.cross = true; break;
            case 'square': gamepad.buttons.square = true; break;
            
            case 'l1': gamepad.bumpers.l1 = true; break;
            case 'r1': gamepad.bumpers.r1 = true; break;
            
            case 'l2': 
                // Vertical position sensitivity for trigger
                gamepad.triggers.l2 = Math.min(1, Math.max(0, 1 - (y - 45) / 25));
                break;
            
            case 'r2':
                gamepad.triggers.r2 = Math.min(1, Math.max(0, 1 - (y - 45) / 25));
                break;
            
            case 'ps': gamepad.ps = true; break;
        }
    }

    function applyStickConstraints(deltaX: number, deltaY: number, stick: 'leftStick' | 'rightStick'): void {
        // Apply circular constraint to joystick
        const distance = Math.sqrt(deltaX * deltaX + deltaY * deltaY);
        const maxDistance = 1;
        
        if (distance <= maxDistance) {
            // Within bounds
            if (stick === 'leftStick') {
                gamepad.leftStick.x = deltaX;
                gamepad.leftStick.y = deltaY;
            } else {
                gamepad.rightStick.x = deltaX;
                gamepad.rightStick.y = deltaY;
            }
        } else {
            // Outside bounds, normalize to edge of circle
            const normalizeFactor = maxDistance / distance;
            if (stick === 'leftStick') {
                gamepad.leftStick.x = deltaX * normalizeFactor;
                gamepad.leftStick.y = deltaY * normalizeFactor;
            } else {
                gamepad.rightStick.x = deltaX * normalizeFactor;
                gamepad.rightStick.y = deltaY * normalizeFactor;
            }
        }
    }

    onMount(() => {
        // Set up container dimensions for coordinate calculations
        if (container) {
            containerRect = container.getBoundingClientRect();
            
            // Add global mouse handlers
            window.addEventListener('mousemove', handleMouseMove);
            window.addEventListener('mouseup', handleMouseUp);
            
            return () => {
                window.removeEventListener('mousemove', handleMouseMove);
                window.removeEventListener('mouseup', handleMouseUp);
            };
        }
    });
</script>

<div class="flex flex-col items-center justify-center w-full h-full">
    <h2 class="text-orange-300 text-lg font-bold mb-3">PS5 Controller</h2>
    
    <div class="relative w-full max-w-3xl aspect-[2/1]" bind:this={container} onmousedown={handleMouseDown} role="button" tabindex="0">
        <!-- Base Controller SVG -->
        <svg width="353" height="235" viewBox="0 0 706 470" fill="none" xmlns="http://www.w3.org/2000/svg">
            <path d="M48.1939 458.265L48.3078 458.338L48.4329 458.389L49.0001 457C48.4329 458.389 48.4332 458.389 48.4337 458.389L48.4353 458.39L48.4409 458.392L48.4619 458.4L48.5416 458.433C48.6114 458.461 48.7142 458.502 48.8482 458.555C49.1162 458.662 49.5092 458.816 50.013 459.01C51.0206 459.398 52.4723 459.944 54.2555 460.577C57.8198 461.844 62.7197 463.467 68.0516 464.887C73.376 466.305 79.1729 467.531 84.5211 467.987C89.8193 468.438 94.9032 468.156 98.6508 466.352C100.668 465.38 102.758 463.226 104.881 460.382C107.041 457.489 109.373 453.691 111.832 449.19C116.751 440.186 122.251 428.233 127.971 414.771C139.412 387.845 151.795 354.735 162.254 326.766L162.261 326.747L162.812 325.275C173.561 312.753 182.631 305.491 193.104 302.451C203.645 299.392 215.867 300.534 233.089 305.443L233.29 305.5H233.5H469.5H469.743L469.973 305.423C484.622 300.555 495.608 298.562 506.101 301.181C516.552 303.789 526.785 311.043 539.725 325.327C548.173 344.507 556.193 367.399 563.802 389.119C567.04 398.364 570.205 407.397 573.296 415.841C578.471 429.977 583.45 442.482 588.221 451.466C590.603 455.953 592.974 459.641 595.342 462.227C597.676 464.775 600.235 466.5 603 466.5C611.682 466.5 622.448 464.465 630.969 462.46C635.245 461.454 638.985 460.448 641.658 459.694C642.994 459.316 644.064 459.002 644.801 458.781C645.17 458.67 645.456 458.583 645.65 458.524C645.747 458.494 645.821 458.471 645.871 458.456L645.881 458.452L645.891 458.449L645.935 458.439C646.001 458.423 646.097 458.4 646.22 458.369C646.467 458.308 646.824 458.215 647.28 458.086C648.19 457.83 649.492 457.433 651.082 456.866C654.26 455.732 658.6 453.913 663.261 451.167C672.577 445.68 683.264 436.436 688.418 421.489C693.473 406.828 697.238 383.939 699.737 364.819C700.99 355.238 701.929 346.566 702.555 340.29C702.868 337.151 703.103 334.611 703.26 332.854C703.338 331.976 703.397 331.293 703.436 330.83C703.456 330.598 703.47 330.421 703.48 330.302L703.491 330.167L703.494 330.133L703.495 330.124L703.495 330.121C703.495 330.121 703.495 330.121 702 330L703.495 330.121L703.505 329.995L703.494 329.869C693.403 215.033 678.827 150.793 629.878 36.9076L629.567 36.1828L628.793 36.0288L523.293 15.0288L522.842 14.939L522.418 15.1175L513.324 18.9466C417.96 2.34933 324.84 -4.68879 190.209 18.9403L182.139 15.1427L181.693 14.933L181.21 15.0283L74.7099 36.0283L73.9902 36.1702L73.6603 36.8253C11.2707 160.725 -0.64136 257.775 1.99975 330.019C1.59611 365.152 1.80894 385.068 14.5846 421.496L14.6223 421.604L14.6757 421.704C19.5047 430.784 23.8114 437.349 28.976 442.946C34.142 448.544 40.1116 453.114 48.1939 458.265Z" fill="url(#paint0_linear_4_4)" stroke-width="3"/>
            <g id="leftStick">
                <circle cx="238.5" cy="229.5" r="51" stroke="#141414" stroke-width="11"/>
                <circle cx="238.5" cy="229.5" r="23" fill="#141414" bind:this={leftStickElement}/>
            </g>
            <g id="rightStick"> 
                <circle cx="461.5" cy="229.5" r="51" stroke="#141414" stroke-width="11"/>
                <circle cx="461.5" cy="229.5" r="23" fill="#141414" bind:this={rightStickElement}/>
            </g>
            <g id="cross">
                <circle cx="568" cy="185" r="23" fill="#141414" bind:this={crossElement}/>
                <path d="M557 196.5L579.5 174M557 174L579.5 196.5" stroke="#D9D9D9" stroke-width="3" stroke-linecap="round"/>
            </g>
            <g id="circle">
                <circle cx="620" cy="134" r="23" fill="#141414" bind:this={circleElement}/>
                <circle cx="620" cy="134" r="13" stroke="#D9D9D9" stroke-width="3"/>
            </g>
            <g id="triangle">
                <circle cx="568" cy="82" r="23" fill="#141414" bind:this={triangleElement}/>
                <path d="M567.139 69.457L555.892 88.4913C555.498 89.1579 555.978 90 556.752 90H579.248C580.022 90 580.502 89.1579 580.108 88.4913L568.861 69.457C568.474 68.8019 567.526 68.8019 567.139 69.457Z" stroke="#D9D9D9" stroke-width="3"/>
            <g id="square">
                <circle cx="517" cy="134" r="23" fill="#141414"/>
                <path d="M506 143.5V124C506 123.448 506.448 123 507 123H527C527.552 123 528 123.448 528 124V143.5C528 144.052 527.552 144.5 527 144.5H507C506.448 144.5 506 144.052 506 143.5Z" stroke="#D9D9D9" stroke-width="3"/>
            </g>
            <path d="M233 135.5L212.5 31.5C213.405 18.8277 219.509 16.4781 233 14.5C328.397 4.04362 379.932 4.49449 469 14.5C481.631 15.9708 488.951 15.6469 490.5 29.5L468 134.5C458.704 150.883 450.428 155.134 431.5 156L268 157C248.914 156.415 241.979 150.658 233 135.5Z" fill="#141414" bind:this={psElement}/>
            <path d="M513.845 72.337C511.168 73.1069 508.809 72.4788 506.894 70.4731C504.283 67.7373 504.414 63.5355 505.193 59.8344L507.174 50.4235C507.719 47.8336 508.637 45.2161 510.674 43.5259C514.403 40.4313 518.839 40.5291 521.778 44.3721C523.833 47.0604 523.61 50.7318 522.868 54.0332L520.952 62.5486C520.009 66.738 517.972 71.15 513.845 72.337Z" fill="#141414" bind:this={optionsElement}/>
            <path d="M187.782 72.8452C190.464 73.3316 192.866 72.439 194.786 70.2698C197.179 67.5664 197.155 63.6254 196.501 60.075L194.774 50.7029C194.264 47.9322 193.328 45.1096 191.121 43.359C187.294 40.3246 182.776 40.6165 179.899 44.8197C178.187 47.3209 178.265 50.5525 178.775 53.5401L180.289 62.4056C181.072 66.9913 183.204 72.0149 187.782 72.8452Z" fill="#141414" bind:this={shareElement}/>
            <g id="dpadLeft">
                <path d="M157.163 147.912C153.574 145.171 144 137.476 144 133.413C144 129.389 153.395 121.8 157.06 118.993C158.001 118.273 159.117 117.838 160.292 117.694L180.554 115.213C182.137 115.019 183.775 115.328 185.027 116.314C187.754 118.461 188.491 120.491 189 123.913C189.591 130.943 189.51 134.884 189 141.913C188.821 145.381 187.678 147.353 184.964 149.776C183.733 150.876 182.096 151.394 180.447 151.307C172.457 150.888 163.708 149.687 160.146 149.163C159.059 149.003 158.036 148.579 157.163 147.912Z" fill="#141414" bind:this={dpadRightElement}/>
                <path d="M173 129.801V138.394C173 139.311 174.13 139.744 174.743 139.063L178.426 134.971C178.756 134.604 178.769 134.051 178.457 133.669L174.774 129.168C174.179 128.441 173 128.862 173 129.801Z" fill="#D9D9D9"/>
            </g>
            <g id="dpadDown">
                <path d="M118.501 157.163C121.243 153.574 128.937 144 133 144C137.025 144 144.613 153.395 147.42 157.06C148.141 158.001 148.576 159.117 148.72 160.292L151.201 180.554C151.394 182.137 151.086 183.775 150.099 185.027C147.952 187.754 145.922 188.491 142.5 189C135.47 189.591 131.53 189.51 124.5 189C121.032 188.821 119.061 187.678 116.637 184.964C115.538 183.733 115.02 182.096 115.106 180.447C115.525 172.457 116.726 163.708 117.25 160.146C117.41 159.059 117.835 158.036 118.501 157.163Z" fill="#141414"/>
                <path d="M138.043 173H128.745C127.812 173 127.387 174.165 128.102 174.766L132.529 178.485C132.888 178.786 133.407 178.798 133.78 178.514L138.65 174.795C139.41 174.214 138.999 173 138.043 173Z" fill="#D9D9D9"/>
            </g>
            
            <path d="M109.251 119.501C112.84 122.243 122.414 129.937 122.414 134C122.414 138.025 113.018 145.613 109.353 148.42C108.413 149.141 107.297 149.576 106.121 149.72L85.8592 152.201C84.2763 152.394 82.6389 152.086 81.386 151.099C78.6599 148.952 77.9224 146.922 77.4135 143.5C76.8225 136.47 76.9033 132.53 77.4135 125.5C77.5929 122.032 78.7353 120.061 81.4491 117.637C82.6804 116.538 84.3178 116.02 85.9663 116.106C93.9569 116.525 102.706 117.726 106.268 118.25C107.355 118.41 108.378 118.835 109.251 119.501Z" fill="#141414"/>
            <path d="M94 138.199V129.606C94 128.689 92.8698 128.256 92.2567 128.937L88.5741 133.029C88.2439 133.396 88.2308 133.949 88.5435 134.331L92.226 138.832C92.8211 139.559 94 139.138 94 138.199Z" fill="#D9D9D9"/>
            <path d="M147.912 109.251C145.171 112.84 137.476 122.414 133.413 122.414C129.389 122.414 121.8 113.018 118.993 109.353C118.273 108.413 117.838 107.297 117.694 106.121L115.213 85.8592C115.019 84.2764 115.328 82.6389 116.314 81.386C118.461 78.6599 120.491 77.9224 123.913 77.4135C130.943 76.8225 134.884 76.9034 141.913 77.4135C145.381 77.593 147.353 78.7353 149.776 81.4491C150.876 82.6804 151.394 84.3178 151.307 85.9664C150.888 93.9569 149.687 102.706 149.163 106.268C149.003 107.355 148.579 108.378 147.912 109.251Z" fill="#141414"/>
            <path d="M128.801 93H137.394C138.311 93 138.744 91.8698 138.063 91.2567L133.971 87.5741C133.604 87.2439 133.051 87.2308 132.669 87.5435L128.168 91.226C127.441 91.8211 127.862 93 128.801 93Z" fill="#D9D9D9"/>
            <path d="M614.326 24.1559L615.5 35L521 16.6428L522.658 7.35708C523.166 4.51492 525.37 2.2586 528.219 1.79276C561.934 -3.7194 589.75 3.85565 611.555 19.3527C613.131 20.473 614.118 22.2334 614.326 24.1559Z" fill="#141414"/>
            <path d="M87.1738 24.1559L86 35L180.5 16.6428L178.842 7.35708C178.334 4.51492 176.13 2.2586 173.281 1.79276C139.566 -3.7194 111.75 3.85565 89.9455 19.3527C88.3693 20.473 87.3819 22.2334 87.1738 24.1559Z" fill="#141414"/>
            <defs>
                <linearGradient id="paint0_linear_4_4" x1="352.582" y1="5.24463" x2="352.582" y2="466.676" gradientUnits="userSpaceOnUse">
                    <stop stop-color="#3E3E3E"/>
                    <stop offset="1" stop-color="#2A2A2A"/>
                </linearGradient>
            </defs>
        </svg>
    </div>
    
    <!-- Controller Values Display -->
    <div class="mt-4 text-orange-300 text-sm grid grid-cols-2 gap-6 w-full max-w-2xl">
        <div>
            <div class="bg-neutral-800/60 rounded p-2 mb-2">
                <h3 class="font-bold border-b border-orange-500/30 pb-1 mb-1">Sticks</h3>
                <div>Left: X: {gamepad.leftStick.x.toFixed(2)}, Y: {gamepad.leftStick.y.toFixed(2)}</div>
                <div>Right: X: {gamepad.rightStick.x.toFixed(2)}, Y: {gamepad.rightStick.y.toFixed(2)}</div>
            </div>
            
            <div class="bg-neutral-800/60 rounded p-2">
                <h3 class="font-bold border-b border-orange-500/30 pb-1 mb-1">Buttons</h3>
                <div class="grid grid-cols-2">
                    <div>△: {gamepad.buttons.triangle ? 'PRESSED' : 'released'}</div>
                    <div>○: {gamepad.buttons.circle ? 'PRESSED' : 'released'}</div>
                    <div>✕: {gamepad.buttons.cross ? 'PRESSED' : 'released'}</div>
                    <div>□: {gamepad.buttons.square ? 'PRESSED' : 'released'}</div>
                    <div>PS: {gamepad.ps ? 'PRESSED' : 'released'}</div>
                </div>
            </div>
        </div>
        
        <div>
            <div class="bg-neutral-800/60 rounded p-2 mb-2">
                <h3 class="font-bold border-b border-orange-500/30 pb-1 mb-1">D-Pad</h3>
                <div class="grid grid-cols-2">
                    <div>Up: {gamepad.dpad.up ? 'PRESSED' : 'released'}</div>
                    <div>Down: {gamepad.dpad.down ? 'PRESSED' : 'released'}</div>
                    <div>Left: {gamepad.dpad.left ? 'PRESSED' : 'released'}</div>
                    <div>Right: {gamepad.dpad.right ? 'PRESSED' : 'released'}</div>
                </div>
            </div>
            
            <div class="bg-neutral-800/60 rounded p-2">
                <h3 class="font-bold border-b border-orange-500/30 pb-1 mb-1">Shoulder Buttons</h3>
                <div>L1: {gamepad.bumpers.l1 ? 'PRESSED' : 'released'}, R1: {gamepad.bumpers.r1 ? 'PRESSED' : 'released'}</div>
                <div>L2: {(gamepad.triggers.l2 * 100).toFixed(0)}%, R2: {(gamepad.triggers.r2 * 100).toFixed(0)}%</div>
            </div>
        </div>
    </div>
</div>