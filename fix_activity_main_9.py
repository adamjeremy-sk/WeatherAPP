import re

def main():
    with open('app/src/main/res/layout/activity_main.xml', 'r') as f:
        main_lines = f.readlines()
        
    start_idx = -1
    end_idx = -1
    for i, line in enumerate(main_lines):
        if '<!-- Active ECCC / CAP Weather Alert Banner Card -->' in line:
            start_idx = i
        elif start_idx != -1 and '<!-- Main Selected Day Weather Card -->' in line:
            end_idx = i - 1
            break
            
    if start_idx != -1 and end_idx != -1:
        include_line = '                        <include layout="@layout/section_alert_banner" />\n\n'
        new_main_lines = main_lines[:start_idx] + [include_line] + main_lines[end_idx:]
        with open('app/src/main/res/layout/activity_main.xml', 'w') as f:
            f.writelines(new_main_lines)
        print("Successfully replaced alert banner.")
    else:
        print(f"Could not find alert banner bounds. start={start_idx}, end={end_idx}")

if __name__ == "__main__":
    main()
