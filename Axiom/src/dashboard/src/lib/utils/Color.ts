export type RGB = {
    r: number;
    g: number;
    b: number;
};

export type ColorStop = {
    color: RGB;
    position: number;
};

export function rgbToString(rgb: RGB): string {
    return `rgb(${rgb.r}, ${rgb.g}, ${rgb.b})`;
}

export function generateGradient(colorStops: ColorStop[], value: number): RGB {
    // Ensure stops are sorted by position
    colorStops.sort((a, b) => a.position - b.position);
    
    // Find the two colors to interpolate between
    let startStop = colorStops[0];
    let endStop = colorStops[colorStops.length - 1];
    
    for (let i = 0; i < colorStops.length - 1; i++) {
        if (value >= colorStops[i].position && value <= colorStops[i + 1].position) {
            startStop = colorStops[i];
            endStop = colorStops[i + 1];
            break;
        }
    }

    // If value is outside the range, return the nearest color
    if (value <= startStop.position) return startStop.color;
    if (value >= endStop.position) return endStop.color;

    // Calculate the percentage between these two specific stops
    const percentage = (value - startStop.position) / (endStop.position - startStop.position);
    
    // Interpolate between the colors
    return {
        r: Math.round(startStop.color.r + (endStop.color.r - startStop.color.r) * percentage),
        g: Math.round(startStop.color.g + (endStop.color.g - startStop.color.g) * percentage),
        b: Math.round(startStop.color.b + (endStop.color.b - startStop.color.b) * percentage)
    };
}