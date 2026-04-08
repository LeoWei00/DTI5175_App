package com.example.shareat

import android.os.Bundle
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query

class MessagesActivity : AppCompatActivity() {

    private lateinit var recyclerConversations: RecyclerView
    private lateinit var tvConversationCount: TextView
    private lateinit var btnBackMessages: ImageButton

    private lateinit var db: FirebaseFirestore
    private lateinit var adapter: ConversationAdapter
    private val conversationList = mutableListOf<Conversation>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_messages)

        recyclerConversations = findViewById(R.id.recyclerConversations)
        tvConversationCount = findViewById(R.id.tvConversationCount)
        btnBackMessages = findViewById(R.id.btnBackMessages)

        db = FirebaseFirestore.getInstance()

        adapter = ConversationAdapter(conversationList)
        recyclerConversations.layoutManager = LinearLayoutManager(this)
        recyclerConversations.adapter = adapter

        btnBackMessages.setOnClickListener {
            finish()
        }

        loadConversations()
    }

    private fun loadConversations() {
        val currentUser = FirebaseAuth.getInstance().currentUser
        if (currentUser == null) {
            Toast.makeText(this, "You must be logged in", Toast.LENGTH_LONG).show()
            return
        }

        db.collection("chats")
            .whereArrayContains("participants", currentUser.uid)
            .orderBy("last_message_at", Query.Direction.DESCENDING)
            .get()
            .addOnSuccessListener { result ->
                conversationList.clear()

                for (document in result) {
                    val conversation = document.toObject(Conversation::class.java)
                    conversationList.add(conversation)
                }

                adapter.notifyDataSetChanged()
                tvConversationCount.text = conversationList.size.toString() + " chats"
            }
            .addOnFailureListener { e ->
                Toast.makeText(
                    this,
                    "Error loading chats: ${e.message}",
                    Toast.LENGTH_LONG
                ).show()
            }
    }
}