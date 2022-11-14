package com.selimozturk.monexin.ui.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView.OnItemClickListener
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.android.volley.Request
import com.android.volley.toolbox.StringRequest
import com.android.volley.toolbox.Volley
import com.selimozturk.monexin.R
import com.selimozturk.monexin.databinding.FragmentDenemeBinding
import com.selimozturk.monexin.utils.showToast
import org.json.JSONObject


class DenemeFragment : Fragment() {

    private lateinit var binding: FragmentDenemeBinding
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FragmentDenemeBinding.inflate(inflater, container, false)
        return binding.root
    }
}