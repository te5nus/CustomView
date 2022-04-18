package com.example.testcustomview

import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {

    private var customView: CustomView? = null
    private val list = mutableListOf("$50", "$100", "$200")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        customView = findViewById(R.id.custom)

        customView?.apply {
            setElements(list)
            setListener(object : CustomViewListener {
                override fun onValueSelected(value: String) = Unit
            })
        }
    }

}