# WeatherAPP Libre

Privacy-first, zero-GPS weather and astronomical intelligence for Android.

WeatherAPP Libre is a multi-model weather, natural-light, and sky-almanac
planner for photographers, anglers, hunters, and stargazers. Every city is
selected manually; the app requests no location permission and ships no
advertising, analytics, or tracking of any kind.

## Features

- Selectable forecast models (ECCC GEM, NOAA GFS, ECMWF, NOAA GEFS, CMA-GFS)
- 36-hour hourly and 7-day forecasts with full solar and lunar detail
- Golden Hour and Blue Hour windows; 13-phase Natural Light view
- True Dark Sky, Night Visibility, and Solunar Activity pages
- Official Environment Canada CAP-CP severe weather alerts (opt-in)
- Eleven themes, including nine boutique palettes
- Community extensions: themes and forecast providers via the catalog at
  weatherapplibre.ca

## Licence

Copyright (C) 2026 Adam Hainstock.
This program is free software released under the GNU General Public
License version 3; see [LICENSE](LICENSE).
The WeatherAPP Libre name and logo are reserved trademarks; see
[NOTICE](NOTICE).

## Building

Requires Android Studio with the JDK and Android SDK levels declared in
`app/build.gradle.kts`.

    ./gradlew assembleDebug

## Data Attribution

Weather data via Open-Meteo (CC BY 4.0). Severe weather alerts via
Environment and Climate Change Canada (Open Government Licence - Canada).
Full attribution appears in the in-app Licenses & Sources screen.

## Extensions

Theme and forecast-provider extension manifests are documented in
`docs/extensions.md` (schema v1). Extensions are declarative data files;
the catalog at weatherapplibre.ca is operated under its own terms of
service.

## Contributing

Issues and pull requests are welcome. By submitting a pull request you
agree that your contribution is licensed under GPLv3.
