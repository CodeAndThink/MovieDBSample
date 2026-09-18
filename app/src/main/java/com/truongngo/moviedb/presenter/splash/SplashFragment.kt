package com.truongngo.moviedb.presenter.splash

import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import com.truongngo.moviedb.R
import com.truongngo.moviedb.databinding.FragmentSplashBinding

/** Startup-only graph entry. NavigationViewModel owns the delay and session check. */
class SplashFragment : Fragment(R.layout.fragment_splash) {
    private var binding: FragmentSplashBinding? = null

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding = FragmentSplashBinding.bind(view)
    }

    override fun onDestroyView() {
        binding = null
        super.onDestroyView()
    }
}
