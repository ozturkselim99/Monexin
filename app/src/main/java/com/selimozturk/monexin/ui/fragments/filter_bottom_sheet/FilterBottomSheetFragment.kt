package com.selimozturk.monexin.ui.fragments.filter_bottom_sheet

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import androidx.core.widget.addTextChangedListener
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.selimozturk.monexin.R
import com.selimozturk.monexin.databinding.FragmentFilterBottomSheetBinding
import com.selimozturk.monexin.ui.fragments.date_picker.DatePickerFragment
import com.selimozturk.monexin.utils.convertToTimestamp
import com.selimozturk.monexin.utils.setVisible
import com.selimozturk.monexin.utils.showToast

class FilterBottomSheetFragment : BottomSheetDialogFragment() {

    private lateinit var binding: FragmentFilterBottomSheetBinding

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FragmentFilterBottomSheetBinding.inflate(inflater, container, false)
        initViews()
        return binding.root
    }

    private fun initViews() {
        val examples = resources.getStringArray(R.array.example)
        val arrayAdapter = ArrayAdapter(requireContext(), R.layout.dropdown_item, examples)

        binding.example.setAdapter(arrayAdapter)

        binding.transactionMinDateLayout.setOnClickListener {
            datePicker(0)
        }
        binding.transactionMaxDateLayout.setOnClickListener {
            datePicker(1)
        }
        binding.filterButton.setOnClickListener {
            dismiss()
        }
    }

    private fun datePicker(dateType:Int){
        val datePickerFragment = DatePickerFragment()
        val supportFragmentManager = requireActivity().supportFragmentManager
        // we have to implement setFragmentResultListener
        supportFragmentManager.setFragmentResultListener(
            "REQUEST_KEY",
            viewLifecycleOwner
        ) { resultKey, bundle ->
            if (resultKey == "REQUEST_KEY") {
                val date = bundle.getString("SELECTED_DATE")
                if (dateType==0){
                    binding.selectedMinDateText.text=date
                }
                else{
                    binding.selectedMaxDateText.text=date
                }
            }
        }
        datePickerFragment.show(supportFragmentManager, "DatePickerFragment")
    }



}