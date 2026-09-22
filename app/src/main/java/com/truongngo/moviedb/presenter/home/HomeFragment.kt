package com.truongngo.moviedb.presenter.home

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import com.truongngo.moviedb.presenter.navigation.AppDestination
import com.truongngo.moviedb.presenter.navigation.NavigationViewModel
import androidx.hilt.navigation.fragment.hiltNavGraphViewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager2.widget.ViewPager2
import com.truongngo.moviedb.presenter.common.messageResource
import com.truongngo.moviedb.R
import com.truongngo.moviedb.databinding.FragmentHomeBinding
import com.truongngo.moviedb.databinding.LayoutHomeSectionBinding
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class HomeFragment : Fragment() {
    companion object {
        fun newInstance() = HomeFragment()
        private const val BANNER_INDEX = "home_banner_index"
    }

    private val navigation: NavigationViewModel by activityViewModels()
    private val viewModel: HomeViewModel by hiltNavGraphViewModels(R.id.main_navigation)
    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!
    private var bannerAdapter: HomeBannerAdapter? = null
    private val sectionAdapters = mutableMapOf<MovieSection, HomeMovieAdapter>()
    private var bannerIndex = 0
    private var renderedRefreshVersion: Int? = null

    private val pageCallback = object : ViewPager2.OnPageChangeCallback() {
        override fun onPageSelected(position: Int) {
            val size = bannerAdapter?.movies?.size ?: 0
            if (size > 0) bannerIndex = BannerPages.movieIndex(position, size)
            updatePageIndicator()
        }

        override fun onPageScrollStateChanged(state: Int) {
            val pager = _binding?.nowPlayingPager ?: return
            val size = bannerAdapter?.movies?.size ?: return
            if (state == ViewPager2.SCROLL_STATE_IDLE && size > 1) {
                val target = BannerPages.settledPosition(pager.currentItem, size)
                if (target != pager.currentItem) pager.setCurrentItem(target, false)
            }
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.searchInput.setOnClickListener { viewModel.onEvent(HomeEvent.SearchClicked) }
        bannerIndex = savedInstanceState?.getInt(BANNER_INDEX) ?: bannerIndex
        bannerAdapter = HomeBannerAdapter { viewModel.onEvent(HomeEvent.MovieClicked(it.id)) }
        binding.nowPlayingPager.apply {
            adapter = bannerAdapter
            // The real movie index is restored explicitly, not the sentinel adapter position.
            isSaveEnabled = false
            offscreenPageLimit = 1
            registerOnPageChangeCallback(pageCallback)
        }
        setupSection(MovieSection.POPULAR, binding.popular, R.string.home_popular)
        setupSection(MovieSection.TOP_RATED, binding.topRated, R.string.home_top_rated)
        setupSection(MovieSection.UPCOMING, binding.upcoming, R.string.home_upcoming)
        binding.refresh.setOnChildScrollUpCallback { _, _ -> binding.scroll.canScrollVertically(-1) }
        binding.refresh.setOnRefreshListener { viewModel.onEvent(HomeEvent.Refresh) }
        binding.bannerRetry.setOnClickListener { viewModel.onEvent(HomeEvent.RetryNowPlaying) }
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch { viewModel.stateFlow.collect(::render) }
                launch {
                    viewModel.effectFlow.collect { effect ->
                        when (effect) {
                            HomeEffect.NavigateSearch -> navigation.navigate(AppDestination.SEARCH)
                            is HomeEffect.NavigateDetail -> navigation.navigate(AppDestination.Detail(effect.movieId))
                        }
                    }
                }
            }
        }
    }

    private fun setupSection(section: MovieSection, views: LayoutHomeSectionBinding, title: Int) {
        views.title.setText(title)
        val adapter = HomeMovieAdapter { viewModel.onEvent(HomeEvent.MovieClicked(it.id)) }
        sectionAdapters[section] = adapter
        views.movies.layoutManager = LinearLayoutManager(requireContext(), RecyclerView.HORIZONTAL, false)
        views.movies.adapter = adapter
        views.movies.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                if (dx == 0) return
                val manager = recyclerView.layoutManager as LinearLayoutManager
                if (manager.findLastVisibleItemPosition() >= adapter.itemCount - 4) {
                    viewModel.onEvent(HomeEvent.LoadMore(section))
                }
            }
        })
        views.action.setOnClickListener {
            val state = viewModel.stateFlow.value.sections.getValue(section)
            viewModel.onEvent(if (state.error != null) HomeEvent.RetrySection(section) else HomeEvent.LoadMore(section))
        }
    }

    private fun render(state: HomeState) {
        binding.refresh.isRefreshing = state.isRefreshing
        val adapter = bannerAdapter ?: return
        val oldId = adapter.movies.getOrNull(bannerIndex)?.id
        val previousIndex = bannerIndex
        if (adapter.submitMovies(state.nowPlaying)) {
            val index = state.nowPlaying.indexOfFirst { it.id == oldId }.takeIf { it >= 0 }
                ?: previousIndex.coerceIn(0, (state.nowPlaying.size - 1).coerceAtLeast(0))
            bannerIndex = index
            binding.nowPlayingPager.setCurrentItem(if (state.nowPlaying.size > 1) index + 1 else 0, false)
        }
        binding.nowPlayingPager.isVisible = state.nowPlaying.isNotEmpty()
        binding.bannerLoading.isVisible = state.isNowPlayingLoading && state.nowPlaying.isEmpty()
        binding.bannerRetry.isVisible = state.nowPlayingError != null && !state.isNowPlayingLoading
        binding.bannerStatus.isVisible = state.nowPlayingError != null || (!state.isNowPlayingLoading && state.nowPlaying.isEmpty())
        binding.bannerStatus.setText(state.nowPlayingError?.messageResource() ?: R.string.home_empty)
        updatePageIndicator()
        val refreshed = renderedRefreshVersion != null && renderedRefreshVersion != state.refreshVersion
        renderSection(MovieSection.POPULAR, binding.popular, state, refreshed)
        renderSection(MovieSection.TOP_RATED, binding.topRated, state, refreshed)
        renderSection(MovieSection.UPCOMING, binding.upcoming, state, refreshed)
        renderedRefreshVersion = state.refreshVersion
    }

    private fun renderSection(section: MovieSection, views: LayoutHomeSectionBinding, state: HomeState, refreshed: Boolean) {
        val list = state.sections.getValue(section)
        sectionAdapters.getValue(section).submitList(list.movies) {
            if (refreshed && list.error == null) views.movies.scrollToPosition(0)
        }
        views.movies.isVisible = list.movies.isNotEmpty()
        views.skeleton.isVisible = list.isLoading && list.movies.isEmpty()
        views.loading.isVisible = list.isLoading && list.movies.isNotEmpty()
        views.status.setText(when {
            list.error != null -> list.error.messageResource()
            list.isLoading -> R.string.home_loading
            list.movies.isEmpty() -> R.string.home_empty
            !list.canLoadMore -> R.string.home_end
            else -> R.string.home_swipe_more
        })
        views.action.isVisible = list.error != null || (list.canLoadMore && list.movies.isNotEmpty())
        views.action.isEnabled = !list.isLoading && !state.isRefreshing
        views.action.setText(if (list.error != null) R.string.home_retry else R.string.home_load_more)
    }

    private fun updatePageIndicator() {
        val views = _binding ?: return
        val count = bannerAdapter?.movies?.size ?: 0
        views.pageIndicator.isVisible = count > 0
        if (count > 0) views.pageIndicator.text = getString(R.string.home_page, bannerIndex + 1, count)
    }

    override fun onSaveInstanceState(outState: Bundle) {
        outState.putInt(BANNER_INDEX, bannerIndex)
        super.onSaveInstanceState(outState)
    }

    override fun onDestroyView() {
        binding.nowPlayingPager.unregisterOnPageChangeCallback(pageCallback)
        binding.nowPlayingPager.adapter = null
        listOf(binding.popular, binding.topRated, binding.upcoming).forEach {
            it.movies.clearOnScrollListeners()
            it.movies.adapter = null
        }
        sectionAdapters.clear()
        bannerAdapter = null
        renderedRefreshVersion = null
        _binding = null
        super.onDestroyView()
    }
}
