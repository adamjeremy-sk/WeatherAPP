# Master Prompt Examples (Structural Reference)

*Historical examples of gated prompt structure. Prompt text preserved verbatim except for formatting cleanup and the addition of verification gates where none existed.*

---

## Example 1: Semantic Theme Engine

You are implementing a Semantic Theme Engine to decouple UI layouts from hardcoded colors and centralize all visual styling into a role-based view tree traversal.

CONTEXT: Currently, colors are applied via hardcoded hex values in XML/code or via static @color/ resources. This prevents clean boutique palette switching and requires repetitive boilerplate in Manager classes. We will introduce `ThemeRole` tags in XML, and an engine that walks the view tree to apply the active palette's tokens dynamically.

### STEP 0: AUDIT (READ-ONLY, NO EDITS)

0.1 Quote the current `BoutiqueThemeEngine.applyTheme()` implementation to understand how themes are currently dispatched.
0.2 Identify one complex layout file (e.g., `dialog_alerts_settings.xml` or `item_hourly_row.xml`) that currently uses hardcoded hex or @color/ attributes to serve as the migration pilot.

STOP AND WAIT FOR MY CONFIRMATION.

### STEP 1: DEFINE ROLES AND ATTRIBUTES

1.1 Create a `ThemeRole` enum in the main source set, mapping every semantic role to a property in `ColorPalette` (e.g., `TEXT_PRIMARY`, `SURFACE_CARD`, `ALERT_STROKE_EXTREME`, `TEXT_SECONDARY`).
1.2 Add a custom XML attribute `app:themeRole` in `res/values/attrs.xml`.
1.3 Implement a custom View wrapper or a static helper `ThemeRoleReader` that can parse the `themeRole` attribute from an `AttributeSet`.

EVIDENCE: Verbatim enum, attrs.xml diff, and the attribute reader.

STOP AND WAIT FOR MY CONFIRMATION.

### STEP 2: THE TREE WALKER (THEME BINDER)

2.1 Implement `ThemeBinder.bind(view: View, palette: ColorPalette)` in the BoutiqueThemeEngine or a dedicated helper.
2.2 It must recursively walk the view tree.
2.3 For each view, check for the `app:themeRole` attribute (using the reader from 1.3).
2.4 Apply the corresponding palette token based on the view type:
- TextView / Button: `setTextColor()`
- MaterialCardView: `setCardBackgroundColor()`, `strokeColor`
- ImageView: `setColorFilter()` or `imageTintList`
- Generic View: `setBackgroundColor()`
2.5 The walker must be efficient and avoid unnecessary allocations.

EVIDENCE: Verbatim implementation of the Tree Walker.

STOP AND WAIT FOR MY CONFIRMATION.

### STEP 3: PILOT MIGRATION

3.1 Take the pilot layout identified in Step 0.2.
3.2 Strip every `android:textColor`, `app:cardBackgroundColor`, `app:strokeColor`, and `android:background` that points to a hardcoded hex or @color/.
3.3 Replace them with `app:themeRole="..."` using the enum values from Step 1.
3.4 In the Manager class that inflates this layout, replace the manual palette color application code with a single call to `ThemeBinder.bind(rootView, activePalette)`.

EVIDENCE: Verbatim before and after of the XML layout and the Manager binding code.

STOP AND WAIT FOR MY CONFIRMATION.

### STEP 4: VERIFICATION

4.1 Full test suite verbatim.
4.2 I will verify on device that the migrated screen themes correctly across light, dark, and boutique palettes, and that the Manager class no longer contains manual color-setting boilerplate for that screen.
4.3 Edited-file list.

STOP AND WAIT FOR MY CONFIRMATION.

### RAILS

1. This is a pilot migration. Do not migrate the entire application yet.
2. No new third-party dependencies.
3. The ThemeBinder must not crash if it encounters a view type it doesn't explicitly know how to style.

### DEFINITION OF DONE

1. The ThemeRole enum and XML attribute exist.
2. The Tree Walker successfully applies palette tokens dynamically.
3. One complex screen is fully migrated to the Semantic Theme Engine, proving the architecture works.

