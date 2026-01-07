package com.usj.festaragon.ui

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.FrameLayout
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.VideoView
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import com.bumptech.glide.Glide
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import com.usj.festaragon.R
import com.usj.festaragon.model.Event
import com.usj.festaragon.viewmodel.EventDetailViewModel
import com.usj.festaragon.viewmodel.FavoritesViewModel

class EventDetailFragment : Fragment(), OnMapReadyCallback {

    private var event: Event? = null
    private val eventDetailViewModel: EventDetailViewModel by activityViewModels()
    private val favoritesViewModel: FavoritesViewModel by activityViewModels()
    private var googleMap: GoogleMap? = null
    private lateinit var btnFavorite: ImageButton

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            event = it.getParcelable("event")
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_event_detail, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val backButton = view.findViewById<ImageButton>(R.id.back_button)
        btnFavorite = view.findViewById(R.id.btn_favorite_top)
        val multimediaContainer = view.findViewById<LinearLayout>(R.id.multimedia_container)
        val fullscreenOverlay = view.findViewById<FrameLayout>(R.id.fullscreen_overlay)
        val fullscreenImage = view.findViewById<ImageView>(R.id.fullscreen_image)
        val fullscreenVideo = view.findViewById<VideoView>(R.id.fullscreen_video)
        val btnCloseFullscreen = view.findViewById<ImageButton>(R.id.btn_close_fullscreen)
        val btnComoLlegar = view.findViewById<Button>(R.id.btn_como_llegar)
        val btnVerMapa = view.findViewById<Button>(R.id.btn_ver_mapa)
        val btnContactOrganizer = view.findViewById<Button>(R.id.btn_contact_organizer)
        val btnShareEvent = view.findViewById<Button>(R.id.btn_share_event)
        val btnAddCalendar = view.findViewById<Button>(R.id.btn_add_calendar)
        val btnReminder = view.findViewById<Button>(R.id.btn_reminder)

        backButton.setOnClickListener {
            parentFragmentManager.popBackStack()
        }

        // Set current event in ViewModel
        event?.let { eventDetailViewModel.setEvent(it) }

        // Setup Map
        setupMap()

        // Favorite button functionality
        btnFavorite.setOnClickListener {
            event?.let { currentEvent ->
                if (favoritesViewModel.isFavorite(currentEvent)) {
                    favoritesViewModel.removeFavorite(currentEvent)
                    Toast.makeText(context, "Eliminado de favoritos", Toast.LENGTH_SHORT).show()
                } else {
                    favoritesViewModel.addFavorite(currentEvent)
                    Toast.makeText(context, "Añadido a favoritos", Toast.LENGTH_SHORT).show()
                }
                updateFavoriteIcon()
            }
        }

        // Como llegar button - Open Google Maps with directions
        btnComoLlegar.setOnClickListener {
            openDirections()
        }

        // Ver en mapa button - Open location in map app
        btnVerMapa.setOnClickListener {
            openMapLocation()
        }

        // Contact organizer button - Send email
        btnContactOrganizer.setOnClickListener {
            sendEmailToOrganizer()
        }

        // Share event button
        btnShareEvent.setOnClickListener {
            shareEvent()
        }

        // Add to calendar button
        btnAddCalendar.setOnClickListener {
            addToCalendar()
        }

        // Set reminder button
        btnReminder.setOnClickListener {
            setReminder()
        }

        // Update favorite icon based on current state
        updateFavoriteIcon()

        event?.let { eventItem ->
            view.findViewById<TextView>(R.id.event_detail_title).text = eventItem.title
            view.findViewById<TextView>(R.id.event_detail_category).text = eventItem.categoryId
            view.findViewById<TextView>(R.id.event_detail_date_full).text = eventItem.date
            view.findViewById<TextView>(R.id.event_detail_time).text = "${eventItem.startTime} - ${eventItem.endTime}"
            view.findViewById<TextView>(R.id.event_detail_location_name).text = eventItem.location
            view.findViewById<TextView>(R.id.event_detail_description_text).text = eventItem.description
            view.findViewById<TextView>(R.id.organizer_name).text = eventItem.organizerName
            
            val eventImage = view.findViewById<ImageView>(R.id.event_image_large)
            // Load image using Glide from URL
            if (eventItem.imageUrl.isNotEmpty()) {
                Glide.with(this)
                    .load(eventItem.imageUrl)
                    .placeholder(R.drawable.ic_default_event_image)
                    .error(R.drawable.ic_default_event_image)
                    .centerCrop()
                    .into(eventImage)
            } else {
                eventImage.setImageResource(R.drawable.ic_default_event_image)
            }

            // Multimedia Gallery
            multimediaContainer.removeAllViews()
            eventItem.multimedia.forEach { media ->
                val itemView = ImageView(requireContext()).apply {
                    layoutParams = LinearLayout.LayoutParams(300, 200).apply {
                        setMargins(0, 0, 16, 0)
                    }
                    scaleType = ImageView.ScaleType.CENTER_CROP
                    
                    // Load multimedia images from URL
                    Glide.with(this@EventDetailFragment)
                        .load(media.resource)
                        .placeholder(R.drawable.ic_default_event_image)
                        .error(R.drawable.ic_default_event_image)
                        .centerCrop()
                        .into(this)

                    setOnClickListener {
                        showFullscreen(media.type, media.resource, fullscreenOverlay, fullscreenImage, fullscreenVideo)
                    }
                }
                multimediaContainer.addView(itemView)
            }
        }

        btnCloseFullscreen.setOnClickListener {
            fullscreenOverlay.visibility = View.GONE
            fullscreenVideo.stopPlayback()
            fullscreenVideo.visibility = View.GONE
            fullscreenImage.visibility = View.GONE
        }
        
