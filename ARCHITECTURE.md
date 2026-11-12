# Architecture — WeatherAPP Libre
Privacy-first, ad-free Android weather + astronomy app (Kotlin, GPLv3, public beta on Play): manual location tabs, selectable forecast models, official ECCC/MeteoAlarm alerts, on-device Meeus-grade solar/lunar tools, home-screen widgets. No GPS permission, no accounts, no ad/analytics SDKs.

*Last verified against repo: v0.8.2 (VC30), structure read 2026-11-03 and re-audited 2026-11-12 at bec248f. Claim tags: **[V]** verified by a command or test; **[U]** unverified; **[F]** known false or violated today, with its fix scheduled in ACTION_PLAN.md. An untagged line is a structural description read from the repo; if the repo disagrees, the repo wins. No astronomy accuracy claim is [V] until a golden test earns it (AGENTS.md rules 23-24).*

## Modules & build
| Module | Path | Responsibility | Depends on |
|---|---|---|---|
| `:app` | app/ | All app code, XML+ViewBinding UI, workers, unit tests | :theme-engine |
| `:theme-engine` | theme-engine/ | Portable palette module (3 files) | none |
Build: AGP 9.4.1, Gradle 9.6.0, version catalog (gradle/libs.versions.toml), compileSdk/targetSdk 37, minSdk 33, Java 11 in both modules, R8 + shrinkResources on release.
Build gate: `verifyEdgeToEdge` task (app/build.gradle.kts) scans src/main for forbidden deprecated window APIs and runs before the build; never bypass or weaken it. **[F]** It is wired to `assembleDebug` and `assembleRelease` only, so `bundleRelease` (the Play artifact) skips it (CODE_AUDIT M8; ACTION_PLAN R1.4).
Release signing: falls back to the debug key when keystore properties are absent (intentional, documented in app/build.gradle.kts; such builds cannot go to Play).
Runtime deps: appcompat, material, constraintlayout, activity-ktx, core-ktx, OkHttp, Gson, Retrofit (+gson converter), swiperefreshlayout, WorkManager, coroutines, play-app-update-ktx. Debug: LeakCanary. Tests: JUnit4, Robolectric, work-testing, xmlpull/xpp3/kxml2.

## Source tree — :app (com.weatherapp.libre), grouped by role
root/ managers: MainActivity (thin host: wiring+intents only), WeatherDisplayManager, AstronomyDisplayManager, HourlyDisplayManager, SettingsDialogManager (~2,300 lines: owns every settings screen), LocationDialogManager, ThemeManager, DarkSkyManager, NightVisibilityManager, SolunarManager, CacheInspector, AppUpdateManager, EasterEggManager.
root/ data & prefs: WeatherRepository (Open-Meteo OkHttp+Gson, model routing, in-flight request dedup, disk cache capped at 50 entries FIFO), LocationPreferences (SharedPreferences facade: settings, tabs as Double raw-bits, cache JSON blobs, page HTML; process-wide synchronized lock), WidgetPreferences, WeatherModels, AstronomicalModels, HourlyMapper, WeatherCodeUtils, UnitUtils, UrlConfig (all remote URLs), SolarPhaseDetail.
root/ RecyclerView adapters: DayForecast, HourlyForecast, LocationSearch, MenuOrganization, MetricGrid, DarkSkyWindow, NightVisibilityPeriod, SolarEvent, SolarPhase, SolunarDay.
root/ widgets: WidgetSyncWorker (60-min periodic, CONNECTED + battery-not-low constraints, redraw helpers), WeatherWidgetProvider.kt (BaseWeatherWidgetProvider + open WeatherWidgetProvider + 4x2/4x1/2x1 subclasses).
root/ views & helpers: SnowflakeView + HeatShimmerView (easter-egg overlays), ExpandCollapseHelper, SystemBarContrastHelper (central edge-to-edge + 4-side insets for dialogs; small floating dialogs bypass it).
alerts/ pipeline: workers/AlertSyncWorker (15-min, consent-gated), routing/AlertProviderRouter, providers/AlertProvider + EcccProvider (Datamart CAP-CP scrape) + MeteoAlarmProvider + meteoalarm/MeteoGateClient (Retrofit) + MeteoGateModels, cache/GlobalAlertCache, models/UnifiedAlert + CapModels + AlertFetchResult, CapXmlParser (DTD disabled, 5 MB ceiling), AlertMatcher (polygon/circle, 1e-9 edge epsilon), AlertNotificationHelper, AlertBannerManager, AlertConsentDialog, utils/CountryCodeMapper + CountryCodeResolver, AlertLogRepository (Mutex-guarded) + AlertLogEntry, GlobalAlertTestHarness (debug-only menu, typed isTest flag).
lunar/ PROTECTED Meeus math (not verified; ASTRONOMY_AUDIT H1/H2; ACTION_PLAN R2): AstroTime, AstronomyMath, AtmosphericRefraction, DeltaT (Espenak polynomials), LunarCalculator, LunarDataTypes, LunarEclipseCalculator, LunarPosition, MoonPhaseCalculator, MoonRiseSetCalculator, Observer, SolarPosition, TopocentricConverter.
solar/ PROTECTED math (Sun pipeline sound; sunrise definition and dip policy wrong, ASTRONOMY_AUDIT H3/M1; ACTION_PLAN R2): SolarPositionEngine, SolarRiseSetSolver, SolarLightingEngine (twilight/golden/blue), SolarDataTypes, Observer.
almanac/: MoonAlmanacManager, LunarCalendarManager, LunarDayAdapter, LunarCacheManager (3-yr event cache), CalendarIntentHelper. Gson-serialized almanac classes carry @Keep.

