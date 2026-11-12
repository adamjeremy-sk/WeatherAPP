# Astronomy audit: lunar/ and solar/ (2026-11-12)

Read-only audit of `app/src/main/java/com/weatherapp/libre/{lunar,solar}/` (18 files, 2,694 lines) and their tests (20 files, 1,473 lines), requested by Adam, who said the freeze exists "to prevent the coding agent from trying to fix UI stuff by adjusting the formulas". No code was changed.

## 0. Bottom line

1. **The freeze was a guardrail, but the docs turned it into a certificate.** ARCHITECTURE.md (lines 23, 24, 80), AI_ONBOARDING.md (lines 44, 55) and AGENTS.md rule 5 say "FROZEN / VERIFIED, ≤1 min / ≤1° vs official sources". No test in the repo measures that. Part of it is false.
2. **The Moon series is wrong** (`LunarPosition.kt`). Against NASA's greatest-eclipse times it is off by +3.5, +22.1 and -12.3 minutes. The full Meeus Ch. 47 table is off by under 1 minute on the same three eclipses.
3. **Moon phase times are wrong by up to 47 minutes** (new moon, over the 13 lunations of 2026) and **eclipse times by up to 22 minutes** (in the three eclipses checked), both inherited from the Moon series. This month the app's phase times differ from a published list by -8.5, +24.0, +17.2 and +9.4 minutes.
4. **Sunrise and sunset are computed at the wrong altitude.** The solver lands exactly at true altitude -1.0000° (a discontinuity in the refraction function), not the standard -0.8333°. For Ottawa on 2026-11-12 the app gives 06:55:56 / 16:37:29; sunrise-sunset.org lists 06:57:33 / 16:35:46.
5. **The solar math core is sound.** Using the standard definition, my port of the app's Sun position code matches sunrise-sunset.org to 4 s and 2 s. What is off is the definition, the elevation-dip policy and the Moon.

## 1. Scope, method and limits

- I could not build or run the app (no Android SDK or Gradle on the device). Nothing here is a test run of the app.
- Method: read every file; mechanically transliterated the Kotlin into Python (the Moon tables are parsed straight from `LunarPosition.kt`, so no retyping errors); compared against references.
- **References, and how far to trust them:**
  - *Meeus Ch. 47 and 49 tables, reproduced from my memory.* They reproduce Meeus's published Example 47.a sums exactly (ΣL = -1127527, ΣR = -16590875, the book's values) and the Ch. 49 new-moon Example 49.a to 5 s. That is strong evidence for the longitude and distance tables. My latitude table (Table 47.B) is **not** exact (β is 8″ off at the example), so I draw no conclusion from β.
  - *NASA greatest-eclipse times* copied from the repo's own `LunarEclipseCalculatorTest.kt`.
  - *Phase times* for Nov 2026 from time-ok.com (page captured while Ottawa was on UTC-4; I assumed UTC-4; the reference series agrees with all four to within 1 min, which supports that assumption).
  - *Sunrise/sunset* for Ottawa on 2026-11-12 from sunrise-sunset.org (one date, one city).
- ΔT (2026) of about 69 s is from my memory of IERS data. The USNO page I fetched had no numbers. Treat L1 below as unverified.
- Adam should confirm the Moon-table findings against his own copy of Meeus (Example 47.a, Table 47.A) before anyone changes code. See section 7 for checks that do not depend on me.

## 2. Scorecard

| File | Verdict | Notes |
|---|---|---|
| `AstroTime.kt` | OK | JD/TT conversions correct; UTC≈UT1 is documented. |
| `AstronomyMath.kt` | OK | GMST (12.4), mean obliquity, low-precision nutation, ecliptic→equatorial all match Meeus. |
| `DeltaT.kt` | OK, one caveat | All 15 branches match the Espenak–Meeus polynomials. The 2005–2050 branch is a prediction (L1). |
| `AtmosphericRefraction.kt` | Formula OK, claims wrong | Saemundsson (Meeus 16.3), labelled "Bennett / 16.4". The -1° discontinuity is real and does matter for the Sun (H3). |
| `lunar/SolarPosition.kt`, `solar/SolarPositionEngine.kt` | OK, small defect | Meeus Ch. 25. Nutation counted twice (L2). |
| `LunarPosition.kt` | **Wrong** | H1. |
| `MoonPhaseCalculator.kt` | **Wrong** | H2. |
| `LunarEclipseCalculator.kt` | Geometry plausible; times wrong because of H1 | Header's "~10″ / ~20 s" error budget is false. |
| `MoonRiseSetCalculator.kt` | Logic OK; ≤1.7 min error from H1 | Ignores elevation dip (M1). |
| `TopocentricConverter.kt` | OK | Meeus Ch. 40. Computes a dip-aware threshold that nothing uses (M1). |
| `SolarRiseSetSolver.kt` | **Wrong definition** | H3, M1. Brent implementation is correct. |
| `SolarLightingEngine.kt` | Works; UI text inside the "frozen" core | L6. |
| Tests | Do not measure accuracy | M2. |

