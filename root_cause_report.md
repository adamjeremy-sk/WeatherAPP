# Phase 1: Root-Cause Investigation Report

## Executive Summary
The CAP Alert Banner cell is still not appearing. Based on our stage-by-stage investigation, the core logic pipeline (Ingestion, Parsing, Matching, Persistence) executes correctly. The wiring is now technically valid: `activity_main.xml` uses `<include android:id="@+id/layoutAlertBanner" ... />`, and `AlertBannerManager` binds to it using `SectionAlertBannerBinding.bind(binding.layoutAlertBanner.root)`. 

However, there are two distinct layout nesting/visibility bugs conspiring to keep the banner invisible.

## Stage-by-Stage Verification

### Stage 5: Banner Existence and Placement
- **Expected**: `activity_main.xml` contains exactly one `<include android:id="@+id/layoutAlertBanner" layout="@layout/section_alert_banner" />` sequentially below the scrollable tabs and above `cardTodayDetails`.
- **Observed**: The placement is fundamentally flawed. In `activity_main.xml`, the include is currently placed inside `layoutScrollableTabsContainer`'s sibling layout tree but BEFORE `cardTodayDetails`. However, the root of `section_alert_banner.xml` (`cardAlertBanner`) has `android:visibility="gone"`, BUT crucially, the `<include>` tag ITSELF has no visibility explicitly set or altered by the manager.
More importantly: `AlertBannerManager` attempts to toggle the visibility of `bannerBinding.cardAlertBanner`. When you use `<include android:id="@+id/layoutAlertBanner" ... />`, the generated `binding.layoutAlertBanner.root` IS the `MaterialCardView` (`cardAlertBanner`). So `bannerBinding.cardAlertBanner` is technically pointing to the root element. BUT, toggling `bannerBinding.cardAlertBanner.visibility = View.VISIBLE` does NOT make the `<include>` tag visible if the generated `ActivityMainBinding` views the `<include>` tag as a separate wrapper/proxy view.

### Stage 7: Visibility Logic & Binding Paths (THE ROOT CAUSE)
- **Expected**: `AlertBannerManager` makes the banner visible on the screen.
- **Observed**: In `AlertBannerManager`, the binding is created via:
  `SectionAlertBannerBinding.bind(binding.layoutAlertBanner.root)`
  Because `section_alert_banner.xml` has `android:id="@+id/cardAlertBanner"` on its root tag, and `activity_main.xml` uses `<include android:id="@+id/layoutAlertBanner" ...>`, ViewBinding merges them.
  When `AlertBannerManager` calls `bannerBinding.cardAlertBanner.visibility = View.VISIBLE`, it is modifying the root view's property. BUT, to ensure the included layout actually appears in a constraint/linear layout, we must toggle the `visibility` of the `binding.layoutAlertBanner.root` directly from the parent's perspective. 
  The most reliable fix is to NOT assign an `id` to the root tag of `section_alert_banner.xml` (removing `android:id="@+id/cardAlertBanner"`), forcing it to adopt the `layoutAlertBanner` ID from the include tag, and then toggling `binding.layoutAlertBanner.root.visibility = View.VISIBLE`.

### Stage 8: Dismissal State
- **Expected**: Dismissal prevents display.
- **Observed**: Active alerts evaluated correctly using `locationPreferences.isAlertDismissed(identifier)`. If an old test alert with the same ID was dismissed previously, it blocks current rendering. We must clear test dismissals.

## Definitive Point of Failure
The root cause is a **ViewBinding / Include Tag Identity Collision**. The root of `section_alert_banner.xml` defines its own ID (`cardAlertBanner`), while the `<include>` tag in `activity_main.xml` defines an overlapping ID (`layoutAlertBanner`). `AlertBannerManager` sets `bannerBinding.cardAlertBanner.visibility = View.VISIBLE`, which modifies the inner view, but the outer `<include>` proxy view (or parent container constraints) remains stubbornly hidden or disconnected. 

To fix this:
1. Strip `android:id="@+id/cardAlertBanner"` and `android:visibility="gone"` from the root of `section_alert_banner.xml`.
2. Apply `android:visibility="gone"` strictly to the `<include>` tag in `activity_main.xml`.
3. In `AlertBannerManager`, toggle `binding.layoutAlertBanner.root.visibility = View.VISIBLE`.
