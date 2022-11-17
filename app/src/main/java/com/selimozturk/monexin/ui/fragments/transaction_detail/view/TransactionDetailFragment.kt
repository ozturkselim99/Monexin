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
        initViews()
        return binding.root
    }

    private fun initViews() {
        getTransactionDetail()
        deleteTransaction()
        updateTransaction()

        binding.transactionDetailBackButton.setOnClickListener {
            findNavController().popBackStack()
        }
    }

    private fun getTransactionDetail() {
        if (args.transaction.type == "Expense") {
            binding.transactionDetailInfo.setBackgroundColor(
                ContextCompat.getColor(
                    requireContext(),
                    R.color.accent_4
                )
            )
        } else {
            binding.transactionDetailInfo.setBackgroundColor(
                ContextCompat.getColor(
                    requireContext(),
                    R.color.accent_3
                )
            )
        }
        binding.transactionDetailAmount.text = args.transaction.amount.toString()
        binding.transactionDetailTitle.text = args.transaction.title
        binding.transactionDetailCratedAt.text = args.transaction.createdAt.convertToLongTime()
        binding.transactionDetailDescription.text = args.transaction.description
        if (args.transaction.photoPath != "") {
            transactionViewModel.downloadTransactionImage(args.transaction.photoPath)
            transactionViewModel.transactionImageState.observe(viewLifecycleOwner) {
                when (it) {
                    is Resource.Success -> {
                        Glide.with(requireContext())
                            .load(it.result)
                            .into(binding.transactionImage)
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
        } else {
            binding.transactionImage.setVisible(false)
            binding.transactionImageNotFoundLayout.setVisible(true)
        }
    }

    private fun deleteTransaction() {
        binding.transactionDetailDeleteButton.setOnClickListener {
            val dialogBinding: TransactionDeleteDialogBinding =
                TransactionDeleteDialogBinding.inflate(layoutInflater)
            val builder = AlertDialog.Builder(requireContext()).setView(dialogBinding.root).show()
            dialogBinding.deleteTransactionCancelButton.setOnClickListener {
                builder.dismiss()
            }
            dialogBinding.deleteTransactionDeleteButton.setOnClickListener {
                if (args.transaction.type == "Expense") {
                    transactionViewModel.deleteTransaction("expenses", args.transaction.id)
                } else {
                    transactionViewModel.deleteTransaction("incomes", args.transaction.id)
                }
                context?.showToast("Transaction Deleted")
                findNavController().popBackStack()
                builder.dismiss()
            }
        }
    }

    private fun updateTransaction() {
        binding.transactionEditButton.setOnClickListener {
            val direction =
                TransactionDetailFragmentDirections.actionTransactionDetailToTransactionFragment(
                    args.transaction
                )
            findNavController().navigate(direction)
        }
    }

}