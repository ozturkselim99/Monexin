package com.selimozturk.monexin.ui.fragments.transaction_detail.view

import android.app.AlertDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.selimozturk.monexin.R
import com.selimozturk.monexin.databinding.FragmentTransactionDetailBinding
import com.selimozturk.monexin.databinding.TransactionDeleteDialogBinding
import com.selimozturk.monexin.ui.fragments.transaction_detail.viewmodel.TransactionDetailViewModel
import com.selimozturk.monexin.utils.Resource
import com.selimozturk.monexin.utils.convertToLongTime
import com.selimozturk.monexin.utils.setVisible
import com.selimozturk.monexin.utils.showToast
import dagger.hilt.android.AndroidEntryPoint


@AndroidEntryPoint
class TransactionDetailFragment : Fragment() {

    private lateinit var binding: FragmentTransactionDetailBinding
    private val args: TransactionDetailFragmentArgs by navArgs()
    private val transactionViewModel by viewModels<TransactionDetailViewModel>()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentTransactionDetailBinding.inflate(inflater, container, false)
        getTransactionDetail()
        initViews()
        return binding.root
    }

    private fun initViews() = with(binding) {
        transactionEditButton.setOnClickListener {
            updateTransaction()
        }
        transactionDetailDeleteButton.setOnClickListener {
            deleteTransaction()
        }
        transactionDetailBackButton.setOnClickListener {
            findNavController().popBackStack()
        }
    }

    private fun getTransactionDetail() = with(binding) {
        if (args.transaction.type == "Expense") {
            transactionDetailInfo.setBackgroundColor(
                ContextCompat.getColor(
                    requireContext(),
                    R.color.accent_4
                )
            )
        } else {
            transactionDetailInfo.setBackgroundColor(
                ContextCompat.getColor(
                    requireContext(),
                    R.color.accent_3
                )
            )
        }
        transactionDetailAmount.text = args.transaction.amount.toString()
        transactionDetailTitle.text = args.transaction.title
        transactionDetailCratedAt.text = args.transaction.createdAt.convertToLongTime()
        transactionDetailDescription.text = args.transaction.description
        if (args.transaction.photoPath != "") {
            transactionViewModel.downloadTransactionImage(args.transaction.photoPath)
            transactionViewModel.transactionImageState.observe(viewLifecycleOwner) {
                when (it) {
                    is Resource.Success -> {
                        Glide.with(requireContext())
                            .load(it.result)
                            .diskCacheStrategy(DiskCacheStrategy.DATA)
                            .into(transactionImage)
                    }
                    is Resource.Loading -> {
                        transactionImageProgressBar.setVisible(true)
                    }
                    is Resource.Failure -> {
                        context?.showToast(it.exception.message.toString())
                        transactionImageProgressBar.setVisible(false)
                    }
                }
            }
        } else {
            transactionImage.setVisible(false)
            transactionImageNotFoundText.setVisible(true)
        }
    }

    private fun deleteTransaction() {
            val dialogBinding: TransactionDeleteDialogBinding =
                TransactionDeleteDialogBinding.inflate(layoutInflater)
            val builder = AlertDialog.Builder(requireContext()).setView(dialogBinding.root).show()
            dialogBinding.deleteTransactionCancelButton.setOnClickListener {
                builder.dismiss()
            }
            dialogBinding.deleteTransactionDeleteButton.setOnClickListener {
                transactionViewModel.deleteTransaction(args.transaction)
                context?.showToast("Transaction Deleted")
                findNavController().popBackStack()
                builder.dismiss()
            }
    }

    private fun updateTransaction() {
            val direction =
                TransactionDetailFragmentDirections.actionTransactionDetailToTransactionFragment(
                    args.transaction
                )
            findNavController().navigate(direction)
    }

}