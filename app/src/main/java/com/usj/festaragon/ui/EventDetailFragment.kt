package com.usj.festaragon.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import com.google.android.material.appbar.MaterialToolbar
import com.usj.festaragon.R
import com.usj.festaragon.model.Event

class EventDetailFragment : Fragment() {

    private var event: Event? = null

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

        val toolbar = view.findViewById<MaterialToolbar>(R.id.toolbar)
        toolbar.setNavigationOnClickListener {
            parentFragmentManager.popBackStack()
        }

        event?.let {
            view.findViewById<TextView>(R.id.event_detail_title).text = it.title
            view.findViewById<TextView>(R.id.event_detail_date).text = "${it.date} (${it.startTime} - ${it.endTime})"
            view.findViewById<TextView>(R.id.event_detail_location).text = it.location
            // Description can be added if available in the model
        }
    }
}
