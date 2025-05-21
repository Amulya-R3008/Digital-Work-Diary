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
import org.json.JSONException;
import org.json.JSONObject;
import java.util.*;

public class TopicPlannerFragment extends Fragment {

    private static final String TAG = "TopicPlannerFragment";
    private TableLayout topicTable;
    private Button addRowButton, deleteRowButton, editButton, saveButton;
    private List<UnitData> unitDataList = new ArrayList<>();
    private boolean unitsLoaded = false;
    private boolean isEditMode = false;
    private ParseObject loadedPlan = null;

    // Info fields
    private TextView facultyName;
    private EditText courseTitle, totalHours, seeMarks, cieMarks, credits, semester, courseCode, academicYear;

    // Use a unique tag ID for storing subtopic selections in TableRow
    private static final int SUBTOPIC_TAG_KEY = R.id.btnSubtopics;

    private static class UnitData {
        String unit;
        String hours; // as string
        List<MainTopic> mainTopics;
        List<String> subtopics;
        UnitData(String unit, String hours, List<MainTopic> mainTopics, List<String> subtopics) {
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

        // Find all info fields
        facultyName = rootView.findViewById(R.id.facultyName);
        courseTitle = rootView.findViewById(R.id.courseTitle);
        totalHours = rootView.findViewById(R.id.totalHours);
        seeMarks = rootView.findViewById(R.id.seeMarks);
        cieMarks = rootView.findViewById(R.id.cieMarks);
        credits = rootView.findViewById(R.id.credits);
        semester = rootView.findViewById(R.id.semester);
        courseCode = rootView.findViewById(R.id.courseCode);
        academicYear = rootView.findViewById(R.id.academicYear);

        topicTable = rootView.findViewById(R.id.topicTable);
        addRowButton = rootView.findViewById(R.id.addRowButton);
        deleteRowButton = rootView.findViewById(R.id.deleteRowButton);
        editButton = rootView.findViewById(R.id.editButton);
        saveButton = rootView.findViewById(R.id.saveButton);

        // Show faculty name (prefer 'name' field, fallback to username)
        ParseUser currentUser = ParseUser.getCurrentUser();
        String displayName = currentUser.getString("name");
        if (displayName == null || displayName.trim().isEmpty()) {
            displayName = currentUser.getUsername();
        }
        facultyName.setText("Faculty: " + displayName);

        setupButtons();
        loadSubjectInfoAndPlan();

        return rootView;
    }

    private void loadSubjectInfoAndPlan() {
        String subjectName = getSubjectNameFromArgs();
        if (TextUtils.isEmpty(subjectName)) return;
        ParseQuery<ParseObject> query = ParseQuery.getQuery("SubjectInfo");
        query.whereEqualTo("subjectName", subjectName);
        query.getFirstInBackground((subject, e) -> {
            if (subject != null && getActivity() != null) {
                requireActivity().runOnUiThread(() -> {
                    // All fields as string
                    courseTitle.setText(subject.getString("subjectName"));
                    totalHours.setText(subject.getString("totalHours"));
                    seeMarks.setText(subject.getString("seeMarks"));
                    cieMarks.setText(subject.getString("cieMarks"));
                    credits.setText(subject.getString("credits"));
                    semester.setText(subject.getString("semester"));
                    courseCode.setText(subject.getString("courseCode"));

                    // Make these fields non-editable (except academicYear)
                    setEditTextEnabled(courseTitle, false);
                    setEditTextEnabled(totalHours, false);
                    setEditTextEnabled(seeMarks, false);
                    setEditTextEnabled(cieMarks, false);
                    setEditTextEnabled(credits, false);
                    setEditTextEnabled(semester, false);
                    setEditTextEnabled(courseCode, false);
                    setEditTextEnabled(academicYear, false); // Will be enabled in edit mode

                    // Parse units for the table
                    JSONArray unitsArray = subject.getJSONArray("units");
                    parseUnitsArray(unitsArray);

                    // Now load the plan or defaults
                    loadPlanOrDefaults();
                });
            }
        });
    }

    private void setEditTextEnabled(EditText editText, boolean enabled) {
        editText.setEnabled(enabled);
        editText.setFocusable(enabled);
        editText.setFocusableInTouchMode(enabled);
    }

