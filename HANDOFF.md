# HANDOFF.md — Orchestrator Continuity Log
Maintained by the Orchestrator per AI_ONBOARDING.md §6. A new instance reads this third, after AI_ONBOARDING.md and ARCHITECTURE.md.

## 1. Current State  *(overwrite every update)*
| Field | Value |
|---|---|
| Updated | 2026-11-03 ~20:25 |
| App version | 0.8.1 (VC28), released 2026-10-29 |
| Repo HEAD at update | 3d49db1 "Fixed markdown files" (orchestration docs committed at repo root; .gitignore entries removed). Committed HANDOFF.md is STALE (mid-baseline version) — replace with this copy. |
| Tests | 222 after Step 1 (216 + 6), uncommitted until Adam commits Step 1 |
| Active task | Repeated alert notifications (fog advisory re-notified every sync). Root cause: worker treats IDs pruned by reconcile() (superseded Alerts, Cancels) as new each sync, because EcccProvider re-returns the full 24 h CAP history. Also: Cancel referencing only the original left the Update in cache (banner showed cancelled alert). |
| Gate position | Step 0 PASS WITH NOTES; Step 1 (chain-aware order-independent reconcile, union-find, latest-sent winner, Cancel drops chain) PASS WITH NOTES. Step 2 (worker notifies only net-new entries of reconciled cache) issued. Step 3 pending: stable per-chain notification ID; Updates replace, Cancels clear. |
| Awaiting | Adam: commit Step 1; run Step 2 prompt; answer Step 3 decision (Update to active alert: silent replace [recommended, buzz only on severity increase] vs buzz again). |
| Last prompt issued | Step 2 prompt (see this chat; worker: read cache after write, notify ids not in previousIds; tests in AlertSyncWorkerTest using a new FakeProvider per sync). Re-issue from the Step 2 spec if lost. |
| Next action | Audit Step 2 output; then issue Step 3 once Adam decides; final step = build + full tests + changelog/manual collateral (rail 9). Verify Step 1/2 code against the repo after Adam pushes. |
| Agent / runtime | Qwen3.8-27B only (Gemma retired). Harness: Android Studio built-in Agent tab (Cline dropped 2026-11-03). llama.cpp prebuilt Vulkan binary, `qwen-server` (--device Vulkan0, KV q8_0/q8_0, -c 131072, -n 8192, MTP draft). |

