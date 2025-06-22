# PermissionManager for Android

[![](https://jitpack.io/v/jksalcedo/PermissionManager.svg)](https://jitpack.io/#jksalcedo/PermissionManager)

A modern, lightweight, and robust Android library for handling runtime permissions with the power of Kotlin Coroutines. It abstracts away the boilerplate of ActivityResultContracts and provides a clean, sequential, and easy-to-read way to request permissions in your app.
Why use PermissionManager?

- **Coroutine-Based:** Say goodbye to callback hell. Request permissions in a sequential, synchronous-like style within your coroutines.

- **Lifecycle Aware:** Built on top of registerForActivityResult, it's completely lifecycle-safe and automatically handles cleanup.

- **Simple & Clean API:** The API is designed to be intuitive and easy to understand, reducing the amount of code you need to write.

- **Robust Request Handling:** Features a built-in queue to safely handle multiple, rapid-fire permission requests without race conditions.

- **Handles All Cases:** Easily distinguish between granted, denied, and permanently denied permissions to provide a better user experience.
  

## Features

✅ Coroutine-first API using suspendCancellableCoroutine.

✅ Simple initialization for Activities and Fragments.

✅ Request single or multiple permissions with one call.

✅ Gracefully handles configuration changes.

✅ No more overriding onRequestPermissionsResult.

✅ Written entirely in Kotlin.


## Installation

Add the following dependencies to your module's build.gradle.kts (or build.gradle) file.

**Step 1: Add JitPack repository**

Add JitPack to your project's settings.gradle file:
```
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
        maven { url 'https://jitpack.io' } // Add this line
    }
}
```
**Step 2: Add the library dependency**
```
// build.gradle.kts
dependencies {
    implementation("com.github.jksalcedo.PermissionManager:permission-manager:1.0.0")

    // PermissionManager also requires the following dependencies
    implementation("androidx.activity:activity-ktx:1.9.0")
    implementation("androidx.fragment:fragment-ktx:1.8.1")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.8.1")
}
```

## How to Use
### 1. Initialization

Initialize the PermissionManager in your Activity or Fragment. It's best to do this as a property delegate or a lazy-initialized property.

**In an Activity:**
```
class MainActivity : AppCompatActivity() {

    private val permissionManager: PermissionManager by lazy {
        PermissionManager.from(this)
    }

    // ...
}
```
**In a Fragment:**
```
class MyFragment : Fragment() {

    private val permissionManager: PermissionManager by lazy {
        PermissionManager.from(this)
    }

    // ...
}
```
### 2. Requesting Permissions

Launch a coroutine from a LifecycleScope and call the request() method.

**Requesting a Single Permission:**

Here’s how you would request the camera permission when a button is clicked.
```
// Inside your Activity or Fragment
fun onCameraButtonClick() {
    lifecycleScope.launch {
        when (permissionManager.request(Manifest.permission.CAMERA)) {
            is PermissionResult.Granted -> {
                // Permission is granted. You can now use the camera.
                Toast.makeText(this@MainActivity, "Camera permission granted!", Toast.LENGTH_SHORT).show()
                openCamera()
            }
            is PermissionResult.Denied -> {
                // Permission was denied. Show a message explaining why you need it.
                Toast.makeText(this@MainActivity, "Camera permission denied.", Toast.LENGTH_SHORT).show()
            }
            is PermissionResult.PermanentlyDenied -> {
                // Permission was permanently denied.
                // You must direct the user to the app settings to enable it.
                Toast.makeText(this@MainActivity, "Camera permission permanently denied.", Toast.LENGTH_SHORT).show()
                // Intent to open app settings
            }
        }
    }
}
```

**Requesting Multiple Permissions:**

You can request multiple permissions in a single call.
```
fun requestStorageAndLocation() {
    lifecycleScope.launch {
        val result = permissionManager.request(
        Manifest.permission.READ_EXTERNAL_STORAGE,
        Manifest.permission.ACCESS_FINE_LOCATION
        )

        when (result) {
            is PermissionResult.Granted -> {
                // All permissions were granted
                Log.d("Permissions", "Storage and Location granted.")
            }
            is PermissionResult.Denied -> {
                // One or more permissions were denied
                Log.d("Permissions", "Denied: ${result.deniedPermissions}")
            }
            is PermissionResult.PermanentlyDenied -> {
                // One or more permissions were permanently denied
                Log.d("Permissions", "Permanently Denied: ${result.permanentlyDeniedPermissions}")
            }
        }
    }
}
```

### 3. Checking Permissions and Showing a Rationale

Before requesting a permission, you can check if it's already granted or if you should show a rationale to the user (if they've denied it previously).
```
fun checkAndRequestCamera() {
    // Check if permission is already granted
    if (permissionManager.arePermissionsGranted(Manifest.permission.CAMERA)) {
        openCamera()
        return
    }

    lifecycleScope.launch {
        // Check if you should show a rationale
        if (permissionManager.shouldShowRationale(Manifest.permission.CAMERA)) {
            // Show a dialog or UI explaining why you need the permission
            showCameraRationaleDialog {
                // After rationale, request again
                requestCameraPermission()
            }
        } else {
            // Either first time asking or permanently denied
            requestCameraPermission()
        }
    }
}

private suspend fun requestCameraPermission() {
    val result = permissionManager.request(Manifest.permission.CAMERA)
    if (result is PermissionResult.Granted) {
    openCamera()
    }
}
```

## Contributing

Contributions are always welcome! Please feel free to submit a pull request or open an issue.
