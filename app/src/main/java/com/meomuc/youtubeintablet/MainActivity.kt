package com.meomuc.youtubeintablet

import android.app.Activity
import android.os.Bundle
import android.widget.TextView

/** Khung ticket #2: chỉ mở được app. Chưa có WebView (ticket #3). */
class MainActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(TextView(this).apply {
            text = getString(R.string.app_name)
            setTextColor(android.graphics.Color.BLACK)
            gravity = android.view.Gravity.CENTER
            textSize = 24f
        })
    }
}
