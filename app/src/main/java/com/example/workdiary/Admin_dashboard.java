package com.example.workdiary;

import android.os.Bundle;
import android.util.Log;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.parse.ParseObject;
import com.parse.ParseQuery;
import com.parse.ParseUser;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

public class Admin_dashboard extends AppCompatActivity {

    private TextView tvTotalFaculty, tvWorkdiarySubmitted, tvPendingSubmissions;
    private RecyclerView rvFacultyList;
    private FacultyAdapter adapter;
    private List<Faculty> facultyList = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin_dashboard);

        tvTotalFaculty = findViewById(R.id.tvTotalFaculty);
        tvWorkdiarySubmitted = findViewById(R.id.tvWorkdiarySubmitted);
        tvPendingSubmissions = findViewById(R.id.tvPendingSubmissions);
        rvFacultyList = findViewById(R.id.rvFacultyList);
        rvFacultyList.setLayoutManager(new LinearLayoutManager(this));

        adapter = new FacultyAdapter(facultyList, faculty -> openWorkDiary(faculty.getUserId()));
        rvFacultyList.setAdapter(adapter);

        fetchFacultyAndStatuses();
    }

    private void fetchFacultyAndStatuses() {
        ParseQuery<ParseUser> facultyQuery = ParseUser.getQuery();
        facultyQuery.whereNotEqualTo("name", "Admin");
        facultyQuery.findInBackground((users, e) -> {
            if (e == null && users != null) {
                facultyList.clear();
                for (ParseUser user : users) {
                    facultyList.add(new Faculty(user.getString("name"), "Pending", user.getObjectId()));
                }
                tvTotalFaculty.setText(String.valueOf(facultyList.size()));
                fetchTodaySubmissions();
            }
        });
    }

    private void fetchTodaySubmissions() {
        // Adjust this format string to match EXACTLY what you see in your Workdiary DB's dayDate field.
        String today = new SimpleDateFormat("EEE dd-MM-yyyy", Locale.ENGLISH).format(new Date()).toUpperCase();
        Log.d("DateDebug", "Today's date string: " + today);

        ParseQuery<ParseObject> diaryQuery = ParseQuery.getQuery("Workdiary");
        diaryQuery.whereEqualTo("dayDate", today);
        diaryQuery.include("user"); // Load the user pointer
        diaryQuery.findInBackground((submissions, e) -> {
            if (e == null && submissions != null) {
                Set<String> submittedIds = new HashSet<>();
                for (ParseObject obj : submissions) {
                    ParseUser userPointer = obj.getParseUser("user");
                    if (userPointer != null) {
                        String userId = userPointer.getObjectId();
                        submittedIds.add(userId);
                        Log.d("WorkdiaryDebug", "Submission: userId=" + userId + ", dayDate=" + obj.getString("dayDate"));
                    }
                }
                int submittedCount = 0;
                for (Faculty faculty : facultyList) {
                    Log.d("FacultyDebug", "Faculty: userId=" + faculty.getUserId());
                    if (submittedIds.contains(faculty.getUserId())) {
                        faculty.setStatus("Submitted");
                        submittedCount++;
                    } else {
                        faculty.setStatus("Pending");
                    }
                }
                tvWorkdiarySubmitted.setText(String.valueOf(submittedCount));
                tvPendingSubmissions.setText(String.valueOf(facultyList.size() - submittedCount));
                adapter.notifyDataSetChanged();
            } else if (e != null) {
                Log.e("WorkdiaryError", "Error fetching submissions: " + e.getMessage());
            }
        });
    }

    private void openWorkDiary(String userId) {
        // Implement navigation to work diary if needed
    }
}
