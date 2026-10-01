import re

def main():
    with open('app/src/main/java/com/weatherapp/libre/MainActivity.kt', 'r') as f:
        main_lines = f.readlines()
        
    for i, line in enumerate(main_lines):
        if 'lateinit var weatherDisplayManager' in line:
            main_lines.insert(i, '    lateinit var alertBannerManager: com.weatherapp.libre.alerts.AlertBannerManager\n')
            break
            
    for i, line in enumerate(main_lines):
        if 'weatherDisplayManager = WeatherDisplayManager' in line:
            main_lines.insert(i+1, '        alertBannerManager = com.weatherapp.libre.alerts.AlertBannerManager(this, binding, locationPreferences)\n')
            break
            
    with open('app/src/main/java/com/weatherapp/libre/MainActivity.kt', 'w') as f:
        f.writelines(main_lines)
    print("Fixed MainActivity AlertBannerManager.")

if __name__ == "__main__":
    main()
