package com.selimozturk.monexin.ui.incomes.view

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import androidx.core.view.isVisible
import androidx.core.widget.addTextChangedListener
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.transition.AutoTransition
import androidx.transition.TransitionManager
import com.selimozturk.monexin.R
import com.selimozturk.monexin.databinding.FragmentIncomesBinding
import com.selimozturk.monexin.model.FilterModel
import com.selimozturk.monexin.model.Transactions
import com.selimozturk.monexin.ui.MainViewModel
import com.selimozturk.monexin.ui.TransactionAdapter
import com.selimozturk.monexin.ui.date_picker.DatePickerFragment
import com.selimozturk.monexin.ui.incomes.viewmodel.IncomesViewModel
import com.selimozturk.monexin.utils.*
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class IncomesFragment : Fragment() {

    private val mainViewModel: MainViewModel by activityViewModels()
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
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        binding = FragmentIncomesBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupRecyclerview()
        initViews()
        initBestMatchFilterList()
        dateFilterControl()
        getIncomes(null)
        setupObservers()
    }

    private fun setupRecyclerview() = with(binding) {
        val layoutManager = LinearLayoutManager(requireContext())
        incomeTransactionsRW.layoutManager = layoutManager
    }

    private fun loadTransactions(transactions: List<Transactions>) = with(binding) {
        incomeTransactionsRW.adapter = adapter
        adapter.items = transactions
        adapter.onItemClicked = { transaction ->
            val direction = IncomesFragmentDirections.actionIncomesFragmentToTransactionDetail(
                transaction
            )
            findNavController().navigate(direction)
        }
    }

    private fun expandableFilterLayout() = with(binding) {
        TransitionManager.beginDelayedTransition(filtersLayout.filters, AutoTransition())
        if (filtersLayout.filters.isVisible) {
            filtersText.rightDrawable(R.drawable.ic_arrow_down)
            filtersLayout.filters.visibility = View.GONE
        } else {
            filtersText.rightDrawable(R.drawable.ic_arrow_up)
            filtersLayout.filters.visibility = View.VISIBLE
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

    private fun setupObservers() = with(binding) {
        incomesViewModel.incomesState.observe(viewLifecycleOwner) { state ->
            when (state) {
                is Resource.Success -> {
                    incomesProgressBar.setVisible(false)
                    incomesAmountText.text = state.result.activeIncome
                    incomesList = state.result.incomes
                    transactionNotFoundText.setVisible(state.result.incomes.isEmpty())
                    loadTransactions(state.result.incomes)
                }
                is Resource.Loading -> {
                    incomesProgressBar.setVisible(true)
                }
                is Resource.Failure -> {
                    context?.showToast(state.exception.message.toString())
                    incomesProgressBar.setVisible(false)
                }
            }
        }
        mainViewModel.isIncomesReselected.observe(viewLifecycleOwner) {
            binding.incomeTransactionsRW.smoothScrollToPosition(0)
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
                //86400000(1 gün) seçilen max date kapsaması için topluyorum
                maxDate = (it.toString().convertToTimestamp() + 86400000).toString()
                if (minDate.isNotEmpty() && (maxDate.toLong() <= minDate.toLong())) {
                    context?.showToast("Maximum date must be greater than the minimum date")
                    filtersLayout.selectedMaxDateText.setText(R.string.max_date)
                }
            }
        }
    }

    private fun getIncomes(filterModel: FilterModel?) {
        incomesViewModel.getIncomes(filterModel)
    }

    private fun searchFilter(text: String) {
        val filteredList = incomesList.filter { it.title.lowercase().contains(text.lowercase()) }
        adapter.items = filteredList
    }

    private fun datePicker(dateType: DateType) = with(binding) {
        val datePickerFragment = DatePickerFragment()
        val supportFragmentManager = requireActivity().supportFragmentManager
        supportFragmentManager.setFragmentResultListener(
            "REQUEST_KEY", viewLifecycleOwner
        ) { resultKey, bundle ->
            if (resultKey == "REQUEST_KEY") {
                val date = bundle.getString("SELECTED_DATE")
                when (dateType) {
                    DateType.MIN_DATE -> filtersLayout.selectedMinDateText.text = date
                    DateType.MAX_DATE -> filtersLayout.selectedMaxDateText.text = date
                }
            }
        }
        datePickerFragment.show(supportFragmentManager, "DatePickerFragment")
    }

}