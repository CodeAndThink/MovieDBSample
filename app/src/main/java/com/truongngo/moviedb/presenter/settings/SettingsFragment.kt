package com.truongngo.moviedb.presenter.settings

import android.Manifest
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import com.truongngo.moviedb.presenter.navigation.NavigationViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.hilt.navigation.fragment.hiltNavGraphViewModels
import com.truongngo.moviedb.domain.model.AppLanguage
import com.truongngo.moviedb.R
import com.truongngo.moviedb.databinding.FragmentSettingsBinding
import com.truongngo.moviedb.presenter.enum.ThemeMode
import com.truongngo.moviedb.presenter.common.PermissionManager
import com.truongngo.moviedb.presenter.common.PermissionMessages
import com.truongngo.moviedb.presenter.common.RuntimePermission
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class SettingsFragment : Fragment() {

    private val viewModel: SettingsViewModel by hiltNavGraphViewModels(R.id.main_navigation)
    private val navigation: NavigationViewModel by activityViewModels()
    private val notificationPermission = PermissionManager(
        this,
        listOf(RuntimePermission(Manifest.permission.POST_NOTIFICATIONS, minSdk = 33)),
        PermissionMessages(
            R.string.push_permission_title, R.string.push_permission_rationale,
            R.string.push_permission_denied, R.string.push_permission_blocked,
        ),
    ) { }
    private var isRendering = false
    private var _binding: FragmentSettingsBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSettingsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.settings.collect { settings ->
                    isRendering = true
                    when (settings.themeMode) {
                        ThemeMode.SYSTEM -> binding.radioSystem.isChecked = true
                        ThemeMode.LIGHT -> binding.radioLight.isChecked = true
                        ThemeMode.DARK -> binding.radioDark.isChecked = true
                    }
                    binding.radioGroupLanguage.check(when (settings.language) {
                        AppLanguage.ENGLISH -> R.id.radioEnglish
                        AppLanguage.VIETNAMESE -> R.id.radioVietnamese
                    })
                    isRendering = false
                }
            }
        }

        binding.radioGroupLanguage.setOnCheckedChangeListener { _, checkedId ->
            if (isRendering) return@setOnCheckedChangeListener
            when (checkedId) {
                R.id.radioEnglish -> viewModel.onLanguageChanged(AppLanguage.ENGLISH)
                R.id.radioVietnamese -> viewModel.onLanguageChanged(AppLanguage.VIETNAMESE)
            }
        }

        binding.btnEnableNotifications.setOnClickListener { notificationPermission.request() }

        binding.btnLogout.setOnClickListener { navigation.logout() }

        binding.radioGroupTheme.setOnCheckedChangeListener { _, checkedId ->
            if (isRendering) return@setOnCheckedChangeListener
            val newMode = when (checkedId) {
                R.id.radioSystem -> ThemeMode.SYSTEM
                R.id.radioLight -> ThemeMode.LIGHT
                R.id.radioDark -> ThemeMode.DARK
                else -> ThemeMode.SYSTEM
            }
            viewModel.onThemeModeChanged(newMode)
        }
    }

    override fun onDestroyView() {
        notificationPermission.dismiss()
        binding.btnEnableNotifications.setOnClickListener(null)
        binding.btnLogout.setOnClickListener(null)
        binding.radioGroupTheme.setOnCheckedChangeListener(null)
        binding.radioGroupLanguage.setOnCheckedChangeListener(null)
        _binding = null
        super.onDestroyView()
    }
}
