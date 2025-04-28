package com.example.workdiary;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.snackbar.Snackbar;
import com.parse.ParseException;
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

        welcomeText = findViewById(R.id.welcomeText);

        startTypingAnimation();

        emailInput = findViewById(R.id.emailInput);
        passwordInput = findViewById(R.id.passwordInput);
        loginButton = findViewById(R.id.loginButton);
        signupButton = findViewById(R.id.signupButton);

        // **Remove this check to prevent auto-login behavior**
        // Check if the user is already logged in
        // if (ParseUser.getCurrentUser() != null) {
        //     navigateToHome();
        // }

        // Handle login button click
        loginButton.setOnClickListener(v -> {
            String email = emailInput.getText().toString().trim();
            String password = passwordInput.getText().toString().trim();

            if (email.isEmpty() || password.isEmpty()) {
                Snackbar.make(v, "Please fill in both fields", Snackbar.LENGTH_SHORT).show();
                return;
            }

            // Verify user credentials with Back4App
            ParseUser.logInInBackground(email, password, (user, e) -> {
                if (e == null && user != null) {
                    // Successful login
                    Snackbar.make(v, "Successfully logged in!", Snackbar.LENGTH_SHORT).show();
                    navigateToHome();
                } else {
                    // Failed login
                    Snackbar.make(v, "Login failed: " + e.getMessage(), Snackbar.LENGTH_LONG).show();
                }
            });
        });

        // Handle signup button click
        signupButton.setOnClickListener(v -> {
            Intent signupIntent = new Intent(MainActivity.this, SignupActivity.class);
            startActivity(signupIntent);
        });
    }

    private void navigateToHome() {
        // Redirect to the home activity after successful login
        Intent intent = new Intent(MainActivity.this, HomeActivity.class);
        startActivity(intent);
        finish();
    }

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
