---
trigger: glob
globs: "dashboard/**, demo-store/**"
description: "Agency-grade frontend rules from the Premium Frontend kit. For BugDNA app screens, bugdna-app-ui.md overrides the marketing-site parts (WebGL, Lenis, custom cursor)."
---

# Agency-Grade Frontend (Lusion / Awwwards SOTD / FWA level)
Distilled from independent craftspeople: pbakaus/impeccable, Leonxlnx/taste-skill, emilkowalski/skills, ibelick/ui-skills, Vercel web-interface-guidelines (Rauno-era), Lusion, Bruno Simon, Yuri Artiukh (akella), Edan Kwan, Codrops, darkroom/satus, 14islands. See references.md.

## 0. Reference-study workflow (mandatory before designing)
1. Name 2–3 references from references.md that match the brief (e.g. "lusion.co hero + akella/DistortedPixels hover + codrops/ScrollBlurTypography").
2. For each, write what specifically to borrow: technique, timing, layout move. Read the reference repo source if available.
3. Write a 6-line brief: concept, aesthetic direction, type pair, palette (hex), signature moment, motion language (easings + durations).
4. Only then code. After building, critique against §8 and do one refinement pass.

## 1. Concept over components
- Every site has ONE idea that drives visuals and motion (a material, a metaphor, a physical behaviour). Sections express the idea; they are not "feature cards".
- Design the signature moment first (hero WebGL, transition, scroll scene), then the system around it.

## 2. Layout systems
- 12-col (desktop) / 4-col (mobile) grid, gutters via CSS vars; break it deliberately: oversized type bleeding off-grid, offset images, overlapping layers.
- Big scale contrast: display 10–18vw vs body 14–16px. Huge whitespace or deliberate density — never medium everything.
- Sticky/pinned editorial sections, horizontal galleries, split-screen, full-bleed media. Avoid centered hero + 3 cards + CTA band.

## 3. Typography
- Characterful display face (PP Neue Montreal, PP Editorial New, Neue Haas, Monument Extended, Migra, Fraunces, GT Sectra); mono for small labels/metadata (uppercase, 11–12px, +0.05em tracking).
- Headlines: line-height 0.85–0.95, tracking −0.02 to −0.05em; clamp() fluid sizes.
- Kinetic type: split into lines → words → chars; reveal through overflow:hidden masks (yPercent 100→0), stagger 0.02–0.05s per char / 0.08–0.12s per line. Scroll-scrubbed variable-font weight/blur (codrops/ScrollBlurTypography), marquee text that reacts to scroll velocity.

## 4. Color, texture, light
- 1 dominant + 1 neutral + 1 sharp accent; off-black (#0a0a0a–#111) and off-white (#f2f0eb) rather than pure.
- Film grain overlay (animated noise, opacity 0.04–0.08, mix-blend-mode overlay), soft vignettes, gradient light in shaders not flat CSS gradients.

## 5. Motion language (concrete values)
- Easings: expo.out cubic-bezier(0.16,1,0.3,1) for reveals; quart.inOut cubic-bezier(0.76,0,0.24,1) for page/curtain transitions; power2.out for hovers. Never linear, never default "ease".
- Durations: hover 0.25–0.4s; reveals 0.9–1.4s; page transitions 0.8–1.2s; loader 1.5–2.5s max.
- Smooth scroll: Lenis (lerp 0.08–0.1 or duration 1.2), synced to GSAP ticker; ScrollTrigger scrub 0.5–1 for smoothing.
- Use scroll velocity as an input (skew, RGB shift, stretch on images/text, clamp ±10).
- Interruptible, physics-feel motion for UI (springs: stiffness ~300, damping ~30) — Emil Kowalski principles: animate only what clarifies, fast for frequent actions, nothing on keyboard-repeat actions.
- Page transitions: persistent WebGL canvas; outgoing image morphs into next page's hero (FLIP + shader), or curtain wipe with quart.inOut. Barba.js / Next App Router + View Transitions.

## 6. WebGL techniques
- Image planes synced to DOM (lusionltd/WebGL-Scroll-Sync, 14islands/r3f-scroll-rig): one fixed canvas, meshes track getBoundingClientRect, scroll offset in uniforms.
- Hover distortion: uniforms uMouse, uHover (lerped 0→1), uTime; displacement via noise/texture map; RGB split proportional to velocity (akella/DistortedPixels, fake3d depth-map parallax).
- Scroll-driven 3D: camera on a spline, progress from ScrollTrigger; particles/GPGPU flow fields (edankwan/The-Spirit); fluid/cursor trails.
- Post: bloom (low), chromatic aberration, noise, custom fullscreen pass for transitions.
- Bruno Simon/Three.js Journey patterns: physics world, baked lighting, matcaps, performance-first assets (Draco/KTX2, merged geometry).
- Perf: dpr min(devicePixelRatio,2), lerp in render loop (no per-frame React state), dispose, pause when offscreen, low-power fallback image.

## 7. Cursor & micro-interactions
- Custom cursor lerped (0.15–0.2) with blend-mode difference; morphs to labels ("View", "Drag") over targets.
- Magnetic buttons (pull 0.3–0.4 of offset, elastic return), text-roll hover (duplicate line slides up), underline draw from left/out to right.
- Every interactive element has hover, active, focus-visible states.

## 8. Hard anti-slop rules
- BANNED: Inter/Roboto/Poppins default, purple/blue gradient hero, glassmorphism cards grid, emoji icons, generic lucide icon + title + text cards, "Get Started" pill CTA everywhere, uniform rounded-2xl + shadow-lg, stock 3D blobs, fade-up-everything with same timing, lorem-ish marketing copy.
- If a layout could be any SaaS template, redo it. If motion has no relation to the concept, cut it.
- Write real, specific copy; numbers, names, details.

## 9. Craft baseline (non-negotiable)
- prefers-reduced-motion: disable Lenis, shaders static, instant reveals.
- Semantic HTML, focus-visible, AA contrast, no layout shift, 60fps, lazy-load WebGL (dynamic import, ssr:false), LCP < 2.5s.
- Stack: Next.js/TS, Tailwind + CSS vars, GSAP (+SplitText, ScrollTrigger), Lenis, R3F/drei or raw three.js + custom GLSL. Starter reference: darkroomengineering/satus.
