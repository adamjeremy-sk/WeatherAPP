with open('app/src/main/java/com/weatherapp/libre/ThemeManager.kt', 'r') as f:
    content = f.read()

import re
content = content.replace('R.id.layoutMetricGoldenBlueHourContainer', 'com.weatherapp.libre.R.id.layoutMetricGoldenBlueHourContainer')
content = content.replace('R.id.tvMorningGolden', 'com.weatherapp.libre.R.id.tvMorningGolden')
content = content.replace('R.id.tvEveningGolden', 'com.weatherapp.libre.R.id.tvEveningGolden')
content = content.replace('R.id.tvMorningBlue', 'com.weatherapp.libre.R.id.tvMorningBlue')
content = content.replace('R.id.tvEveningBlue', 'com.weatherapp.libre.R.id.tvEveningBlue')

with open('app/src/main/java/com/weatherapp/libre/ThemeManager.kt', 'w') as f:
    f.write(content)

with open('app/src/main/java/com/weatherapp/libre/WeatherDisplayManager.kt', 'r') as f:
    content = f.read()

content = content.replace('R.id.layoutMetricGoldenBlueHourContainer', 'com.weatherapp.libre.R.id.layoutMetricGoldenBlueHourContainer')
content = content.replace('R.id.tvMorningGolden', 'com.weatherapp.libre.R.id.tvMorningGolden')
content = content.replace('R.id.tvEveningGolden', 'com.weatherapp.libre.R.id.tvEveningGolden')
content = content.replace('R.id.tvMorningBlue', 'com.weatherapp.libre.R.id.tvMorningBlue')
content = content.replace('R.id.tvEveningBlue', 'com.weatherapp.libre.R.id.tvEveningBlue')

with open('app/src/main/java/com/weatherapp/libre/WeatherDisplayManager.kt', 'w') as f:
    f.write(content)
