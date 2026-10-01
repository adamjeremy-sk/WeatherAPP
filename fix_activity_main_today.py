import re

def main():
    with open('app/src/main/res/layout/activity_main.xml', 'r') as f:
        main_lines = f.readlines()
        
    for i, line in enumerate(main_lines):
        if 'layout="@layout/section_today_details"' in line:
            main_lines[i] = '                        <!-- Main Selected Day Weather Card -->\n                        <com.google.android.material.card.MaterialCardView\n                            android:id="@+id/cardTodayDetails"\n                            android:layout_width="match_parent"\n                            android:layout_height="wrap_content"\n                            android:layout_marginHorizontal="16dp"\n                            android:layout_marginTop="12dp"\n                            app:cardBackgroundColor="@color/surface_card"\n                            app:cardCornerRadius="20dp"\n                            app:cardElevation="3dp"\n                            app:strokeColor="@color/divider_color"\n                            app:strokeWidth="1dp">\n\n                            <LinearLayout\n                                android:layout_width="match_parent"\n                                android:layout_height="wrap_content"\n                                android:orientation="vertical"\n                                android:padding="20dp">\n\n                                <include layout="@layout/section_today_details" />\n\n                            </LinearLayout>\n\n                        </com.google.android.material.card.MaterialCardView>\n'
            
    with open('app/src/main/res/layout/activity_main.xml', 'w') as f:
        f.writelines(main_lines)
    print("Fixed today details in activity_main.xml.")

if __name__ == "__main__":
    main()