## 3. Findings

### H1 (High). The lunar longitude series is truncated and partly mangled

`LunarPosition.kt:63-96` has 34 of the 60 terms of Meeus Table 47.A. The latitude table has 13 of 60 (lines 109-121) and the distance table 15 of 60 (lines 134-148). Worse, the longitude rows are corrupted from line 67 on: the right coefficients are attached to the wrong arguments.

| Line | In the app (D, M, M′, F → coefficient) | Book argument for that coefficient |
|---|---|---|
| 67 | (-2, 0, 1, 0) → -185116 | (0, 1, 0, 0) |
| 68 | (0, 1, 0, 0) → -114332 | (0, 0, 0, 2) |
| 69 | (0, 0, 0, 2) → -58793 | (2, 0, -2, 0), and the sign is +58793 |
| 70 | (2, 0, -2, 0) → 57066 | (2, -1, -1, 0) |
| 71 | (2, -1, -1, 0) → 53322 | (2, 0, 1, 0) |
| 72 | (2, 0, 1, 0) → 45758 | (2, -1, 0, 0) |
| 73 | (2, -1, 0, 0) → -40923 | (0, 1, -1, 0) |
| 74 | (0, 1, -1, 0) → -34720 | (1, 0, 0, 0) |
| 75 | (1, 0, 0, 0) → -30383 | (0, 1, 1, 0) |
| 76 | (0, 1, 1, 0) → 15327 | (2, 0, 0, -2) |
| 77 | (2, -1, 1, 0) → -12528 | (0, 0, 1, 2) |

From line 86 on the rows deviate further. The distance table also has `(0,0,0,2) → -31490` at line 139 (book: -3149) and `(2,-1,1,0) → 10321` at line 147 (book: argument (2,0,0,-2)). Lines 120-121 in the latitude table look swapped; I could not validate that table (see section 1).

The fifth-largest term (the annual equation, -185116·sin M) and the 2F term (-114332) are among the ones attached to the wrong arguments.

**Evidence**

| Check | App | Reference |
|---|---|---|
| Meeus Example 47.a (1992-04-12 0h TD): λ before nutation | 133.1440° | 133.1627° (book) |
| Same: Δ (distance) | 368401.1 km | 368409.7 km (book) |
| Same: ΣL | -1,146,173 | -1,127,527 (book) |
| Moon longitude error, every 3 h through 2026 | mean 0.128°, median 0.101°, 95th percentile 0.325°, **max 0.453°** | full table |
| Greatest eclipse 2025-03-14 vs NASA 06:58:43 | +3.5 min | +0.7 min |
| Greatest eclipse 2024-09-18 vs NASA 02:44:18 | **+22.1 min** | +0.4 min |
| Greatest eclipse 2024-03-25 vs NASA 07:12:51 | **-12.3 min** | +0.3 min |

The mangled rows are in the very first commit of the file (`4dfbde4`, 2026-09-28, "Changed moon phases from XML to calculations"). I cannot tell how the table was produced. Right coefficients with wrong arguments is the pattern you get from a table recalled rather than copied. It has never been correct.

### H2 (High). Moon phase times are wrong, and a refinement loop hides the second bug

`MoonPhaseCalculator.computeMeeusPhaseJde` (lines 60-167) does two things.

1. **The Ch. 49 formula is itself wrong.** On Meeus's Example 49.a (new moon, k = -283) the app's own formulas give JDE 2443192.64011; the book gives 2443192.65118, a 16-minute miss. The causes I could confirm: the sign of the `E·sin(M′-M)` term is negative (line 86, and line 105 for the full moon) where the book has it positive; `0.01044` on line 85 where the book has `0.01039`; and the quarter-phase block (lines 119-127) uses coefficients (`-0.38662`, `0.16524`, …) that do not match the book's (`-0.62801`, `0.17172`, …). Unrefined, the first and last quarters are off by 3.5 hours on average and up to 6. The table of planetary arguments is also truncated (8 of 14 terms).
2. **Commit `cd08cc8` (2026-09-29, the day after the series first appeared) added a three-step Newton refinement** (lines 152-164, divisor 12.190749 °/day) that is not part of Meeus. It pulls every phase onto the app's own Moon and Sun longitudes. That removes the multi-hour quarter errors but replaces the Ch. 49 accuracy with the H1 error.

