package com.usj.festaragon.ui

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Build
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
import androidx.fragment.app.activityViewModels
import com.google.android.material.switchmaterial.SwitchMaterial
import com.usj.festaragon.R
import com.usj.festaragon.viewmodel.FavoritesViewModel
import java.io.File
import java.io.FileOutputStream

class ProfileFragment : Fragment() {

    private lateinit var profileImage: ImageView
    private lateinit var locationSwitch: SwitchMaterial
    private lateinit var cameraSwitch: SwitchMaterial
    private lateinit var eventRemindersSwitch: SwitchMaterial
    private lateinit var emailNotificationsSwitch: SwitchMaterial
    private lateinit var pushNotificationsSwitch: SwitchMaterial
    private lateinit var sharedPreferences: SharedPreferences
    
    private val favoritesViewModel: FavoritesViewModel by activityViewModels()

    // Personal Info Views
    private lateinit var tvName: TextView
    private lateinit var tvEmail: TextView
    private lateinit var tvPhone: TextView
    private lateinit var tvLocation: TextView
    private lateinit var headerName: TextView
    private lateinit var headerEmail: TextView
    private lateinit var headerPhone: TextView
    
    // Notification Views
    private lateinit var noticeTimeValue: TextView
    private lateinit var eventRemindersSubtitle: TextView

    companion object {
        private const val PICK_IMAGE_REQUEST = 1
        private const val TAKE_PHOTO_REQUEST = 2
        private const val PREFS_NAME = "UserProfilePrefs"
        private const val KEY_NAME = "user_name"
        private const val KEY_EMAIL = "user_email"
        private const val KEY_PHONE = "user_phone"
        private const val KEY_LOCATION = "user_location"
        private const val KEY_IMAGE_PATH = "user_image_path"
        private const val KEY_EMAIL_NOTIF = "email_notifications_enabled"
        private const val KEY_PUSH_NOTIF = "push_notifications_enabled"
    }

