package com.example.onesingalnotification

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.onesignal.OneSignal
import android.widget.Button
import android.widget.EditText

import android.widget.Toast
class MainActivity : AppCompatActivity() {
    private lateinit var etEmail: EditText
    private lateinit var etPassword: EditText
    private lateinit var btnLogin: Button
    private lateinit var etReceiverUid: EditText
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        etEmail = findViewById(R.id.etEmail)
        etPassword = findViewById(R.id.etPassword)
        btnLogin = findViewById(R.id.btnLogin)
        etReceiverUid = findViewById(R.id.etReceiverUid)

        btnLogin.setOnClickListener {
            val email = etEmail.text.toString()
            val password = etPassword.text.toString()
            val receiverUid = etReceiverUid.text.toString()

            if(email.isEmpty() || password.isEmpty() || receiverUid.isEmpty()){
                Toast.makeText(this, "Fill all fields", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            login(email, password, receiverUid)
        }
    }
    private fun login(email: String, password: String, receiverUid: String) {
        FirebaseAuth.getInstance()
            .signInWithEmailAndPassword(email, password)
            .addOnSuccessListener {
                saveUserToFirestore()

                val intent = Intent(this, ChatActivity::class.java)
                intent.putExtra("receiverId", receiverUid)
                startActivity(intent)
                finish()
            }
            .addOnFailureListener {
                Toast.makeText(this, "Login Failed: ${it.message}", Toast.LENGTH_SHORT).show()
            }
    }
    private fun saveUserToFirestore() {
        val uid = FirebaseAuth.getInstance().uid!!
        val oneSignalId = OneSignal.User.pushSubscription.id

        val map = hashMapOf(
            "uid" to uid,
            "oneSignalId" to oneSignalId
        )

        FirebaseFirestore.getInstance()
            .collection("users")
            .document(uid)
            .set(map)
    }
}