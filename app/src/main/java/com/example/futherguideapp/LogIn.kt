package com.example.futherguideapp

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.*

class LogIn : AppCompatActivity() {

    private val job = Job()
    private lateinit var auth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_log_in)

        // Initialize Firebase Auth
        auth = FirebaseAuth.getInstance()

        userLogIn()
    }

    private fun userLogIn() {

        val userEmailInput: EditText = findViewById(R.id.emailEt)
        val userPasswordInput: EditText = findViewById(R.id.passwordEt)
        val logInButton: Button = findViewById(R.id.btn_logIn)
        val signUpText: TextView = findViewById(R.id.txt_SigUp)

        signUpText.setOnClickListener {
            val intent = Intent(this, SignUp::class.java)
            startActivity(intent)
        }

        logInButton.setOnClickListener {
            val email = userEmailInput.text.toString().trim()
            val password = userPasswordInput.text.toString().trim()

            if (email.isEmpty() || password.isEmpty()) {
                Toast.makeText(this, "Please enter both email and password.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // Perform Firebase authentication
            auth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(this) { task ->
                    if (task.isSuccessful) {
                        // Login success
                        Toast.makeText(this, "Login successful!", Toast.LENGTH_SHORT).show()
                        val intent = Intent(this, HomePage::class.java)
                        startActivity(intent)
                        finish()
                    } else {
                        // If login fails, display a message to the user.
                        Toast.makeText(this, "Login failed: ${task.exception?.message}", Toast.LENGTH_SHORT).show()
                    }
                }
        }
    }
    override fun onDestroy() {
        super.onDestroy()
        job.cancel()
    }
}
