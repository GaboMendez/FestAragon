package com.usj.festaragon.model

data class Event(
    val id: String,
    val title: String,
    val startTime: String,
    val endTime: String,
    val location: String,
    var isFavorite: Boolean = false
)