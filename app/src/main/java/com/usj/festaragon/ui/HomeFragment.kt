package com.usj.festaragon.ui

import android.graphics.Typeface
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.GridLayout
import android.widget.ImageButton
import android.widget.LinearLayout
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
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
                val results = mutableListOf<String>()
                for (j in 0 until eventosArray.length()) {
                    val evento = eventosArray.getJSONObject(j)
                    if (evento.getString("categoriaId") == categoriaId) {
                        results.add(evento.getString("titulo"))
                    }
                }
                Log.d("Search", "Found ${results.size} results for category '$categoriaNombre'")
            }
            categoryButtonsContainer.addView(button)
        }

        // Day filter buttons
        // Se deshabilita la selección a partir del día actual para ajustarse a los datos del JSON.
        // Para volver a la versión original, descomentar la siguiente línea y comentar las posteriores:
        // val calendar = Calendar.getInstance()
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
                val results = mutableListOf<String>()
                for (j in 0 until eventosArray.length()) {
                    val evento = eventosArray.getJSONObject(j)
                    val inicio = evento.getString("inicio").substring(0, 10)
                    if (inicio == dayString) {
                        results.add(evento.getString("titulo"))
                    }
                }
                Log.d("Search", "Found ${results.size} results for day '$dayString'")
            }
            dayFilterContainer.addView(button)
            calendar.add(Calendar.DAY_OF_YEAR, 1)
        }

        // Today's Events
        val todayEvents = mutableListOf<Event>()
        // Se usa la fecha del primer evento como "hoy" para que coincida con los datos de prueba del JSON.
        val todayString = sdf.format(parser.parse(firstEventDateString)!!)
        // Para usar la fecha real del sistema, comenta la línea superior y descomenta la siguiente:
        // val todayString = sdf.format(Calendar.getInstance().time)
        for (i in 0 until eventosArray.length()) {
            val evento = eventosArray.getJSONObject(i)
            val inicio = evento.getString("inicio").substring(0, 10)
            if (inicio == todayString) {
                todayEvents.add(
                    Event(
                        id = evento.getString("id"),
                        title = evento.getString("titulo"),
                        startTime = evento.getString("inicio").substring(11, 16),
                        endTime = evento.getString("fin").substring(11, 16),
                        location = evento.getJSONObject("lugar").getString("nombre")
                    )
                )
            }
        }

        todayEventsRecyclerView.layoutManager = LinearLayoutManager(requireContext())
        todayEventsRecyclerView.adapter = EventsAdapter(todayEvents, favoritesViewModel)

        searchButton.setOnClickListener {
            val searchTerm = searchEditText.text.toString()
            if (searchTerm.isNotEmpty()) {
                val results = mutableListOf<String>()
                for (i in 0 until eventosArray.length()) {
                    val event = eventosArray.getJSONObject(i)
                    val titulo = event.getString("titulo")
                    if (titulo.contains(searchTerm, ignoreCase = true)) {
                        results.add(titulo)
                    }
                }
                Log.d("Search", "Found ${results.size} results for '$searchTerm'")
            }
        }
    }
}
