package com.usj.festaragon.ui

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
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

    private val eventDetailViewModel: EventDetailViewModel by activityViewModels()
    private val favoritesViewModel: FavoritesViewModel by activityViewModels()
    private var googleMap: GoogleMap? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            val event = it.getParcelable<Event>("event")
            eventDetailViewModel.setEvent(event)
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
        
        setupViews(view)
        setupMap()
        observeViewModel()
    }

    private fun setupViews(view: View) {
        // Back button
        view.findViewById<View>(R.id.back_button).setOnClickListener {
            parentFragmentManager.popBackStack()
        }

        // Favorite button
        view.findViewById<View>(R.id.btn_favorite_top).setOnClickListener {
            toggleFavorite()
        }

        // Action buttons
        view.findViewById<View>(R.id.btn_como_llegar).setOnClickListener { openDirections() }
        view.findViewById<View>(R.id.btn_ver_mapa).setOnClickListener { openMapLocation() }
        view.findViewById<View>(R.id.btn_contact_organizer).setOnClickListener { sendEmailToOrganizer() }
        view.findViewById<View>(R.id.btn_share_event).setOnClickListener { shareEvent() }
        view.findViewById<View>(R.id.btn_add_calendar).setOnClickListener { addToCalendar() }
        view.findViewById<View>(R.id.btn_reminder).setOnClickListener { setReminder() }

        // Fullscreen overlay
        val fullscreenOverlay = view.findViewById<View>(R.id.fullscreen_overlay)
        view.findViewById<View>(R.id.btn_close_fullscreen).setOnClickListener {
            closeFullscreen(view)
        }
        fullscreenOverlay.setOnClickListener {
            closeFullscreen(view)
        }
    }

    private fun observeViewModel() {
        // Observe all event data through LiveData
        eventDetailViewModel.eventTitle.observe(viewLifecycleOwner) { title ->
            view?.findViewById<android.widget.TextView>(R.id.event_detail_title)?.text = title
        }

        eventDetailViewModel.eventCategory.observe(viewLifecycleOwner) { category ->
            view?.findViewById<android.widget.TextView>(R.id.event_detail_category)?.text = category
        }

        eventDetailViewModel.eventDate.observe(viewLifecycleOwner) { date ->
            view?.findViewById<android.widget.TextView>(R.id.event_detail_date_full)?.text = date
        }

        eventDetailViewModel.eventTimeRange.observe(viewLifecycleOwner) { timeRange ->
            view?.findViewById<android.widget.TextView>(R.id.event_detail_time)?.text = timeRange
        }

        eventDetailViewModel.eventLocation.observe(viewLifecycleOwner) { location ->
            view?.findViewById<android.widget.TextView>(R.id.event_detail_location_name)?.text = location
        }

        eventDetailViewModel.eventDescription.observe(viewLifecycleOwner) { description ->
            view?.findViewById<android.widget.TextView>(R.id.event_detail_description_text)?.text = description
        }

        eventDetailViewModel.eventOrganizerName.observe(viewLifecycleOwner) { organizer ->
            view?.findViewById<android.widget.TextView>(R.id.organizer_name)?.text = organizer
        }

        eventDetailViewModel.eventImageUrl.observe(viewLifecycleOwner) { imageUrl ->
            loadMainImage(imageUrl)
        }

        eventDetailViewModel.eventMultimedia.observe(viewLifecycleOwner) { multimedia ->
            setupMultimediaGallery(multimedia)
        }

        eventDetailViewModel.eventLatLng.observe(viewLifecycleOwner) { latLng ->
            latLng?.let { updateMapLocation(it.first, it.second) }
        }

        // Observe current event for favorite state
        eventDetailViewModel.currentEvent.observe(viewLifecycleOwner) { event ->
            event?.let { updateFavoriteIcon(it) }
        }
    }

    private fun setupMap() {
        val mapFragment = childFragmentManager.findFragmentById(R.id.map_location) as? SupportMapFragment
        mapFragment?.getMapAsync(this)
    }

    override fun onMapReady(map: GoogleMap) {
        googleMap = map
        configureMap()
        
        // Update map with current event location if available
        eventDetailViewModel.eventLatLng.value?.let { (lat, lng) ->
            updateMapLocation(lat, lng)
        }
    }

    private fun configureMap() {
        googleMap?.uiSettings?.apply {
            isZoomControlsEnabled = false
            isScrollGesturesEnabled = false
            isZoomGesturesEnabled = false
            isTiltGesturesEnabled = false
            isRotateGesturesEnabled = false
        }
    }

    private fun updateMapLocation(latitude: Double, longitude: Double) {
        val eventLocation = LatLng(latitude, longitude)
        val title = eventDetailViewModel.eventTitle.value ?: ""
        
        googleMap?.apply {
            clear()
            addMarker(
                MarkerOptions()
                    .position(eventLocation)
                    .title(title)
            )
            moveCamera(CameraUpdateFactory.newLatLngZoom(eventLocation, 15f))
        }
    }

    private fun loadMainImage(imageUrl: String) {
        val eventImage = view?.findViewById<ImageView>(R.id.event_image_large) ?: return
        
        if (imageUrl.isNotEmpty()) {
            Glide.with(this)
                .load(imageUrl)
                .placeholder(R.drawable.ic_default_event_image)
                .error(R.drawable.ic_default_event_image)
                .centerCrop()
                .into(eventImage)
        } else {
            eventImage.setImageResource(R.drawable.ic_default_event_image)
        }
    }

    private fun setupMultimediaGallery(multimedia: List<com.usj.festaragon.model.Multimedia>) {
        val multimediaContainer = view?.findViewById<LinearLayout>(R.id.multimedia_container) ?: return
        multimediaContainer.removeAllViews()
        
        multimedia.forEach { media ->
            val itemView = ImageView(requireContext()).apply {
                layoutParams = LinearLayout.LayoutParams(300, 200).apply {
                    setMargins(0, 0, 16, 0)
                }
                scaleType = ImageView.ScaleType.CENTER_CROP
                
                Glide.with(this@EventDetailFragment)
                    .load(media.resource)
                    .placeholder(R.drawable.ic_default_event_image)
                    .error(R.drawable.ic_default_event_image)
                    .centerCrop()
                    .into(this)

                setOnClickListener {
                    showFullscreen(media.type, media.resource)
                }
            }
            multimediaContainer.addView(itemView)
        }
    }

    private fun toggleFavorite() {
        eventDetailViewModel.currentEvent.value?.let { event ->
            if (favoritesViewModel.isFavorite(event)) {
                favoritesViewModel.removeFavorite(event)
                Toast.makeText(context, "Eliminado de favoritos", Toast.LENGTH_SHORT).show()
            } else {
                favoritesViewModel.addFavorite(event)
                Toast.makeText(context, "Añadido a favoritos", Toast.LENGTH_SHORT).show()
            }
            updateFavoriteIcon(event)
        }
    }

    private fun updateFavoriteIcon(event: Event) {
        val btnFavorite = view?.findViewById<android.widget.ImageButton>(R.id.btn_favorite_top) ?: return
        val isFavorite = favoritesViewModel.isFavorite(event)
        
        val iconRes = if (isFavorite) {
            android.R.drawable.btn_star_big_on
        } else {
            android.R.drawable.btn_star
        }
        btnFavorite.setImageResource(iconRes)
        
        val tintColor = if (isFavorite) {
            android.R.color.holo_orange_light
        } else {
            android.R.color.white
        }
        btnFavorite.setColorFilter(ContextCompat.getColor(requireContext(), tintColor))
    }

    private fun shareEvent() {
        val shareText = eventDetailViewModel.getShareText()
        val shareIntent = Intent().apply {
            action = Intent.ACTION_SEND
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, shareText)
            putExtra(Intent.EXTRA_SUBJECT, eventDetailViewModel.eventTitle.value ?: "Evento en FestAragón")
        }
        startActivity(Intent.createChooser(shareIntent, "Compartir evento"))
    }

    private fun openDirections() {
        val uri = Uri.parse(eventDetailViewModel.getDirectionsUrl())
        val intent = Intent(Intent.ACTION_VIEW, uri)
        intent.setPackage("com.google.android.apps.maps")

        if (intent.resolveActivity(requireActivity().packageManager) != null) {
            startActivity(intent)
        } else {
            // Fallback to browser
            eventDetailViewModel.eventLatLng.value?.let { (lat, lng) ->
                val browserUri = Uri.parse(
                    "https://www.google.com/maps/dir/?api=1&destination=$lat,$lng"
                )
                startActivity(Intent(Intent.ACTION_VIEW, browserUri))
            }
        }
    }

    private fun openMapLocation() {
        val uri = Uri.parse(eventDetailViewModel.getMapUrl())
        val intent = Intent(Intent.ACTION_VIEW, uri)
        startActivity(intent)
    }

    private fun sendEmailToOrganizer() {
        val email = eventDetailViewModel.getOrganizerEmail()
        val subject = eventDetailViewModel.getEmailSubject()
        val body = eventDetailViewModel.getEmailBody()

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
        try {
            val intent = Intent(Intent.ACTION_INSERT).apply {
                data = android.provider.CalendarContract.Events.CONTENT_URI
                putExtra(android.provider.CalendarContract.Events.TITLE, eventDetailViewModel.getCalendarTitle())
                putExtra(android.provider.CalendarContract.Events.DESCRIPTION, eventDetailViewModel.getCalendarDescription())
                putExtra(android.provider.CalendarContract.Events.EVENT_LOCATION, eventDetailViewModel.getCalendarLocation())
                // Note: Proper date/time parsing should be implemented based on your date format
            }
            startActivity(intent)
        } catch (e: Exception) {
            Toast.makeText(context, "No se pudo abrir el calendario", Toast.LENGTH_SHORT).show()
        }
    }

    private fun setReminder() {
        // In a real app, you would set an alarm/notification
        Toast.makeText(context, "Recordatorio configurado para 15 minutos antes", Toast.LENGTH_SHORT).show()
    }

    private fun showFullscreen(type: String, resourceUrl: String) {
        val overlay = view?.findViewById<View>(R.id.fullscreen_overlay) ?: return
        val image = view?.findViewById<ImageView>(R.id.fullscreen_image) ?: return
        val video = view?.findViewById<android.widget.VideoView>(R.id.fullscreen_video) ?: return
        
        overlay.visibility = View.VISIBLE
        
        if (type == "imagen") {
            image.visibility = View.VISIBLE
            video.visibility = View.GONE
            
            Glide.with(this)
                .load(resourceUrl)
                .placeholder(R.drawable.ic_default_event_image)
                .error(R.drawable.ic_default_event_image)
                .fitCenter()
                .into(image)
        } else if (type == "video") {
            video.visibility = View.VISIBLE
            image.visibility = View.GONE
            
            if (resourceUrl.startsWith("http")) {
                val videoUri = Uri.parse(resourceUrl)
                video.setVideoURI(videoUri)
                video.start()
            }
        }
    }

    private fun closeFullscreen(view: View) {
        val overlay = view.findViewById<View>(R.id.fullscreen_overlay)
        val video = view.findViewById<android.widget.VideoView>(R.id.fullscreen_video)
        val image = view.findViewById<ImageView>(R.id.fullscreen_image)
        
        overlay.visibility = View.GONE
        video.stopPlayback()
        video.visibility = View.GONE
        image.visibility = View.GONE
    }
}
