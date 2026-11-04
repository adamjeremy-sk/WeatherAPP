# HANDOFF.md — Orchestrator Continuity Log
Maintained by the Orchestrator per AI_ONBOARDING.md §6. A new instance reads this third, after AI_ONBOARDING.md and ARCHITECTURE.md.

## 1. Current State  *(overwrite every update)*
| Field | Value |
|---|---|
| Updated | 2026-11-03 |
| App version | 0.8.1 (VC28), released 2026-10-29 |
| Repo HEAD at update | b47934a "Website updates" |
| Tests | 216 passing (per 0.8.0 changelog) |
| Active task | Baseline audit of local LLM agent (first run on fresh OS) — read-only, 3 gates |
| Gate position | Step 0.1 COMPLETE — PASS WITH NOTES (build OK, verifyEdgeToEdge clean, 216/216 tests; notes: self-split step once, summarized counts without raw output, "1s / no JDK download" unexplained). Step 0.2 COMPLETE — PASS WITH NOTES: 9/9 facts correct, avoided translatable=false trap; JDK 25 was pre-provisioned in ~/.gradle/jdks (provenance resolved). Defect: fabricated "approximate" line number (0.2.1 cited :15-16, real :46-47) when grep run without -n. Step 0.3 issued with no-guessed-line-numbers + quoted-context rules. |
| Awaiting | Adam: paste agent's Step 0.3 output (may arrive in 2 parts: R1–R5, R6–R10) |
| Last prompt issued | orchestration/PROMPT_baseline_audit.md (verbatim, full 3-step prompt) |
| Next action | Grade each step against orchestration/ANSWER_KEY_baseline_audit.md (never show the key to the agent). Step 0.1 failures are environment issues — help Adam fix the machine, then re-run 0.1. After 0.3: write an agent capability profile into §3/§2 and decide whether R5 (bottom sheets) and R6 (Float last/default coords) become fix tasks. |

**Open decisions (Adam's call)**
- Should the orchestration docs (AGENTS.md, AI_ONBOARDING.md, ARCHITECTURE.md, HANDOFF.md, MASTER_EXAMPLE.md) live in the repo, and where (root vs docs/ai/)?
- F-Droid eligibility vs play-app-update-ktx dependency.

## 2. Decision Log  *(append only, one line each, newest last)*
- 2026-11-03 — Orchestrator/auditor role moved from Qwen to Claude (claude.ai Project "WeatherAPP"). Coding agent remains a local LLM. Gate workflow unchanged.
- 2026-11-03 — ARCHITECTURE.md / AI_ONBOARDING.md / AGENTS.md corrected against repo at b47934a; HANDOFF.md hand-off system introduced (AI_ONBOARDING §6).
- 2026-11-03 — Local agent's context is frequently wiped: standing corrections go into AGENTS.md, not just prompts. Added to AGENTS.md: one STEP = all its items; raw output for every count; no guessed line numbers; quoted context for classifications; no temp files; CONTEXT RESETS section. Continuation prompts must state last verified state (AI_ONBOARDING §2.5).

## 3. Backlog & Known Debt  *(edit freely)*
- Rail violations found by orchestrator (pending baseline-audit confirmation): LocationDialogManager uses BottomSheetDialog for search + recent-locations dialogs (no-bottom-sheets rail); LocationPreferences stores last-used and fixed-default lat/lon as Float (Double invariant — needs prefs migration plan); unused BottomSheetDialog import in ThemeManager.
- Merge duplicate SolarLightingEngineTest classes (test root vs test solar/) — must not reduce test count.
- No CI workflow (verifyEdgeToEdge + unit tests would be natural first jobs).
- SettingsDialogManager is ~2,300 lines (owns every settings screen) — candidate for a planned split (needs plan per AGENTS rule 10).
- docs/extensions.md (theme + forecast-provider manifests, schema v1) is an unimplemented draft; Phase V.
- Orchestrator cannot build locally (no Android SDK in its workspace) — Adam remains build authority.

## 4. Session Log  *(newest first, keep 10)*
### 2026-11-03 — Baseline audit prompt
- Wrote 3-gate read-only calibration prompt (environment smoke test, codebase fact check with known answers, invariant sweep with genuine violations + decoys) and a private answer key.
### 2026-11-03 — Onboarding Claude as Orchestrator
- Cloned repo at b47934a and audited it against the Qwen-era docs.
- Found and fixed doc drift: dead GoldenHourActivity + 5 activity_* layouts already deleted; legacy AlertManager/AlertCache/AlertTestHarness already deleted; theme-engine now compileSdk 37 / Java 11 (mismatch resolved); stale proguard Room rule gone; 2x2→2x1 receiver rename done; backup rules now exclude all prefs (no longer templates); layout counts updated (15 section, 16 item, 19 dialog); added verifyEdgeToEdge gate, SystemBarContrastHelper floating-dialog exemption, and 0.8.0 hardening facts.
- Added hand-off protocol (AI_ONBOARDING §6) and this file.
