package com.truongngo.moviedb.presenter.search

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import androidx.core.view.isVisible
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.core.widget.doAfterTextChanged
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.truongngo.moviedb.presenter.common.messageResource
import com.truongngo.moviedb.R
import com.truongngo.moviedb.databinding.FragmentSearchBinding
import com.truongngo.moviedb.presenter.navigation.AppDestination
import com.truongngo.moviedb.presenter.navigation.NavigationViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class SearchFragment : Fragment() {
    private val viewModel: SearchViewModel by viewModels()
    private val navigation: NavigationViewModel by activityViewModels()
    private var _binding: FragmentSearchBinding? = null
    private val binding get() = _binding!!
    private var resultsAdapter: SearchAdapter? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentSearchBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val adapter = SearchAdapter {
            hideKeyboard()
            viewModel.onEvent(SearchEvent.MovieClicked(it))
        }
        resultsAdapter = adapter
        binding.results.layoutManager = LinearLayoutManager(requireContext())
        binding.results.adapter = adapter
        binding.results.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                if (dy > 0 && (recyclerView.layoutManager as LinearLayoutManager).findLastVisibleItemPosition() >= adapter.itemCount - 3) {
                    viewModel.onEvent(SearchEvent.LoadMore)
                }
            }
        })
        binding.query.setText(viewModel.stateFlow.value.query)
        binding.query.setSelection(binding.query.length())
        binding.query.doAfterTextChanged { viewModel.onEvent(SearchEvent.QueryChanged(it.toString())) }
        binding.query.setOnEditorActionListener { _, action, _ ->
            if (action == EditorInfo.IME_ACTION_SEARCH) {
                viewModel.onEvent(SearchEvent.Submit)
                hideKeyboard()
                true
            } else false
        }
        binding.clear.setOnClickListener { binding.query.text?.clear() }
        binding.back.setOnClickListener { hideKeyboard(); viewModel.onEvent(SearchEvent.BackClicked) }
        binding.retry.setOnClickListener { viewModel.onEvent(SearchEvent.Retry) }
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch { viewModel.stateFlow.collect(::render) }
                launch { viewModel.effectFlow.collect { effect ->
                    when (effect) {
                        SearchEffect.NavigateBack -> navigation.back()
                        is SearchEffect.NavigateDetail -> navigation.navigate(AppDestination.Detail(effect.movieId))
                    }
                } }
            }
        }
        if (savedInstanceState == null && viewModel.stateFlow.value.query.isEmpty()) {
            binding.query.requestFocus()
            binding.query.post {
                _binding?.query?.let { WindowInsetsControllerCompat(requireActivity().window, it).show(WindowInsetsCompat.Type.ime()) }
            }
        }
    }

    private fun hideKeyboard() {
        WindowInsetsControllerCompat(requireActivity().window, binding.query).hide(WindowInsetsCompat.Type.ime())
        binding.query.clearFocus()
    }

    private fun render(state: SearchState) {
        resultsAdapter?.submitList(state.movies)
        binding.clear.isVisible = state.query.isNotEmpty()
        binding.loading.isVisible = state.isLoading && state.movies.isEmpty()
        binding.moreLoading.isVisible = state.isLoading && state.movies.isNotEmpty()
        binding.retry.isVisible = state.error != null && !state.isLoading
        binding.status.isVisible = !state.isLoading && (state.movies.isEmpty() || state.error != null)
        binding.status.setText(when {
            state.query.isBlank() -> R.string.search_prompt
            state.error != null -> state.error.messageResource()
            else -> R.string.search_empty
        })
    }

    override fun onDestroyView() {
        binding.results.clearOnScrollListeners()
        binding.results.adapter = null
        resultsAdapter = null
        _binding = null
        super.onDestroyView()
    }
}
