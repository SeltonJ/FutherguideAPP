package com.example.futherguideapp

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import com.example.futherguideapp.models.Bird
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase

class AddNewBird : AppCompatActivity() {

    // Reference to the Firebase Realtime Database
    private val databaseReference = FirebaseDatabase.getInstance().getReference("birds")

    private lateinit var scientificNameInput: EditText
    private lateinit var commonNameInput: EditText
    private lateinit var locationInput: EditText
    private lateinit var howManyInput: EditText
    private lateinit var addBirdButton: Button
    private lateinit var backToBirdListButton: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_add_new_bird)

        initializeUIComponents()
        addNewBird()
        backToBirdList()
    }

    private fun addNewBird() {
        addBirdButton.setOnClickListener {
            if (!checkEditTextStatus()) {
                Toast.makeText(this, "Please fill in all fields.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (!isValidAddress(locationInput.text.toString())) {
                Toast.makeText(this, "Invalid address format. Please enter a correct address.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }


            val uid = FirebaseAuth.getInstance().currentUser?.uid
            if (uid == null) {
                Toast.makeText(this, "User must be logged in to add a bird", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val scientificName = scientificNameInput.text.toString().trim()
            val commonName = commonNameInput.text.toString().trim()
            val location = locationInput.text.toString().trim()
            val howManyText = howManyInput.text.toString().trim()

            try {
                val howMany = howManyText.toInt()

                // Generate a unique ID for the bird entry and add under the user's UID
                val birdId = databaseReference.child(uid).push().key ?: return@setOnClickListener
                val bird = Bird(scientificName, commonName, location, howMany)

                databaseReference.child(uid).child(birdId).setValue(bird)
                    .addOnCompleteListener { task ->
                        if (task.isSuccessful) {
                            Toast.makeText(this, "New bird added to the database.", Toast.LENGTH_SHORT).show()
                            // Clear input fields after successful addition
                            clearInputFields()
                            // Optionally, navigate back to the bird list or update UI
                            val intent = Intent(this, BirdList::class.java)
                            startActivity(intent)
                            finish() // Finish current activity if you don't want it on the back stack
                        } else {
                            Toast.makeText(this, "Failed to add bird to the database.", Toast.LENGTH_SHORT).show()
                        }
                    }
            } catch (e: NumberFormatException) {
                Toast.makeText(this, "Invalid 'How Many' value. Please enter a valid number.", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun isValidAddress(address: String): Boolean {
        // Regular Expression for basic address validation
        val addressRegex = "^[a-zA-Z0-9,\\s]+".toRegex()
        return address.isNotEmpty() && addressRegex.matches(address)
    }
    private fun clearInputFields() {
        scientificNameInput.setText("")
        commonNameInput.setText("")
        locationInput.setText("")
        howManyInput.setText("")
    }

    private fun backToBirdList() {
        backToBirdListButton = findViewById(R.id.btn_back)

        backToBirdListButton.setOnClickListener {
            // Navigate to Home page
            val intent = Intent(this, BirdList::class.java)
            startActivity(intent)
        }
    }

    private fun initializeUIComponents() {
        // Initialize your UI components here
        scientificNameInput = findViewById(R.id.scientific_nameEt)
        commonNameInput = findViewById(R.id.commonNameEt)
        locationInput = findViewById(R.id.birdLocationEt)
        howManyInput = findViewById(R.id.userNameEt)
        addBirdButton = findViewById(R.id.btn_newBird)
    }

    private fun checkEditTextStatus(): Boolean {
        // Getting values from EditTexts and storing them in String Variables.
        val scientificName = scientificNameInput.text.toString()
        val commonName = commonNameInput.text.toString()
        val location = locationInput.text.toString()
        val howMany = howManyInput.text.toString()

        // Checking if EditTexts are empty
        return scientificName.isNotEmpty() && commonName.isNotEmpty()
                && location.isNotEmpty() && howMany.isNotEmpty()
    }
}