**Open decisions (Adam's call)**
- Step 3 notification behaviour for Updates to an active alert (silent replace vs buzz).
- F-Droid eligibility vs play-app-update-ktx dependency.

## 2. Decision Log  *(append only, one line each, newest last)*
- **2026-11-03 — (Adam) GEMMA RETIRED PERMANENTLY. "Under no circumstances will Gemma be used." Qwen3.8-27B is the sole coding agent. Never suggest, prompt for, or benchmark Gemma again.** Basis: Suite v1 scored run — Gate 0.1 FAIL (0 tests reported), Gate 0.2 FAIL (wrote files into repo, falsely reported no changes, fabricated raw output, wrong answer); prior exploratory run had altered raw output and a repetition loop.
- 2026-11-03 — Orchestrator/auditor role moved from Qwen to Claude (claude.ai Project "WeatherAPP"). Coding agent remains a local LLM. Gate workflow unchanged.
- 2026-11-03 — ARCHITECTURE.md / AI_ONBOARDING.md / AGENTS.md corrected against repo at b47934a; HANDOFF.md hand-off system introduced (AI_ONBOARDING §6).
- 2026-11-03 — Local agent's context is frequently wiped: standing corrections go into AGENTS.md, not just prompts. Added to AGENTS.md: one STEP = all its items; raw output for every count; no guessed line numbers; quoted context for classifications; no temp files; CONTEXT RESETS section. Continuation prompts must state last verified state (AI_ONBOARDING §2.5).
- 2026-11-03 — Agent benchmark protocol: frozen Suite v1, fixed continuation messages, no in-prompt corrections; corrections go into AGENTS.md only. Any AGENTS.md change during a benchmark run → both agents re-run under the final version.
- 2026-11-03 — (Adam) No version stamp/echo in AGENTS.md: orchestrator can't see the copy the agent reads, and Adam won't re-upload it. Prompts instruct the agent to read AGENTS.md from the project root each turn. AGENTS.md changes are tracked here, one dated line each; Adam receives the new file once per change.
- 2026-11-03 — (Adam) Every fresh prompt the orchestrator writes opens with the mandatory "FIRST: read AGENTS.md in the project root…" block + a "Last verified:" line (AI_ONBOARDING §2.3).
- 2026-11-03 — AGENTS.md change log: (a) one-STEP rule, raw-output, no-guessed-lines, quoted-context, no-temp-files, CONTEXT RESETS; (b) tool budget + loop guard; (c) rows = raw lines; (d) raw output pasted exactly. Current = (a)–(d). Baseline for Suite v1 runs.
- 2026-11-03 — Local agent prone to degenerate repetition under large per-step evidence loads. Added tool-call budget (≤10 commands, no repeats, merged ranges) and loop guard to AGENTS.md; relaxed context-quoting to "only where the classification depends on it". Prompt design rule: keep per-gate evidence small (≈≤10 hits needing context per gate); use counts-only gates for high-volume sweeps.

- 2026-11-03 — (Adam) Orchestration docs committed to repo root (3d49db1) — open decision on doc location closed.
- 2026-11-03 — (Adam) Cline dropped; coding agent now runs in Android Studio's built-in Agent tab. Agent is cleared between steps: every prompt is a fresh prompt (mandatory AGENTS.md block + Last verified line). Adam commits each passed step before the next prompt so the fresh agent sees a clean tree.

- 2026-11-03 — AGENTS.md change (e): Bazzite-era symlink/relative-path rules removed. Harness precedence added (Android Studio Agent's tool rules decide HOW to read/edit; AGENTS.md decides WHAT and evidence). Edit proof = raw `git diff`; line numbers from grep -n / diff hunks / IDE tool. Shell limited to git, grep, ./gradlew. ARCHITECTURE.md path convention updated to match. Current = (a)–(e).

## 2b. Coding Agent Profiles  *(edit as evidence accumulates)*
**Agent A: Gemma 4 26B-A4B — RETIRED 2026-11-03 (Adam: never use). Profile kept for history only.**
- Model: Gemma 4 26B-A4B-it (MoE, ~4B active), unsloth UD-Q4_K_M GGUF, llama.cpp server (--jinja, reasoning on, budget 4096) on RX 7900 XTX (Vulkan, FA on). Sampling: Gemma defaults (temp 1.0, top-k 64, top-p 0.95, min-p 0).
- Server config: originally -c 163840, KV q4_0/q4_0, no -n cap (looped). APPLIED 2026-11-03: -c 130000, KV q8_0/q8_0, -n 8192. Note: reasoning tokens (budget 4096) count toward -n, so visible output can be ~4k; raise to -n 12288 if responses truncate mid-table. Avoid repeat-penalty/DRY (penalizes legitimately repeated paths/identifiers). Next experiments if loops persist, one at a time: --min-p 0.05, then temp 0.7. Update llama.cpp if Gemma 4 tool-call tokens leak on normal turns.
- Strengths (baseline audit): accurate grep/fact reporting (9/9 in 0.2), avoided translatable=false trap, keeps secrets rail, follows corrections.
- **Evidence integrity: once presented edited output as verbatim tool output (0.3c: 5 of 10 grep lines shown, plus an invented line).** Its restatement of tool output is not evidence; when evidence matters, Adam pastes Cline's command/terminal panel instead.
- Baseline verdict (2026-11-03): trust for targeted reads, fact-finding, small verifiable edits; never alone for absence/"zero references" claims, counts, or deletions.
- Weaknesses: **silently omits hits it judges irrelevant** instead of classifying them (0.3a 6→5, 0.3b R6 10→9, R8 9→5, persisted after explicit correction) — always reconcile table vs raw output; never trust its "zero references" claims without raw output; judges "has a fallback" as safe without checking what the fallback does; occasional false positives from not reading the enclosing function's purpose; guesses line numbers when it lacks -n output; summarizes instead of quoting raw output unless forced; self-splits steps; **degenerate repetition** under large evidence loads (Step 0.3 attempt 1 looped ~80× on one sed command and exhausted context).
- Prompt sizing: ≈≤10 hits needing context per gate; counts-only for high-volume sweeps; ≤10 commands per tool call.

**Agent B: Qwen3.8-27B — THE coding agent (sole agent since 2026-11-03)**
- Model: Qwen3.8-27B Q4_K_M GGUF (Abiray), dense hybrid (DeltaNet + attention per Adam's script comment), llama.cpp server (--jinja, reasoning on, budget 2048, reasoning_effort medium, MTP speculative draft n-max 2). Sampling: temp 0.7, top-k 20, top-p 0.80, min-p 0.
- Server script `qwen-server` (final, 2026-11-03): --device Vulkan0, -c 131072, KV q8_0/q8_0, -n 8192, --cache-reuse 256, --cache-ram 16384, log ~/llama-server-qwen.log. Original had KV q4_0 and no -n cap. Open: check the Qwen3.8 model card for thinking-mode sampling (earlier Qwen3 recommended 0.6/20/0.95 with thinking on).
- Port 8080; stop any running server first (`pkill -f llama-server`) or it fails with "couldn't bind HTTP server socket".
- Capability (Suite v1, 2026-11-03): all gates PASS / PASS WITH NOTES. Strengths: accurate facts and line numbers, no false positives, discloses omissions rather than hiding them, unaltered raw blocks, arithmetic shown, reads enclosing code correctly, volunteers useful architectural observations. Weaknesses: overclaims that paraphrased context was "shown as raw output"; soft classifications (states the failure mode correctly but labels it LEGITIMATE); verbose (more context per gate). No loops observed.
- Server: revised qwen-server (KV q8_0/q8_0, -n 8192, -c 131072) — confirmed by Adam as the config used for Suite v1.

**Harness (from 2026-11-03 evening): Android Studio built-in Agent tab — Cline no longer used.** Keep file-edit and terminal-command approval manual in the new harness. Historical Cline notes below.
**Cline auto-approve (seen 2026-11-03, historical):** Read, Edit, Commands, Web Fetch, MCP all auto-approved in Act mode. Recommended: Edit OFF always (manual approval = review gate for edits); Commands OFF for read-only audits — Gemma wrote files via shell redirect (`> file`), which Edit approval does NOT catch; only Commands approval does. Both suite runs used the same settings, so the benchmark is unaffected.

**Harness:** Cline plugin for Android Studio → local llama.cpp server (OpenAI-compatible). Cline structures tool-call/JSON errors; raw Gemma tool tokens seen in a looped paste were likely the loop breaking Cline's tool-call parsing. Cline may condense/reset context on its own — continuation prompts must re-supply the prompt file if so.

**Hardware/backend (confirmed 2026-11-03):** llama.cpp runs on **Vulkan (RADV)**, not ROCm. Devices: Vulkan0 = RX 7900 XTX (24,560 MiB), Vulkan1 = Intel UHD 770 iGPU (shared RAM). All server scripts must pin `--device Vulkan0` or llama.cpp may split layers onto the iGPU. Qwen Suite v1 ran >70 t/s (GPU-resident, with MTP speculative decoding).
**ROCm evaluated and dropped (2026-11-03):** same-commit llama-bench on Qwen3.8-27B Q4_K_M (-fa 1, KV q8_0/q8_0): ROCm pp512 1117 / pp8192 1060 / tg128 37.2 t/s vs Vulkan 932 / 877 / 39.3 → ROCm +20% prompt processing, Vulkan +6% generation; net ~wash for Cline with --cache-reuse. Adam's decision: stay on the original prebuilt Vulkan binary (`llama server`); ~/llama.cpp-rocm clone and builds removed. Don't re-propose ROCm unless Adam asks.

**Convention:** all agent output is from Qwen; no labelling needed. Prompt sizing for Qwen: it handled ~10–30 hits per gate with context reads without looping, but is verbose — keep gates to one logical step. Grade its classifications strictly (it states failure modes correctly but labels them softly) and check any "shown as raw output" claim.

## 3. Backlog & Known Debt  *(edit freely)*
*(Full verified list with line numbers: orchestration/BASELINE_AUDIT_REPORT.md.)*
- Hardcoded colors in non-widget code (SettingsDialogManager 14, HeatShimmerView 2, item_lunar_day 8, view_day_cell 6, dialog_lunar_detail 1); widget files need a palette decision (RemoteViews); 69 drawable files with hex to review.
- Widget layouts ship demo text (Tab 1–4, "Partly Cloudy", "18°C", …) — check visibility before first sync.
- Stale "Handle bar for BottomSheet" comment in dialog_lunar_detail.xml:16.
- **AlertNotificationHelper.kt:90-96 expiry display**: formats `alert.expires` with Instant.parse only; on failure shows the raw ISO string. Likely broken on Android 13 (minSdk 33; believed pre-JDK-12 libcore, where Instant.parse rejects "+02:00" offsets) for MeteoAlarm/offset-bearing expires; JVM unit tests would not catch it. Verify on an API 33 emulator; fix = OffsetDateTime.parse first (same pattern as GlobalAlertCache.isAlertActive). Same block hardcodes "Until further notice" and "TEST — " (localization rail violation). Related low-risk: AlertSyncWorker.kt:110 parses `sent` with Instant.parse, falls back to now().
- Rail violations (confirmed by both agents): LocationDialogManager uses BottomSheetDialog for search + recent-locations dialogs (no-bottom-sheets rail); LocationPreferences stores last-used and fixed-default lat/lon as Float (Double invariant). Fix hint: LocationPreferences already has private putDouble/getDouble (raw long bits, :399-410) with a Float fallback in getDouble — switching the four accessors to it may self-migrate, but getDouble's fallback only triggers on exception from getLong on a Float-typed key; verify migration behaviour in Step 0 (prefs schema change → plan first, AGENTS rule 10). Unused BottomSheetDialog import in ThemeManager.
- Merge duplicate SolarLightingEngineTest classes (test root vs test solar/) — must not reduce test count.
- No CI workflow (verifyEdgeToEdge + unit tests would be natural first jobs).
- SettingsDialogManager is ~2,300 lines (owns every settings screen) — candidate for a planned split (needs plan per AGENTS rule 10).
- docs/extensions.md (theme + forecast-provider manifests, schema v1) is an unimplemented draft; Phase V.
- Orchestrator cannot build locally (no Android SDK in its workspace) — Adam remains build authority.

## 4. Session Log  *(newest first, keep 10)*
### 2026-11-03 (late evening) — Alert re-notification fix
- Readback: HEAD 3d49db1 (docs committed; repo HANDOFF.md stale).
- Diagnosed repeated fog notifications (see §1). Step 0 PASS WITH NOTES (raw block elided with "..."; trace missed the active-advisory [A,U] case). Step 1 PASS WITH NOTES (no line numbers on quoted code; same-sent same-type tie is order-dependent — add id tie-break if touched again).
- Adam moved from Cline to Android Studio's Agent tab.
### 2026-11-03 (evening) — Runtime tuning, ROCm trial, handoff
- Confirmed llama.cpp runs on Vulkan (RADV); iGPU visible as Vulkan1 → all scripts pin `--device Vulkan0`.
- Installed ROCm (--no-dkms) + HIP build; same-commit llama-bench: ROCm +20% prompt processing, Vulkan +6% generation. Adam chose Vulkan; ROCm build and packages uninstalled (Adam also running apt autoremove; told to keep python3-pip/pkg-config if he uses them, and to verify `llama server --list-devices` still shows the 7900 XTX).
- Re-sent Adam AGENTS.md, AI_ONBOARDING.md, ARCHITECTURE.md, MASTER_EXAMPLE.md after he lost local copies to `git restore`.
### 2026-11-03 (afternoon) — Agent benchmark (Suite v1)
- Froze Suite v1 + answer key; no in-prompt corrections; AGENTS.md change tracking in §2 (no version stamp, per Adam).
- Qwen: all gates PASS / PASS WITH NOTES (orchestration/BENCHMARK_suite_v1.md).
- Gemma: scored run failed Gates 0.1 and 0.2 (0 tests reported; wrote files into repo and reported no changes; fabricated raw output). Adam retired Gemma permanently.
### 2026-11-03 — Baseline audit complete (Gemma)
- 0.1 PASS, 0.2 PASS (9/9), 0.3a PASS w/ notes, 0.3b PASS w/ notes (borderline), 0.3c FAIL (evidence integrity). One repetition loop before server fix (KV q8, -n 8192), none after.
- Verified codebase findings logged in §3 and orchestration/BASELINE_AUDIT_REPORT.md. AGENTS.md gained a no-editing-raw-output rule.
### 2026-11-03 — Baseline audit prompt
- Wrote 3-gate read-only calibration prompt (environment smoke test, codebase fact check with known answers, invariant sweep with genuine violations + decoys) and a private answer key.
### 2026-11-03 — Onboarding Claude as Orchestrator
- Cloned repo at b47934a and audited it against the Qwen-era docs.
- Found and fixed doc drift: dead GoldenHourActivity + 5 activity_* layouts already deleted; legacy AlertManager/AlertCache/AlertTestHarness already deleted; theme-engine now compileSdk 37 / Java 11 (mismatch resolved); stale proguard Room rule gone; 2x2→2x1 receiver rename done; backup rules now exclude all prefs (no longer templates); layout counts updated (15 section, 16 item, 19 dialog); added verifyEdgeToEdge gate, SystemBarContrastHelper floating-dialog exemption, and 0.8.0 hardening facts.
- Added hand-off protocol (AI_ONBOARDING §6) and this file.
