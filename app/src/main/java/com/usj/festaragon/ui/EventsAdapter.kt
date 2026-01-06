package com.usj.festaragon.ui

import android.graphics.drawable.StateListDrawable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import android.widget.ToggleButton
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.usj.festaragon.R
import com.usj.festaragon.model.Event
import com.usj.festaragon.viewmodel.FavoritesViewModel
import java.text.SimpleDateFormat
import java.util.Locale

class EventsAdapter(
    private val events: List<Event>,
    private val favoritesViewModel: FavoritesViewModel
) : RecyclerView.Adapter<EventsAdapter.ViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_evento, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val event = events[position]
        holder.title.text = event.title

        val inputFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val outputFormat = SimpleDateFormat("dd/MM/yy", Locale.getDefault())
        val date = inputFormat.parse(event.date)
        holder.time.text = "${outputFormat.format(date!!)}, ${event.startTime}"

        holder.location.text = event.location

        // Programmatically create and set the selector for the favorite toggle
        val context = holder.itemView.context
        val stateListDrawable = StateListDrawable()
        stateListDrawable.addState(
            intArrayOf(android.R.attr.state_checked),
            ContextCompat.getDrawable(context, android.R.drawable.btn_star_big_on)
        )
        stateListDrawable.addState(
            intArrayOf(),
            ContextCompat.getDrawable(context, android.R.drawable.btn_star_big_off)
        )
        holder.favoriteToggle.background = stateListDrawable

        holder.favoriteToggle.isChecked = favoritesViewModel.isFavorite(event)

        holder.favoriteToggle.setOnClickListener {
            if (holder.favoriteToggle.isChecked) {
                favoritesViewModel.addFavorite(event)
            } else {
                favoritesViewModel.removeFavorite(event)
            }
        }

        // Placeholder image
        holder.image.setImageResource(R.drawable.ic_launcher_background)
    }

    override fun getItemCount() = events.size

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val image: ImageView = view.findViewById(R.id.event_image)
        val title: TextView = view.findViewById(R.id.event_title)
        val time: TextView = view.findViewById(R.id.event_time)
        val location: TextView = view.findViewById(R.id.event_location)
        val favoriteToggle: ToggleButton = view.findViewById(R.id.favorite_toggle)
    }
}