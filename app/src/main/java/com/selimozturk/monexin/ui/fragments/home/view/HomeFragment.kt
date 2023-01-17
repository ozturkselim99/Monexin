package com.selimozturk.monexin.ui.fragments.home.view

import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.widget.addTextChangedListener
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import app.futured.donut.DonutSection
import com.google.android.material.appbar.AppBarLayout
import com.selimozturk.monexin.R
import com.selimozturk.monexin.ui.TransactionAdapter
import com.selimozturk.monexin.databinding.FragmentHomeBinding
import com.selimozturk.monexin.model.Transactions
import com.selimozturk.monexin.ui.fragments.date_picker.DatePickerFragment
import com.selimozturk.monexin.ui.fragments.home.viewmodel.HomeViewModel
import com.selimozturk.monexin.utils.*
import dagger.hilt.android.AndroidEntryPoint


@AndroidEntryPoint
class HomeFragment : Fragment() {

    private lateinit var binding: FragmentHomeBinding
    private var minDate: String = "0"
    private var maxDate: String = System.currentTimeMillis().toString()
    private val homeViewModel by viewModels<HomeViewModel>()
    private val adapter = TransactionAdapter()
    private val offsetChangedListener = object : AppBarLayout.OnOffsetChangedListener {
        override fun onOffsetChanged(appBarLayout: AppBarLayout?, verticalOffset: Int) {
            val toEnable = verticalOffset == 0
            if (!binding.homeSwipeRefreshLayout.isEnabled.xor(toEnable)) return
            binding.homeSwipeRefreshLayout.isEnabled = toEnable
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentHomeBinding.inflate(inflater, container, false)
        minDate = "0"
        maxDate = System.currentTimeMillis().toString()
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupRecyclerview()
        getHomeInfo(minDate, maxDate)
        initViews()
        dateRangeFilterControl()
    }

    override fun onPause() = with(binding) {
        super.onPause()
        appBarLayout.removeOnOffsetChangedListener(offsetChangedListener)
    }

    private fun dateRangeFilterClear() = with(binding) {
        selectedMaxDateText.setText(R.string.max_date)
        selectedMinDateText.setText(R.string.min_date)
        minDate = "0"
        maxDate = System.currentTimeMillis().toString()
        getHomeInfo(minDate, maxDate)
        dateRangeClearButton.setVisible(false)
    }

    private fun dateRangeFilterControl() = with(binding) {
        selectedMinDateText.addTextChangedListener {
            if (it.toString() != "Min Date") {
                minDate = (it.toString().convertToTimestamp()).toString()
                if (maxDate.isNotEmpty() && (minDate.toLong() >= maxDate.toLong())) {
                    context?.showToast("Minimum date must be less than the maximum date")
                    selectedMinDateText.setText(R.string.min_date)
                } else {
                    dateRangeClearButton.setVisible(true)
                    getHomeInfo(minDate, maxDate)
                }
            }
        }
        binding.selectedMaxDateText.addTextChangedListener {
            if (it.toString() != "Max Date") {
                //86400000(1 gün) seçilen max date kapsaması için topluyorum
                maxDate = (it.toString().convertToTimestamp() + 86400000).toString()
                if (minDate.isNotEmpty() && (maxDate.toLong() <= minDate.toLong())) {
                    context?.showToast("Maximum date must be greater than the minimum date")
                    selectedMaxDateText.setText(R.string.max_date)
                } else {
                    getHomeInfo(minDate, maxDate)
                }
            }
        }
    }

    private fun createDonutView(activeExpense: Float, activeIncome: Float) = with(binding) {
        val section1 = DonutSection(
            name = "expense",
            color = Color.parseColor("#FB1D32"),
            amount = activeExpense,
        )

        val section2 = DonutSection(
            name = "income",
            color = Color.parseColor("#FFB98E"),
            amount = activeIncome,
        )
        donutView.submitData(listOf(section1, section2))
    }

    private fun setupRecyclerview() = with(binding) {
        val layoutManager = LinearLayoutManager(requireContext())
        transactionsRW.layoutManager = layoutManager
    }

    private fun loadTransactions(transactions: List<Transactions>) = with(binding) {
        adapter.items = transactions
        transactionsRW.adapter = adapter
        adapter.onItemClicked = { transaction ->
            val direction =
                HomeFragmentDirections.actionHomeFragmentToTransactionDetail(transaction)
            findNavController().navigate(direction)
        }
    }

    private fun initViews() = with(binding) {
        appBarLayout.addOnOffsetChangedListener(offsetChangedListener)
        homeSwipeRefreshLayout.setOnRefreshListener {
            getHomeInfo(minDate, maxDate)
            homeSwipeRefreshLayout.isRefreshing = false
        }
        transactionMinDateLayout.setOnClickListener {
            datePicker(DateType.MIN_DATE)
        }
        transactionMaxDateLayout.setOnClickListener {
            datePicker(DateType.MAX_DATE)
        }
        addTransactionButton.setOnClickListener {
            val direction = HomeFragmentDirections.actionHomeFragmentToTransactionFragment(null)
            findNavController().navigate(direction)
        }
        dateRangeClearButton.setOnClickListener {
            dateRangeFilterClear()
        }
    }

    private fun getHomeInfo(minDate: String?, maxDate: String?) = with(binding) {
        homeViewModel.getHomeInfo(minDate, maxDate)
        homeViewModel.homeInfoState.observe(viewLifecycleOwner) {
            when (it) {
                is Resource.Success -> {
                    homeInfoProgressBar.setVisible(false)
                    expenseAmountText.text = it.result.activeExpense
                    incomesAmountText.text = it.result.activeIncome
                    userDisplayNameText.text = "Hello, " + it.result.displayName
                    createDonutView(
                        it.result.activeExpense.toFloat(),
                        it.result.activeIncome.toFloat()
                    )
                    transactionNotFoundText.setVisible(it.result.recentlyAdded.isEmpty())
                    loadTransactions(it.result.recentlyAdded)
                }
                is Resource.Loading -> {
                    homeInfoProgressBar.setVisible(true)
                }
                is Resource.Failure -> {
                    context?.showToast(it.exception.message.toString())
                    homeInfoProgressBar.setVisible(false)
                }
            }
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
                    selectedMinDateText.text = date
                } else {
                    selectedMaxDateText.text = date
                }
            }
        }
        datePickerFragment.show(supportFragmentManager, "DatePickerFragment")
    }

}