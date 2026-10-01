import re

def main():
    with open('app/src/main/res/layout/activity_main.xml', 'r') as f:
        main_lines = f.readlines()
        
    start_idx = -1
    end_idx = -1
    for i, line in enumerate(main_lines):
        if '<!-- Scrollable Location Tabs Container' in line:
            start_idx = i
        elif start_idx != -1 and '<!-- Location Tabs Card -->' in line:
            pass
        elif start_idx != -1 and '<include layout="@layout/section_tabs_area"' in line:
            end_idx = i
            break
            
    if start_idx != -1 and end_idx != -1:
        # the frame layout was never closed? Wait, it shouldn't have been extracted like that.
        include_line = '                        <!-- Location Tabs Card -->\n                        <include layout="@layout/section_tabs_area" />\n'
        new_main_lines = main_lines[:start_idx] + [include_line] + main_lines[end_idx+1:]
        with open('app/src/main/res/layout/activity_main.xml', 'w') as f:
            f.writelines(new_main_lines)
        print("Successfully replaced tabs.")
    else:
        print(f"Could not find tabs bounds. start={start_idx}, end={end_idx}")

if __name__ == "__main__":
    main()
