package com.example.workdiary;

import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.*;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import com.parse.ParseObject;
import com.parse.ParseQuery;
import com.parse.ParseUser;
import org.json.JSONArray;
import org.json.JSONObject;
import org.json.JSONException;
import java.util.*;

public class TopicPlannerFragment extends Fragment {

    private static final String TAG = "TopicPlannerFragment";

    private EditText courseTitleEditText, totalHoursEditText, seeMarksEditText,
            semesterEditText, courseCodeEditText, creditsEditText, cieMarksEditText, academicYearEditText;

    private TableLayout topicTable;
    private Button addRowButton, deleteRowButton;

    private List<String> weekList, dayList, bloomList, coList, activityList;
    private List<UnitData> unitDataList = new ArrayList<>();
    private boolean unitsLoaded = false;
    private Map<String, Integer> unitAssignedCount = new HashMap<>();

    // Data classes
    private static class UnitData {
        String unit;
        int hours;
        List<MainTopic> mainTopics;
        List<String> subtopics; // Only for units with no main topics
        UnitData(String unit, int hours, List<MainTopic> mainTopics, List<String> subtopics) {
            this.unit = unit;
            this.hours = hours;
            this.mainTopics = mainTopics;
            this.subtopics = subtopics;
        }
    }

    private static class MainTopic {
        String name;
        List<String> subtopics;
        MainTopic(String name, List<String> subtopics) {
            this.name = name;
            this.subtopics = subtopics;
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {

        View rootView = inflater.inflate(R.layout.fragment_topic_planner, container, false);

        TextView facultyNameTextView = rootView.findViewById(R.id.facultyName);
        ParseUser currentUser = ParseUser.getCurrentUser();
        if (currentUser != null) {
            String facultyName = currentUser.getString("name");
            facultyNameTextView.setText(facultyName != null && !facultyName.isEmpty() ? "Faculty: " + facultyName : "Faculty: Name not available");
        } else {
            facultyNameTextView.setText("Faculty: Not logged in");
        }

        courseTitleEditText = rootView.findViewById(R.id.courseTitle);
        totalHoursEditText = rootView.findViewById(R.id.totalHours);
        seeMarksEditText = rootView.findViewById(R.id.seeMarks);
        semesterEditText = rootView.findViewById(R.id.semester);
        courseCodeEditText = rootView.findViewById(R.id.courseCode);
        creditsEditText = rootView.findViewById(R.id.credits);
        cieMarksEditText = rootView.findViewById(R.id.cieMarks);
        academicYearEditText = rootView.findViewById(R.id.academicYear);

        topicTable = rootView.findViewById(R.id.topicTable);
        addRowButton = rootView.findViewById(R.id.addRowButton);
        deleteRowButton = rootView.findViewById(R.id.deleteRowButton);

        weekList = new ArrayList<>();
        for (int i = 1; i <= 20; i++) weekList.add(String.valueOf(i));
        dayList = new ArrayList<>();
        for (int i = 1; i <= 140; i++) dayList.add(String.valueOf(i));
        bloomList = Arrays.asList("L1", "L2", "L3", "L4", "L5");
        coList = Arrays.asList("CO1", "CO2", "CO3", "CO4", "CO5");
        activityList = Arrays.asList("Google Classroom", "Quiz", "QuickLearn", "Presentation");

        fetchSubjectMetadataAndUnits();

        addRowButton.setOnClickListener(v -> {
            if (!unitsLoaded || unitDataList.isEmpty()) {
                Toast.makeText(getContext(), "Please wait, units are still loading...", Toast.LENGTH_SHORT).show();
            } else {
                addTableRow();
            }
        });
        deleteRowButton.setOnClickListener(v -> deleteLastTableRow());

        return rootView;
    }

    private void fetchSubjectMetadataAndUnits() {
        Bundle args = getArguments();
        if (args == null || !args.containsKey("subjectName")) {
            Toast.makeText(getContext(), "Missing subject name", Toast.LENGTH_SHORT).show();
            Log.e(TAG, "No subjectName passed in arguments");
            return;
        }
        String subjectName = args.getString("subjectName");

        ParseQuery<ParseObject> query = ParseQuery.getQuery("SubjectInfo");
        query.whereEqualTo("subjectName", subjectName);

        query.findInBackground((objects, e) -> {
            if (e == null && objects != null && !objects.isEmpty()) {
                ParseObject subject = objects.get(0);

                if (subject.getString("subjectName") != null)
                    courseTitleEditText.setText(subject.getString("subjectName"));
                if (subject.getString("totalHours") != null)
                    totalHoursEditText.setText(subject.getString("totalHours"));
                if (subject.getString("seeMarks") != null)
                    seeMarksEditText.setText(subject.getString("seeMarks"));
                if (subject.getString("semester") != null)
                    semesterEditText.setText(subject.getString("semester"));
                if (subject.getString("courseCode") != null)
                    courseCodeEditText.setText(subject.getString("courseCode"));
                if (subject.getString("credits") != null)
                    creditsEditText.setText(subject.getString("credits"));
                if (subject.getString("cieMarks") != null)
                    cieMarksEditText.setText(subject.getString("cieMarks"));
                if (subject.getString("academicYear") != null)
                    academicYearEditText.setText(subject.getString("academicYear"));

                JSONArray unitsArray = subject.getJSONArray("units");
                if (unitsArray != null && unitsArray.length() > 0) {
                    parseUnitsArray(unitsArray);
                    unitsLoaded = true;
                    requireActivity().runOnUiThread(this::addTableRow);
                }
            } else {
                Log.e(TAG, "Error fetching subject: " + (e != null ? e.getMessage() : "No data"));
                Toast.makeText(getContext(), "Failed to fetch subject metadata", Toast.LENGTH_LONG).show();
            }
        });
    }

    private void parseUnitsArray(JSONArray unitsArray) {
        unitDataList.clear();
        try {
            for (int i = 0; i < unitsArray.length(); i++) {
                JSONObject unitObj = unitsArray.getJSONObject(i);
                String unit = unitObj.getString("unit");
                int hours = unitObj.optInt("hours", 0);

                List<MainTopic> mainTopics = new ArrayList<>();
                if (unitObj.has("mainTopics")) {
                    JSONArray mainTopicsArray = unitObj.getJSONArray("mainTopics");
                    for (int j = 0; j < mainTopicsArray.length(); j++) {
                        JSONObject mtObj = mainTopicsArray.getJSONObject(j);
                        String name = mtObj.getString("name");
                        List<String> subtopics = new ArrayList<>();
                        JSONArray subtopicsArray = mtObj.getJSONArray("subtopics");
                        for (int k = 0; k < subtopicsArray.length(); k++) {
                            subtopics.add(subtopicsArray.getString(k));
                        }
                        mainTopics.add(new MainTopic(name, subtopics));
                    }
                }
                List<String> subtopics = new ArrayList<>();
                if (unitObj.has("subtopics")) {
                    JSONArray subtopicsArray = unitObj.getJSONArray("subtopics");
                    for (int j = 0; j < subtopicsArray.length(); j++) {
                        subtopics.add(subtopicsArray.getString(j));
                    }
                }
                unitDataList.add(new UnitData(unit, hours, mainTopics, subtopics));
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    private void addTableRow() {
        if (getContext() == null || unitDataList.isEmpty()) return;
        LayoutInflater inflater = LayoutInflater.from(getContext());
        TableRow row = (TableRow) inflater.inflate(R.layout.table_row_topic, topicTable, false);

        Spinner weekSpinner = row.findViewById(R.id.spinnerWeek);
        weekSpinner.setAdapter(new ArrayAdapter<>(getContext(), android.R.layout.simple_spinner_item, weekList));

        Spinner daySpinner = row.findViewById(R.id.spinnerDay);
        daySpinner.setAdapter(new ArrayAdapter<>(getContext(), android.R.layout.simple_spinner_item, dayList));

        Spinner unitSpinner = row.findViewById(R.id.spinnerUnit);
        Spinner mainTopicSpinner = row.findViewById(R.id.spinnerMainTopic);
        Spinner subTopicSpinner = row.findViewById(R.id.spinnerSubTopic);

        // Only show units that haven't reached their hours limit
        List<String> availableUnits = new ArrayList<>();
        for (UnitData ud : unitDataList) {
            int assigned = unitAssignedCount.getOrDefault(ud.unit, 0);
            if (assigned < ud.hours) {
                availableUnits.add(ud.unit);
            }
        }
        if (availableUnits.isEmpty()) {
            Toast.makeText(getContext(), "All units have reached their maximum allowed hours.", Toast.LENGTH_SHORT).show();
            return;
        }
        ArrayAdapter<String> unitAdapter = new ArrayAdapter<>(getContext(), android.R.layout.simple_spinner_item, availableUnits);
        unitAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        unitSpinner.setAdapter(unitAdapter);

        unitSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                String selectedUnit = availableUnits.get(position);
                UnitData selectedUnitData = null;
                for (UnitData ud : unitDataList) {
                    if (ud.unit.equals(selectedUnit)) {
                        selectedUnitData = ud;
                        break;
                    }
                }
                if (selectedUnitData == null) return;

                // If mainTopics is empty, hide mainTopicSpinner and use subtopics
                if (selectedUnitData.mainTopics == null || selectedUnitData.mainTopics.isEmpty()) {
                    mainTopicSpinner.setVisibility(View.GONE);
                    setMultiSelectSpinner(subTopicSpinner, selectedUnitData.subtopics);
                } else {
                    mainTopicSpinner.setVisibility(View.VISIBLE);

                    List<String> mainTopicNames = new ArrayList<>();
                    for (MainTopic mt : selectedUnitData.mainTopics) {
                        mainTopicNames.add(mt.name);
                    }
                    ArrayAdapter<String> mainAdapter = new ArrayAdapter<>(getContext(), android.R.layout.simple_spinner_item, mainTopicNames);
                    mainAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                    mainTopicSpinner.setAdapter(mainAdapter);

                    final UnitData finalSelectedUnitData = selectedUnitData;
                    mainTopicSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                        @Override
                        public void onItemSelected(AdapterView<?> parent, View view, int pos, long id) {
                            MainTopic selectedMainTopic = finalSelectedUnitData.mainTopics.get(pos);
                            setMultiSelectSpinner(subTopicSpinner, selectedMainTopic.subtopics);
                        }
                        @Override
                        public void onNothingSelected(AdapterView<?> parent) {}
                    });
                }
            }
            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });

