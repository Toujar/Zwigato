/** @type {import('tailwindcss').Config} */
export default {
  content: ['./index.html', './src/**/*.{js,ts,jsx,tsx}'],
  theme: {
    extend: {
      colors: {
        primary:     '#0EA5E9',   // sky-500  — iOS light blue
        primaryDark: '#0284C7',   // sky-600
        accent:      '#38BDF8',   // sky-400
        secondary:   '#0F172A',   // slate-900
        dark:        '#020617',   // slate-950
        light:       '#F0F9FF',   // sky-50
        muted:       '#94A3B8',   // slate-400
        glass: {
          white:  'rgba(255,255,255,0.65)',
          blue:   'rgba(224,242,254,0.55)',
          dark:   'rgba(15,23,42,0.65)',
          border: 'rgba(255,255,255,0.45)',
          'border-blue': 'rgba(186,230,253,0.60)',
        },
      },
      fontFamily: {
        sans: ['-apple-system','BlinkMacSystemFont','"SF Pro Display"','Poppins','sans-serif'],
      },
      borderRadius: {
        glass: '20px',
        card:  '16px',
        pill:  '9999px',
        '3xl': '1.5rem',
        '4xl': '2rem',
      },
      boxShadow: {
        glass:
          '0 8px 32px rgba(14,165,233,0.12), 0 2px 8px rgba(14,165,233,0.08), inset 0 1px 0 rgba(255,255,255,0.60)',
        'glass-lg':
          '0 20px 60px rgba(14,165,233,0.18), 0 4px 16px rgba(14,165,233,0.10), inset 0 1px 0 rgba(255,255,255,0.55)',
        'glass-dark':
          '0 8px 32px rgba(0,0,0,0.35), 0 2px 8px rgba(0,0,0,0.25), inset 0 1px 0 rgba(255,255,255,0.10)',
        float:
          '0 12px 40px rgba(14,165,233,0.35), 0 4px 12px rgba(14,165,233,0.18)',
        card:  '0 4px 24px rgba(14,165,233,0.10)',
        'card-hover': '0 16px 48px rgba(14,165,233,0.18)',
        sm:    '0 1px 4px rgba(14,165,233,0.08)',
      },
      backdropBlur: {
        glass: '20px',
        xs: '4px', sm: '8px', md: '12px', lg: '20px', xl: '40px',
      },
      backgroundImage: {
        'hero-gradient':
          'linear-gradient(135deg, #0EA5E9 0%, #38BDF8 45%, #7DD3FC 100%)',
        'glass-gradient':
          'linear-gradient(135deg, rgba(255,255,255,0.72) 0%, rgba(224,242,254,0.55) 100%)',
        'blue-mesh':
          'radial-gradient(ellipse at 20% 50%, rgba(14,165,233,0.18) 0%, transparent 60%), radial-gradient(ellipse at 80% 20%, rgba(56,189,248,0.15) 0%, transparent 55%), radial-gradient(ellipse at 50% 80%, rgba(125,211,252,0.12) 0%, transparent 55%)',
        'shimmer':
          'linear-gradient(90deg, rgba(255,255,255,0) 0%, rgba(255,255,255,0.65) 50%, rgba(255,255,255,0) 100%)',
      },
      keyframes: {
        float:      { '0%,100%': { transform: 'translateY(0)' }, '50%': { transform: 'translateY(-12px)' } },
        shimmer:    { '0%': { backgroundPosition: '-200% 0' }, '100%': { backgroundPosition: '200% 0' } },
        'scale-in': { '0%': { opacity: '0', transform: 'scale(0.92)' }, '100%': { opacity: '1', transform: 'scale(1)' } },
        'slide-up': { '0%': { opacity: '0', transform: 'translateY(20px)' }, '100%': { opacity: '1', transform: 'translateY(0)' } },
        'fade-in':  { '0%': { opacity: '0' }, '100%': { opacity: '1' } },
      },
      animation: {
        float:      'float 4s ease-in-out infinite',
        shimmer:    'shimmer 2s linear infinite',
        'scale-in': 'scale-in 0.25s cubic-bezier(0.34,1.56,0.64,1)',
        'slide-up': 'slide-up 0.35s ease-out',
        'fade-in':  'fade-in 0.3s ease-out',
      },
      transitionTimingFunction: {
        spring: 'cubic-bezier(0.34,1.56,0.64,1)',
        ios:    'cubic-bezier(0.25,0.46,0.45,0.94)',
      },
    },
  },
  plugins: [],
}
