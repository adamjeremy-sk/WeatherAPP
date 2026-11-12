# R0 milestone review (AGREED with Adam, 2026-11-12)
Written by the Orchestrator at the end of R0.2 (AI_ONBOARDING §2.7) and agreed in discussion with Adam on 2026-11-12. Binding conclusions are in AGENTS.md / AI_ONBOARDING.md as noted below.

## What was promised and what was done
- R0.0 process docs aligned with vermilion-editor (committed 41862a1, aa83b29, ffc2210). Done.
- R0.1 `.gitignore` (`/evidence/`, `.kotlin/`) and `.aiexclude`. Done; the `.aiexclude` effect is unconfirmed (the `@` picker hides git-ignored files anyway).
- R0.2 rails as tests: `ProtectedPathsTest` (18 files), `SizeRatchetTest` (8 files), `RailsComparerTest` (9 tests); 243 tests, 0 failures; plus a Gradle input `railsConfig` that the plan did not list. Done.
- R0.3 this review. Done (2026-11-12).

## What was learned
Process
- Multi-step prompts with an audit between steps worked: 5 steps, 2 small fixes, no rollback.
- Reading evidence files and sources on the machine caught things the pasted reports hid: an overwritten proof run, missing raw output, a stale status line.
- Gemini's reports drifted (dropped lines, prompt text mixed in) until a fixed template went into AGENTS.md rule 15; after that the report was complete.
- A correction to "Last verified" (a third modified doc) arrived after the prompt was pasted; send such corrections before the Coder starts.
- Adam's division of labour: the Coder writes all the code from parameters; the Orchestrator dictates only sourced data, and in the astronomy gates formulas and code too.
Technical
- Contracts plus named test cases produced correct code on the first try; the one defect was Kotlin `assert()` in a test (rule 20 now forbids it).
- A guard that reads files outside Gradle's inputs can be skipped as UP-TO-DATE; declaring the files as inputs was needed and was proven by a controlled change.
- The red run (guard fails while its config file is missing) shows the guards fire for the right reason.

## Decisions taken in this review (Adam, 2026-11-12)
1. **Second reader for HIGH gates: a fresh chat** (new chat in the WeatherAPP Project, Sonnet 5.5, High). Nothing is pasted: the desktop link gives every chat access to the repo folder (Adam works only at home on the desktop app), so the reviewer reads AGENTS.md, the gate prompt in `docs/milestones/`, `git diff` and `git status`, and the evidence files from the working tree. Procedure recorded in AI_ONBOARDING §1 (SECOND READER). A subagent is not used for HIGH gates.
2. **Gemini sizing stays conservative for R1.1** (about 10 hits needing context per gate, at most 10 commands per tool call, 4-5 light steps per prompt). R1.1 is cut into: Step 0 read-only audit, Step 1 red test, Step 2 fix, Step 3 green run and counts. It widens only after two clean gates on audit evidence (R0.1 clean, R0.2 needed two fixes: one clean gate so far).
3. **The MeteoGate key stays in `local.properties`** (option A). Rule 13 and the "never open" rails are the only guard until `.aiexclude` is proven; the risk is accepted (free public alert API; rotate if it leaks). Revisit if the Coder ever opens a secret file.

## Next milestone: R1 (alerts and privacy)
- Gates: R1.1 ECCC memo per location (HIGH; plan first if the memo entry type changes), R1.2 backup rules (HIGH), R1.3 Statcounter block (HIGH; privacy statements; Adam updates the Play Data Safety answers), R1.4 three small fixes, R1.5 review.
- Each HIGH gate gets the second reader (decision 1) before its commit.
- Open decisions needed before R1: none (D1 decided). D3, D4, D8 are needed before R2.
- AGENTS.md: no change needed after R0 (rules 15, 20 already added mid-R0).
