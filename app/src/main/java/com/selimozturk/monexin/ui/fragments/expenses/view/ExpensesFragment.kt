package com.selimozturk.monexin.ui.fragments.expenses.view

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import androidx.core.widget.addTextChangedListener
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.transition.AutoTransition
import androidx.transition.TransitionManager
import com.selimozturk.monexin.R
import com.selimozturk.monexin.ui.TransactionAdapter
import com.selimozturk.monexin.databinding.FragmentExpensesBinding
import com.selimozturk.monexin.model.FilterModel
import com.selimozturk.monexin.model.Transactions
import com.selimozturk.monexin.ui.activities.MainViewModel
import com.selimozturk.monexin.ui.fragments.date_picker.DatePickerFragment
import com.selimozturk.monexin.ui.fragments.expenses.viewmodel.ExpensesViewModel
import com.selimozturk.monexin.utils.*
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class ExpensesFragment : Fragment() {

    private val mainViewModel: MainViewModel by activityViewModels()
    private lateinit var binding: FragmentExpensesBinding
    private val adapter = TransactionAdapter()
    private val expensesViewModel by viewModels<ExpensesViewModel>()
    private var expensesList: List<Transactions> = listOf()
    private var minDate: String = "0"
    private var maxDate: String = System.currentTimeMillis().toString()
    private var bestMatchResult: BestMatchResult = BestMatchResult.DESCENDING_BY_DATE
    private var minAmount: String = Double.MIN_VALUE.toString()
    private var maxAmount: String = Double.MAX_VALUE.toString()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentExpensesBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupRecyclerview()
        initViews()
        initBestMatchFilterList()
        dateFilterControl()
        getExpenses(null)
        observeExpensesReselected()
    }
    private fun observeExpensesReselected(){
        mainViewModel.isExpensesReselected.observe(viewLifecycleOwner) {
            binding.expenseTransactionsRW.smoothScrollToPosition(0)
        }
    }

    private fun setupRecyclerview() = with(binding) {
        val layoutManager = LinearLayoutManager(requireContext())
        expenseTransactionsRW.layoutManager = layoutManager
    }

    private fun loadTransactions(transactions: List<Transactions>) = with(binding) {
        adapter.items = transactions
        expenseTransactionsRW.adapter = adapter
        adapter.onItemClicked = { transaction ->
            val direction =
                ExpensesFragmentDirections.actionExpensesFragmentToTransactionDetail(transaction)
            findNavController().navigate(direction)
        }
    }

    private fun expandableFilterLayout() = with(binding) {
        if (filtersLayout.filters.visibility == View.GONE) {
            TransitionManager.beginDelayedTransition(
                filtersLayout.filters,
                AutoTransition()
            )
            filtersText.rightDrawable(R.drawable.ic_arrow_up)
            filtersLayout.filters.visibility = View.VISIBLE
        } else {
            TransitionManager.beginDelayedTransition(filtersLayout.filters, AutoTransition())
            filtersText.rightDrawable(R.drawable.ic_arrow_down)
            filtersLayout.filters.visibility = View.GONE
        }
    }

    private fun getExpensesByFilters() = with(binding) {
        minAmount = filtersLayout.minTransactionInput.text.toString().ifEmpty {
            Double.MIN_VALUE.toString()
        }
        maxAmount = filtersLayout.maxTransactionInput.text.toString().ifEmpty {
            Double.MAX_VALUE.toString()
        }
        getExpenses(FilterModel(bestMatchResult, minAmount, maxAmount, minDate, maxDate))
        TransitionManager.beginDelayedTransition(filtersLayout.filters, AutoTransition())
        filtersText.rightDrawable(R.drawable.ic_arrow_down)
        filtersLayout.filters.visibility = View.GONE
    }

    private fun initViews() = with(binding) {
        filtersText.setOnClickListener {
            expandableFilterLayout()
        }
        filtersLayout.filterButton.setOnClickListener {
            getExpensesByFilters()
        }
        filtersLayout.transactionMinDateLayout.setOnClickListener {
            datePicker(DateType.MIN_DATE)
        }
        filtersLayout.transactionMaxDateLayout.setOnClickListener {
            datePicker(DateType.MAX_DATE)
        }
        expensesSearchInput.addTextChangedListener { editable ->
            searchFilter(editable.toString())
        }
    }

    private fun initBestMatchFilterList() = with(binding) {
        val arrayAdapter =
            ArrayAdapter(requireContext(), R.layout.dropdown_item, BestMatchResult.values())
        filtersLayout.bestMatchFilterList.setAdapter(arrayAdapter)
        filtersLayout.bestMatchFilterList.onItemClickListener =
            AdapterView.OnItemClickListener { _, _, position, _ ->
                bestMatchResult = BestMatchResult.values()[position]
            }
    }

    private fun dateFilterControl() = with(binding) {
        filtersLayout.selectedMinDateText.addTextChangedListener {
            if (it.toString() != "Min Date") {
                minDate = (it.toString().convertToTimestamp()).toString()
                if (maxDate.isNotEmpty() && (minDate.toLong() >= maxDate.toLong())) {
                    context?.showToast("Minimum date must be less than the maximum date")
                    filtersLayout.selectedMinDateText.setText(R.string.min_date)
                }
            }
        }
        filtersLayout.selectedMaxDateText.addTextChangedListener {
            if (it.toString() != "Max Date") {
                //86400000(1 gün) seçilen max date kapsaması için topluyorum
                maxDate = (it.toString().convertToTimestamp() + 86400000).toString()
                if (minDate.isNotEmpty() && (maxDate.toLong() <= minDate.toLong())) {
                    context?.showToast("Maximum date must be greater than the minimum date")
                    filtersLayout.selectedMaxDateText.setText(R.string.max_date)
                }
            }
        }
    }

    private fun getExpenses(filterModel: FilterModel?) = with(binding) {
        expensesViewModel.getExpenses(filterModel)
        expensesViewModel.expensesState.observe(viewLifecycleOwner) {
            when (it) {
                is Resource.Success -> {
                    expensesProgressBar.setVisible(false)
                    expensesAmountText.text = it.result.activeExpense
                    expensesList = it.result.expenses
                    transactionNotFoundText.setVisible(it.result.expenses.isEmpty())
                    loadTransactions(it.result.expenses)
                }
                is Resource.Loading -> {
                    expensesProgressBar.setVisible(true)
                }
                is Resource.Failure -> {
                    context?.showToast(it.exception.message.toString())
                    expensesProgressBar.setVisible(false)
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

    private fun datePicker(dateType: DateType) = with(binding) {
        val datePickerFragment = DatePickerFragment()
        val supportFragmentManager = requireActivity().supportFragmentManager
        supportFragmentManager.setFragmentResultListener(
            "REQUEST_KEY",
            viewLifecycleOwner
        ) { resultKey, bundle ->
            if (resultKey == "REQUEST_KEY") {
                val date = bundle.getString("SELECTED_DATE")
                if (dateType == DateType.MIN_DATE) {
                    filtersLayout.selectedMinDateText.text = date
                } else {
                    filtersLayout.selectedMaxDateText.text = date
                }
            }
        }
        datePickerFragment.show(supportFragmentManager, "DatePickerFragment")
    }

}