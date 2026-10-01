import re

def main():
    with open('app/src/main/res/layout/activity_main.xml', 'r') as f:
        main_lines = f.readlines()
        
    start_idx = -1
    end_idx = -1
    for i, line in enumerate(main_lines):
        if '<!-- Main Selected Day Weather Card -->' in line:
            start_idx = i
        elif start_idx != -1 and '<!-- 7-Day Forecast Title -->' in line:
            end_idx = i - 1
            break
            
    if start_idx != -1 and end_idx != -1:
        include_line = '                        <!-- Main Selected Day Weather Card -->\n                        <include layout="@layout/section_today_details" />\n\n'
        card_content = main_lines[start_idx+1:end_idx]
        with open('app/src/main/res/layout/section_today_details.xml', 'w') as f:
            f.write('<?xml version="1.0" encoding="utf-8"?>\n')
            f.writelines(card_content)
        new_main_lines = main_lines[:start_idx] + [include_line] + main_lines[end_idx:]
        with open('app/src/main/res/layout/activity_main.xml', 'w') as f:
            f.writelines(new_main_lines)
        print("Successfully extracted cardTodayDetails.")
    else:
        print(f"Could not find cardTodayDetails bounds. start={start_idx}, end={end_idx}")

if __name__ == "__main__":
    main()
