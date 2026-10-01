import re

def main():
    with open('app/src/main/res/layout/activity_main.xml', 'r') as f:
        main_lines = f.readlines()
        
    start_idx = -1
    end_idx = -1
    for i, line in enumerate(main_lines):
        if '<!-- Location Tabs Card -->' in line:
            start_idx = i - 1
        elif start_idx != -1 and '<!-- Main Selected Day Weather Card -->' in line:
            end_idx = i - 3
            break
            
    if start_idx != -1 and end_idx != -1:
        include_line = '                        <!-- Location Tabs Card -->\n                        <include layout="@layout/section_tabs_area" />\n\n'
        card_content = main_lines[start_idx+2:end_idx]
        with open('app/src/main/res/layout/section_tabs_area.xml', 'w') as f:
            f.write('<?xml version="1.0" encoding="utf-8"?>\n')
            f.writelines(card_content)
        new_main_lines = main_lines[:start_idx] + [include_line] + main_lines[end_idx:]
        with open('app/src/main/res/layout/activity_main.xml', 'w') as f:
            f.writelines(new_main_lines)
        print("Successfully extracted tabs.")
    else:
        print(f"Could not find tabs bounds. start={start_idx}, end={end_idx}")

if __name__ == "__main__":
    main()
