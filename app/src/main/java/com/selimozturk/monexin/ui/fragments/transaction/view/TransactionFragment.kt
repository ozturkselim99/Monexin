package com.selimozturk.monexin.ui.fragments.transaction.view

import android.Manifest
import android.app.Activity
import android.app.Dialog
import android.content.Intent
import android.content.pm.PackageManager
import android.database.Cursor
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.MediaStore
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.RadioButton
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.net.toFile
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.bumptech.glide.Glide
import com.selimozturk.monexin.R
import com.selimozturk.monexin.databinding.FragmentTransactionBinding
import com.selimozturk.monexin.model.Transactions
import com.selimozturk.monexin.ui.fragments.transaction.viewmodel.TransactionViewModel
import com.selimozturk.monexin.utils.Resource
import com.selimozturk.monexin.utils.customIsEmpty
import com.selimozturk.monexin.utils.setVisible
import com.selimozturk.monexin.utils.showToast
import dagger.hilt.android.AndroidEntryPoint
import java.io.File


@AndroidEntryPoint
class TransactionFragment : Fragment() {

    private lateinit var binding: FragmentTransactionBinding
    private val transactionViewModel by viewModels<TransactionViewModel>()
    private val args: TransactionFragmentArgs by navArgs()
    private var photoUri: Uri? = null
    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            Log.i("monexin", "Permission granted")
        } else {
            Log.i("monexin", "Permission denied")
        }
    }
    private var resultLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                getSelectedImageFromGallery(result.data?.data)
            }
        }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentTransactionBinding.inflate(inflater, container, false)
        getTakenPhoto()
        initViews()
        deleteTakenPhoto()
        return binding.root
    }

    private fun requestGalleryPermission() {
        when {
            ContextCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.READ_EXTERNAL_STORAGE
            ) == PackageManager.PERMISSION_GRANTED -> {
                openGallery()
            }
            ActivityCompat.shouldShowRequestPermissionRationale(
                requireActivity(),
                Manifest.permission.READ_EXTERNAL_STORAGE
            ) -> Log.i("monexin", "Show gallery permission dialog")
            else -> requestPermissionLauncher.launch(Manifest.permission.READ_EXTERNAL_STORAGE)
        }
    }

    private fun getTakenPhoto() {
        findNavController().currentBackStackEntry?.savedStateHandle?.getLiveData<Uri>("Uri")
            ?.observe(
                viewLifecycleOwner
            ) { uri ->
                binding.takenPhotoLayout.setVisible(true)
                binding.transactionAttachmentLayout.visibility = View.GONE
                photoUri = uri
                Glide.with(requireContext())
                    .load(uri)
                    .into(binding.takenPhoto)
            }
    }

    private fun initViews() {
        binding.transactionBackButton.setOnClickListener {
            findNavController().popBackStack()
        }
        if (args.transactionUpdate != null) {
            binding.addTransactionButton.text = "Update Transaction"
            bindTransactionToInputs()
            updateTransaction()
        } else {
            addTransaction()
        }
        binding.transactionCameraButton.setOnClickListener {
            findNavController().navigate(R.id.action_transactionFragment_to_cameraFragment)
        }
        binding.transactionImageButton.setOnClickListener {
            requestGalleryPermission()
        }
    }

    private fun openGallery() {
        val galleryIntent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
        resultLauncher.launch(galleryIntent)
    }

    private fun getSelectedImageFromGallery(uri: Uri?) {
        var filePath: String? = ""
        if (uri != null && "content" == uri.scheme) {
            val cursor: Cursor? = context?.contentResolver?.query(
                uri,
                arrayOf(MediaStore.Images.ImageColumns.DATA),
                null,
                null,
                null
            )
            cursor?.moveToFirst()
            if (cursor != null) {
                filePath = cursor.getString(0)
            }
            cursor?.close()
        } else {
            filePath = uri?.path
        }
        photoUri = Uri.fromFile(File(filePath))
        binding.transactionAttachmentLayout.visibility = View.GONE
        binding.takenPhotoLayout.setVisible(true)
        Glide.with(requireContext())
            .load(uri)
            .into(binding.takenPhoto)
    }

    private fun deleteTakenPhoto() {
        binding.takenPhotoDeleteButton.setOnClickListener {
            args.transactionUpdate?.photoPath = ""
            photoUri = null
            binding.transactionAttachmentLayout.setVisible(true)
            binding.takenPhotoLayout.visibility = View.GONE
        }
    }

    private fun addTransaction() {
        val dialog = Dialog(requireContext())
        binding.addTransactionButton.setOnClickListener {
            if (isInputsEmpty() && isRadioButtonChecked()) {
                val title = binding.transactionTitleInput.text.toString()
                val description = binding.transactionDescriptionInput.text.toString()
                val amount = binding.transactionInputAmount.text.toString().toDouble()
                val checkedRadioButtonId = binding.transactionTypeRadioGroup.checkedRadioButtonId
                val photoPath = if (photoUri != null) {
                    "images/" + photoUri?.toFile()?.name.toString()
                } else {
                    ""
                }
                val addedTransaction = Transactions(
                    "1",
                    title,
                    description,
                    System.currentTimeMillis().toString(),
                    binding.root.findViewById<RadioButton>(checkedRadioButtonId).text.toString(),
                    amount,
                    photoPath,
                )
                transactionViewModel.addTransaction(addedTransaction, photoUri)
                transactionViewModel.transactionAddState.observe(viewLifecycleOwner) {
                    when (it) {
                        is Resource.Success -> {
                            binding.transactionAddProgressBar.setVisible(false)
                            dialog.setContentView(R.layout.transaction_succesfully_dialog)
                            dialog.show()
                            Handler(Looper.getMainLooper()).postDelayed({
                                dialog.dismiss()
                                requireActivity().onBackPressed()
                            }, 3000)
                        }
                        is Resource.Loading -> {
                            binding.transactionAddProgressBar.setVisible(true)
                        }
                        is Resource.Failure -> {
                            dialog.setContentView(R.layout.transaction_failed_dialog)
                            dialog.show()
                            Handler(Looper.getMainLooper()).postDelayed({
                                dialog.dismiss()
                            }, 3000)
                            binding.transactionAddProgressBar.setVisible(false)
                        }
                    }
                }
            }
        }
    }

    private fun updateTransaction() {
        binding.addTransactionButton.setOnClickListener {
            if (isInputsEmpty() && isRadioButtonChecked()) {
                val title = binding.transactionTitleInput.text.toString()
                val description = binding.transactionDescriptionInput.text.toString()
                val amount = binding.transactionInputAmount.text.toString().toDouble()
                val checkedRadioButtonId = binding.transactionTypeRadioGroup.checkedRadioButtonId
                val photoPath = if (photoUri != null) {
                    "images/" + photoUri?.toFile()?.name.toString()
                } else {
                    ""
                }
                val updatedTransaction = Transactions(
                    args.transactionUpdate?.id!!,
                    title,
                    description,
                    args.transactionUpdate?.createdAt.toString(),
                    binding.root.findViewById<RadioButton>(checkedRadioButtonId).text.toString(),
                    amount,
                    photoPath
                )
                transactionViewModel.updateTransaction(updatedTransaction, photoUri)
                transactionViewModel.transactionUpdateState.observe(viewLifecycleOwner) {
                    when (it) {
                        is Resource.Success -> {
                            binding.transactionAddProgressBar.setVisible(false)
                            context?.showToast("Updated Transaction")
                            findNavController().navigate(R.id.action_transactionFragment_to_homeFragment)
                        }
                        is Resource.Loading -> {
                            binding.transactionAddProgressBar.setVisible(true)
                        }
                        is Resource.Failure -> {
                            binding.transactionAddProgressBar.setVisible(false)
                        }
                    }
                }
            }
        }
    }

    private fun bindTransactionToInputs() {
        binding.transactionTitleInput.setText(args.transactionUpdate?.title)
        binding.transactionDescriptionInput.setText(args.transactionUpdate?.description)
        binding.transactionInputAmount.setText(args.transactionUpdate?.amount.toString())
        if (args.transactionUpdate?.photoPath != "") {
            binding.takenPhotoLayout.setVisible(true)
            binding.transactionAttachmentLayout.visibility = View.GONE
            transactionViewModel.downloadTransactionImage(args.transactionUpdate?.photoPath)
            transactionViewModel.transactionImageState.observe(viewLifecycleOwner) {
                when (it) {
                    is Resource.Success -> {
                        Glide.with(requireContext())
                            .load(it.result)
                            .into(binding.takenPhoto)
                    }
                    is Resource.Loading -> {
                        binding.transactionImageProgressBar.setVisible(true)
                    }
                    is Resource.Failure -> {
                        context?.showToast(it.exception.message.toString())
                        binding.transactionImageProgressBar.setVisible(false)
                    }
                }
            }
        }

        if (args.transactionUpdate?.type == "Expense") {
            binding.expenseRadioButton.isChecked = true
        } else {
            binding.incomeRadioButton.isChecked = true
        }
    }

    private fun isInputsEmpty(): Boolean {
        val titleResult = binding.transactionTitleInput.customIsEmpty()
        val descriptionResult = binding.transactionDescriptionInput.customIsEmpty()
        val amountResult = binding.transactionInputAmount.customIsEmpty()
        return (titleResult && descriptionResult && amountResult)
    }

    private fun isRadioButtonChecked(): Boolean {
        val checkedRadioButtonId = binding.transactionTypeRadioGroup.checkedRadioButtonId
        if (checkedRadioButtonId == -1) context?.showToast("Please select transaction type")
        return checkedRadioButtonId != -1
    }

}