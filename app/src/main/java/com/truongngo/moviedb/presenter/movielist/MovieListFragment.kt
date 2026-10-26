package com.truongngo.moviedb.presenter.movielist

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.paging.CombinedLoadStates
import androidx.paging.LoadState
import androidx.recyclerview.widget.LinearLayoutManager
import com.truongngo.moviedb.R
import com.truongngo.moviedb.databinding.FragmentMovieListBinding
import com.truongngo.moviedb.domain.model.HomeFeed
import com.truongngo.moviedb.domain.model.MovieLoadError
import com.truongngo.moviedb.domain.model.MoviePagingException
import com.truongngo.moviedb.presenter.navigation.AppDestination
import com.truongngo.moviedb.presenter.navigation.NavigationViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

@AndroidEntryPoint
class MovieListFragment : Fragment() {
    private val viewModel: MovieListViewModel by viewModels()
    private val navigation: NavigationViewModel by activityViewModels()
    private var _binding: FragmentMovieListBinding? = null
    private var movieAdapter: MovieListAdapter? = null
    private var loadStates: CombinedLoadStates? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentMovieListBinding.inflate(inflater, container, false)
        return _binding!!.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val binding = _binding!!
        val adapter = MovieListAdapter { viewModel.onEvent(MovieListEvent.MovieClicked(it)) }
        movieAdapter = adapter
        binding.title.setText(when (viewModel.feed) {
            HomeFeed.POPULAR -> R.string.home_popular
            HomeFeed.TOP_RATED -> R.string.home_top_rated
            HomeFeed.UPCOMING -> R.string.home_upcoming
            HomeFeed.NOW_PLAYING -> error("Unsupported movie list")
        })
        binding.movies.layoutManager = LinearLayoutManager(requireContext())
        binding.movies.adapter = adapter
        binding.back.setOnClickListener { viewModel.onEvent(MovieListEvent.BackClicked) }
        binding.retry.setOnClickListener { adapter.retry() }
        binding.refresh.setOnRefreshListener {
            if (loadStates?.refresh !is LoadState.Loading) adapter.refresh()
        }
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch { viewModel.movies.collectLatest { adapter.submitData(it) } }
                launch { adapter.loadStateFlow.collect { loadStates = it; render() } }
                launch { adapter.onPagesUpdatedFlow.collect { render() } }
                launch { viewModel.effectFlow.collect { effect ->
                    when (effect) {
                        MovieListEffect.NavigateBack -> navigation.back()
                        is MovieListEffect.NavigateDetail -> navigation.navigate(AppDestination.Detail(effect.movieId))
                    }
                } }
            }
        }
    }

    private fun render() {
        val binding = _binding ?: return
        val states = loadStates ?: return
        val empty = movieAdapter?.itemCount == 0
        val error = (states.refresh as? LoadState.Error) ?: (states.append as? LoadState.Error)
        val refreshing = states.refresh is LoadState.Loading
        binding.refresh.isRefreshing = refreshing
        binding.moreLoading.isVisible = states.append is LoadState.Loading
        binding.retry.isVisible = error != null && !refreshing
        binding.status.isVisible = error != null || (empty && !refreshing)
        binding.status.setText(if (error == null) R.string.home_empty else when ((error.error as? MoviePagingException)?.reason) {
            MovieLoadError.CONNECTION -> R.string.home_error_connection
            MovieLoadError.AUTHENTICATION -> R.string.home_error_auth
            else -> R.string.home_error_general
        })
    }

    override fun onDestroyView() {
        _binding?.apply {
            refresh.setOnRefreshListener(null)
            retry.setOnClickListener(null)
            back.setOnClickListener(null)
            movies.adapter = null
        }
        movieAdapter = null
        loadStates = null
        _binding = null
        super.onDestroyView()
    }
}
