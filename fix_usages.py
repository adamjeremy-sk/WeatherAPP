import re

def main():
    with open('app/src/main/java/com/weatherapp/libre/SettingsDialogManager.kt', 'r') as f:
        main_lines = f.readlines()
        
    for i, line in enumerate(main_lines):
        if 'activity.weatherDisplayManager.updateAlertBanner()' in line:
            main_lines[i] = '                        activity.alertBannerManager.updateAlertBanner(activity.weatherDisplayManager.currentLocation)\n'
            
    with open('app/src/main/java/com/weatherapp/libre/SettingsDialogManager.kt', 'w') as f:
        f.writelines(main_lines)
        
    with open('app/src/main/java/com/weatherapp/libre/WeatherDisplayManager.kt', 'r') as f:
        main_lines = f.readlines()
        
    for i, line in enumerate(main_lines):
        if 'updateAlertBanner()' in line:
            main_lines[i] = ''
            
    with open('app/src/main/java/com/weatherapp/libre/WeatherDisplayManager.kt', 'w') as f:
        f.writelines(main_lines)
        
    print("Fixed Usages.")

if __name__ == "__main__":
    main()