## theme-engine (portable; zero app coupling)
ThemeMode (int-constant modes), ColorPalette (typed tokens), BoutiqueThemeEngine (getPalette(mode), luminance-derived contrast). Public surface = ThemeMode + getPalette only.

## Manifest components
Activity: .MainActivity (only Activity). Receivers: .WeatherWidgetProvider4x2 / 4x1 / 2x1 (exported=true, custom ACTION_WIDGET_SWITCH_TAB; receivers validate widget ID ownership and tab-index bounds). No services, no AlarmManager; WorkManager only.
Permissions: INTERNET, POST_NOTIFICATIONS, SET_ALARM only.
Backup: allowBackup=true. Intended: backup_rules.xml excludes all app SharedPreferences (location, widget, alert cache, alert log, ECCC memo); data_extraction_rules.xml governs Android 12+. **[F]** Actual (CODE_AUDIT H2): both XML files name `location_preferences.xml` and `widget_preferences.xml`, but the real files are `weather_location_prefs` and `weather_widget_prefs`, so saved locations, settings, forecast cache and alert consent state ARE included in Auto Backup and device transfer today. `global_alert_cache_prefs`, `alert_log_prefs` and `eccc_memo_prefs` are named correctly. Fix: ACTION_PLAN R1.2.

## Layouts & resources (res/)
layout/: activity_main.xml (composition root: <include> tags only) + 15 section_*.xml decomposed sections.
layout/: 16 item_*.xml + view_day_cell.xml = RecyclerView cells matching the adapters above.
layout/: 19 dialog_*.xml = every settings/info screen is a fullscreen Dialog (no extra Activities, no bottom sheets) + nav_header_settings.xml.
layout/: widget_layout_2x1/4x1/4x2.xml (RemoteViews).
xml/: widget_info_2x1/4x1/4x2; backup_rules.xml; data_extraction_rules.xml.
values/: strings.xml + values-fr/strings.xml (French typography enforced); values-night/ for night overrides.

## Assets & tests
assets/cap_fixtures/: 6 CAP XML (tornado warning/update/cancel, thunderstorm, winter storm, special statement) — drive reconcile tests and the debug harness.
assets/meteoalarm_fixtures/: 3 MeteoGate JSON (red wind, orange rain, yellow frost).
app/src/test/: 243 unit tests **[V]** (232 at v0.8.2 plus 11 rails tests, R0.2, 2026-11-12) mirroring packages (root, alerts, alerts/workers, lunar, solar, almanac, rails). A falling count is a defect.
rails/ (test package, R0.2) **[V]**: RepoRoot, ProtectedManifest, SizeBaseline and their tests. `ProtectedPathsTest` compares SHA-256 of every file under lunar/, solar/ and any `golden/` directory with `config/protected.sha256` (18 files today); `SizeRatchetTest` compares file sizes with `config/size-baseline.txt` (8 main files over 500 lines; the numbers may only fall); `RailsComparerTest` (9 tests) proves both comparers fire on fake mismatches. `app/build.gradle.kts` declares both config files as inputs of every Test task (`railsConfig`), proven by a controlled change of a config file.
Known debt: two same-named SolarLightingEngineTest classes (test root and test solar/) — merge pending. No CI workflow yet. **[F]** No test measures astronomy accuracy: tolerances of 10-30 min and one day exist in the astronomy tests (ASTRONOMY_AUDIT M2), and the UI managers have no tests (CODE_AUDIT L13). Every remaining item is in ACTION_PLAN.md.

## Root & website
Gradle: settings.gradle.kts, root build.gradle.kts, app/build.gradle.kts, theme-engine/build.gradle.kts, gradle/libs.versions.toml, app/proguard-rules.pro, app/src/main/keepRules/rules.keep.
Website at repo root = weatherapplibre.ca (CNAME). Pages fetched into in-app WebView with 7-day cache; navigation allowlisted to weatherapplibre.ca, external links open in system browser: index, about, almanac, changelog, licenses, privacypolicy, roadmap, support, usermanual, template.html. changelog.txt is the plain-text changelog source. docs/extensions.md = unimplemented draft schema v1.

## Data flow
Forecast: manager → WeatherRepository → Open-Meteo (api, ensemble-api, air-quality-api, geocoding-api) → SharedPreferences JSON cache → widgets draw cache-only.
Alerts: AlertSyncWorker → router → EcccProvider (dd.weather.gc.ca) | MeteoAlarmProvider (api.meteogate.eu) → GlobalAlertCache → banner + notification (opt-in).
Astronomy: pure on-device compute from lat/lon/elev + ZoneId; UTC Instants throughout. **[F]** Accuracy is not as documented: the Sun pipeline is sound, but the Moon series and moon-phase times are wrong (up to 47 min), and sunrise/sunset are computed at the wrong altitude (1-2 min); see ASTRONOMY_AUDIT_2026-11-12.md and ACTION_PLAN R2.

