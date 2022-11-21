package com.selimozturk.monexin.ui.fragments.auth.view

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.selimozturk.monexin.R
import com.selimozturk.monexin.databinding.FragmentRegisterBinding
import com.selimozturk.monexin.ui.fragments.auth.viewmodel.AuthViewModel
import com.selimozturk.monexin.utils.Resource
import com.selimozturk.monexin.utils.setVisible
import com.selimozturk.monexin.utils.showToast
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class RegisterFragment : Fragment() {

    private lateinit var binding: FragmentRegisterBinding
    private val authViewModel by viewModels<AuthViewModel>()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentRegisterBinding.inflate(inflater, container, false)
        initViews()
        return binding.root
    }

    private fun initViews(){
        binding.loginButton.setOnClickListener {
            findNavController().navigate(R.id.action_registerFragment_to_loginFragment)
        }
        binding.registerBackButton.setOnClickListener {
            findNavController().popBackStack()
        }
        binding.registerButton.setOnClickListener {
            register()
        }
    }

    private fun register(){
            authViewModel.register(binding.fullNameInput.text.toString(),binding.emailInput.text.toString(),binding.passwordInput.text.toString())
            authViewModel.registerState.observe(viewLifecycleOwner){
                when(it){
                    is Resource.Success ->{
                        context?.showToast("Successfully Registered")
                        binding.registerProgressBar.setVisible(false)
                        findNavController().popBackStack()
                    }
                    is Resource.Loading ->{
                        binding.registerProgressBar.setVisible(true)
                    }
                    is Resource.Failure ->{
                        context?.showToast(it.exception.message.toString())
                        binding.registerProgressBar.setVisible(false)
                    }
                }
            }
    }

}