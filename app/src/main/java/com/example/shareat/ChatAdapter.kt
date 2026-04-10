package com.example.shareat

import android.graphics.Color
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.auth.FirebaseAuth


class ChatAdapter(
    private val messages: MutableList<ChatMessage>
) : RecyclerView.Adapter<ChatAdapter.ChatViewHolder>() {

    class ChatViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val messageContainer: LinearLayout = itemView.findViewById(R.id.messageContainer)
        val tvMessageText: TextView = itemView.findViewById(R.id.tvMessageText)
        val tvSenderName: TextView = itemView.findViewById(R.id.tvSenderName)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ChatViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_message, parent, false)
        return ChatViewHolder(view)
    }

    override fun onBindViewHolder(holder: ChatViewHolder, position: Int) {
        val message = messages[position]
        val currentUserId = FirebaseAuth.getInstance().currentUser?.uid ?: ""

        holder.tvMessageText.text = message.text
        holder.tvSenderName.text = message.sender_name

        val rootLayout = holder.itemView as LinearLayout
        val containerParams = holder.messageContainer.layoutParams as LinearLayout.LayoutParams

        if (message.sender_id == currentUserId) {
            holder.messageContainer.setBackgroundResource(R.drawable.chat_bubble_me)
            rootLayout.gravity = Gravity.END
            containerParams.gravity = Gravity.END
            holder.tvSenderName.visibility = View.GONE
            holder.tvMessageText.setTextColor(Color.WHITE)
        } else {
            holder.messageContainer.setBackgroundResource(R.drawable.chat_bubble_other)
            rootLayout.gravity = Gravity.START
            containerParams.gravity = Gravity.START
            holder.tvSenderName.visibility = View.VISIBLE
            holder.tvMessageText.setTextColor(Color.parseColor("#1E2B27"))
        }

        holder.messageContainer.layoutParams = containerParams
    }

    override fun getItemCount(): Int = messages.size
}