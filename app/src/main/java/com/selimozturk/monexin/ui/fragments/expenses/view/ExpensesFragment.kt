package com.selimozturk.monexin.ui.fragments.expenses.view

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import androidx.core.widget.addTextChangedListener
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.transition.AutoTransition
import androidx.transition.TransitionManager
import com.selimozturk.monexin.R
import com.selimozturk.monexin.adapter.TransactionAdapter
import com.selimozturk.monexin.databinding.FragmentExpensesBinding
import com.selimozturk.monexin.model.FilterModel
import com.selimozturk.monexin.model.Transactions
import com.selimozturk.monexin.ui.fragments.date_picker.DatePickerFragment
import com.selimozturk.monexin.ui.fragments.expenses.viewmodel.ExpensesViewModel
import com.selimozturk.monexin.utils.Resource
import com.selimozturk.monexin.utils.convertToTimestamp
import com.selimozturk.monexin.utils.setVisible
import com.selimozturk.monexin.utils.showToast
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class ExpensesFragment : Fragment() {

    private lateinit var binding: FragmentExpensesBinding
    private val adapter = TransactionAdapter()
    private val expensesViewModel by viewModels<ExpensesViewModel>()
    private var expensesList: List<Transactions> = listOf()
    private var minDate: String = "0"
    private var maxDate: String = System.currentTimeMillis().toString()
    private var bestMatchResult: String = "0"
    private var minAmount: String = Double.MIN_VALUE.toString()
    private var maxAmount: String = Double.MAX_VALUE.toString()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentExpensesBinding.inflate(inflater, container, false)
        setupRecyclerview()
        initViews()
        getExpenses(null)
        getExpensesByFilters()
        return binding.root
    }

    private fun setupRecyclerview() {
        val layoutManager = LinearLayoutManager(requireContext())
        binding.expenseTransactionsRW.layoutManager = layoutManager
    }

    private fun loadTransactions(transactions: List<Transactions>) {
        adapter.items = transactions
        binding.expenseTransactionsRW.adapter = adapter
        adapter.onItemClicked = { transaction ->
            val direction =
                ExpensesFragmentDirections.actionExpensesFragmentToTransactionDetail(transaction)
            findNavController().navigate(direction)
        }
    }

    private fun expandableFilterLayout() {
        binding.filtersTextLayout.setOnClickListener {
            if (binding.filterLayout.visibility == View.GONE) {
                TransitionManager.beginDelayedTransition(
                    binding.filterLayout,
                    AutoTransition()
                )
                binding.arrowDirectionImage.setImageResource(R.drawable.ic_arrow_up)
                binding.filterLayout.visibility = View.VISIBLE
            } else {
                TransitionManager.beginDelayedTransition(binding.filterLayout, AutoTransition())
                binding.arrowDirectionImage.setImageResource(R.drawable.ic_arrow_down)
                binding.filterLayout.visibility = View.GONE
            }
        }
    }

    private fun getExpensesByFilters() {
        binding.filterButton.setOnClickListener {
            minAmount = binding.minTransactionInput.text.toString().ifEmpty {
                Double.MIN_VALUE.toString()
            }
            maxAmount = binding.maxTransactionInput.text.toString().ifEmpty {
                Double.MAX_VALUE.toString()
            }
            getExpenses(FilterModel(bestMatchResult, minAmount, maxAmount, minDate, maxDate))
        }
    }

    private fun initViews() {
        initBestMatchFilterList()
        dateFilterControl()
        expandableFilterLayout()
        binding.transactionMinDateLayout.setOnClickListener {
            datePicker(0)
        }
        binding.transactionMaxDateLayout.setOnClickListener {
            datePicker(1)
        }
        binding.expensesSearchInput.addTextChangedListener { editable ->
            searchFilter(editable.toString())
        }
    }

    private fun initBestMatchFilterList() {
        val bestMatchFilterList = resources.getStringArray(R.array.bestMatchFilterList)
        val arrayAdapter =
            ArrayAdapter(requireContext(), R.layout.dropdown_item, bestMatchFilterList)
        binding.bestMatchFilterList.setAdapter(arrayAdapter)
        binding.bestMatchFilterList.onItemClickListener =
            AdapterView.OnItemClickListener { parent, view, position, id ->
                bestMatchResult = position.toString()
            }
    }

    private fun dateFilterControl() {
        binding.selectedMinDateText.addTextChangedListener {
            if (it.toString() != "Min Date") {
                minDate = (it.toString().convertToTimestamp()).toString()
                if (maxDate.isNotEmpty() && (minDate.toLong() >= maxDate.toLong())) {
                    context?.showToast("Minimum date must be less than the maximum date")
                    binding.selectedMinDateText.text = "Min Date"
                }
            }
        }
        binding.selectedMaxDateText.addTextChangedListener {
            if (it.toString() != "Max Date") {
                //86400000(1 gün) seçilen max date kapsaması için topluyorum
                maxDate = (it.toString().convertToTimestamp() + 86400000).toString()
                if (minDate.isNotEmpty() && (maxDate.toLong() <= minDate.toLong())) {
                    context?.showToast("Maximum date must be greater than the minimum date")
                    binding.selectedMaxDateText.text = "Max Date"
                }
            }
        }
    }

    private fun getExpenses(filterModel: FilterModel?) {
        expensesViewModel.getExpenses(filterModel)
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
        val filteredList: ArrayList<Transactions> = arrayListOf()
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

    private fun datePicker(dateType: Int) {
        val datePickerFragment = DatePickerFragment()
        val supportFragmentManager = requireActivity().supportFragmentManager
        supportFragmentManager.setFragmentResultListener(
            "REQUEST_KEY",
            viewLifecycleOwner
        ) { resultKey, bundle ->
            if (resultKey == "REQUEST_KEY") {
                val date = bundle.getString("SELECTED_DATE")
                if (dateType == 0) {
                    binding.selectedMinDateText.text = date
                } else {
                    binding.selectedMaxDateText.text = date
                }
            }
        }
        datePickerFragment.show(supportFragmentManager, "DatePickerFragment")
    }

}