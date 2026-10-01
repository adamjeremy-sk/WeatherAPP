import os

def wrap_with_merge(filepath):
    with open(filepath, 'r') as f:
        content = f.read()
    
    if '<merge' in content:
        return # Already wrapped
        
    lines = content.split('\n')
    xml_decl = lines[0]
    rest = '\n'.join(lines[1:])
    
    new_content = xml_decl + '\n<merge xmlns:android="http://schemas.android.com/apk/res/android" xmlns:app="http://schemas.android.com/apk/res-auto" xmlns:tools="http://schemas.android.com/tools">\n' + rest + '\n</merge>\n'
    
    with open(filepath, 'w') as f:
        f.write(new_content)

if __name__ == "__main__":
    files = [
        'app/src/main/res/layout/section_header.xml',
        'app/src/main/res/layout/section_tabs_area.xml',
        'app/src/main/res/layout/section_today_details.xml',
        'app/src/main/res/layout/section_alert_banner.xml'
    ]
    for f in files:
        wrap_with_merge(f)
        print(f"Wrapped {f} with merge.")
