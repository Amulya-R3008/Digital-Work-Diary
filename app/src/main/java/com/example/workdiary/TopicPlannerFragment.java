package com.example.workdiary;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.*;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
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
    private List<UnitMainTopic> unitMainTopicList = new ArrayList<>();
    private boolean unitsLoaded = false;

    private static class UnitMainTopic {
        String unit;
        List<String> mainTopics;
        List<String> topics;
        UnitMainTopic(String unit, List<String> mainTopics, List<String> topics) {
            this.unit = unit;
            this.mainTopics = mainTopics;
            this.topics = topics;
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
            if (!unitsLoaded || unitMainTopicList.isEmpty()) {
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

                JSONArray unitsArray = subject.getJSONArray("units");
                Log.d(TAG, "unitsArray: " + unitsArray); // Debug log
                if (unitsArray != null && unitsArray.length() > 0) {
                    parseUnitsArray(unitsArray);
                    unitsLoaded = true;
                    requireActivity().runOnUiThread(this::addTableRow); // Add the first row automatically
                }
            } else {
                Log.e(TAG, "Error fetching subject: " + (e != null ? e.getMessage() : "No data"));
                Toast.makeText(getContext(), "Failed to fetch subject metadata", Toast.LENGTH_LONG).show();
            }
        });
    }

    // Use this instead of parseUnitsJson
    private void parseUnitsArray(JSONArray unitsArray) {
        unitMainTopicList.clear();
        try {
            for (int i = 0; i < unitsArray.length(); i++) {
                JSONObject unitObj = unitsArray.getJSONObject(i);
                String unit = unitObj.getString("unit");
                List<String> mainTopics = new ArrayList<>();
                Object mainTopicObj = unitObj.get("main topic");
                if (mainTopicObj instanceof JSONArray) {
                    JSONArray mainTopicArray = (JSONArray) mainTopicObj;
                    for (int j = 0; j < mainTopicArray.length(); j++) {
                        mainTopics.add(mainTopicArray.getString(j));
                    }
                } else if (mainTopicObj instanceof String) {
                    mainTopics.add((String) mainTopicObj);
                }
                List<String> topics = new ArrayList<>();
                JSONArray topicArray = unitObj.getJSONArray("topic");
                for (int j = 0; j < topicArray.length(); j++) {
                    topics.add(topicArray.getString(j));
                }
                unitMainTopicList.add(new UnitMainTopic(unit, mainTopics, topics));
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    private void addTableRow() {
        if (getContext() == null || unitMainTopicList.isEmpty()) return;
        LayoutInflater inflater = LayoutInflater.from(getContext());
        TableRow row = (TableRow) inflater.inflate(R.layout.table_row_topic, topicTable, false);

        Spinner weekSpinner = row.findViewById(R.id.spinnerWeek);
        weekSpinner.setAdapter(new ArrayAdapter<>(getContext(), android.R.layout.simple_spinner_item, weekList));

        Spinner daySpinner = row.findViewById(R.id.spinnerDay);
        daySpinner.setAdapter(new ArrayAdapter<>(getContext(), android.R.layout.simple_spinner_item, dayList));

        Spinner unitSpinner = row.findViewById(R.id.spinnerUnit);
        Spinner mainTopicSpinner = row.findViewById(R.id.spinnerMainTopic);
        Spinner subTopicSpinner = row.findViewById(R.id.spinnerSubTopic);

        // Populate unit spinner
        List<String> unitNames = new ArrayList<>();
        for (UnitMainTopic umt : unitMainTopicList) {
            unitNames.add(umt.unit);
        }
        ArrayAdapter<String> unitAdapter = new ArrayAdapter<>(getContext(), android.R.layout.simple_spinner_item, unitNames);
        unitAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        unitSpinner.setAdapter(unitAdapter);

        // Listener: when unit changes, update main topic and sub topic
        unitSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                if (position < 0 || position >= unitMainTopicList.size()) return;
                UnitMainTopic selectedUnit = unitMainTopicList.get(position);

                ArrayAdapter<String> mainTopicAdapter = new ArrayAdapter<>(getContext(), android.R.layout.simple_spinner_item, selectedUnit.mainTopics);
                mainTopicAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                mainTopicSpinner.setAdapter(mainTopicAdapter);

                ArrayAdapter<String> subTopicAdapter = new ArrayAdapter<>(getContext(), android.R.layout.simple_spinner_item, selectedUnit.topics);
                subTopicAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                subTopicSpinner.setAdapter(subTopicAdapter);
            }
            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });

        // Set initial main topic and sub topic for the first unit
        if (!unitMainTopicList.isEmpty()) {
            UnitMainTopic firstUnit = unitMainTopicList.get(0);
            mainTopicSpinner.setAdapter(new ArrayAdapter<>(getContext(), android.R.layout.simple_spinner_item, firstUnit.mainTopics));
            subTopicSpinner.setAdapter(new ArrayAdapter<>(getContext(), android.R.layout.simple_spinner_item, firstUnit.topics));
        }

        Spinner bloomSpinner = row.findViewById(R.id.spinnerBloom);
        bloomSpinner.setAdapter(new ArrayAdapter<>(getContext(), android.R.layout.simple_spinner_item, bloomList));

        Spinner coSpinner = row.findViewById(R.id.spinnerCO);
        coSpinner.setAdapter(new ArrayAdapter<>(getContext(), android.R.layout.simple_spinner_item, coList));

        Spinner activitySpinner = row.findViewById(R.id.spinnerActivity);
        ArrayAdapter<String> activityAdapter = new ArrayAdapter<>(getContext(), android.R.layout.simple_spinner_item, activityList);
        activityAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        activitySpinner.setAdapter(activityAdapter);

        topicTable.addView(row);
    }

    private void deleteLastTableRow() {
        int childCount = topicTable.getChildCount();
        if (childCount > 1) {
            topicTable.removeViewAt(childCount - 1);
        }
    }
}
