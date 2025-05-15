package com.example.workdiary;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.MenuItem;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.PopupMenu;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.parse.ParseUser;

public class HomeActivity extends AppCompatActivity {

    private ImageView profileIcon;
    private LinearLayout tileTimetable, tileWorkDone, tileTopicsPlanner;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);

        // Ensure user is logged in
        ParseUser currentUser = ParseUser.getCurrentUser();
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

        // Logout menu
        profileIcon.setOnClickListener(view -> {
            PopupMenu popup = new PopupMenu(HomeActivity.this, profileIcon);
            popup.getMenuInflater().inflate(R.menu.profile_menu, popup.getMenu());
            popup.setOnMenuItemClickListener(item -> {
                if (item.getItemId() == R.id.menu_logout) {
                    ParseUser.logOut();
                    Toast.makeText(HomeActivity.this, "Logged out", Toast.LENGTH_SHORT).show();
                    startActivity(new Intent(HomeActivity.this, MainActivity.class));
                    finish();
                    return true;
                }
                return false;
            });
            popup.show();
        });

        tileTimetable.setOnClickListener(v -> {
            Intent intent = new Intent(HomeActivity.this, TimetableActivity.class);
            startActivity(intent);
        });

        tileWorkDone.setOnClickListener(v -> {
            Intent intent = new Intent(HomeActivity.this, WorkDoneActivity.class);
            startActivity(intent);
        });

        tileTopicsPlanner.setOnClickListener(v -> {
            Intent intent = new Intent(HomeActivity.this, TopicsPlannerActivity.class);
            startActivity(intent);
        });
    }
}
