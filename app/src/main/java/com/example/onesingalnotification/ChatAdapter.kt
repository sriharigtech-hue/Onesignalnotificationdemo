package com.example.onesingalnotification
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.auth.FirebaseAuth

class ChatAdapter(private val list: List<ChatModel>) :
    RecyclerView.Adapter<ChatAdapter.ViewHolder>() {
    private val currentUserId = FirebaseAuth.getInstance().uid
    companion object {
        private const val TYPE_SENDER = 0
        private const val TYPE_RECEIVER = 1
    }

    override fun getItemViewType(position: Int): Int {
        return if (list[position].senderId == currentUserId) TYPE_SENDER else TYPE_RECEIVER
    }

    inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvMsg: TextView = view.findViewById(R.id.tvMessage)
    }


    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val layoutId = if (viewType == TYPE_SENDER)
            R.layout.item_chat_sender
        else
            R.layout.item_chat_receiver

        val view = LayoutInflater.from(parent.context)
            .inflate(layoutId, parent, false)
        return ViewHolder(view)
    }

    override fun getItemCount() = list.size

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.tvMsg.text = list[position].message
    }
}