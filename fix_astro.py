with open('app/src/main/java/com/weatherapp/libre/AstronomyDisplayManager.kt', 'r') as f:
    content = f.read()

content = content.replace('binding.layoutWeatherDetailsContainer', 'binding.root.findViewById<android.view.View>(R.id.layoutWeatherDetailsContainer)?')
content = content.replace('binding.layoutInlineGoldenHourContainer', 'binding.root.findViewById<android.view.View>(R.id.layoutInlineGoldenHourContainer)?')
content = content.replace('binding.tvInlineSelectedDate', 'binding.root.findViewById<android.widget.TextView>(R.id.tvInlineSelectedDate)?')
content = content.replace('binding.btnInlineMenu.text', 'binding.root.findViewById<com.google.android.material.button.MaterialButton>(R.id.btnInlineMenu)?.text')
content = content.replace('binding.btnInlineMenu.setIconResource', 'binding.root.findViewById<com.google.android.material.button.MaterialButton>(R.id.btnInlineMenu)?.setIconResource')

with open('app/src/main/java/com/weatherapp/libre/AstronomyDisplayManager.kt', 'w') as f:
    f.write(content)
