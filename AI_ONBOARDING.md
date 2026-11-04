# AI_ONBOARDING.md — Read First (Every Instance)
**Audience**: Any new Orchestrator (AI assistant) or Coding Agent instance.
**Purpose**: To establish the permanent architectural laws, behavioral protocols, hand-off procedure, and known traps of WeatherAPP Libre.
**Read order for a new Orchestrator**: AI_ONBOARDING.md → ARCHITECTURE.md → HANDOFF.md → (AGENTS.md, MASTER_EXAMPLE.md as reference).

## 1. Roles & Protocol of Interaction (CRITICAL)
*   **Adam (Owner)**: Verifies builds, runs device checks, approves gates, and commits.
*   **Orchestrator (Claude, in the claude.ai Project "WeatherAPP")**: Discusses tasks with Adam, authors gated prompts, audits agent output, and maintains these documents including HANDOFF.md.
    *   **PROTOCOL**: You **NEVER** stop to wait for the agent. When Adam pastes the Agent's output, you analyze the evidence and **immediately** output the *next* prompt. You only stop to ask Adam for a decision at explicit architectural crossroads.
    *   **AUDIT**: You have read access to the public repo (github.com/adamjeremy-sk/WeatherAPP). Audit pasted evidence first; once Adam pushes, verify claims against the actual source. You cannot push and normally cannot run Gradle (no Android SDK) — Adam is the build authority.
    *   **AUDIT VERDICT**: Every audit ends with one of: **PASS** (issue next prompt), **PASS WITH NOTES** (issue next prompt, carry notes forward), **FAIL** (issue a corrective prompt for the same step; do not advance the gate). Adam commits only when a whole task is finished (he uses `git restore .` to roll back in between); at that point give him a `git commit -m "…"` line in a code box (commit command only). Between steps nothing is committed, so every prompt's "Last verified" line lists all files the task has modified so far.
*   **Coding Agent (local LLM: Qwen3.8-27B via llama.cpp + Cline in Android Studio — see HANDOFF.md §2b for its profile)**: Executes code changes from AGENTS.md rules. **Gemma is permanently retired by Adam's decision; never use, suggest, or prompt for it.**
    *   **PROTOCOL**: The Agent executes exactly ONE logical step per turn and **MUST** end every response with "STOPPING AND WAITING FOR VERIFICATION". It has no memory between sessions.

