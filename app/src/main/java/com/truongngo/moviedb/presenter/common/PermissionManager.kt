package com.truongngo.moviedb.presenter.common

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.StringRes
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import androidx.core.content.edit
import androidx.fragment.app.Fragment
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.truongngo.moviedb.R

/** A runtime permission and the Android versions on which the feature needs it. */
data class RuntimePermission(val name: String, val minSdk: Int = 23, val maxSdk: Int = Int.MAX_VALUE)

data class PermissionMessages(
    @param:StringRes val title: Int,
    @param:StringRes val rationale: Int,
    @param:StringRes val denied: Int,
    @param:StringRes val blocked: Int,
)

data class PermissionResult(
    val granted: Set<String>,
    val denied: Set<String>,
    val blocked: Set<String>,
) {
    val allGranted: Boolean get() = denied.isEmpty() && blocked.isEmpty()
}

/**
 * Create as a Fragment property (before STARTED), then call request() from a user action.
 * Configure only permissions needed together; special access permissions need separate flows.
 * Returning from Settings never automatically resumes the feature: call request() again.
 * Call dismiss() in onDestroyView(). The fixed configuration survives Fragment recreation.
 */
class PermissionManager(
    private val fragment: Fragment,
    permissions: List<RuntimePermission>,
    private val messages: PermissionMessages,
    private val onResult: (PermissionResult) -> Unit,
) {
    private val permissions = permissions.filter { Build.VERSION.SDK_INT in it.minSdk..it.maxSdk }
        .map { it.name }.distinct()
    private var dialog: AlertDialog? = null
    private var requesting = false
    private val preferences get() = fragment.requireContext()
        .getSharedPreferences("runtime_permissions", Context.MODE_PRIVATE)
    private val launcher = fragment.registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions(),
    ) {
        requesting = false
        val result = currentResult()
        result.denied.filter { fragment.shouldShowRequestPermissionRationale(it) }.forEach { permission ->
            preferences.edit { putBoolean(permission, true) }
        }
        deliver(result)
    }

    fun request() {
        if (requesting || dialog?.isShowing == true) return
        val result = currentResult()
        when {
            result.allGranted -> onResult(result)
            result.blocked.isNotEmpty() -> deliver(result)
            result.denied.any { fragment.shouldShowRequestPermissionRationale(it) } -> {
                dialog = builder(messages.rationale)
                    .setNegativeButton(R.string.permission_cancel) { _, _ -> onResult(result) }
                    .setOnCancelListener { onResult(result) }
                    .setPositiveButton(R.string.permission_retry) { _, _ -> launchMissing() }
                    .show()
            }
            else -> launchMissing()
        }
    }

    private fun launchMissing() {
        val missing = permissions.filterNot(::isGranted)
        if (missing.isEmpty()) {
            onResult(currentResult())
            return
        }
        missing.filter { fragment.shouldShowRequestPermissionRationale(it) }.forEach { permission ->
            preferences.edit { putBoolean(permission, true) }
        }
        requesting = true
        launcher.launch(missing.toTypedArray())
    }

    private fun isGranted(permission: String) = ContextCompat.checkSelfPermission(
        fragment.requireContext(), permission,
    ) == PackageManager.PERMISSION_GRANTED

    private fun currentResult(): PermissionResult {
        val granted = permissions.filter(::isGranted).toSet()
        val missing = permissions.toSet() - granted
        // A first request or dismissed prompt can also have rationale=false. Require
        // a previous retryable denial before classifying a missing permission as blocked.
        val blocked = missing.filter {
            preferences.getBoolean(it, false) && !fragment.shouldShowRequestPermissionRationale(it)
        }.toSet()
        return PermissionResult(granted, missing - blocked, blocked)
    }

    private fun deliver(result: PermissionResult) {
        if (!result.allGranted) {
            dialog = if (result.blocked.isNotEmpty()) {
                builder(messages.blocked)
                    .setNegativeButton(R.string.permission_cancel, null)
                    .setPositiveButton(R.string.permission_settings) { _, _ ->
                        fragment.startActivity(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                            data = Uri.fromParts("package", fragment.requireContext().packageName, null)
                        })
                    }.show()
            } else {
                builder(messages.denied).setPositiveButton(android.R.string.ok, null).show()
            }
        }
        onResult(result)
    }

    private fun builder(@StringRes message: Int) = MaterialAlertDialogBuilder(fragment.requireContext())
        .setTitle(messages.title).setMessage(message)

    fun dismiss() {
        dialog?.dismiss()
        dialog = null
    }
}
