# Phase 1: Inventory

## 1. Views and Ownership

| View ID | Current Parent Chain | Current Kotlin Owner | Target Section |
|---------|----------------------|----------------------|----------------|
| `drawerLayout` | Root | `MainActivity`, `ThemeManager` | `activity_main.xml` |
| `main` | `drawerLayout` | `ThemeManager` | `activity_main.xml` |
| `snowflakeOverlay` | `main` | `EasterEggManager` | `section_overlays.xml` |
| `heatShimmerOverlay` | `main` | `EasterEggManager` | `section_overlays.xml` |
| `cardHeader` | `LinearLayout` < `main` | `ThemeManager` | `section_header.xml` |
| `btnMenu` | `ViewGroup` < `cardHeader` | `MainActivity`, `ThemeManager` | `section_header.xml` |
| `layoutLocationClickable` | `ViewGroup` < `cardHeader` | `MainActivity` | `section_header.xml` |
| `ivPin` | `layoutLocationClickable` | `ThemeManager` | `section_header.xml` |
| `tvLocationName` | `layoutLocationClickable` | `ThemeManager`, `WeatherDisplayManager` | `section_header.xml` |
| `tvAttribution` | `layoutLocationClickable` | `ThemeManager`, `WeatherDisplayManager` | `section_header.xml` |
| `layoutStickyTabsContainer` | `LinearLayout` < `main` | (None) | `section_sticky_tabs.xml` |
| `swipeRefresh` | `LinearLayout` < `main` | `MainActivity`, `WeatherDisplayManager` | `activity_main.xml` |
| `layoutScrollableTabsContainer` | `ScrollView` < `swipeRefresh` | (None) | `section_tabs_area.xml` |
| `cardLocationTabs` | `layoutScrollableTabsContainer` | `ThemeManager` | `section_tabs_area.xml` |
| `tabLayoutLocations` | `cardLocationTabs` | `MainActivity`, `ThemeManager` | `section_tabs_area.xml` |
| `cardAlertBanner` | `ScrollView` < `swipeRefresh` | `WeatherDisplayManager`, `ThemeManager` | `section_alert_banner.xml` |
| `tvAlertHeaderTitle` | `cardAlertBanner` | `WeatherDisplayManager`, `ThemeManager` | `section_alert_banner.xml` |
| `btnDismissAlert` | `cardAlertBanner` | `WeatherDisplayManager` | `section_alert_banner.xml` |
| `tvAlertHeadline` | `cardAlertBanner` | `WeatherDisplayManager` | `section_alert_banner.xml` |
| `tvAlertInstructions` | `cardAlertBanner` | `WeatherDisplayManager` | `section_alert_banner.xml` |
| `tvAlertSource` | `cardAlertBanner` | (None) | `section_alert_banner.xml` |
| `cardTodayDetails` | `ScrollView` < `swipeRefresh` | `ThemeManager` | `activity_main.xml` |
| `tvSelectedDateHeader` | `cardTodayDetails` | `WeatherDisplayManager`, `ThemeManager` | `section_today_summary.xml` |
| `btnInlineMenu` | `cardTodayDetails` | `MainActivity`, `AstronomyDisplayManager` | `section_inline_subtabs.xml` |
| `layoutHeroSection` | `cardTodayDetails` | `ThemeManager` | `section_today_summary.xml` |
| `tvCurrentTemp` | `layoutHeroSection` | `WeatherDisplayManager`, `ThemeManager` | `section_today_summary.xml` |
| `tvMainEmoji` | `layoutHeroSection` | `WeatherDisplayManager` | `section_today_summary.xml` |
| `tvMainCondition` | `layoutHeroSection` | `WeatherDisplayManager`, `ThemeManager` | `section_today_summary.xml` |
| `layoutWeatherDetailsContainer`| `cardTodayDetails` | `AstronomyDisplayManager` | `section_inline_subtabs.xml` |
| `cardTempRange` | `layoutWeatherDetailsContainer` | (None) | `section_today_summary.xml` |
| `cardHighTemp` | `cardTempRange` | `MainActivity`, `ThemeManager` | `section_today_summary.xml` |
| `btnDismissHighEffect` | `cardHighTemp` | `MainActivity` | `section_today_summary.xml` |
| `tvDailyHigh` | `cardHighTemp` | `WeatherDisplayManager`, `ThemeManager` | `section_today_summary.xml` |
| `cardLowTemp` | `cardTempRange` | `MainActivity`, `ThemeManager` | `section_today_summary.xml` |
| `btnDismissLowEffect` | `cardLowTemp` | `MainActivity` | `section_today_summary.xml` |
| `tvDailyLow` | `cardLowTemp` | `WeatherDisplayManager`, `ThemeManager` | `section_today_summary.xml` |
| `layoutMetricWind` | `layoutWeatherDetailsContainer` | `ThemeManager` | RETIRED (`section_metric_grid.xml`) |
| `tvWindSpeed` | `layoutMetricWind` | `WeatherDisplayManager`, `ThemeManager` | RETIRED (`section_metric_grid.xml`) |
| `tvWindGust` | `layoutMetricWind` | `WeatherDisplayManager`, `ThemeManager` | RETIRED (`section_metric_grid.xml`) |
| `layoutMetricAqi` | `layoutWeatherDetailsContainer` | `ThemeManager` | RETIRED (`section_metric_grid.xml`) |
| `tvAirQuality` | `layoutMetricAqi` | `WeatherDisplayManager`, `ThemeManager` | RETIRED (`section_metric_grid.xml`) |
| `tvHumidity` | `layoutMetricAqi` | `WeatherDisplayManager`, `ThemeManager` | RETIRED (`section_metric_grid.xml`) |
| `layoutMetricUv` | `layoutWeatherDetailsContainer` | `ThemeManager` | RETIRED (`section_metric_grid.xml`) |
| `tvUvIndex` | `layoutMetricUv` | `WeatherDisplayManager`, `ThemeManager` | RETIRED (`section_metric_grid.xml`) |
| `layoutMetricVisibility` | `layoutWeatherDetailsContainer` | `ThemeManager` | RETIRED (`section_metric_grid.xml`) |
| `tvVisibility` | `layoutMetricVisibility`| `WeatherDisplayManager`, `ThemeManager` | RETIRED (`section_metric_grid.xml`) |
| `layoutMetricCeiling` | `layoutWeatherDetailsContainer` | `ThemeManager` | RETIRED (`section_metric_grid.xml`) |
| `tvCeiling` | `layoutMetricCeiling` | `WeatherDisplayManager`, `ThemeManager` | RETIRED (`section_metric_grid.xml`) |
| `layoutMetricSunrise` | `layoutWeatherDetailsContainer` | `ThemeManager` | `section_sun_moon.xml` |
| `tvSunrise` | `layoutMetricSunrise` | `WeatherDisplayManager`, `ThemeManager` | `section_sun_moon.xml` |
| `tvSunriseAzimuth` | `layoutMetricSunrise` | `WeatherDisplayManager`, `ThemeManager` | `section_sun_moon.xml` |
| `layoutMetricSunset` | `layoutWeatherDetailsContainer` | `ThemeManager` | `section_sun_moon.xml` |
| `tvSunset` | `layoutMetricSunset` | `WeatherDisplayManager`, `ThemeManager` | `section_sun_moon.xml` |
| `tvSunsetAzimuth` | `layoutMetricSunset` | `WeatherDisplayManager`, `ThemeManager` | `section_sun_moon.xml` |
| `layoutMetricDaylight` | `layoutWeatherDetailsContainer` | `ThemeManager` | `section_sun_moon.xml` |
| `tvTotalDaylight` | `layoutMetricDaylight`| `WeatherDisplayManager`, `ThemeManager` | `section_sun_moon.xml` |
| `layoutMetricMoon` | `layoutWeatherDetailsContainer` | `WeatherDisplayManager`, `ThemeManager`| `section_sun_moon.xml` |
| `tvMoonPhase` | `layoutMetricMoon` | `WeatherDisplayManager`, `ThemeManager` | `section_sun_moon.xml` |
| `tvMoonCyclePercentage` | `layoutMetricMoon` | `WeatherDisplayManager`, `ThemeManager` | `section_sun_moon.xml` |
| `layoutMetricGoldenBlueHourContainer` | `layoutWeatherDetailsContainer` | `WeatherDisplayManager`, `ThemeManager` | `section_inline_subtabs.xml` |
| `tvMorningGolden` | `layoutMetricGoldenBlueHourContainer` | `WeatherDisplayManager` | `section_inline_subtabs.xml` |
| `tvEveningGolden` | `layoutMetricGoldenBlueHourContainer` | `WeatherDisplayManager` | `section_inline_subtabs.xml` |
| `tvMorningBlue` | `layoutMetricGoldenBlueHourContainer` | `WeatherDisplayManager` | `section_inline_subtabs.xml` |
| `tvEveningBlue` | `layoutMetricGoldenBlueHourContainer` | `WeatherDisplayManager` | `section_inline_subtabs.xml` |
| `layoutInlineGoldenHourContainer` | `cardTodayDetails` | `AstronomyDisplayManager` | `section_inline_subtabs.xml` |
| `btnInlinePrevDay` | `layoutInlineGoldenHourContainer` | `MainActivity` | `section_inline_subtabs.xml` |
| `layoutInlineDateClickable` | `layoutInlineGoldenHourContainer`| `MainActivity` | `section_inline_subtabs.xml` |
| `tvInlineSelectedDate` | `layoutInlineDateClickable` | `AstronomyDisplayManager` | `section_inline_subtabs.xml` |
| `btnInlineNextDay` | `layoutInlineGoldenHourContainer` | `MainActivity` | `section_inline_subtabs.xml` |
| `rvInlineSolarPhases` | `layoutInlineGoldenHourContainer` | `MainActivity` | `section_inline_subtabs.xml` |
| `rvForecast` | `ScrollView` < `swipeRefresh` | `MainActivity` | `section_forecast.xml` |
| `pbLoading` | `main` | `WeatherDisplayManager` | `section_states.xml` |
| `layoutError` | `main` | `WeatherDisplayManager` | `section_states.xml` |
| `tvErrorMessage` | `layoutError` | `WeatherDisplayManager` | `section_states.xml` |
| `btnRetry` | `layoutError` | `MainActivity` | `section_states.xml` |
| `btnDismissError` | `layoutError` | `MainActivity` | `section_states.xml` |
| `navigationView` | `drawerLayout` | `MainActivity`, `ThemeManager` | `activity_main.xml` |

