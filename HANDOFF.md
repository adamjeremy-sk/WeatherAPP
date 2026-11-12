# HANDOFF.md — Orchestrator Continuity Log
Maintained by the Orchestrator per AI_ONBOARDING.md §6. A new instance reads this third, after AI_ONBOARDING.md and ARCHITECTURE.md, then ACTION_PLAN.md. **Capped at 15 KB.** The full pre-2026-11-12 log (every decision, Qwen/Gemma profiles, benchmarking notes, session logs) is in `docs/archive/HANDOFF_2026-11-04.md`.

## 1. Current State  *(overwrite every update)*
| Field | Value |
|---|---|
| Updated | 2026-11-12 (process alignment session) |
| App version | 0.8.2 (VC30), committed and pushed. Play upload / device check status not reported. |
| Repo HEAD at update | bec248f "Docs: handoff 2026-11-04 …". App code unchanged since d6477d6 (0.8.2 VC30). **Uncommitted, by the Orchestrator this session:** AGENTS.md, AI_ONBOARDING.md, ARCHITECTURE.md, HANDOFF.md, MASTER_EXAMPLE.md, ACTION_PLAN.md (new), docs/archive/HANDOFF_2026-11-04.md (new), docs/audits/ (3 reports, new), docs/sources/meeus-ch47/ (verified Moon tables, new). |
| Tests | 232 `@Test` (counted, last build 232 at 0.8.2). None measures astronomy accuracy. |
| Active task | Remediation program R0-R4 (ACTION_PLAN.md). The process docs were rewritten to the vermilion-editor methods; **no gate has been issued yet.** |
| Gate position | Before R0.1. |
| Awaiting | Adam: review and commit the docs (suggested message below); then the Orchestrator issues the R0.1 prompt. D1, D2, D7 and the push policy are decided (§2). |
| Last prompt issued | None outstanding. |
| Next action | On resume: State Readback, then R0.1 (`.gitignore`: `evidence/`, `.kotlin/`; new `.aiexclude`), then R0.2 (protected-path and size-ratchet tests). |
| Coder | Gemini 3.6 Flash (cloud) in Android Studio's Agent tab, for the whole remediation (Adam, 2026-11-12). Adam's Google Pro stays enabled for now and he will say if that changes (2026-11-12). |

