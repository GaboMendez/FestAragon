package com.usj.festaragon.ui

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.provider.MediaStore
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import com.usj.festaragon.R

class ProfileFragment : Fragment() {

    private lateinit var profileImage: ImageView

    companion object {
        private const val PICK_IMAGE_REQUEST = 1
        private const val TAKE_PHOTO_REQUEST = 2
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_profile, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        profileImage = view.findViewById(R.id.profile_image)

        profileImage.setOnClickListener {
            showImagePickerOptions()
        }

        view.findViewById<View>(R.id.logout_button).setOnClickListener {
            showLogoutConfirmationDialog()
        }
    }

    private fun showImagePickerOptions() {
        val options = arrayOf<CharSequence>("Elegir de la galería", "Tomar foto")
        val builder = AlertDialog.Builder(requireContext())
        builder.setTitle("Elige una opción")
        builder.setItems(options) { _, item ->
            when (item) {
                0 -> {
                    val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
                    startActivityForResult(intent, PICK_IMAGE_REQUEST)
                }
                1 -> {
                    val intent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
                    startActivityForResult(intent, TAKE_PHOTO_REQUEST)
                }
            }
        }
        builder.show()
    }

    private fun showLogoutConfirmationDialog() {
        AlertDialog.Builder(requireContext())
            .setTitle("Cerrar sesión")
            .setMessage("¿Estás seguro de que deseas cerrar sesión?")
            .setPositiveButton("Sí") { _, _ ->
                // Handle logout
            }
            .setNegativeButton("No", null)
            .show()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode == Activity.RESULT_OK) {
            when (requestCode) {
                PICK_IMAGE_REQUEST -> {
                    val imageUri = data?.data
                    profileImage.setImageURI(imageUri)
                }
                TAKE_PHOTO_REQUEST -> {
                    val imageBitmap = data?.extras?.get("data") as? android.graphics.Bitmap
                    profileImage.setImageBitmap(imageBitmap)
                }
            }
        }
    }
}