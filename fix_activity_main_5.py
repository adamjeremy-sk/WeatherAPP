import re

def main():
    with open('app/src/main/res/layout/activity_main.xml', 'r') as f:
        main_lines = f.readlines()
        
    start_idx = -1
    end_idx = -1
    for i, line in enumerate(main_lines):
        if '<!-- Snowfall Easter Egg Layer -->' in line:
            start_idx = i
        elif start_idx != -1 and '<!-- Heat Mirage Shimmer Easter Egg Layer -->' in line:
            # Need to get the end of the heat shimmer layer
            pass
        elif start_idx != -1 and '<LinearLayout' in line:
            end_idx = i - 1
            break
            
    if start_idx != -1 and end_idx != -1:
        include_line = '        <include layout="@layout/section_overlays" />\n\n'
        card_content = main_lines[start_idx:end_idx]
        with open('app/src/main/res/layout/section_overlays.xml', 'w') as f:
            f.write('<?xml version="1.0" encoding="utf-8"?>\n<merge xmlns:android="http://schemas.android.com/apk/res/android" xmlns:app="http://schemas.android.com/apk/res-auto" xmlns:tools="http://schemas.android.com/tools">\n')
            f.writelines(card_content)
            f.write('</merge>\n')
        new_main_lines = main_lines[:start_idx] + [include_line] + main_lines[end_idx:]
        with open('app/src/main/res/layout/activity_main.xml', 'w') as f:
            f.writelines(new_main_lines)
        print("Successfully extracted section_overlays.")
    else:
        print(f"Could not find section_overlays bounds. start={start_idx}, end={end_idx}")

if __name__ == "__main__":
    main()