---

## Example 2: Dead-Code Elimination and Legacy Cleanup

You are executing a dead-code elimination and legacy cleanup pass on the WeatherAPP Libre codebase. Following the recent massive architectural refactoring (decomposing MainActivity, modularizing XML layouts, and rebuilding the solar/lunar engines), there are likely orphaned files, unused adapters, legacy layout files, and obsolete utility classes remaining in the project. Your objective is to identify and permanently remove all orphaned and legacy code, resources, and assets.

### 1. STRICT SAFETY RULES

1. DO NOT GUESS. Before deleting ANY file, you must perform a comprehensive project-wide search (grep) for its class name, layout name, or resource ID to ensure it is not referenced anywhere in the Kotlin code, XML layouts, AndroidManifest.xml, or navigation graphs.
2. If a file has even ONE active reference, it is NOT orphaned. Do not delete it. Instead, list it in your report as "Active - Retained" with the file that references it.
3. Do not delete any file inside the `lunar` or `solar` packages unless you can prove with 100% certainty that it is an abandoned draft (e.g., an old `SolarCalculator.kt` that was replaced by `SolarPositionEngine`).
4. Do not modify the logic of any surviving files during this cleanup pass. This is strictly a deletion and import-cleanup task.
5. After deletions, the project MUST compile successfully with zero errors.

### 2. PRIMARY SUSPECTS FOR AUDIT

Investigate the following specific areas based on the current project structure:

2.1 Redundant or Orphaned Adapters
The project currently contains multiple adapters. Verify which are actively used by RecyclerViews and which are legacy:
- SolarPhaseAdapter.kt vs SolarEventAdapter.kt (Are both used? Is one a legacy remnant?)
- DayForecastAdapter.kt (Verify it is still used by the forecast RecyclerView).
- MetricGridAdapter.kt (Verify it is used by the new section_metric_grid.xml).
- LocationSearchAdapter.kt (Verify it is used by dialog_search_location.xml).
- Any other adapter not explicitly bound to a RecyclerView in a Manager or Activity.

2.2 Orphaned Data Classes
- SolarPhaseDetail.kt (Was this replaced by a new data model in the solar package? Check if it is still used by the UI).
- LunarDataTypes.kt vs SolarDataTypes.kt (Ensure these are actively imported by the calculators and UI managers).
- WeatherModels.kt (Ensure this is the active source of truth for ForecastModel enums and is not duplicated elsewhere).

2.3 Legacy Layout Files
Audit `res/layout/` for files that are no longer inflated:
- activity_main.xml (Must be retained, but verify it only contains <include> tags and no legacy leaf views).
- Are there old monolithic layout files that were replaced by the `section_*.xml` files?
- Verify all `dialog_*.xml` files are actively inflated by SettingsDialogManager or LocationDialogManager.
- Verify all `item_*.xml` files are actively inflated by their respective adapters.
- Verify all `section_*.xml` files are actively included in activity_main.xml or other parent layouts.
- Check for orphaned layouts like `activity_radar.xml`, `activity_solar_events.xml`, or `activity_golden_hour.xml`. If their corresponding Activities exist and are registered in the Manifest, retain them. If they are empty stubs or unregistered, flag them for deletion.

