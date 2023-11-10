package com.example.futherguideapp

import android.annotation.SuppressLint
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.text.Html
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import com.example.futherguideapp.adapters.BirdObservationAdapter
import com.example.futherguideapp.models.BirdObservation
import com.example.futherguideapp.models.UserBirdObservation
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.concurrent.thread

class BirdDetails : AppCompatActivity() {

    private lateinit var observationAdapter: BirdObservationAdapter
    private val observations: MutableList<UserBirdObservation> = mutableListOf()

    // Firebase database reference
    private val databaseReference = FirebaseDatabase.getInstance().getReference("observations")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_bird_details)

        observationAdapter = BirdObservationAdapter(observations)

        // Initialize views
        val txtBirdsSciName: TextView = findViewById(R.id.txt_birdsSciName)
        val txtBirdLocation: TextView = findViewById(R.id.txt_birdLocation)
        val txtBirdComName: TextView = findViewById(R.id.txt_birdComName)
        val txtBirdHowMany: TextView = findViewById(R.id.txt_birdHowMany)
        val txtStreetName: TextView = findViewById(R.id.txt_StreetName)

        // Retrieve the selected bird information from the intent
        val selectedBird = intent.getParcelableExtra<BirdObservation>("selectedBird")

        val btnAddObservation: Button = findViewById(R.id.btn_birdObservations)

        // Handle adding bird observation
        btnAddObservation.setOnClickListener {
            selectedBird?.let { bird ->
                addBirdObservation(bird)
                Toast.makeText(this, "Observation added successfully!", Toast.LENGTH_SHORT).show()

            }
        }

        if (selectedBird != null) {
            val scientificName = selectedBird.sciName
            val comName = selectedBird.comName
            val location = selectedBird.locName
            val birdCount = selectedBird.howMany
            val birdLat = selectedBird.lat
            val birdLng = selectedBird.lng

            val locationText = "<b>Bird location:</b> ${location}."
            val howManyText = "<b>Number of birds:</b> ${birdCount}."
            val sciName = "<b>Specie name:</b> ${scientificName}."

            txtBirdComName.text = comName
            txtBirdsSciName.text = Html.fromHtml(sciName)
            txtBirdLocation.text = Html.fromHtml(locationText)
            txtBirdHowMany.text = Html.fromHtml(howManyText)

            thread {
                val streetName = getPlaceNameFromCoordinates(birdLat, birdLng)

                runOnUiThread{
                    val streetText = "<b>Street name:</b> ${streetName}."
                    txtStreetName.text = Html.fromHtml(streetText)
                }
            }

            val lisOfBirds: Button = findViewById(R.id.btn_back)

            lisOfBirds.setOnClickListener {
                val intent = Intent(this, BirdList::class.java)
                startActivity(intent)
            }
        }
    }

    private fun addBirdObservation(selectedBird: BirdObservation) {
        val currentUser = FirebaseAuth.getInstance().currentUser
        currentUser?.let { user ->
            // Get the current date/time
            val currentDate = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date())

            // Create a reference to the user's observations
            val userObservationsRef = databaseReference.child("userObservations").child(user.uid)

            // Check if an observation for this bird already exists
            userObservationsRef.orderByChild("birdName").equalTo(selectedBird.comName)
                .addListenerForSingleValueEvent(object : ValueEventListener {
                    override fun onDataChange(dataSnapshot: DataSnapshot) {
                        if (dataSnapshot.exists()) {
                            // Observation exists, increment quantity
                            for (snapshot in dataSnapshot.children) {
                                val currentQuantity = snapshot.child("quantity").getValue(Int::class.java) ?: 0
                                snapshot.ref.child("quantity").setValue(currentQuantity + 1)
                            }
                            Toast.makeText(this@BirdDetails, "Observation quantity incremented.", Toast.LENGTH_SHORT).show()
                        } else {
                            // Observation does not exist, create a new entry
                            val newObservationId = userObservationsRef.push().key
                            newObservationId?.let { observationId ->
                                val observationData = mapOf(
                                    "birdName" to selectedBird.comName,
                                    "quantity" to 1,
                                    "observationDate" to currentDate,
                                    "location" to selectedBird.locName
                                )
                                userObservationsRef.child(observationId).setValue(observationData)
                                    .addOnSuccessListener {
                                        Toast.makeText(this@BirdDetails, "Observation added successfully!", Toast.LENGTH_SHORT).show()
                                    }
                                    .addOnFailureListener {
                                        Toast.makeText(this@BirdDetails, "Failed to add observation.", Toast.LENGTH_SHORT).show()
                                    }
                            }
                        }
                    }

                    override fun onCancelled(databaseError: DatabaseError) {
                        Toast.makeText(this@BirdDetails, "Error: ${databaseError.message}", Toast.LENGTH_SHORT).show()
                    }
                })
        } ?: run {
            Toast.makeText(this, "User not logged in", Toast.LENGTH_SHORT).show()
        }
    }
}