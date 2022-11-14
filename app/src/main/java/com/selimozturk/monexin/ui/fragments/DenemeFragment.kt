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
    val list: MutableList<BerkayModel> = ArrayList()
    val url = "https://demonuts.com/Demonuts/JsonTest/Tennis/json_parsing.php"
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FragmentDenemeBinding.inflate(inflater, container, false)
        val examples = resources.getStringArray(R.array.example)
        downloadTask()
        return binding.root
    }

    private fun downloadTask() {
        val queue = Volley.newRequestQueue(requireContext())
        val stringRequest = StringRequest(Request.Method.GET, url,
            { response ->
                context?.showToast(response.toString())
                val data = response.toString()
                val jObj = JSONObject(data)
                val jArray = jObj.getJSONArray("data")
                for (i in 0 until jArray.length()){
                    val jsonObject=jArray.getJSONObject(i)
                    val name=jsonObject.getString("name")
                    val imgURL=jsonObject.getString("imgURL")
                    val m=BerkayModel(name,imgURL)
                    list.add(m)
                }
                val arrayAdapter = ArrayAdapter(requireContext(), R.layout.dropdown_item, list)
                binding.example.setAdapter(arrayAdapter)
                binding.example.onItemClickListener = OnItemClickListener { parent, arg1, pos, id ->
                    val value = arrayAdapter.getItem(pos) ?: ""
                    Toast.makeText(
                        requireContext(),
                        value.toString(),
                        Toast.LENGTH_LONG
                    ).show()
                }
            },
            { error ->
                context?.showToast(error.toString())
            })
        queue.add(stringRequest)


    }

}