package com.truongngo.moviedb.presenter.main

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.core.view.isVisible
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.NavController
import androidx.navigation.fragment.NavHostFragment
import com.truongngo.moviedb.R
import com.truongngo.moviedb.databinding.FragmentMainBinding
import com.truongngo.moviedb.presenter.MainActivityViewModel
import com.truongngo.moviedb.presenter.navigation.AppDestination
import com.truongngo.moviedb.presenter.navigation.AppNavigator
import com.truongngo.moviedb.presenter.navigation.NavigationViewModel
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import kotlinx.coroutines.launch

@AndroidEntryPoint
class MainFragment : Fragment() {
    @Inject lateinit var navigator: AppNavigator
    private val navigation: NavigationViewModel by activityViewModels()
    private val activityViewModel: MainActivityViewModel by activityViewModels()
    private var _binding: FragmentMainBinding? = null
    private val binding get() = _binding!!
    private var controller: NavController? = null
    private val destinationListener = NavController.OnDestinationChangedListener { _, destination, _ ->
        _binding?.bottomNavigation?.menu?.findItem(destination.id)?.isChecked = true
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentMainBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val host = childFragmentManager.findFragmentById(R.id.main_fragment_container) as NavHostFragment
        controller = host.navController
        host.navController.addOnDestinationChangedListener(destinationListener)
        navigator.attachMain(host.navController)

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                activityViewModel.isConnected.collect { connected ->
                    binding.disconnectMessage.isVisible = connected == false
                }
            }
        }

        binding.bottomNavigation.setOnItemSelectedListener { item ->
            val destination = when (item.itemId) {
                R.id.home -> AppDestination.HOME
                R.id.settings -> AppDestination.SETTINGS
                else -> null
            }
            if (destination != null) {
                navigation.navigate(destination)
                true
            } else {
                false
            }
        }
    }

    override fun onDestroyView() {
        controller?.let {
            it.removeOnDestinationChangedListener(destinationListener)
            navigator.detachMain(it)
        }
        controller = null
        binding.bottomNavigation.setOnItemSelectedListener(null)
        _binding = null
        super.onDestroyView()
    }
}
