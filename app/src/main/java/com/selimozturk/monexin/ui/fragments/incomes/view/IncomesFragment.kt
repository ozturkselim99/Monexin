package com.selimozturk.monexin.ui.fragments.incomes.view

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.widget.addTextChangedListener
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.selimozturk.monexin.R
import com.selimozturk.monexin.adapter.TransactionAdapter
import com.selimozturk.monexin.databinding.FragmentIncomesBinding
import com.selimozturk.monexin.model.Transactions
import com.selimozturk.monexin.ui.fragments.incomes.viewmodel.IncomesViewModel
import com.selimozturk.monexin.utils.Resource
import com.selimozturk.monexin.utils.setVisible
import com.selimozturk.monexin.utils.showToast
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class IncomesFragment : Fragment() {

    private lateinit var binding: FragmentIncomesBinding
    private val adapter = TransactionAdapter()
    private val incomesViewModel by viewModels<IncomesViewModel>()
    private var incomesList: List<Transactions> = listOf()


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FragmentIncomesBinding.inflate(inflater, container, false)
        setupRecyclerview()
        initViews()
        getIncomes()
        return binding.root
    }

    private fun setupRecyclerview() {
        val layoutManager = LinearLayoutManager(requireContext())
        binding.incomeTransactionsRW.layoutManager = layoutManager
    }

    private fun loadTransactions(transactions: List<Transactions>) {
        adapter.items=transactions
        binding.incomeTransactionsRW.adapter = adapter
        adapter.onItemClicked = { transaction ->
            val direction =
                IncomesFragmentDirections.actionIncomesFragmentToTransactionDetail(transaction)
            findNavController().navigate(direction)
        }
    }

    private fun initViews(){
        binding.incomesFilterButton.setOnClickListener {
            findNavController().navigate(R.id.action_incomesFragment_to_filterBottomSheetFragment2)
        }
        binding.incomesSearchInput.addTextChangedListener { editable ->
            searchFilter(editable.toString())
        }
    }

    private fun getIncomes(){
        incomesViewModel.getIncomes()
        incomesViewModel.incomesState.observe(viewLifecycleOwner){
            when (it) {
                is Resource.Success -> {
                    binding.incomesProgressBar.setVisible(false)
                    binding.incomesAmountText.text=it.result.activeIncome
                    incomesList = it.result.incomes
                    binding.transactionNotFoundLayout.setVisible(it.result.incomes.isEmpty())
                    loadTransactions(it.result.incomes)
                }
                is Resource.Loading -> {
                    binding.incomesProgressBar.setVisible(true)
                }
                is Resource.Failure -> {
                    context?.showToast(it.exception.message.toString())
                    binding.incomesProgressBar.setVisible(false)
                }
            }
        }
    }

    private fun searchFilter(text: String) {
        var filteredList: ArrayList<Transactions> = arrayListOf()
        for (item in incomesList) {
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