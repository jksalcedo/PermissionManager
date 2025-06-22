package com.jksalcedo.permissionmanager

import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {

    private lateinit var permissionManager: PermissionManager
    private lateinit var statusTextView: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Initialize UI elements
        statusTextView = findViewById(R.id.status_text)
        val requestButton: Button = findViewById(R.id.request_button)

        // Initialize PermissionManager
        permissionManager = PermissionManager.from(this)

        // Set up button click listener to request permissions
        requestButton.setOnClickListener {
            requestPermissions()
        }

        // Initial check
        updateStatus("Initial state: Tap button to request permissions")
    }

    private fun requestPermissions() {
        CoroutineScope(Dispatchers.Main).launch {
            try {
                val result = permissionManager.request(
                    android.Manifest.permission.CAMERA,
                    android.Manifest.permission.ACCESS_FINE_LOCATION
                )
                when (result) {
                    is PermissionManager.PermissionResult.Granted -> {
                        updateStatus("Permissions granted!")
                    }
                    is PermissionManager.PermissionResult.Denied -> {
                        updateStatus("Permissions denied: ${result.deniedPermissions}")
                        if (result.deniedPermissions.isNotEmpty()) {
                            ActivityCompat.requestPermissions(
                                this@MainActivity,
                                result.deniedPermissions.toTypedArray(),
                                100
                            )
                        }
                    }
                    is PermissionManager.PermissionResult.PermanentlyDenied -> {
                        updateStatus("Permanently denied: ${result.permanentlyDeniedPermissions}. Please enable in settings.")
                    }
                }
            } catch (e: Exception) {
                Log.e("PermissionTest", "Error requesting permissions", e)
                updateStatus("Error: ${e.message}")
            }
        }
    }

    private fun updateStatus(message: String) {
        statusTextView.text = message
    }

    // Handle permission request results (optional, for manual re-request)
    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == 100) {
            val denied = permissions.filterIndexed { index, _ -> grantResults[index] == PackageManager.PERMISSION_DENIED }
            if (denied.isNotEmpty()) {
                updateStatus("Still denied: $denied")
            } else {
                updateStatus("Permissions granted after re-request!")
            }
        }
    }
}