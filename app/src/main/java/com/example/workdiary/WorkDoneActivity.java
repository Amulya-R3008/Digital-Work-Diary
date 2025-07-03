package com.example.workdiary;

import android.os.Bundle;
import android.util.Log;
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
        adapter = new WorkdoneAdapter(rowList);
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

        String todayDate = new SimpleDateFormat("EEE dd-MM-yyyy", Locale.getDefault()).format(new Date()).toUpperCase();

        query.findInBackground((list, e) -> {
            Map<String, WorkdoneRow> uniqueRows = new LinkedHashMap<>();
            Set<String> savedTodayKeys = new HashSet<>();
            if (e == null && list != null && !list.isEmpty()) {
                for (ParseObject obj : list) {
                    WorkdoneRow row = new WorkdoneRow();
                    row.dayDate = obj.getString("dayDate");
                    row.time = obj.getString("time");
                    row.classField = obj.getString("class");
                    row.course = obj.getString("course");
                    row.portion = obj.getString("portion");
                    row.no = obj.getString("no");
                    row.remarks = obj.getString("remarks");
                    String dayKey = (row.dayDate != null ? row.dayDate.trim() : "") + "|" +
                            (row.time != null ? row.time.trim() : "") + "|" +
                            (row.course != null ? row.course.trim().toUpperCase() : "");
                    uniqueRows.put(dayKey, row);
                    if (todayDate.equals(row.dayDate)) {
                        String todayKey = (row.time != null ? row.time.trim() : "") + "|" +
                                (row.course != null ? row.course.trim().toUpperCase() : "");
                        savedTodayKeys.add(todayKey);
                    }
                }
            }
            rowList.clear();
            rowList.addAll(uniqueRows.values());
            runOnUiThread(() -> adapter.notifyDataSetChanged());
            fetchTimetableForTodayAndAssignPortion(savedTodayKeys);
        });
    }

    private void saveWorkdoneToBack4App() {
        ParseUser currentUser = ParseUser.getCurrentUser();
        if (currentUser == null) return;

        String todayDate = new SimpleDateFormat("EEE dd-MM-yyyy", Locale.getDefault()).format(new Date()).toUpperCase();

        ParseQuery<ParseObject> query = ParseQuery.getQuery("WorkdoneStatement");
        query.whereEqualTo("user", currentUser);
        query.whereEqualTo("dayDate", todayDate);
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
            for (WorkdoneRow row : rowList) {
                if (todayDate.equals(row.dayDate)) {
                    ParseObject workdoneObj = new ParseObject("WorkdoneStatement");
                    workdoneObj.put("user", currentUser);
                    workdoneObj.put("dayDate", row.dayDate);
                    workdoneObj.put("time", row.time);
                    workdoneObj.put("class", row.classField);
                    workdoneObj.put("course", row.course);
                    workdoneObj.put("portion", row.portion);
                    workdoneObj.put("no", row.no);
                    workdoneObj.put("remarks", row.remarks);
                    workdoneObj.saveInBackground();
                }
            }
            runOnUiThread(() -> Toast.makeText(this, "Workdone Statement saved!", Toast.LENGTH_SHORT).show());
        });
    }

    private void fetchTimetableForTodayAndAssignPortion(Set<String> savedTodayKeys) {
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
            boolean added = false;
            List<WorkdoneRow> autoRows = new ArrayList<>();
            if (e == null && timetable != null) {
                for (int i = 0; i < TIMES.length; i++) {
                    String cellField = "cell_" + rowIdx + "_" + COLS[i];
                    String subject = timetable.getString(cellField);
                    String timeKey = TIMES[i].trim();
                    String subjectKey = (subject != null) ? subject.trim().toUpperCase() : "";
                    String key = timeKey + "|" + subjectKey;
                    if (subject != null && !subject.trim().isEmpty()
                            && !savedTodayKeys.contains(key)) {
                        WorkdoneRow row = new WorkdoneRow();
                        row.dayDate = todayDate;
                        row.time = timeKey;
                        row.course = subjectKey;
                        autoRows.add(row);
                        rowList.add(row);
                        added = true;
                    }
                }
            }
            if (added) {
                fetchAndAssignPortionsForAutoRows(todayDate, autoRows);
            } else {
                runOnUiThread(() -> adapter.notifyDataSetChanged());
            }
        });
    }

    private void fetchAndAssignPortionsForAutoRows(String todayDate, List<WorkdoneRow> autoRows) {
        ParseUser currentUser = ParseUser.getCurrentUser();
        if (currentUser == null) return;

        Map<String, List<WorkdoneRow>> subjectToAutoRows = new HashMap<>();
        for (WorkdoneRow wr : autoRows) {
            String subject = wr.course != null ? wr.course.trim().toUpperCase() : "";
            subjectToAutoRows.computeIfAbsent(subject, k -> new ArrayList<>()).add(wr);
        }

        for (String subject : subjectToAutoRows.keySet()) {
            Set<String> coveredPortions = new HashSet<>();
            for (WorkdoneRow wr : rowList) {
                String courseKey = wr.course != null ? wr.course.trim().toUpperCase() : "";
                String portion = wr.portion != null ? wr.portion.trim() : "";
                if (subject.equals(courseKey) && !portion.isEmpty()) {
                    coveredPortions.add(portion);
                }
            }
            assignNextPortionsToAutoRows(currentUser, subject, subjectToAutoRows.get(subject), coveredPortions);
        }
    }

    private void assignNextPortionsToAutoRows(ParseUser user, String subject, List<WorkdoneRow> autoRows, Set<String> coveredPortions) {
        ParseQuery<ParseObject> topicQuery = ParseQuery.getQuery("TopicPlan");
        topicQuery.whereEqualTo("user", user);
        topicQuery.whereEqualTo("subjectName", subject);

        topicQuery.getFirstInBackground((topicPlan, e2) -> {
            List<String> portionsOrdered = new ArrayList<>();
            if (e2 == null && topicPlan != null) {
                List<Object> rowsList = topicPlan.getList("rows");
                if (rowsList != null) {
                    for (Object rowObj : rowsList) {
                        if (rowObj instanceof Map) {
                            Map row = (Map) rowObj;
                            List<String> subTopics = null;
                            Object subTopicsObj = row.get("subTopics");
                            if (subTopicsObj instanceof List) {
                                subTopics = (List<String>) subTopicsObj;
                            }
                            String mainTopic = (String) row.get("mainTopic");
                            if (subTopics != null && !subTopics.isEmpty()) {
                                for (Object sub : subTopics) {
                                    if (sub != null) {
                                        String[] splitSubs = sub.toString().split(",");
                                        for (String s : splitSubs) {
                                            String portion = s.trim();
                                            if (!portion.isEmpty() && !coveredPortions.contains(portion)) {
                                                portionsOrdered.add(portion);
                                            }
                                        }
                                    }
                                }
                            } else if (mainTopic != null && !mainTopic.trim().isEmpty() && !coveredPortions.contains(mainTopic.trim())) {
                                portionsOrdered.add(mainTopic.trim());
                            }
                        }
                    }
                }
            }
            for (int i = 0; i < autoRows.size(); i++) {
                if (i < portionsOrdered.size()) {
                    autoRows.get(i).portion = portionsOrdered.get(i);
                    coveredPortions.add(portionsOrdered.get(i));
                } else {
                    autoRows.get(i).portion = "";
                }
            }
            runOnUiThread(() -> adapter.notifyDataSetChanged());
        });
    }

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
