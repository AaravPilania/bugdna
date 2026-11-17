---
name: premium-component
description: Build an agency-grade component/section
---
1. Read .agents/rules/premium-frontend.md and .agents/rules/references.md (for BugDNA also .agents/rules/bugdna-app-ui.md); pick 2–3 references and note what to borrow.
2. Write a short design brief: purpose, aesthetic direction, fonts, palette, the signature motion/WebGL moment.
3. Build the component in TS/React with Tailwind + CSS vars; add GSAP (useGSAP) / Lenis / R3F as needed.
4. Add prefers-reduced-motion fallback and lazy-load heavy parts.
5. Review against the anti-generic rules; refine spacing, type and easing once more.

BugDNA note: this project is React + Vite in JavaScript (per the spec). For the dashboard, skip GSAP/Lenis/R3F/WebGL and follow .agents/rules/bugdna-app-ui.md.
