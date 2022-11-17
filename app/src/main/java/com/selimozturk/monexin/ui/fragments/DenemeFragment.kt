package com.selimozturk.monexin.ui.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.selimozturk.monexin.databinding.FragmentDenemeBinding


class DenemeFragment : Fragment() {

    private lateinit var binding: FragmentDenemeBinding
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentDenemeBinding.inflate(inflater, container, false)
        return binding.root
    }

}