2.4 Legacy Assets and Resources
- assets/privacypolicy.md: We recently moved the privacy policy to render from an external URL (https://adamjeremy-sk.github.io/WeatherAPP/privacypolicy.html). Verify if this local markdown file is still being read by any Kotlin code. If not, delete it.
- res/drawable/: Identify any vector drawables or PNGs that are no longer referenced in any XML layout or Kotlin code.
- res/values/strings.xml: Identify and remove any string resources that are no longer referenced (use Android Studio's lint tools or grep).

2.5 Obsolete Managers or Helpers
- WeatherNotificationHelper.kt: Verify this is actively used by the new alert system. If the new CAP-CP system uses a different helper, this might be legacy.
- UnitUtils.kt: Verify all functions inside are actively called. Remove any unused utility functions within the file.
- WeatherCodeUtils.kt: Verify this is still used. (Note: Open-Meteo weather codes are still used for the main forecast display, so this should likely be retained, but verify).

### 3. EXECUTION PHASES

PHASE 1: The Audit Report (DO NOT DELETE YET)
Generate a table listing every file you suspect is orphaned. For each file, provide:
- File Name
- Suspected Status (Orphaned / Active)
- Grep Search Result (Number of references found, and which files reference it)
- Recommendation (Delete / Retain)
Present this report and wait for confirmation before proceeding to Phase 2.
(Self-correction for autonomous execution: If you are running autonomously without human intervention, proceed to Phase 2 ONLY for files with ZERO references. If a file has >0 references but you suspect it is legacy, mark it as "Requires Human Review" and do not delete it.)

PHASE 2: Safe Deletion
1. Delete all files confirmed to have ZERO references.
2. Remove the corresponding imports from any surviving Kotlin files.
3. Remove any unused IDs from `res/values/ids.xml` (if applicable).

STOP AND WAIT FOR MY CONFIRMATION.

PHASE 3: Verification
1. Run a full project build.
2. If the build fails, immediately restore the deleted file that caused the failure, fix the reference, and document the mistake.
3. Run Android Studio's "Remove Unused Resources" tool (or equivalent Gradle task `shrinkResources` analysis) to catch any remaining dead XML or drawables.

STOP AND WAIT FOR MY CONFIRMATION.

### 4. REQUIRED OUTPUT

Produce a final Cleanup Manifest containing:
1. List of Deleted Files (with proof of zero references).
2. List of Retained Files (that looked suspicious but were proven active).
3. List of Files Flagged for Human Review (files with references that look like legacy technical debt).
4. Confirmation that the project compiles successfully after all deletions.

---

## Example 3: Test Harness Notifications and Alert Banner

We need to address two several critical bugs.

### CRITICAL SAFETY RULES

1. Make surgical, targeted edits. Do not rewrite entire files unless explicitly instructed to consolidate them.
2. Respect the existing Manager-based architecture.
3. activity_main.xml is a protected composition root. You may ONLY add <include> tags to it. Do not paste raw CardView or LinearLayout blocks into it.
4. MainActivity.kt is a protected lifecycle router. Do not add direct ViewBinding references (e.g., binding.tvAlert...) or business logic to it. Delegate everything to Managers.
5. Verify all changes compile successfully before finishing.

### TASK 1: Fix Test Harness System Notifications (Options 1 & 2)

The Tornado Warning (Option 1) and Severe Thunderstorm Warning (Option 2) in the test harness are not triggering their respective system notifications.

Requirement:
1. Investigate the test harness injection pipeline (likely in SettingsDialogManager or the Alert testing controller).
2. Ensure that when these specific test fixtures are injected, they bypass any "live fetch only" restrictions and are passed directly to the WeatherNotificationHelper (or equivalent notification mapper).
3. Verify that the severity mapping correctly routes Extreme (Tornado) and Severe (Thunderstorm) to the IMPORTANCE_HIGH notification channel.
4. Ensure the test notification is actually dispatched via NotificationManager.notify() and is not being silently swallowed by a missing permission check or incorrect context.

STOP AND WAIT FOR MY CONFIRMATION.

### TASK 2: Implement the Alert Banner Cell

Currently, no alerts (live or test) create a visible cell on the main weather page.

Requirement:
1. Create a new layout file: res/layout/section_alert_banner.xml. This must be a standalone CardView/MaterialCardView cell.
2. In activity_main.xml, surgically insert an <include> tag for this new section. It MUST be placed sequentially AFTER the location tabs include and BEFORE the today's summary include. Do not alter any other lines in activity_main.xml.
3. Create an AlertBannerManager class. This manager must own the ViewBinding for section_alert_banner.
4. The AlertBannerManager must:
- Observe the active tab/location.
- Query the alert repository/state for an active, un-dismissed alert for that specific location.
- If an alert exists, populate the banner (Event, Headline, Severity styling) and set visibility to VISIBLE.
- If no alert exists, or if it was dismissed by the user, set visibility to GONE.
- Handle the dismiss button click: persist the dismissal state per-location/per-alert-ID, hide the banner, and log the action.
5. Wire the AlertBannerManager into MainActivity.kt using 10 lines or fewer (instantiation and setup only).

STOP AND WAIT FOR MY CONFIRMATION.

### DEFINITION OF DONE

This task is complete only when ALL of the following criteria are met:
1. Test Notifications: Injecting the Tornado Warning and Severe Thunderstorm Warning via the test harness successfully triggers high-importance system notifications.
2. Alert Banner: A distinct alert banner cell appears between the location tabs and the today's forecast when an alert is active. It swaps content based on the active tab, persists dismissal per-location, and hides when dismissed.
3. Theme Reactivity: The Golden Hour and Blue Hour cells instantly update their colors when switching between any light, dark, or boutique themes.
4. Metric Alignment: The 10 metric cells under the current temperature are consolidated, uniformly spaced, perfectly aligned, and do not shift when data is missing.
5. Selective Wipe: Installing a build with a higher versionCode clears all weather cache, alerts, and logs, but perfectly retains the user's theme, cities, tab settings, units, TTL, and model preferences.
6. Architecture & Safety: activity_main.xml remains under 520 lines with only <include> tags added. MainActivity.kt has zero direct UI manipulation for the alert banner. All logic is properly delegated to AlertBannerManager, ThemeManager, and the Migration routine.
7. Build Status: The project compiles successfully with zero errors and no broken ViewBinding references.

---

## Example 4: 7-Day Forecast Atmospheric Phenomena Cleanup

You are executing a 7-Day Forecast Atmospheric Phenomena Cleanup for WeatherAPP Libre. This prompt is self-contained.

CONTEXT (BINDING):
- The 7-day forecast expanded cell currently displays atmospheric lunar phenomena lines (e.g., "Halo lunaire possible (70 %)").
- These should be removed from the 7-day forecast and kept only in the Moon Phase & Cycle popup.
- The UI element to remove is the TextView with id="tv_atmospheric_phenomena" in the expanded cell layout.

### STRICT RAILS & CONSTRAINTS (DO NOT VIOLATE)

1. DO NOT MODIFY MATH OR ASTRONOMICAL CALCULATION ENGINES.
2. DO NOT ADD NEW UI ELEMENTS; only remove the existing atmospheric phenomena display.
3. EXECUTION PROTOCOL: One step at a time. End each step with "STOPPING AND WAITING FOR VERIFICATION" and provide read-back proof.

### STEP 1: READ-ONLY AUDIT

1.1 Locate the expanded cell layout and adapter binding for the atmospheric phenomena:
- Layout File: item_day_forecast_expanded.xml (res/layout/)
- TextView ID: tv_atmospheric_phenomena
- Binding Code: DayForecastAdapter.kt (binding.tvAtmosphericPhenomena)
1.2 Verify the Moon Phase & Cycle popup still displays these phenomena (for confirmation they are not removed globally).
1.3 OUTPUT: Quote the layout XML block containing tv_atmospheric_phenomena and the adapter binding line.

STOP AND WAIT FOR MY VERIFICATION BEFORE PROCEEDING TO STEP 2.

### STEP 2: REMOVE ATMOSPHERIC PHENOMENA

2.1 DELETE the TextView with id="tv_atmospheric_phenomena" from the expanded cell layout (item_day_forecast_expanded.xml).
2.2 DELETE the binding code that sets tv_atmospheric_phenomena.text in DayForecastAdapter.kt.
2.3 READ-BACK PROOF: Quote the removed layout XML block and the removed binding line.

STOP AND WAIT FOR MY VERIFICATION BEFORE PROCEEDING TO STEP 3.

### STEP 3: BUILD & TEST

3.1 Run :app:assembleDebug and confirm BUILD SUCCESSFUL.
3.2 Run :app:testDebugUnitTest and confirm all 152 tests pass.
3.3 Report exact pass/fail counts.

STOP AND WAIT FOR MY FINAL CONFIRMATION.