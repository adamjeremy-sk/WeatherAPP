# Meeus Chapter 47 tables (Moon longitude, distance, latitude) — source file for gate R2.2

**Status: verified source data, not yet used by the app.** Prepared by the Orchestrator on 2026-11-12 for ACTION_PLAN gate R2.2 (decision D2, option b: an openly licensed implementation, fetched, then checked).

## Provenance
- The two tables are Table 47.A (periodic terms for longitude and distance, 60 rows) and Table 47.B (latitude, 60 rows) of Jean Meeus, *Astronomical Algorithms*, 2nd ed.
- Transcribed from the `ta` and `tb` arrays in `src/moonposition.js` of the **astronomia** package (https://github.com/commenthol/astronomia), file header: `@copyright 2013 Sonia Keys`, `@copyright 2016 commenthol`, `@license MIT`. Fetched 2026-11-12 from `raw.githubusercontent.com/commenthol/astronomia/master/src/moonposition.js`. The repository-wide licence was not read; the file's own header is MIT.
- The CSV files here are a mechanical conversion of those arrays (same row order, same numbers). No row was typed by a model; the transfer was checked as below.
- **Attribution**: when the Coder copies these numbers into Kotlin, the app's open-source licences page (`licenses.html`) and the file's header comment name the astronomia package (Sonia Keys, commenthol, MIT) as the transcription source and Meeus as the origin of the tables. MIT is compatible with the app's GPLv3.

## Columns
- 47A: `D, M, M', F` multipliers, then the coefficient of `sin(argument)` for the longitude sum (unit 1e-6 degree) and of `cos(argument)` for the distance sum (unit 1e-3 km). Terms with |M| = 1 are multiplied by E, |M| = 2 by E².
- 47B: `D, M, M', F`, then the coefficient of `sin(argument)` for the latitude sum (1e-6 degree), same E rule.
- The additive terms (A1, A2, A3 and the L'-F, L'-M', L'+M' corrections) and the fundamental arguments are *not* in these CSV files; they are in the book (Chapter 47) and in the existing `LunarPosition.kt`. The oracle script contains the versions the check used.

## Verification done (2026-11-12)
1. Row counts: 60 rows each; all 60 argument tuples distinct in each table.
2. **Meeus Example 47.a** (1992-04-12 0h TD, JDE 2448724.5), reproduced by `oracle_47a.py` with these tables: 
   - ΣL = -1127527.03 (book -1127527), ΣR = -16590875.18 (book -16590875), ΣB = -3229126.42 (book -3229126)
   - λ before nutation 133.162655° (book 133.162655°), β -3.229126° (book -3.229126°), Δ 368409.68 km (book 368409.7 km).
3. Independent check, `oracle_phases_nov2026.py`: with these tables and the low-precision Sun, the four November-2026 principal phases land within 0.26 to 0.86 min of the published list used in the astronomy audit (listing resolution is 1 min; ΔT assumed 69 s).
4. Not independently verified: the T², T³, T⁴ coefficients of the fundamental arguments (they matter below 0.0001° before 2100), and the additive-term constants beyond what Example 47.a exercises. Gate R2.2 adds golden tests for them from the book.

## How R2.2 uses this
- The Orchestrator gives the Coder these two CSV files as the exact source, plus the contract (60/60 rows, argument order `D, M, M', F`, E handling). The Coder converts them into the Kotlin arrays and may not alter a number; the gate's row-count test and Meeus Example 47.a (at 0.001° and 0.1 km) are the proof, and the Orchestrator diffs the Kotlin numbers against the CSV by script before the verdict.
- Provenance rule (AGENTS.md rule 24): these numbers are sourced from this directory, not from any model's memory.

SHA-256 of the CSV files at preparation time are listed in `SHA256SUMS`.
