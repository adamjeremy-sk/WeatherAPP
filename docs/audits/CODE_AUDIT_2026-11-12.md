# Full Code Audit — WeatherAPP Libre — 2026-11-12

**Repo:** ~/AndroidStudioProjects/WeatherAPP at `bec248f` (clean tree, v0.8.2 / VC30). Read-only audit via the desktop link; nothing in the repo was modified.
**Auditor:** Claude (Sonnet 5.5). **Method:** read the orchestration docs, then read ~35 source files in full or in the relevant ranges, plus grep sweeps across all of `app/src/main` and `app/src/test`. Every `file:line` below came from numbered output in this session.

## Scope and limits (read first)
- **Not built, not run.** The workspace has no Android SDK/Gradle and no network, so I could not run `./gradlew :app:testDebugUnitTest` or `assembleDebug`. Test count (232 `@Test`) is counted, not executed. Adam remains build authority.
- **Frozen math not audited.** `lunar/` and `solar/` are "do not touch" per ARCHITECTURE.md; I only confirmed they have test files (19 of the 50 test files).
- Findings are from reading code. Where a claim depends on runtime or live-API behaviour I say **(unverified)**.

## Scorecard
| Area | Verdict |
|---|---|
| Concurrency rails (no runBlocking / GlobalScope / .execute(), CancellationException rethrown, typed Failure) | **Clean** — verified by grep |
| Manifest / permissions / PendingIntents / TLS | **Clean** — 3 permissions, all PendingIntents `FLAG_IMMUTABLE`, no cleartext, no location APIs |
| Alert pipeline correctness | **1 high bug (H1)**, 2 medium |
| Privacy posture vs. code | **2 high mismatches (H2, H3)** |
| Localization | EN/FR parity perfect (571/569 keys, 0 missing, typography clean) but **many hardcoded English strings (M5)** |
| Tests | 232 as documented; strong on alerts/astronomy, **zero coverage of UI managers** |
| Docs vs. code | Several drifted claims (listed in §Doc drift) |

---

## HIGH