    private void setupButtons() {
        editButton.setOnClickListener(v -> toggleEditMode(true));
        saveButton.setOnClickListener(v -> savePlan());
        addRowButton.setOnClickListener(v -> {
            if (unitsLoaded && isEditMode) {
                String currentUnit = getSelectedUnitFromLastRow();
                if (currentUnit != null && canAddMoreRowsForUnit(currentUnit)) {
                    addTableRow();
                } else {
                    showToast("Maximum hours reached for " + currentUnit);
                }
            } else {
                showToast("Enable edit mode first");
            }
        });
        deleteRowButton.setOnClickListener(v -> {
            if (isEditMode) deleteLastRow();
            else showToast("Enable edit mode first");
        });
    }

    private void loadPlanOrDefaults() {
        String subjectName = getSubjectNameFromArgs();
        if (TextUtils.isEmpty(subjectName)) return;
        ParseQuery<ParseObject> query = ParseQuery.getQuery("TopicPlan");
        query.whereEqualTo("subjectName", subjectName);
        query.whereEqualTo("user", ParseUser.getCurrentUser());
        query.getFirstInBackground((plan, e) -> {
            requireActivity().runOnUiThread(() -> {
                topicTable.removeViews(1, topicTable.getChildCount() - 1);
                if (plan != null) {
                    loadedPlan = plan;
                    // Load academic year
                    String planAcademicYear = plan.getString("academicYear");
                    academicYear.setText(planAcademicYear != null ? planAcademicYear : "");
                    setEditTextEnabled(academicYear, false);

                    JSONArray savedRows = plan.getJSONArray("rows");
                    if (savedRows != null) {
                        for (int i = 0; i < savedRows.length(); i++) {
                            try {
                                addRowWithData(savedRows.getJSONObject(i));
                            } catch (JSONException ex) {
                                Log.e(TAG, "Error parsing saved row", ex);
                            }
                        }
                    }
                    toggleEditMode(false);
                } else {
                    loadedPlan = null;
                    academicYear.setText(""); // Clear academic year for new plan
                    setEditTextEnabled(academicYear, false);
                    addTableRow();
                    toggleEditMode(false);
                }
            });
        });
        unitsLoaded = true;
    }

    private void parseUnitsArray(JSONArray unitsArray) {
        unitDataList.clear();
        if (unitsArray == null) return;
        try {
            for (int i = 0; i < unitsArray.length(); i++) {
                JSONObject unitObj = unitsArray.getJSONObject(i);
                String unit = unitObj.getString("unit");
                String hours = unitObj.optString("hours", ""); // as string
                List<MainTopic> mainTopics = new ArrayList<>();
                List<String> unitSubtopics = new ArrayList<>();
                JSONArray mainTopicsArray = unitObj.optJSONArray("mainTopics");
                if (mainTopicsArray != null && mainTopicsArray.length() > 0) {
                    for (int j = 0; j < mainTopicsArray.length(); j++) {
                        JSONObject topicObj = mainTopicsArray.getJSONObject(j);
                        String topicName = topicObj.getString("name");
                        JSONArray subtopicsArray = topicObj.getJSONArray("subtopics");
                        List<String> subtopics = new ArrayList<>();
                        for (int k = 0; k < subtopicsArray.length(); k++) {
                            subtopics.add(subtopicsArray.getString(k));
                        }
                        mainTopics.add(new MainTopic(topicName, subtopics));
                    }
                } else {
                    JSONArray subtopicsArray = unitObj.optJSONArray("subtopics");
                    if (subtopicsArray != null) {
                        for (int k = 0; k < subtopicsArray.length(); k++) {
                            unitSubtopics.add(subtopicsArray.getString(k));
                        }
                    }
                }
                unitDataList.add(new UnitData(unit, hours, mainTopics, unitSubtopics));
            }
        } catch (JSONException e) {
            Log.e(TAG, "Unit parsing error", e);
        }
    }

    private void addTableRow() {
        TableRow row = (TableRow) LayoutInflater.from(getContext())
                .inflate(R.layout.table_row_topic, topicTable, false);
        setupSpinners(row, null, null, null);
        topicTable.addView(row);
    }

