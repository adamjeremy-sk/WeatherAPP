# WeatherAPP Libre Extension Schema (Draft v1)

Extensions are declarative JSON manifests hosted in the community catalog
at weatherapplibre.ca and installed from within the app. Two kinds exist
in v1: `theme` and `forecast-provider`. Compiled-code plugins are not
supported in v1.

## Common Fields (both kinds)

| Field         | Type   | Required | Purpose                                  |
|---------------|--------|----------|------------------------------------------|
| schemaVersion | int    | yes      | Must be 1 for this specification.        |
| kind          | string | yes      | `theme` or `forecast-provider`.          |
| id            | string | yes      | Reverse-DNS unique identifier.           |
| name          | string | yes      | User-visible display name.               |
| author        | string | yes      | Author or organization name.             |
| version       | string | yes      | Semantic version of the manifest.        |
| minAppVersion | int    | yes      | Minimum app versionCode required.        |
| license       | string | yes      | SPDX identifier; OSI-approved or CC-*.   |
| attribution   | object | no       | { text, url } shown in Licenses screen.  |

## kind: theme

The `palette` object must supply the complete token set of the host
version's ThemePalette class. The authoritative token list is exported by
the app (Info > Licenses > "Copy palette template") and must be matched
exactly; unknown tokens are rejected, missing tokens fail validation.
`dark` declares whether the theme participates in dark-mode behaviors
(icon contrast, snow/shimmer effects).

Example:

    {
      "schemaVersion": 1,
      "kind": "theme",
      "id": "ca.example.themes.midnight-fjord",
      "name": "Midnight Fjord",
      "author": "Example Author",
      "version": "1.0.0",
      "minAppVersion": 19,
      "license": "CC-BY-4.0",
      "dark": true,
      "palette": {
        "surfaceCard":  "#16203A",
        "textPrimary":  "#E8EEF9",
        "textSecondary":"#9FB0C7",
        "dividerColor": "#2A3854",
        "brandPrimary": "#6EA8FE"
      },
      "attribution": {
        "text": "Midnight Fjord by Example Author",
        "url": "https://example.com/midnight-fjord"
      }
    }

(The palette object above is abbreviated for readability; a valid
submission contains every token of the target ThemePalette version.)

## kind: forecast-provider

v1 providers must serve the Open-Meteo v1 JSON contract
(`apiFamily: "open-meteo-v1"`), including `current`, `daily`, and
`hourly` blocks with standard variable names. `dataAttribution` is
mandatory and is rendered by the app's dynamic attribution line.

    {
      "schemaVersion": 1,
      "kind": "forecast-provider",
      "id": "de.example.providers.icon-seamless",
      "name": "DWD ICON",
      "author": "Example Author",
      "version": "1.0.0",
      "minAppVersion": 19,
      "license": "MIT",
      "endpoint": {
        "apiFamily": "open-meteo-v1",
        "baseUrl": "https://api.open-meteo.com/v1/forecast",
        "modelsKey": "icon_seamless"
      },
      "dataAttribution": {
        "text": "Data: DWD via Open-Meteo",
        "url": "https://www.dwd.de",
        "license": "CC-BY-4.0"
      },
      "supportedVariables": [
        "temperature_2m", "precipitation", "weather_code",
        "wind_speed_10m", "cloud_cover", "visibility", "uv_index"
      ]
    }

## Catalog Submission Rules (summary; binding terms live at
## weatherapplibre.ca)

1. Manifest must validate against schema v1.
2. `dataAttribution` with a licence and URL is mandatory for providers.
3. Extensions must not declare permissions, tracking, analytics, or
   network endpoints beyond the declared `baseUrl`.
4. Extension licence must be OSI-approved or Creative Commons.
5. The catalog operator may decline or remove any submission.
6. Submissions grant the catalog operator a non-exclusive right to host
   and distribute the manifest.
