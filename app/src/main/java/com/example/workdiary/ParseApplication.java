package com.example.workdiary;

import android.app.Application;

import com.parse.Parse;

public class ParseApplication extends Application {
    @Override
    public void onCreate() {
        super.onCreate();

        Parse.initialize(new Parse.Configuration.Builder(this)
                .applicationId("OXoMymVc7PosW9r6JvRKL9K1Gh2XlJ6dbytS1l6G") // Replace with your Back4App App ID
                .clientKey("iYCvs9T8lcJzeezJO7xb4UEijcbnq8h5X8nXj9oU")  // Replace with your Back4App Client Key
                .server("https://parseapi.back4app.com/")
                .build()
        );
    }
}
