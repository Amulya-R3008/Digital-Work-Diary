package com.example.workdiary;

import android.Manifest;
import android.app.DownloadManager;
import android.content.ContentValues;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.pdf.PdfDocument;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.view.View;
import android.widget.*;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import com.parse.ParseObject;
import com.parse.ParseQuery;
import com.parse.ParseUser;
import yuku.ambilwarna.AmbilWarnaDialog;
import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;

public class TimetableActivity extends AppCompatActivity {

    private TableLayout tableLayout;
    private Button editButton, saveButton, downloadButton;
    private boolean isEditable = false;
    private ParseObject timetableObject;
    private EditText selectedCell;
    private static final int REQUEST_WRITE_PERMISSION = 1001;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_timetable);

        tableLayout = findViewById(R.id.tableLayout);
        editButton = findViewById(R.id.editButton);
        saveButton = findViewById(R.id.saveButton);
        downloadButton = findViewById(R.id.downloadButton);

        setTableEditable(false);
        loadTimetableFromParse();

        editButton.setOnClickListener(v -> {
            isEditable = true;
            setTableEditable(true);
        });

        saveButton.setOnClickListener(v -> {
            if (isEditable) saveTimetableToParse();
        });

        downloadButton.setOnClickListener(v -> {
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
                if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)
                        != PackageManager.PERMISSION_GRANTED) {
                    ActivityCompat.requestPermissions(this,
                            new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, REQUEST_WRITE_PERMISSION);
                } else {
                    downloadTimetableAsPdf();
                }
            } else {
                downloadTimetableAsPdf();
            }
        });
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_WRITE_PERMISSION) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                downloadTimetableAsPdf();
            } else {
                Toast.makeText(this, "Permission denied to write to storage", Toast.LENGTH_SHORT).show();
            }
        }
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

    // --- PDF DOWNLOAD FUNCTIONALITY ---

    private void downloadTimetableAsPdf() {
        String fileName = "timetable.pdf";
        PdfDocument pdfDocument = new PdfDocument();
        Paint paint = new Paint();
        int pageWidth = 595; // A4 size in points (approx 8.3in * 72)
        int pageHeight = 842; // A4 size in points (approx 11.7in * 72)
        int y = 40;

        PdfDocument.PageInfo pageInfo = new PdfDocument.PageInfo.Builder(pageWidth, pageHeight, 1).create();
        PdfDocument.Page page = pdfDocument.startPage(pageInfo);

        for (int i = 0; i < tableLayout.getChildCount(); i++) {
            View rowView = tableLayout.getChildAt(i);
            if (rowView instanceof TableRow) {
                TableRow row = (TableRow) rowView;
                int x = 20;
                for (int j = 0; j < row.getChildCount(); j++) {
                    View cell = row.getChildAt(j);
                    String cellText = "";
                    if (cell instanceof EditText) {
                        cellText = ((EditText) cell).getText().toString();
                    } else if (cell instanceof TextView) {
                        cellText = ((TextView) cell).getText().toString();
                    }
                    paint.setColor(Color.BLACK);
                    paint.setTextSize(14f);
                    page.getCanvas().drawText(cellText, x, y, paint);
                    x += 100; // Adjust column width as needed
                }
                y += 30; // Adjust row height as needed
                if (y > pageHeight - 40) break; // Avoid overflow
            }
        }
        pdfDocument.finishPage(page);

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                ContentValues values = new ContentValues();
                values.put(MediaStore.Downloads.DISPLAY_NAME, fileName);
                values.put(MediaStore.Downloads.MIME_TYPE, "application/pdf");
                values.put(MediaStore.Downloads.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS);

                Uri uri = getContentResolver().insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values);
                if (uri != null) {
                    OutputStream os = getContentResolver().openOutputStream(uri);
                    if (os != null) {
                        pdfDocument.writeTo(os);
                        os.close();
                        Toast.makeText(this, "Timetable PDF downloaded to Downloads", Toast.LENGTH_LONG).show();
                    }
                }
            } else {
                File downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
                File file = new File(downloadsDir, fileName);
                FileOutputStream fos = new FileOutputStream(file);
                pdfDocument.writeTo(fos);
                fos.close();

                DownloadManager downloadManager = (DownloadManager) getSystemService(Context.DOWNLOAD_SERVICE);
                downloadManager.addCompletedDownload(
                        file.getName(),
                        "Timetable downloaded",
                        true,
                        "application/pdf",
                        file.getAbsolutePath(),
                        file.length(),
                        true
                );
                Toast.makeText(this, "Timetable PDF downloaded to Downloads", Toast.LENGTH_LONG).show();
            }
        } catch (Exception e) {
            Toast.makeText(this, "PDF download failed: " + e.getMessage(), Toast.LENGTH_LONG).show();
        } finally {
            pdfDocument.close();
        }
    }
}
