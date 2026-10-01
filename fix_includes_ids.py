import re

def main():
    with open('app/src/main/res/layout/activity_main.xml', 'r') as f:
        main_lines = f.readlines()
        
    for i, line in enumerate(main_lines):
        if 'layout="@layout/section_header"' in line:
            main_lines[i] = '            <include layout="@layout/section_header" />\n'
        elif 'layout="@layout/section_tabs_area"' in line:
            main_lines[i] = '                        <include layout="@layout/section_tabs_area" />\n'
        elif 'layout="@layout/section_alert_banner"' in line:
            main_lines[i] = '                        <include layout="@layout/section_alert_banner" />\n'
        elif 'layout="@layout/section_today_details"' in line:
            main_lines[i] = '                        <include layout="@layout/section_today_details" />\n'

    with open('app/src/main/res/layout/activity_main.xml', 'w') as f:
        f.writelines(main_lines)
    print("Fixed IDs in activity_main.xml.")

if __name__ == "__main__":
    main()
