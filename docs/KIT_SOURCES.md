# Where each kit file came from

| Workspace path | Source in Premium Frontend AI Kit | Change |
|---|---|---|
| .agents/rules/premium-frontend.md | premium-frontend.md (= antigravity/.agent/rules/premium-frontend.md) | YAML frontmatter added (trigger: glob) — Antigravity ignores rules/*.md without it |
| .agents/rules/references.md | references.md (= antigravity/.agent/rules/references.md) | frontmatter added (trigger: model_decision) |
| .agents/rules/web-interface-guidelines.md | indie-skills/web-interface-guidelines/AGENTS.md | frontmatter added (trigger: glob) |
| .agents/rules/bugdna-app-ui.md | — (new for BugDNA) | app-screen override layer |
| .agents/skills/premium-component/SKILL.md | antigravity/.agent/workflows/premium-component.md | workflow → skill; name added; rules path updated to .agents/; BugDNA note appended |
| .agents/skills/impeccable/SKILL.md | indie-skills/impeccable/SKILL.md | none (scripts/ and reference/ not in kit) |
| .agents/skills/design-taste-frontend/SKILL.md | indie-skills/taste-skill/SKILL.md | folder named after frontmatter name (scripts/, references/, assets/, library/ not in kit) |
| .agents/skills/baseline-ui/SKILL.md | indie-skills/ui-skills-baseline/SKILL.md | none |
| .agents/skills/fixing-motion-performance/SKILL.md | indie-skills/ui-skills-motion-perf/SKILL.md | none |
| .agents/skills/animate/SKILL.md + RECIPES.md | indie-skills/emil-kowalski/animate-SKILL.md + animate-RECIPES.md | RECIPES renamed to match the skill's link |
| .agents/skills/animation-vocabulary/SKILL.md | indie-skills/emil-kowalski/animation-vocabulary-SKILL.md | none |

Not installed: USER_RULES_paste.txt, cursor/.cursor/rules/*.mdc, optional/vendor-company-skills/*, src/*, README.md.
If your Antigravity build does not list these under Customizations, rename `.agents/` to the legacy `.agent/`.
