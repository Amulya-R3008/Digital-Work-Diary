package com.example.workdiary;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.snackbar.Snackbar;
import com.parse.ParseException;
import com.parse.ParseUser;
import com.parse.SignUpCallback;

import java.util.regex.Pattern;

public class SignupActivity extends AppCompatActivity {
    private EditText nameInput, emailInput, phoneInput, passwordInput, confirmPasswordInput;
    private Button signupButton,cancel;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_signup);

        // Log out any existing user session
        if (ParseUser.getCurrentUser() != null) {
            ParseUser.logOut();
        }

        nameInput = findViewById(R.id.inputName);
        emailInput = findViewById(R.id.inputEmail);
        phoneInput = findViewById(R.id.inputPhone);
        passwordInput = findViewById(R.id.inputPassword);
        confirmPasswordInput = findViewById(R.id.inputConfirmPassword);
        signupButton = findViewById(R.id.btnSignup);
        cancel=findViewById(R.id.btnCancel);

        cancel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(SignupActivity.this, MainActivity.class);
                startActivity(intent);
                finish();
            }
        });

        signupButton.setOnClickListener(v -> {
            String name = nameInput.getText().toString().trim();
            String email = emailInput.getText().toString().trim();
            String phone = phoneInput.getText().toString().trim();
            String password = passwordInput.getText().toString().trim();
            String confirmPassword = confirmPasswordInput.getText().toString().trim();

            // Validation checks
            if (name.isEmpty() || email.isEmpty() || phone.isEmpty() || password.isEmpty() || confirmPassword.isEmpty()) {
                Snackbar.make(v, "Please fill in all fields", Snackbar.LENGTH_SHORT).show();
                return;
            }

            // Validate Name (minimum 2 characters)
            if (name.length() < 2) {
                Snackbar.make(v, "Name should have at least 2 characters", Snackbar.LENGTH_SHORT).show();
                return;
            }

            // Validate Email (should be a valid RVCE email)
            if (!isValidEmail(email)) {
                Snackbar.make(v, "Email should be a valid RVCE email (@rvce.edu.in)", Snackbar.LENGTH_SHORT).show();
                return;
            }

            // Validate Phone Number (should be exactly 10 digits)
            if (!isValidPhoneNumber(phone)) {
                Snackbar.make(v, "Phone number should be exactly 10 digits", Snackbar.LENGTH_SHORT).show();
                return;
            }

            // Validate Password (at least 8 characters, one special character, one number, and both cases)
            if (!isValidPassword(password)) {
                Snackbar.make(v, "Password should be at least 8 characters and contain one special character, one number, and both upper and lower case letters", Snackbar.LENGTH_LONG).show();
                return;
            }

            // Ensure passwords match
            if (!password.equals(confirmPassword)) {
                Snackbar.make(v, "Passwords do not match", Snackbar.LENGTH_SHORT).show();
                return;
            }

            // Create new ParseUser and sign up
            ParseUser user = new ParseUser();
            user.setUsername(email);
            user.setEmail(email);
            user.setPassword(password);
            user.put("phone", phone);
            user.put("name", name);

            user.signUpInBackground(new SignUpCallback() {
                @Override
                public void done(ParseException e) {
                    if (e == null) {
                        // Sign up successful
                        Snackbar.make(v, "Signup successful! Please log in.", Snackbar.LENGTH_SHORT).show();
                        Intent intent = new Intent(SignupActivity.this, MainActivity.class);
                        startActivity(intent);
                        finish();
                    } else {
                        // Error occurred
                        Snackbar.make(v, "Sign-up failed: " + e.getMessage(), Snackbar.LENGTH_LONG).show();
                    }
                }
            });
        });
    }

    // Method to validate email (RVCE email)
    private boolean isValidEmail(String email) {
        return email != null && email.matches("^[a-zA-Z0-9._%+-]+@rvce\\.edu\\.in$");
    }

    // Method to validate phone number (10 digits)
    private boolean isValidPhoneNumber(String phone) {
        return phone != null && phone.matches("\\d{10}");
    }

    // Method to validate password (at least 8 characters, one special character, one number, and both uppercase and lowercase letters)
    private boolean isValidPassword(String password) {
        return password != null && password.length() >= 8 &&
                Pattern.compile("[A-Z]").matcher(password).find() && // Contains an uppercase letter
                Pattern.compile("[a-z]").matcher(password).find() && // Contains a lowercase letter
                Pattern.compile("[0-9]").matcher(password).find() && // Contains a number
                Pattern.compile("[!@#\\$%\\^&\\*]").matcher(password).find(); // Contains a special character
    }
}
