package com.usj.festaragon.viewmodel

import android.app.Application
import android.location.Location
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.usj.festaragon.data.DataRepository
import com.usj.festaragon.model.Event
import org.json.JSONArray
import org.json.JSONObject

class MapsViewModel(application: Application) : AndroidViewModel(application) {

    private val _events = MutableLiveData<List<Event>>()
    val events: LiveData<List<Event>> = _events

    private val _filteredEvents = MutableLiveData<List<Event>>()
    val filteredEvents: LiveData<List<Event>> = _filteredEvents

    private val _selectedCategories = MutableLiveData<MutableSet<String>>(mutableSetOf())
    val selectedCategories: LiveData<MutableSet<String>> = _selectedCategories

    private val _userLocation = MutableLiveData<Location?>()
    val userLocation: LiveData<Location?> = _userLocation

    private val _selectedEvent = MutableLiveData<Event?>()
    val selectedEvent: LiveData<Event?> = _selectedEvent

    private val _categories = MutableLiveData<List<Category>>()
    val categories: LiveData<List<Category>> = _categories

    private var allEvents: List<Event> = emptyList()

    data class Category(
        val id: String,
        val name: String,
        val icon: String
    )

    init {
        loadEventsFromRepository()
    }

    private fun loadEventsFromRepository() {
        try {
            // Get categories from repository
            val repoCategories = DataRepository.getCategories()
            val categoryList = repoCategories.map { cat ->
                Category(
                    id = cat.id,
                    name = cat.name,
                    icon = cat.icon
                )
            }
            _categories.value = categoryList

            // Get events from repository
            val eventList = DataRepository.getEvents()
            allEvents = eventList
            _events.value = eventList
            _filteredEvents.value = eventList
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun setUserLocation(location: Location?) {
        _userLocation.value = location
        // Re-sort events by distance when location changes
        if (location != null) {
            sortEventsByDistance(location)
        }
    }

    private fun sortEventsByDistance(location: Location) {
        val sorted = (_filteredEvents.value ?: allEvents).sortedBy { event ->
            calculateDistance(location.latitude, location.longitude, event.latitude, event.longitude)
        }
        _filteredEvents.value = sorted
    }

    fun toggleCategoryFilter(categoryId: String) {
        val currentCategories = _selectedCategories.value ?: mutableSetOf()
        if (currentCategories.contains(categoryId)) {
            currentCategories.remove(categoryId)
        } else {
            currentCategories.add(categoryId)
        }
        _selectedCategories.value = currentCategories
        applyFilters()
    }

    fun clearFilters() {
        _selectedCategories.value = mutableSetOf()
        _filteredEvents.value = allEvents
        userLocation.value?.let { sortEventsByDistance(it) }
    }

    private fun applyFilters() {
        val selectedCats = _selectedCategories.value ?: emptySet()
        val filtered = if (selectedCats.isEmpty()) {
            allEvents
        } else {
            allEvents.filter { it.categoryId in selectedCats }
        }
        _filteredEvents.value = filtered
        userLocation.value?.let { sortEventsByDistance(it) }
    }

    fun selectEvent(event: Event?) {
        _selectedEvent.value = event
    }

    fun searchEvents(query: String) {
        if (query.isBlank()) {
            applyFilters()
            return
        }

        val selectedCats = _selectedCategories.value ?: emptySet()
        val filtered = allEvents.filter { event ->
            val matchesQuery = event.title.contains(query, ignoreCase = true) ||
                    event.location.contains(query, ignoreCase = true) ||
                    event.description.contains(query, ignoreCase = true)
            val matchesCategory = selectedCats.isEmpty() || event.categoryId in selectedCats
            matchesQuery && matchesCategory
        }
        _filteredEvents.value = filtered
        userLocation.value?.let { sortEventsByDistance(it) }
    }

    fun calculateDistance(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Float {
        val results = FloatArray(1)
        Location.distanceBetween(lat1, lon1, lat2, lon2, results)
        return results[0]
    }

    fun getDistanceString(event: Event): String {
        val location = userLocation.value ?: return ""
        val distance = calculateDistance(
            location.latitude, location.longitude,
            event.latitude, event.longitude
        )
        return when {
            distance < 1000 -> String.format("%.0f m", distance)
            else -> String.format("%.1f km", distance / 1000)
        }
    }
}
