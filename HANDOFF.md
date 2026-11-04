# HANDOFF.md — Orchestrator Continuity Log
Maintained by the Orchestrator per AI_ONBOARDING.md §6. A new instance reads this third, after AI_ONBOARDING.md and ARCHITECTURE.md.

## 1. Current State  *(overwrite every update)*
| Field | Value |
|---|---|
| Updated | 2026-11-04 ~10:10 (MTP benchmark; app state unchanged) |
| App version | 0.8.2 (VC30) — committed and pushed; Play upload pending Adam's device check. VC29 = earlier 0.8.1.1 hotfix (skipped in changelog). |
| Repo HEAD at update | d6477d6 "0.8.2 VC30: fix double insets on cache/alert-log screens, restore single-line forecast labels" (code verified by orchestrator: 0 inset listeners in SettingsDialogManager, 88dp/72dp label columns). Committed ARCHITECTURE.md/HANDOFF.md at d6477d6 are OLDER than the Project copies — commit the Project copies. |
| Tests | 232 passing |
| Active task | None. 0.8.2 complete (alert re-notification fix + double-inset fix + restored single-line forecast labels). |
| Gate position | n/a |
| Awaiting | Adam: device check (alerts notify once / silent updates; Data Cache, Cache Inspector, full-content viewer, Alert Log headers sit under the status bar like System; "Wednesday"/"11:00 PM" one line at large font + French; Data Cache cards not clipped on the right — first screenshot looked clipped, possibly a crop); upload to Play with the release notes from this session; commit the refreshed docs; choose next task. |
| Last prompt issued | None outstanding. |
| Next action | On resume: State Readback, then next task. Recommended: notification text localization in AlertNotificationHelper (expiry via OffsetDateTime first; "Until further notice", "TEST — ", "Instructions:", "Description:", "Area:", "Expires:", "Source:", channel names/descriptions → EN/FR resources) + "Today"/"Tomorrow"/"Day N" in WeatherRepository.kt:603-608. Alternatives: bottom sheets → full-screen dialogs; Float coords → Double (prefs schema → plan first). |
| Agent / runtime | Qwen3.8-27B (local llama.cpp Vulkan) and Gemini 3.6 Flash (cloud, temporary) in Android Studio's Agent tab; fresh chat per prompt; commit only at task end. |

