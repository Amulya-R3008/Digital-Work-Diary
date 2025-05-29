package com.example.workdiary;

import android.Manifest;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.PopupMenu;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import com.parse.ParseUser;

public class HomeActivity extends AppCompatActivity {

    private ImageView profileIcon;
    private LinearLayout tileTimetable, tileWorkDone, tileTopicsPlanner;
    private SharedPreferences prefs;
    private static final int[] REMINDER_VALUES = {5, 10, 15, 30, 60};

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);

        TextView welcomeText = findViewById(R.id.welcomeText);

        ParseUser currentUser = ParseUser.getCurrentUser();
        if (currentUser != null) {
            // Fetch the name field from the user object
            String name = currentUser.getString("name");
            if (name != null && !name.isEmpty()) {
                welcomeText.setText("Welcome " + name+"!");
            } else {
                // Fallback to username if name is not set
                String username = currentUser.getUsername();
                welcomeText.setText("Welcome " + username+"!");
            }
        } else {
            welcomeText.setText("Welcome");
        }

        // Ensure user is logged in
        //ParseUser currentUser = ParseUser.getCurrentUser();
        if (currentUser == null) {
            Toast.makeText(this, "Session expired. Please login again.", Toast.LENGTH_LONG).show();
            startActivity(new Intent(this, MainActivity.class));
            finish();
            return;
        }

        profileIcon = findViewById(R.id.profileIcon);
        tileTimetable = findViewById(R.id.tileTimetable);
        tileWorkDone = findViewById(R.id.tileWorkDone);
        tileTopicsPlanner = findViewById(R.id.tileTopicsPlanner);
        prefs = getSharedPreferences("app_prefs", MODE_PRIVATE);

        // Profile menu (notification, reminder, logout)
        profileIcon.setOnClickListener(this::showProfileMenu);

        // Timetable tile click - ensure this is not null
        if (tileTimetable == null) {
            Toast.makeText(this, "Timetable tile not found in layout!", Toast.LENGTH_LONG).show();
        } else {
            tileTimetable.setOnClickListener(v -> {
                Intent intent = new Intent(HomeActivity.this, TimetableActivity.class);
                startActivity(intent);
            });
        }

        // Work Done tile click
        if (tileWorkDone == null) {
            Toast.makeText(this, "Work Done tile not found in layout!", Toast.LENGTH_LONG).show();
        } else {
            tileWorkDone.setOnClickListener(v -> {
                Intent intent = new Intent(HomeActivity.this, WorkDoneActivity.class);
                startActivity(intent);
            });
        }

        // Topics Planner tile click
        if (tileTopicsPlanner == null) {
            Toast.makeText(this, "Topics Planner tile not found in layout!", Toast.LENGTH_LONG).show();
        } else {
            tileTopicsPlanner.setOnClickListener(v -> {
                Intent intent = new Intent(HomeActivity.this, TopicsPlannerActivity.class);
                startActivity(intent);
            });
        }
    }

    private void showProfileMenu(android.view.View view) {
        PopupMenu popup = new PopupMenu(this, view);
        popup.getMenuInflater().inflate(R.menu.profile_menu, popup.getMenu());

        popup.setOnMenuItemClickListener(item -> {
            int id = item.getItemId();
            if (id == R.id.menu_logout) {
                ParseUser.logOut();
                Toast.makeText(this, "Logged out", Toast.LENGTH_SHORT).show();
                startActivity(new Intent(this, MainActivity.class));
                finish();
                return true;
            } else if (id == R.id.menu_notifications) {
                requestNotificationPermission();
                return true;
            } else if (id == R.id.menu_reminder) {
                showReminderDialog();
                return true;
            }
            return false;
        });
        popup.show();
    }

    private void showReminderDialog() {
        final String[] options = {"5 minutes before", "10 minutes before", "15 minutes before", "30 minutes before", "1 hour before"};
        final int[] values = REMINDER_VALUES;
        int saved = prefs.getInt("reminder_minutes", 10);
        int checked = 1; // default to 10 min
        for (int i = 0; i < values.length; i++) if (values[i] == saved) checked = i;

        new AlertDialog.Builder(this)
                .setTitle("Choose Reminder Time")
                .setSingleChoiceItems(options, checked, null)
                .setPositiveButton("OK", (dialog, which) -> {
                    ListView lw = ((AlertDialog) dialog).getListView();
                    int selected = lw.getCheckedItemPosition();
                    prefs.edit().putInt("reminder_minutes", values[selected]).apply();
                    Toast.makeText(this, "Reminders set to " + options[selected], Toast.LENGTH_SHORT).show();
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void requestNotificationPermission() {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                    != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.POST_NOTIFICATIONS}, 100);
            } else {
                Toast.makeText(this, "Notifications already enabled", Toast.LENGTH_SHORT).show();
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == 100) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(this, "Notifications enabled", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, "Notifications permission denied", Toast.LENGTH_SHORT).show();
            }
        }
    }
}
