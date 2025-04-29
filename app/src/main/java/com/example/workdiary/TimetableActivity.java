package com.example.workdiary;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.parse.ParseException;
import com.parse.ParseObject;
import com.parse.ParseQuery;
import com.parse.ParseUser;

public class TimetableActivity extends AppCompatActivity {

    private TableLayout tableLayout;
    private Button editButton, saveButton;
    private boolean isEditable = false;
    private ParseObject timetableObject; // Keep reference to avoid creating duplicates

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_timetable);

        tableLayout = findViewById(R.id.tableLayout);
        editButton = findViewById(R.id.editButton);
        saveButton = findViewById(R.id.saveButton);

        // Initially, disable editing
        setTableEditable(false);
        loadTimetableFromParse();

        editButton.setOnClickListener(v -> {
            isEditable = true;
            setTableEditable(true);
        });

        saveButton.setOnClickListener(v -> {
            if (isEditable) {
                saveTimetableToParse();
            }
        });

        // Optional: Back button logic if needed
        findViewById(R.id.backButton).setOnClickListener(v -> finish());
    }

    private void setTableEditable(boolean editable) {
        for (int i = 1; i < tableLayout.getChildCount(); i++) {
            View rowView = tableLayout.getChildAt(i);
            if (rowView instanceof TableRow) {
                TableRow row = (TableRow) rowView;
                for (int j = 1; j < row.getChildCount(); j++) {
                    View cell = row.getChildAt(j);
                    if (cell instanceof EditText) {
                        cell.setEnabled(editable);
                    }
                }
            }
        }
    }

    private void saveTimetableToParse() {
        ParseUser user = ParseUser.getCurrentUser();
        if (user == null) {
            Toast.makeText(this, "User not logged in", Toast.LENGTH_SHORT).show();
            return;
        }

        if (timetableObject == null) {
            timetableObject = new ParseObject("Timetable");
            timetableObject.put("userId", user.getObjectId());
        }

        for (int i = 1; i < tableLayout.getChildCount(); i++) {
            View rowView = tableLayout.getChildAt(i);
            if (rowView instanceof TableRow) {
                TableRow row = (TableRow) rowView;
                for (int j = 1; j < row.getChildCount(); j++) {
                    View cell = row.getChildAt(j);
                    if (cell instanceof EditText) {
                        String key = "cell_" + i + "_" + j;
                        String value = ((EditText) cell).getText().toString().trim();
                        timetableObject.put(key, value);
                    }
                }
            }
        }

        timetableObject.saveInBackground(e -> {
            if (e == null) {
                Toast.makeText(this, "Saved successfully", Toast.LENGTH_SHORT).show();
                setTableEditable(false);
                isEditable = false;
            } else {
                Toast.makeText(this, "Error saving: " + e.getMessage(), Toast.LENGTH_LONG).show();
            }
        });
    }

    private void loadTimetableFromParse() {
        ParseUser user = ParseUser.getCurrentUser();
        if (user == null) return;

        ParseQuery<ParseObject> query = new ParseQuery<>("Timetable");
        query.whereEqualTo("userId", user.getObjectId());
        query.getFirstInBackground((object, e) -> {
            if (e == null && object != null) {
                timetableObject = object;
                for (int i = 1; i < tableLayout.getChildCount(); i++) {
                    View rowView = tableLayout.getChildAt(i);
                    if (rowView instanceof TableRow) {
                        TableRow row = (TableRow) rowView;
                        for (int j = 1; j < row.getChildCount(); j++) {
                            View cell = row.getChildAt(j);
                            if (cell instanceof EditText) {
                                String key = "cell_" + i + "_" + j;
                                String data = object.getString(key);
                                if (data != null) {
                                    ((EditText) cell).setText(data);
                                }
                            }
                        }
                    }
                }
            }
        });
    }
}
