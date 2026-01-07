package com.usj.festaragon.workers

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.work.Worker
import androidx.work.WorkerParameters
import com.usj.festaragon.R

class NotificationWorker(context: Context, workerParams: WorkerParameters) : Worker(context, workerParams) {

    override fun doWork(): Result {
        val eventTitle = inputData.getString("eventTitle") ?: return Result.failure()
        val eventTime = inputData.getString("eventTime") ?: ""

        // Push Notification
        sendPushNotification(eventTitle, eventTime)

        // Email Notification
        checkAndSendEmail(eventTitle, eventTime)

        return Result.success()
    }

    private fun sendPushNotification(title: String, time: String) {
        val notificationManager = applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel("event_notifications", "Event Notifications", NotificationManager.IMPORTANCE_DEFAULT)
            notificationManager.createNotificationChannel(channel)
        }

        val notification = NotificationCompat.Builder(applicationContext, "event_notifications")
            .setContentTitle(title)
            .setContentText("Empieza a las: $time")
            .setSmallIcon(android.R.drawable.ic_popup_reminder)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .build()

        notificationManager.notify(title.hashCode(), notification)
    }

    private fun checkAndSendEmail(title: String, time: String) {
        val sharedPrefs = applicationContext.getSharedPreferences("UserProfilePrefs", Context.MODE_PRIVATE)
        val emailNotificationsEnabled = sharedPrefs.getBoolean("email_notifications_enabled", false)
        
        if (emailNotificationsEnabled) {
            val userEmail = sharedPrefs.getString("user_email", "maria.garcia@email.com") ?: return
            
            // Simulation of sending an email via Intent (as a worker cannot directly send SMTP emails without a backend or specific library)
            // In a real scenario, this would call a backend API.
            // For this project, we can trigger a notification or log that an email would be sent.
            
            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_EMAIL, arrayOf(userEmail))
                putExtra(Intent.EXTRA_SUBJECT, "Recordatorio de Evento: $title")
                putExtra(Intent.EXTRA_TEXT, "Hola, te recordamos que el evento '$title' comenzará pronto, a las $time. ¡No te lo pierdas!")
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            
            // Note: Directly starting an activity from a background worker is generally not recommended or possible in newer Android versions 
            // without special permissions. Usually, you'd call a web service here.
            
            println("Email simulated to $userEmail for event $title")
        }
    }
}
