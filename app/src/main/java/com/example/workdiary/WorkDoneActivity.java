package com.example.workdiary;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.parse.ParseObject;
import com.parse.ParseQuery;
import com.parse.ParseUser;
import java.text.SimpleDateFormat;
import java.util.*;

public class WorkDoneActivity extends AppCompatActivity {
    private List<WorkdoneRow> rowList = new ArrayList<>();
    private WorkdoneAdapter adapter;
    private boolean isEditMode = false;

    // Define your period time slots and their column indices
    private static final String[] TIMES = {
            "9:00-10:00", "10:00-11:00", "11:00-11:30", "11:30-12:30",
            "12:30-1:30", "1:30-2:30", "2:30-3:30", "3:30-4:30"
    };
    private static final int[] COLS = {1,2,3,4,5,6,7,8};

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_work_done);

        RecyclerView rv = findViewById(R.id.rv_workdone);
        rv.setLayoutManager(new LinearLayoutManager(this));
        adapter = new WorkdoneAdapter(rowList, position -> {
            rowList.remove(position);
            adapter.notifyItemRemoved(position);
        });
        rv.setAdapter(adapter);

        Button btnAddRow = findViewById(R.id.btn_add_row);
        Button btnEdit = findViewById(R.id.btn_edit);
        Button btnSave = findViewById(R.id.btn_save);

        btnAddRow.setOnClickListener(v -> {
            if (isEditMode) {
                fetchTimetableForAllPeriods();
            }
        });

        btnAddRow.setVisibility(View.GONE);

        btnEdit.setOnClickListener(v -> {
            isEditMode = true;
            btnAddRow.setVisibility(View.VISIBLE);
            adapter.setEditMode(true);
            Toast.makeText(this, "Edit mode enabled", Toast.LENGTH_SHORT).show();
        });

        btnSave.setOnClickListener(v -> {
            isEditMode = false;
            btnAddRow.setVisibility(View.GONE);
            adapter.setEditMode(false);
            Toast.makeText(this, "Saved!", Toast.LENGTH_SHORT).show();
        });

        // Always fetch and display only periods with a class for today
        fetchTimetableForAllPeriods();
    }

    private int getRowIndexForDay(String day) {
        switch (day.toUpperCase()) {
            case "MONDAY": return 1;
            case "TUESDAY": return 2;
            case "WEDNESDAY": return 3;
            case "THURSDAY": return 4;
            case "FRIDAY": return 5;
            case "SATURDAY": return 6;
            default: return -1;
        }
    }

    // Fetch only periods with a class for today and fill the list
    private void fetchTimetableForAllPeriods() {
        ParseUser currentUser = ParseUser.getCurrentUser();
        if (currentUser == null) {
            Toast.makeText(this, "User not logged in", Toast.LENGTH_SHORT).show();
            return;
        }
        String userId = currentUser.getObjectId();

        Date now = new Date();
        String todayDay = new SimpleDateFormat("EEEE", Locale.getDefault()).format(now).toUpperCase();
        String todayDate = new SimpleDateFormat("EEE dd-MM-yyyy", Locale.getDefault()).format(now).toUpperCase();
        int rowIdx = getRowIndexForDay(todayDay);

        if (rowIdx == -1) {
            Toast.makeText(this, "Invalid day: " + todayDay, Toast.LENGTH_SHORT).show();
            return;
        }

        ParseQuery<ParseObject> query = ParseQuery.getQuery("Timetable");
        query.whereEqualTo("userId", userId);

        query.getFirstInBackground((timetable, e) -> {
            rowList.clear();
            if (e == null && timetable != null) {
                for (int i = 0; i < TIMES.length; i++) {
                    String cellField = "cell_" + rowIdx + "_" + COLS[i];
                    String subject = timetable.getString(cellField);
                    if (subject != null && !subject.trim().isEmpty()) {
                        WorkdoneRow row = new WorkdoneRow();
                        row.dayDate = todayDate;
                        row.time = TIMES[i];
                        row.course = subject;
                        rowList.add(row);
                    }
                }
            } else {
                Toast.makeText(this, "No timetable found for you.", Toast.LENGTH_SHORT).show();
            }
            runOnUiThread(() -> adapter.notifyDataSetChanged());
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        fetchTimetableForAllPeriods();
    }
}
