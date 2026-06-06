
## Nail-It (指尖 SOP) — Implementation Plan

A mobile-first, 4-screen interactive prototype simulating an AI nail-art DIY assistant. Navigation is local React state (no router changes) so the whole flow lives in the home route.

### Design tokens (src/styles.css)
Add semantic tokens in oklch:
- `--background` ivory `#FAFAFA`
- `--foreground` charcoal `#2C2C2C`
- `--brand` muted rose `#D4A3A3`
- `--success` mint `#A8D5BA`
- `--muted-strike` `#9CA3AF`
- `--focus-bg` dark charcoal `#1A1A1A`
- `--gradient-cta` champagne gold → rose gold
- Body backdrop `#E5E5E5`
- Font: clean sans-serif (system stack + Inter fallback; no remote @import per Tailwind v4 rules).

### Route
Replace placeholder `src/routes/index.tsx`. Update head() with proper title/description. Render a centered phone frame (max-w-[430px], mx-auto, min-h-screen) on a `#E5E5E5` body background.

### Component structure
Single page with `useState<'home'|'tryon'|'prep'|'focus'>('home')` and conditional rendering. Sub-components in `src/components/nailit/`:
- `PhoneFrame.tsx` — centered 430px container with ivory bg
- `HomeScreen.tsx` — logo, dashed input, floating paste tooltip, gradient CTA
- `TryOnScreen.tsx` — back chevron, large hero image (65vh), "长按对比原手" press button, dual action bar
- `PrepScreen.tsx` — thumbnail, two checklist sections, custom checkboxes with strikethrough transition, sticky CTA disabled until ≥1 checked
- `FocusScreen.tsx` — dark mode, 50% hero, bold step text, circular SVG countdown ring with `60s` decrementing via `setInterval`, animated audio-wave icon, long-press handler on screen to exit to home

### Images
Generate 3 placeholder assets via imagegen and import as ES6:
- `nail-tryon.jpg` — elegant manicured hand, muted rose nails, soft lighting
- `nail-thumb.jpg` — small product shot of same design
- `nail-step.jpg` — close-up of nail being painted with gel brush

### Key interactions
- Home CTA → tryon
- TryOn left button → home; right button → prep
- TryOn "长按对比原手" — `onPointerDown/Up` toggles a "before" overlay state
- Prep checkboxes — controlled state; CTA enabled when `checked.length > 0`; strikethrough via Tailwind transition
- Focus screen — countdown timer using `useEffect` + `setInterval`; long-press (700ms) on screen returns to home
- Floating "检测到链接" tooltip — small absolutely-positioned chip above input with subtle bounce

### Visual details
- Gradient CTA: `bg-[linear-gradient(135deg,#E8D5B7,#D4A3A3)]` rounded-full, soft shadow
- Checkboxes: custom div with brand-rose fill + check icon (lucide `Check`) on checked
- Countdown: SVG circle with `strokeDasharray` animated from full → 0
- Audio wave: 3 bars with staggered `animate-pulse` / custom keyframes
- Icons: lucide-react (`Sparkles`, `ChevronLeft`, `Check`, `Mic`)

### Files to create/modify
- modify `src/styles.css` (add tokens + body backdrop + keyframes)
- modify `src/routes/index.tsx` (mount NailItApp + head meta)
- create `src/components/nailit/NailItApp.tsx`
- create `src/components/nailit/PhoneFrame.tsx`
- create `src/components/nailit/HomeScreen.tsx`
- create `src/components/nailit/TryOnScreen.tsx`
- create `src/components/nailit/PrepScreen.tsx`
- create `src/components/nailit/FocusScreen.tsx`
- generate 3 images under `src/assets/`

No backend, no routing changes, no new dependencies (lucide-react already present).
