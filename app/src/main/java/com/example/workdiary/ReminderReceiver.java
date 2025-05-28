package com.example.workdiary;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.telephony.SmsManager;
import android.util.Log;
import android.widget.Toast;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.core.content.ContextCompat;
import android.content.pm.PackageManager;

public class ReminderReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        String subject = intent.getStringExtra("subject");
        String time = intent.getStringExtra("time");
        String userName = intent.getStringExtra("userName");
        String phone = intent.getStringExtra("phone");

        // Notification block
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    "reminder_channel", "Reminders", NotificationManager.IMPORTANCE_HIGH);
            NotificationManager manager = context.getSystemService(NotificationManager.class);
            if (manager != null) manager.createNotificationChannel(channel);
        }
        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, "reminder_channel")
                .setSmallIcon(android.R.drawable.ic_dialog_info)
                .setContentTitle("Class Reminder")
                .setContentText("Hi " + userName + ", you have " + subject + " at " + time)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true);
        NotificationManagerCompat.from(context).notify((int) System.currentTimeMillis(), builder.build());

        // SMS block (with permission check and SecurityException handling)
        if (phone != null && !phone.isEmpty()) {
            if (ContextCompat.checkSelfPermission(context, android.Manifest.permission.SEND_SMS)
                    == PackageManager.PERMISSION_GRANTED) {
                try {
                    SmsManager smsManager = SmsManager.getDefault();
                    String message = "Hi " + userName + ", reminder: " + subject + " at " + time;
                    smsManager.sendTextMessage(phone, null, message, null, null);
                    Log.d("Reminders", "SMS sent to: " + phone);
                    Toast.makeText(context, "SMS sent to: " + phone, Toast.LENGTH_LONG).show();
                } catch (SecurityException e) {
                    Log.e("Reminders", "SMS SecurityException: " + e.getMessage());
                    Toast.makeText(context, "SMS failed: permission denied.", Toast.LENGTH_LONG).show();
                } catch (Exception e) {
                    Log.e("Reminders", "SMS failed: " + e.getMessage());
                    Toast.makeText(context, "SMS failed: " + e.getMessage(), Toast.LENGTH_LONG).show();
                }
            } else {
                Toast.makeText(context, "SMS permission not granted. Cannot send SMS reminder.", Toast.LENGTH_LONG).show();
                Log.e("Reminders", "SMS permission not granted.");
            }
        } else {
            Log.d("Reminders", "No phone number provided for SMS.");
        }
    }
}
