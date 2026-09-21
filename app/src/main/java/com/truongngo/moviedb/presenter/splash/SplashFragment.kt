package com.truongngo.moviedb.presenter.splash

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.View
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.core.content.edit
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import com.truongngo.moviedb.R
import com.truongngo.moviedb.databinding.FragmentSplashBinding
import com.truongngo.moviedb.presenter.navigation.NavigationViewModel
import dagger.hilt.android.AndroidEntryPoint

/** Startup asks once; Settings owns explicit retries and permission explanations. */
@AndroidEntryPoint
class SplashFragment : Fragment(R.layout.fragment_splash) {
    private val navigation: NavigationViewModel by activityViewModels()
    private var binding: FragmentSplashBinding? = null
    private var permissionRequestPending = false
    private val permissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) {
            permissionRequestPending = false
            navigation.notificationPermissionFinished()
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        permissionRequestPending = savedInstanceState?.getBoolean(REQUEST_PENDING) ?: false
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding = FragmentSplashBinding.bind(view)
    }

    override fun onResume() {
        super.onResume()
        // ActivityResultRegistry reconnects the existing request after recreation.
        if (permissionRequestPending) return
        val context = requireContext()
        val preferences = context.getSharedPreferences("runtime_permissions", Context.MODE_PRIVATE)
        if (
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED ||
            preferences.getBoolean(SPLASH_PROMPTED, false)
        ) {
            navigation.notificationPermissionFinished()
            return
        }
        // Persist before launch so later app starts never repeatedly prompt after a denial.
        preferences.edit { putBoolean(SPLASH_PROMPTED, true) }
        permissionRequestPending = true
        permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
    }

    override fun onSaveInstanceState(outState: Bundle) {
        outState.putBoolean(REQUEST_PENDING, permissionRequestPending)
        super.onSaveInstanceState(outState)
    }

    override fun onDestroyView() {
        binding = null
        super.onDestroyView()
    }

    private companion object {
        const val SPLASH_PROMPTED = "splash_notification_prompted"
        const val REQUEST_PENDING = "notification_request_pending"
    }
}
