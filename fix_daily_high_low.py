import re

def main():
    # 1. Read section_today_summary.xml to get cardTempRange block
    with open('app/src/main/res/layout/section_today_summary.xml', 'r') as f:
        summary_lines = f.readlines()
        
    start_idx = -1
    end_idx = -1
    for i, line in enumerate(summary_lines):
        if 'android:id="@+id/cardTempRange"' in line:
            start_idx = i - 1 # <LinearLayout
        elif '<!-- Low Temp Card -->' in line:
            pass # just marking progress
            
    # we know the file ends with </merge>, so we can just trace back from the end to find the closing LinearLayout
    for i in range(len(summary_lines)-1, -1, -1):
        if '</merge>' in summary_lines[i]:
            continue
        if '</LinearLayout>' in summary_lines[i]:
            end_idx = i
            break
            
    card_temp_range_block = summary_lines[start_idx:end_idx+1]
    
    # 2. Insert into activity_main.xml
    with open('app/src/main/res/layout/activity_main.xml', 'r') as f:
        main_lines = f.readlines()
        
    insert_idx = -1
    for i, line in enumerate(main_lines):
        if '<include layout="@layout/section_inline_subtabs"' in line:
            # We want to insert above the comment right before this
            insert_idx = i - 1
            if '<!-- Container 1: Default Standard Weather Details -->' in main_lines[insert_idx]:
                break
                
    if insert_idx != -1:
        new_main_lines = main_lines[:insert_idx] + card_temp_range_block + ["\n"] + main_lines[insert_idx:]
        with open('app/src/main/res/layout/activity_main.xml', 'w') as f:
            f.writelines(new_main_lines)
        print("Successfully inserted cardTempRange.")
    else:
        print("Could not find insertion point.")

if __name__ == "__main__":
    main()
