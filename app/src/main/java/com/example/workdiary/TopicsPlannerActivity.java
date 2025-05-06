package com.example.workdiary;

import android.os.Bundle;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.google.android.material.tabs.TabLayout;
import com.google.android.material.tabs.TabLayoutMediator;
import androidx.viewpager2.widget.ViewPager2;
import com.parse.ParseQuery;
import com.parse.ParseObject;
import com.parse.ParseUser;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class TopicsPlannerActivity extends AppCompatActivity {

    private TabLayout tabLayout;
    private ViewPager2 viewPager;
    private TopicPlannerAdapter adapter;
    private List<String> subjectsList;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_topics_planner); // Make sure this is your correct layout

        // Initialize UI elements
        tabLayout = findViewById(R.id.tabLayout);
        viewPager = findViewById(R.id.viewPager);

        // Fetch user's timetable from Back4App
        fetchTimetableData();
    }

    private void fetchTimetableData() {
        ParseQuery<ParseObject> query = ParseQuery.getQuery("Timetable");
        query.whereEqualTo("userId", ParseUser.getCurrentUser().getObjectId());
        query.getFirstInBackground((timetableObject, e) -> {
            if (e == null && timetableObject != null) {
                subjectsList = extractSubjectsFromCells(timetableObject);
                if (!subjectsList.isEmpty()) {
                    setupViewPager();
                } else {
                    Toast.makeText(this, "No subjects found in timetable.", Toast.LENGTH_SHORT).show();
                }
            } else {
                Toast.makeText(this, "Error fetching timetable: " + (e != null ? e.getMessage() : "No data"), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private List<String> extractSubjectsFromCells(ParseObject timetableObject) {
        Set<String> subjectSet = new HashSet<>();

        for (String key : timetableObject.keySet()) {
            if (key.startsWith("cell_")) {
                String subject = timetableObject.getString(key);
                if (subject != null && !subject.trim().isEmpty()) {
                    subjectSet.add(subject.trim());
                }
            }
        }

        return new ArrayList<>(subjectSet);
    }

    private void setupViewPager() {
        adapter = new TopicPlannerAdapter(this, subjectsList);
        viewPager.setAdapter(adapter);

        new TabLayoutMediator(tabLayout, viewPager, (tab, position) -> {
            tab.setText(subjectsList.get(position));
        }).attach();
    }
}
