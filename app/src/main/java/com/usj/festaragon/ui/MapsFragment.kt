package com.usj.festaragon.ui

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.BitmapDescriptor
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.LatLngBounds
import com.google.android.gms.maps.model.Marker
import com.google.android.gms.maps.model.MarkerOptions
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.usj.festaragon.R
import com.usj.festaragon.model.Event
import com.usj.festaragon.ui.adapter.MapEventsAdapter
import com.usj.festaragon.viewmodel.FavoritesViewModel
import com.usj.festaragon.viewmodel.MapsViewModel

class MapsFragment : Fragment(), OnMapReadyCallback {

    private val mapsViewModel: MapsViewModel by activityViewModels()
    private val favoritesViewModel: FavoritesViewModel by activityViewModels()

    private var googleMap: GoogleMap? = null
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var bottomSheetBehavior: BottomSheetBehavior<View>

    private lateinit var searchEditText: EditText
    private lateinit var categoryChipGroup: ChipGroup
    private lateinit var eventsRecyclerView: RecyclerView
    private lateinit var clearFiltersButton: TextView
    private lateinit var legendContainer: View
    private lateinit var toggleViewButton: TextView

    private val eventMarkers = mutableMapOf<String, Marker>()
    private var isListView = true

    private val locationPermissionRequest = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        when {
            permissions.getOrDefault(Manifest.permission.ACCESS_FINE_LOCATION, false) -> {
                enableMyLocation()
            }
            permissions.getOrDefault(Manifest.permission.ACCESS_COARSE_LOCATION, false) -> {
                enableMyLocation()
            }
            else -> {
                Toast.makeText(context, "Permiso de ubicación denegado", Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_maps, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(requireActivity())

        initViews(view)
        setupMap()
        setupBottomSheet(view)
        setupCategoryChips()
        setupSearch()
        setupZoomControls(view)
        setupObservers()
        requestLocationPermission()
    }

    private fun initViews(view: View) {
        searchEditText = view.findViewById(R.id.search_edit_text)
        categoryChipGroup = view.findViewById(R.id.category_chip_group)
        eventsRecyclerView = view.findViewById(R.id.events_recycler_view)
        clearFiltersButton = view.findViewById(R.id.clear_filters_button)
        legendContainer = view.findViewById(R.id.legend_container)
        toggleViewButton = view.findViewById(R.id.toggle_view_button)

        val myLocationButton = view.findViewById<ImageButton>(R.id.my_location_button)
        myLocationButton.setOnClickListener {
            centerOnUserLocation()
        }

        clearFiltersButton.setOnClickListener {
            mapsViewModel.clearFilters()
            categoryChipGroup.clearCheck()
        }

        toggleViewButton.setOnClickListener {
            isListView = !isListView
            toggleViewButton.text = if (isListView) "Ver Leyenda" else "Ver Lista"
            legendContainer.visibility = if (isListView) View.GONE else View.VISIBLE
            eventsRecyclerView.visibility = if (isListView) View.VISIBLE else View.GONE
        }

        eventsRecyclerView.layoutManager = LinearLayoutManager(context)
    }

    private fun setupMap() {
        val mapFragment = childFragmentManager.findFragmentById(R.id.map_container) as? SupportMapFragment
        mapFragment?.getMapAsync(this)
    }

    private fun setupBottomSheet(view: View) {
        val bottomSheet = view.findViewById<View>(R.id.bottom_sheet)
        bottomSheetBehavior = BottomSheetBehavior.from(bottomSheet)
        bottomSheetBehavior.state = BottomSheetBehavior.STATE_COLLAPSED
        bottomSheetBehavior.peekHeight = 200
    }

    private fun setupCategoryChips() {
        mapsViewModel.categories.observe(viewLifecycleOwner) { categories ->
            categoryChipGroup.removeAllViews()
            categories.forEach { category ->
                val chip = Chip(requireContext()).apply {
                    text = category.name
                    isCheckable = true
                    isCheckedIconVisible = false

                    val iconRes = when (category.id) {
                        "musica" -> R.drawable.ic_music_marker
                        "cultural" -> R.drawable.ic_culture_marker
                        "infantil" -> R.drawable.ic_children_marker
                        "tradicional" -> R.drawable.ic_traditional_marker
                        else -> R.drawable.ic_map
                    }
                    chipIcon = ContextCompat.getDrawable(context, iconRes)
                    isChipIconVisible = true

                }
                categoryChipGroup.addView(chip)
            }
        }
    }

    private fun setupSearch() {
        searchEditText.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                mapsViewModel.searchEvents(s?.toString() ?: "")
            }
        })
    }

    private fun setupZoomControls(view: View) {
        view.findViewById<FloatingActionButton>(R.id.fab_zoom_in).setOnClickListener {
            googleMap?.animateCamera(CameraUpdateFactory.zoomIn())
        }

        view.findViewById<FloatingActionButton>(R.id.fab_zoom_out).setOnClickListener {
            googleMap?.animateCamera(CameraUpdateFactory.zoomOut())
        }

        view.findViewById<FloatingActionButton>(R.id.fab_layers).setOnClickListener {
            toggleMapType()
        }

        view.findViewById<FloatingActionButton>(R.id.fab_filter).setOnClickListener {
            showFilterDialog()
        }
    }

    private fun showFilterDialog() {
        val categories = mapsViewModel.categories.value ?: return
        val categoryNames = categories.map { it.name }.toTypedArray()
        val selectedCategories = mapsViewModel.selectedCategories.value ?: mutableSetOf()
        val checkedItems = categories.map { it.id in selectedCategories }.toBooleanArray()
        
        // Track temporary selections in the dialog - start fresh with current state
        val tempSelectedCategories = mutableSetOf<String>()
        tempSelectedCategories.addAll(selectedCategories)

        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Filtrar por categoría")
            .setMultiChoiceItems(categoryNames, checkedItems) { _, which, isChecked ->
                val categoryId = categories[which].id
                if (isChecked) {
                    tempSelectedCategories.add(categoryId)
                } else {
                    tempSelectedCategories.remove(categoryId)
                }
            }
            .setPositiveButton("Aplicar") { _, _ ->
                // Clear current filters first
                mapsViewModel.clearFilters()

                // Apply only the newly selected filters
                tempSelectedCategories.forEach { categoryId ->
                    mapsViewModel.toggleCategoryFilter(categoryId)
                }
            }
            .setNeutralButton("Limpiar filtros") { _, _ ->
                // Clear all category filters
                mapsViewModel.clearFilters()
            }
            .show()
    }

    private fun toggleMapType() {
        googleMap?.let { map ->
            map.mapType = when (map.mapType) {
                GoogleMap.MAP_TYPE_NORMAL -> GoogleMap.MAP_TYPE_SATELLITE
                GoogleMap.MAP_TYPE_SATELLITE -> GoogleMap.MAP_TYPE_HYBRID
                GoogleMap.MAP_TYPE_HYBRID -> GoogleMap.MAP_TYPE_TERRAIN
                else -> GoogleMap.MAP_TYPE_NORMAL
            }
        }
    }

    private fun setupObservers() {
        mapsViewModel.filteredEvents.observe(viewLifecycleOwner) { events ->
            updateMarkers(events)
            updateEventsList(events)
        }

        mapsViewModel.selectedEvent.observe(viewLifecycleOwner) { event ->
            event?.let {
                centerOnEvent(it)
                highlightMarker(it.id)
            }
        }
    }

    override fun onMapReady(map: GoogleMap) {
        googleMap = map

        map.uiSettings.apply {
            isZoomControlsEnabled = false
            isCompassEnabled = true
            isMyLocationButtonEnabled = false
        }

        // Set initial camera position to pueblo coordinates
        val puebloLocation = LatLng(42.1412, -0.4087)
        map.moveCamera(CameraUpdateFactory.newLatLngZoom(puebloLocation, 15f))

        // Setup marker click listener
        map.setOnMarkerClickListener { marker ->
            val eventId = marker.tag as? String
            val event = mapsViewModel.filteredEvents.value?.find { it.id == eventId }
            event?.let {
                mapsViewModel.selectEvent(it)
                bottomSheetBehavior.state = BottomSheetBehavior.STATE_EXPANDED
            }
            true
        }

        // Load events after map is ready
        mapsViewModel.filteredEvents.value?.let { updateMarkers(it) }

        if (hasLocationPermission()) {
            enableMyLocation()
        }
    }

    private fun updateMarkers(events: List<Event>) {
        googleMap?.let { map ->
            // Clear existing markers
            eventMarkers.values.forEach { it.remove() }
            eventMarkers.clear()

            // Add new markers
            events.forEach { event ->
                val position = LatLng(event.latitude, event.longitude)
                val markerOptions = MarkerOptions()
                    .position(position)
                    .title(event.title)
                    .snippet("${event.location} • ${event.startTime} - ${event.endTime}")
                    .icon(getMarkerIcon(event.categoryId))

                val marker = map.addMarker(markerOptions)
                marker?.tag = event.id
                marker?.let { eventMarkers[event.id] = it }
            }

            // Fit all markers in view if we have events
            if (events.isNotEmpty() && eventMarkers.isNotEmpty()) {
                val boundsBuilder = LatLngBounds.builder()
                events.forEach { event ->
                    boundsBuilder.include(LatLng(event.latitude, event.longitude))
                }
                try {
                    val bounds = boundsBuilder.build()
                    map.animateCamera(CameraUpdateFactory.newLatLngBounds(bounds, 100))
                } catch (e: Exception) {
                    // Fallback to default location if bounds building fails
                }
            }
        }
    }

    private fun getMarkerIcon(categoryId: String): BitmapDescriptor {
        val hue = when (categoryId) {
            "musica" -> BitmapDescriptorFactory.HUE_VIOLET
            "cultural" -> BitmapDescriptorFactory.HUE_BLUE
            "infantil" -> BitmapDescriptorFactory.HUE_GREEN
            "tradicional" -> BitmapDescriptorFactory.HUE_ORANGE
            else -> BitmapDescriptorFactory.HUE_RED
        }
        return BitmapDescriptorFactory.defaultMarker(hue)
    }

    private fun updateEventsList(events: List<Event>) {
        eventsRecyclerView.adapter = MapEventsAdapter(
            events = events,
            mapsViewModel = mapsViewModel,
            favoritesViewModel = favoritesViewModel,
            onEventClick = { event ->
                mapsViewModel.selectEvent(event)
            },
            onDirectionsClick = { event ->
                openDirections(event)
            }
        )
    }

    private fun centerOnEvent(event: Event) {
        googleMap?.animateCamera(
            CameraUpdateFactory.newLatLngZoom(
                LatLng(event.latitude, event.longitude),
                17f
            )
        )
    }

    private fun highlightMarker(eventId: String) {
        eventMarkers[eventId]?.showInfoWindow()
    }

    private fun centerOnUserLocation() {
        if (hasLocationPermission()) {
            try {
                fusedLocationClient.lastLocation.addOnSuccessListener { location ->
                    location?.let {
                        googleMap?.animateCamera(
                            CameraUpdateFactory.newLatLngZoom(
                                LatLng(it.latitude, it.longitude),
                                16f
                            )
                        )
                    }
                }
            } catch (e: SecurityException) {
                e.printStackTrace()
            }
        } else {
            requestLocationPermission()
        }
    }

    private fun openDirections(event: Event) {
        val uri = Uri.parse("google.navigation:q=${event.latitude},${event.longitude}")
        val intent = Intent(Intent.ACTION_VIEW, uri)
        intent.setPackage("com.google.android.apps.maps")

        if (intent.resolveActivity(requireActivity().packageManager) != null) {
            startActivity(intent)
        } else {
            // Fallback to browser
            val browserUri = Uri.parse(
                "https://www.google.com/maps/dir/?api=1&destination=${event.latitude},${event.longitude}"
            )
            startActivity(Intent(Intent.ACTION_VIEW, browserUri))
        }
    }

    private fun hasLocationPermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            requireContext(),
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
    }

    private fun requestLocationPermission() {
        locationPermissionRequest.launch(
            arrayOf(
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION
            )
        )
    }

    private fun enableMyLocation() {
        if (hasLocationPermission()) {
            try {
                googleMap?.isMyLocationEnabled = true
                fusedLocationClient.lastLocation.addOnSuccessListener { location ->
                    location?.let {
                        mapsViewModel.setUserLocation(it)
                    }
                }
            } catch (e: SecurityException) {
                e.printStackTrace()
            }
        }
    }
}