        // Set initial main topic and sub topic for the first available unit
        if (!availableUnits.isEmpty()) {
            String firstUnit = availableUnits.get(0);
            UnitData firstUnitData = null;
            for (UnitData ud : unitDataList) {
                if (ud.unit.equals(firstUnit)) {
                    firstUnitData = ud;
                    break;
                }
            }
            if (firstUnitData != null) {
                if (firstUnitData.mainTopics == null || firstUnitData.mainTopics.isEmpty()) {
                    mainTopicSpinner.setVisibility(View.GONE);
                    setMultiSelectSpinner(subTopicSpinner, firstUnitData.subtopics);
                } else {
                    mainTopicSpinner.setVisibility(View.VISIBLE);
                    List<String> mainTopicNames = new ArrayList<>();
                    for (MainTopic mt : firstUnitData.mainTopics) {
                        mainTopicNames.add(mt.name);
                    }
                    ArrayAdapter<String> mainAdapter = new ArrayAdapter<>(getContext(), android.R.layout.simple_spinner_item, mainTopicNames);
                    mainAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                    mainTopicSpinner.setAdapter(mainAdapter);

                    if (!firstUnitData.mainTopics.isEmpty()) {
                        setMultiSelectSpinner(subTopicSpinner, firstUnitData.mainTopics.get(0).subtopics);
                    }
                    final UnitData finalFirstUnitData = firstUnitData;
                    mainTopicSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                        @Override
                        public void onItemSelected(AdapterView<?> parent, View view, int pos, long id) {
                            MainTopic selectedMainTopic = finalFirstUnitData.mainTopics.get(pos);
                            setMultiSelectSpinner(subTopicSpinner, selectedMainTopic.subtopics);
                        }
                        @Override
                        public void onNothingSelected(AdapterView<?> parent) {}
                    });
                }
            }
        }

        Spinner bloomSpinner = row.findViewById(R.id.spinnerBloom);
        bloomSpinner.setAdapter(new ArrayAdapter<>(getContext(), android.R.layout.simple_spinner_item, bloomList));

        Spinner coSpinner = row.findViewById(R.id.spinnerCO);
        coSpinner.setAdapter(new ArrayAdapter<>(getContext(), android.R.layout.simple_spinner_item, coList));

        Spinner activitySpinner = row.findViewById(R.id.spinnerActivity);
        ArrayAdapter<String> activityAdapter = new ArrayAdapter<>(getContext(), android.R.layout.simple_spinner_item, activityList);
        activityAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        activitySpinner.setAdapter(activityAdapter);

        // Track unit assignment count when row is added
        String selectedUnit = availableUnits.get(0);
        unitAssignedCount.put(selectedUnit, unitAssignedCount.getOrDefault(selectedUnit, 0) + 1);

        topicTable.addView(row);
    }

    private void setMultiSelectSpinner(Spinner spinner, List<String> items) {
        spinner.setOnTouchListener((v, event) -> {
            showMultiSelectDialog(items, spinner);
            return true;
        });
    }

    private void showMultiSelectDialog(List<String> items, Spinner spinner) {
        boolean[] checkedItems = new boolean[items.size()];
        AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
        builder.setTitle("Select Items")
                .setMultiChoiceItems(items.toArray(new String[0]), checkedItems,
                        (dialog, which, isChecked) -> checkedItems[which] = isChecked)
                .setPositiveButton("OK", (dialog, which) -> {
                    List<String> selected = new ArrayList<>();
                    for (int i = 0; i < checkedItems.length; i++) {
                        if (checkedItems[i]) selected.add(items.get(i));
                    }
                    updateSpinnerDisplay(spinner, selected);
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void updateSpinnerDisplay(Spinner spinner, List<String> selected) {
        ArrayAdapter<String> adapter = new ArrayAdapter<>(getContext(),
                android.R.layout.simple_spinner_item,
                Collections.singletonList(TextUtils.join(", ", selected)));
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinner.setAdapter(adapter);
    }

    private void deleteLastTableRow() {
        int childCount = topicTable.getChildCount();
        if (childCount > 1) { // Keep header row
            View lastChild = topicTable.getChildAt(childCount-1);
            if (lastChild instanceof TableRow) {
                Spinner unitSpinner = lastChild.findViewById(R.id.spinnerUnit);
                if (unitSpinner != null && unitSpinner.getSelectedItem() != null) {
                    String unit = unitSpinner.getSelectedItem().toString();
                    int count = unitAssignedCount.getOrDefault(unit, 1);
                    if (count > 1) {
                        unitAssignedCount.put(unit, count - 1);
                    } else {
                        unitAssignedCount.remove(unit);
                    }
                }
            }
            topicTable.removeViewAt(childCount - 1);
        }
    }
}
