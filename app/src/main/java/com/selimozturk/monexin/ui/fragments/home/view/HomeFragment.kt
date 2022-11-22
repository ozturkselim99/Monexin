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
import com.selimozturk.monexin.R
import com.selimozturk.monexin.adapter.TransactionAdapter
import com.selimozturk.monexin.databinding.FragmentHomeBinding
import com.selimozturk.monexin.model.Transactions
import com.selimozturk.monexin.ui.fragments.date_picker.DatePickerFragment
import com.selimozturk.monexin.ui.fragments.home.viewmodel.HomeViewModel
import com.selimozturk.monexin.utils.Resource
import com.selimozturk.monexin.utils.convertToTimestamp
import com.selimozturk.monexin.utils.setVisible
import com.selimozturk.monexin.utils.showToast
import dagger.hilt.android.AndroidEntryPoint


@AndroidEntryPoint
class HomeFragment : Fragment() {

    private lateinit var binding: FragmentHomeBinding
    private var minDate: String= "0"
    private var maxDate: String=System.currentTimeMillis().toString()
    private val homeViewModel by viewModels<HomeViewModel>()
    private val adapter = TransactionAdapter()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentHomeBinding.inflate(inflater, container, false)
        minDate = "0"
        maxDate = System.currentTimeMillis().toString()
        setupRecyclerview()
        getHomeInfo(minDate, maxDate)
        initViews()
        dateRangeFilterControl()
        return binding.root
    }

    private fun dateRangeFilterClear() {
        binding.selectedMaxDateText.setText(R.string.max_date)
        binding.selectedMinDateText.setText(R.string.min_date)
        minDate = "0"
        maxDate = System.currentTimeMillis().toString()
        getHomeInfo(minDate, maxDate)
        binding.dateRangeClearButton.setVisible(false)
    }

    private fun dateRangeFilterControl() {
        binding.selectedMinDateText.addTextChangedListener {
            if (it.toString() != "Min Date") {
                minDate = (it.toString().convertToTimestamp()).toString()
                if (maxDate.isNotEmpty() && (minDate.toLong() >= maxDate.toLong())) {
                    context?.showToast("Minimum date must be less than the maximum date")
                    binding.selectedMinDateText.setText(R.string.min_date)
                } else {
                    binding.dateRangeClearButton.setVisible(true)
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
                    binding.selectedMaxDateText.setText(R.string.max_date)
                } else {
                    getHomeInfo(minDate, maxDate)
                }
            }
        }
    }

    private fun createDonutView(activeExpense: Float, activeIncome: Float) {
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
        binding.donutView.submitData(listOf(section1, section2))
    }

    private fun setupRecyclerview() {
        val layoutManager = LinearLayoutManager(requireContext())
        binding.transactionsRW.layoutManager = layoutManager
    }

    private fun loadTransactions(transactions: List<Transactions>) {
        adapter.items=transactions
        binding.transactionsRW.adapter = adapter
        adapter.onItemClicked = { transaction ->
            val direction =
                HomeFragmentDirections.actionHomeFragmentToTransactionDetail(transaction)
            findNavController().navigate(direction)
        }
    }

    private fun initViews() {
        binding.transactionMinDateLayout.setOnClickListener {
            datePicker(0)
        }
        binding.transactionMaxDateLayout.setOnClickListener {
            datePicker(1)
        }
        binding.addTransactionButton.setOnClickListener {
            val direction = HomeFragmentDirections.actionHomeFragmentToTransactionFragment(null)
            findNavController().navigate(direction)
        }
        binding.dateRangeClearButton.setOnClickListener {
            dateRangeFilterClear()
        }
    }

    private fun getHomeInfo(minDate: String?, maxDate: String?) {
        homeViewModel.getHomeInfo(minDate, maxDate)
        homeViewModel.homeInfoState.observe(viewLifecycleOwner) {
            when (it) {
                is Resource.Success -> {
                    binding.homeInfoProgressBar.setVisible(false)
                    binding.expenseAmountText.text = it.result.activeExpense
                    binding.incomesAmountText.text = it.result.activeIncome
                    binding.userDisplayNameText.text = "Hello, " + it.result.displayName
                    createDonutView(
                        it.result.activeExpense.toFloat(),
                        it.result.activeIncome.toFloat()
                    )
                    binding.transactionNotFoundText.setVisible(it.result.recentlyAdded.isEmpty())
                    loadTransactions(it.result.recentlyAdded)
                }
                is Resource.Loading -> {
                    binding.homeInfoProgressBar.setVisible(true)
                }
                is Resource.Failure -> {
                    context?.showToast(it.exception.message.toString())
                    binding.homeInfoProgressBar.setVisible(false)
                }
            }
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