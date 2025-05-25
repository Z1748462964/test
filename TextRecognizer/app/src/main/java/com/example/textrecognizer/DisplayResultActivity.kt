package com.example.textrecognizer

import android.os.Bundle
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

class DisplayResultActivity : AppCompatActivity() {

    // 视图控件 (View Controls)
    private lateinit var recognizedTextView: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_display_result)

        // 初始化视图 (Initialize Views)
        recognizedTextView = findViewById(R.id.recognizedTextView)

        // 获取意图 (Get Intent)
        val intent = intent
        if (intent != null && intent.hasExtra("recognized_text")) {
            val recognizedText = intent.getStringExtra("recognized_text")
            // 显示识别到的文本 (Display the recognized text)
            recognizedTextView.text = recognizedText
        } else {
            // 如果没有文本传递过来，显示错误或默认信息 (Show error or default message if no text is passed)
            recognizedTextView.text = "未能获取到识别文本。"
        }

        // 设置标题 (Set Title)
        supportActionBar?.title = "识别结果"
    }
}
