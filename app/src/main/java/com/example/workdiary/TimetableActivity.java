package com.example.workdiary;

import android.graphics.Color;
import android.os.Bundle;
import android.view.View;
import android.widget.*;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import com.parse.ParseObject;
import com.parse.ParseQuery;
import com.parse.ParseUser;
import yuku.ambilwarna.AmbilWarnaDialog;

public class TimetableActivity extends AppCompatActivity {

    private TableLayout tableLayout;
    private Button editButton, saveButton;
    private boolean isEditable = false;
    private ParseObject timetableObject;
    private EditText selectedCell;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_timetable);

        tableLayout = findViewById(R.id.tableLayout);
        editButton = findViewById(R.id.editButton);
        saveButton = findViewById(R.id.saveButton);

        // (Assume your timetable is already built in XML or dynamically)
        setTableEditable(false);
        loadTimetableFromParse();

        editButton.setOnClickListener(v -> {
            isEditable = true;
            setTableEditable(true);
        });

        saveButton.setOnClickListener(v -> {
            if (isEditable) saveTimetableToParse();
        });
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
                        if (editable) {
                            cell.setOnClickListener(v -> {
                                selectedCell = (EditText) cell;
                                showCellOptionsDialog();
                            });
                        } else {
                            cell.setOnClickListener(null);
                        }
                    }
                }
            }
        }
    }

    private void showCellOptionsDialog() {
        String[] options = {"Edit Subject", "Change Text Color", "Change Background Color"};
        new AlertDialog.Builder(this)
                .setTitle("Cell Options")
                .setItems(options, (dialog, which) -> {
                    if (which == 0) {
                        selectedCell.requestFocus();
                        selectedCell.setSelection(selectedCell.getText().length());
                    } else if (which == 1) {
                        openColorPicker(true);
                    } else if (which == 2) {
                        openColorPicker(false);
                    }
                })
                .show();
    }

    private void openColorPicker(boolean forText) {
        int initialColor = forText ? selectedCell.getCurrentTextColor()
                : ((selectedCell.getBackground() instanceof android.graphics.drawable.ColorDrawable)
                ? ((android.graphics.drawable.ColorDrawable) selectedCell.getBackground()).getColor()
                : Color.WHITE);
        new AmbilWarnaDialog(this, initialColor, new AmbilWarnaDialog.OnAmbilWarnaListener() {
            @Override
            public void onOk(AmbilWarnaDialog dialog, int color) {
                if (forText) {
                    selectedCell.setTextColor(color);
                } else {
                    selectedCell.setBackgroundColor(color);
                }
            }
            @Override
            public void onCancel(AmbilWarnaDialog dialog) {}
        }).show();
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
                        EditText et = (EditText) cell;
                        String key = "cell_" + i + "_" + j;
                        timetableObject.put(key, et.getText().toString().trim());
                        timetableObject.put(key + "_textColor", et.getCurrentTextColor());
                        int bgColor = Color.WHITE;
                        if (et.getBackground() instanceof android.graphics.drawable.ColorDrawable) {
                            bgColor = ((android.graphics.drawable.ColorDrawable) et.getBackground()).getColor();
                        }
                        timetableObject.put(key + "_bgColor", bgColor);
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
                                EditText et = (EditText) cell;
                                String key = "cell_" + i + "_" + j;
                                String data = object.getString(key);
                                if (data != null) et.setText(data);
                                int textColor = object.getInt(key + "_textColor");
                                int bgColor = object.getInt(key + "_bgColor");
                                if (textColor != 0) et.setTextColor(textColor);
                                if (bgColor != 0) et.setBackgroundColor(bgColor);
                            }
                        }
                    }
                }
            }
        });
    }
}
