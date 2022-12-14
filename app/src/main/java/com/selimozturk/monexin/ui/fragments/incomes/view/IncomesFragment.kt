package com.selimozturk.monexin.ui.fragments.incomes.view

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
import com.selimozturk.monexin.databinding.FragmentIncomesBinding
import com.selimozturk.monexin.model.FilterModel
import com.selimozturk.monexin.model.Transactions
import com.selimozturk.monexin.ui.fragments.date_picker.DatePickerFragment
import com.selimozturk.monexin.ui.fragments.incomes.viewmodel.IncomesViewModel
import com.selimozturk.monexin.utils.*
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class IncomesFragment : Fragment() {

    private lateinit var binding: FragmentIncomesBinding
    private val adapter = TransactionAdapter()
    private val incomesViewModel by viewModels<IncomesViewModel>()
    private var incomesList: List<Transactions> = listOf()
    private var minDate: String = "0"
    private var maxDate: String = System.currentTimeMillis().toString()
    private var bestMatchResult: BestMatchResult = BestMatchResult.DESCENDING_BY_DATE
    private var minAmount: String = Double.MIN_VALUE.toString()
    private var maxAmount: String = Double.MAX_VALUE.toString()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentIncomesBinding.inflate(inflater, container, false)
        setupRecyclerview()
        initViews()
        getIncomes(null)
        initBestMatchFilterList()
        dateFilterControl()
        return binding.root
    }

    private fun setupRecyclerview() = with(binding) {
        val layoutManager = LinearLayoutManager(requireContext())
        incomeTransactionsRW.layoutManager = layoutManager
    }

    private fun loadTransactions(transactions: List<Transactions>) = with(binding) {
        adapter.items = transactions
        incomeTransactionsRW.adapter = adapter
        adapter.onItemClicked = { transaction ->
            val direction =
                IncomesFragmentDirections.actionIncomesFragmentToTransactionDetail(transaction)
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
            TransitionManager.beginDelayedTransition(
                filtersLayout.filters,
                AutoTransition()
            )
            filtersText.rightDrawable(R.drawable.ic_arrow_down)
            filtersLayout.filters.visibility = View.GONE
        }
    }

    private fun getIncomesByFilters() = with(binding) {
        minAmount = filtersLayout.minTransactionInput.text.toString().ifEmpty {
            Double.MIN_VALUE.toString()
        }
        maxAmount = filtersLayout.maxTransactionInput.text.toString().ifEmpty {
            Double.MAX_VALUE.toString()
        }
        getIncomes(FilterModel(bestMatchResult, minAmount, maxAmount, minDate, maxDate))
        TransitionManager.beginDelayedTransition(filtersLayout.filters, AutoTransition())
        filtersText.rightDrawable(R.drawable.ic_arrow_down)
        filtersLayout.filters.visibility = View.GONE
    }

    private fun initViews() = with(binding) {
        filtersText.setOnClickListener {
            expandableFilterLayout()
        }
        filtersLayout.filterButton.setOnClickListener {
            getIncomesByFilters()
        }
        filtersLayout.transactionMinDateLayout.setOnClickListener {
            datePicker(DateType.MIN_DATE)
        }
        filtersLayout.transactionMaxDateLayout.setOnClickListener {
            datePicker(DateType.MAX_DATE)
        }
        incomesSearchInput.addTextChangedListener { editable ->
            searchFilter(editable.toString())
        }
    }

    private fun initBestMatchFilterList() = with(binding) {
        val bestMatchFilterList = resources.getStringArray(R.array.bestMatchFilterList)
        val arrayAdapter =
            ArrayAdapter(requireContext(), R.layout.dropdown_item, bestMatchFilterList)
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
                //86400000(1 g??n) se??ilen max date kapsamas?? i??in topluyorum
                maxDate = (it.toString().convertToTimestamp() + 86400000).toString()
                if (minDate.isNotEmpty() && (maxDate.toLong() <= minDate.toLong())) {
                    context?.showToast("Maximum date must be greater than the minimum date")
                    filtersLayout.selectedMaxDateText.setText(R.string.max_date)
                }
            }
        }
    }

    private fun getIncomes(filterModel: FilterModel?) = with(binding) {
        incomesViewModel.getIncomes(filterModel)
        incomesViewModel.incomesState.observe(viewLifecycleOwner) {
            when (it) {
                is Resource.Success -> {
                    incomesProgressBar.setVisible(false)
                    incomesAmountText.text = it.result.activeIncome
                    incomesList = it.result.incomes
                    transactionNotFoundText.setVisible(it.result.incomes.isEmpty())
                    loadTransactions(it.result.incomes)
                }
                is Resource.Loading -> {
                    incomesProgressBar.setVisible(true)
                }
                is Resource.Failure -> {
                    context?.showToast(it.exception.message.toString())
                    incomesProgressBar.setVisible(false)
                }
            }
        }
    }

    private fun searchFilter(text: String) {
        val filteredList: ArrayList<Transactions> = arrayListOf()
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