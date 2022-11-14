package com.selimozturk.monexin.ui.activities

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.selimozturk.monexin.R
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class IntroActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_intro)
    }
}