/** @type {import('tailwindcss').Config} */
export default {
  content: ['./index.html', './src/**/*.{vue,js,ts,jsx,tsx}'],
  theme: {
    extend: {
      colors: {
        'deep-space': '#0a0e1a',
        'indigo-night': '#1a1f3a',
        'starlight': '#e8eaf6',
        'dark-gold': '#c5a572',
        'moonlight': '#9fa8da',
      },
      fontFamily: {
        'display': ['"Cormorant Garamond"', 'serif'],
        'body': ['"Manrope"', 'sans-serif'],
        'mono': ['"JetBrains Mono"', 'monospace'],
      },
      backdropBlur: {
        'xs': '2px',
      },
      animation: {
        'star-drift': 'starDrift 60s linear infinite',
        'glow-pulse': 'glowPulse 4s ease-in-out infinite',
      },
      keyframes: {
        starDrift: {
          '0%': { transform: 'translateY(0)' },
          '100%': { transform: 'translateY(-100px)' },
        },
        glowPulse: {
          '0%, 100%': { opacity: '0.6' },
          '50%': { opacity: '1' },
        },
      },
    },
  },
  plugins: [],
}
