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
import com.google.firebase.firestore.SetOptions

class MainActivity : AppCompatActivity() {

    private lateinit var etEmail: EditText
    private lateinit var etPassword: EditText
    private lateinit var btnLogin: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val currentUser = FirebaseAuth.getInstance().currentUser
        if (currentUser != null) {
            startActivity(Intent(this, UsersActivity::class.java))
            finish()
            return
        }

        setContentView(R.layout.activity_main)

        etEmail = findViewById(R.id.etEmail)
        etPassword = findViewById(R.id.etPassword)
        btnLogin = findViewById(R.id.btnLogin)

        btnLogin.setOnClickListener {
            val email = etEmail.text.toString()
            val password = etPassword.text.toString()

            if (email.isEmpty() || password.isEmpty()) {
                Toast.makeText(this, "Fill all fields", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            login(email, password)
        }
    }

    private fun login(email: String, password: String) {
        FirebaseAuth.getInstance()
            .signInWithEmailAndPassword(email, password)
            .addOnSuccessListener {
                saveUserToFirestore()
                startActivity(Intent(this, UsersActivity::class.java))
                finish()
            }
            .addOnFailureListener {
                Toast.makeText(this, "Login Failed: ${it.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun saveUserToFirestore() {
        val uid = FirebaseAuth.getInstance().uid ?: return

        val subscription = OneSignal.User.pushSubscription

        val oneSignalId = subscription.id

        if (oneSignalId.isNullOrEmpty()) {
            Toast.makeText(this, "OneSignal ID not ready yet", Toast.LENGTH_SHORT).show()
            return
        }

        val map = hashMapOf(
            "uid" to uid,
            "name" to etEmail.text.toString().substringBefore("@"),
            "oneSignalId" to oneSignalId
        )

        FirebaseFirestore.getInstance()
            .collection("users")
            .document(uid)
            .set(map, SetOptions.merge())
    }


}
