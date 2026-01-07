package com.usj.festaragon.ui

import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.graphics.drawable.StateListDrawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.GridLayout
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.ToggleButton
import androidx.appcompat.widget.SwitchCompat
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.commit
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.usj.festaragon.R
import com.usj.festaragon.model.Event
import com.usj.festaragon.ui.adapter.EventsAdapter
import com.usj.festaragon.model.Multimedia
import com.usj.festaragon.viewmodel.FavoritesViewModel
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class HomeFragment : Fragment() {

    private val favoritesViewModel: FavoritesViewModel by activityViewModels()
    private lateinit var eventosArray: JSONArray
    private val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    private lateinit var showPastEventsSwitch: SwitchCompat
    private val categoryToggleButtons = mutableListOf<ToggleButton>()
    private var selectedCategoryId: String? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_home, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val searchEditText = view.findViewById<EditText>(R.id.search_edit_text)
        val searchButton = view.findViewById<ImageButton>(R.id.search_button)
        val categoryButtonsContainer = view.findViewById<GridLayout>(R.id.category_buttons_container)
        val dayFilterContainer = view.findViewById<LinearLayout>(R.id.day_filter_container)
        val todayEventsRecyclerView = view.findViewById<RecyclerView>(R.id.today_events_recycler_view)
        showPastEventsSwitch = view.findViewById(R.id.show_past_events_switch)

        val jsonString = requireContext().assets.open("data-pueblo.json").bufferedReader().use { it.readText() }
        val jsonObject = JSONObject(jsonString)
        eventosArray = jsonObject.getJSONArray("eventos")

        // Category buttons
        val categoriasArray = jsonObject.getJSONArray("categorias")
        for (i in 0 until categoriasArray.length()) {
            val categoria = categoriasArray.getJSONObject(i)
            val categoriaNombre = categoria.getString("nombre")
            val categoriaId = categoria.getString("id")

            val toggleButton = ToggleButton(requireContext()).apply {
                textOn = categoriaNombre
                textOff = categoriaNombre
                text = categoriaNombre
                tag = categoriaId
                background = createToggleBackgroundSelector()
                setTextColor(createToggleTextColorSelector())
            }
            categoryToggleButtons.add(toggleButton)

            val params = GridLayout.LayoutParams()
            params.width = 0
            params.columnSpec = GridLayout.spec(GridLayout.UNDEFINED, 1f)
            params.setMargins(8, 8, 8, 8)
            toggleButton.layoutParams = params

            toggleButton.setOnClickListener { clickedButton ->
                if ((clickedButton as ToggleButton).isChecked) {
                    selectedCategoryId = clickedButton.tag as String
                    categoryToggleButtons.forEach { otherButton ->
                        if (otherButton != clickedButton) {
                            otherButton.isChecked = false
                        }
                    }
                } else {
                    selectedCategoryId = null
                }
                // Trigger filtering when category is selected/deselected
                filterEvents(showPastEventsSwitch.isChecked) { evento ->
                    selectedCategoryId == null || evento.getString("categoriaId") == selectedCategoryId
                }
            }
            categoryButtonsContainer.addView(toggleButton)
        }

        // Day filter buttons (logic remains the same, triggers search)
        val firstEventDateString = eventosArray.getJSONObject(0).getString("inicio")
        val parser = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault())
        val calendar = Calendar.getInstance()
        calendar.time = parser.parse(firstEventDateString)!!
        val dayFormat = SimpleDateFormat("dd", Locale.getDefault())
        for (i in 0..4) {
            val button = Button(requireContext()).apply{
                text = dayFormat.format(calendar.time)
                if (i == 0) {
                    setTypeface(null, Typeface.BOLD)
                }
                val params = LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
                layoutParams = params
            }
            val dayString = sdf.format(calendar.time)
            button.setOnClickListener {
                 filterEvents(showPastEventsSwitch.isChecked) { evento ->
                    evento.getString("inicio").substring(0, 10) == dayString
                }
            }
            dayFilterContainer.addView(button)
            calendar.add(Calendar.DAY_OF_YEAR, 1)
        }

        // Today's Events
        val todayEvents = mutableListOf<Event>()
        val todayString = sdf.format(parser.parse(firstEventDateString)!!)
        eventosArray.forEach { evento ->
            if (evento.getString("inicio").substring(0, 10) == todayString) {
                todayEvents.add(createEventFromJsonObject(evento))
            }
        }
        todayEventsRecyclerView.layoutManager = LinearLayoutManager(requireContext())
        todayEventsRecyclerView.adapter = EventsAdapter(todayEvents, favoritesViewModel) { event ->
            navigateToEventDetail(event)
        }

        searchButton.setOnClickListener {
            val searchTerm = searchEditText.text.toString()
            filterEvents(showPastEventsSwitch.isChecked) { evento ->
                val matchesText = searchTerm.isEmpty() || evento.getString("titulo").contains(searchTerm, ignoreCase = true)
                val matchesCategory = selectedCategoryId == null || evento.getString("categoriaId") == selectedCategoryId
                matchesText && matchesCategory
            }
        }
    }
    private fun createToggleBackgroundSelector(): StateListDrawable {
        val stateListDrawable = StateListDrawable()
        val checkedDrawable = GradientDrawable().apply {
            shape = GradientDrawable.RECTANGLE
            setColor(Color.parseColor("#424242"))
            cornerRadius = 20f * resources.displayMetrics.density
        }
        val uncheckedDrawable = GradientDrawable().apply {
            shape = GradientDrawable.RECTANGLE
            setColor(Color.parseColor("#EEEEEE"))
            setStroke( (1f * resources.displayMetrics.density).toInt(), Color.parseColor("#BDBDBD"))
            cornerRadius = 20f * resources.displayMetrics.density
        }
        stateListDrawable.addState(intArrayOf(android.R.attr.state_checked), checkedDrawable)
        stateListDrawable.addState(intArrayOf(), uncheckedDrawable)
        return stateListDrawable
    }

    private fun createToggleTextColorSelector(): ColorStateList {
        val states = arrayOf(
            intArrayOf(android.R.attr.state_checked),
            intArrayOf()
        )
        val colors = intArrayOf(
            Color.WHITE,
            Color.BLACK
        )
        return ColorStateList(states, colors)
    }

    private fun filterEvents(showPast: Boolean, filter: (JSONObject) -> Boolean) {
        val results = mutableListOf<Event>()
        val testDateCalendar = Calendar.getInstance().apply {
            set(2025, Calendar.AUGUST, 10)
        }

        eventosArray.forEach { evento ->
            val eventDate = sdf.parse(evento.getString("inicio").substring(0, 10))
            if (filter(evento)) {
                if (showPast || !eventDate.before(testDateCalendar.time)) {
                    results.add(createEventFromJsonObject(evento))
                }
            }
        }
        navigateToSearchResults(results)
    }

    private fun createEventFromJsonObject(jsonObject: JSONObject): Event {
        // Parse multimedia array
        val multimediaArray = jsonObject.getJSONArray("multimedia")
        val multimediaList = mutableListOf<Multimedia>()
        
        for (i in 0 until multimediaArray.length()) {
            val mediaObj = multimediaArray.getJSONObject(i)
            multimediaList.add(Multimedia(
                type = mediaObj.getString("tipo"),
                resource = mediaObj.getString("recurso")
            ))
        }
        
        // Get first image from array as the main imageUrl
        val imageUrl = multimediaList.firstOrNull { it.type == "imagen" }?.resource ?: ""
        val imageName = imageUrl.substringBeforeLast(".").takeIf { it.isNotEmpty() }

        // Read category and organizer directly from JSON
        val categoryId = jsonObject.getString("categoriaId")
        val organizerName = jsonObject.optString("organizadorNombre", "")
        val organizerContact = jsonObject.optString("organizadorContacto", "")

        return Event(
            id = jsonObject.getString("id"),
            title = jsonObject.getString("titulo"),
            date = jsonObject.getString("inicio").substring(0, 10),
            startTime = jsonObject.getString("inicio").substring(11, 16),
            endTime = jsonObject.getString("fin").substring(11, 16),
            location = jsonObject.getJSONObject("lugar").getString("nombre"),
            description = jsonObject.optString("descripcion"),
            imageUrl = imageUrl,
            imageName = imageName,
            multimedia = multimediaList,
            categoryId = categoryId,
            organizerName = organizerName,
            organizerContact = organizerContact
        )
    }

    private fun navigateToSearchResults(results: List<Event>) {
        parentFragmentManager.commit {
            replace(R.id.fragment_container, SearchResultsFragment().apply {
                arguments = bundleOf("searchResults" to ArrayList(results))
            })
            addToBackStack(null)
        }
    }

    private fun navigateToEventDetail(event: Event) {
        parentFragmentManager.commit {
            replace(R.id.fragment_container, EventDetailFragment().apply {
                arguments = bundleOf("event" to event)
            })
            addToBackStack(null)
        }
    }

    // Extension function to iterate over JSONArray
    fun JSONArray.forEach(action: (JSONObject) -> Unit) {
        for (i in 0 until this.length()) {
            action(this.getJSONObject(i))
        }
    }
}