**Result over the 13 lunations of 2026** (app vs. the correct longitude series with the same Sun):

| Phase | Mean error | Worst |
|---|---|---|
| New moon | 25.2 min | 46.8 min |
| First quarter | 12.8 min | 21.9 min |
| Full moon | 7.3 min | 18.4 min |
| Last quarter | 16.0 min | 30.1 min |

**This month (Nov 2026) against a published list** (times converted to UTC on the UTC-4 assumption; app logic vs list; the correct series is within 1 min on all four):

| Event | App | List (UTC) | Difference |
|---|---|---|---|
| Last quarter, Nov 1 | 20:19 | 20:28 | -8.5 min |
| New moon, Nov 9 | 07:26 | 07:02 | +24.0 min |
| First quarter, Nov 17 | 12:04 | 11:47 | +17.2 min |
| Full moon, Nov 24 | 15:02 | 14:53 | +9.4 min |

(My Python replays the app's logic from the listed time rather than from the app's own Ch. 49 starting estimate; the Newton loop converges to the same place, so the figures stand to within a minute.)

The phase *name* and illumination shown to users are barely affected (a 0.13° elongation error is about 0.1% illumination). The *times* of the four principal phases are what is wrong. `ageDays` is computed from the same refined new-moon time, so it is internally consistent but shifted by the same error.

### H3 (High). Sunrise and sunset converge on the refraction discontinuity, not on -0.8333°

`SolarRiseSetSolver.findAllCrossings` (lines 230-262) finds where the *apparent* altitude (true altitude plus refraction) equals the target. The target for sunrise/sunset is `-0.8333` (`LightingDefinitions`, `SolarDataTypes.kt:6`), which is the standard **true**-altitude definition (34′ refraction + 16′ semi-diameter). Comparing it with the refracted altitude double-counts refraction. On top of that, `SolarPositionEngine.calculateRefraction` (lines 119-125) returns 0 below true altitude -1.0°, so the apparent altitude jumps from -1.0° to about -0.35° at true -1.0°. The target -0.8333° sits inside that jump, so Brent bisection converges on the jump itself.

Confirmed numerically: for Ottawa on 2026-11-12 with elevation 0, the solved instant has **true altitude -1.0000°** and apparent -0.3517°; the standard sunrise is at true -0.8333°.

