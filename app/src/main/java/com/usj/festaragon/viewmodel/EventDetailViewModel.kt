package com.usj.festaragon.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.usj.festaragon.model.Event

class EventDetailViewModel(application: Application) : AndroidViewModel(application) {

    private val _currentEvent = MutableLiveData<Event?>()
    val currentEvent: LiveData<Event?> = _currentEvent

    fun setEvent(event: Event?) {
        _currentEvent.value = event
    }

    fun getOrganizerEmail(): String {
        // Generate a generic email based on organizer name
        // In a real app, this would come from the Event model
        val organizerName = _currentEvent.value?.organizerName ?: "organizador"
        return "${organizerName.lowercase().replace(" ", ".")}@festaragon.es"
    }

    fun getShareText(): String {
        val event = _currentEvent.value ?: return ""
        return """
            🎉 ${event.title}
            
            📅 ${event.date}
            🕐 ${event.startTime} - ${event.endTime}
            📍 ${event.location}
            
            ${event.description}
            
            #FestAragon #${event.categoryId.replaceFirstChar { it.uppercase() }}
        """.trimIndent()
    }

    fun getDirectionsUrl(): String {
        val event = _currentEvent.value ?: return ""
        return "google.navigation:q=${event.latitude},${event.longitude}"
    }

    fun getMapUrl(): String {
        val event = _currentEvent.value ?: return ""
        return "geo:${event.latitude},${event.longitude}?q=${event.latitude},${event.longitude}(${event.title})"
    }
}
