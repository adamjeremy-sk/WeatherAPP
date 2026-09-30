with open('app/src/main/java/com/weatherapp/libre/AstronomyDisplayManager.kt', 'r') as f:
    content = f.read()
content = content.replace('binding.root.findViewById<android.view.View>(R.id.layoutWeatherDetailsContainer)?.visibility', 'binding.root.findViewById<android.view.View>(R.id.layoutWeatherDetailsContainer)?.visibility')
# Actually the error says unresolved reference layoutWeatherDetailsContainer
content = content.replace('binding.layoutWeatherDetailsContainer', 'binding.root.findViewById<android.view.View>(R.id.layoutWeatherDetailsContainer)?')
content = content.replace('binding.layoutInlineGoldenHourContainer', 'binding.root.findViewById<android.view.View>(R.id.layoutInlineGoldenHourContainer)?')
content = content.replace('binding.tvInlineSelectedDate', 'binding.root.findViewById<android.widget.TextView>(R.id.tvInlineSelectedDate)?')

with open('app/src/main/java/com/weatherapp/libre/AstronomyDisplayManager.kt', 'w') as f:
    f.write(content)

