package com.usj.festaragon.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class Multimedia(
    val type: String,
    val resource: String
) : Parcelable

@Parcelize
data class Event(
    val id: String,
    val title: String,
    val date: String,
    val startTime: String,
    val endTime: String,
    val location: String,
    val description: String? = null,
    val imageName: String? = null,
    val multimedia: List<Multimedia> = emptyList(),
    var isFavorite: Boolean = false
) : Parcelable
