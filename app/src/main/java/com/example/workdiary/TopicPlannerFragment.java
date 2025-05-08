package com.example.workdiary;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.TableLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.parse.ParseObject;
import com.parse.ParseQuery;
import com.parse.ParseUser;

public class TopicPlannerFragment extends Fragment {

    private static final String TAG = "TopicPlannerFragment";

    private EditText courseTitleEditText, totalHoursEditText, seeMarksEditText,
            semesterEditText, courseCodeEditText, creditsEditText, cieMarksEditText, academicYearEditText;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {

        View rootView = inflater.inflate(R.layout.fragment_topic_planner, container, false);

        // Set faculty name from ParseUser
        TextView facultyNameTextView = rootView.findViewById(R.id.facultyName);
        ParseUser currentUser = ParseUser.getCurrentUser();
        if (currentUser != null) {
            String facultyName = currentUser.getString("name"); // 'name' field in _User class
            if (facultyName != null && !facultyName.isEmpty()) {
                facultyNameTextView.setText("Faculty: " + facultyName);
            } else {
                facultyNameTextView.setText("Faculty: Name not available");
            }
        } else {
            facultyNameTextView.setText("Faculty: Not logged in");
        }

        // Initialize EditTexts
        courseTitleEditText = rootView.findViewById(R.id.courseTitle);
        totalHoursEditText = rootView.findViewById(R.id.totalHours);
        seeMarksEditText = rootView.findViewById(R.id.seeMarks);
        semesterEditText = rootView.findViewById(R.id.semester);
        courseCodeEditText = rootView.findViewById(R.id.courseCode);
        creditsEditText = rootView.findViewById(R.id.credits);
        cieMarksEditText = rootView.findViewById(R.id.cieMarks);
        academicYearEditText = rootView.findViewById(R.id.academicYear); // User input only

        fetchSubjectMetadata();

        // You can also initialize TableLayout, buttons, etc. here as needed

        return rootView;
    }

    private void fetchSubjectMetadata() {
        Bundle args = getArguments();
        if (args == null || !args.containsKey("subjectName")) {
            Toast.makeText(getContext(), "Missing subject name", Toast.LENGTH_SHORT).show();
            Log.e(TAG, "No subjectName passed in arguments");
            return;
        }

        String subjectName = args.getString("subjectName");
        Log.d(TAG, "Fetching metadata for subject: " + subjectName);

        ParseQuery<ParseObject> query = ParseQuery.getQuery("SubjectInfo");
        query.whereEqualTo("subjectName", subjectName);

        query.findInBackground((objects, e) -> {
            if (e == null) {
                if (objects != null && !objects.isEmpty()) {
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
                    // academicYearEditText is for user entry only, do not set
                } else {
                    Log.e(TAG, "No subjects found for: " + subjectName);
                    Toast.makeText(getContext(), "No matching subject found", Toast.LENGTH_LONG).show();
                }
            } else {
                Log.e(TAG, "Error fetching subject: " + e.getMessage());
                Toast.makeText(getContext(), "Failed to fetch subject metadata", Toast.LENGTH_LONG).show();
            }
        });
    }
}
