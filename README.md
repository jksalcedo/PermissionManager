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


---

## Limitations

While PermissionManager aims to simplify runtime permission handling, it's important to be aware of the following considerations:

* **Single Active Request:** The library processes permission requests sequentially using an internal queue. This means **only one permission request dialog can be active at a time**. If multiple `request()` calls are made in quick succession, they'll be queued and presented to the user one after another.
* **Android's Permission Dialog UI:** The appearance and behavior of the permission dialogs are controlled by the Android system. PermissionManager **doesn't offer customization of these dialogs**.
* **Manual Step for Full Background Permissions:** For permissions like `android.permission.ACCESS_BACKGROUND_LOCATION` or `android.permission.ACTIVITY_RECOGNITION` (on Android 10+), getting full background access often requires an **additional user interaction to navigate to the app's system settings** after the initial permission dialog. While your `PermissionManager` can return a `PermissionResult.BackgroundPermissionRequiredSettings` to indicate this, and provides the `openAppSettings()` utility, the library's `request()` method **doesn't automatically initiate this navigation**. Developers must explicitly call `openAppSettings()` based on the `PermissionResult` to complete the process.
* **Context Dependency:** The `PermissionManager` instance is tied to the lifecycle of an `Activity` or `Fragment`. If your app's architecture involves requesting permissions from contexts outside of an `Activity` or `Fragment` (e.g., from a `Service` or `Application` class directly), you'll need to find alternative methods for permission requests in those scenarios, as this library is **not designed for such use cases**.


## Installation

Add the following dependencies to your module's build.gradle.kts (or build.gradle) file.

**Step 1: Add JitPack repository**

Add JitPack to your project's settings.gradle file:
```kotlin
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
```kotlin
// build.gradle.kts
dependencies {
    implementation("com.github.jksalcedo:PermissionManager:1.0.0")

    // PermissionManager also requires the following dependencies
    implementation("androidx.activity:activity-ktx:1.9.0")
    implementation("androidx.fragment:fragment-ktx:1.8.1")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.8.1")
}
```

## API
| Function / Class                       | Description                                              | Parameters                  | Returns                 |
|----------------------------------------|----------------------------------------------------------|-----------------------------|-------------------------|
| `PermissionManager.from(activity)`     | Instantiates a `PermissionManager` tied to a `ComponentActivity`. | `activity: ComponentActivity` | `PermissionManager`     |
| `PermissionManager.from(fragment)`     | Instantiates a `PermissionManager` tied to a `Fragment`. | `fragment: Fragment`         | `PermissionManager`     |
| `suspend PermissionManager.request(vararg permissions)` | Suspends and requests the specified permissions. Returns when the user responds. | `permissions: String...`     | `PermissionResult`      |
| `PermissionManager.arePermissionsGranted(vararg permissions)` | Checks if all permissions are already granted. | `permissions: String...`     | `Boolean`               |
| `PermissionManager.shouldShowRationale(permission)` | Checks if rationale should be shown for a permission. | `permission: String`         | `Boolean`               |
| `PermissionManager.openAppSettings()`  | Opens the app’s settings screen (for manually granting permissions). | —                           | `Unit`                  |
| `PermissionResult.Granted`             | Indicates all permissions granted.                      | —                           | —                       |
| `PermissionResult.Denied(deniedPermissions)` | Indicates one or more permissions denied.           | `deniedPermissions: List<String>` | —                   |
| `PermissionResult.PermanentlyDenied(permanentlyDeniedPermissions)` | Indicates one or more permissions permanently denied (user selected “Don’t ask again”). | `permanentlyDeniedPermissions: List<String>` | — |
| `PermissionResult.BackgroundPermissionRequiredSettings(permission)` | User must manually enable a background permission in settings. | `permission: String`         | —                       |
| `Context.isPermissionGranted(permission)` | Checks if a specific permission is granted.          | `permission: String`         | —                       |


## How to Use
### 1. Initialization

Initialize the PermissionManager in your Activity or Fragment. It's best to do this as a property delegate or a lazy-initialized property.

**In an Activity:**
```kotlin
class MainActivity : AppCompatActivity() {

    private val permissionManager: PermissionManager by lazy {
        PermissionManager.from(this)
    }

    // ...
}
```
**In a Fragment:**
```kotlin
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
```kotlin
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
```kotlin
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
```kotlin
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

Contributions are welcome! To contribute:

1. Fork the repository.
2. Create a new branch for your feature or bugfix.
3. Make your changes and add tests if needed.
4. Open a pull request with a clear description of your changes.

Please follow the existing code style and conventions.
