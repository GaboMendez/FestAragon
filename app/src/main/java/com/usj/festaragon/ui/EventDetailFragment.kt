package com.usj.festaragon.ui

import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.VideoView
import androidx.fragment.app.Fragment
import com.bumptech.glide.Glide
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
        val multimediaContainer = view.findViewById<LinearLayout>(R.id.multimedia_container)
        val fullscreenOverlay = view.findViewById<FrameLayout>(R.id.fullscreen_overlay)
        val fullscreenImage = view.findViewById<ImageView>(R.id.fullscreen_image)
        val fullscreenVideo = view.findViewById<VideoView>(R.id.fullscreen_video)
        val btnCloseFullscreen = view.findViewById<ImageButton>(R.id.btn_close_fullscreen)

        toolbar.setNavigationOnClickListener {
            parentFragmentManager.popBackStack()
        }

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
