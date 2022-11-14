package com.selimozturk.monexin

import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.ImageView
import androidx.core.net.toFile
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.bumptech.glide.Glide

class TakenPhotoFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        val view = inflater.inflate(R.layout.fragment_taken_photo, container, false)
        Glide.with(requireContext())
            .load(arguments?.get("uri"))
            .into(view.findViewById<ImageView>(R.id.imageExample))

        view.findViewById<ImageButton>(R.id.imageButton).setOnClickListener {
            var uri: Uri = arguments?.get("uri") as Uri
            uri.toFile().delete()
            findNavController().popBackStack()
        }
        view.findViewById<ImageButton>(R.id.imageButton2).setOnClickListener {
            val direction =
                TakenPhotoFragmentDirections.actionTakenPhotoFragmentToTransactionFragment(null)
            findNavController().navigate(direction)
        }
        return view
    }

}