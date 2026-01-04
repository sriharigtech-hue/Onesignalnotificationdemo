package com.example.onesingalnotification.call

import android.Manifest
import android.os.Bundle
import com.google.android.material.floatingactionbutton.FloatingActionButton
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.example.onesingalnotification.R
import com.google.firebase.firestore.FirebaseFirestore
import io.agora.rtc2.IRtcEngineEventHandler
import io.agora.rtc2.RtcEngine
import io.agora.rtc2.RtcEngineConfig

class CallActivity : AppCompatActivity() {

    private lateinit var rtcEngine: RtcEngine
    private lateinit var firestore: FirebaseFirestore

    private lateinit var tvUserName: TextView
    private lateinit var btnAccept: FloatingActionButton
    private lateinit var btnReject: FloatingActionButton

    private var callId: String = ""
    private var channelName: String = ""

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (!isGranted) {
            Toast.makeText(this, "Microphone permission denied", Toast.LENGTH_SHORT).show()
            finish()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_call)

        tvUserName = findViewById(R.id.tvUserName)
        btnAccept = findViewById(R.id.btnAccept)
        btnReject = findViewById(R.id.btnReject)

        // Get callId and channelName from Intent
        callId = intent.getStringExtra("callId") ?: ""
        channelName = intent.getStringExtra("channel") ?: "default_channel"
        tvUserName.text = intent.getStringExtra("callerName") ?: "Calling..."

        firestore = FirebaseFirestore.getInstance()

        // Request microphone permission
        requestPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)

        // Initialize Agora Engine
        initAgoraEngine()

        btnAccept.setOnClickListener {
            acceptCall()
        }

        btnReject.setOnClickListener {
            rejectCall()
        }

        // Listen for call ended
        firestore.collection("calls").document(callId)
            .addSnapshotListener { snapshot, _ ->
                if (snapshot != null && snapshot.exists()) {
                    val status = snapshot.getString("status")
                    if (status == "ended") {
                        endCall()
                    }
                }
            }
    }

    private fun initAgoraEngine() {
        val config = RtcEngineConfig().apply {
            mContext = this@CallActivity
            mAppId = "YOUR_AGORA_APP_ID" // Paste your Agora App ID
            mEventHandler = object : IRtcEngineEventHandler() {
                override fun onJoinChannelSuccess(channel: String?, uid: Int, elapsed: Int) {}
                override fun onUserJoined(uid: Int, elapsed: Int) {}
                override fun onUserOffline(uid: Int, reason: Int) {}
            }
        }
        rtcEngine = RtcEngine.create(config)
    }

    private fun acceptCall() {
        // Update call status in Firestore
        firestore.collection("calls").document(callId)
            .update("status", "accepted")
        // Join Agora channel
        rtcEngine.joinChannel(null, channelName, "", 0)
        Toast.makeText(this, "Call Started", Toast.LENGTH_SHORT).show()
    }

    private fun rejectCall() {
        firestore.collection("calls").document(callId)
            .update("status", "ended")
        endCall()
    }

    private fun endCall() {
        rtcEngine.leaveChannel()
        RtcEngine.destroy()
        finish()
    }

    override fun onDestroy() {
        super.onDestroy()
        endCall()
    }
}
