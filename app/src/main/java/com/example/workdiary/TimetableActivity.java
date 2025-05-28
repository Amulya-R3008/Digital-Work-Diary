package com.example.workdiary;

import android.Manifest;
import android.app.AlarmManager;
import android.app.AlarmManager.AlarmClockInfo;
import android.app.DownloadManager;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.pdf.PdfDocument;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.provider.Settings;
import android.telephony.SmsManager;
import android.util.Log;
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
import java.util.Calendar;

public class TimetableActivity extends AppCompatActivity {

    private static final String TAG = "TimetableActivity";
    private TableLayout tableLayout;
    private Button editButton, saveButton, downloadButton;
    private boolean isEditable = false;
    private ParseObject timetableObject;
    private EditText selectedCell;
    private static final int REQUEST_WRITE_PERMISSION = 1001;
    private static final int REQUEST_SMS_PERMISSION = 1002;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_timetable);

        tableLayout = findViewById(R.id.tableLayout);
        editButton = findViewById(R.id.editButton);
        saveButton = findViewById(R.id.saveButton);
        downloadButton = findViewById(R.id.downloadButton);

        checkSmsPermission();
        checkExactAlarmPermission();

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

    private void checkSmsPermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.SEND_SMS)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.SEND_SMS}, REQUEST_SMS_PERMISSION);
        }
    }

    private void checkExactAlarmPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            AlarmManager alarmManager = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
            if (alarmManager != null && !alarmManager.canScheduleExactAlarms()) {
                new AlertDialog.Builder(this)
                        .setTitle("Permission Needed")
                        .setMessage("Please allow 'Alarms & reminders' permission for accurate reminders")
                        .setPositiveButton("Open Settings", (dialog, which) -> {
                            Intent intent = new Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM);
                            intent.setData(Uri.parse("package:" + getPackageName()));
                            startActivity(intent);
                        })
                        .setNegativeButton("Cancel", null)
                        .show();
            }
        }
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
        if (requestCode == REQUEST_SMS_PERMISSION) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(this, "SMS permission granted", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, "SMS permission denied. SMS reminders will not work.", Toast.LENGTH_LONG).show();
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
                scheduleAllReminders();
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
                scheduleAllReminders();
            }
        });
    }

    private void downloadTimetableAsPdf() {
        String fileName = "timetable.pdf";
        PdfDocument pdfDocument = new PdfDocument();
        Paint paint = new Paint();
        int pageWidth = 595;
        int pageHeight = 842;
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
                    x += 100;
                }
                y += 30;
                if (y > pageHeight - 40) break;
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

    // --- REMINDER SCHEDULING ---

    private void scheduleAllReminders() {
        try {
            SharedPreferences prefs = getSharedPreferences("app_prefs", MODE_PRIVATE);
            int reminderMinutes = prefs.getInt("reminder_minutes", 10);

            String userName = ParseUser.getCurrentUser().getUsername();
            String phone = ParseUser.getCurrentUser().getString("phone");

            for (int i = 1; i < tableLayout.getChildCount(); i++) {
                View rowView = tableLayout.getChildAt(i);
                if (rowView instanceof TableRow) {
                    TableRow row = (TableRow) rowView;
                    String day = "Day";
                    View dayCell = row.getChildAt(0);
                    if (dayCell instanceof TextView) {
                        day = ((TextView) dayCell).getText().toString();
                    }
                    for (int j = 1; j < row.getChildCount(); j++) {
                        View cell = row.getChildAt(j);
                        if (cell instanceof EditText) {
                            EditText et = (EditText) cell;
                            String subject = et.getText().toString().trim();
                            if (!subject.isEmpty()) {
                                String periodTime = getPeriodTime(j);
                                Calendar classTime = getNextClassTime(day, periodTime);
                                Calendar reminderTime = (Calendar) classTime.clone();
                                reminderTime.add(Calendar.MINUTE, -reminderMinutes);

                                Log.d(TAG, "Scheduling reminder for " + subject + " at " +
                                        reminderTime.get(Calendar.HOUR_OF_DAY) + ":" +
                                        String.format("%02d", reminderTime.get(Calendar.MINUTE)) +
                                        " on " + day);

                                scheduleReminder(this, reminderTime, subject, periodTime, userName, phone);
                            }
                        }
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(this, "Error scheduling reminders: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    // --- Period mapping matches your screenshot exactly ---
    private String getPeriodTime(int columnIndex) {
        String[] times = {
                "09:00", // 1: 9:00-10:00
                "10:00", // 2: 10:00-11:00
                "11:00", // 3: 11:00-11:30
                "11:30", // 4: 11:30-12:30
                "12:30", // 5: 12:30-1:30
                "13:30", // 6: 1:30-2:30
                "14:30", // 7: 2:30-3:30
                "15:30"  // 8: 3:30-4:30
        };
        int periodIdx = columnIndex - 1;
        if (periodIdx >= 0 && periodIdx < times.length) return times[periodIdx];
        return "";
    }

    private Calendar getNextClassTime(String day, String time) {
        Calendar now = Calendar.getInstance();
        Calendar classTime = (Calendar) now.clone();
        String[] days = {"Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday"};
        int dayOfWeek = Calendar.MONDAY; // default
        for (int i = 0; i < days.length; i++) {
            if (days[i].equalsIgnoreCase(day)) {
                dayOfWeek = Calendar.MONDAY + i;
                break;
            }
        }
        classTime.set(Calendar.DAY_OF_WEEK, dayOfWeek);

        try {
            String[] parts = time.split(":");
            int hour = Integer.parseInt(parts[0]);
            int min = Integer.parseInt(parts[1]);
            classTime.set(Calendar.HOUR_OF_DAY, hour);
            classTime.set(Calendar.MINUTE, min);
            classTime.set(Calendar.SECOND, 0);
            classTime.set(Calendar.MILLISECOND, 0);
            if (classTime.before(now)) classTime.add(Calendar.DAY_OF_YEAR, 7);
        } catch (Exception e) {
            Log.e(TAG, "Error parsing time: " + time, e);
        }
        return classTime;
    }

    public void scheduleReminder(Context context, Calendar reminderTime, String subject, String classTime, String userName, String phone) {
        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (!alarmManager.canScheduleExactAlarms()) {
                Toast.makeText(context, "Please allow 'Alarms & reminders' permission for reminders.", Toast.LENGTH_LONG).show();
                Intent intent = new Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM);
                intent.setData(Uri.parse("package:" + context.getPackageName()));
                context.startActivity(intent);
                return;
            }
        }

        Intent intent = new Intent(context, ReminderReceiver.class);
        intent.putExtra("subject", subject);
        intent.putExtra("time", classTime);
        intent.putExtra("userName", userName);
        intent.putExtra("phone", phone);

        int requestCode = (int) reminderTime.getTimeInMillis();
        PendingIntent pendingIntent = PendingIntent.getBroadcast(
                context,
                requestCode,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                AlarmClockInfo acInfo = new AlarmClockInfo(reminderTime.getTimeInMillis(), pendingIntent);
                alarmManager.setAlarmClock(acInfo, pendingIntent);
                Log.d(TAG, "Using setAlarmClock for exact timing");
            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, reminderTime.getTimeInMillis(), pendingIntent);
            } else {
                alarmManager.setExact(AlarmManager.RTC_WAKEUP, reminderTime.getTimeInMillis(), pendingIntent);
            }
            Toast.makeText(context, "Reminder set for: " + reminderTime.getTime(), Toast.LENGTH_SHORT).show();
        } catch (SecurityException e) {
            Log.e(TAG, "Security exception - permission denied");
            Toast.makeText(context, "Cannot schedule reminders - check permissions", Toast.LENGTH_LONG).show();
        }
    }
}
