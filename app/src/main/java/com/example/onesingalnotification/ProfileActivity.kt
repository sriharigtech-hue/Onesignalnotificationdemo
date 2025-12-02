package com.example.onesingalnotification

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.widget.ImageView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import java.io.File
import java.io.FileOutputStream

class ProfileActivity : AppCompatActivity() {

    private lateinit var profileImage: ImageView
    private var imageUri: Uri? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_profile)

        profileImage = findViewById(R.id.profileImage)

        loadProfileImage()

        profileImage.setOnClickListener {
            showImagePickerOptions()
        }
    }

    private fun showImagePickerOptions() {
        val options = arrayOf("Gallery", "Camera")
        AlertDialog.Builder(this)
            .setTitle("Select Image")
            .setItems(options) { _, which ->
                when (which) {
                    0 -> galleryLauncher.launch("image/*")
                    1 -> openCamera()
                }
            }.show()
    }

    private val galleryLauncher =
        registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
            uri?.let {
                val bitmap = MediaStore.Images.Media.getBitmap(contentResolver, it)
                saveAndSetImage(bitmap)
            }
        }

    private val cameraLauncher =
        registerForActivityResult(ActivityResultContracts.TakePicture()) { success ->
            if (success) {
                imageUri?.let {
                    val bitmap = MediaStore.Images.Media.getBitmap(contentResolver, it)
                    saveAndSetImage(bitmap)
                }
            }
        }

    private fun openCamera() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
            != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.CAMERA), 101)
        } else {
            val file = File.createTempFile("camera_pic", ".jpg", cacheDir)

            imageUri = FileProvider.getUriForFile(
                this,
                "$packageName.fileprovider",
                file
            )

            imageUri?.let {
                cameraLauncher.launch(it)
            }
        }
    }


    private fun saveAndSetImage(bitmap: Bitmap) {
        saveProfileImage(bitmap)
        Toast.makeText(this, "Image Saved Locally", Toast.LENGTH_SHORT).show()
        profileImage.setImageBitmap(bitmap)
    }

    private fun saveProfileImage(bitmap: Bitmap) {
        val file = File(filesDir, "profile_pic.jpg")
        FileOutputStream(file).use {
            bitmap.compress(Bitmap.CompressFormat.JPEG, 90, it)
        }

        getSharedPreferences("PROFILE", MODE_PRIVATE)
            .edit()
            .putString("image_path", file.absolutePath)
            .apply()
    }

    private fun loadProfileImage() {
        val path = getSharedPreferences("PROFILE", MODE_PRIVATE)
            .getString("image_path", null)

        path?.let {
            val bitmap = BitmapFactory.decodeFile(it)
            profileImage.setImageBitmap(bitmap)
        }
    }
}
