package com.example.futherguideapp

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Button

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val logInButton: Button = findViewById(R.id.btn_logIn)
        val signUpButton: Button = findViewById(R.id.btn_signUp)

        logInButton.setOnClickListener {
            // Navigate to Log In screen
            val intent = Intent(this@MainActivity, LogIn::class.java)
            startActivity(intent)
        }

        signUpButton.setOnClickListener {
            // Navigate to Sign Up screen
            val intent = Intent(this@MainActivity, SignUp::class.java)
            startActivity(intent)
        }
    }
}