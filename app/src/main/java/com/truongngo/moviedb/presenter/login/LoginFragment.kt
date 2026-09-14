package com.truongngo.moviedb.presenter.login

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.widget.Toast
import androidx.core.widget.doAfterTextChanged
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.truongngo.moviedb.R
import com.truongngo.moviedb.databinding.FragmentLoginBinding
import com.truongngo.moviedb.presenter.MainActivityViewModel
import com.truongngo.moviedb.presenter.enum.LoadStatus
import com.truongngo.moviedb.presenter.navigation.NavigationViewModel
import com.truongngo.moviedb.presenter.navigation.AppDestination
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class LoginFragment : Fragment() {
    private val viewModel: LoginViewModel by viewModels()
    private val navigation: NavigationViewModel by activityViewModels()
    private val mainActivityViewModel: MainActivityViewModel by activityViewModels()
    private var _binding: FragmentLoginBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentLoginBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.etEmail.doAfterTextChanged {
            viewModel.onEvent(LoginEvent.EmailChanged(it.toString()))
        }
        binding.etPassword.doAfterTextChanged {
            viewModel.onEvent(LoginEvent.PasswordChanged(it.toString()))
        }
        binding.cbRememberMe.setOnCheckedChangeListener { _, checked ->
            viewModel.onEvent(LoginEvent.RememberMeChanged(checked))
        }
        binding.btnLogin.setOnClickListener { viewModel.onEvent(LoginEvent.LoginClicked) }
        binding.etPassword.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                viewModel.onEvent(LoginEvent.LoginClicked)
                true
            } else false
        }
        binding.tvSignup.setOnClickListener { viewModel.onEvent(LoginEvent.SignupClicked) }

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch { viewModel.stateFlow.collect(::render) }
                launch {
                    viewModel.uiEffect.collect { effect ->
                        when (effect) {
                            LoginEffect.ShowLoginError -> Toast.makeText(
                                requireContext(), R.string.login_failed, Toast.LENGTH_LONG
                            ).show()
                            LoginEffect.NavigateHome -> navigation.authenticated()
                            LoginEffect.NavigateSignup -> navigation.navigate(AppDestination.SIGNUP)
                        }
                    }
                }
            }
        }
    }

    private fun render(state: LoginState) {
        if (binding.etEmail.text.toString() != state.email) binding.etEmail.setText(state.email)
        if (binding.etPassword.text.toString() != state.password) binding.etPassword.setText(state.password)
        binding.cbRememberMe.isChecked = state.rememberMe
        binding.tilEmail.error = if (state.emailError) getString(R.string.login_invalid_email) else null
        binding.tilPassword.error = if (state.passwordError) getString(R.string.login_required_password) else null
        val loading = state.loadStatus == LoadStatus.LOADING
        binding.etEmail.isEnabled = !loading
        binding.tilPassword.isEnabled = !loading
        binding.cbRememberMe.isEnabled = !loading
        binding.btnLogin.isEnabled = !loading
        binding.tvSignup.isEnabled = !loading
        if (loading) mainActivityViewModel.showLoading() else mainActivityViewModel.hideLoading()
    }

    override fun onStop() {
        mainActivityViewModel.hideLoading()
        super.onStop()
    }

    override fun onDestroyView() {
        _binding = null
        super.onDestroyView()
    }
}
