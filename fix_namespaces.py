import os

def clean_namespaces(filepath):
    with open(filepath, 'r') as f:
        content = f.read()
        
    # The `<merge>` adds namespaces. We should remove namespaces from the first child tag.
    content = content.replace('xmlns:android="http://schemas.android.com/apk/res/android"', '')
    content = content.replace('xmlns:app="http://schemas.android.com/apk/res-auto"', '')
    content = content.replace('xmlns:tools="http://schemas.android.com/tools"', '')
    
    # Put them back only on the `<merge>` tag
    content = content.replace('<merge \n>', '<merge xmlns:android="http://schemas.android.com/apk/res/android" xmlns:app="http://schemas.android.com/apk/res-auto" xmlns:tools="http://schemas.android.com/tools">')
    
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
