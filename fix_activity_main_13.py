import re

def main():
    with open('app/src/main/res/layout/activity_main.xml', 'r') as f:
        main_lines = f.readlines()
        
    start_idx = -1
    end_idx = -1
    for i, line in enumerate(main_lines):
        if '<!-- Scrollable Location Tabs Container (Scrolls away when Sticky Tabs = False) -->' in line:
            start_idx = i
        elif start_idx != -1 and '<FrameLayout' in line:
            pass
        elif start_idx != -1 and 'android:paddingTop="8dp">' in line:
            end_idx = i
            break
            
    if start_idx != -1 and end_idx != -1:
        new_main_lines = main_lines[:start_idx] + main_lines[end_idx+1:]
        with open('app/src/main/res/layout/activity_main.xml', 'w') as f:
            f.writelines(new_main_lines)
        print("Successfully removed broken frame layout start.")
    else:
        print(f"Could not find broken frame layout bounds. start={start_idx}, end={end_idx}")

if __name__ == "__main__":
    main()