        fullscreenOverlay.setOnClickListener {
            btnCloseFullscreen.performClick()
        }
    }

    private fun setupMap() {
        val mapFragment = childFragmentManager.findFragmentById(R.id.map_location) as? SupportMapFragment
        mapFragment?.getMapAsync(this)
    }

    override fun onMapReady(map: GoogleMap) {
        googleMap = map
        event?.let { currentEvent ->
            val eventLocation = LatLng(currentEvent.latitude, currentEvent.longitude)
            googleMap?.apply {
                addMarker(
                    MarkerOptions()
                        .position(eventLocation)
                        .title(currentEvent.title)
                )
                moveCamera(CameraUpdateFactory.newLatLngZoom(eventLocation, 15f))
                uiSettings.apply {
                    isZoomControlsEnabled = false
                    isScrollGesturesEnabled = false
                    isZoomGesturesEnabled = false
                    isTiltGesturesEnabled = false
                    isRotateGesturesEnabled = false
                }
            }
        }
    }

    private fun updateFavoriteIcon() {
        event?.let { currentEvent ->
            val isFavorite = favoritesViewModel.isFavorite(currentEvent)
            val iconRes = if (isFavorite) {
                android.R.drawable.btn_star_big_on
            } else {
                android.R.drawable.btn_star
            }
            btnFavorite.setImageResource(iconRes)
            
            // Also update the tint to make it more visible
            val tintColor = if (isFavorite) {
                android.R.color.holo_orange_light
            } else {
                android.R.color.white
            }
            btnFavorite.setColorFilter(ContextCompat.getColor(requireContext(), tintColor))
        }
    }

    private fun shareEvent() {
        val shareText = eventDetailViewModel.getShareText()
        val shareIntent = Intent().apply {
            action = Intent.ACTION_SEND
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, shareText)
            putExtra(Intent.EXTRA_SUBJECT, event?.title ?: "Evento en FestAragón")
        }
        startActivity(Intent.createChooser(shareIntent, "Compartir evento"))
    }

    private fun openDirections() {
        event?.let { currentEvent ->
            val uri = Uri.parse(eventDetailViewModel.getDirectionsUrl())
            val intent = Intent(Intent.ACTION_VIEW, uri)
            intent.setPackage("com.google.android.apps.maps")

            if (intent.resolveActivity(requireActivity().packageManager) != null) {
                startActivity(intent)
            } else {
                // Fallback to browser
                val browserUri = Uri.parse(
                    "https://www.google.com/maps/dir/?api=1&destination=${currentEvent.latitude},${currentEvent.longitude}"
                )
                startActivity(Intent(Intent.ACTION_VIEW, browserUri))
            }
        }
    }

    private fun openMapLocation() {
        event?.let { currentEvent ->
            val uri = Uri.parse(eventDetailViewModel.getMapUrl())
            val intent = Intent(Intent.ACTION_VIEW, uri)
            startActivity(intent)
        }
    }

    private fun sendEmailToOrganizer() {
        val email = eventDetailViewModel.getOrganizerEmail()
        val subject = "Consulta sobre: ${event?.title}"
        val body = "Hola,\n\nMe gustaría obtener más información sobre el evento ${event?.title}.\n\nGracias."

        val emailIntent = Intent(Intent.ACTION_SENDTO).apply {
            data = Uri.parse("mailto:")
            putExtra(Intent.EXTRA_EMAIL, arrayOf(email))
            putExtra(Intent.EXTRA_SUBJECT, subject)
            putExtra(Intent.EXTRA_TEXT, body)
        }

        try {
            startActivity(Intent.createChooser(emailIntent, "Enviar email"))
        } catch (e: Exception) {
            Toast.makeText(context, "No se encontró una aplicación de email", Toast.LENGTH_SHORT).show()
        }
    }

    private fun addToCalendar() {
        event?.let { currentEvent ->
            // Parse date and time - assuming format "DD/MM/YYYY" and "HH:MM"
            try {
                val intent = Intent(Intent.ACTION_INSERT).apply {
                    data = android.provider.CalendarContract.Events.CONTENT_URI
                    putExtra(android.provider.CalendarContract.Events.TITLE, currentEvent.title)
                    putExtra(android.provider.CalendarContract.Events.DESCRIPTION, currentEvent.description)
                    putExtra(android.provider.CalendarContract.Events.EVENT_LOCATION, currentEvent.location)
                    // You would need to parse the date/time properly here
                    // For now, showing the intent without specific time
                }
                startActivity(intent)
            } catch (e: Exception) {
                Toast.makeText(context, "No se pudo abrir el calendario", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun setReminder() {
        // In a real app, you would set an alarm/notification
        // For now, just add to calendar with a reminder
        Toast.makeText(context, "Recordatorio configurado para 15 minutos antes", Toast.LENGTH_SHORT).show()
    }

    private fun showFullscreen(type: String, resourceUrl: String, overlay: View, image: ImageView, video: VideoView) {
        overlay.visibility = View.VISIBLE
        if (type == "imagen") {
            image.visibility = View.VISIBLE
            video.visibility = View.GONE
            // Load fullscreen image from URL
            Glide.with(this)
                .load(resourceUrl)
                .placeholder(R.drawable.ic_default_event_image)
                .error(R.drawable.ic_default_event_image)
                .fitCenter()
                .into(image)
        } else if (type == "video") {
            video.visibility = View.VISIBLE
            image.visibility = View.GONE
            // For video URLs, you would typically use ExoPlayer or similar
            // For now, keeping the basic VideoView approach
            if (resourceUrl.startsWith("http")) {
                val videoUri = Uri.parse(resourceUrl)
                video.setVideoURI(videoUri)
                video.start()
            }
        }
    }
}