    private void addRowWithData(JSONObject rowData) {
        TableRow row = (TableRow) LayoutInflater.from(getContext())
                .inflate(R.layout.table_row_topic, topicTable, false);

        try {
            String unit = rowData.getString("unit");
            String mainTopic = rowData.getString("mainTopic");
            List<String> subTopics = new ArrayList<>();
            JSONArray subtopicsArr = rowData.optJSONArray("subTopics");
            if (subtopicsArr != null) {
                for (int i = 0; i < subtopicsArr.length(); i++) {
                    subTopics.add(subtopicsArr.getString(i));
                }
            }
            setupSpinners(row, unit, mainTopic, subTopics);

            setSpinnerSelection(row.findViewById(R.id.spinnerWeek), rowData.getString("week"));
            setSpinnerSelection(row.findViewById(R.id.spinnerDay), rowData.getString("day"));
            setSpinnerSelection(row.findViewById(R.id.spinnerBloom), rowData.getString("bloom"));
            setSpinnerSelection(row.findViewById(R.id.spinnerCO), rowData.getString("co"));
            setSpinnerSelection(row.findViewById(R.id.spinnerActivity), rowData.getString("activity"));

            topicTable.addView(row);
        } catch (JSONException e) {
            Log.e(TAG, "Parse error in addRowWithData", e);
        }
    }

