package com.selimozturk.monexin.ui.auth.view

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.selimozturk.monexin.R
import com.selimozturk.monexin.databinding.FragmentLoginBinding
import com.selimozturk.monexin.ui.MainActivity
import com.selimozturk.monexin.ui.auth.viewmodel.AuthViewModel
import com.selimozturk.monexin.utils.Resource
import com.selimozturk.monexin.utils.SessionManager
import com.selimozturk.monexin.utils.setVisible
import com.selimozturk.monexin.utils.showToast
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class LoginFragment : Fragment() {

    @Inject
    lateinit var sessionManager: SessionManager
    private lateinit var binding: FragmentLoginBinding
    private val authViewModel by viewModels<AuthViewModel>()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        binding = FragmentLoginBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initViews()
        setupObservers()
    }

    private fun initViews() = with(binding) {
        registerButton.setOnClickListener {
            findNavController().navigate(R.id.action_loginFragment_to_registerFragment)
        }
        loginButton.setOnClickListener {
            login()
        }
    }

    private fun login() = with(binding) {
        authViewModel.login(
            emailInput.text.toString(), passwordInput.text.toString()
        )
    }

    private fun setupObservers() = with(binding) {
        authViewModel.loginState.observe(viewLifecycleOwner) { state ->
            when (state) {
                is Resource.Success -> {
                    context?.showToast("Welcome")
                    loginProgressBar.setVisible(false)
                    sessionManager.setLogin(true)
                    val intent = Intent(requireContext(), MainActivity::class.java)
                    startActivity(intent)
                    requireActivity().finish()
                }
                is Resource.Loading -> {
                    loginProgressBar.setVisible(true)
                }
                is Resource.Failure -> {
                    context?.showToast(state.exception.message.toString())
                    loginProgressBar.setVisible(false)
                }
            }
        }
    }

}