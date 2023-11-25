package com.kien.android_bubble

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import com.kien.android_bubble.databinding.ActivityMainBinding
import com.kien.bubble.BubbleManager

class MainActivity : AppCompatActivity() {
    private val viewBinding: ActivityMainBinding by lazy {
        ActivityMainBinding.inflate(layoutInflater)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(viewBinding.root)

        viewBinding.btnAddBubble.setOnClickListener { BubbleManager.startBubble(this) }
    }
}