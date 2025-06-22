package com.jksalcedo.permissionmanager

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import kotlinx.coroutines.CancellableContinuation
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

/**
 * Manages Android runtime permissions with a modern, coroutine-based API.
 *
 * This class simplifies the process of requesting permissions by abstracting away the
 * boilerplate code associated with ActivityResultLaunchers and providing a clean,
 * suspendable function.
 *
 * @property activity The ComponentActivity associated with this permission manager.
 * @property launcher The ActivityResultLauncher responsible for initiating permission requests.
 */
class PermissionManager private constructor(
    private val activity: ComponentActivity,
    private var launcher: ActivityResultLauncher<Array<String>>?
) {

    // A queue to hold continuations for pending permission requests.
    // This handles scenarios where requests might be made in quick succession.
    private val requestQueue = ArrayDeque<Pair<Array<out String>, ContinuationHolder>>()

    // A nullable Continuation holder to manage the active request's state.
    private var activeContinuation: ContinuationHolder? = null

    /**
     * Sealed class representing the possible outcomes of a permission request.
     */
    sealed class PermissionResult {
        /** Indicates that all requested permissions have been granted by the user. */
        data object Granted : PermissionResult()

        /**
         * Indicates that one or more permissions were denied by the user.
         * @property deniedPermissions A list of permissions that were not granted.
         */
        data class Denied(val deniedPermissions: List<String>) : PermissionResult()

        /**
         * Indicates that one or more permissions were permanently denied by the user
         * (i.e., the user selected "Don't ask again").
         * @property permanentlyDeniedPermissions A list of permissions that were permanently denied.
         */
        data class PermanentlyDenied(val permanentlyDeniedPermissions: List<String>) : PermissionResult()

        /**
         * Indicates that a background permission is required and the user needs to be
         * redirected to the app's settings page to grant it. This is typically used for
         * permissions like `ACCESS_BACKGROUND_LOCATION`.
         * @property permission The specific background permission that needs to be granted via settings.
         */
        data class BackgroundPermissionRequiredSettings(val permission: String) : PermissionResult()
    }

    /**
     * A holder for the coroutine's continuation to manage cancellation.
     */
    private class ContinuationHolder(
        private val continuation: CancellableContinuation<PermissionResult>
    ) {
        fun resume(result: PermissionResult) {
            if (continuation.isActive) {
                continuation.resume(result)
            }
        }

        fun setupCancellation(onCancel: () -> Unit) {
            continuation.invokeOnCancellation { onCancel() }
        }
    }


    companion object {
        /**
         * Creates a PermissionManager instance tied to a ComponentActivity's lifecycle.
         *
         * @param activity The ComponentActivity from which permissions will be requested.
         * @return A configured instance of PermissionManager.
         */
        @RequiresApi(Build.VERSION_CODES.Q)
        fun from(activity: ComponentActivity): PermissionManager {
            val manager = PermissionManager(activity, null)
            manager.launcher = activity.registerForActivityResult(
                ActivityResultContracts.RequestMultiplePermissions()
            ) { permissions ->
                manager.handlePermissionResult(permissions)
            }
            return manager
        }

        /**
         * Creates a PermissionManager instance tied to a Fragment's lifecycle.
         *
         * @param fragment The Fragment from which permissions will be requested.
         * @return A configured instance of PermissionManager.
         */
        @RequiresApi(Build.VERSION_CODES.Q)
        fun from(fragment: Fragment): PermissionManager {
            val manager = PermissionManager(fragment.requireActivity(), null)
            manager.launcher = fragment.registerForActivityResult(
                ActivityResultContracts.RequestMultiplePermissions()
            ) { permissions ->
                manager.handlePermissionResult(permissions)
            }
            return manager
        }
    }

    /**
     * Processes the next permission request in the queue if one is available.
     */
    private fun processNextRequest() {
        if (activeContinuation != null) return // Another request is already active.

        val nextRequest = requestQueue.removeFirstOrNull()
        if (nextRequest != null) {
            val (permissions, continuationHolder) = nextRequest
            activeContinuation = continuationHolder
            launcher?.launch(permissions as Array<String>)
        }
    }

    /**
     * Handles the result received from the ActivityResultLauncher and resumes the
     * corresponding coroutine.
     */
    @RequiresApi(Build.VERSION_CODES.Q)
    private fun handlePermissionResult(result: Map<String, Boolean>) {
        val continuation = activeContinuation
        activeContinuation = null // Clear the active continuation.

        val denied = result.filterValues { !it }.keys.toList()

        val permissionResult = if (denied.isEmpty()) {
            // Check for specific background permissions here if all initially granted
            val backgroundLocationRequested = result.keys.contains(Manifest.permission.ACCESS_BACKGROUND_LOCATION)
            if (backgroundLocationRequested && !isBackgroundLocationGranted()) {
                // Even if the initial dialog was "granted", if it's background location,
                // we might still need to guide them to settings.
                PermissionResult.BackgroundPermissionRequiredSettings(Manifest.permission.ACCESS_BACKGROUND_LOCATION)
            } else {
                PermissionResult.Granted
            }
        } else {
            val permanentlyDenied = denied.filter {
                !activity.shouldShowRequestPermissionRationale(it)
            }
            if (permanentlyDenied.isNotEmpty()) {
                PermissionResult.PermanentlyDenied(permanentlyDenied)
            } else {
                // Handle specific cases for denied permissions
                if (denied.contains(Manifest.permission.ACCESS_BACKGROUND_LOCATION)) {
                    // On Android 10+, denying background location might mean they just gave foreground.
                    // You could potentially differentiate here, but usually, it's safer to assume
                    // a denial means they need to be prompted to settings for background.
                    PermissionResult.BackgroundPermissionRequiredSettings(Manifest.permission.ACCESS_BACKGROUND_LOCATION)
                } else {
                    PermissionResult.Denied(denied)
                }
            }
        }
        continuation?.resume(permissionResult)
        processNextRequest() // Process the next item in the queue.
    }

    /**
     * Suspends the current coroutine and requests the specified permissions.
     * The coroutine will resume when the user responds to the permission dialog.
     *
     * @param permissions A vararg of permission strings to request (e.g., Manifest.permission.CAMERA).
     * @return A [PermissionResult] indicating the outcome of the request.
     */
    suspend fun request(vararg permissions: String): PermissionResult {
        // If all permissions are already granted, return immediately.
        if (arePermissionsGranted(*permissions)) {
            return PermissionResult.Granted
        }

        return suspendCancellableCoroutine { continuation ->
            val holder = ContinuationHolder(continuation)

            holder.setupCancellation {
                // If the coroutine is cancelled, remove its request from the queue.
                requestQueue.removeAll { it.second === holder }
                if (activeContinuation === holder) {
                    activeContinuation = null
                    processNextRequest()
                }
            }

            requestQueue.add(permissions to holder)
            processNextRequest()
        }
    }

    /**
     * Checks if all specified permissions have been granted.
     *
     * @param permissions The permissions to check.
     * @return `true` if all permissions are granted, `false` otherwise.
     */
    fun arePermissionsGranted(vararg permissions: String): Boolean {
        return permissions.all {
            ContextCompat.checkSelfPermission(activity, it) == PackageManager.PERMISSION_GRANTED
        }
    }

    /**
     * Determines whether you should show a rationale for a permission request.
     * This should be called before `request()` if a permission was previously denied.
     *
     * @param permission The permission to check.
     * @return `true` if a rationale should be shown, `false` otherwise.
     */
    fun shouldShowRationale(permission: String): Boolean {
        return activity.shouldShowRequestPermissionRationale(permission)
    }

    // Helper function to check if background location is truly granted
    private fun isBackgroundLocationGranted(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            ContextCompat.checkSelfPermission(
                activity,
                Manifest.permission.ACCESS_BACKGROUND_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            true // Background location is implicitly granted with ACCESS_FINE_LOCATION on older Android versions
        }
    }

    fun openAppSettings() {
        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
            data = Uri.fromParts("package", activity.packageName, null)
            addCategory(Intent.CATEGORY_DEFAULT)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        activity.startActivity(intent)
    }
}

/**
 * Extension function for the Context to conveniently check if a single permission is granted.
 *
 * @param permission The permission string (e.g., Manifest.permission.READ_CONTACTS).
 * @return `true` if the permission is granted, `false` otherwise.
 */
fun Context.isPermissionGranted(permission: String): Boolean {
    return ContextCompat.checkSelfPermission(this, permission) == PackageManager.PERMISSION_GRANTED
}