| Ottawa, 2026-11-12 (EST) | Sunrise | Sunset |
|---|---|---|
| App as shipped (elevation 70 m) | 06:55:56 | 16:37:29 |
| App logic, elevation 0 | 06:56:34 | 16:36:51 |
| Standard definition, sea level (my port of the app's Sun code) | 06:57:37 | 16:35:48 |
| sunrise-sunset.org | **06:57:33** | **16:35:46** |

So the Sun position, GAST, topocentric and time pipeline is right (4 s and 2 s off the reference). On 2026-06-21 and 2026-12-21 the pattern is the same: at elevation 0 the app is 1.1 to 1.2 min early at sunrise and late at sunset, and 1.8 to 1.9 min with Ottawa's 70 m.

The code comment on the discontinuity (`AtmosphericRefraction.kt:15-16`: "lies strictly below all rise/set event thresholds") is true for the Moon (target ≈ -0.26°, which is above the jump) and false for the Sun. `SolarLightingEngine` then tells the user "upper limb crosses the horizon (elevation -0.83°)" (lines 194, 258), which is not what was computed.

Twilights (-6°, -12°, -18°) are fine by luck: below -1° refraction is 0, so apparent equals true.

### M1 (Medium). Elevation dip is applied to Sun events and to every threshold, and not to the Moon

- `findAllCrossings` subtracts `observer.horizonDipDegrees` from **every** target (line 236), including the twilight, golden-hour and blue-hour angles. Dip lowers the visible sea horizon; it has no business moving "-6° civil twilight".
- The app passes `location.elevationMeters` into the observer (`AstronomyDisplayManager.kt:144`, `WidgetSyncWorker.kt:284`, `SolunarManager.kt:27`, `DarkSkyManager.kt:26`). The built-in locations carry elevations (Ottawa 70 m, Edmonton 645 m, Montreal 216 m, Halifax 60 m).
- Effect against standard almanac times (computed): **Ottawa 70 m: 1.7 min** earlier sunrise and later sunset (so the day is 3.4 min too long); **Calgary 1,045 m: 7.4 min** each side, 15 min on day length. A city on a plateau does not see a depressed horizon.
- `solar/Observer` uses the geometric dip `acos(R/(R+h))` (1.93′·√h); `lunar/Observer` uses the nautical 1.76′·√h. Two formulas, two answers.
- `TopocentricConverter.topocentricHorizontalState` computes `horizonThresholdDegrees` including dip (line 34), but `MoonRiseSetCalculator.computeTargetAltitude` uses `-semidiameter` only. Nothing reads `horizonThresholdDegrees` in production code (grep: only its declaration and its one writer). So moonrise ignores elevation while sunrise uses it.

This one is a **design decision for Adam** (mountain-top viewer versus almanac match), not a bug to fix silently.

### M2 (Medium). The tests do not measure the accuracy the docs claim

| Test | What it actually checks |
|---|---|
| `LunarPositionTest` | RA and Dec in range, semidiameter within 0.01° of 0.259°, distance within [350000, 420000] km. No position is compared to anything. |
| `MoonPhaseCalculatorTest.testQuarterPhaseWCorrection` | `assertEquals(2451557.48, jde, 1.0)`: tolerance **one day**. |
| `LunarEclipseCalculatorTest` | Compares to NASA times with tolerances of **10, 25 and 30 minutes**. They match the errors in H1 (3.5 / 22.1 / 12.3 min), and have been that size since the commit that added them (`2360a04`). The 2024-09-18 peak (+22 min) passes only because its limit is 30. |
| `SolarRiseSetSolverTest` | Sunrise hour in 4..7 (a four-hour window) and "1000 m is earlier than 0 m". |
| `SolarEveningRegressionTest.testParisOracle` | Sunrise hour in 7..8, sunset hour == 19, day length 600..720 min. The year sweep checks morning/evening **symmetry**, not absolute time. |
| `AtmosphericRefractionTest` | Encodes the discontinuity as expected behaviour. |

None of them uses a published example (Meeus 47.a, 49.a) or an official sunrise table.

### M3 (Medium). Statements in code and docs that are false or unsupported

| Where | Claim | Reality |
|---|---|---|
| `LunarEclipseCalculator.kt:62-64` | Moon series error "~10″ longitude / ~20 s time" | Mean 460″, worst 1,630″; 3 to 22 min against NASA. |
| ARCHITECTURE.md:80, AI_ONBOARDING.md:44, :55 | "verified ≤1 min / ≤1° vs official sources" | Sun: about 1 to 2 min (H3, M1). Moon phases: up to 47 min. Eclipses: up to 22 min. Moonrise: ≤1.7 min. No committed evidence. |
| `AtmosphericRefraction.kt:9`, `SolarPositionEngine.kt:104,117` | "Bennett (Meeus 16.4)" | The formula is Saemundsson's (16.3); fine for true altitude, wrong label. |
| `SolarLightingEngine.kt:226` | Daytime = "from end of morning civil twilight to start of evening civil twilight" | Daytime is sunrise to sunset. |

### M4 (Medium). The same job is implemented several times

- Two `Observer` classes (`lunar/` and `solar/`), with different defaults, nullability and dip formula.
- Two refraction implementations (`AtmosphericRefraction` and a private copy in `SolarPositionEngine`).
- Two Sun position implementations (`lunar/SolarPosition` and the inline code in `SolarPositionEngine`).
- Two topocentric conversions (`TopocentricConverter` and the inline block in `SolarPositionEngine`).
- Two identical Brent solvers (`MoonRiseSetCalculator`, `SolarRiseSetSolver`).
- Two test classes named `SolarLightingEngineTest` (test root and `solar/`); both run, because the packages differ.

Any fix (the nutation defect L2, the discontinuity H3) must be made in each copy.

### Low

- **L1.** ΔT 2005-2050 branch gives 75.07 s for 2026.0. From memory the measured value is about 69 s (unverified). A 6 s bias on eclipse and phase times (TT→UTC), about 0.2 s on moonrise. Harmless next to H1.
- **L2.** Both Sun implementations add the full nutation Δψ **and** the low-precision `-0.00478·sin Ω` term, which is the same nutation (`lunar/SolarPosition.kt:35`, `solar/SolarPositionEngine.kt:50`). Up to 0.0048°: 0.9 s on sunrise, up to about 34 s on moon-phase times.
- **L3.** Solar noon is the maximum of apparent altitude, found by a 5-minute scan then golden-section on whole seconds. Within seconds at mid-latitudes; not the meridian transit, and it will differ more at polar latitudes. Not measured.
- **L4.** Crossings are detected by sign change on 10-minute (Sun) and 2-minute (Moon) samples. Two crossings inside one step (grazing sun at the polar edge) are missed.
- **L5.** `Observer` throws `IllegalArgumentException` outside ±90°/±180° (both classes). I did not check that every caller sanitises its input.
- **L6.** `SolarLightingEngine` contains English UI text, emoji and "——" placeholders (lines 38, 58-60, 112-338), which breaks the strings.xml rule inside the "frozen" folder.
- **L7.** `AstroTime.julianCenturies` ("scheduled for removal") is still called by `LunarPosition.compute` and `SolarPositionEngine`; the arguments are JDE, so the numbers are right.
- **L8.** The root `SolarLightingEngineTest` uses `LocalDate.now()`, so it is date-dependent.

## 4. What is solid

- GMST, GAST, nutation (low precision), obliquity, ecliptic→equatorial and the Meeus Ch. 40 topocentric correction: read and match the book.
- The ΔT polynomials: all 15 branches match the Espenak–Meeus expressions as I know them; the continuity test is meaningful.
- The Sun pipeline end to end (a 3 s match with a published table once H3's definition is corrected).
- Brent's method: the convoluted `cond1` line is equivalent to the textbook "s not between (3a+b)/4 and b" in both orientations. Pre-1970 epochs and negative nanoseconds are handled.
- Eclipse geometry and shadow constants (8.794″, 959.63″, factor 1.02) are the standard ones, and with the correct Moon series the greatest-eclipse time lands within 1 min of NASA on all three test eclipses. I did not check the P1..P4 contact times.
- Polar day and night: handled (`SolarLightingEngine` lines 56-75).

## 5. What each finding changes for the user

| Feature | Effect |
|---|---|
| Moon phase dates and times, lunar calendar | Principal-phase times off by up to 47 min; the date can be wrong when a phase falls within an hour of local midnight. |
| Lunar eclipse timings | Peak and contacts off by up to about 22 min. Type and magnitude not measured. |
| Moonrise and moonset | ≤1.7 min (mean 35 s) from the series; elevation ignored. |
| Illumination %, phase name | Negligible. |
| Sunrise, sunset, golden hour, blue hour, twilights | About 1 to 2 min at sea-level cities, 7 min at 1,000 m, because of H3 and M1. |
| Solar noon | Fine to seconds at mid-latitudes. |

## 6. Recommended path (decisions are Adam's)

1. **Decide the freeze policy.** Split the label in the docs into **PROTECTED** (the agent must not edit; reason: stops UI fixes from touching formulas) and **VERIFIED** (only with a cited test). Today nothing earns VERIFIED.
2. **Write golden tests first**, from sources that are not the code: Meeus Examples 47.a and 49.a (book values, 0.001° and 1 minute), NASA greatest-eclipse and contact times (limit 2 minutes), 4 to 8 official phase times for a year, an almanac table for sea-level sunrise and sunset in 3 cities (limit 90 s), plus the known polar cases. They fail today; that is the point. Remove the 1-day and 30-minute tolerances.
3. **Unfreeze `lunar/` for one gate.** Replace the three series with the full Table 47.A and 47.B copied from the book by a person (not recalled by any model; a recalled table is the likely source of H1, and my own latitude table shows the risk), and decide between pure Ch. 49 and the Newton refinement. Keep the coding agent out of the table itself: the Orchestrator dictates the exact tables and the tests, as vermilion-editor does with its oracle.
4. **Decide the Sun definition** (H3): true altitude -0.8333°, or apparent upper limb. Fix both Sun and Moon consistently and remove the duplicated refraction.
5. **Decide the dip policy** (M1): almanac match (no dip), or dip only for a user-declared "open horizon".
6. Then merge the duplicates (M4) and correct the comments (M3, L6) in a separate gate.

I can draft the golden-test prompt and the table-replacement prompt in the gated format when Adam says go.

## 7. Reproducing and checking my work without trusting me

- Meeus Example 47.a is in the book (Chapter 47): run `LunarPosition.compute(2448724.5)` and compare λ = 133.167265°, β = -3.229126°, Δ = 368409.7 km. The app should differ by about 0.019° in longitude.
- The three NASA times are already in `LunarEclipseCalculatorTest.kt`; set the tolerance to 2 minutes and the tests fail.
- Ottawa sunrise: https://sunrise-sunset.org/ca/ottawa (2026-11-12: 06:57:33 / 16:35:46). Run `SolarRiseSetSolver.solve` with elevation 0 and compare.
- Phase list: https://time-ok.com/moon/ottawa.

Sources used: sunrise-sunset.org (Ottawa sunrise table), time-ok.com (Ottawa moon phases). The USNO ΔT page (https://maia.usno.navy.mil/products/deltaT) lists files only and gave no numbers.