## Key decisions
Locations manual; alerts strictly opt-in (ALERT_CONSENT_OPT_IN); zero synthetic alerts in release builds.
No DI, no Compose, no ViewModel/StateFlow: ViewBinding + manager classes mutate views directly.
Network calls use non-blocking enqueue() wrappers (no call.execute()).
Secrets via local.properties → BuildConfig (METEOGATE_API_KEY); never committed. Missing key → EU alerts fail fast, rest of app unaffected.
GPLv3 + trademark reservation (NOTICE). Open decision: play-app-update-ktx blocks F-Droid eligibility.

## Conventions the agent keeps getting wrong
A display or UI problem is never fixed by changing a formula in lunar/ or solar/ (AGENTS.md rule 23): stop and report. The 2026-11-12 audit did find real defects in the math itself (ASTRONOMY_AUDIT); they are fixed only in an UNLOCK gate with golden tests (ACTION_PLAN R2). Precision bugs also arise in the data layer: Float coordinates caused a 5-min sunrise error.
Coordinates/elevation Double end-to-end (putDouble/getDouble raw long bits); Float truncation caused a 5-min sunrise error. **[F]** `LocationPreferences` still stores the fixed-default and last-used coordinates as Float (CODE_AUDIT L1; ACTION_PLAN R3.7).
Alert providers return typed Failure (AlertFetchResult), never emptyList() on error — empty wipes the cache.
CAP expires may carry offsets (+02:00): OffsetDateTime.parse, not Instant.parse.
Alert chains: providers re-return the full 24-h CAP history every sync. GlobalAlertCache.reconcile() groups messages into chains (union-find over id + references), keeps the latest-sent winner, drops chains whose winner is a Cancel, and carries a stable chainKey. The worker notifies only net-new reconciled alerts; one notification per chain+location (AlertNotificationHelper.notificationIdFor); Updates replace silently unless severity rises; vanished chains cancel their notification. Never diff against the raw provider list.
Dismissals coordinate-scoped (app_alert_dismissed_<lat>_<lon>_<id>); test alert IDs timestamped.
ECCC Datamart: /today/alerts/cap/{YYYYMMDD}/{EEEE}/{hh}/*.cap; SK→CWWG, MB→CWWG, AB→CWEG, ON→CWTO, QC→CWUL, BC→CWVR, Atlantic→CWHX; 24-h lookback. **[F]** The ECCC memo (`eccc_memo_${ecccCode}_${filename}`) has no location in its key, so two tabs sharing a region (Ottawa and Toronto, both CWTO) can miss or misattribute alerts (CODE_AUDIT H1; ACTION_PLAN R1.1). **[F]** Yukon, NWT and Nunavut have no office-code mapping and get no alerts (CODE_AUDIT L5; R3.5).
Strings externalized EN+FR with French typography (space before :;!?»); formatting only via UnitUtils/WeatherCodeUtils.
Colors only from theme-engine palette tokens; no hex in views/adapters/drawables.
Edge-to-edge: full-screen dialogs go through SystemBarContrastHelper; small floating dialogs must NOT (0.8.1 regression). SystemBarContrastHelper pads the dialog's decorView by the system-bar insets; never add a second systemBars padding listener on a dialog's content root (that doubled the top band on Data Cache / Cache Inspector / Alert Log until 0.8.2).
Forecast label columns (item_day_forecast 88dp, item_hourly_row time 72dp) are fixed-width for row alignment and use maxLines=1 + uniform autosize; keep that pattern for any new fixed label column.
Shell commands run from the project root with relative paths; built-in IDE file tools may use absolute paths inside the project (see AGENTS.md TOOLING).

## Do not touch
**PROTECTED** (AGENTS.md rule 23; the Coder edits these only in a gate whose prompt carries an `UNLOCK:` line): `app/src/main/java/com/weatherapp/libre/lunar/`, `app/src/main/java/com/weatherapp/libre/solar/`, any `golden/` test directory, and `config/protected.sha256` (the last two exist since gate R0.2; `ProtectedPathsTest` fails on any change, **[V]**). Protected means the Coder must not edit it. It does **not** mean correct: the earlier label "verified ≤1 min / ≤1° vs official sources" is withdrawn (it had no test behind it; ASTRONOMY_AUDIT_2026-11-12.md).
verifyEdgeToEdge build gate (do not disable or loosen; wiring it to more tasks is allowed).
local.properties, keystores, app/release/ binaries: never commit, and the Coder never opens them (cloud model; AGENTS.md rule 13).
Statcounter snippet at bottom of every *.html: UNLOCK is pending the privacy decision D1 (ACTION_PLAN R1.3). **[F]** The README and privacy policy say there is no analytics, while the in-app WebView runs this snippet even from the cache (CODE_AUDIT H3).
