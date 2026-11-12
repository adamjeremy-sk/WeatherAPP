# Human / Orchestrator / Coder drift: WeatherAPP vs vermilion-editor (2026-11-12)

Requested by Adam. Method: read the process documents of `~/Projects/vermilion-editor` (AGENTS.md, opencode.json, AI_ONBOARDING.md, docs/milestones/M0-review.md and M1-review.md, docs/roadmap.md, ARCHITECTURE.md §9, HANDOFF.md) and compared them with WeatherAPP's orchestration docs (AGENTS.md, AI_ONBOARDING.md, HANDOFF.md, ARCHITECTURE.md). Companion documents: `CODE_AUDIT_2026-11-12.md`, `ASTRONOMY_AUDIT_2026-11-12.md`.

## 0. Limits of this analysis

- I read **documents, not chat transcripts**. I can see what the process says and when it changed, not what was said in the sessions.
- The Orchestrator wrote most of these documents, so they are **self-report**. "The process works" is the Orchestrator's claim; I checked it only where the repo gives independent evidence (test counts, commits, files).
- vermilion-editor's HANDOFF.md (138 KB) was **skimmed** (sections 1, 2 and 4), not read in full.
- No git history of the vermilion chats exists in what I read, so the "when" of each change comes from document dates and commit counts, not from conversations.

## 1. Bottom line

The three roles are not the same roles they were when WeatherAPP's process was written (docs frozen 2026-11-03/04). In the following eight days vermilion-editor made 89 commits and the process moved in one consistent direction: **the Human and the Coder gained a voice and rules moved from prose into machinery, while the Orchestrator became the author and verifier of nearly everything.** WeatherAPP's docs stayed at the earlier stage and lack the rules that vermilion learned by getting burnt.

The WeatherAPP astronomy audit is a concrete case of the cost: a "FROZEN / VERIFIED" label that no test backs up (see §5).

## 2. Timeline in numbers

| Measure | WeatherAPP (docs frozen 11-03/04) | vermilion-editor (11-05 to 11-12) |
|---|---|---|
| AGENTS.md | 15 rules | 22 rules, 9 KB → 13.4 KB |
| AI_ONBOARDING.md | short | 13 KB → 25 KB |
| HANDOFF.md | dated 2026-11-04 | 13 KB → 138 KB (about 35k tokens to read on every resume) |
| Tests | no accuracy tests (ASTRONOMY_AUDIT M2) | 67 → 100 → 345 |
| Enforcement | prose | opencode.json deny-list, ctest rails, CI, evidence files |
| Commits in that window | n/a | 89 |

## 3. The six drifts

### D1. The Human moved from approver to co-designer, and rules arrive by dated decree

WeatherAPP's docs cast Adam as the person who approves and builds. In vermilion, AI_ONBOARDING §2.9 and AGENTS rules 16-22 are labelled "Adam, <date>": Adam is now setting process rules directly and often, after each failure. This works (each rule has a story behind it), but it makes the rulebook a log of incidents rather than a design, and the order of rules is chronological.

### D2. The Orchestrator moved from auditor to author-and-verifier

In vermilion the Orchestrator dictates file lists, runs compile, mutation and fuzz checks, computes sha256 of evidence, sweeps all docs after each gate, owns the roadmap and milestone reviews, and writes reference implementations that the Coder then types or adapts. The AGENTS rule that the Orchestrator "never edits code" and the practice of dictating the code are in tension: the line between "dictates" and "writes" is now thin. For astronomy this has a good side (the oracle method, §5) and a risk: the Orchestrator's recalled constants end up in the repo, as the lunar tables in H1 of the astronomy audit probably did.

### D3. The Coder moved from typist to trial implementer with a voice

New in vermilion: **rule 22 "Prompt doubts"** (the Coder must report any doubt about a prompt instead of guessing), a stop-if-"Last verified"-mismatch check before starting, continuation fixes inside a gate, and a trial in which the local Qwen model writes implementations rather than only applying dictated edits. Opencode runs `qwen3.8-27b` on llama.cpp with a 131,072 context. This makes the Coder safer (it can refuse) and more capable (it can implement), and WeatherAPP's docs have neither the doubt channel nor the stop condition.

### D4. Enforcement moved from prose to machinery

| Prose in WeatherAPP | Machinery in vermilion |
|---|---|
| "Do not run git push" etc. | `opencode.json` denies git push, commit, checkout, restore, reset, clean, stash and related commands |
| "Keep files small", "format code" | `SourceCheck.Repo` and `Format.Check` ctest rails (rules 16 and 19) |
| "Report what you ran" | evidence files (rule 18), sha256 computed by the Orchestrator |
| "Verify before you claim" | CI and a frozen-original oracle |

The principle: an agent's compliance should not depend on remembering a sentence. WeatherAPP's equivalents are all sentences. Notably, the freeze on `lunar/` and `solar/` is itself only a sentence (AGENTS rule 5); nothing blocks a coding agent from editing them.

