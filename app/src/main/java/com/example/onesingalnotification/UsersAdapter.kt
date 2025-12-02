package com.example.onesingalnotification

import android.content.Context
import android.content.Intent
import android.graphics.BitmapFactory
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class UsersAdapter(
    private val context: Context,
    private val list: List<UserModel>
) : RecyclerView.Adapter<UsersAdapter.ViewHolder>() {

    inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvName: TextView = view.findViewById(R.id.tvUsername)
        val tvLastMsg: TextView = view.findViewById(R.id.tvLastMsg)
        val imgProfile: ImageView = view.findViewById(R.id.profileImage)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_user, parent, false)
        return ViewHolder(view)
    }

    override fun getItemCount() = list.size

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val user = list[position]

        holder.tvName.text = user.name
        holder.tvLastMsg.text = "Tap to chat"

        loadProfileImage(holder.imgProfile)

        //  CLICK ON PROFILE IMAGE → OPEN PROFILE ACTIVITY
        holder.imgProfile.setOnClickListener {
            val intent = Intent(it.context, ProfileActivity::class.java)
            it.context.startActivity(intent)
        }

        //  CLICK ON NAME / MESSAGE → OPEN CHAT
        holder.tvName.setOnClickListener {
            val intent = Intent(it.context, ChatActivity::class.java)
            intent.putExtra("receiverId", user.uid)
            it.context.startActivity(intent)
        }

        holder.tvLastMsg.setOnClickListener {
            val intent = Intent(it.context, ChatActivity::class.java)
            intent.putExtra("receiverId", user.uid)
            it.context.startActivity(intent)
        }


    }

    // ✅ Load locally saved profile image
    private fun loadProfileImage(imageView: ImageView) {
        val path = context
            .getSharedPreferences("PROFILE", Context.MODE_PRIVATE)
            .getString("image_path", null)

        if (path != null) {
            val bitmap = BitmapFactory.decodeFile(path)
            imageView.setImageBitmap(bitmap)
        }
    }
}
