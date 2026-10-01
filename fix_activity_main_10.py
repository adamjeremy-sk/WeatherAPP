import re

def main():
    with open('app/src/main/res/layout/activity_main.xml', 'r') as f:
        main_lines = f.readlines()
        
    start_idx = -1
    end_idx = -1
    for i, line in enumerate(main_lines):
        if '<!-- Top App Bar / Location Header Card -->' in line:
            start_idx = i
        elif start_idx != -1 and '<!-- Sticky Location Tabs Container' in line:
            end_idx = i - 1
            break
            
    if start_idx != -1 and end_idx != -1:
        include_line = '            <!-- Top App Bar / Location Header Card -->\n            <include layout="@layout/section_header" />\n\n'
        new_main_lines = main_lines[:start_idx] + [include_line] + main_lines[end_idx:]
        with open('app/src/main/res/layout/activity_main.xml', 'w') as f:
            f.writelines(new_main_lines)
        print("Successfully replaced header.")
    else:
        print(f"Could not find header bounds. start={start_idx}, end={end_idx}")

if __name__ == "__main__":
    main()
