import os

def clean_namespaces(filepath):
    with open(filepath, 'r') as f:
        content = f.read()
        
    content = content.replace('<merge   >', '<merge xmlns:android="http://schemas.android.com/apk/res/android" xmlns:app="http://schemas.android.com/apk/res-auto" xmlns:tools="http://schemas.android.com/tools">')
    
    with open(filepath, 'w') as f:
        f.write(content)

if __name__ == "__main__":
    files = [
        'app/src/main/res/layout/section_header.xml',
        'app/src/main/res/layout/section_tabs_area.xml',
        'app/src/main/res/layout/section_today_details.xml',
        'app/src/main/res/layout/section_alert_banner.xml'
    ]
    for f in files:
        clean_namespaces(f)
        print(f"Cleaned {f}.")