    private val locationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val granted = permissions.entries.all { it.value }
        locationSwitch.isChecked = granted
        val msg = if (granted) "Permiso de ubicación concedido" else "Permiso de ubicación denegado"
        Toast.makeText(requireContext(), msg, Toast.LENGTH_SHORT).show()
    }

    private val cameraPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        cameraSwitch.isChecked = isGranted
        val msg = if (isGranted) "Permiso de cámara concedido" else "Permiso de cámara denegado"
        Toast.makeText(requireContext(), msg, Toast.LENGTH_SHORT).show()
    }
    
    private val notificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
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
        return inflater.inflate(R.layout.fragment_profile, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        sharedPreferences = requireContext().getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

        // Initialize Views
        profileImage = view.findViewById(R.id.profile_image)
        locationSwitch = view.findViewById(R.id.switch_location)
        cameraSwitch = view.findViewById(R.id.switch_camera)
        eventRemindersSwitch = view.findViewById(R.id.switch_event_reminders)
        emailNotificationsSwitch = view.findViewById(R.id.switch_email_notifications)
        pushNotificationsSwitch = view.findViewById(R.id.switch_push_notifications)
        
        tvName = view.findViewById(R.id.value_name)
        tvEmail = view.findViewById(R.id.value_email)
        tvPhone = view.findViewById(R.id.value_phone)
        tvLocation = view.findViewById(R.id.value_location)
        headerName = view.findViewById(R.id.header_name)
        headerEmail = view.findViewById(R.id.header_email)
        headerPhone = view.findViewById(R.id.header_phone)
        
        noticeTimeValue = view.findViewById(R.id.value_notice_time)
        eventRemindersSubtitle = view.findViewById(R.id.tv_event_reminders_subtitle)

        // Load Persisted Data
        loadUserData()

        // Sync with unified LiveData from ViewModel
        favoritesViewModel.notificationsEnabled.observe(viewLifecycleOwner) { isEnabled ->
            eventRemindersSwitch.isChecked = isEnabled
        }
        
        favoritesViewModel.noticeTimeMinutes.observe(viewLifecycleOwner) { minutes ->
            val timeText = "$minutes min"
            noticeTimeValue.text = timeText
            eventRemindersSubtitle.text = "$timeText antes"
        }

        // Personal Info Listeners
        view.findViewById<View>(R.id.row_name).setOnClickListener { showEditDialog("Nombre", KEY_NAME, tvName) }
        view.findViewById<View>(R.id.row_email).setOnClickListener { showEditDialog("Email", KEY_EMAIL, tvEmail) }
        view.findViewById<View>(R.id.row_phone).setOnClickListener { showEditDialog("Teléfono", KEY_PHONE, tvPhone) }
        view.findViewById<View>(R.id.row_location).setOnClickListener { showEditDialog("Ubicación", KEY_LOCATION, tvLocation) }

        profileImage.setOnClickListener {
            showImagePickerOptions()
        }

        // Location Permission Listeners
        val locationRow = view.findViewById<View>(R.id.row_location_permission)
        locationRow.setOnClickListener {
            if (!locationSwitch.isChecked) checkAndRequestLocationPermission() else showRevokePermissionDialog("ubicación")
        }
        locationSwitch.setOnClickListener {
            if (locationSwitch.isChecked) checkAndRequestLocationPermission() else showRevokePermissionDialog("ubicación")
        }

        // Camera Permission Listeners
        val cameraRow = view.findViewById<View>(R.id.row_camera_permission)
        cameraRow.setOnClickListener {
            if (!cameraSwitch.isChecked) checkAndRequestCameraPermission() else showRevokePermissionDialog("cámara")
        }
        cameraSwitch.setOnClickListener {
            if (cameraSwitch.isChecked) checkAndRequestCameraPermission() else showRevokePermissionDialog("cámara")
        }
        
        // Event Reminders Switch Listener
        eventRemindersSwitch.setOnClickListener {
            val isChecked = eventRemindersSwitch.isChecked
            if (isChecked) {
                checkAndRequestNotificationPermission()
            } else {
                favoritesViewModel.setNotificationsEnabled(false)
            }
        }

        // Email Notifications Switch Listener
        emailNotificationsSwitch.setOnCheckedChangeListener { _, isChecked ->
            sharedPreferences.edit().putBoolean(KEY_EMAIL_NOTIF, isChecked).apply()
        }

        // Push Notifications Switch Listener
        pushNotificationsSwitch.setOnCheckedChangeListener { _, isChecked ->
            sharedPreferences.edit().putBoolean(KEY_PUSH_NOTIF, isChecked).apply()
        }
        
        // Notice Time Listener
        view.findViewById<View>(R.id.row_notice_time).setOnClickListener {
            showNoticeTimeDialog()
        }

        updatePermissionStates()
    }

    override fun onResume() {
        super.onResume()
        updatePermissionStates()
    }

    private fun updatePermissionStates() {
        val fineLocation = ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION)
        val coarseLocation = ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_COARSE_LOCATION)
        locationSwitch.isChecked = fineLocation == PackageManager.PERMISSION_GRANTED || coarseLocation == PackageManager.PERMISSION_GRANTED

        val camera = ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.CAMERA)
        cameraSwitch.isChecked = camera == PackageManager.PERMISSION_GRANTED
    }

    private fun checkAndRequestLocationPermission() {
        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            locationPermissionLauncher.launch(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION))
        } else {
            locationSwitch.isChecked = true
        }
    }

    private fun checkAndRequestCameraPermission() {
        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
        } else {
            cameraSwitch.isChecked = true
        }
    }
    
    private fun checkAndRequestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                // Revert switch visually until permission is granted
                eventRemindersSwitch.isChecked = false
                notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            } else {
                favoritesViewModel.setNotificationsEnabled(true)
            }
        } else {
            favoritesViewModel.setNotificationsEnabled(true)
        }
    }

    private fun showRevokePermissionDialog(type: String) {
        AlertDialog.Builder(requireContext())
            .setTitle("Revocar permisos")
            .setMessage("Para desactivar completamente el acceso a la $type, debes hacerlo desde los ajustes de la aplicación.")
            .setPositiveButton("Ir a Ajustes") { _, _ ->
                val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                val uri = Uri.fromParts("package", requireContext().packageName, null)
                intent.data = uri
                startActivity(intent)
            }
            .setNegativeButton("Cancelar") { _, _ ->
                updatePermissionStates()
            }
            .show()
    }

    private fun loadUserData() {
        val name = sharedPreferences.getString(KEY_NAME, "María García López")
        val email = sharedPreferences.getString(KEY_EMAIL, "maria.garcia@email.com")
        val phone = sharedPreferences.getString(KEY_PHONE, "+34 612 345 678")
        val location = sharedPreferences.getString(KEY_LOCATION, "Aragón, España")
        val imagePath = sharedPreferences.getString(KEY_IMAGE_PATH, null)
        val emailNotif = sharedPreferences.getBoolean(KEY_EMAIL_NOTIF, false)
        val pushNotif = sharedPreferences.getBoolean(KEY_PUSH_NOTIF, true)

        tvName.text = name
        tvEmail.text = email
        tvPhone.text = phone
        tvLocation.text = location
        emailNotificationsSwitch.isChecked = emailNotif
        pushNotificationsSwitch.isChecked = pushNotif
        
        headerName.text = name?.split(" ")?.get(0) ?: "María"
        headerEmail.text = email
        headerPhone.text = phone

        if (imagePath != null) {
            val file = File(imagePath)
            if (file.exists()) {
                val bitmap = BitmapFactory.decodeFile(file.absolutePath)
                profileImage.setImageBitmap(bitmap)
            }
        }
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
            if (key == KEY_NAME) headerName.text = newValue.split(" ")[0]
            if (key == KEY_EMAIL) headerEmail.text = newValue
            if (key == KEY_PHONE) headerPhone.text = newValue
        }
        builder.setNegativeButton("Cancelar", null)
        builder.show()
    }

    private fun showNoticeTimeDialog() {
        val options = arrayOf("5 min", "10 min", "15 min", "30 min", "60 min")
        val minutes = arrayOf(5, 10, 15, 30, 60)
        
        val builder = AlertDialog.Builder(requireContext())
        builder.setTitle("Seleccionar tiempo de aviso")
        builder.setItems(options) { _, which ->
            favoritesViewModel.setNoticeTime(minutes[which])
        }
        builder.show()
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
                    if (cameraSwitch.isChecked) {
                        val intent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
                        startActivityForResult(intent, TAKE_PHOTO_REQUEST)
                    } else {
                        Toast.makeText(requireContext(), "El acceso a la cámara está desactivado en la configuración de privacidad", Toast.LENGTH_LONG).show()
                    }
                }
            }
        }
        builder.show()
    }

    private fun saveImageToInternalStorage(uri: Uri): String? {
        return try {
            val inputStream = requireContext().contentResolver.openInputStream(uri) ?: return null
            val file = File(requireContext().filesDir, "profile_image.jpg")
            val outputStream = FileOutputStream(file)
            inputStream.use { input ->
                outputStream.use { output ->
                    input.copyTo(output)
                }
            }
            file.absolutePath
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    private fun saveBitmapToInternalStorage(bitmap: Bitmap): String? {
        return try {
            val file = File(requireContext().filesDir, "profile_image.jpg")
            val outputStream = FileOutputStream(file)
            bitmap.compress(Bitmap.CompressFormat.JPEG, 100, outputStream)
            outputStream.close()
            file.absolutePath
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode == Activity.RESULT_OK) {
            var imagePath: String? = null
            when (requestCode) {
                PICK_IMAGE_REQUEST -> {
                    data?.data?.let { uri ->
                        imagePath = saveImageToInternalStorage(uri)
                        profileImage.setImageURI(uri)
                    }
                }
                TAKE_PHOTO_REQUEST -> {
                    val imageBitmap = data?.extras?.get("data") as? Bitmap
                    imageBitmap?.let { bitmap ->
                        imagePath = saveBitmapToInternalStorage(bitmap)
                        profileImage.setImageBitmap(bitmap)
                    }
                }
            }
            imagePath?.let {
                sharedPreferences.edit().putString(KEY_IMAGE_PATH, it).apply()
            }
        }
    }
}
