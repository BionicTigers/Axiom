import type { Config } from 'tailwindcss'

export default {
  content: [
    "./src/**/*.{html,js,svelte,ts}",
  ],
  theme: {
    extend: {
        colors: {
            blurple: {
                '50': '#eef3ff',
                '100': '#e0e9ff',
                '200': '#c6d6ff',
                '300': '#a4b9fd',
                '400': '#8093f9',
                '500': '#5865f2',
                '600': '#4445e7',
                '700': '#3836cc',
                '800': '#2f2fa4',
                '900': '#2d2f82',
                '950': '#1a1a4c',
            }
        }
    },
  },
  plugins: [],
} satisfies Config 