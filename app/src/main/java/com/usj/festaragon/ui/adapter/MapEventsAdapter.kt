package com.usj.festaragon.ui.adapter

import android.graphics.drawable.StateListDrawable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import android.widget.ToggleButton
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
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
        private val image: ImageView = view.findViewById(R.id.event_image)
        private val title: TextView = view.findViewById(R.id.event_title)
        private val location: TextView = view.findViewById(R.id.event_location)
        private val time: TextView = view.findViewById(R.id.event_time)
        private val favoriteButton: ToggleButton = view.findViewById(R.id.favorite_button)
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

            // Load event image
            if (event.imageUrl.isNotEmpty()) {
                Glide.with(itemView.context)
                    .load(event.imageUrl)
                    .placeholder(R.drawable.ic_default_event_image)
                    .error(R.drawable.ic_default_event_image)
                    .centerCrop()
                    .into(image)
            } else {
                image.setImageResource(R.drawable.ic_default_event_image)
            }

            // Set up favorite toggle with star drawable
            val stateListDrawable = StateListDrawable()
            stateListDrawable.addState(
                intArrayOf(android.R.attr.state_checked),
                ContextCompat.getDrawable(itemView.context, android.R.drawable.btn_star_big_on)
            )
            stateListDrawable.addState(
                intArrayOf(),
                ContextCompat.getDrawable(itemView.context, android.R.drawable.btn_star_big_off)
            )
            favoriteButton.background = stateListDrawable

            // Update favorite state
            favoriteButton.isChecked = favoritesViewModel.isFavorite(event)

            favoriteButton.setOnClickListener {
                if (favoriteButton.isChecked) {
                    favoritesViewModel.addFavorite(event)
                    Toast.makeText(itemView.context, "Añadido a favoritos", Toast.LENGTH_SHORT).show()
                } else {
                    favoritesViewModel.removeFavorite(event)
                    Toast.makeText(itemView.context, "Eliminado de favoritos", Toast.LENGTH_SHORT).show()
                }
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