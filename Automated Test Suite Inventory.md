Automated Test Suite Inventory
This document provides a comprehensive, exhaustive inventory of the 137 automated unit tests across all test modules in the WeatherAPP codebase.
1. General & Manager Integration Tests (AstronomicalManagersTest)
•
testDarkSkyManager_returnsValidWindows
◦
Calculates astronomical dark sky windows for Ottawa on the spring equinox and verifies that all window intervals have valid UTC start/end ordering, positive durations, interference indices within [0, 100], and valid quality labels.
•
testDarkSkyManager_sevenNightsCalculation
◦
Computes dark sky window summaries for a 7-consecutive-night range starting on the spring equinox and asserts that summaries and window lists are generated for every night.
•
testDarkSkyManager_highLatitudeSummerNoDarkness
◦
Tests dark sky window calculation in Tromsø, Norway during summer solstice (midnight sun) and verifies that no dark sky windows are returned and an appropriate explanatory note is present.
•
testNightVisibilityManager_returnsValidPeriods
◦
Computes natural night visibility periods for Ottawa on the equinox and asserts that returned periods have valid UTC time bounds, valid light category classifications, and non-null twilight statuses.
•
testNightVisibilityManager_handlesCloudCoverData
◦
Supplies a 48-hour mock cloud cover map (75% cloudiness) to NightVisibilityManager and asserts that cloud cover availability is flagged and cloud cover status strings are attached to each period.
•
testNightVisibilityManager_cloudCoverAttenuatesAmbientLight
◦
Compares visibility period calculations under 0% clear sky versus 100% heavy cloud cover to verify that cloud attenuation logic executes deterministically without error.
•
testSolunarManager_returnsValidForecast
◦
Calculates a solunar activity forecast for Ottawa on the equinox and asserts that the phase name, 0–1 illumination fraction, 0–100 activity rating, 2-hour major periods (transit/nadir), and 1-hour minor periods (moonrise/moonset) are correctly generated.
•
testSolunarManager_sevenDaysCalculation
◦
Computes 7-day solunar forecasts and verifies valid ratings, phase names, and activity periods for every day.
•
testSolunarManager_multipleGlobalLocations
◦
Tests solunar forecast calculations across 6 global test locations (Equatorial, Southern Hemisphere, Arctic, Antarctic, International Date Line, and Fractional Timezones) to ensure global coverage without exceptions.
•
testCalculations_deterministicForIdenticalInputs
◦
Evaluates solunar, dark sky, and night visibility calculations twice with identical inputs and asserts exact equality of ratings, phases, and window counts to guarantee mathematical determinism.
2. Navigation & UI Tests
Back Navigation (BackNavigationTest)
•
testShouldInterceptBack_matrix
◦
Evaluates MainActivity.shouldInterceptBack across a truth-table matrix of menu depth, navigation drawer state, and sub-tab selection to ensure correct back-button interception semantics.
Day Forecast Adapter (DayForecastAdapterTest)
•
testJoinTwoLines_standardSunPosition
◦
Tests DayForecastAdapter.joinTwoLines with standard sunrise/sunset strings and verifies that lines are joined by a single newline character without bullet artifacts.
•
testJoinTwoLines_standardMoonPosition
◦
Tests joining standard moonrise and moonset strings and asserts exact two-line formatting.
•
testJoinTwoLines_noRiseInput
◦
Asserts proper two-line formatting when sunrise/moonrise is absent ("No Rise").
•
testJoinTwoLines_noSetInput
◦
Asserts proper two-line formatting when sunset/moonset is absent ("No Set").
Feels Like Hero Component (FeelsLikeHeroTest)
•
testFeelsLikeFormattingMetricPrecisionParity
◦
Compares UnitUtils.formatTempPrecise against direct string formatting in Celsius to verify 0.1°C precision parity for apparent temperature display.
•
testFeelsLikeFormattingUsCustomaryPrecisionParity
◦
Compares UnitUtils.formatTempPrecise against direct Fahrenheit conversion to verify 0.1°F precision parity.
•
testFeelsLikeVisibilityPredicate_nullValue_yieldsGone
◦
Asserts that WeatherDisplayManager.shouldShowFeelsLike returns false (GONE) when apparent temperature is null.
•
testFeelsLikeVisibilityPredicate_notToday_yieldsGone
◦
Asserts that shouldShowFeelsLike returns false when viewing forecast days other than today.
•
testFeelsLikeVisibilityPredicate_todayAndPresentValue_yieldsVisible
◦
Asserts that shouldShowFeelsLike returns true (VISIBLE) when viewing today's forecast with a valid apparent temperature.
•
testFeelsLikeVisibilityPredicate_todayAndValueEqualsActualTemp_yieldsVisible
◦
Asserts that shouldShowFeelsLike returns true even when apparent temperature equals actual temperature.
First Launch & Seeding (FirstLaunchSeedingTest)
•
testDefaultLocationConstants
◦
Verifies the pre-configured geographic coordinates, elevation, and administrative metadata for default seed locations (Edmonton, Montreal, Halifax).
•
testFirstLaunchSeedingOrderAndCompletion
◦
Simulates first-launch preference initialization using an in-memory SharedPreferences implementation, verifying tab location seeding and first-launch flag transition.
Hourly Forecast Mapping (HourlyMapperTest)
•
testWindowSlicingTo36Hours
◦
Slices a 50-hour raw forecast stream starting at 14:00 UTC and asserts that exactly 36 hourly rows starting at current hour are produced with day dividers and no truncation footer.
•
testDstContinuitySpringForward
◦
Tests hourly forecast list building across a Daylight Saving Time spring-forward transition, asserting 36 continuous hours spaced exactly 3600 seconds apart.
•
testNullHandling
◦
Passes raw hourly data containing null temperature and precipitation values and asserts graceful mapping to null model fields.
•
testTruncationAndHorizonFooter
◦
Passes a truncated 10-hour dataset and verifies that a HorizonFooter item is appended to inform the user that data ends.
•
testContinuousListWithStaticDividers
◦
Slices a 60-hour dataset and asserts that 36 hour rows and appropriate DayDivider items are rendered in chronological sequence.
Location Preferences & Migration (LocationPreferencesMigrationTest)
•
testMigrationWithLegacyWeatherAlertsTrue
◦
Verifies that legacy app_weather_alerts_enabled = true preferences migrate to ALERT_CONSENT_OPT_IN and clean up legacy keys.
•
testMigrationWithLegacyWeatherAlertsFalse
◦
Verifies that legacy app_weather_alerts_enabled = false preferences migrate to ALERT_CONSENT_OPT_OUT.
•
testMigrationWithLegacyAlertConsentVersionOnly
◦
Tests migration when only a legacy consent version integer is present, defaulting to opt-in.
•
testMigrationWithNoLegacyKeysRemainsUnset
◦
Verifies that fresh installs with no legacy keys remain in the ALERT_CONSENT_UNSET state.
•
testMigrationIsIdempotent
◦
Executes migration twice in succession and asserts that preference state remains unchanged without error.
•
testAlertConsentStatePreservedAcrossVersionUpdate
◦
Simulates an app version update and asserts that user alert consent preferences are strictly preserved.
Menu Organization & Navigation Preferences (MenuOrganizationTest)
•
testMenuOrderPreferenceRoundTrip
◦
Saves a custom sub-tab order to SharedPreferences and verifies exact round-trip retrieval.
•
testSeparatorCountRoundTripAndClamping
◦
Asserts that default menu order configuration contains exactly two separator items.
•
testEnabledFlagsPreferenceRoundTrip
◦
Toggles sub-tab visibility flags in preferences and verifies accurate retrieval.
•
testDefaultLaunchSubTabPreferenceRoundTrip
◦
Verifies saving and reading the default launch sub-tab preference.
•
testLaunchResolutionFreshVsRecreation
◦
Tests tab resolution logic distinguishing fresh app launches (uses default launch tab) from Activity recreation (restores saved tab).
•
testActiveTabValidityEdgeCase
◦
Tests active tab fallback logic when a previously selected tab is disabled or invalidated.
•
testSevenDayInlineVisibilityPredicate
◦
Verifies visibility predicate logic for 7-day forecast display in inline vs menu mode across different sub-tabs.
•
testBuildMenuListFromOrderAndFlags
◦
Verifies menu item list construction filtering disabled items while maintaining separators.
Metric Cell UI (MetricCellTest)
•
testMetricCellModel
◦
Instantiates a MetricCell model object for wind and asserts property values.
•
testMetricCellModel_secondaryIsNull
◦
Verifies construction of a MetricCell with a null secondary text field.
Moon Phase Dialog Messaging (MoonPhaseDialogMessageTest)
•
testMoonPhaseDialogMessageFormatting
◦
Formats a multi-line moon phase detail dialog message and asserts line counts, absence of leading whitespace, and bullet formatting on upcoming phase lines.
App Root Solar Lighting Engine (SolarLightingEngineTest)
•
testSolarLightingEngineCalculation
◦
Calls SolarLightingEngine.calculateSolarLightingPhases for Ottawa on the current date and asserts non-null sunrise/sunset events with valid azimuths.
Tab Long Press (TabLongPressTest)
•
testShouldReloadAfterTabEdit_equalIndexes_returnsTrue
◦
Asserts that shouldReloadAfterTabEdit returns true when the edited tab index equals the currently active tab index.
•
testShouldReloadAfterTabEdit_differingIndexes_returnsFalse
◦
Asserts that shouldReloadAfterTabEdit returns false when the edited tab index differs from the active tab index.
•
testShouldReloadAfterTabEdit_boundaryIndexes_zeroAndCountMinusOne
◦
Tests boundary tab indices (0 and max) for tab edit reload predicate logic.
Unit Conversion Utilities (UnitUtilsTest)
•
testShouldShowGusts_nullFalse
◦
Asserts that shouldShowGusts returns false when gust speed is null.
•
testShouldShowGusts_equalToWindPlusThreeFalse
◦
Asserts that gusts equal to wind speed + 3.0 km/h do not trigger gust display.
•
testShouldShowGusts_oneTenthAboveThresholdTrue
◦
Asserts that gusts 3.1 km/h above wind speed trigger gust display.
•
testShouldShowGusts_belowWindFalse
◦
Asserts that gusts lower than sustained wind speed return false.
•
testFormatWindGusts_nullAndSuppressedCases
◦
Asserts that suppressed or null gust inputs return null formatted strings.
•
testFormatWindGusts_presentCases
◦
Verifies wind gust string formatting in Metric (km/h) and US Customary (mph) unit systems.
3. Weather Alerts Tests (com.weatherapp.libre.alerts)
Alert Cache (AlertCacheTest)
•
testTabLocationCacheKeysAreDistinct
◦
Generates cache keys for different Canadian locations sharing default ID 0L and asserts distinct key strings based on name, region, and coordinates.
•
testCacheRoundTrip
◦
Serializes a CachedAlert object to JSON and back, asserting full field fidelity.
•
testExpiredPrunedOnRead
◦
Passes an expired alert through the cache read filter and asserts that expired entries are pruned.
•
testPerLocationKeying
◦
Asserts that cached alerts stored under one location key do not surface for a different location key.
•
testReplaceNotAppend
◦
Verifies that updating alert cache for a location replaces existing entries rather than appending.
•
testTestFixturePreservedOnWorkerWrite
◦
Verifies that active test fixture alerts are preserved when background worker updates real alert caches.
Alert Consent (AlertConsentTest)
•
testDefaultUnsetConsentStateShowsPrompt
◦
Asserts that ALERT_CONSENT_UNSET state triggers the alert consent UI prompt.
•
testOptInConsentStateHidesPromptAndEnablesAlerts
◦
Asserts that ALERT_CONSENT_OPT_IN hides the prompt and enables alert processing.
•
testOptOutConsentStateHidesPromptAndDisablesAlerts
◦
Asserts that ALERT_CONSENT_OPT_OUT hides the prompt and disables alert processing.
•
testLegacyKeysIgnoredByNewReaders
◦
Asserts that legacy SharedPreferences keys are distinct from new consent keys.
Alert Matching Engine (AlertMatcherTest)
•
testCircleRadiusMatchingTrue
◦
Tests CAP alert matching using a geographic circle radius and asserts positive match for Ottawa and negative match for Vancouver.
•
testPointInPolygonMatchingTrueAndFalse
◦
Tests CAP alert matching using ray-casting point-in-polygon geometry for a bounding box around Ottawa.
•
testPublicCanadaScopeMatching
◦
Tests matching for nationwide public alerts with no area restrictions.
•
testGeocodeMatching
◦
Tests CAP alert matching using Canadian Location Codes (CLC) geocode values.
CAP XML Parser (CapXmlParserTest)
•
testParseTornadoWarningEnglishLocale
◦
Parses a multi-language CAP XML alert for an English locale target and asserts extraction of English headline, severity, urgency, and circle geometry.
•
testParseTornadoWarningFrenchLocale
◦
Parses the same CAP XML alert for a French locale target (fr-CA) and asserts extraction of French headline and event description.
•
testParseUpdateAndExtractReferences
◦
Parses a CAP update message and extracts referenced original alert identifiers.
•
testParseInvalidXmlReturnsNull
◦
Passes malformed XML to CapXmlParser and verifies graceful null return.
Notification Intents (NotificationIntentTest)
•
testRequestCodeDerivation
◦
Asserts unique, deterministic notification request code generation derived from alert ID and location key.
•
testTabResolutionMatchingKeyAndFallback
◦
Tests notification tap intent location resolution matching tab keys and defaulting to tab 0 for unknown locations.
4. Lunar Astronomy Module Tests (com.weatherapp.libre.lunar)
Astronomical Time Utilities (AstroTimeTest)
•
testPre1970RoundTrip
◦
Tests Julian Date conversion round-trip for Apollo 11 landing (1969) and negative fractional epoch seconds, asserting nanosecond precision within 200 µs Double precision limit.
•
testEpochBoundary
◦
Tests round-trip conversion for Unix Epoch (1970-01-01T00:00:00Z).
•
testModernRoundTrip
◦
Tests round-trip conversion for a modern date (2026).
•
testTtUtcInversionConsistency
◦
Verifies 3-step iterative inversion consistency between jdUtcToJdeTT and jdeTTToJdUtc within 10⁻⁹ JD (~86 µs).
•
testJulianCenturiesTT
◦
Asserts exact values for julianCenturiesTT at epoch J2000.0 (0.0) and J2000.0 + 1 century (1.0).
Astronomical Mathematics (AstronomyMathTest)
•
testNormalizeDegrees
◦
Asserts degree normalization into [0, 360) for negative angles, large positive angles, exact multiples, and idempotence.
•
testTrigPrecisionWithLargeAngle
◦
Compares trigonometric sine outputs of large un-normalized angles vs normalized angles to verify floating-point precision.
•
testGmstDegreesBoundsAndDayRate
◦
Sweeps GMST calculation across two centuries verifying bounds in [0, 360) and daily earth rotation rate (~0.985647°/day).
Atmospheric Refraction (AtmosphericRefractionTest)
•
testEarlyReturnForAltitudesBelowCutoff
◦
Asserts that calculateRefractionDegrees returns exactly 0.0° for true altitudes below -1.0^∘ to prevent Bennett formula asymptotic divergence.
•
testHorizonSanity
◦
Verifies horizon refraction at 0.0^∘ true altitude is within physical bounds [0.40^∘, 0.55^∘].
•
testNearCutoffSanity
◦
Verifies refraction at -0.9^∘ true altitude is within [0.55^∘, 0.75^∘].
•
testMonotonicDecreaseAndZenithZero
◦
Asserts monotonic decrease of refraction from horizon to zenith (90^∘), where refraction is exactly 0.0^∘.
•
testPressureScaling
◦
Verifies that higher atmospheric pressure yields higher refraction.
•
testTemperatureScaling
◦
Verifies that lower temperature (denser air) yields higher refraction.
•
testUpperBound
◦
Sweeps altitudes from -1.0^∘ to 90.0^∘ verifying refraction never exceeds 1.0^∘.
Delta T (Δ T) Model (DeltaTTest)
•
testContinuityAtBranchBoundaries
◦
Evaluates NASA GSFC Espenak/Meeus Δ T piecewise polynomials across 14 century branch boundaries, asserting continuity within 0.5 seconds.
•
testKnownValues
◦
Validates Δ T estimates against historical values for years 1900, 2000, 2020, 2050, and 2200.
Lunar Calculator & Data Types (LunarCalculatorTest)
•
testCyclePercentageSemantics
◦
Constructs MoonPhaseInfo at First Quarter (90^∘) and Last Quarter (270^∘) and asserts cyclePercentage values of 25% and 75% respectively.
•
testIlluminationRounding
◦
Tests illumination percentage formatting, verifying 0.996 rounds to 100% and 0.994 rounds to 99%.
•
testApiCleanupPhaseAtNoObserver
◦
Verifies LunarCalculator.phaseAt(instant) call without an observer parameter.
•
testPhaseCategorySemantics
◦
Tests 8-state LunarPhaseCategory enum mapping across all 45° phase angle boundaries.
•
testEventTypeSemantics
◦
Verifies that major moon phase events return strictly one of the 4 primary quarter types (NEW_MOON, FIRST_QUARTER, FULL_MOON, LAST_QUARTER).
•
testTypeSafetyGeocentricDistanceKm
◦
Asserts that MoonPhaseInfo.geocentricDistanceKm returns a non-null Double greater than 300,000 km.
Geocentric Lunar Position (LunarPositionTest)
•
testRaDecExposure
◦
Computes geocentric lunar state and asserts valid bounds for rightAscensionDegrees [0, 360) and declinationDegrees [-90, 90].
•
testSemidiameterSanity
◦
Verifies exact geometric lunar semidiameter calculation (∼ 0.259^∘).
•
testNormalizationSanityFutureDate
◦
Computes lunar position for year 2500 and asserts distance in [350000, 420000] km and normalized longitude.
Moon Phase Calculations (MoonPhaseCalculatorTest)
•
testQuarterPhaseWCorrection
◦
Computes Meeus Chapter 49 quarter phase dates including "W" planetary perturbation corrections and asserts accuracy against J2000 table values.
•
testPrecedingNewMoonRobustness
◦
Evaluates candidate search for preceding new moon 1 second after vs 1 second before a new moon, asserting correct age calculation.
Moon Rise & Set Calculations (MoonRiseSetCalculatorTest)
•
testPre1970Robustness
◦
Calls findRiseSetEventsInInterval for July 1969 and asserts non-empty events with non-negative nanoseconds.
•
testPerformanceSanity
◦
Benchmarks moonEventsForLocalDate execution time, asserting completion in under 500 ms.
•
testRefractionSanity
◦
Verifies that apparent altitude at moonrise equals negative semidiameter within 0.01^∘.
Observer Model (ObserverTest)
•
testValidConstruction
◦
Asserts successful creation of an Observer with valid latitude and longitude.
•
testInvalidLatitude
◦
Asserts IllegalArgumentException when latitude is 91.0^∘.
•
testInvalidLongitude
◦
Asserts IllegalArgumentException when longitude is 181.0^∘.
•
testBoundaryValues
◦
Asserts successful creation at coordinate boundaries (± 90^∘ lat, ± 180^∘ lon).
•
testHorizonDipExplicit
◦
Verifies that explicit horizonDipDegrees takes precedence over elevation-calculated dip.
•
testHorizonDipCalculated
◦
Verifies geometric horizon dip calculation for 100m elevation (∼ 0.293^∘).
•
testBelowSeaLevel
◦
Asserts horizon dip is 0.0^∘ for observer elevations at or below sea level.
Lunar Solar Position (SolarPositionTest in lunar)
•
testNormalizationSanityFutureDate
◦
Computes solar position for year 2500 and asserts longitude in [0, 360), distance in [0.98, 1.02] AU, and latitude 0.0^∘.
•
testTimescaleMigrationCurrentEpoch
◦
Verifies solar position calculation for current epoch using julianCenturiesTT.
Topocentric Converter (TopocentricConverterTest)
•
testHorizonDipIntegration
◦
Verifies that topocentricHorizontalState incorporates observer horizon dip into horizonThresholdDegrees.
•
testAtan2StabilityNorthPole
◦
Tests topocentric coordinate conversion at North Pole (90^∘N) asserting non-NaN declination and right ascension via atan2.
5. Solar Astronomy Module Tests (com.weatherapp.libre.solar)
Solar Evening Regression & Symmetry (SolarEveningRegressionTest)
•
testParisOracle
◦
Computes solar lighting for Paris on Oct 3, 2026, validating sunrise hour (07:00–08:30 local), sunset hour (19:00 local), and daylight duration.
•
testFullYearSweepMirrorResidualsAndChainContinuity
◦
Executes a full 365-day year sweep for Saskatoon, Tromsø, and Wellington, testing mirror symmetry between morning/evening twilights and verifying core twilight chain continuity.
Solar Interval Containment (SolarIntervalContainmentTest)
•
testSaskatoonOverlapHighlights
◦
Tests overlapping active phase states (e.g. Civil Twilight + Golden Hour during morning overlap) and validates 180-second active event window boundaries.
Solar Chronological Lighting Engine (SolarLightingEngineTest in solar)
•
testDynamicTimelineMidLatitude
◦
Calls calculateChronologicalSolarPhases for a mid-latitude equinox, asserting exactly 15 phases spanning midnight to midnight (00:00 to 24:00).
•
testPolarEdgeCaseNoAstroTwilight
◦
Calls calculateChronologicalSolarPhases for Iceland in summer solstice (no astronomical twilight) and asserts no "——" placeholder strings and full 24-hour day coverage.
•
test2359BugFix
◦
Asserts that the final phase of the day does NOT contain the hardcoded literal string "23:59" in its time range text.
Solar Observer (SolarObserverTest)
•
testValidConstruction
◦
Asserts successful creation of a solar Observer.
•
testInvalidLatitude
◦
Asserts IllegalArgumentException when latitude is 91.0^∘.
•
testInvalidLongitude
◦
Asserts IllegalArgumentException when longitude is 181.0^∘.
•
testHorizonDipCalculation
◦
Verifies exact geometric horizon dip (arccos(R / (R + h))) for 1000m elevation (∼ 1.015^∘).
•
testBelowSeaLevel
◦
Asserts horizon dip is 0.0^∘ for negative elevations.
Solar Phase Metadata & Naming Symmetry (SolarPhaseMetadataTest)
•
testPhaseMetadata
◦
Solves solar lighting for equinox and asserts exact lower and upper altitude bounds on all morning and evening phase objects against LightingDefinitions.DEFAULT.
•
testNamingSymmetry
◦
Asserts that morningCivilTwilight, morningNauticalTwilight, and morningAstronomicalTwilight start strictly before their corresponding evening dusk counterparts.
Topocentric Solar Engine (SolarPositionEngineTest)
•
testRefractionEarlyReturn
◦
Asserts that calculateRefraction returns 0.0° when solar true altitude is below -1.0^∘.
•
testRefractionHorizonSanity
◦
Verifies Bennett refraction near 0.0^∘ true altitude is approximately 0.48^∘.
•
testAtan2StabilityNorthPole
◦
Evaluates computeTopocentric at North Pole (90^∘N) asserting non-NaN output using atan2.
Solar Rise & Set Solver (SolarRiseSetSolverTest)
•
testApparentAltitudeShiftMidLatitude
◦
Calculates sunrise for mid-latitude and verifies sunrise occurs between 04:00 and 08:00 local time using apparent altitude crossings.
•
testHorizonDipEffect
◦
Compares 0m vs 1000m elevation observers and verifies that 1000m sunrise occurs strictly earlier due to horizon dip.
•
testNegativeEpochRobustness
◦
Calls solve() for July 1969 and asserts valid solar lighting results without negative nanoseconds.
•
testArcticEdgeCaseMidnightSun
◦
Calls solve() for Tromsø during summer solstice, verifying graceful handling of continuous daylight without crashing.
Generated by gemini-3.6-flash
