package com.example.digitallearningplatform

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.animation.AlphaAnimation
import android.widget.ImageView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.firebase.FirebaseApp

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)

        val logo: ImageView = findViewById(R.id.logo)

        // Fade-in animation for the logo
        val fadeIn = AlphaAnimation(0f, 1f)
        fadeIn.duration = 1500
        logo.startAnimation(fadeIn)
        logo.alpha = 1f

        // Wait for 2.5 seconds and start MainActivity
        Handler(Looper.getMainLooper()).postDelayed({
            startActivity(Intent(this, SignupActivity::class.java))
            finish()
        }, 2500)
    }
}