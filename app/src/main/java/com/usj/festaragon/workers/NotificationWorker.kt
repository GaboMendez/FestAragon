package com.usj.festaragon.workers

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.work.Worker
import androidx.work.WorkerParameters
import com.usj.festaragon.R

class NotificationWorker(context: Context, workerParams: WorkerParameters) : Worker(context, workerParams) {

    override fun doWork(): Result {
        val eventTitle = inputData.getString("eventTitle") ?: return Result.failure()
        val eventTime = inputData.getString("eventTime") ?: ""

        val notificationManager = applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel("event_notifications", "Event Notifications", NotificationManager.IMPORTANCE_DEFAULT)
            notificationManager.createNotificationChannel(channel)
        }

        val notification = NotificationCompat.Builder(applicationContext, "event_notifications")
            .setContentTitle(eventTitle)
            .setContentText("Empieza a las: $eventTime")
            .setSmallIcon(R.drawable.ic_star)
            .build()

        notificationManager.notify(eventTitle.hashCode(), notification)

        return Result.success()
    }
}