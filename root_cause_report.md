# Lunar Engine Root-Cause Investigation & Fix Report

## 1. Topocentric Parallax
- **Expected**: Rise/set solving must use true topocentric equatorial coordinates, adjusted for horizontal parallax before projection to altitude.
- **Observed**: The engine was using a shortcut `h0` threshold (`0.7275 * HP - 0.5667`) applied to geocentric coordinates. This incorrectly approximated parallax and refraction simultaneously, failing to account for true topocentric deformation. 
- **Fixed**: Removed the shortcut threshold. Implemented Meeus Chapter 40 rigorous topocentric transformation in `TopocentricConverter.kt` to adjust Right Ascension and Declination for equatorial horizontal parallax before calculating true altitude.

## 2. Threshold Convention
- **Expected**: The root finder must solve for `apparent topocentric altitude = -SD` (upper limb touching horizon).
- **Observed**: The engine used a fabricated threshold blending standard refraction, parallax, and semidiameter. Solving for this against refracted altitude double-counted refraction and parallax.
- **Fixed**: Updated `MoonRiseSetCalculator.kt` to target exactly `-SD` for the Moon.

## 3. Horizon Dip Leakage
- **Expected**: Standard sea-level horizon must be used for lunar and solar events to match almanacs (no observer elevation dip).
- **Observed**: `dipDeg` was being subtracted from the target altitude for the Moon and the Sun, leaking observer elevation into standard astronomical timings.
- **Fixed**: Removed `horizonDipDegrees` from the target altitude for both the Moon and the Sun (`SolarRiseSetSolver.kt`).

## 4. Semidiameter
- **Expected**: Instantaneous semidiameter must be `0.27245 * HP` in degrees.
- **Observed**: Computed as `asin(0.272481 * sinParallax)`, which is a slightly different convention.
- **Fixed**: Standardized in `LunarPosition.kt` to strictly use `0.27245 * HP`.

## 5. Root-Finder Precision
- **Expected**: Bracket steps no coarser than 2 minutes, refining to < 0.5 seconds.
- **Observed**: Coarse initial stepping of 10 minutes (`600s`) caused the solver to occasionally skip tight brackets or converge slowly.
- **Fixed**: Reduced search step to 120 seconds. The Brent solver already enforces a 0.5-second tolerance loop.

All changes are fully tested with rigorous unit tests for acceptance tolerances, residuals across 30 days/3 cities, and azimuth mirror symmetry.
