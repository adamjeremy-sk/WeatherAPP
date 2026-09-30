# The layoutMetricGoldenBlueHourContainer and its contents were dropped. We need to add them back into section_inline_subtabs.xml.

content = '''<?xml version="1.0" encoding="utf-8"?>
<merge xmlns:android="http://schemas.android.com/apk/res/android"
    xmlns:app="http://schemas.android.com/apk/res-auto"
    xmlns:tools="http://schemas.android.com/tools">

    <LinearLayout
        android:id="@+id/layoutWeatherDetailsContainer"
        android:layout_width="match_parent"
        android:layout_height="wrap_content"
        android:orientation="vertical">

        <include layout="@layout/section_metric_grid" />
        <include layout="@layout/section_sun_moon" />

        <!-- Golden & Blue Hour Container (Toggled via Theme & Display Settings) -->
        <LinearLayout
            android:id="@+id/layoutMetricGoldenBlueHourContainer"
            android:layout_width="match_parent"
            android:layout_height="wrap_content"
            android:orientation="vertical"
            android:visibility="gone">

            <!-- Golden Hour Preview Card -->
            <LinearLayout
                android:layout_width="match_parent"
                android:layout_height="wrap_content"
                android:layout_marginTop="12dp"
                android:background="@drawable/bg_golden_hour"
                android:orientation="vertical"
                android:padding="14dp">

                <TextView
                    android:layout_width="wrap_content"
                    android:layout_height="wrap_content"
                    android:text="Golden Hour"
                    android:textColor="@color/golden_hour_primary"
                    android:textSize="15sp"
                    android:textStyle="bold" />

                <LinearLayout
                    android:layout_width="match_parent"
                    android:layout_height="wrap_content"
                    android:layout_marginTop="8dp"
                    android:orientation="horizontal">

                    <LinearLayout
                        android:layout_width="0dp"
                        android:layout_height="wrap_content"
                        android:layout_weight="1"
                        android:orientation="vertical">

                        <TextView
                            android:layout_width="wrap_content"
                            android:layout_height="wrap_content"
                            android:text="Morning"
                            android:textColor="@color/golden_hour_primary"
                            android:textSize="12sp" />

                        <TextView
                            android:id="@+id/tvMorningGolden"
                            android:layout_width="wrap_content"
                            android:layout_height="wrap_content"
                            android:layout_marginTop="2dp"
                            android:textColor="@color/text_primary"
                            android:textSize="14sp"
                            android:textStyle="bold"
                            tools:text="6:43 AM – 7:48 AM" />

                    </LinearLayout>

                    <LinearLayout
                        android:layout_width="0dp"
                        android:layout_height="wrap_content"
                        android:layout_weight="1"
                        android:orientation="vertical">

                        <TextView
                            android:layout_width="wrap_content"
                            android:layout_height="wrap_content"
                            android:text="Evening"
                            android:textColor="@color/golden_hour_primary"
                            android:textSize="12sp" />

                        <TextView
                            android:id="@+id/tvEveningGolden"
                            android:layout_width="wrap_content"
                            android:layout_height="wrap_content"
                            android:layout_marginTop="2dp"
                            android:textColor="@color/text_primary"
                            android:textSize="14sp"
                            android:textStyle="bold"
                            tools:text="6:04 PM – 7:09 PM" />

                    </LinearLayout>

                </LinearLayout>

            </LinearLayout>

            <!-- Blue Hour Preview Card -->
            <LinearLayout
                android:layout_width="match_parent"
                android:layout_height="wrap_content"
                android:layout_marginTop="12dp"
                android:background="@drawable/bg_blue_hour"
                android:orientation="vertical"
                android:padding="14dp">

                <TextView
                    android:layout_width="wrap_content"
                    android:layout_height="wrap_content"
                    android:text="Blue Hour"
                    android:textColor="@color/blue_hour_primary"
                    android:textSize="15sp"
                    android:textStyle="bold" />

                <LinearLayout
                    android:layout_width="match_parent"
                    android:layout_height="wrap_content"
                    android:layout_marginTop="8dp"
                    android:orientation="horizontal">

                    <LinearLayout
                        android:layout_width="0dp"
                        android:layout_height="wrap_content"
                        android:layout_weight="1"
                        android:orientation="vertical">

                        <TextView
                            android:layout_width="wrap_content"
                            android:layout_height="wrap_content"
                            android:text="Morning"
                            android:textColor="@color/blue_hour_primary"
                            android:textSize="12sp" />

                        <TextView
                            android:id="@+id/tvMorningBlue"
                            android:layout_width="wrap_content"
                            android:layout_height="wrap_content"
                            android:layout_marginTop="2dp"
                            android:textColor="@color/text_primary"
                            android:textSize="14sp"
                            android:textStyle="bold"
                            tools:text="6:27 AM – 6:38 AM" />

                    </LinearLayout>

                    <LinearLayout
                        android:layout_width="0dp"
                        android:layout_height="wrap_content"
                        android:layout_weight="1"
                        android:orientation="vertical">

                        <TextView
                            android:layout_width="wrap_content"
                            android:layout_height="wrap_content"
                            android:text="Evening"
                            android:textColor="@color/blue_hour_primary"
                            android:textSize="12sp" />

                        <TextView
                            android:id="@+id/tvEveningBlue"
                            android:layout_width="wrap_content"
                            android:layout_height="wrap_content"
                            android:layout_marginTop="2dp"
                            android:textColor="@color/text_primary"
                            android:textSize="14sp"
                            android:textStyle="bold"
                            tools:text="7:07 PM – 7:18 PM" />

                    </LinearLayout>

                </LinearLayout>

            </LinearLayout>

        </LinearLayout>

    </LinearLayout>

    <!-- Container 2: Inline Golden Hour View -->
    <LinearLayout
        android:id="@+id/layoutInlineGoldenHourContainer"
        android:layout_width="match_parent"
        android:layout_height="wrap_content"
        android:orientation="vertical"
        android:visibility="gone">

        <LinearLayout
            android:layout_width="match_parent"
            android:layout_height="wrap_content"
            android:gravity="center_vertical"
            android:orientation="horizontal">

            <ImageButton
                android:id="@+id/btnInlinePrevDay"
                android:layout_width="36dp"
                android:layout_height="36dp"
                android:background="?attr/selectableItemBackgroundBorderless"
                android:contentDescription="Previous Day"
                android:padding="6dp"
                android:scaleType="fitCenter"
                android:src="@drawable/ic_arrow_left"
                app:tint="@color/text_primary" />

            <LinearLayout
                android:id="@+id/layoutInlineDateClickable"
                android:layout_width="0dp"
                android:layout_height="wrap_content"
                android:layout_weight="1"
                android:clickable="true"
                android:focusable="true"
                android:gravity="center"
                android:orientation="horizontal">

                <TextView
                    android:id="@+id/tvInlineSelectedDate"
                    android:layout_width="wrap_content"
                    android:layout_height="wrap_content"
                    android:textColor="@color/brand_primary"
                    android:textSize="14sp"
                    android:textStyle="bold"
                    tools:text="TODAY • THU, SEP 24, 2026" />

                <ImageView
                    android:layout_width="18dp"
                    android:layout_height="18dp"
                    android:layout_marginStart="6dp"
                    android:contentDescription="Calendar"
                    android:src="@drawable/ic_calendar"
                    app:tint="@color/brand_primary" />

            </LinearLayout>

            <ImageButton
                android:id="@+id/btnInlineNextDay"
                android:layout_width="36dp"
                android:layout_height="36dp"
                android:background="?attr/selectableItemBackgroundBorderless"
                android:contentDescription="Next Day"
                android:padding="6dp"
                android:scaleType="fitCenter"
                android:src="@drawable/ic_arrow_right"
                app:tint="@color/text_primary" />

        </LinearLayout>

        <androidx.recyclerview.widget.RecyclerView
            android:id="@+id/rvInlineSolarPhases"
            android:layout_width="match_parent"
            android:layout_height="wrap_content"
            android:layout_marginTop="12dp"
            android:nestedScrollingEnabled="false"
            tools:itemCount="15"
            tools:listitem="@layout/item_solar_phase" />

    </LinearLayout>

</merge>
'''

with open('app/src/main/res/layout/section_inline_subtabs.xml', 'w') as f:
    f.write(content)

