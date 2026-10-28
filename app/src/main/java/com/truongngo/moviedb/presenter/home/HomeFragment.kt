package com.truongngo.moviedb.presenter.home

import android.os.Bundle
import android.os.Parcelable
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
import androidx.recyclerview.widget.ConcatAdapter
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
    private val sectionScrollStates = mutableMapOf<MovieSection, Parcelable>()

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
        MovieSection.entries.forEach { section ->
            savedInstanceState?.getParcelable("home_scroll_${section.name}", Parcelable::class.java)?.let {
                sectionScrollStates[section] = it
            }
        }
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
                            is HomeEffect.NavigateMovieList -> navigation.navigate(AppDestination.MovieList(effect.feed))
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
        // Included section layouts share the same RecyclerView ID; restore each feed explicitly.
        views.movies.isSaveEnabled = false
        views.movies.layoutManager = LinearLayoutManager(requireContext(), RecyclerView.HORIZONTAL, false)
        val seeMore = { viewModel.onEvent(HomeEvent.SeeMoreClicked(section)) }
        val description = getString(R.string.home_see_more_category, getString(title))
        views.seeMore.contentDescription = description
        views.seeMore.setOnClickListener { seeMore() }
        views.movies.adapter = ConcatAdapter(adapter, HomeSeeMoreAdapter(description, seeMore))
        views.action.setOnClickListener { viewModel.onEvent(HomeEvent.RetrySection(section)) }
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
        MovieSection.entries.forEach { renderSection(it, state.sections.getValue(it)) }
    }

    private fun renderSection(section: MovieSection, state: HomeSectionState) {
        val views = when (section) {
            MovieSection.POPULAR -> binding.popular
            MovieSection.TOP_RATED -> binding.topRated
            MovieSection.UPCOMING -> binding.upcoming
        }
        val adapter = sectionAdapters.getValue(section)
        val firstContent = adapter.currentList.isEmpty() && state.movies.isNotEmpty()
        adapter.submitList(state.movies) {
            if (_binding == null || sectionAdapters[section] !== adapter) return@submitList
            if (firstContent) {
                val restored = sectionScrollStates.remove(section)
                if (restored != null) views.movies.layoutManager?.onRestoreInstanceState(restored)
                else views.movies.scrollToPosition(0)
                // Without an explicit initial anchor, the existing See more item moves to the end
                // when movies are inserted before it, taking the viewport along with it.
            }
        }
        views.movies.isVisible = !state.isLoading || state.movies.isNotEmpty()
        views.skeleton.isVisible = state.isLoading && state.movies.isEmpty()
        views.loading.isVisible = state.isLoading && state.movies.isNotEmpty()
        views.footer.isVisible = !state.isLoading && (state.error != null || state.movies.isEmpty())
        views.status.setText(state.error?.messageResource() ?: R.string.home_empty)
        views.action.isVisible = state.error != null
    }

    private fun updatePageIndicator() {
        val views = _binding ?: return
        val count = bannerAdapter?.movies?.size ?: 0
        views.pageIndicator.isVisible = count > 0
        if (count > 0) views.pageIndicator.text = getString(R.string.home_page, bannerIndex + 1, count)
    }

    override fun onSaveInstanceState(outState: Bundle) {
        outState.putInt(BANNER_INDEX, bannerIndex)
        saveSectionScrollStates()
        sectionScrollStates.forEach { (section, state) -> outState.putParcelable("home_scroll_${section.name}", state) }
        super.onSaveInstanceState(outState)
    }

    private fun saveSectionScrollStates() {
        val views = _binding ?: return
        val rows = mapOf(MovieSection.POPULAR to views.popular, MovieSection.TOP_RATED to views.topRated,
            MovieSection.UPCOMING to views.upcoming)
        rows.forEach { (section, row) ->
            if ((sectionAdapters[section]?.itemCount ?: 0) > 0) {
                row.movies.layoutManager?.onSaveInstanceState()?.let { sectionScrollStates[section] = it }
            }
        }
    }

    override fun onDestroyView() {
        saveSectionScrollStates()
        binding.refresh.setOnRefreshListener(null)
        binding.refresh.setOnChildScrollUpCallback(null)
        binding.searchInput.setOnClickListener(null)
        binding.bannerRetry.setOnClickListener(null)
        binding.nowPlayingPager.unregisterOnPageChangeCallback(pageCallback)
        binding.nowPlayingPager.adapter = null
        listOf(binding.popular, binding.topRated, binding.upcoming).forEach {
            it.seeMore.setOnClickListener(null)
            it.action.setOnClickListener(null)
            it.movies.clearOnScrollListeners()
            it.movies.adapter = null
        }
        sectionAdapters.clear()
        bannerAdapter = null
        _binding = null
        super.onDestroyView()
    }
}
