package com.selimozturk.monexin.ui.profile.view

import android.app.AlertDialog
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import com.selimozturk.monexin.databinding.ClearDataDialogBinding
import com.selimozturk.monexin.databinding.FragmentProfileBinding
import com.selimozturk.monexin.databinding.SignOutDialogBinding
import com.selimozturk.monexin.ui.IntroActivity
import com.selimozturk.monexin.ui.MainViewModel
import com.selimozturk.monexin.ui.auth.viewmodel.AuthViewModel
import com.selimozturk.monexin.ui.profile.viewmodel.ProfileViewModel
import com.selimozturk.monexin.utils.Resource
import com.selimozturk.monexin.utils.SessionManager
import com.selimozturk.monexin.utils.setVisible
import com.selimozturk.monexin.utils.showToast
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class ProfileFragment : Fragment() {

    @Inject
    lateinit var sessionManager: SessionManager
    private lateinit var binding: FragmentProfileBinding
    private val authViewModel by viewModels<AuthViewModel>()
    private val profileViewModel by viewModels<ProfileViewModel>()
    private val mainViewModel: MainViewModel by activityViewModels()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        binding = FragmentProfileBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initViews()
        getProfileInfo()
        observeProfileReselected()
    }

    private fun observeProfileReselected() {
        mainViewModel.isProfileReselected.observe(viewLifecycleOwner) {
            binding.profileNestedScrollView.smoothScrollTo(0, 0)
        }
    }

    private fun initViews() = with(binding) {
        profileClearAppDataButton.setOnClickListener {
            clearAppData()
        }
        signOutButton.setOnClickListener {
            signOut()
        }
    }

    private fun getProfileInfo() = with(binding) {
        profileViewModel.getProfileInfo()
        profileViewModel.profileState.observe(viewLifecycleOwner) {state->
            when (state) {
                is Resource.Success -> {
                    profileProgressBar.setVisible(false)
                    profileAccountNumberText.text = state.result.accountNumber
                    profileNameText.text = state.result.name
                    profileEmailText.text = state.result.email
                    profileDeviceText.text = state.result.deviceName
                    profileJoinedDateText.text = state.result.joinedData
                }
                is Resource.Loading -> {
                    profileProgressBar.setVisible(true)
                }
                is Resource.Failure -> {
                    context?.showToast(state.exception.message.toString())
                    profileProgressBar.setVisible(false)
                }
            }
        }
    }

    private fun signOut() {
        val dialogBinding: SignOutDialogBinding = SignOutDialogBinding.inflate(layoutInflater)
        val builder = AlertDialog.Builder(requireContext()).setView(dialogBinding.root).show()
        dialogBinding.signOutButton.setOnClickListener {
            authViewModel.signOut()
            sessionManager.setLogin(false)
            builder.dismiss()
            val intent = Intent(requireContext(), IntroActivity::class.java)
            startActivity(intent)
            requireActivity().finish()
        }
        dialogBinding.signOutCancelButton.setOnClickListener {
            builder.dismiss()
        }
    }

    private fun clearAppData() {
        val dialogBinding: ClearDataDialogBinding = ClearDataDialogBinding.inflate(layoutInflater)
        val builder = AlertDialog.Builder(requireContext()).setView(dialogBinding.root).show()
        dialogBinding.clearTransactionClearButton.setOnClickListener {
            profileViewModel.clearAllTransaction()
            builder.dismiss()
        }
        dialogBinding.clearTransactionCancelButton.setOnClickListener {
            builder.dismiss()
        }
    }

}