    private void setupSpinners(TableRow row, String unitValue, String mainTopicValue, List<String> subTopicsValue) {
        Spinner unitSpinner = row.findViewById(R.id.spinnerUnit);
        Spinner mainTopicSpinner = row.findViewById(R.id.spinnerMainTopic);

        unitSpinner.setOnItemSelectedListener(null);
        mainTopicSpinner.setOnItemSelectedListener(null);

        ArrayAdapter<String> unitAdapter = new ArrayAdapter<>(requireContext(),
                R.layout.custom_spinner_item, getAvailableUnits());
        unitAdapter.setDropDownViewResource(R.layout.custom_spinner_dropdown_item);
        unitSpinner.setAdapter(unitAdapter);
        if (unitValue != null) setSpinnerSelection(unitSpinner, unitValue);

        UnitData selectedUnit = getUnitData(unitValue != null ? unitValue : getAvailableUnits().get(0));
        List<String> mainTopics = new ArrayList<>();
        if (selectedUnit != null && selectedUnit.mainTopics != null) {
            for (MainTopic mt : selectedUnit.mainTopics) mainTopics.add(mt.name);
        }
        ArrayAdapter<String> mainTopicAdapter = new ArrayAdapter<>(requireContext(),
                R.layout.custom_spinner_item, mainTopics);
        mainTopicAdapter.setDropDownViewResource(R.layout.custom_spinner_dropdown_item);
        mainTopicSpinner.setAdapter(mainTopicAdapter);
        if (mainTopicValue != null) setSpinnerSelection(mainTopicSpinner, mainTopicValue);

        String selectedMainTopic = mainTopicValue != null ? mainTopicValue :
                (mainTopicSpinner.getSelectedItem() != null ? mainTopicSpinner.getSelectedItem().toString() : null);
        List<String> subtopicsList = new ArrayList<>();
        if (selectedUnit != null && selectedUnit.mainTopics != null) {
            for (MainTopic mt : selectedUnit.mainTopics) {
                if (mt.name.equals(selectedMainTopic)) {
                    subtopicsList = mt.subtopics;
                    break;
                }
            }
        }
        if (subtopicsList.isEmpty() && selectedUnit != null && selectedUnit.subtopics != null) {
            subtopicsList = selectedUnit.subtopics;
        }
        setupSubtopicsButton(row, subtopicsList, subTopicsValue);

        unitSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                String newUnit = unitSpinner.getSelectedItem().toString();
                UnitData newUnitData = getUnitData(newUnit);
                List<String> newMainTopics = new ArrayList<>();
                if (newUnitData != null && newUnitData.mainTopics != null) {
                    for (MainTopic mt : newUnitData.mainTopics) newMainTopics.add(mt.name);
                }
                ArrayAdapter<String> newMainTopicAdapter = new ArrayAdapter<>(requireContext(),
                        R.layout.custom_spinner_item, newMainTopics);
                newMainTopicAdapter.setDropDownViewResource(R.layout.custom_spinner_dropdown_item);
                mainTopicSpinner.setAdapter(newMainTopicAdapter);
                if (!newMainTopics.isEmpty()) mainTopicSpinner.setSelection(0);
            }
            @Override public void onNothingSelected(AdapterView<?> parent) {}
        });

        mainTopicSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                String selectedUnit = unitSpinner.getSelectedItem().toString();
                UnitData unitData = getUnitData(selectedUnit);
                String selectedMainTopic = mainTopicSpinner.getSelectedItem() != null ?
                        mainTopicSpinner.getSelectedItem().toString() : null;
                List<String> subtopicsList = new ArrayList<>();
                if (unitData != null && unitData.mainTopics != null) {
                    for (MainTopic mt : unitData.mainTopics) {
                        if (mt.name.equals(selectedMainTopic)) {
                            subtopicsList = mt.subtopics;
                            break;
                        }
                    }
                }
                if (subtopicsList.isEmpty() && unitData != null && unitData.subtopics != null) {
                    subtopicsList = unitData.subtopics;
                }
                setupSubtopicsButton(row, subtopicsList, (List<String>) row.getTag(SUBTOPIC_TAG_KEY));
            }
            @Override public void onNothingSelected(AdapterView<?> parent) {}
        });

        setupCustomSpinner(row, R.id.spinnerWeek, R.array.weeks, "Week 1");
        setupCustomSpinner(row, R.id.spinnerDay, R.array.days, "Day 1");
        setupCustomSpinner(row, R.id.spinnerBloom, R.array.bloom_levels, "L1: Remember");
        setupCustomSpinner(row, R.id.spinnerCO, R.array.course_outcomes, "CO1");
        setupCustomSpinner(row, R.id.spinnerActivity, R.array.activities, "Google Classroom");

        Button btn = row.findViewById(R.id.btnSubtopics);
        btn.setEnabled(isEditMode);
    }

    private void setupCustomSpinner(TableRow row, int spinnerId, int arrayResId, String defaultValue) {
        Spinner spinner = row.findViewById(spinnerId);
        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(
                requireContext(), arrayResId, R.layout.custom_spinner_item);
        adapter.setDropDownViewResource(R.layout.custom_spinner_dropdown_item);
        spinner.setAdapter(adapter);
        setSpinnerSelection(spinner, defaultValue);
    }

    private void setupSubtopicsButton(TableRow row, List<String> items, List<String> savedSelection) {
        Button btn = row.findViewById(R.id.btnSubtopics);
        if (items == null || items.isEmpty()) {
            btn.setText("No Subtopics Available");
            row.setTag(SUBTOPIC_TAG_KEY, new ArrayList<>());
            btn.setEnabled(false);
            return;
        }
        btn.setEnabled(isEditMode);
        List<String> selected = new ArrayList<>(savedSelection != null ? savedSelection : new ArrayList<>());
        btn.setOnClickListener(v -> {
            boolean[] checkedItems = new boolean[items.size()];
            for (int i = 0; i < items.size(); i++) {
                checkedItems[i] = selected.contains(items.get(i));
            }
            new AlertDialog.Builder(requireContext())
                    .setTitle("Select Subtopics")
                    .setMultiChoiceItems(items.toArray(new String[0]), checkedItems, (dialog, which, isChecked) -> {
                        if (isChecked) {
                            if (!selected.contains(items.get(which))) selected.add(items.get(which));
                        } else {
                            selected.remove(items.get(which));
                        }
                    })
                    .setPositiveButton("OK", (dialog, which) -> {
                        row.setTag(SUBTOPIC_TAG_KEY, new ArrayList<>(selected));
                        updateButtonText(btn, selected);
                    })
                    .setNegativeButton("Cancel", null)
                    .show();
        });
        updateButtonText(btn, selected);
        row.setTag(SUBTOPIC_TAG_KEY, selected);
    }

    private void updateButtonText(Button btn, List<String> selections) {
        if (selections == null || selections.isEmpty()) {
            btn.setText("Select Subtopics");
        } else {
            btn.setText(TextUtils.join(", ", selections));
        }
    }

    private boolean canAddMoreRowsForUnit(String unitName) {
        UnitData unit = getUnitData(unitName);
        if (unit == null) return false;
        int allowed = 0;
        try {
            allowed = Integer.parseInt(unit.hours);
        } catch (NumberFormatException e) {
            allowed = 0;
        }
        int currentRows = 0;
        for (int i = 1; i < topicTable.getChildCount(); i++) {
            TableRow row = (TableRow) topicTable.getChildAt(i);
            Spinner unitSpinner = row.findViewById(R.id.spinnerUnit);
            if (unitSpinner.getSelectedItem().toString().equals(unitName)) {
                currentRows++;
            }
        }
        return currentRows < allowed;
    }

    private String getSelectedUnitFromLastRow() {
        if (topicTable.getChildCount() > 1) {
            TableRow lastRow = (TableRow) topicTable.getChildAt(topicTable.getChildCount() - 1);
            Spinner unitSpinner = lastRow.findViewById(R.id.spinnerUnit);
            return unitSpinner.getSelectedItem().toString();
        }
        return null;
    }

    private JSONArray getTableData() {
        JSONArray data = new JSONArray();
        for (int i = 1; i < topicTable.getChildCount(); i++) {
            TableRow row = (TableRow) topicTable.getChildAt(i);
            JSONObject rowData = new JSONObject();
            try {
                rowData.put("unit", getSpinnerValue(row, R.id.spinnerUnit));
                rowData.put("mainTopic", getSpinnerValue(row, R.id.spinnerMainTopic));
                rowData.put("week", getSpinnerValue(row, R.id.spinnerWeek));
                rowData.put("day", getSpinnerValue(row, R.id.spinnerDay));
                rowData.put("bloom", getSpinnerValue(row, R.id.spinnerBloom));
                rowData.put("co", getSpinnerValue(row, R.id.spinnerCO));
                rowData.put("activity", getSpinnerValue(row, R.id.spinnerActivity));
                List<String> subtopics = (List<String>) row.getTag(SUBTOPIC_TAG_KEY);
                rowData.put("subTopics", subtopics != null ? new JSONArray(subtopics) : new JSONArray());
                data.put(rowData);
            } catch (JSONException e) {
                Log.e(TAG, "Row data collection error", e);
            }
        }
        return data;
    }

    private String getSubjectNameFromArgs() {
        Bundle args = getArguments();
        return args != null ? args.getString("subjectName", "") : "";
    }

    private String getSpinnerValue(TableRow row, int spinnerId) {
        Spinner spinner = row.findViewById(spinnerId);
        return spinner.getSelectedItem() != null ? spinner.getSelectedItem().toString() : "";
    }

    private void setSpinnerSelection(Spinner spinner, String value) {
        if (spinner == null || value == null) return;
        ArrayAdapter adapter = (ArrayAdapter) spinner.getAdapter();
        if (adapter == null) return;
        for (int i = 0; i < adapter.getCount(); i++) {
            if (value.equals(adapter.getItem(i))) {
                spinner.setSelection(i);
                break;
            }
        }
    }

    private UnitData getUnitData(String unitName) {
        for (UnitData ud : unitDataList) {
            if (ud.unit.equals(unitName)) return ud;
        }
        return null;
    }

    private List<String> getAvailableUnits() {
        List<String> units = new ArrayList<>();
        for (UnitData ud : unitDataList) units.add(ud.unit);
        return units;
    }

    private void deleteLastRow() {
        int count = topicTable.getChildCount();
        if (count > 1) {
            topicTable.removeViewAt(count - 1);
        }
    }

    private void showToast(String message) {
        Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show();
    }

    private void toggleEditMode(boolean enable) {
        isEditMode = enable;
        for (int i = 1; i < topicTable.getChildCount(); i++) {
            TableRow row = (TableRow) topicTable.getChildAt(i);
            setRowEditable(row, enable);
            Button btn = row.findViewById(R.id.btnSubtopics);
            btn.setEnabled(enable);
        }
        addRowButton.setEnabled(enable);
        deleteRowButton.setEnabled(enable);
        saveButton.setEnabled(enable);
        editButton.setEnabled(!enable);

        // Only academicYear is editable in edit mode
        setEditTextEnabled(academicYear, enable);
    }

    private void setRowEditable(TableRow row, boolean editable) {
        for (int i = 0; i < row.getChildCount(); i++) {
            View v = row.getChildAt(i);
            v.setEnabled(editable);
        }
    }

    private void savePlan() {
        String subjectName = getSubjectNameFromArgs();
        if (TextUtils.isEmpty(subjectName)) return;
        JSONArray rows = getTableData();
        ParseObject plan = loadedPlan != null ? loadedPlan : new ParseObject("TopicPlan");
        plan.put("subjectName", subjectName);
        plan.put("user", ParseUser.getCurrentUser());
        plan.put("rows", rows);

        // Save academic year
        String year = academicYear.getText().toString();
        plan.put("academicYear", year);

        plan.saveInBackground(e -> {
            if (e == null) {
                showToast("Plan saved successfully");
                loadedPlan = plan;
                toggleEditMode(false);
            } else {
                showToast("Error saving plan: " + e.getMessage());
            }
        });
    }
}
