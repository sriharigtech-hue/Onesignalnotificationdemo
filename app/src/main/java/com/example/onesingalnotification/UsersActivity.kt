package com.example.onesingalnotification

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class UsersActivity : AppCompatActivity() {

    private lateinit var recyclerView: RecyclerView
    private val userList = ArrayList<UserModel>()
    private lateinit var adapter: UsersAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_users)

        recyclerView = findViewById(R.id.recyclerUsers)
        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView = findViewById(R.id.recyclerUsers)
        recyclerView.layoutManager = LinearLayoutManager(this)
        adapter = UsersAdapter(this, userList)
        recyclerView.adapter = adapter


        loadUsers()
    }
    override fun onResume() {
        super.onResume()
        // Forces RecyclerView to reload all profile images
        adapter.notifyDataSetChanged()
    }

    private fun loadUsers() {
        val myUid = FirebaseAuth.getInstance().uid ?: return

        FirebaseFirestore.getInstance()
            .collection("users")
            .get()
            .addOnSuccessListener { result ->
                userList.clear()

                for (doc in result) {
                    val uid = doc.getString("uid") ?: ""
                    val name = doc.getString("name") ?: "User"
                    val oneSignalId = doc.getString("oneSignalId") ?: ""

                    if (uid != myUid) {
                        userList.add(
                            UserModel(uid, name, oneSignalId) //  FULL MODEL
                        )
                    }
                }
                adapter.notifyDataSetChanged()
            }
    }
}
