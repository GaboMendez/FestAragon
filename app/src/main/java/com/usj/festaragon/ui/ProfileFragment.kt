package com.usj.festaragon.ui

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.provider.Settings
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.google.android.material.switchmaterial.SwitchMaterial
import com.usj.festaragon.R

class ProfileFragment : Fragment() {

    private lateinit var profileImage: ImageView
    private lateinit var locationSwitch: SwitchMaterial

    companion object {
        private const val PICK_IMAGE_REQUEST = 1
        private const val TAKE_PHOTO_REQUEST = 2
    }

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val granted = permissions.entries.all { it.value }
        if (granted) {
            locationSwitch.isChecked = true
            Toast.makeText(requireContext(), "Permiso de ubicación concedido", Toast.LENGTH_SHORT).show()
        } else {
            locationSwitch.isChecked = false
            Toast.makeText(requireContext(), "Permiso de ubicación denegado", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_profile, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        profileImage = view.findViewById(R.id.profile_image)
        locationSwitch = view.findViewById(R.id.switch_location)

        profileImage.setOnClickListener {
            showImagePickerOptions()
        }

        view.findViewById<View>(R.id.logout_button).setOnClickListener {
            showLogoutConfirmationDialog()
        }

        val locationRow = view.findViewById<View>(R.id.row_location_permission)
        
        // Handle clicking the entire row
        locationRow.setOnClickListener {
            if (!locationSwitch.isChecked) {
                checkAndRequestLocationPermission()
            } else {
                showRevokePermissionDialog()
            }
        }

        // Handle clicking the switch directly
        locationSwitch.setOnClickListener {
            // Note: By the time onClick triggers, the switch has already toggled its state
            if (locationSwitch.isChecked) {
                checkAndRequestLocationPermission()
            } else {
                showRevokePermissionDialog()
            }
        }
        
        updateLocationSwitchState()
    }

    override fun onResume() {
        super.onResume()
        // Refresh state in case they changed permissions in Settings
        updateLocationSwitchState()
    }

    private fun updateLocationSwitchState() {
        val fineLocation = ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION)
        val coarseLocation = ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_COARSE_LOCATION)
        locationSwitch.isChecked = fineLocation == PackageManager.PERMISSION_GRANTED || coarseLocation == PackageManager.PERMISSION_GRANTED
    }

    private fun checkAndRequestLocationPermission() {
        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            requestPermissionLauncher.launch(arrayOf(
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ))
        } else {
            locationSwitch.isChecked = true
            Toast.makeText(requireContext(), "El permiso ya ha sido concedido", Toast.LENGTH_SHORT).show()
        }
    }

    private fun showRevokePermissionDialog() {
        AlertDialog.Builder(requireContext())
            .setTitle("Revocar permisos")
            .setMessage("Para desactivar completamente el acceso a la ubicación, debes hacerlo desde los ajustes de la aplicación.")
            .setPositiveButton("Ir a Ajustes") { _, _ ->
                val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                val uri = Uri.fromParts("package", requireContext().packageName, null)
                intent.data = uri
                startActivity(intent)
            }
            .setNegativeButton("Cancelar") { _, _ ->
                updateLocationSwitchState() // Restore the toggle to ON
            }
            .show()
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