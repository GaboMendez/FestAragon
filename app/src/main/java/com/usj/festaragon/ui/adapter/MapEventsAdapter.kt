package com.usj.festaragon.ui.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.usj.festaragon.R
import com.usj.festaragon.model.Event
import com.usj.festaragon.viewmodel.FavoritesViewModel
import com.usj.festaragon.viewmodel.MapsViewModel


// Adapter for events list in map view
class MapEventsAdapter(
    private val events: List<Event>,
    private val mapsViewModel: MapsViewModel,
    private val favoritesViewModel: FavoritesViewModel,
    private val onEventClick: (Event) -> Unit,
    private val onDirectionsClick: (Event) -> Unit
) : RecyclerView.Adapter<MapEventsAdapter.ViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_event_map, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val event = events[position]
        holder.bind(event)
    }

    override fun getItemCount() = events.size

    inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        private val title: TextView = view.findViewById(R.id.event_title)
        private val location: TextView = view.findViewById(R.id.event_location)
        private val time: TextView = view.findViewById(R.id.event_time)
        private val favoriteButton: ImageButton = view.findViewById(R.id.favorite_button)
        private val directionsButton: ImageButton = view.findViewById(R.id.directions_button)

        fun bind(event: Event) {
            title.text = event.title

            val distanceStr = mapsViewModel.getDistanceString(event)
            location.text = if (distanceStr.isNotEmpty()) {
                "${event.location} - $distanceStr"
            } else {
                event.location
            }

            time.text = "${event.startTime} - ${event.endTime}"

            // Update favorite state
            val isFavorite = favoritesViewModel.isFavorite(event)
            favoriteButton.setColorFilter(
                ContextCompat.getColor(
                    itemView.context,
                    if (isFavorite) android.R.color.holo_red_light else android.R.color.darker_gray
                )
            )

            favoriteButton.setOnClickListener {
                if (favoritesViewModel.isFavorite(event)) {
                    favoritesViewModel.removeFavorite(event)
                } else {
                    favoritesViewModel.addFavorite(event)
                }
                notifyItemChanged(adapterPosition)
            }

            directionsButton.setOnClickListener {
                onDirectionsClick(event)
            }

            itemView.setOnClickListener {
                onEventClick(event)
            }
        }
    }
}