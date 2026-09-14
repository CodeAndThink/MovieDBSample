package com.truongngo.moviedb

import android.os.Bundle
import android.content.Intent
import android.widget.Toast
import androidx.navigation.fragment.NavHostFragment
import com.truongngo.moviedb.presenter.navigation.NavigationViewModel
import com.truongngo.moviedb.presenter.navigation.AppNavigator
import com.truongngo.moviedb.presenter.navigation.AppDestination
import javax.inject.Inject
import kotlinx.coroutines.flow.combine
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.graphics.Insets
import androidx.core.view.isVisible
import androidx.appcompat.app.AppCompatDelegate
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.truongngo.moviedb.databinding.ActivityMainBinding
import com.truongngo.moviedb.presenter.MainActivityViewModel
import com.truongngo.moviedb.presenter.enum.ThemeMode
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {

    lateinit var binding: ActivityMainBinding

    private val viewModel: MainActivityViewModel by viewModels()
    private val navigation: NavigationViewModel by viewModels()
    @Inject lateinit var navigator: AppNavigator

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        binding = ActivityMainBinding.inflate(layoutInflater)

        setContentView(binding.root)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            // The root already reserves system-bar space. Do not let Material views
            // add it again; keep other inset types (such as the IME) available.
            WindowInsetsCompat.Builder(insets)
                .setInsets(WindowInsetsCompat.Type.systemBars(), Insets.NONE)
                .setInsetsIgnoringVisibility(WindowInsetsCompat.Type.systemBars(), Insets.NONE)
                .build()
        }

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { state ->
                    binding.progressLoading.isVisible = state.isLoading
                    applyTheme(state.themeMode)
                }
            }
        }

        val host = supportFragmentManager.findFragmentById(R.id.main_container) as NavHostFragment
        navigator.attachRoot(host.navController)
        navigation.start(intent.takeIf { it.action == Intent.ACTION_VIEW }?.dataString, savedInstanceState != null)
        if (savedInstanceState != null && host.navController.currentDestination?.id in setOf(R.id.main_screen, R.id.detail, R.id.search)) {
            navigation.checkRestoredSession()
        }
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.RESUMED) {
                launch {
                    combine(navigation.command, viewModel.uiState) { command, state ->
                        command.takeUnless { state.isLoading }
                    }.collect { command ->
                        if (command != null) {
                            when (command) {
                                "BACK" -> navigator.back()
                                "BACK_TO_LOGIN" -> navigator.backToLogin()
                                "RESET_LOGIN" -> navigator.resetToLogin()
                                else -> navigator.navigate(AppDestination.decode(command))
                            }
                            navigation.consumed(command)
                        }
                    }
                }
                launch {
                    navigation.invalidLink.collect { invalid ->
                        if (invalid) {
                            Toast.makeText(this@MainActivity, R.string.unsupported_link, Toast.LENGTH_SHORT).show()
                            navigation.invalidLinkShown()
                        }
                    }
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        if (intent.action == Intent.ACTION_VIEW) {
            intent.dataString?.let(navigation::handleLink)
        }
    }

    override fun onDestroy() {
        navigator.detachRoot()
        super.onDestroy()
    }

    private fun applyTheme(themeMode: ThemeMode) {
        val mode = when (themeMode) {
            ThemeMode.SYSTEM -> AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM
            ThemeMode.LIGHT -> AppCompatDelegate.MODE_NIGHT_NO
            ThemeMode.DARK -> AppCompatDelegate.MODE_NIGHT_YES
        }
        AppCompatDelegate.setDefaultNightMode(mode)
    }
}