### D5. Blame moved from the Coder to the Orchestrator's prompts, and memory moved into one huge file

The M0 review says most failures "came from the prompts, not from Qwen". That is a healthy diagnosis and it changed practice (Prompt doubts, stop conditions). The cost: continuity now lives in HANDOFF.md, which has grown 10x in eight days. A fresh Orchestrator spends about 35k tokens reading it before doing anything, and the early part is stale. WeatherAPP's HANDOFF.md is the opposite problem: dated 11-04, with a §3 backlog that is carried debt.

### D6. WeatherAPP's docs stalled, and some of their rules now contradict vermilion's

| WeatherAPP | vermilion-editor | Consequence |
|---|---|---|
| AI_ONBOARDING: "PASS WITH NOTES, carry notes forward" | AGENTS rule 17: **no carried debt**; fix loops close inside the gate | WeatherAPP's HANDOFF §3 backlog is exactly the debt vermilion forbids. Several audit findings (CODE_AUDIT) are items that were carried. |
| "Fresh agent chat per prompt" | multi-step prompts with continuation fixes and CONTEXT RESET rules | A fresh chat loses the gate state; the Coder re-derives things (or re-invents them). |
| No evidence files | Rule 18 evidence files per gate | WeatherAPP's "verified" has no artefact behind it. |
| No file-size or format rule | Rules 16 and 19 enforced by ctest | WeatherAPP has large files (see CODE_AUDIT). |
| No "Prompt doubts" | Rule 22 | A Coder that doubts a prompt can only comply or guess. |

## 4. What drifted well and what drifted badly

**Good drift (keep, port):** machinery over prose; Prompt doubts; stop-on-mismatch; evidence files; the oracle method; milestone reviews that blame prompts; separating "Do not touch" from correctness (vermilion's ARCHITECTURE §9 lists protected files, and makes no correctness claims about them).

**Bad drift (contain):** the 138 KB HANDOFF; rules as dated decrees without a hierarchy; the Orchestrator verifying its own work (compile, mutation, fuzz, sha256 and the doc sweep all by the same author; a second check by someone else is not in the process); the thin line between "dictating" and "writing" code.

## 5. Link to the astronomy audit

WeatherAPP's freeze began as a guardrail: keep the coding agent from "fixing" UI problems by touching formulas (Adam's own words). Over time the docs added "VERIFIED ≤1 min / ≤1°" (ARCHITECTURE.md:80, AI_ONBOARDING.md:44 and :55, AGENTS rule 5). A label of protection became a label of correctness, with nothing under it. The astronomy audit found the Moon series wrong (H1), phases up to 47 min off (H2), sunrise at the wrong altitude (H3), and tests with tolerances of one day and 30 minutes (M2).

vermilion-editor shows the way out of the conflation:

- **Protection is a list** (ARCHITECTURE §9, plus the `opencode.json` deny-list), not a claim.
- **Correctness is a test against an independent oracle** (a frozen original run side by side), and a number in a doc must be reproducible from a file in `evidence/`.

For astronomy the equivalent oracle is a published value, not another copy of the code: Meeus Examples 47.a and 49.a, NASA eclipse times, an almanac table.

## 6. Recommendations (all decisions are Adam's)

1. **Port the proven vermilion rules to WeatherAPP**, adapted to Kotlin and Gradle: no carried debt (17), evidence files (18), Prompt doubts (22), stop on "Last verified" mismatch, file-size and format rails (16, 19 via a Gradle check or a unit test over the repo). Drop "carry notes forward" and "fresh chat per prompt", or restate them so they do not contradict the ported rules.
2. **Split the freeze label into PROTECTED and VERIFIED** (see the astronomy audit §6). PROTECTED is machine-enforced where possible: a deny-list in the coding agent's config for `lunar/` and `solar/` edits, and a test that fails if those folders change without a gate.
3. **Require independent numerical verification** before any "VERIFIED": golden tests built from published sources, with tolerances taken from the claim (1 min ⇒ 60 s), never from the current output.
4. **Cap HANDOFF.md.** Keep a one-page current-state file and move history into dated archives, in both projects. For vermilion, a roll-up of the 138 KB into the last two milestones.
5. **Clarify the Orchestrator's code boundary.** Decide whether "never edits code" means "never commits code" or "never writes code in prompts". If the Orchestrator dictates tables or constants, they must be copied from a primary source by a person or be tested against one (the recalled-table risk).
6. **Add a second reader.** One independent check per milestone (a person, a different model, or a script) on claims the Orchestrator made about its own gate.

## 7. Open questions for Adam

- Is the Qwen-writes-implementations trial in vermilion meant to become the model for WeatherAPP, or to stay a local experiment? The answer decides how much of rule 22 and the stop conditions apply.
- Do you want the freeze enforced mechanically for `lunar/` and `solar/` (deny-list plus a repo test), or only relabelled in the docs?
- Should WeatherAPP's HANDOFF be rewritten fresh, or rebuilt from vermilion's current format?
