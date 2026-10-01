import re

def main():
    with open('app/src/main/res/layout/section_today_details.xml', 'r') as f:
        main_lines = f.readlines()
        
    main_lines[1] = '<com.google.android.material.card.MaterialCardView xmlns:android="http://schemas.android.com/apk/res/android" xmlns:app="http://schemas.android.com/apk/res-auto" xmlns:tools="http://schemas.android.com/tools"\n'
    
    with open('app/src/main/res/layout/section_today_details.xml', 'w') as f:
        f.writelines(main_lines)
    print("Fixed.")

if __name__ == "__main__":
    main()
