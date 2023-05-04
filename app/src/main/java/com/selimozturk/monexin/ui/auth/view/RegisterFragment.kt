package com.selimozturk.monexin.ui.auth.view

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.selimozturk.monexin.R
import com.selimozturk.monexin.databinding.FragmentRegisterBinding
import com.selimozturk.monexin.ui.auth.viewmodel.AuthViewModel
import com.selimozturk.monexin.utils.Resource
import com.selimozturk.monexin.utils.setVisible
import com.selimozturk.monexin.utils.showToast
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class RegisterFragment : Fragment() {

    private lateinit var binding: FragmentRegisterBinding
    private val authViewModel by viewModels<AuthViewModel>()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        binding = FragmentRegisterBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initViews()
        setupObservers()
    }

    private fun initViews() = with(binding) {
        loginButton.setOnClickListener {
            findNavController().navigate(R.id.action_registerFragment_to_loginFragment)
        }
        registerBackButton.setOnClickListener {
            findNavController().popBackStack()
        }
        registerButton.setOnClickListener {
            register()
        }
    }

    private fun register() = with(binding) {
        authViewModel.register(
            fullNameInput.text.toString(), emailInput.text.toString(), passwordInput.text.toString()
        )
    }

    private fun setupObservers() = with(binding) {
        authViewModel.registerState.observe(viewLifecycleOwner) { state ->
            when (state) {
                is Resource.Success -> {
                    context?.showToast("Successfully Registered")
                    registerProgressBar.setVisible(false)
                    findNavController().popBackStack()
                }
                is Resource.Loading -> {
                    registerProgressBar.setVisible(true)
                }
                is Resource.Failure -> {
                    context?.showToast(state.exception.message.toString())
                    registerProgressBar.setVisible(false)
                }
            }
        }
    }

}