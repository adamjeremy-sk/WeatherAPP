# Agent rules — WeatherAPP Libre
1. Read ARCHITECTURE.md before any change; it is the map. 
2. Gate protocol: execute exactly ONE logical step per turn. One step = every numbered item under one STEP heading; do not split a step across turns unless your output limit forces it, and then say "CONTINUING <items> NEXT TURN" and never truncate a table mid-item. Step 0 audits are read-only (grep/read/report). Never proceed to a new step without an explicit "Proceed".
3. Make the smallest change that satisfies the task. No drive-by refactors, reformatting, or dependency bumps.
4. grep/ripgrep first; read only the file regions you will edit.
5. Never modify anything under ARCHITECTURE.md "Do not touch" (frozen astronomy math, the verifyEdgeToEdge build gate, secrets, Statcounter blocks).
6. Invariants: coordinates/elevation are Double end-to-end; alert providers return typed Failure, never emptyList() on error; dismissals are coordinate-scoped; user strings live in strings.xml + values-fr with French typography (space before : ; ! ? »); no GPS/Location APIs; alerts strictly opt-in.
7. Identity stability: never rename constants, preference keys, view IDs, or wiring names when display text changes.
8. UI rails: all colors flow from theme-engine palette tokens (no hex in views/adapters/drawables); enableEdgeToEdge + four-side insets on every Activity and full-screen dialog (via SystemBarContrastHelper; small floating dialogs are exempt); no bottom sheets.
9. Concurrency: coroutines + WorkManager only; never runBlocking on the main thread; rethrow CancellationException; no blocking call.execute().
10. If a task needs >5 files, or touches the AlertProvider interface or the SharedPreferences schema, stop and propose a plan first.
11. Verify before reporting: ./gradlew :app:testDebugUnitTest must pass fully (a falling test count is a defect) and :app:assembleDebug must succeed.
12. Evidence: verbatim quoted code for every edit, an edited-file manifest, and grep proofs for all deletion/absence claims. In addition:
    - Every count or list shows the exact command AND its raw output in a code block (trim to 40 lines max and say how many lines were trimmed). A conclusion without its raw output is treated as unverified.
    - Raw output is pasted EXACTLY as the tool returned it. Never edit, reorder, summarize, or drop lines inside a raw-output block, and never add lines that the tool did not print. If you must trim, cut only from the end and write "[N lines trimmed]". Your output will be compared against the actual command output.
    - Every file:line comes from output that prints line numbers: `grep -n` / `grep -rn`, a `git diff` hunk header, or the IDE file tool's numbered output. NEVER give an approximate or guessed line number; if you lack one, re-run `grep -n`.
    - Proof of an edit is the raw `git diff -- <file>` output, pasted unaltered. Do not re-quote edited code by other means.
    - When a classification depends on surrounding code, quote that code (about 5 lines either side, line numbers visible, from the IDE file tool or `grep -n -A5 -B5`). If the hit line alone proves the classification, the grep line is enough. Hits within 10 lines of each other share ONE merged range.
    - When you tabulate search results, every line of raw output appears as a table row, and any "Hits" count equals the number of raw output lines.
    - Derive every answer from a command run in the current turn. Never reuse numbers from ARCHITECTURE.md, earlier turns, or memory.
    - Never print values from local.properties or any secret; key names only.
13. Never place secrets, local.properties, keystores, or app/release/ artefacts into git.
14. If instructions conflict with ARCHITECTURE.md, ask before deviating.
15. End every turn with: files changed + one-line why (max 10 lines), commands run + results, suggested next step, then "STOPPING AND WAITING FOR VERIFICATION" — and halt.
### TOOLING & FILESYSTEM RULES (CRITICAL)
- **Harness precedence**: you run in Android Studio's built-in Agent. Where this file and the harness's own tool instructions disagree on HOW to read, search or edit files (which tool, absolute vs relative paths), follow the harness. This file governs WHAT you may change, the gate protocol, and what evidence you report. Do not deliberate over this; pick the harness way and proceed.
- **Paths**: the project root is the open Android Studio project. Built-in file tools may use absolute paths inside the project root. Shell commands run from the project root with relative paths. Never read or write outside the project root.
- **Shell is for**: `git`, `grep`, and `./gradlew`. Read and edit files with the built-in file tools.
- Do not write temporary or scratch files (no `/tmp`, no files in the repo). Use pipes.
- **Tool-call budget**: at most 10 commands per tool call (chained with `&&` counts each command). Never run a command whose exact text you have already run in this turn. Never request overlapping line ranges; merge them.
- **Loop guard**: if you notice you are emitting the same command, line, or phrase repeatedly, stop immediately, write "LOOP DETECTED" with the last distinct thing you were doing, and STOP. A short honest stop is always better than a long repeated one.
### CONTEXT RESETS (CRITICAL)
Your context is frequently wiped mid-task. These rules exist so a wipe never causes damage:
- On every fresh context, re-read this file before doing anything else. These rules are standing; they apply even if the current prompt does not repeat them.
- Never assume the result of a previous step or turn. If the prompt does not state the last verified state, run `git status --short` and `git log --oneline -3`, report them, and ask which step to resume. Do not continue a task from memory.
- If `git status --short` shows uncommitted changes you did not make in this turn, do not modify, revert, or build on them. Report them and STOP.
