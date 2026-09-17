package com.truongngo.moviedb.presenter.detail

import android.content.Intent
import android.os.Bundle
import com.truongngo.moviedb.domain.model.DownloadState
import com.truongngo.moviedb.domain.model.DownloadStatus
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.fragment.app.activityViewModels
import com.truongngo.moviedb.presenter.navigation.NavigationViewModel
import coil.load
import android.Manifest
import androidx.appcompat.app.AlertDialog
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.truongngo.moviedb.presenter.common.PermissionManager
import com.truongngo.moviedb.presenter.common.PermissionMessages
import com.truongngo.moviedb.presenter.common.RuntimePermission
import com.truongngo.moviedb.R
import com.truongngo.moviedb.data.network.utils.NetworkUtils
import com.truongngo.moviedb.databinding.FragmentDetailBinding
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class DetailFragment : Fragment() {
    private val downloadViewModel: DownloadViewModel by viewModels()
    private var downloadConfirmation: AlertDialog? = null

    private val downloadPermissions = PermissionManager(
        fragment = this,
        permissions = listOf(RuntimePermission(Manifest.permission.POST_NOTIFICATIONS, minSdk = 33)),
        messages = PermissionMessages(
            title = R.string.download_permission_title,
            rationale = R.string.download_permission_rationale,
            denied = R.string.download_permission_denied,
            blocked = R.string.download_permission_blocked,
        ),
    ) { result -> if (result.allGranted) startDownload() }
    private val navigation: NavigationViewModel by activityViewModels()
    private val viewModel: DetailViewModel by viewModels()
    private var _binding: FragmentDetailBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentDetailBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.download.setOnClickListener {
            if (downloadViewModel.stateFlow.value.isActive) downloadViewModel.onEvent(DownloadEvent.Cancel)
            else confirmDownload()
        }
        binding.share.setOnClickListener { viewModel.onEvent(DetailEvent.ShareClicked) }
        binding.back.setOnClickListener { viewModel.onEvent(DetailEvent.BackClicked) }
        binding.retry.setOnClickListener { viewModel.onEvent(DetailEvent.Retry) }
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch { viewModel.stateFlow.collect(::render) }
                launch { downloadViewModel.stateFlow.collect(::renderDownload) }
                launch {
                    viewModel.effectFlow.collect { effect ->
                        when (effect) {
                            DetailEffect.NavigateBack -> navigation.back()
                            is DetailEffect.ShareMovie -> startActivity(Intent.createChooser(
                                Intent(Intent.ACTION_SEND).apply {
                                    type = "text/plain"
                                    putExtra(Intent.EXTRA_TEXT, effect.text)
                                }, getString(R.string.detail_share),
                            ))
                        }
                    }
                }
            }
        }
    }

    private fun confirmDownload() {
        if (downloadConfirmation?.isShowing == true) return
        downloadConfirmation = MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.download_confirm_title)
            .setMessage(R.string.download_confirm_message)
            .setNegativeButton(R.string.permission_cancel, null)
            .setPositiveButton(R.string.download_confirm_allow) { _, _ -> downloadPermissions.request() }
            .show()
    }

    private fun startDownload() {
        viewModel.stateFlow.value.movie?.let { downloadViewModel.onEvent(DownloadEvent.Start(it.title)) }
    }

    private fun renderDownload(state: DownloadState) = with(binding) {
        download.setText(if (state.isActive) R.string.download_cancel else R.string.download_start)
        downloadProgress.isVisible = state.isActive
        downloadProgress.isIndeterminate = state.status == DownloadStatus.QUEUED
        downloadProgress.progress = state.percent
        downloadStatus.text = when (state.status) {
            DownloadStatus.IDLE -> getString(R.string.download_demo)
            DownloadStatus.QUEUED -> getString(R.string.download_queued)
            DownloadStatus.RUNNING -> getString(R.string.download_progress, state.percent)
            DownloadStatus.COMPLETED -> getString(R.string.download_complete)
            DownloadStatus.CANCELLED -> getString(R.string.download_cancelled)
            DownloadStatus.FAILED -> getString(R.string.download_failed)
        }
    }

    private fun render(state: DetailState): Unit = with(binding) {
        loading.isVisible = state.isLoading
        content.isVisible = state.movie != null
        share.isVisible = state.movie?.let { it.id > 0 || it.title.isNotBlank() } == true
        status.isVisible = state.error != null
        retry.isVisible = state.error != null && state.error != DetailError.INVALID_MOVIE
        status.setText(when (state.error) {
            DetailError.CONNECTION -> R.string.home_error_connection
            DetailError.AUTHENTICATION -> R.string.home_error_auth
            DetailError.INVALID_MOVIE -> R.string.detail_invalid_movie
            else -> R.string.home_error_general
        })
        state.movie?.let { movie ->
            title.text = movie.title
            originalTitle.text = movie.originalTitle
            originalTitle.isVisible = !movie.originalTitle.isNullOrBlank() && movie.originalTitle != movie.title
            metadata.text = getString(R.string.detail_metadata, movie.voteAverage, movie.voteCount,
                movie.releaseDate?.takeIf { it.isNotBlank() } ?: getString(R.string.detail_unknown))
            runtime.text = movie.runtime?.takeIf { it > 0 }?.let { getString(R.string.detail_runtime, it) }
            runtime.isVisible = !runtime.text.isNullOrBlank()
            genres.text = movie.genres.orEmpty().joinToString(" • ") { it.name }
            genres.isVisible = !genres.text.isNullOrBlank()
            overview.text = movie.overview?.takeIf { it.isNotBlank() } ?: getString(R.string.detail_no_overview)
            backdrop.load(NetworkUtils.imageUrl(movie.backdropPath ?: movie.posterPath, "w780")) {
                crossfade(true)
                placeholder(R.drawable.home_image_placeholder)
                error(R.drawable.home_image_placeholder)
                fallback(R.drawable.home_image_placeholder)
            }
            poster.load(NetworkUtils.imageUrl(movie.posterPath, "w342")) {
                placeholder(R.drawable.home_image_placeholder)
                error(R.drawable.home_image_placeholder)
                fallback(R.drawable.home_image_placeholder)
            }
        }
    }

    override fun onDestroyView() {
        downloadConfirmation?.dismiss()
        downloadConfirmation = null
        downloadPermissions.dismiss()
        binding.share.setOnClickListener(null)
        _binding = null
        super.onDestroyView()
    }
}
