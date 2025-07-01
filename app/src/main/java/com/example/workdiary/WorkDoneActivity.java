package com.example.workdiary;

import android.os.Bundle;
import android.widget.Button;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.parse.ParseObject;
import com.parse.ParseQuery;
import com.parse.ParseUser;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;

public class WorkDoneActivity extends AppCompatActivity {
    private List<WorkdoneRow> rowList = new ArrayList<>();
    private WorkdoneAdapter adapter;
    private boolean isEditMode = false;

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
            if (position >= 0 && position < rowList.size()) {
                rowList.remove(position);
                adapter.notifyItemRemoved(position);
                adapter.notifyItemRangeChanged(position, rowList.size() - position);
            }
        });
        rv.setAdapter(adapter);

        Button btnAddRow = findViewById(R.id.btn_add_row);
        Button btnEdit = findViewById(R.id.btn_edit);
        Button btnSave = findViewById(R.id.btn_save);

        btnAddRow.setOnClickListener(v -> {
            if (isEditMode) {
                rowList.add(new WorkdoneRow());
                adapter.notifyItemInserted(rowList.size() - 1);
            }
        });

        btnAddRow.setVisibility(Button.GONE);

        btnEdit.setOnClickListener(v -> {
            isEditMode = true;
            btnAddRow.setVisibility(Button.VISIBLE);
            adapter.setEditMode(true);
            Toast.makeText(this, "Edit mode enabled", Toast.LENGTH_SHORT).show();
        });

        btnSave.setOnClickListener(v -> {
            isEditMode = false;
            btnAddRow.setVisibility(Button.GONE);
            adapter.setEditMode(false);
            saveWorkdoneToBack4App();
        });

        loadOrFetchWorkdone();
    }

    private void loadOrFetchWorkdone() {
        ParseUser currentUser = ParseUser.getCurrentUser();
        if (currentUser == null) return;

        ParseQuery<ParseObject> query = ParseQuery.getQuery("WorkdoneStatement");
        query.whereEqualTo("user", currentUser);
        query.orderByAscending("dayDate").addAscendingOrder("time");

        query.findInBackground((list, e) -> {
            rowList.clear();
            boolean found = false;
            if (e == null && list != null && !list.isEmpty()) {
                for (ParseObject obj : list) {
                    WorkdoneRow row = new WorkdoneRow();
                    row.dayDate = obj.getString("dayDate");
                    row.time = obj.getString("time");
                    row.className = obj.getString("class"); // Correct field
                    row.course = obj.getString("course");
                    row.portion = obj.getString("portion");
                    row.no = obj.getString("no"); // Correct field
                    row.remarks = obj.getString("remarks");
                    rowList.add(row);
                }
                found = true;
            }
            runOnUiThread(() -> adapter.notifyDataSetChanged());
            if (!found) {
                fetchTimetableForAllPeriods();
            }
        });
    }

    private void saveWorkdoneToBack4App() {
        ParseUser currentUser = ParseUser.getCurrentUser();
        if (currentUser == null) return;

        // Delete all previous entries for this user before saving new ones
        ParseQuery<ParseObject> query = ParseQuery.getQuery("WorkdoneStatement");
        query.whereEqualTo("user", currentUser);
        query.findInBackground((list, e) -> {
            if (e == null && list != null) {
                for (ParseObject obj : list) {
                    try {
                        obj.delete();
                    } catch (Exception ex) {
                        ex.printStackTrace();
                    }
                }
            }
            // Now save the new data
            for (WorkdoneRow row : rowList) {
                ParseObject workdoneObj = new ParseObject("WorkdoneStatement");
                workdoneObj.put("user", currentUser);
                workdoneObj.put("dayDate", row.dayDate);
                workdoneObj.put("time", row.time);
                workdoneObj.put("class", row.className); // must match schema
                workdoneObj.put("course", row.course);
                workdoneObj.put("portion", row.portion);
                workdoneObj.put("no", row.no); // must match schema
                workdoneObj.put("remarks", row.remarks);
                workdoneObj.saveInBackground();
            }
            runOnUiThread(() -> Toast.makeText(this, "Workdone Statement saved!", Toast.LENGTH_SHORT).show());
        });
    }

    // --- REQUIRED: fetchTimetableForAllPeriods ---
    private void fetchTimetableForAllPeriods() {
        ParseUser currentUser = ParseUser.getCurrentUser();
        if (currentUser == null) return;
        String userId = currentUser.getObjectId();

        Date now = new Date();
        String todayDay = new SimpleDateFormat("EEEE", Locale.getDefault()).format(now).toUpperCase();
        String todayDate = new SimpleDateFormat("EEE dd-MM-yyyy", Locale.getDefault()).format(now).toUpperCase();
        int rowIdx = getRowIndexForDay(todayDay);

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
                        row.course = subject.trim().toUpperCase();
                        rowList.add(row);
                    }
                }
            }
            // Sort WorkdoneRows by date+time
            Collections.sort(rowList, (a, b) -> {
                SimpleDateFormat sdf = new SimpleDateFormat("EEE dd-MM-yyyy HH:mm", Locale.getDefault());
                try {
                    Date dateA = sdf.parse(a.dayDate + " " + a.time.split("-")[0]);
                    Date dateB = sdf.parse(b.dayDate + " " + b.time.split("-")[0]);
                    return dateA.compareTo(dateB);
                } catch (ParseException ex) {
                    return 0;
                }
            });
            fetchAndAssignPortionsForAllRows();
        });
    }

    // --- REQUIRED: fetchAndAssignPortionsForAllRows ---
    private void fetchAndAssignPortionsForAllRows() {
        ParseUser currentUser = ParseUser.getCurrentUser();
        if (currentUser == null) return;

        // Group WorkdoneRows by subject
        Map<String, List<WorkdoneRow>> subjectToRows = new HashMap<>();
        for (WorkdoneRow wr : rowList) {
            String subject = wr.course != null ? wr.course.trim().toUpperCase() : "";
            subjectToRows.computeIfAbsent(subject, k -> new ArrayList<>()).add(wr);
        }

        // For each subject, fetch the TopicPlan and assign portions
        for (String subject : subjectToRows.keySet()) {
            fetchTopicPlanAndAssignPortion(currentUser, subject, subjectToRows.get(subject));
        }
    }

    // --- REQUIRED: fetchTopicPlanAndAssignPortion ---
    private void fetchTopicPlanAndAssignPortion(ParseUser user, String subject, List<WorkdoneRow> workdoneRows) {
        ParseQuery<ParseObject> topicQuery = ParseQuery.getQuery("TopicPlan");
        topicQuery.whereEqualTo("user", user);
        topicQuery.whereEqualTo("subjectName", subject);

        topicQuery.getFirstInBackground((topicPlan, e) -> {
            List<Map> sortedTopicRows = new ArrayList<>();
            if (e == null && topicPlan != null) {
                List<Object> rowsList = topicPlan.getList("rows");
                if (rowsList != null) {
                    for (Object rowObj : rowsList) {
                        if (rowObj instanceof Map) {
                            sortedTopicRows.add((Map) rowObj);
                        }
                    }
                    // Sort by week, then day if needed
                    Collections.sort(sortedTopicRows, (a, b) -> {
                        int weekA = parseNumberFromString(a.get("week"));
                        int weekB = parseNumberFromString(b.get("week"));
                        int dayA = parseNumberFromString(a.get("day"));
                        int dayB = parseNumberFromString(b.get("day"));
                        if (weekA != weekB) return weekA - weekB;
                        return dayA - dayB;
                    });
                }
            }
            // Build portions list: use first subTopic if present, else mainTopic
            List<String> portionsOrdered = new ArrayList<>();
            for (Map row : sortedTopicRows) {
                List<String> subTopics = null;
                Object subTopicsObj = row.get("subTopics");
                if (subTopicsObj instanceof List) {
                    subTopics = (List<String>) subTopicsObj;
                }
                String mainTopic = (String) row.get("mainTopic");
                String portion = null;
                if (subTopics != null && !subTopics.isEmpty() && subTopics.get(0) != null && !subTopics.get(0).trim().isEmpty()) {
                    portion = subTopics.get(0);
                } else if (mainTopic != null && !mainTopic.trim().isEmpty()) {
                    portion = mainTopic;
                }
                if (portion != null && !portion.trim().isEmpty()) {
                    portionsOrdered.add(portion);
                }
            }
            // Assign portions in order to the workdoneRows for this subject
            for (int i = 0; i < workdoneRows.size(); i++) {
                if (i < portionsOrdered.size()) {
                    workdoneRows.get(i).portion = portionsOrdered.get(i);
                } else {
                    workdoneRows.get(i).portion = "";
                }
            }
            runOnUiThread(() -> adapter.notifyDataSetChanged());
        });
    }

    // --- REQUIRED: parseNumberFromString ---
    private int parseNumberFromString(Object value) {
        if (value == null) return -1;
        String str = value.toString().replaceAll("[^\\d]", "");
        if (str.isEmpty()) return -1;
        try {
            return Integer.parseInt(str);
        } catch (NumberFormatException e) {
            return -1;
        }
    }

    // --- REQUIRED: getRowIndexForDay ---
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

    @Override
    protected void onResume() {
        super.onResume();
        loadOrFetchWorkdone();
    }
}
