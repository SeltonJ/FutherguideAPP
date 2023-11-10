package com.example.futherguideapp

import android.annotation.SuppressLint
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import com.example.futherguideapp.models.User
import com.google.firebase.auth.FirebaseAuth
import com.google.android.material.textfield.TextInputLayout
import com.google.firebase.database.FirebaseDatabase

class SignUp : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth

    private lateinit var userNameInput: EditText
    private lateinit var userSurnameInput: EditText
    private lateinit var userEmailInput: EditText
    private lateinit var userPasswordInput: EditText
    private lateinit var userConfirmPasswordInput: EditText

    private lateinit var nameInputLayout: TextInputLayout
    private lateinit var surnameInputLayout: TextInputLayout
    private lateinit var emailInputLayout: TextInputLayout
    private lateinit var passwordLayout: TextInputLayout
    private lateinit var confirmPasswordLayout: TextInputLayout

    private lateinit var signUpButton: Button
    private lateinit var signInText: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_sign_up)

        goToLoginPage()
        initializeUIComponents()
        registerUser()
        checkEditTextStatus()
        auth = FirebaseAuth.getInstance()
    }

    private fun registerUser() {
        signUpButton.setOnClickListener {
            val email = userEmailInput.text.toString().trim()
            val password = userPasswordInput.text.toString().trim()
            val username = userNameInput.text.toString().trim()
            val surname = userSurnameInput.text.toString().trim()

            // Basic validation for email and password
            if (email.isEmpty() || password.isEmpty() || username.isEmpty() || surname.isEmpty()) {
                Toast.makeText(this, "Please fill in all the fields.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (password != userConfirmPasswordInput.text.toString().trim()) {
                Toast.makeText(this, "Passwords do not match.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (!isValidPassword(password)) {
                Toast.makeText(this, "Password does not meet the criteria.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // Create a new user with Firebase Authentication
            auth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(this) { task ->
                    if (task.isSuccessful) {
                        // Sign in success, update UI with the signed-in user's information
                        val firebaseUser = auth.currentUser
                        Toast.makeText(this, "User registered successfully!", Toast.LENGTH_SHORT).show()

                        // Save additional user information in the Firebase Realtime Database
                        val user = User(
                            userName = username,
                            userSurname = surname,
                            userEmail = email,
                            userPassword= password,
                        )

                        val databaseReference = FirebaseDatabase.getInstance().getReference("users")
                        firebaseUser?.let {
                            databaseReference.child(it.uid).setValue(user)
                                .addOnSuccessListener {
                                    // Data saved successfully!
                                    navigateToLoginScreen()
                                }
                                .addOnFailureListener {
                                    // Failed to save data
                                    Log.e("FirebaseDatabase", "Failed to save user data", it)
                                    Toast.makeText(baseContext, "Failed to save user data: ${it.message}", Toast.LENGTH_SHORT).show()                                }
                        }
                    } else {
                        // If sign in fails, display a message to the user.
                        Toast.makeText(this, "Registration failed: ${task.exception?.message}", Toast.LENGTH_SHORT).show()
                    }
                }
        }
    }

    private fun navigateToLoginScreen() {
        val intent = Intent(this, LogIn::class.java)
        startActivity(intent)
    }

    @SuppressLint("SuspiciousIndentation")
    private fun goToLoginPage(){
        signInText = findViewById(R.id.txt_signIn);
            signInText.setOnClickListener {
            // Navigate to Log In screen
            val intent = Intent(this, LogIn::class.java)
            startActivity(intent)
        }
    }

    fun isValidPassword(password: String): Boolean {
        val regex = "^(?=.*[0-9])(?=.*[a-z])(?=.*[A-Z])(?=.*[@#$%^&+=])(?=\\S+$).{5,}$".toRegex()
        return regex.matches(password)
    }

    private fun initializeUIComponents() {

        // Initialize your UI components here
        userNameInput = findViewById(R.id.userNameEt)
        userSurnameInput = findViewById(R.id.userSurnameEt)
        userEmailInput = findViewById(R.id.emailEt)
        userPasswordInput = findViewById(R.id.passwordEt)
        userConfirmPasswordInput = findViewById(R.id.confirmUserPasswordEt)
        signUpButton = findViewById(R.id.btn_signUp)
        passwordLayout = findViewById(R.id.edit_userPassword)
        confirmPasswordLayout = findViewById(R.id.edit_userPassword)
        nameInputLayout = findViewById(R.id.edit_userName)
        surnameInputLayout = findViewById(R.id.edit_userSurname)
        emailInputLayout = findViewById(R.id.edit_userEmail)
        signInText = findViewById(R.id.txt_signIn)
    }

    private fun checkEditTextStatus(): Boolean {

        // Initialize your UI components here
        initializeUIComponents()

        // Getting values from EditTexts and storing them in String Variables.
        val username = userNameInput.text.toString()
        val surname = userSurnameInput.text.toString()
        val email = userEmailInput.text.toString()
        val password = userPasswordInput.text.toString()
        val confirmPassword = userConfirmPasswordInput.text.toString()

        // Checking if EditTexts are empty or not using Kotlin's isNullOrEmpty() function.
        return username.isNotEmpty() && surname.isNotEmpty() && email.isNotEmpty() && password.isNotEmpty() && confirmPassword.isNotEmpty()
    }
}
