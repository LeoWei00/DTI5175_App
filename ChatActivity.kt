package com.example.shareeat

import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

class MainActivity : AppCompatActivity() {

    private lateinit var recyclerMessages: RecyclerView
    private lateinit var edtMessage: EditText
    private lateinit var btnSend: Button

    private val messages = mutableListOf<String>()
    private lateinit var chatAdapter: ChatAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.`activity_chat.xml`)

        recyclerMessages = findViewById(R.id.recyclerMessages)
        edtMessage = findViewById(R.id.edtMessage)
        btnSend = findViewById(R.id.btnSend)

        chatAdapter = ChatAdapter(messages)
        recyclerMessages.layoutManager = LinearLayoutManager(this)
        recyclerMessages.adapter = chatAdapter

        btnSend.setOnClickListener {
            val messageText = edtMessage.text.toString().trim()

            if (messageText.isNotEmpty()) {
                messages.add(messageText)
                chatAdapter.notifyItemInserted(messages.size - 1)
                recyclerMessages.scrollToPosition(messages.size - 1)
                edtMessage.text.clear()
            }
        }
    }
}