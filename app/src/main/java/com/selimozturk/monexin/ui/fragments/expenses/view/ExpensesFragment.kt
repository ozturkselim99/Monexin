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
import com.selimozturk.monexin.utils.*
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
        initBestMatchFilterList()
        dateFilterControl()
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
            if (binding.filtersLayout.filters.visibility == View.GONE) {
                TransitionManager.beginDelayedTransition(
                    binding.filtersLayout.filters,
                    AutoTransition()
                )
                binding.filtersText.rightDrawable(R.drawable.ic_arrow_up)
                binding.filtersLayout.filters.visibility = View.VISIBLE
            } else {
                TransitionManager.beginDelayedTransition(binding.filtersLayout.filters, AutoTransition())
                binding.filtersText.rightDrawable(R.drawable.ic_arrow_down)
                binding.filtersLayout.filters.visibility = View.GONE
            }
    }

    private fun getExpensesByFilters() {
        minAmount = binding.filtersLayout.minTransactionInput.text.toString().ifEmpty {
            Double.MIN_VALUE.toString()
        }
        maxAmount = binding.filtersLayout.maxTransactionInput.text.toString().ifEmpty {
            Double.MAX_VALUE.toString()
        }
        getExpenses(FilterModel(bestMatchResult, minAmount, maxAmount, minDate, maxDate))
        TransitionManager.beginDelayedTransition(binding.filtersLayout.filters, AutoTransition())
        binding.filtersText.rightDrawable(R.drawable.ic_arrow_down)
        binding.filtersLayout.filters.visibility = View.GONE
    }

    private fun initViews() {
        binding.filtersText.setOnClickListener {
            expandableFilterLayout()
        }
        binding.filtersLayout.filterButton.setOnClickListener {
            getExpensesByFilters()
        }
        binding.filtersLayout.transactionMinDateLayout.setOnClickListener {
            datePicker(0)
        }
        binding.filtersLayout.transactionMaxDateLayout.setOnClickListener {
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
        binding.filtersLayout.bestMatchFilterList.setAdapter(arrayAdapter)
        binding.filtersLayout.bestMatchFilterList.onItemClickListener =
            AdapterView.OnItemClickListener { _, _, position, _ ->
                bestMatchResult = position.toString()
            }
    }

    private fun dateFilterControl() {
        binding.filtersLayout.selectedMinDateText.addTextChangedListener {
            if (it.toString() != "Min Date") {
                minDate = (it.toString().convertToTimestamp()).toString()
                if (maxDate.isNotEmpty() && (minDate.toLong() >= maxDate.toLong())) {
                    context?.showToast("Minimum date must be less than the maximum date")
                    binding.filtersLayout.selectedMinDateText.setText(R.string.min_date)
                }
            }
        }
        binding.filtersLayout.selectedMaxDateText.addTextChangedListener {
            if (it.toString() != "Max Date") {
                //86400000(1 gün) seçilen max date kapsaması için topluyorum
                maxDate = (it.toString().convertToTimestamp() + 86400000).toString()
                if (minDate.isNotEmpty() && (maxDate.toLong() <= minDate.toLong())) {
                    context?.showToast("Maximum date must be greater than the minimum date")
                    binding.filtersLayout.selectedMaxDateText.setText(R.string.max_date)
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
                    binding.transactionNotFoundText.setVisible(it.result.expenses.isEmpty())
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
                    binding.filtersLayout.selectedMinDateText.text = date
                } else {
                    binding.filtersLayout.selectedMaxDateText.text = date
                }
            }
        }
        datePickerFragment.show(supportFragmentManager, "DatePickerFragment")
    }

}