**Open decisions (Adam's call; details and defaults in ACTION_PLAN.md "Decisions")**
Decided 2026-11-12: D1 (block Statcounter in the WebView), D2 (openly licensed Moon tables), D7 (audits in the repo). Still open: D3 Sun rise/set definition · D4 elevation (dip) policy · D5 formatter · D6 territory office codes · D8 phase refinement (each has a default and a needed-by gate in the plan). Older: F-Droid eligibility vs play-app-update-ktx; Adam's global git identity is still the placeholder "Your Name <you@example.com>" (his call).

## 2. Decisions  *(standing decisions in one line; append new dated ones at the end)*
**Standing (summarized from the archive; dates are the originals)**
- Gemma is retired permanently; never use, suggest or benchmark it (Adam, 2026-11-03).
- The Orchestrator is Claude in the claude.ai Project "WeatherAPP"; the Coder is a model in Android Studio's Agent tab (see §2b).
- Only Adam commits and pushes; the Orchestrator edits only the root orchestration docs (plus `docs/milestones/`, `docs/archive/`) through the desktop link, read-only git, and may commit handoff docs when Adam explicitly asks (method in the archive, 2026-11-04 entries).
- Commit only when a whole task is finished; between steps work stays uncommitted; the Coder is forbidden restore/checkout/stash/reset/clean (Adam, 2026-11-03).
- Standing corrections go into AGENTS.md, which changes are tracked by a dated line here; no version stamp in it; prompts open with the mandatory "FIRST: read AGENTS.md" block and a "Last verified" line (Adam, 2026-11-03).
- Alert updates replace the notification silently; re-alert only when severity increases; cancel or expiry clears it (Adam, 2026-11-03).
- 0.8.2 shipped as VC30; VC29 (an undocumented 0.8.1.1 label hotfix, since rebuilt in 0.8.2) is skipped in the changelog.
- The ">5 files → plan first" limit is gone; rule 10 gates only AlertProvider, prefs schema (now also Gson cache formats and backup rules) (Adam, 2026-11-03).
- LLM speed benchmarking lives in ~/Projects/llm-suite-test; ROCm was evaluated and dropped (Adam, 2026-11-03/04).

**New, 2026-11-12 (Adam)**
- 2026-11-12 — Adam adopted all recommendations of CODE_AUDIT, ASTRONOMY_AUDIT and DRIFT_ANALYSIS.
- 2026-11-12 — The Human/Orchestrator/Coder methods refined in vermilion-editor are adopted for WeatherAPP: AGENTS.md rules 16-22 (size, no carried debt, evidence files, formatting, language traps, no file-writing checks, Prompt doubts); multi-step prompts audited along the way; milestone reviews; mirror rule; HANDOFF cap. AGENTS.md change (h): rules 16-24, secrets rule for a cloud Coder, Gemini tooling section. Current = (a)–(h).
- 2026-11-12 — "FROZEN/VERIFIED" is withdrawn. `lunar/` and `solar/` are PROTECTED (the Coder may not edit them; origin: stop UI fixes from touching formulas) and are NOT VERIFIED. VERIFIED returns only with a golden test that cites its source (AGENTS.md rules 23-24).
- 2026-11-12 — Coder for the remediation program: Gemini 3.6 Flash. Qwen is not used in it unless Adam says so.
- 2026-11-12 — The "carry notes forward" verdict and "fresh agent chat for every prompt" are replaced by "no carried debt" and multi-step prompts (see AI_ONBOARDING §2).
- 2026-11-12 — HIGH-STAKES gates (astronomy, alert pipeline, privacy statements and backup rules, prefs schema) get a second reader: a cold review in a fresh context.
- 2026-11-12 — Tables and constants dictated by the Orchestrator need provenance; recalled numbers are never a source.
- 2026-11-12 — (Adam) D1: block Statcounter inside the in-app WebView. D2: use an openly licensed implementation for the Moon tables (done: astronomia MIT tables verified against Meeus Example 47.a, in `docs/sources/meeus-ch47/`). D7: audits copied to `docs/audits/`. Push at the end of every gate after the verdict (vermilion policy adopted). Google Pro stays enabled.
- 2026-11-12 — (Adam) Android Studio Agent settings: it may do anything except `git push` and destructive git; edits and commands are NOT hand-approved. The rules and the R0.2 tests are the guard; the Orchestrator checks `git log -1`/`git status --short` in every audit.

## 2b. Coder Profile  *(edit as evidence accumulates)*
**Gemini 3.6 Flash (cloud) in Android Studio's Agent tab.** Used from Step 3a of the 2026-11-03 alert fix. Observed so far: correct code, caught a spec gap (chainKey lost when a fresh provider copy replaced the cached one); weaknesses: "..." inside a raw block despite an explicit prohibition; a retyped diff (a blank added line missing its "+"); one drive-by import removal. Check the "Generated by" footer on every report. Grade its classifications strictly and check any "shown as raw output" claim. It has read access to everything in the project, so the secrets rule (AGENTS.md 13) and an `.aiexclude` file (R0.1) matter. Evidence from the first remediation gates will update this profile; the sizing in AI_ONBOARDING §2.6 stays at the conservative figure until then.
**Harness**: Android Studio built-in Agent; per Adam (2026-11-12) it may do anything except `git push` and destructive git, with no hand approval; the exact list of blocked git commands is not recorded (ask Adam if it matters). `commit` is not blocked by the IDE, so AGENTS.md forbids it. Quirks: it sometimes blanks piped output and path-like lines; it drops stdout when `awk` is the bare last command of a pipe (wrap in `( … )` or pipe to `| cat`); use single unpiped commands for evidence.
**Not in use**: Qwen3.8-27B (local llama.cpp; profile, server config and speed tuning in the archive), Gemma (retired).

## 3. Backlog & Known Debt
All findings and the older backlog items are in **ACTION_PLAN.md**, ordered by priority, each with an ID. There is no separate list here (a second list is how debt gets lost). Parked, not scheduled: docs/extensions.md (theme + forecast-provider manifests, schema v1; Phase V); F-Droid eligibility vs play-app-update-ktx.

## 4. Session Log  *(newest first, keep 3)*
### 2026-11-12 — Audits, drift analysis, process alignment
- Audited the code (CODE_AUDIT) and `lunar/`+`solar/` (ASTRONOMY_AUDIT); read vermilion-editor's process docs and wrote DRIFT_ANALYSIS. Findings of note: ECCC memo not keyed by location (missed alerts); backup rules name the wrong prefs files; Statcounter runs in the in-app pages; Moon series wrong (phase times up to 47 min); sunrise at the wrong altitude.
- Rewrote AGENTS.md and AI_ONBOARDING.md, updated ARCHITECTURE.md (claim tags, withdrawn "verified" label, corrected Do-not-touch), MASTER_EXAMPLE.md (current-format example) and this file; wrote ACTION_PLAN.md; archived the old HANDOFF. Nothing in the app code was touched. Documents were read, not chat transcripts; the astronomy checks used Python transliterations and published values because the Orchestrator cannot build.
### 2026-11-04 — LLM speed benchmarking (spun off), workflow changes
- Tuned the Qwen server's MTP drafting; moved the benchmark to ~/Projects/llm-suite-test; orchestrator got read/write access to the repo folder (docs only) and may commit handoff docs when asked. Details: archive.
### 2026-11-03 — 0.8.2 (VC30), alert re-notification fix, baseline audit, runtime tuning
- Fixed repeated fog notifications (chains, silent updates, escalation re-alerts); 0.8.2/VC30 released with the label-column and double-inset fixes; Gemma retired after Suite v1; docs corrected against the repo. Details: archive.
