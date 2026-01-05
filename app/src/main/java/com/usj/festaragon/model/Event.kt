package com.usj.festaragon.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class Event(
    val id: String,
    val title: String,
    val startTime: String,
    val endTime: String,
    val location: String,
    var isFavorite: Boolean = false
) : Parcelable