with open('app/src/main/java/com/weatherapp/libre/ThemeManager.kt', 'r') as f:
    content = f.read()

# Replace R.id.xxx with com.weatherapp.libre.R.id.xxx
import re
content = re.sub(r'R\.id\.(layoutMetricGoldenBlueHourContainer|tvMorningGolden|tvEveningGolden|tvMorningBlue|tvEveningBlue)', r'com.weatherapp.libre.R.id.\1', content)

with open('app/src/main/java/com/weatherapp/libre/ThemeManager.kt', 'w') as f:
    f.write(content)

with open('app/src/main/java/com/weatherapp/libre/WeatherDisplayManager.kt', 'r') as f:
    content = f.read()

content = re.sub(r'R\.id\.(layoutMetricGoldenBlueHourContainer|tvMorningGolden|tvEveningGolden|tvMorningBlue|tvEveningBlue)', r'com.weatherapp.libre.R.id.\1', content)

with open('app/src/main/java/com/weatherapp/libre/WeatherDisplayManager.kt', 'w') as f:
    f.write(content)
