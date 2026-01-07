package com.usj.festaragon.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.map
import com.usj.festaragon.model.Event
import com.usj.festaragon.model.Multimedia

class EventDetailViewModel(application: Application) : AndroidViewModel(application) {

    private val _currentEvent = MutableLiveData<Event?>()
    val currentEvent: LiveData<Event?> = _currentEvent

    // Derived LiveData for UI display
    val eventTitle: LiveData<String> = _currentEvent.map { it?.title ?: "" }
    val eventCategory: LiveData<String> = _currentEvent.map { it?.categoryName ?: "" }
    val eventDate: LiveData<String> = _currentEvent.map { it?.date ?: "" }
    val eventTimeRange: LiveData<String> = _currentEvent.map { 
        it?.let { "${it.startTime} - ${it.endTime}" } ?: ""
    }
    val eventLocation: LiveData<String> = _currentEvent.map { it?.location ?: "" }
    val eventDescription: LiveData<String> = _currentEvent.map { it?.description ?: "" }
    val eventImageUrl: LiveData<String> = _currentEvent.map { it?.imageUrl ?: "" }
    val eventOrganizerName: LiveData<String> = _currentEvent.map { it?.organizerName ?: "" }
    val eventMultimedia: LiveData<List<Multimedia>> = _currentEvent.map { it?.multimedia ?: emptyList() }
    val eventLatLng: LiveData<Pair<Double, Double>?> = _currentEvent.map { 
        it?.let { Pair(it.latitude, it.longitude) }
    }

    fun setEvent(event: Event?) {
        _currentEvent.value = event
    }

    fun getOrganizerEmail(): String {
        // Generate a generic email based on organizer name
        // In a real app, this would come from the Event model
        val organizerName = _currentEvent.value?.organizerName ?: "organizador"
        return "${organizerName.lowercase().replace(" ", ".")}@festaragon.es"
    }

    fun getEmailSubject(): String {
        val eventTitle = _currentEvent.value?.title ?: "evento"
        return "Consulta sobre: $eventTitle"
    }

    fun getEmailBody(): String {
        val eventTitle = _currentEvent.value?.title ?: "evento"
        return "Hola,\n\nMe gustaría obtener más información sobre el evento $eventTitle.\n\nGracias."
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

    fun getCalendarTitle(): String = _currentEvent.value?.title ?: ""
    
    fun getCalendarDescription(): String = _currentEvent.value?.description ?: ""
    
    fun getCalendarLocation(): String = _currentEvent.value?.location ?: ""
}
