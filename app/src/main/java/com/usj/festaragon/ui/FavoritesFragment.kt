package com.usj.festaragon.ui

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.widget.SwitchCompat
import androidx.core.content.ContextCompat
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.commit
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.usj.festaragon.R
import com.usj.festaragon.model.Event
import com.usj.festaragon.ui.adapter.EventsAdapter
import com.usj.festaragon.viewmodel.FavoritesViewModel

class FavoritesFragment : Fragment() {

    private val favoritesViewModel: FavoritesViewModel by activityViewModels()

    private val requestPermissionLauncher = registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted: Boolean ->
        if (isGranted) {
            favoritesViewModel.setNotificationsEnabled(true)
        } else {
            Toast.makeText(requireContext(), "Permiso de notificaciones denegado", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_favorites, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val recyclerView = view.findViewById<RecyclerView>(R.id.favorites_recycler_view)
        val notificationsSwitch = view.findViewById<SwitchCompat>(R.id.notifications_switch)
        val emptyStateContainer = view.findViewById<View>(R.id.empty_state_container)
        recyclerView.layoutManager = LinearLayoutManager(requireContext())

        // Observe the unified state
        favoritesViewModel.notificationsEnabled.observe(viewLifecycleOwner) { isEnabled ->
            notificationsSwitch.isChecked = isEnabled
        }

        favoritesViewModel.favoriteEvents.observe(viewLifecycleOwner) { events ->
            recyclerView.adapter = EventsAdapter(events, favoritesViewModel) { event ->
                navigateToEventDetail(event)
            }
            
            // Show empty state when there are no favorites
            if (events.isEmpty()) {
                emptyStateContainer.visibility = View.VISIBLE
                recyclerView.visibility = View.GONE
            } else {
                emptyStateContainer.visibility = View.GONE
                recyclerView.visibility = View.VISIBLE
            }
        }

        notificationsSwitch.setOnClickListener {
            val isChecked = notificationsSwitch.isChecked
            if (isChecked) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED) {
                        favoritesViewModel.setNotificationsEnabled(true)
                    } else {
                        // Revert switch until permission is granted
                        notificationsSwitch.isChecked = false
                        requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                    }
                } else {
                    favoritesViewModel.setNotificationsEnabled(true)
                }
            } else {
                favoritesViewModel.setNotificationsEnabled(false)
            }
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
