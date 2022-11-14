package com.selimozturk.monexin.ui.fragments.expenses.view

import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.widget.addTextChangedListener
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.bumptech.glide.Glide
import com.selimozturk.monexin.R
import com.selimozturk.monexin.adapter.TransactionAdapter
import com.selimozturk.monexin.databinding.FragmentExpensesBinding
import com.selimozturk.monexin.model.Transactions
import com.selimozturk.monexin.ui.fragments.expenses.viewmodel.ExpensesViewModel
import com.selimozturk.monexin.utils.Resource
import com.selimozturk.monexin.utils.setVisible
import com.selimozturk.monexin.utils.showToast
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class ExpensesFragment : Fragment() {

    private lateinit var binding: FragmentExpensesBinding
    private val adapter = TransactionAdapter()
    private val expensesViewModel by viewModels<ExpensesViewModel>()
    private var expensesList: List<Transactions> = listOf()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FragmentExpensesBinding.inflate(inflater, container, false)
        setupRecyclerview()
        initViews()
        getExpenses()

        return binding.root
    }

    private fun setupRecyclerview() {
        val layoutManager = LinearLayoutManager(requireContext())
        binding.expenseTransactionsRW.layoutManager = layoutManager
    }

    private fun loadTransactions(transactions: List<Transactions>) {
        adapter.items=transactions
        binding.expenseTransactionsRW.adapter = adapter
        adapter.onItemClicked = { transaction ->
            val direction =
                ExpensesFragmentDirections.actionExpensesFragmentToTransactionDetail(transaction)
            findNavController().navigate(direction)
        }
    }

    private fun initViews() {
        binding.expensesFilterButton.setOnClickListener {
            findNavController().navigate(R.id.action_expensesFragment_to_filterBottomSheetFragment)
        }
        binding.expensesSearchInput.addTextChangedListener { editable ->
            searchFilter(editable.toString())
        }
    }

    private fun getExpenses() {
        expensesViewModel.getExpenses()
        expensesViewModel.expensesState.observe(viewLifecycleOwner) {
            when (it) {
                is Resource.Success -> {
                    binding.expensesProgressBar.setVisible(false)
                    binding.expensesAmountText.text = it.result.activeExpense
                    expensesList = it.result.expenses
                    binding.transactionNotFoundLayout.setVisible(it.result.expenses.isEmpty())
                    loadTransactions(it.result.expenses)
                }
                is Resource.Loading -> {
                    binding.expensesProgressBar.setVisible(true)
                }
                is Resource.Failure -> {
                    context?.showToast(it.exception.message.toString())
                    binding.expensesProgressBar.setVisible(false)
                }
            }
        }
    }

    private fun searchFilter(text: String) {
        var filteredList: ArrayList<Transactions> = arrayListOf()
        for (item in expensesList) {
            if (item.title.lowercase().contains(text.lowercase())) {
                filteredList.add(item)
            }
        }
        if (filteredList.isNotEmpty()) {
            adapter.items = filteredList
            adapter.notifyDataSetChanged()
        }
    }

}