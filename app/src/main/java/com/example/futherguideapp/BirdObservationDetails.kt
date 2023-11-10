package com.example.futherguideapp

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.text.Html
import android.widget.Button
import android.widget.TextView

class BirdObservationDetails : AppCompatActivity() {

    private lateinit var back: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_bird_observation_details)

        // Initialize views
        val dateViewed: TextView = findViewById(R.id.txt_date)
        val txtBirdLocation: TextView = findViewById(R.id.txt_locationViewd)
        val numberOfTimesViewed: TextView = findViewById(R.id.txt_NumberOfTimesViewed)
        val name: TextView = findViewById(R.id.txt_birdName)

        val birdName = intent.getStringExtra("birdName")
        val location = intent.getStringExtra("location")
        val observationDate = intent.getStringExtra("observationDate")
        val quantity = intent.getIntExtra("quantity", 0)

        val locationText = "<b>Bird location:</b> ${location}."
        val howManyText = "<b>Number of times viewed:</b> ${quantity}."
        val date = "<b>Last time viewed:</b> ${observationDate}."

        numberOfTimesViewed.text = Html.fromHtml(howManyText)
        dateViewed.text = Html.fromHtml(date)
        txtBirdLocation.text = Html.fromHtml(locationText)
        name.text = birdName

        goToUserAccountPage()
    }


    private fun goToUserAccountPage(){
        back = findViewById(R.id.btn_back);
        back.setOnClickListener {
            // Navigate to User Account screen
            val intent = Intent(this, UserAccount::class.java)
            startActivity(intent)
        }
    }
}