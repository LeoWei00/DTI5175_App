package com.example.shareat

import android.content.Intent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.auth.FirebaseAuth

class ConversationAdapter(
    private val conversations: MutableList<Conversation>
) : RecyclerView.Adapter<ConversationAdapter.ConversationViewHolder>() {

    class ConversationViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvConversationName: TextView = itemView.findViewById(R.id.tvConversationName)
        val tvConversationPostTitle: TextView = itemView.findViewById(R.id.tvConversationPostTitle)
        val tvConversationLastMessage: TextView = itemView.findViewById(R.id.tvConversationLastMessage)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ConversationViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_conversation, parent, false)
        return ConversationViewHolder(view)
    }

    override fun onBindViewHolder(holder: ConversationViewHolder, position: Int) {
        val conversation = conversations[position]
        val currentUserId = FirebaseAuth.getInstance().currentUser?.uid ?: ""

        val otherUserName = if (conversation.participants.size >= 2 && conversation.participant_names.size >= 2) {
            if (conversation.participants[0] == currentUserId) {
                conversation.participant_names[1]
            } else {
                conversation.participant_names[0]
            }
        } else {
            "Conversation"
        }

        val otherUserId = if (conversation.participants.size >= 2) {
            if (conversation.participants[0] == currentUserId) {
                conversation.participants[1]
            } else {
                conversation.participants[0]
            }
        } else {
            ""
        }

        holder.tvConversationName.text = otherUserName
        holder.tvConversationPostTitle.text = "About: ${conversation.post_title}"
        holder.tvConversationLastMessage.text =
            if (conversation.last_message.isNotEmpty()) conversation.last_message else "No messages yet"

        holder.itemView.setOnClickListener {
            val context = holder.itemView.context
            val intent = Intent(context, ChatActivity::class.java)

            intent.putExtra("OTHER_USER_ID", otherUserId)
            intent.putExtra("OTHER_USER_NAME", otherUserName)
            intent.putExtra("POST_ID", conversation.post_id)
            intent.putExtra("POST_TITLE", conversation.post_title)

            context.startActivity(intent)
        }
    }

    override fun getItemCount(): Int = conversations.size
}