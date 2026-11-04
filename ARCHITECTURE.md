# Architecture — WeatherAPP Libre
Privacy-first, ad-free Android weather + astronomy app (Kotlin, GPLv3, public beta on Play): manual location tabs, selectable forecast models, official ECCC/MeteoAlarm alerts, on-device Meeus-grade solar/lunar tools, home-screen widgets. No GPS permission, no accounts, no ad/analytics SDKs.

*Last verified against repo: v0.8.1 (VC28), commit b47934a, 2026-11-03.*

## Modules & build
| Module | Path | Responsibility | Depends on |
|---|---|---|---|
| `:app` | app/ | All app code, XML+ViewBinding UI, workers, unit tests | :theme-engine |
| `:theme-engine` | theme-engine/ | Portable palette module (3 files) | none |
Build: AGP 9.4.1, Gradle 9.6.0, version catalog (gradle/libs.versions.toml), compileSdk/targetSdk 37, minSdk 33, Java 11 in both modules, R8 + shrinkResources on release.
Build gate: `verifyEdgeToEdge` task (app/build.gradle.kts) scans src/main for forbidden deprecated window APIs and runs before the build; never bypass or weaken it.
Release signing: falls back to the debug key when keystore properties are absent (intentional, documented in app/build.gradle.kts; such builds cannot go to Play).
Runtime deps: appcompat, material, constraintlayout, activity-ktx, core-ktx, OkHttp, Gson, Retrofit (+gson converter), swiperefreshlayout, WorkManager, coroutines, play-app-update-ktx. Debug: LeakCanary. Tests: JUnit4, Robolectric, work-testing, xmlpull/xpp3/kxml2.

## Source tree — :app (com.weatherapp.libre), grouped by role
root/ managers: MainActivity (thin host: wiring+intents only), WeatherDisplayManager, AstronomyDisplayManager, HourlyDisplayManager, SettingsDialogManager (~2,300 lines: owns every settings screen), LocationDialogManager, ThemeManager, DarkSkyManager, NightVisibilityManager, SolunarManager, CacheInspector, AppUpdateManager, EasterEggManager.
root/ data & prefs: WeatherRepository (Open-Meteo OkHttp+Gson, model routing, in-flight request dedup, disk cache capped at 50 entries FIFO), LocationPreferences (SharedPreferences facade: settings, tabs as Double raw-bits, cache JSON blobs, page HTML; process-wide synchronized lock), WidgetPreferences, WeatherModels, AstronomicalModels, HourlyMapper, WeatherCodeUtils, UnitUtils, UrlConfig (all remote URLs), SolarPhaseDetail.
root/ RecyclerView adapters: DayForecast, HourlyForecast, LocationSearch, MenuOrganization, MetricGrid, DarkSkyWindow, NightVisibilityPeriod, SolarEvent, SolarPhase, SolunarDay.
root/ widgets: WidgetSyncWorker (60-min periodic, CONNECTED + battery-not-low constraints, redraw helpers), WeatherWidgetProvider.kt (BaseWeatherWidgetProvider + open WeatherWidgetProvider + 4x2/4x1/2x1 subclasses).
root/ views & helpers: SnowflakeView + HeatShimmerView (easter-egg overlays), ExpandCollapseHelper, SystemBarContrastHelper (central edge-to-edge + 4-side insets for dialogs; small floating dialogs bypass it).
alerts/ pipeline: workers/AlertSyncWorker (15-min, consent-gated), routing/AlertProviderRouter, providers/AlertProvider + EcccProvider (Datamart CAP-CP scrape) + MeteoAlarmProvider + meteoalarm/MeteoGateClient (Retrofit) + MeteoGateModels, cache/GlobalAlertCache, models/UnifiedAlert + CapModels + AlertFetchResult, CapXmlParser (DTD disabled, 5 MB ceiling), AlertMatcher (polygon/circle, 1e-9 edge epsilon), AlertNotificationHelper, AlertBannerManager, AlertConsentDialog, utils/CountryCodeMapper + CountryCodeResolver, AlertLogRepository (Mutex-guarded) + AlertLogEntry, GlobalAlertTestHarness (debug-only menu, typed isTest flag).
lunar/ FROZEN Meeus math: AstroTime, AstronomyMath, AtmosphericRefraction, DeltaT (Espenak polynomials), LunarCalculator, LunarDataTypes, LunarEclipseCalculator, LunarPosition, MoonPhaseCalculator, MoonRiseSetCalculator, Observer, SolarPosition, TopocentricConverter.
solar/ FROZEN math: SolarPositionEngine, SolarRiseSetSolver, SolarLightingEngine (twilight/golden/blue), SolarDataTypes, Observer.
almanac/: MoonAlmanacManager, LunarCalendarManager, LunarDayAdapter, LunarCacheManager (3-yr event cache), CalendarIntentHelper. Gson-serialized almanac classes carry @Keep.

