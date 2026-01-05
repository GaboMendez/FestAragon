package com.usj.festaragon.ui

import android.graphics.Typeface
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.GridLayout
import android.widget.ImageButton
import android.widget.LinearLayout
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.commit
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.usj.festaragon.R
import com.usj.festaragon.model.Event
import com.usj.festaragon.viewmodel.FavoritesViewModel
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class HomeFragment : Fragment() {

    private val favoritesViewModel: FavoritesViewModel by activityViewModels()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_home, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val searchEditText = view.findViewById<EditText>(R.id.search_edit_text)
        val searchButton = view.findViewById<ImageButton>(R.id.search_button)
        val categoryButtonsContainer = view.findViewById<GridLayout>(R.id.category_buttons_container)
        val dayFilterContainer = view.findViewById<LinearLayout>(R.id.day_filter_container)
        val todayEventsRecyclerView = view.findViewById<RecyclerView>(R.id.today_events_recycler_view)

        val jsonString = requireContext().assets.open("data-pueblo.json").bufferedReader().use { it.readText() }
        val jsonObject = JSONObject(jsonString)
        val eventosArray = jsonObject.getJSONArray("eventos")

        // Category buttons
        val categoriasArray = jsonObject.getJSONArray("categorias")
        for (i in 0 until categoriasArray.length()) {
            val categoria = categoriasArray.getJSONObject(i)
            val categoriaNombre = categoria.getString("nombre")
            val categoriaId = categoria.getString("id")

            val button = Button(requireContext())
            button.text = categoriaNombre

            val params = GridLayout.LayoutParams()
            params.width = 0
            params.columnSpec = GridLayout.spec(GridLayout.UNDEFINED, 1f)
            params.setMargins(8, 8, 8, 8)
            button.layoutParams = params

            button.setOnClickListener {
                val results = mutableListOf<Event>()
                for (j in 0 until eventosArray.length()) {
                    val evento = eventosArray.getJSONObject(j)
                    if (evento.getString("categoriaId") == categoriaId) {
                        results.add(createEventFromJsonObject(evento))
                    }
                }
                navigateToSearchResults(results)
            }
            categoryButtonsContainer.addView(button)
        }

        // Day filter buttons
        val firstEventDateString = eventosArray.getJSONObject(0).getString("inicio")
        val parser = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault())
        val calendar = Calendar.getInstance()
        calendar.time = parser.parse(firstEventDateString)!!

        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val dayFormat = SimpleDateFormat("dd", Locale.getDefault())

        for (i in 0..4) {
            val button = Button(requireContext())
            button.text = dayFormat.format(calendar.time)
            if (i == 0) {
                button.setTypeface(null, Typeface.BOLD)
            }

            val params = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT,
                1f
            )
            button.layoutParams = params

            val dayString = sdf.format(calendar.time)
            button.setOnClickListener {
                val results = mutableListOf<Event>()
                for (j in 0 until eventosArray.length()) {
                    val evento = eventosArray.getJSONObject(j)
                    val inicio = evento.getString("inicio").substring(0, 10)
                    if (inicio == dayString) {
                        results.add(createEventFromJsonObject(evento))
                    }
                }
                navigateToSearchResults(results)
            }
            dayFilterContainer.addView(button)
            calendar.add(Calendar.DAY_OF_YEAR, 1)
        }

        // Today's Events
        val todayEvents = mutableListOf<Event>()
        val todayString = sdf.format(parser.parse(firstEventDateString)!!)
        for (i in 0 until eventosArray.length()) {
            val evento = eventosArray.getJSONObject(i)
            val inicio = evento.getString("inicio").substring(0, 10)
            if (inicio == todayString) {
                todayEvents.add(createEventFromJsonObject(evento))
            }
        }

        todayEventsRecyclerView.layoutManager = LinearLayoutManager(requireContext())
        todayEventsRecyclerView.adapter = EventsAdapter(todayEvents, favoritesViewModel) { event ->
            navigateToEventDetail(event)
        }

        searchButton.setOnClickListener {
            val searchTerm = searchEditText.text.toString()
            if (searchTerm.isNotEmpty()) {
                val results = mutableListOf<Event>()
                for (i in 0 until eventosArray.length()) {
                    val evento = eventosArray.getJSONObject(i)
                    if (evento.getString("titulo").contains(searchTerm, ignoreCase = true)) {
                        results.add(createEventFromJsonObject(evento))
                    }
                }
                navigateToSearchResults(results)
            }
        }
    }

    private fun createEventFromJsonObject(jsonObject: JSONObject): Event {
        return Event(
            id = jsonObject.getString("id"),
            title = jsonObject.getString("titulo"),
            date = jsonObject.getString("inicio").substring(0, 10),
            startTime = jsonObject.getString("inicio").substring(11, 16),
            endTime = jsonObject.getString("fin").substring(11, 16),
            location = jsonObject.getJSONObject("lugar").getString("nombre")
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
}
