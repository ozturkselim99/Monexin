package com.selimozturk.monexin.ui.fragments.profile.view

import android.app.AlertDialog
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import com.selimozturk.monexin.databinding.ClearDataDialogBinding
import com.selimozturk.monexin.databinding.FragmentProfileBinding
import com.selimozturk.monexin.databinding.SignOutDialogBinding
import com.selimozturk.monexin.ui.activities.IntroActivity
import com.selimozturk.monexin.ui.fragments.auth.viewmodel.AuthViewModel
import com.selimozturk.monexin.ui.fragments.profile.viewmodel.ProfileViewModel
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

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FragmentProfileBinding.inflate(inflater, container, false)
        initViews()
        return binding.root
    }

    private fun initViews() {
        getProfileInfo()
        clearAppData()
        signOut()
    }

    private fun getProfileInfo() {
        profileViewModel.getProfileInfo()
        profileViewModel.profileState.observe(viewLifecycleOwner) {
            when (it) {
                is Resource.Success -> {
                    binding.profileProgressBar.setVisible(false)
                    binding.profileAccountNumberText.text = it.result.accountNumber
                    binding.profileNameText.text = it.result.name
                    binding.profileEmailText.text = it.result.email
                    binding.profileDeviceText.text = it.result.deviceName
                    binding.profileJoinedDateText.text = it.result.joinedData
                }
                is Resource.Loading -> {
                    binding.profileProgressBar.setVisible(true)
                }
                is Resource.Failure -> {
                    context?.showToast(it.exception.message.toString())
                    binding.profileProgressBar.setVisible(false)
                }
            }
        }
    }

    private fun signOut() {
        binding.signOutButton.setOnClickListener {
            val dialogBinding: SignOutDialogBinding =
                SignOutDialogBinding.inflate(layoutInflater)
            val builder = AlertDialog.Builder(requireContext()).setView(dialogBinding.root).show()
            dialogBinding.signOutButton.setOnClickListener {
                authViewModel.signout()
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
    }

    private fun clearAppData() {
        binding.profileClearAppDataButton.setOnClickListener {
            val dialogBinding: ClearDataDialogBinding =
                ClearDataDialogBinding.inflate(layoutInflater)
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

}