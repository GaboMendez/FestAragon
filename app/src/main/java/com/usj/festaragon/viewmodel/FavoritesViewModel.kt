package com.usj.festaragon.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.usj.festaragon.model.Event

class FavoritesViewModel : ViewModel() {
    private val _favoriteEvents = MutableLiveData<MutableList<Event>>(mutableListOf())
    val favoriteEvents: LiveData<MutableList<Event>> = _favoriteEvents

    fun addFavorite(event: Event) {
        val currentFavorites = _favoriteEvents.value
        currentFavorites?.add(event)
        _favoriteEvents.postValue(currentFavorites)
    }

    fun removeFavorite(event: Event) {
        val currentFavorites = _favoriteEvents.value
        currentFavorites?.remove(event)
        _favoriteEvents.postValue(currentFavorites)
    }

    fun isFavorite(event: Event): Boolean {
        return _favoriteEvents.value?.contains(event) ?: false
    }
}