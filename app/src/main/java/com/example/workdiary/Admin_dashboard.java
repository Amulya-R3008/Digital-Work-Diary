package com.example.workdiary;

import android.content.Intent;
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
import java.util.*;

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

        adapter = new FacultyAdapter(facultyList, faculty -> openWorkDiary(faculty));
        rvFacultyList.setAdapter(adapter);

        fetchFacultyAndStatuses();
    }

    private void fetchFacultyAndStatuses() {
        ParseQuery<ParseUser> facultyQuery = ParseUser.getQuery();
        facultyQuery.whereNotEqualTo("name", "Admin");
        facultyQuery.findInBackground((users, e) -> {
            if (e != null || users == null) return;
            facultyList.clear();
            for (ParseUser user : users) {
                facultyList.add(new Faculty(user.getString("name"), "Pending", user.getObjectId()));
            }
            tvTotalFaculty.setText(String.valueOf(facultyList.size()));
            fetchTodaySubmissionsByDayDate();
        });
    }

    private void fetchTodaySubmissionsByDayDate() {
        String today = new SimpleDateFormat("EEE dd-MM-yyyy", Locale.ENGLISH)
                .format(new Date()).toUpperCase().trim();

        ParseQuery<ParseObject> workdoneQuery = ParseQuery.getQuery("WorkdoneStatement");
        workdoneQuery.whereEqualTo("dayDate", today);
        workdoneQuery.include("user");

        workdoneQuery.findInBackground((submissions, e) -> {
            Set<String> submittedIds = new HashSet<>();
            if (submissions != null) {
                for (ParseObject obj : submissions) {
                    ParseUser userPointer = obj.getParseUser("user");
                    if (userPointer != null) submittedIds.add(userPointer.getObjectId());
                }
            }
            int submittedCount = 0;
            for (Faculty faculty : facultyList) {
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
        });
    }

    private void openWorkDiary(Faculty faculty) {
        Intent intent = new Intent(this, WorkDoneActivity.class);
        intent.putExtra("userId", faculty.getUserId());
        intent.putExtra("facultyName", faculty.getName());
        startActivity(intent);
    }
}