## 2. Visibility Groupings
- `layoutWeatherDetailsContainer`: Toggled by `AstronomyDisplayManager` sub-tab switch.
- `layoutInlineGoldenHourContainer`: Toggled by `AstronomyDisplayManager` sub-tab switch.
- `layoutMetricGoldenBlueHourContainer`: Toggled by `LocationPreferences.isShowGoldenBlueHourMainPageEnabled()`.
- `tvWindGust`: Toggled by wind gust logic (RETIRED; moved to adapter).
- `layoutMetricCeiling`: Toggled by ceiling logic (RETIRED; moved to adapter).
- `layoutError` & `pbLoading`: Toggled by `WeatherDisplayManager`.

## 3. Runtime Manipulated Views
- `cardLocationTabs`: Future-proofed for sticky margins (currently no sticky logic actively running in Kotlin, but `layoutStickyTabsContainer` vs `layoutScrollableTabsContainer` preserves this structure).
- `cardAlertBanner`: Appearance updated by themes, might be margin manipulated.
- `cardTodayDetails`: Shape appearance / margins could be manipulated.

## 4. Proposed Composition Tree
```
activity_main.xml
 ├── drawerLayout
      ├── main
      │    ├── include layout="@layout/section_overlays"
      │    ├── include layout="@layout/section_header"
      │    ├── include layout="@layout/section_sticky_tabs"
      │    ├── swipeRefresh > NestedScrollView > LinearLayout
      │    │    ├── include layout="@layout/section_tabs_area"
      │    │    ├── include layout="@layout/section_alert_banner"
      │    │    ├── cardTodayDetails > LinearLayout
      │    │    │    ├── include layout="@layout/section_today_summary"
      │    │    │    └── include layout="@layout/section_inline_subtabs"
      │    │    └── include layout="@layout/section_forecast"
      │    └── include layout="@layout/section_states"
      └── navigationView
```

*Note on section_inline_subtabs*: It will encapsulate `layoutWeatherDetailsContainer` (which in turn includes `section_metric_grid` and `section_sun_moon`), `layoutInlineGoldenHourContainer`, and `btnInlineMenu`.

Inventory complete.
