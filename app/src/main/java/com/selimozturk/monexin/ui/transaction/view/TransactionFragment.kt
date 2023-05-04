package com.selimozturk.monexin.ui.transaction.view

import android.Manifest
import android.app.Activity
import android.app.Dialog
import android.content.Intent
import android.content.pm.PackageManager
import android.database.Cursor
import android.net.Uri
import android.os.Bundle
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
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.selimozturk.monexin.R
import com.selimozturk.monexin.databinding.FragmentTransactionBinding
import com.selimozturk.monexin.model.Transactions
import com.selimozturk.monexin.ui.transaction.viewmodel.TransactionViewModel
import com.selimozturk.monexin.utils.Resource
import com.selimozturk.monexin.utils.customIsEmpty
import com.selimozturk.monexin.utils.setVisible
import com.selimozturk.monexin.utils.showToast
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.delay
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
            Log.i(getString(R.string.app_name), "Permission granted")
        } else {
            Log.i(getString(R.string.app_name), "Permission denied")
        }
    }
    private var resultLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                getSelectedImageFromGallery(result.data?.data)
            }
        }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        binding = FragmentTransactionBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        getTakenPhoto()
        initViews()
        setupObservers()
    }

    private fun requestCameraPermission() {
        when {
            ContextCompat.checkSelfPermission(
                requireContext(), Manifest.permission.CAMERA
            ) == PackageManager.PERMISSION_GRANTED -> {
                findNavController().navigate(R.id.action_transactionFragment_to_cameraFragment)
            }
            ActivityCompat.shouldShowRequestPermissionRationale(
                requireActivity(), Manifest.permission.CAMERA
            ) -> Log.i(getString(R.string.app_name), "Show camera permission dialog")
            else -> requestPermissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    private fun requestGalleryPermission() {
        when {
            ContextCompat.checkSelfPermission(
                requireContext(), Manifest.permission.READ_EXTERNAL_STORAGE
            ) == PackageManager.PERMISSION_GRANTED -> {
                openGallery()
            }
            ActivityCompat.shouldShowRequestPermissionRationale(
                requireActivity(), Manifest.permission.READ_EXTERNAL_STORAGE
            ) -> Log.i(getString(R.string.app_name), "Show gallery permission dialog")
            else -> requestPermissionLauncher.launch(Manifest.permission.READ_EXTERNAL_STORAGE)
        }
    }

    private fun getTakenPhoto() = with(binding) {
        findNavController().currentBackStackEntry?.savedStateHandle?.getLiveData<Uri>("Uri")
            ?.observe(
                viewLifecycleOwner
            ) { uri ->
                takenPhotoLayout.setVisible(true)
                transactionAttachmentLayout.visibility = View.GONE
                photoUri = uri
                Glide.with(requireContext()).load(uri).diskCacheStrategy(DiskCacheStrategy.DATA)
                    .into(takenPhoto)
            }
    }

    private fun initViews() = with(binding) {
        transactionBackButton.setOnClickListener {
            findNavController().popBackStack()
        }
        if (args.transactionUpdate != null) {
            addTransactionButton.setText(R.string.update_transaction)
            bindTransactionToInputs()
            addTransactionButton.setOnClickListener {
                addOrUpdateTransaction(false)
            }
        } else {
            addTransactionButton.setOnClickListener {
                addOrUpdateTransaction(true)
            }
        }
        transactionCameraButton.setOnClickListener {
            requestCameraPermission()
        }
        transactionImageButton.setOnClickListener {
            requestGalleryPermission()
        }
        takenPhotoDeleteButton.setOnClickListener {
            deleteTakenPhoto()
        }
    }

    private fun setupObservers() = with(binding) {
        transactionViewModel.transactionAddState.observe(viewLifecycleOwner) { state ->
            when (state) {
                is Resource.Success -> {
                    transactionAddProgressBar.setVisible(false)
                    showSuccessDialog()
                    requireActivity().onBackPressed()

                }
                is Resource.Loading -> {
                    transactionAddProgressBar.setVisible(true)
                }
                is Resource.Failure -> {
                    showErrorDialog()
                    transactionAddProgressBar.setVisible(false)
                }
            }
        }
        transactionViewModel.transactionUpdateState.observe(viewLifecycleOwner) { state ->
            when (state) {
                is Resource.Success -> {
                    transactionAddProgressBar.setVisible(false)
                    context?.showToast("Updated Transaction")
                    findNavController().navigate(R.id.action_transactionFragment_to_homeFragment)
                }
                is Resource.Loading -> {
                    transactionAddProgressBar.setVisible(true)
                }
                is Resource.Failure -> {
                    context?.showToast(state.exception.message.toString())
                    transactionAddProgressBar.setVisible(false)
                }
            }
        }
        transactionViewModel.transactionImageState.observe(viewLifecycleOwner) { state ->
            when (state) {
                is Resource.Success -> {
                    Glide.with(requireContext()).load(state.result).into(takenPhoto)
                }
                is Resource.Loading -> {
                    transactionImageProgressBar.setVisible(true)
                }
                is Resource.Failure -> {
                    context?.showToast(state.exception.message.toString())
                    transactionImageProgressBar.setVisible(false)
                }
            }
        }
    }

    private fun openGallery() {
        val galleryIntent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
        resultLauncher.launch(galleryIntent)
    }

    private fun getSelectedImageFromGallery(uri: Uri?) = with(binding) {
        var filePath: String? = ""
        if (uri != null && "content" == uri.scheme) {
            val cursor: Cursor? = context?.contentResolver?.query(
                uri, arrayOf(MediaStore.Images.ImageColumns.DATA), null, null, null
            )
            cursor?.moveToFirst()
            if (cursor != null) {
                filePath = cursor.getString(0)
            }
            cursor?.close()
        } else {
            filePath = uri?.path
        }
        photoUri = Uri.fromFile(File(filePath!!))
        transactionAttachmentLayout.visibility = View.GONE
        takenPhotoLayout.setVisible(true)
        Glide.with(requireContext()).load(uri).into(takenPhoto)
    }

    private fun deleteTakenPhoto() = with(binding) {
        photoUri?.toFile()?.delete()
        args.transactionUpdate?.photoPath = ""
        photoUri = null
        transactionAttachmentLayout.setVisible(true)
        takenPhotoLayout.visibility = View.GONE
    }

    private fun addOrUpdateTransaction(isAdd: Boolean) = with(binding) {
        if (isInputsEmpty() && isRadioButtonChecked()) {
            val title = transactionTitleInput.text.toString()
            val description = transactionDescriptionInput.text.toString()
            val amount = transactionInputAmount.text.toString().toDouble()
            val checkedRadioButtonId = transactionTypeRadioGroup.checkedRadioButtonId
            val photoPath = if (photoUri != null) {
                "images/" + photoUri?.toFile()?.name.toString()
            } else {
                ""
            }
            val transaction = if (isAdd) {
                Transactions(
                    "1",
                    title,
                    description,
                    System.currentTimeMillis().toString(),
                    root.findViewById<RadioButton>(checkedRadioButtonId).text.toString()
                        .lowercase(),
                    amount,
                    photoPath,
                )
            } else {
                Transactions(
                    args.transactionUpdate?.id!!,
                    title,
                    description,
                    args.transactionUpdate?.createdAt.toString(),
                    root.findViewById<RadioButton>(checkedRadioButtonId).text.toString()
                        .lowercase(),
                    amount,
                    photoPath
                )
            }
            if (isAdd) {
                transactionViewModel.addTransaction(transaction, photoUri)
            } else {
                transactionViewModel.updateTransaction(transaction, photoUri)
            }
        }
    }

    private fun showSuccessDialog() {
        val dialog = Dialog(requireContext())
        dialog.setContentView(R.layout.transaction_succesfully_dialog)
        dialog.show()
        viewLifecycleOwner.lifecycleScope.launchWhenStarted {
            delay(3000)
            dialog.dismiss()
        }
    }

    private fun showErrorDialog() {
        val dialog = Dialog(requireContext())
        dialog.setContentView(R.layout.transaction_failed_dialog)
        dialog.show()
        viewLifecycleOwner.lifecycleScope.launchWhenStarted {
            delay(3000)
            dialog.dismiss()
        }
    }

    private fun bindTransactionToInputs() = with(binding) {
        args.transactionUpdate?.let { transaction ->
            transactionTitleInput.setText(transaction.title)
            transactionDescriptionInput.setText(transaction.description)
            transactionInputAmount.setText(transaction.amount.toString())
            if (transaction.photoPath.isNotBlank()) {
                takenPhotoLayout.setVisible(true)
                transactionAttachmentLayout.visibility = View.GONE
                transactionViewModel.downloadTransactionImage(transaction.photoPath)
            }

            when (transaction.type) {
                "expense" -> expenseRadioButton.isChecked = true
                else -> incomeRadioButton.isChecked = true
            }
        }
    }

    private fun isInputsEmpty(): Boolean = with(binding) {
        val titleResult = transactionTitleInput.customIsEmpty()
        val descriptionResult = transactionDescriptionInput.customIsEmpty()
        val amountResult = transactionInputAmount.customIsEmpty()
        return (titleResult && descriptionResult && amountResult)
    }

    private fun isRadioButtonChecked(): Boolean = with(binding) {
        val checkedRadioButtonId = transactionTypeRadioGroup.checkedRadioButtonId
        if (checkedRadioButtonId == -1) context?.showToast("Please select transaction type")
        return checkedRadioButtonId != -1
    }

}