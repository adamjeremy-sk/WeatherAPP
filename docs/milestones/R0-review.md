# R0 milestone review (DRAFT for discussion with Adam, 2026-11-12)
Status: not agreed. Written by the Orchestrator at the end of R0.2 (AI_ONBOARDING §2.7); conclusions that bind future work move into AGENTS.md or AI_ONBOARDING.md after Adam agrees.

## What was promised and what was done
- R0.0 process docs aligned with vermilion-editor (committed 41862a1, aa83b29, ffc2210). Done.
- R0.1 `.gitignore` (`/evidence/`, `.kotlin/`) and `.aiexclude`. Done; the `.aiexclude` effect is unconfirmed (the `@` picker hides git-ignored files anyway).
- R0.2 rails as tests: `ProtectedPathsTest` (18 files), `SizeRatchetTest` (8 files), `RailsComparerTest` (9 tests); 243 tests, 0 failures; plus a Gradle input `railsConfig` that the plan did not list. Done.
- R0.3 this review. Open.

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

## Next milestone: R1 (alerts and privacy)
- Gates: R1.1 ECCC memo per location (HIGH, alert pipeline; plan first if the memo entry type changes), R1.2 backup rules (HIGH), R1.3 Statcounter block (HIGH; privacy statements; Adam updates the Play Data Safety answers), R1.4 three small fixes, R1.5 review.
- Risks: the HIGH gates need the second reader (a cold review in a fresh context): how is it run? Gemini's sizing stays conservative (about 10 hits per gate) until two clean gates are on record; R0.1 was clean, R0.2 needed two fixes.
- Open decisions needed before R1: none (D1 is decided). D3, D4, D8 are needed before R2.
## Questions for Adam
1. Second reader for HIGH gates: a new Sonnet chat given only AGENTS.md, the prompt, `git diff` and the evidence files, or a subagent here?
2. Keep the Gemini sizing as is for R1.1?
3. Should the MeteoGate key move out of `local.properties` so the cloud Coder cannot reach it (the `.aiexclude` effect is unproven)?
