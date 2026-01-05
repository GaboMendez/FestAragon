package com.usj.festaragon.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.appbar.MaterialToolbar
import com.usj.festaragon.R
import com.usj.festaragon.model.Event
import com.usj.festaragon.viewmodel.FavoritesViewModel

class SearchResultsFragment : Fragment() {

    private val favoritesViewModel: FavoritesViewModel by activityViewModels()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_search_results, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val toolbar = view.findViewById<MaterialToolbar>(R.id.toolbar)
        val recyclerView = view.findViewById<RecyclerView>(R.id.search_results_recycler_view)

        toolbar.setNavigationOnClickListener {
            parentFragmentManager.popBackStack()
        }

        val events = arguments?.getParcelableArrayList<Event>("searchResults")?.sortedBy { it.startTime }
        if (events != null) {
            recyclerView.layoutManager = LinearLayoutManager(requireContext())
            recyclerView.adapter = EventsAdapter(events, favoritesViewModel)
        }
    }
}