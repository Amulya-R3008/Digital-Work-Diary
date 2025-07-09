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
        Log.d("DebugCheck", "onCreate called");
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
        Log.d("DebugCheck", "fetchFacultyAndStatuses called");
        ParseQuery<ParseUser> facultyQuery = ParseUser.getQuery();
        facultyQuery.whereNotEqualTo("name", "Admin");
        facultyQuery.findInBackground((users, e) -> {
            if (e != null) {
                Log.e("DebugCheck", "Error fetching faculty: " + e.getMessage());
                return;
            }
            if (users == null || users.isEmpty()) {
                Log.e("DebugCheck", "No faculty found (excluding admin).");
                return;
            }
            facultyList.clear();
            for (ParseUser user : users) {
                facultyList.add(new Faculty(user.getString("name"), "Pending", user.getObjectId()));
            }
            tvTotalFaculty.setText(String.valueOf(facultyList.size()));

            for (Faculty faculty : facultyList) {
                Log.d("DebugCheck", "Faculty name: " + faculty.getName() + ", userId: " + faculty.getUserId());
            }

            fetchTodaySubmissionsByDayDate();
        });
    }

    private void fetchTodaySubmissionsByDayDate() {
        Log.d("DebugCheck", "fetchTodaySubmissionsByDayDate called");

        // Format: "EEE dd-MM-yyyy" (e.g., "WED 09-07-2025")
        String today = new SimpleDateFormat("EEE dd-MM-yyyy", Locale.ENGLISH)
                .format(new Date())
                .toUpperCase()
                .trim();
        Log.d("DebugCheck", "App generated dayDate: '" + today + "', length=" + today.length());

        ParseQuery<ParseObject> workdoneQuery = ParseQuery.getQuery("WorkdoneStatement");
        workdoneQuery.whereEqualTo("dayDate", today);
        workdoneQuery.include("user");
        workdoneQuery.setCachePolicy(ParseQuery.CachePolicy.NETWORK_ONLY);

        workdoneQuery.findInBackground((submissions, e) -> {
            Log.d("DebugCheck", "WorkdoneStatement query callback called");
            if (e != null) {
                Log.e("DebugCheck", "Error fetching submissions: " + e.getMessage());
                return;
            }
            if (submissions == null || submissions.isEmpty()) {
                Log.w("DebugCheck", "No submissions found for today by dayDate. Check DB data and string format.");
            } else {
                Log.d("DebugCheck", "Fetched submissions count: " + submissions.size());
            }

            Set<String> submittedIds = new HashSet<>();
            if (submissions != null) {
                for (ParseObject obj : submissions) {
                    ParseUser userPointer = obj.getParseUser("user");
                    String dbDayDate = obj.getString("dayDate");
                    Log.d("DebugCheck", "WorkdoneStatement: dayDate='" + dbDayDate + "', length=" + (dbDayDate != null ? dbDayDate.length() : 0)
                            + ", userId=" + (userPointer != null ? userPointer.getObjectId() : "null"));
                    if (userPointer != null) {
                        submittedIds.add(userPointer.getObjectId());
                    }
                }
            }

            int submittedCount = 0;
            for (Faculty faculty : facultyList) {
                Log.d("DebugCheck", "Faculty: name='" + faculty.getName() + "', userId=" + faculty.getUserId());
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
            Log.d("DebugCheck", "Submitted count: " + submittedCount + ", Pending count: " + (facultyList.size() - submittedCount));

            if (submissions == null || submissions.isEmpty()) {
                Log.e("DebugCheck", "No WorkdoneStatement entries found for today by dayDate. Possible causes: wrong string format, no data for today.");
            } else if (submittedCount == 0) {
                Log.e("DebugCheck", "No faculty matched submissions for today. Possible causes: pointer mismatch, userId mismatch, or wrong user in WorkdoneStatement.");
            } else {
                Log.i("DebugCheck", "Matching and status update successful.");
            }
        });
    }

    private void openWorkDiary(String userId) {
        Log.d("DebugCheck", "openWorkDiary called for userId: " + userId);
        // Implement navigation to work diary if needed
    }
}
