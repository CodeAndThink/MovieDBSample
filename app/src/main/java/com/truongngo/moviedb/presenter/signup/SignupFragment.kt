package com.truongngo.moviedb.presenter.signup

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import androidx.activity.OnBackPressedCallback
import androidx.core.view.isVisible
import androidx.core.widget.doAfterTextChanged
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.truongngo.moviedb.R
import com.truongngo.moviedb.databinding.FragmentSignupBinding
import com.truongngo.moviedb.domain.auth.SignupException
import com.truongngo.moviedb.presenter.MainActivityViewModel
import com.truongngo.moviedb.presenter.enum.LoadStatus
import com.truongngo.moviedb.presenter.navigation.NavigationViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class SignupFragment : Fragment() {
    companion object {
        fun newInstance() = SignupFragment()
    }

    private val viewModel: SignupViewModel by viewModels()
    private val navigation: NavigationViewModel by activityViewModels()
    private val mainActivityViewModel: MainActivityViewModel by activityViewModels()
    private var _binding: FragmentSignupBinding? = null
    private val binding get() = _binding!!
    private var isRendering = false
    private var blockBackWhileSubmitting: OnBackPressedCallback? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSignupBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        blockBackWhileSubmitting = object : OnBackPressedCallback(false) {
            override fun handleOnBackPressed() = Unit
        }.also { requireActivity().onBackPressedDispatcher.addCallback(viewLifecycleOwner, it) }
        binding.etEmail.doAfterTextChanged {
            if (!isRendering) viewModel.onEvent(SignupEvent.EmailChanged(it.toString()))
        }
        binding.etPassword.doAfterTextChanged {
            if (!isRendering) viewModel.onEvent(SignupEvent.PasswordChanged(it.toString()))
        }
        binding.etConfirmPassword.doAfterTextChanged {
            if (!isRendering) viewModel.onEvent(SignupEvent.ConfirmPasswordChanged(it.toString()))
        }
        binding.btnSignup.setOnClickListener { viewModel.onEvent(SignupEvent.SignupClicked) }
        binding.btnLogin.setOnClickListener { viewModel.onEvent(SignupEvent.LoginClicked) }
        binding.etConfirmPassword.setOnEditorActionListener { _, action, _ ->
            if (action == EditorInfo.IME_ACTION_DONE) {
                viewModel.onEvent(SignupEvent.SignupClicked)
                true
            } else false
        }
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch { viewModel.stateFlow.collect(::render) }
                launch {
                    viewModel.uiEffect.collect { effect ->
                        when (effect) {
                            SignupEffect.NavigateHome -> navigation.authenticated()
                            SignupEffect.NavigateLogin -> navigation.login()
                        }
                    }
                }
            }
        }
    }

    private fun render(state: SignupState) {
        isRendering = true
        try {
            renderForm(state)
        } finally {
            isRendering = false
        }
    }

    private fun renderForm(state: SignupState) {
        if (binding.etEmail.text.toString() != state.email) binding.etEmail.setText(state.email)
        if (binding.etPassword.text.toString() != state.password) binding.etPassword.setText(state.password)
        if (binding.etConfirmPassword.text.toString() != state.confirmPassword) {
            binding.etConfirmPassword.setText(state.confirmPassword)
        }
        binding.tilEmail.error = if (state.emailError) getString(R.string.login_invalid_email) else null
        binding.tilPassword.error = if (state.passwordError) getString(R.string.signup_password_too_short) else null
        binding.tilConfirmPassword.error = if (state.confirmPasswordError) getString(R.string.signup_password_mismatch) else null
        binding.tvError.isVisible = state.error != null
        binding.tvError.text = state.error?.let {
            getString(when (it) {
                SignupException.Reason.EMAIL_IN_USE -> R.string.signup_email_in_use
                SignupException.Reason.WEAK_PASSWORD -> R.string.signup_weak_password
                SignupException.Reason.INVALID_EMAIL -> R.string.login_invalid_email
                SignupException.Reason.NETWORK -> R.string.signup_network_error
                SignupException.Reason.TOO_MANY_REQUESTS -> R.string.signup_too_many_requests
                SignupException.Reason.UNKNOWN -> R.string.signup_failed
            })
        }
        val loading = state.loadStatus == LoadStatus.LOADING
        val enabled = !loading && state.loadStatus != LoadStatus.SUCCESS
        binding.tilEmail.isEnabled = enabled
        binding.tilPassword.isEnabled = enabled
        binding.tilConfirmPassword.isEnabled = enabled
        binding.btnSignup.isEnabled = enabled
        binding.btnLogin.isEnabled = enabled
        blockBackWhileSubmitting?.isEnabled = !enabled
        if (loading) mainActivityViewModel.showLoading() else mainActivityViewModel.hideLoading()
    }

    override fun onStop() {
        mainActivityViewModel.hideLoading()
        super.onStop()
    }

    override fun onDestroyView() {
        blockBackWhileSubmitting = null
        _binding = null
        super.onDestroyView()
    }
}