**Open decisions (Adam's call)**
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

- 2026-11-03 — (Adam) Alert Updates to an active alert replace the notification silently; re-alert (sound) only when severity increases. Cancel/expiry clears the notification.

- 2026-11-03 — (Adam) The ">5 files → plan first" limit was a legacy constraint from Alibaba's Qwen chat interface; removed. AGENTS.md change (f): rule 10 now only gates AlertProvider-interface and SharedPreferences-schema changes. Gates are still kept small for the local agent's sake. Current = (a)–(f).


- 2026-11-03 — (Adam) Gemini 3.6 Flash (cloud, via Android Studio Agent tab; Adam has Google Pro for ~1 month) allowed as a coding agent alongside Qwen — used from Step 3a when Qwen's session neared 120k context. Not Gemma; Gemma stays retired. Reports carry a "Generated by <model>" footer; audit per model.

- 2026-11-03 — (Adam) Fresh agent chat for every prompt. Orchestrator always issues full self-contained prompts (no bare "Proceed" variants); AI_ONBOARDING §2.3 updated. 

- 2026-11-03 — (Adam) Commit only when a whole task is finished; between steps work stays uncommitted (Adam uses `git restore .` to roll back). Prompts list all task-modified files in "Last verified"; AGENTS.md CONTEXT RESETS rule changed accordingly (change (g)) and agent forbidden from restore/checkout/stash/reset/clean. Commit line given at task end only.

- 2026-11-03 — (Adam) VC29 was already used by an undocumented 0.8.1.1 hotfix (not in git history at e9fa1fc). 0.8.2 ships as VC30; VC29 skipped in the changelog. Open: identify what 0.8.1.1 fixed and confirm the fix exists in the current tree.

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
- Model: Qwen3.8-27B Q4_K_M GGUF (Abiray), dense hybrid (DeltaNet + attention per Adam's script comment), llama.cpp server (--jinja, reasoning on, budget 2048, reasoning_effort medium, MTP speculative drafting --spec-type draft-mtp --spec-draft-n-max 5 — see MTP sweep note). Sampling: temp 0.7, top-k 20, top-p 0.80, min-p 0.
- Server script `qwen-server` (final, 2026-11-03): --device Vulkan0, -c 131072, KV q8_0/q8_0, -n 8192, --cache-reuse 256, --cache-ram 16384, log ~/llama-server-qwen.log. Original had KV q4_0 and no -n cap. Open: check the Qwen3.8 model card for thinking-mode sampling (earlier Qwen3 recommended 0.6/20/0.95 with thinking on).
- MTP draft sweep (2026-11-04, mtp_sweep.sh, llama.cpp build 12019 / 1052f7e1d, short prompts × 5 runs, verified per N from server log): no-draft baseline 39.0 t/s (= llama-bench tg128). Geo-mean t/s N1 60.0, N2 74.8, N3 82.5, N4 86.0, N5 86.5, N6 83.5, N7 79.2, N8 36.3. Code peaks N4–N5 (~100–105 t/s, tied within spread); prose peaks N3 (68 t/s) — acceptance per drafted token falls 0.75→0.25 (prose) vs 0.95→0.6 (code). Cost per check is content-independent: ~25.6 ms + ~3.8 ms per drafted token up to N7; N8 jumps to ~128 ms (9-token verify batch — likely a Vulkan kernel threshold), so N≥8 is unusable on this backend regardless of acceptance. Drafting adds ~1.1 GB VRAM + ~150 MB per N (22.3 GB at N5). Current setting N5 is correct for plain drafting. Next: speed_sweep.sh (draft p-min at N7/N8, -ub, f16 KV, min-p, long-context 31k-token prompt).
- Port 8080; stop any running server first (`pkill -f llama-server`) or it fails with "couldn't bind HTTP server socket".
- Capability (Suite v1, 2026-11-03): all gates PASS / PASS WITH NOTES. Strengths: accurate facts and line numbers, no false positives, discloses omissions rather than hiding them, unaltered raw blocks, arithmetic shown, reads enclosing code correctly, volunteers useful architectural observations. Weaknesses: overclaims that paraphrased context was "shown as raw output"; soft classifications (states the failure mode correctly but labels it LEGITIMATE); verbose (more context per gate). No loops observed.
- Server: revised qwen-server (KV q8_0/q8_0, -n 8192, -c 131072) — confirmed by Adam as the config used for Suite v1.

**Harness (from 2026-11-03 evening): Android Studio built-in Agent tab — Cline no longer used.** Keep file-edit and terminal-command approval manual in the new harness. Historical Cline notes below.
**Cline auto-approve (seen 2026-11-03, historical):** Read, Edit, Commands, Web Fetch, MCP all auto-approved in Act mode. Recommended: Edit OFF always (manual approval = review gate for edits); Commands OFF for read-only audits — Gemma wrote files via shell redirect (`> file`), which Edit approval does NOT catch; only Commands approval does. Both suite runs used the same settings, so the benchmark is unaffected.

**Harness:** Cline plugin for Android Studio → local llama.cpp server (OpenAI-compatible). Cline structures tool-call/JSON errors; raw Gemma tool tokens seen in a looped paste were likely the loop breaking Cline's tool-call parsing. Cline may condense/reset context on its own — continuation prompts must re-supply the prompt file if so.

**Hardware/backend (confirmed 2026-11-03):** llama.cpp runs on **Vulkan (RADV)**, not ROCm. Devices: Vulkan0 = RX 7900 XTX (24,560 MiB), Vulkan1 = Intel UHD 770 iGPU (shared RAM). All server scripts must pin `--device Vulkan0` or llama.cpp may split layers onto the iGPU. Qwen Suite v1 ran >70 t/s (GPU-resident, with MTP speculative decoding).
**GPU tuning (Adam, all benchmarks incl. Suite v1 and 2026-11-04 sweeps):** power limit 300 W (stock 325), voltage offset −25 mV, core clock 2800 MHz (stock 2900), memory offset +1300 (stock +1250). Generation is memory-bandwidth bound, so memory clock matters most. Open check: +1300 vs +1250 with mtp_bench (GDDR6 error retries can make an over-limit memory OC slower without crashing).
**ROCm evaluated and dropped (2026-11-03):** same-commit llama-bench on Qwen3.8-27B Q4_K_M (-fa 1, KV q8_0/q8_0): ROCm pp512 1117 / pp8192 1060 / tg128 37.2 t/s vs Vulkan 932 / 877 / 39.3 → ROCm +20% prompt processing, Vulkan +6% generation; net ~wash for Cline with --cache-reuse. Adam's decision: stay on the original prebuilt Vulkan binary (`llama server`); ~/llama.cpp-rocm clone and builds removed. Don't re-propose ROCm unless Adam asks.

**Agent C: Gemini 3.6 Flash (cloud, Android Studio Agent tab) — temporary, from 2026-11-03.** Step 3a: correct code, caught a spec gap (chainKey lost when a fresh provider copy replaced the cached one). Weaknesses seen: "..." inside a raw block despite explicit prohibition; a retyped diff (blank added line missing its +); one drive-by import removal.
**Convention:** check the "Generated by" footer on every report; profiles differ per model. Prompt sizing for Qwen: it handled ~10–30 hits per gate with context reads without looping, but is verbose — keep gates to one logical step. Grade its classifications strictly (it states failure modes correctly but labels them softly) and check any "shown as raw output" claim.

## 3. Backlog & Known Debt  *(edit freely)*
*(Full verified list with line numbers: orchestration/BASELINE_AUDIT_REPORT.md.)*
- Hardcoded colors in non-widget code (SettingsDialogManager 14, HeatShimmerView 2, item_lunar_day 8, view_day_cell 6, dialog_lunar_detail 1); widget files need a palette decision (RemoteViews); 69 drawable files with hex to review.
- Widget layouts ship demo text (Tab 1–4, "Partly Cloudy", "18°C", …) — check visibility before first sync.
- Stale "Handle bar for BottomSheet" comment in dialog_lunar_detail.xml:16.
- **AlertNotificationHelper.kt:90-96 expiry display**: formats `alert.expires` with Instant.parse only; on failure shows the raw ISO string. Likely broken on Android 13 (minSdk 33; believed pre-JDK-12 libcore, where Instant.parse rejects "+02:00" offsets) for MeteoAlarm/offset-bearing expires; JVM unit tests would not catch it. Verify on an API 33 emulator; fix = OffsetDateTime.parse first (same pattern as GlobalAlertCache.isAlertActive). Same block hardcodes "Until further notice" and "TEST — " (localization rail violation). Related low-risk: AlertSyncWorker.kt:110 parses `sent` with Instant.parse, falls back to now().
- Rail violations (confirmed by both agents): LocationDialogManager uses BottomSheetDialog for search + recent-locations dialogs (no-bottom-sheets rail); LocationPreferences stores last-used and fixed-default lat/lon as Float (Double invariant). Fix hint: LocationPreferences already has private putDouble/getDouble (raw long bits, :399-410) with a Float fallback in getDouble — switching the four accessors to it may self-migrate, but getDouble's fallback only triggers on exception from getLong on a Float-typed key; verify migration behaviour in Step 0 (prefs schema change → plan first, AGENTS rule 10). Unused BottomSheetDialog import in ThemeManager.
- **Hardcoded English day labels**: WeatherRepository.kt:603-608 returns "Today", "Tomorrow", "Day N" for the 7-day list — French users see English (localization rail). Needs a Context/string-resource path; data layer has none today.
- Merge duplicate SolarLightingEngineTest classes (test root vs test solar/) — must not reduce test count.
- No CI workflow (verifyEdgeToEdge + unit tests would be natural first jobs).
- SettingsDialogManager is ~2,300 lines (owns every settings screen) — candidate for a planned split (needs plan per AGENTS rule 10).
- docs/extensions.md (theme + forecast-provider manifests, schema v1) is an unimplemented draft; Phase V.
- Orchestrator cannot build locally (no Android SDK in its workspace) — Adam remains build authority.

## 4. Session Log  *(newest first, keep 10)*
### 2026-11-04 (morning) — MTP draft-length benchmark
- Readback: HEAD 033e1e9 (docs committed; stray .kotlin/sessions/*.salive committed — add `.kotlin/` to .gitignore + `git rm --cached` in next task's commit).
- Adam tried draft n-max 5. Wrote mtp_bench.py (speed/acceptance, not Suite v1 — spec decoding is verified by the main model, and Suite v1 is pinned to b47934a + AGENTS.md (a)–(d) + Cline, so no longer comparable). First runs all drafted 5 (qwen-server hard-codes --spec-draft-n-max 5; edits weren't reaching the server). Proper 0–8 sweep done; see §2b.
### 2026-11-03 (late night) — 0.8.2 VC30 UI fixes, handoff
- Found the 0.8.1.1 (VC29) label hotfix was never in git (lost to `git restore`); rebuilt it: 7-day day column 68→88dp, hourly time 54→72dp, maxLines=1 + uniform autosize.
- Double insets: Data Cache, Cached Data Inspector, full-content viewer, Alert Log each re-applied systemBars padding on top of SystemBarContrastHelper's decorView padding → removed the 4 local listeners (24 deletions).
- A Qwen run crashed after applying Step 1; the fresh agent correctly stopped on the unexpected diff; verified instead of redone.
- Release: versionCode 30, changelog txt/html updated, Play notes (en-CA/fr-CA/fr-FR, ≤500 chars) drafted. Committed d6477d6.
- Harness: Android Studio Agent sometimes blanks piped output and path-like lines; evidence now uses unpiped single commands.
### 2026-11-03 (night) — Alert fix completed, 0.8.2 VC30 prepared
- Step 3a (Gemini) chainKey + legacy-null normalization; 3b (Gemini) per-chain notifications, silent updates, escalation re-alerts, cancel clears; 3b-fix (Qwen) order-dependent AlertNotificationHelperTest fixed by ID lookup; Step 4 (Qwen) version 0.8.2/VC30, changelog txt/html, manual bullet. 232/232.
- Workflow changes: fresh agent chat per prompt; commit only at task end (agent forbidden from restore/stash/reset/clean); orchestrator gives `git commit -m` line at task end only.
- Harness quirk: Android Studio Agent output display sometimes blanks piped output and path-like lines (e.g. `git diff --stat`). Use unpiped single commands for evidence (`git diff -- <files>`, `git diff --shortstat`, single awk over the XML files for totals).
- Harness quirk (Qwen found): Android Studio Agent drops stdout when awk is the bare last command of a pipe — wrap in ( … ) or pipe to `| cat`.
- My errors this task: Step 2 test spec (b)/(c) counts; Step 4 grep excluded orchestration/ but the docs live at repo root.
### 2026-11-03 (late evening) — Alert re-notification fix
- Readback: HEAD 3d49db1 (docs committed; repo HANDOFF.md stale).
- Diagnosed repeated fog notifications (see §1). Step 0 PASS WITH NOTES (raw block elided with "..."; trace missed the active-advisory [A,U] case). Step 1 PASS WITH NOTES (no line numbers on quoted code; same-sent same-type tie is order-dependent — add id tie-break if touched again).
- Adam moved from Cline to Android Studio's Agent tab. AGENTS.md change (e).
- Step 2: my spec for tests (b)/(c) was wrong (expected 2, correct 1); agent caught it and stopped — good behaviour. Qwen again put "..." inside a raw block (test totals) — recurring rule 12 breach.
- Found latent risk: Gson leaves non-null Kotlin fields (msgType, references) null for legacy cache JSON → possible NPE in new reconcile; fixed in 3a.
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
