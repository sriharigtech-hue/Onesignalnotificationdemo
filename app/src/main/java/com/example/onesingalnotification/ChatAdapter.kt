package com.example.onesingalnotification
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.auth.FirebaseAuth

class ChatAdapter(private val list: List<ChatItem>) :
    RecyclerView.Adapter<ChatAdapter.ViewHolder>() {
    private val currentUserId = FirebaseAuth.getInstance().uid
    companion object {
        private const val TYPE_DATE = 0
        private const val TYPE_SENDER = 1
        private const val TYPE_RECEIVER = 2
    }


    override fun getItemViewType(position: Int): Int {
        return when (val item = list[position]) {
            is ChatItem.DateItem -> TYPE_DATE
            is ChatItem.MessageItem -> {
                if (item.chat.senderId == currentUserId)
                    TYPE_SENDER
                else
                    TYPE_RECEIVER
            }
        }
    }


    inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvMsg: TextView? = view.findViewById(R.id.tvMessage)
        val tvTime: TextView? = view.findViewById(R.id.tvTime)
        val tvDate: TextView? = view.findViewById(R.id.tvDate)

        val imgStatus: ImageView? = view.findViewById(R.id.imgStatus)

    }



    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {

        val layoutId = when (viewType) {
            TYPE_DATE -> R.layout.item_chat_date
            TYPE_SENDER -> R.layout.item_chat_sender
            else -> R.layout.item_chat_receiver
        }

        val view = LayoutInflater.from(parent.context)
            .inflate(layoutId, parent, false)

        return ViewHolder(view)
    }


    override fun getItemCount() = list.size

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = list[position]

        when(item) {
            is ChatItem.DateItem -> {
                holder.tvDate?.text = item.dateText
                holder.tvDate?.visibility = View.VISIBLE

                // Hide message views
                holder.tvMsg?.visibility = View.GONE
                holder.tvTime?.visibility = View.GONE
                holder.imgStatus?.visibility = View.GONE
            }

            is ChatItem.MessageItem -> {
                holder.tvMsg?.visibility = View.VISIBLE
                holder.tvTime?.visibility = View.VISIBLE
                holder.imgStatus?.visibility = View.VISIBLE
                holder.tvDate?.visibility = View.GONE // hide date for messages

                holder.tvMsg?.text = item.chat.message
                holder.tvTime?.text = android.text.format.DateFormat.format("hh:mm a", item.chat.time)

                when(item.chat.status) {
                    "sent" -> holder.imgStatus?.setImageResource(R.drawable.ic_tick_single)
                    "delivered" -> holder.imgStatus?.setImageResource(R.drawable.ic_tick_double_grey)
                    "seen" -> holder.imgStatus?.setImageResource(R.drawable.ic_tick_double_grey)
                }
            }
        }
    }


}