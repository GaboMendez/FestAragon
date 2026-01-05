package com.usj.festaragon.ui

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.provider.Settings
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
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
    private lateinit var sharedPreferences: SharedPreferences

    // Personal Info Views
    private lateinit var tvName: TextView
    private lateinit var tvEmail: TextView
    private lateinit var tvPhone: TextView
    private lateinit var tvLocation: TextView
    private lateinit var headerName: TextView
    private lateinit var headerEmail: TextView

    companion object {
        private const val PICK_IMAGE_REQUEST = 1
        private const val TAKE_PHOTO_REQUEST = 2
        private const val PREFS_NAME = "UserProfilePrefs"
        private const val KEY_NAME = "user_name"
        private const val KEY_EMAIL = "user_email"
        private const val KEY_PHONE = "user_phone"
        private const val KEY_LOCATION = "user_location"
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

        sharedPreferences = requireContext().getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

        // Initialize Views
        profileImage = view.findViewById(R.id.profile_image)
        locationSwitch = view.findViewById(R.id.switch_location)
        
        tvName = view.findViewById(R.id.value_name)
        tvEmail = view.findViewById(R.id.value_email)
        tvPhone = view.findViewById(R.id.value_phone)
        tvLocation = view.findViewById(R.id.value_location)
        headerName = view.findViewById(R.id.header_name)
        headerEmail = view.findViewById(R.id.header_email)

        // Load Persisted Data
        loadUserData()

        // Set Click Listeners for Editing
        view.findViewById<View>(R.id.row_name).setOnClickListener { showEditDialog("Nombre", KEY_NAME, tvName) }
        view.findViewById<View>(R.id.row_email).setOnClickListener { showEditDialog("Email", KEY_EMAIL, tvEmail) }
        view.findViewById<View>(R.id.row_phone).setOnClickListener { showEditDialog("Teléfono", KEY_PHONE, tvPhone) }
        view.findViewById<View>(R.id.row_location).setOnClickListener { showEditDialog("Ubicación", KEY_LOCATION, tvLocation) }

        profileImage.setOnClickListener {
            showImagePickerOptions()
        }

        view.findViewById<View>(R.id.logout_button).setOnClickListener {
            showLogoutConfirmationDialog()
        }

        val locationRow = view.findViewById<View>(R.id.row_location_permission)
        locationRow.setOnClickListener {
            if (!locationSwitch.isChecked) {
                checkAndRequestLocationPermission()
            } else {
                showRevokePermissionDialog()
            }
        }

        locationSwitch.setOnClickListener {
            if (locationSwitch.isChecked) {
                checkAndRequestLocationPermission()
            } else {
                showRevokePermissionDialog()
            }
        }
        
        updateLocationSwitchState()
    }

    private fun loadUserData() {
        val name = sharedPreferences.getString(KEY_NAME, "María García López")
        val email = sharedPreferences.getString(KEY_EMAIL, "maria.garcia@email.com")
        val phone = sharedPreferences.getString(KEY_PHONE, "+34 612 345 678")
        val location = sharedPreferences.getString(KEY_LOCATION, "Aragón, España")

        tvName.text = name
        tvEmail.text = email
        tvPhone.text = phone
        tvLocation.text = location
        
        // Header usually shows first name or shortened version
        headerName.text = name?.split(" ")?.get(0) ?: "María"
        headerEmail.text = email
    }

    private fun showEditDialog(title: String, key: String, textView: TextView) {
        val builder = AlertDialog.Builder(requireContext())
        builder.setTitle("Editar $title")

        val input = EditText(requireContext())
        input.setText(textView.text)
        builder.setView(input)

        builder.setPositiveButton("Guardar") { _, _ ->
            val newValue = input.text.toString()
            sharedPreferences.edit().putString(key, newValue).apply()
            textView.text = newValue
            
            // Sync header if name or email changed
            if (key == KEY_NAME) headerName.text = newValue.split(" ")[0]
            if (key == KEY_EMAIL) headerEmail.text = newValue
        }
        builder.setNegativeButton("Cancelar", null)
        builder.show()
    }

    override fun onResume() {
        super.onResume()
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
                updateLocationSwitchState()
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