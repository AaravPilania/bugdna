---
trigger: glob
globs: "dashboard/**, demo-store/**"
description: "BugDNA UI rules: adapts premium-frontend.md to an app screen (dashboard). Wins over premium-frontend.md where they conflict."
---

# BugDNA app-screen UI rules (override layer)

Precedence for UI decisions: this file → web-interface-guidelines.md → premium-frontend.md → skill defaults.

## Mode
- The dashboard is an **app screen people work in** ("Operate" mode in the impeccable skill; "app screen" in design-taste-frontend). Ambition = precision, density, restraint.
- From premium-frontend.md KEEP: §0 reference-study + 6-line brief, §1 one concept, §4 palette discipline (off-black/off-white, 1 sharp accent), §7 complete hover/active/focus-visible states, §8 anti-slop bans, §9 craft baseline.
- From premium-frontend.md DROP for the dashboard: WebGL/R3F/shaders, Lenis smooth scroll, scroll-velocity effects, custom cursor, magnetic buttons, loaders, 0.9–1.4 s reveals, 10–18vw display type, Next.js stack (this project is React + Vite + JavaScript per the spec).

## Concept
- "Every bug has a genome": the SHA-256 fingerprint drawn as a 64-cell DNA strand (bug header + thin strip per row). Signature moment: strand pulses once on a new occurrence; regression = red mutation tick + REGRESSION tag.

## Tokens
- bg #0B0C0E · panel #121418 · line #22262C · text #ECEAE4 · muted #8B9097 · accent #C8F169 (live/sync only) · danger #FF5D5D · warn #F2B544.
- Type: Geist (UI), Geist Mono (ids, hashes, endpoints, timestamps; labels 11–12px uppercase +0.05em). Never Inter/Roboto/Poppins.
- Motion: use the animate skill's tokens and durations; interaction feedback ≤ 200 ms; animate transform/opacity only; prefers-reduced-motion disables non-essential motion.

## Layout
- Bug list = dense table, not cards; right rail = Live Activity feed with source badges (API / TRACKER / MYSQL_CLI).
- Bug detail = split: timeline left, controls + AI panel + comments right.
- Primitives: Radix or Base UI (one system only); AlertDialog for deletes; skeletons for loading; errors next to the action; URL-synced filters; h-dvh not h-screen.

## Bans
- Purple/blue gradients, glassmorphism, emoji icons, icon-in-tinted-square + title + text cards, rainbow charts, invented vanity numbers, uniform rounded-2xl + shadow-lg, lorem copy.

## Vendored skills
- impeccable and design-taste-frontend reference scripts/ and reference(s)/ files that are not vendored in this kit. If a step needs one, say so in one line, follow the SKILL.md text, and continue.