## theme-engine (portable; zero app coupling)
ThemeMode (int-constant modes), ColorPalette (typed tokens), BoutiqueThemeEngine (getPalette(mode), luminance-derived contrast). Public surface = ThemeMode + getPalette only.

## Manifest components
Activity: .MainActivity (only Activity). Receivers: .WeatherWidgetProvider4x2 / 4x1 / 2x1 (exported=true, custom ACTION_WIDGET_SWITCH_TAB; receivers validate widget ID ownership and tab-index bounds). No services, no AlarmManager; WorkManager only.
Permissions: INTERNET, POST_NOTIFICATIONS, SET_ALARM only.
Backup: allowBackup=true, but backup_rules.xml excludes all app SharedPreferences (location, widget, alert cache, alert log, ECCC memo); data_extraction_rules.xml governs Android 12+.

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
app/src/test/: 216 unit tests (v0.8.1) mirroring packages (root, alerts, alerts/workers, lunar, solar, almanac). A falling count is a defect.
Known debt: two same-named SolarLightingEngineTest classes (test root and test solar/) — merge pending. No CI workflow yet.

## Root & website
Gradle: settings.gradle.kts, root build.gradle.kts, app/build.gradle.kts, theme-engine/build.gradle.kts, gradle/libs.versions.toml, app/proguard-rules.pro, app/src/main/keepRules/rules.keep.
Website at repo root = weatherapplibre.ca (CNAME). Pages fetched into in-app WebView with 7-day cache; navigation allowlisted to weatherapplibre.ca, external links open in system browser: index, about, almanac, changelog, licenses, privacypolicy, roadmap, support, usermanual, template.html. changelog.txt is the plain-text changelog source. docs/extensions.md = unimplemented draft schema v1.

## Data flow
Forecast: manager → WeatherRepository → Open-Meteo (api, ensemble-api, air-quality-api, geocoding-api) → SharedPreferences JSON cache → widgets draw cache-only.
Alerts: AlertSyncWorker → router → EcccProvider (dd.weather.gc.ca) | MeteoAlarmProvider (api.meteogate.eu) → GlobalAlertCache → banner + notification (opt-in).
Astronomy: pure on-device compute from lat/lon/elev + ZoneId; UTC Instants throughout.

## Key decisions
Locations manual; alerts strictly opt-in (ALERT_CONSENT_OPT_IN); zero synthetic alerts in release builds.
No DI, no Compose, no ViewModel/StateFlow: ViewBinding + manager classes mutate views directly.
Network calls use non-blocking enqueue() wrappers (no call.execute()).
Secrets via local.properties → BuildConfig (METEOGATE_API_KEY); never committed. Missing key → EU alerts fail fast, rest of app unaffected.
GPLv3 + trademark reservation (NOTICE). Open decision: play-app-update-ktx blocks F-Droid eligibility.

## Conventions the agent keeps getting wrong
Never "fix" solar/lunar math; precision bugs live in serialization/display layers.
Coordinates/elevation Double end-to-end (putDouble/getDouble raw long bits); Float truncation caused a 5-min sunrise error.
Alert providers return typed Failure (AlertFetchResult), never emptyList() on error — empty wipes the cache.
CAP expires may carry offsets (+02:00): OffsetDateTime.parse, not Instant.parse.
Dismissals coordinate-scoped (app_alert_dismissed_<lat>_<lon>_<id>); test alert IDs timestamped.
ECCC Datamart: /today/alerts/cap/{YYYYMMDD}/{EEEE}/{hh}/*.cap; SK→CWWG, MB→CWWG, AB→CWEG, ON→CWTO, QC→CWUL, BC→CWVR, Atlantic→CWHX; 24-h lookback.
Strings externalized EN+FR with French typography (space before :;!?»); formatting only via UnitUtils/WeatherCodeUtils.
Colors only from theme-engine palette tokens; no hex in views/adapters/drawables.
Edge-to-edge: full-screen dialogs go through SystemBarContrastHelper; small floating dialogs must NOT (0.8.1 regression).
ALWAYS use project-relative paths for tooling.

## Do not touch
lunar/, solar/ math cores (verified ≤1 min / ≤1° vs official sources).
verifyEdgeToEdge build gate (do not disable or loosen).
local.properties, keystores, app/release/ binaries: never commit.
Statcounter snippet at bottom of every *.html.
