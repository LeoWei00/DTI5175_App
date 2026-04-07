package com.example.shareat

import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query

class ChatActivity : AppCompatActivity() {

    private lateinit var recyclerMessages: RecyclerView
    private lateinit var edtMessage: EditText
    private lateinit var btnSend: Button
    private lateinit var btnBackChat: ImageButton
    private lateinit var tvChatTitle: TextView

    private lateinit var chatAdapter: ChatAdapter
    private val messages = mutableListOf<ChatMessage>()

    private lateinit var db: FirebaseFirestore

    private var chatId: String = ""
    private var currentUserId: String = ""
    private var currentUserName: String = "User"
    private var otherUserId: String = ""
    private var otherUserName: String = "User"
    private var postId: String = ""
    private var postTitle: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_chat)

        recyclerMessages = findViewById(R.id.recyclerMessages)
        edtMessage = findViewById(R.id.edtMessage)
        btnSend = findViewById(R.id.btnSend)
        btnBackChat = findViewById(R.id.btnBackChat)
        tvChatTitle = findViewById(R.id.tvChatTitle)

        db = FirebaseFirestore.getInstance()

        val currentUser = FirebaseAuth.getInstance().currentUser
        if (currentUser == null) {
            Toast.makeText(this, "You must be logged in", Toast.LENGTH_LONG).show()
            finish()
            return
        }

        currentUserId = currentUser.uid
        currentUserName = currentUser.displayName ?: "User"

        otherUserId = intent.getStringExtra("OTHER_USER_ID") ?: ""
        otherUserName = intent.getStringExtra("OTHER_USER_NAME") ?: "User"
        postId = intent.getStringExtra("POST_ID") ?: ""
        postTitle = intent.getStringExtra("POST_TITLE") ?: "Meal Chat"

        if (otherUserId.isEmpty() || postId.isEmpty()) {
            Toast.makeText(this, "Missing chat information", Toast.LENGTH_LONG).show()
            finish()
            return
        }

        val ids = listOf(currentUserId, otherUserId).sorted()
        chatId = "${postId}_${ids[0]}_${ids[1]}"

        tvChatTitle.text = otherUserName

        chatAdapter = ChatAdapter(messages)
        recyclerMessages.layoutManager = LinearLayoutManager(this)
        recyclerMessages.adapter = chatAdapter

        btnBackChat.setOnClickListener {
            finish()
        }

        btnSend.setOnClickListener {
            sendMessage()
        }

        createChatIfNeeded()
        listenForMessages()
    }

    private fun createChatIfNeeded() {
        val chatDoc = hashMapOf(
            "chat_id" to chatId,
            "participants" to listOf(currentUserId, otherUserId),
            "participant_names" to listOf(currentUserName, otherUserName),
            "post_id" to postId,
            "post_title" to postTitle,
            "last_message" to "",
            "last_message_at" to 0L,
            "created_at" to System.currentTimeMillis()
        )

        db.collection("chats")
            .document(chatId)
            .get()
            .addOnSuccessListener { document ->
                if (!document.exists()) {
                    db.collection("chats")
                        .document(chatId)
                        .set(chatDoc)
                }
            }
    }

    private fun listenForMessages() {
        db.collection("chats")
            .document(chatId)
            .collection("messages")
            .orderBy("sent_at", Query.Direction.ASCENDING)
            .addSnapshotListener { snapshot, e ->
                if (e != null) {
                    Toast.makeText(this, "Error loading messages", Toast.LENGTH_SHORT).show()
                    return@addSnapshotListener
                }

                messages.clear()

                snapshot?.documents?.forEach { doc ->
                    val message = doc.toObject(ChatMessage::class.java)
                    if (message != null) {
                        messages.add(message)
                    }
                }

                chatAdapter.notifyDataSetChanged()

                if (messages.isNotEmpty()) {
                    recyclerMessages.scrollToPosition(messages.size - 1)
                }
            }
    }

    private fun sendMessage() {
        val text = edtMessage.text.toString().trim()
        if (text.isEmpty()) return

        val messageRef = db.collection("chats")
            .document(chatId)
            .collection("messages")
            .document()

        val currentTime = System.currentTimeMillis()

        val message = hashMapOf(
            "message_id" to messageRef.id,
            "sender_id" to currentUserId,
            "sender_name" to currentUserName,
            "text" to text,
            "sent_at" to currentTime
        )

        messageRef.set(message)
            .addOnSuccessListener {
                db.collection("chats")
                    .document(chatId)
                    .update(
                        mapOf(
                            "last_message" to text,
                            "last_message_at" to currentTime
                        )
                    )

                edtMessage.setText("")
            }
            .addOnFailureListener { err ->
                Toast.makeText(this, "Failed to send: ${err.message}", Toast.LENGTH_LONG).show()
            }
    }
}