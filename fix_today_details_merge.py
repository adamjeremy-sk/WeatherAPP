import re

def main():
    with open('app/src/main/res/layout/section_today_details.xml', 'r') as f:
        main_lines = f.readlines()
        
    main_lines[0] = '<?xml version="1.0" encoding="utf-8"?>\n<merge xmlns:android="http://schemas.android.com/apk/res/android" xmlns:app="http://schemas.android.com/apk/res-auto" xmlns:tools="http://schemas.android.com/tools">\n'
    main_lines[1] = ''
    main_lines[2] = ''
    main_lines[3] = ''
    main_lines[4] = ''
    main_lines[5] = ''
    main_lines[6] = ''
    main_lines[7] = ''
    main_lines[8] = ''
    main_lines[9] = ''
    main_lines[-1] = '</merge>\n'
    main_lines[-2] = ''
    
    with open('app/src/main/res/layout/section_today_details.xml', 'w') as f:
        f.writelines(main_lines)
    print("Fixed section_today_details.xml to merge.")

if __name__ == "__main__":
    main()