### H1 — ECCC memo is keyed per region+file, but alert matching is per location → missed and misattributed alerts
- `EcccProvider.kt:214` `memoKey = "eccc_memo_${ecccCode}_${filename}"` — no location in the key.
- `:216-224` a memo hit returns the cached result without re-matching; `:261/:266` store the match result computed for whichever location was evaluated first. `AlertMatcher.matchesLocation` (polygon/circle/geocode) is location-specific.
- **Failure scenario (default tabs!):** Ottawa (tab 0) and Toronto (tab 1) both map to `CWTO`. A tornado warning polygon covers Toronto only. Sync evaluates Ottawa first → `matched=false` memoized (48 h, persisted in `eccc_memo_prefs`). Toronto's sync hits the memo and **never sees the warning**. Reverse case: if A matched, B receives A's `UnifiedAlert` (A's coordinates and area text) as a false positive.
- Affects any two tabs sharing a regional code (ON/ON, SK+MB, the four Atlantic provinces).
- `EcccProviderTest` memo tests (`:114-209`) use a single location, so this is uncaught.
- **Fix direction:** memoize the location-independent parse (the `CapAlert`, or "file irrelevant to region"), then run `AlertMatcher` per location; or include `GlobalAlertCache.cacheKey(location)` in the memo key. Add a two-location regression test. Touches no `AlertProvider` interface or prefs schema if the key just gains a suffix (old memo entries age out in 48 h) — still worth a plan-first per rule 10 if the entry type changes.

### H2 — Backup exclusion rules name the wrong SharedPreferences files
- `backup_rules.xml` and `data_extraction_rules.xml` (cloud-backup and device-transfer) exclude `location_preferences.xml` and `widget_preferences.xml`.
- Actual files: `weather_location_prefs` (`LocationPreferences.kt:107`) and `weather_widget_prefs` (`WidgetPreferences.kt:9`).
- Result: saved locations, tabs, recents, all settings, forecast cache blobs, cached page HTML, **alert consent state** and dismissals are **included** in Google Auto Backup and device-to-device transfer. `global_alert_cache_prefs`, `alert_log_prefs`, `eccc_memo_prefs` are correct.
- Contradicts ARCHITECTURE.md ("excludes all app SharedPreferences") and the privacy-first positioning. A restored `ALERT_CONSENT_OPT_IN` on a new device means alert polling can resume without a fresh consent step (`MainActivity.kt:138` schedules the worker at launch; I did not trace its consent guard).
- **Fix:** correct the two filenames in both XML files. Add a unit test that reads the XML and asserts every `getSharedPreferences("…")` name in `src/main` is excluded.

### H3 — In-app info pages run Statcounter JavaScript; policy and README say there is no analytics
- All 10 `*.html` pages end with the Statcounter snippet (`privacypolicy.html:589-604`, project `sc_project=13358478`).
- The in-app viewer enables JS and DOM storage (`SettingsDialogManager.kt:208-209`) and renders pages with `loadDataWithBaseURL(url, html, …)` (`:245`, `:289`). **Cached pages still execute the snippet**, so each view contacts statcounter.com even when the HTML came from the 7-day cache.
- Privacy policy §8.1: "No user data … is transmitted to these pages beyond standard web request headers"; §9: no "developer-operated behavioural tracking"; the "at a glance" box says "No developer-operated analytics". README: "no advertising, analytics, or tracking of any kind". Statcounter is not mentioned anywhere in the policy text.
- Statcounter sees IP, user-agent, page and referrer, and may set cookies, for every in-app page view. Whether that counts as "developer-operated analytics" is a policy judgement, but the documents as written are inaccurate, and this also matters for the Play Data Safety form.
- **Options (pick one):** disable JS in the in-app WebView (pages look static — test); block `statcounter.com` in `shouldInterceptRequest`; serve an app variant of the pages without the snippet; or disclose Statcounter in the policy and Data Safety answers.

---

## MEDIUM

### M1 — Missing MeteoGate key aborts the whole alert sync (README says it "fails silently")
`MeteoGateHeaderInterceptor.init` throws `IllegalStateException` when the key is blank (`MeteoGateClient.kt:19-23`). It is reached through `MeteoAlarmProvider()`'s default argument inside `AlertProviderRouter.getProviderForCountryCode`, called at `AlertSyncWorker.kt:90` **outside any try/catch**. With tabs [EU, CA], the exception escapes `doWork` and Canadian tabs after the EU one are never synced, every cycle. Only affects keyless builds (contributors, CI, any F-Droid build) but contradicts README and ARCHITECTURE. **Fix:** return `AlertFetchResult.Failure` from the provider when the key is blank, or wrap provider resolution per tab.

### M2 — Cache Inspector uses a prefs file that doesn't exist
`CacheInspector.kt:47, 102, 133` use `"alert_cache_prefs"`; the real alert cache is `global_alert_cache_prefs` (`GlobalAlertCache.kt:42`). The inspector never lists the alert cache, and "clear cache" does not clear it (it creates and clears an empty file). No test references either name.

### M3 — Missing forecast data is rendered as real zeros
`WeatherRepository.kt:264-278` merges daily arrays with `?: 0.0` / `?: 0` fallbacks; `:531-541`, `:562-564`, `:618-628`, `:650-652` do the same (`humidity ?: 0`, `visibility ?: 10000.0`). `isBestMatchNeeded` (`:347-364`) only checks that lists are non-empty, not that elements are non-null. A model that doesn't publish a variable (e.g. UV for some models) will show "UV 0", 0° highs, or weather code 0 (clear sky) instead of "—". **(unverified against live responses; check CMA-GFS and ECMWF.)**

### M4 — MeteoAlarm does N+1 requests per tab, every 15 minutes, with no memo
`MeteoAlarmProvider.kt:48-65` fetches the country's warning list, then one detail request per feature, sequentially, per EU tab per sync. ECCC has a memo; MeteoAlarm has none. Risk: API rate limits on the shared key, battery/data. **(unverified: depends on feature counts.)** Consider filtering by bbox/location in the first call or memoizing by identifier.

### M5 — Localization gaps beyond the known list
Known (HANDOFF §3): notification text, `WeatherRepository.kt:603-608`. Additional, all hardcoded English:
- `AlertBannerManager.kt:124,127` ("Area:", "Issued:", "Expires:"), `:157` ("ACTIVE WEATHER ALERTS"), `:158` ("Tap to view details"), `:162` ("Multiple Alert Sources")
- Notification channel names/descriptions `AlertNotificationHelper.kt:26-30,49,58` — and the "(ECCC)" label is shown for MeteoAlarm alerts too
- `DarkSkyWindowAdapter.kt:65` ("Index"), `HourlyForecastAdapter.kt:149` ("Now"), `WeatherDisplayManager.kt:478` ("Unknown"), `WeatherModels.kt:213` (HorizonFooter default), `SettingsDialogManager.kt:170` (contentDescription "Back")
- **Latent coupling:** `WeatherDisplayManager.kt:57` and `:217` decide "is today" via `dayOfWeek.equals("Today")`. When `WeatherRepository` is localized as planned, this logic silently breaks. Give `DayForecast` an explicit `isToday`/offset field first.

### M6 — Widget background work can crash the process
`WidgetSyncWorker.kt:110, 124` and `WeatherWidgetProvider.kt:66` launch on `CoroutineScope(Dispatchers.Default)` with no catch (the receiver has `try/finally` but no `catch`). `rebuildAndUpdate` has no error handling and runs on every `MainActivity.onCreate` via `notifyWidgetsInvalidated`. Any exception there (e.g. RemoteViews too large, null widget info) is uncaught and kills the app. `doWork` itself does wrap in try/catch. Also, `enqueueImmediate` uses `ExistingWorkPolicy.REPLACE`, so rapid tab taps or `onUpdate` calls can keep cancelling an in-flight sync.

### M7 — Large blobs live in the same SharedPreferences file as user settings
`LocationPreferences` stores forecast JSON (up to 50 entries, `:537-591`) and cached page HTML (`:545-558`, changelog alone is 46 KB) in `weather_location_prefs`, alongside tabs and settings. SharedPreferences loads the whole XML on first access and rewrites it on each `apply()`; this can add startup jank and main-thread waits on stop. Moving the caches to files under `cacheDir` (as `LunarCacheManager` already does) also removes them from backups (see H2).

### M8 — `verifyEdgeToEdge` doesn't run for `bundleRelease`
`app/build.gradle.kts` wires the gate only to `assembleDebug` and `assembleRelease`. The Play upload artifact is the `.aab` (`bundleRelease`; one exists in `app/release/`), which bypasses it. ARCHITECTURE says it "runs before the build". Hook it to `preBuild` or `check`.

---

## LOW / HYGIENE

| # | Finding | Evidence |
|---|---|---|
| L1 | **Float coords** (known). Also these getters build `LocationItem` without id/countryCode/elevation, so a tab seeded from the fixed default has elevation 0 (affects solar). Practical precision loss is ~1 m; the real issue is the invariant plus dropped fields. Needs a plan (prefs schema, rule 10). | `LocationPreferences.kt:683-684, 701-702, 713-714, 730-731` |
| L2 | **BottomSheetDialog** ×2 (known), unused import, stale comment | `LocationDialogManager.kt:38,149`; `ThemeManager.kt:16`; `dialog_lunar_detail.xml:16` |
| L3 | **Instant.parse for expiry/sent** (known). Scope narrowed: ECCC alerts are normalized to `Instant.toString()` (`EcccProvider.kt:480-484`) and are safe; only MeteoAlarm stores raw offset strings (`MeteoAlarmProvider.kt:130-135`). Expiry is also formatted in the device zone (`timezoneStr` is never passed by the worker). Three different parse patterns exist for the same job (OffsetDateTime-first in `GlobalAlertCache`/`CapXmlParser`; Instant-only in `AlertNotificationHelper.kt:101`, `AlertSyncWorker.kt:123`; ZonedDateTime in `MeteoAlarmProvider.kt:190`). Centralize in one helper. | |
| L4 | **Hardcoded colors** (known, confirmed): `SettingsDialogManager.kt:1724-1736, 1819`; `HeatShimmerView.kt:33,38`; `WidgetSyncWorker.kt:183-195`; 69 drawables + 6 layouts contain hex | |
| L5 | **Canadian territories get no alerts.** `PROVINCE_TO_ECCC_CODE` has no Yukon / NWT / Nunavut, so `resolveEcccCode` returns null and the provider returns `Success(emptyList())` — the UI shows "no alerts" with no coverage. Office codes for the territories are **unverified** (Yukon likely CWVR, NWT likely CWEG). The substring fallback (`:300-306`) can match 2-letter codes like "ON" inside "Edmonton" when `admin1` is missing. | `EcccProvider.kt:168-191, 203, 300-306` |
| L6 | Geocode rule matches on `areaDesc.contains(location.name)` — possible false positives for short or common names | `AlertMatcher.kt:62-71` |
| L7 | `GlobalAlertCache` has no lock (unlike `LocationPreferences`); a worker and UI (or test harness) can interleave read-modify-write. `readAlertsForLocation` swallows parse errors into `emptyList()`, which makes every alert look new and re-notify. `clearExpiredAlerts` parses without a guard (`:215`), so one corrupt entry throws out of `doWork` (`AlertSyncWorker.kt:172`). | `GlobalAlertCache.kt:200-203, 215` |
| L8 | Test harness + fixtures ship in release (`src/main/java/.../GlobalAlertTestHarness.kt`, `assets/cap_fixtures`, `assets/meteoalarm_fixtures`); only the menu is gated by `BuildConfig.DEBUG` (`SettingsDialogManager.kt:699`). Moving them to a `debug` source set would make "zero synthetic alerts in release" structural. | |
| L9 | WebView hygiene: never `destroy()`ed; nav allowlist checks host only, not scheme (`:217-222`); a new `OkHttpClient()` per page fetch (`:250`); notification `setGroup` with no summary (`AlertNotificationHelper.kt:153`) | `SettingsDialogManager.kt` |
| L10 | Dependencies look 2023-era against AGP 9.4.1 / SDK 37: appcompat 1.6.1, material 1.10.0, activity-ktx 1.8.0, core-ktx 1.10.1, constraintlayout 2.1.4, coroutines 1.8.0, work 2.9.0, retrofit 2.9.0, gson 2.10.1. **(Latest versions not checked offline; run a dependency-updates report.)** AGP is declared twice (`libs.versions.toml:2` and a literal at root `build.gradle.kts:4`). `-keep class androidx.work.impl.** { *; }` in `proguard-rules.pro` is over-broad. | |
| L11 | Repo hygiene: no CI (`.github/` absent); `.kotlin/` not in `.gitignore` (the `.salive` file is no longer tracked at HEAD, so only the ignore rule is left of the HANDOFF chore); `buildConfigField` interpolates the key unescaped; the MeteoGate key necessarily ships in the APK, so treat it as non-secret and rate-limited | |
| L12 | Accessibility (heuristic: images/buttons vs. contentDescription count): widget layouts 15 icons / 0 descriptions; `activity_main` 6/3; `section_states` 2/0; `section_alert_banner` 1/0; `dialog_recent_locations` 1/0; `item_metric_cell` 1/0. Mark decorative ones `importantForAccessibility="no"`, label the rest. | |
| L13 | Test debt: duplicate `SolarLightingEngineTest` (known); zero tests for SettingsDialogManager (2,317 lines), LocationDialogManager, ThemeManager, AppUpdateManager, MeteoGateClient, AlertConsentDialog | |
| L14 | **GEFS (unverified).** The app sends `current=…`, `daily=weather_code,sunrise,sunset,uv_index_max,precipitation_probability_max,…` to the *ensemble* endpoint (`WeatherRepository.kt:323-332`). Open-Meteo's ensemble docs list no `current` parameter and no daily `weather_code`, `sunrise/sunset`, `uv_index_max` or `precipitation_probability_max`. If the API rejects unknown variables, GEFS is unusable; if it ignores them, the fallback (`isBestMatchNeeded`) fills from best-match data. I couldn't test live. Check by selecting GEFS on a device, and add a test with a recorded ensemble response. | |

---

## Doc drift (claims that no longer match the code)
| Doc | Claim | Reality |
|---|---|---|
| ARCHITECTURE.md "Backup" | backup rules exclude all SharedPreferences | two of five filenames are wrong (H2) |
| README.md | no analytics or tracking of any kind | Statcounter in in-app pages (H3) |
| README.md "Building" | without a key, EU alerts fail silently and the rest works | worker aborts mid-loop (M1) |
| ARCHITECTURE.md Build gate | `verifyEdgeToEdge` "runs before the build" | not wired to `bundleRelease` (M8) |
| HANDOFF.md §1 | `.kotlin/sessions/*.salive` committed — chore queued | file no longer tracked; `.gitignore` rule still missing (L11) |
| HANDOFF.md §3 | notification-localization item lists only the notification helper and WeatherRepository | banner and adapters also affected (M5) |

## What multiple orchestrators/agents left behind (pattern view)
1. **Rails enforced by prose, not by build.** Only one rail (edge-to-edge) has a gate, and that gate has a hole (M8). Hex colors, Float coords, bottom sheets, hardcoded strings and backup filenames all drifted anyway. Convert the next ones into cheap build checks/tests (backup-names test, hex-in-views grep task, `Float` ban in `LocationPreferences`).
2. **Same job, several implementations.** Date parsing (3 styles, L3); callback→coroutine OkHttp wrappers copied at least 5 times (`SettingsDialogManager.kt:~252`, `WeatherRepository.kt:103`, `EcccProvider.kt:228/328/402`) while MeteoGate uses Retrofit suspend; two `Observer.kt` (lunar/ and solar/); two types named `CacheEntry` (top-level and `GlobalAlertCache.CacheEntry`).
3. **Fixes that match one agent's mental model of one case.** H1 (memo correct for one location), M2 (renamed prefs file, inspector not updated), M1 (guard written for the happy path). Each passes its own tests; none has a cross-component test.
4. **Agent-isms in code:** `isNull_or_blank_compat()` reinvents `isNullOrBlank` (`AlertNotificationHelper.kt:172`); `(alert.msgType as String?) ?: "Alert"` casts to defeat Kotlin null-safety after Gson (`GlobalAlertCache.kt:195-196`); extensive empty `catch (e: Exception) { // Ignore }` (102 `catch (e: Exception/Throwable)` sites). Harmless individually, but they hide H1-style bugs.
5. **Docs are the weakest link:** ARCHITECTURE.md is rigorous about structure but wrong on two privacy-relevant facts (backup, analytics). The orchestrator docs should carry a "verified by command" tag per claim, like AGENTS.md rule 12 demands of the coding agent.

## What is solid (verified)
- No `runBlocking`, `GlobalScope`, `AsyncTask`, `Thread(`, `.execute()`; `CancellationException` is rethrown at all 11 provider/repository catch sites (grep count); alert providers return typed `Failure`.
- Only `INTERNET`, `POST_NOTIFICATIONS`, `SET_ALARM` (used via the `AlarmClock` intent only); all `PendingIntent`s are `FLAG_IMMUTABLE`; the widget receiver validates widget-ID ownership and tab bounds; no location APIs; the only `http://` strings are XML namespaces.
- Cache-inspector WebView has JS off and HTML-escapes the payload; CAP parser has DOCDECL off and a 5 MB cap (applied after the read, so it limits parsing, not memory).
- Release: R8 + resource shrinking on; every Gson-serialized model I traced carries `@Keep`/`@SerializedName` (WeatherModels, UnifiedAlert, MeteoGate models, AlertLogEntry, EcccMemoEntry, `Lunar3YearCacheWrapper`, `MoonAlmanacDay`); Gradle wrapper has a pinned SHA-256; secrets files are git-ignored and untracked; `app/release/*` is ignored.
- Docs' numbers hold: 232 `@Test`, SettingsDialogManager 2,317 lines, 3 manifest permissions, no CI.

## Suggested order of work
1. **H1** (ECCC memo) — smallest change, highest safety impact; add a two-location test.
2. **H2** (backup filenames) — two-line XML fix + a guard test; correct ARCHITECTURE.md.
3. **H3** (Statcounter) — decide policy: block/disable in-app or disclose; update policy/README/Data Safety.
4. **M1 + M2 + M8** — small, mechanical fixes.
5. **M3, M5, M6** — correctness and robustness; do the `isToday` field before localizing "Today".
6. Then M4/M7 and the Low list; L14 needs a live check first.

*Generated by Claude Sonnet 5.5, 2026-11-12. Read-only; nothing in the repository was changed.*
