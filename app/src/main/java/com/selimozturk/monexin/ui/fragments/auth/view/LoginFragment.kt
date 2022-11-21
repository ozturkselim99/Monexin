package com.selimozturk.monexin.ui.fragments.auth.view

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
import com.selimozturk.monexin.ui.activities.MainActivity
import com.selimozturk.monexin.ui.fragments.auth.viewmodel.AuthViewModel
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
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentLoginBinding.inflate(inflater, container, false)
        initViews()
        return binding.root
    }

    private fun initViews() {
        binding.registerButton.setOnClickListener {
            findNavController().navigate(R.id.action_loginFragment_to_registerFragment)
        }
        binding.loginButton.setOnClickListener {
            login()
        }
    }

    private fun login() {
            authViewModel.login(
                binding.emailInput.text.toString(),
                binding.passwordInput.text.toString()
            )
            authViewModel.loginState.observe(viewLifecycleOwner) {
                when (it) {
                    is Resource.Success -> {
                        context?.showToast("Welcome")
                        binding.loginProgressBar.setVisible(false)
                        sessionManager.setLogin(true)
                        val intent = Intent(requireContext(), MainActivity::class.java)
                        startActivity(intent)
                        activity?.finish()
                    }
                    is Resource.Loading -> {
                        binding.loginProgressBar.setVisible(true)
                    }
                    is Resource.Failure -> {
                        context?.showToast(it.exception.message.toString())
                        binding.loginProgressBar.setVisible(false)
                    }
                }
            }
    }

}