## 2. The Gate System (Workflow Protocol)
Every non-trivial task executes under this protocol. No exceptions.
1.  **Step 0 (Read-Only Audit)**: Grep, read, and report. Zero code changes. Output a categorized hit report with file:line references. STOP AND WAIT.
2.  **Step 1..N (One Step at a Time)**: Execute exactly one logical step. Output verbatim quoted code, an edited-file manifest, and grep proofs. End with "STOPPING AND WAITING FOR VERIFICATION" and CEASE ALL GENERATION.
3.  **Self-Contained Prompts**: The agent is cleared between sessions. Every prompt must carry `CONTEXT (BINDING)`, `STRICT RAILS`, `STEPS`, and `EVIDENCE` requirements. See MASTER_EXAMPLE.md for structure.
    **Mandatory opening block.** Every fresh prompt (new task, or any prompt Adam may send to a freshly wiped agent) MUST begin with this block, verbatim except the bracketed line:
    ```
    FIRST: read AGENTS.md in the project root before doing anything else. Its rules are binding for this entire task and override anything below if they conflict. If you cannot read it, say so and STOP.
    Last verified: [one line — e.g. "Step 1 PASS, tree clean at <short HEAD>" or "New task; tree clean at <short HEAD>"]
    ```
    **Adam starts a fresh agent chat for every prompt** (keeps the local model's context small). So every prompt the Orchestrator issues is a full, self-contained fresh prompt; never a bare "Proceed". The "Last verified" line lists every file the task has modified so far (uncommitted) so the agent builds on them and stops on anything else.
4.  **Final step of every task**: build + full test run with exact counts, then the Documentation Collateral sweep (rail 9) unless Adam defers it.
5.  **Agent context wipes are frequent.** A correction given only inside a prompt is lost on the next wipe. Any correction that should outlive the current step goes into AGENTS.md (Adam commits it); the prompt may repeat it, but AGENTS.md is the durable copy. Every continuation prompt ("Proceed to STEP N") must state the last verified state in one line (e.g. "Last verified: Step 0.2 PASS, tree clean at b47934a") so a freshly wiped agent can resume without guessing.

## 3. Architectural Rails (Non-Negotiable)
1.  **Main Activity is a Thin Orchestrator**: Root Activity owns only wiring and Intent parsing. All business logic lives in dedicated Manager classes.
2.  **Strict Localization (Zero Hardcoded Strings)**: Every user-facing string lives in `strings.xml` AND `values-fr/strings.xml`. French typography enforced (space before `:`, `;`, `!`, `?`, `»`). No hardcoded English in Kotlin/XML.
3.  **Portable Theme Engine**: The `:theme-engine` module exposes only a palette object of typed color tokens. No hardcoded hex in views/adapters. Contrast derives from background luminance, not theme identity.
4.  **Window & Inset Discipline**: Every Activity and full-screen Dialog uses edge-to-edge with 4-side `WindowInsets` padding (dialogs via `SystemBarContrastHelper`; small floating dialogs are exempt). The `verifyEdgeToEdge` Gradle gate enforces this. **NO bottom sheets anywhere.**
5.  **Performance Invariants**: Debounced inputs. Background sync via WorkManager (cache-first). Long computations on `Dispatchers.Default`. **No work on the main thread that can be deferred.**
6.  **Identity Stability**: Internal constants, preference keys, and view IDs are **NEVER** renamed when display text changes. No drive-by refactors.
7.  **Migration Safety**: Removed features self-heal legacy preferences to safe defaults. Cached JSON is forward-compatible (unknown fields ignored).
8.  **RemoteViews (Widgets)**: Framework views only. PendingIntents use component-targeted intents with unique data URIs/request codes. Widgets invalidate synchronously from cache.
9.  **Documentation Collateral**: User-facing changes ship with matching updates to the website manual (usermanual.html), changelog (changelog.txt + changelog.html), and roadmap in the same sweep, unless explicitly deferred.

## 4. Project Summary & Component Map
**WeatherAPP Libre**: Privacy-first, GPLv3 Android weather & astronomy app (Kotlin, XML/ViewBinding, minSdk 33, targetSdk 37, R8 enabled).
*   **Forecasts**: Open-Meteo (OkHttp/Gson).
*   **Alerts**: ECCC Datamart (CAP-CP scraper) & MeteoAlarm (MeteoGate API).
*   **Astronomy**: On-device Meeus/Espenak math (**FROZEN/VERIFIED**).
*   **State**: `LocationPreferences` (SharedPreferences) & `GlobalAlertCache`.

| Component | Role & Rules |
|---|---|
| `*Manager` classes | Own ViewBinding & screen logic. |
| `WeatherRepository` | Open-Meteo fetch/merge/cache. **Warning**: Fetching must be strictly decoupled from widget drawing to prevent infinite refresh loops. |
| `AlertProviderRouter` | Routes country codes to `EcccProvider` or `MeteoAlarmProvider`. |
| `EcccProvider` | Scrapes Datamart CAP-CP. **Warning**: Requires strict adherence to live folder structures (base URL `/today/alerts/cap/`, SK maps to `CWWG`). |
| `AlertSyncWorker` | 15-min sync loop. **Warning**: Must use typed failures to prevent wiping the cache on network errors. |
| `WidgetSyncWorker` | 60-min widget sync. **Warning**: Must not use `runBlocking` or touch the network during UI redraws. |
| `solar/`, `lunar/` | **FROZEN**. Verified math cores. Do not modify. |

## 5. Known Traps & Common Agent Drifts
*   **Drift**: Replacing one hardcoded string with another instead of externalizing to `strings.xml`.
*   **Drift**: Renaming IDs or constants when only display text changes.
*   **Drift**: Proceeding to the next step without waiting for Adam's authorization.
*   **Drift**: Acting on stale docs. If ARCHITECTURE.md disagrees with the repo, the repo wins — flag it and fix the doc.
*   **Trap (Coordinates)**: `Double` end-to-end. `Float` truncation causes 5-min solar errors.
*   **Trap (Empty Wipes)**: Providers returning `emptyList()` on error wipes the cache. Must use typed `AlertFetchResult.Failure`.
*   **Trap (Datamart Truth)**: Base URL is `/today/alerts/cap/`. SK maps to `CWWG`. Files are in `/HH/` folders.
*   **Trap (Expiry Parsing)**: `expires` fields may have offsets (`+02:00`). Use `OffsetDateTime.parse`, not `Instant`.
*   **Trap (Dismissals)**: Scoped to coordinates (`app_alert_dismissed_<lat>_<lon>_<id>`).
*   **Trap (Floating dialogs)**: Do not push small floating dialogs through the full-screen edge-to-edge helper (0.8.1 off-screen regression).

## 6. Hand-off Protocol (Orchestrator continuity)
Orchestrator chats end (context limits, new day, model switch). HANDOFF.md is the single source of truth for "where are we". It lives in the claude.ai Project (so every new chat in the Project sees it) and in the repo for history.

**6.1 Session start (new Orchestrator instance)**
1. Read AI_ONBOARDING.md, ARCHITECTURE.md, HANDOFF.md.
2. Pull the repo and compare HEAD with the commit recorded in HANDOFF.md. If they differ, read `git log` between them and note what changed.
3. Reply to Adam with a **State Readback** (max 8 lines): version/HEAD, active task, gate position, what is awaited next, open decisions, and any drift found. Do nothing else until Adam confirms or corrects it.
4. If the active task is mid-gate, resume from the "Next action" field — typically re-issuing or continuing from the recorded last prompt.

**6.2 Updating HANDOFF.md (Orchestrator)**
Update it (and write it back to the Project) at each of these moments, without being asked:
- a task completes its final gate;
- Adam makes an architectural decision;
- Adam says "handoff", "wrap up", or that he's ending the session;
- the conversation is getting long (risk of context loss).
Rules: overwrite §1 Current State entirely; append one dated line per decision to §2; edit §3 Backlog; prepend a §4 Session Log entry and keep only the 10 newest. Record the last issued prompt verbatim in §1 when a task is mid-gate — a new instance cannot recover it otherwise.

**6.3 Adam's side**
- To start a new chat: open it inside the "WeatherAPP" Project and send "Resume from HANDOFF.md."
- After a handoff update, commit the refreshed HANDOFF.md (and any changed docs) to the repo.
