package com.usj.festaragon.viewmodel

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.work.Data
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.usj.festaragon.model.Event
import com.usj.festaragon.workers.NotificationWorker
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import java.util.concurrent.TimeUnit

class FavoritesViewModel(application: Application) : AndroidViewModel(application) {
    private val _favoriteEvents = MutableLiveData<MutableList<Event>>(mutableListOf())
    val favoriteEvents: LiveData<MutableList<Event>> = _favoriteEvents

    private val workManager = WorkManager.getInstance(application)
    private val sharedPrefs = application.getSharedPreferences("notification_prefs", Context.MODE_PRIVATE)

    fun addFavorite(event: Event) {
        val currentFavorites = _favoriteEvents.value ?: mutableListOf()
        if (!currentFavorites.contains(event)) {
            currentFavorites.add(event)
            _favoriteEvents.value = currentFavorites // Use .value for main thread updates
            if (areNotificationsEnabled()) {
                scheduleNotificationForEvent(event)
            }
        }
    }

    fun removeFavorite(event: Event) {
        val currentFavorites = _favoriteEvents.value ?: mutableListOf()
        if (currentFavorites.remove(event)) {
            _favoriteEvents.value = currentFavorites // Use .value for main thread updates
            cancelNotificationForEvent(event)
        }
    }

    fun isFavorite(event: Event): Boolean {
        return _favoriteEvents.value?.contains(event) ?: false
    }

    fun areNotificationsEnabled(): Boolean {
        return sharedPrefs.getBoolean("notifications_enabled", false)
    }

    fun setNotificationsEnabled(enabled: Boolean) {
        with(sharedPrefs.edit()) {
            putBoolean("notifications_enabled", enabled)
            apply()
        }
        if (enabled) {
            scheduleNotificationsForFavorites(favoriteEvents.value ?: emptyList())
        } else {
            cancelAllFavoriteNotifications()
        }
    }

    private fun scheduleNotificationsForFavorites(events: List<Event>) {
        cancelAllFavoriteNotifications() // Clear old notifications
        events.forEach { scheduleNotificationForEvent(it) }
    }

    private fun scheduleNotificationForEvent(event: Event) {
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val testTodayCalendar = Calendar.getInstance().apply {
            set(2025, Calendar.AUGUST, 10)
        }
        val testTomorrowCalendar = (testTodayCalendar.clone() as Calendar).apply {
            add(Calendar.DAY_OF_YEAR, 1)
        }

        val testTodayStr = sdf.format(testTodayCalendar.time)
        val testTomorrowStr = sdf.format(testTomorrowCalendar.time)

        if (event.date == testTodayStr || event.date == testTomorrowStr) {
            val inputData = Data.Builder()
                .putString("eventTitle", event.title)
                .putString("eventTime", event.startTime)
                .build()

            val notificationWorkRequest = OneTimeWorkRequestBuilder<NotificationWorker>()
                .setInitialDelay(1, TimeUnit.SECONDS) // Fire almost immediately for testing
                .setInputData(inputData)
                .addTag("favorite_notification_${event.id}")
                .build()

            workManager.enqueue(notificationWorkRequest)
        }
    }

    private fun cancelAllFavoriteNotifications() {
        // This cancels all notifications scheduled by this logic
        workManager.cancelAllWorkByTag("favorite_notification_")
    }

    private fun cancelNotificationForEvent(event: Event) {
        workManager.cancelAllWorkByTag("favorite_notification_${event.id}")
    }
}