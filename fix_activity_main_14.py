import re

def main():
    with open('app/src/main/java/com/weatherapp/libre/MainActivity.kt', 'r') as f:
        main_lines = f.readlines()
        
    for i, line in enumerate(main_lines):
        if 'weatherDisplayManager.updateAlertBanner()' in line:
            main_lines[i] = '        alertBannerManager.updateAlertBanner(weatherDisplayManager.currentLocation)\n'
            
    with open('app/src/main/java/com/weatherapp/libre/MainActivity.kt', 'w') as f:
        f.writelines(main_lines)
    print("Fixed MainActivity.")

if __name__ == "__main__":
    main()
