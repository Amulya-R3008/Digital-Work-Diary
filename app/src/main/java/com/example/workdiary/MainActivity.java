package com.example.workdiary;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.text.InputType;
import android.widget.Button;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.snackbar.Snackbar;
import com.parse.ParseUser;

public class MainActivity extends AppCompatActivity {

    private TextView emailInput, passwordInput;
    private Button loginButton;
    private TextView signupButton;
    private TextView welcomeText;
    private Handler handler = new Handler();
    private String fullText = "Welcome";
    private int index = 0;
    private long delay = 150; // milliseconds between each character

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Initialize UI elements
        welcomeText = findViewById(R.id.welcomeText);
        emailInput = findViewById(R.id.emailInput);
        passwordInput = findViewById(R.id.passwordInput);
        loginButton = findViewById(R.id.loginButton);
        signupButton = findViewById(R.id.signupButton);

        // Set password field to password type
        passwordInput.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);

        // Start the welcome typing animation
        startTypingAnimation();

        // Handle login button click
        loginButton.setOnClickListener(v -> {
            String email = emailInput.getText().toString().trim();
            String password = passwordInput.getText().toString().trim();

            if (email.isEmpty() || password.isEmpty()) {
                Snackbar.make(v, "Please fill in both fields", Snackbar.LENGTH_SHORT).show();
                return;
            }

            // Verify user credentials with Back4App (Parse)
            ParseUser.logInInBackground(email, password, (user, e) -> {
                if (e == null && user != null) {
                    Snackbar.make(v, "Successfully logged in!", Snackbar.LENGTH_SHORT).show();
                    // Check for admin email and navigate accordingly
                    if (email.equalsIgnoreCase("admin@rvce.edu.in")) {
                        navigateToAdminDashboard();
                    } else {
                        navigateToHome();
                    }
                } else {
                    Snackbar.make(v, "Login failed: " + (e != null ? e.getMessage() : "Unknown error"), Snackbar.LENGTH_LONG).show();
                }
            });
        });

        // Handle signup button click
        signupButton.setOnClickListener(v -> {
            Intent signupIntent = new Intent(MainActivity.this, SignupActivity.class);
            startActivity(signupIntent);
        });
    }

    // Navigate to HomeActivity for regular users
    private void navigateToHome() {
        Intent intent = new Intent(MainActivity.this, HomeActivity.class);
        startActivity(intent);
        finish();
    }

    // Navigate to AdminDashboardActivity for admin
    private void navigateToAdminDashboard() {
        Intent intent = new Intent(MainActivity.this, Admin_dashboard.class);
        startActivity(intent);
        finish();
    }

    // Typing animation for welcome text
    private void startTypingAnimation() {
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                if (index <= fullText.length()) {
                    welcomeText.setText(fullText.substring(0, index));
                    index++;
                    handler.postDelayed(this, delay);
                }
            }
        }, delay);
    }
}
