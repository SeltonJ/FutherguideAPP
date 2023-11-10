package com.example.futherguideapp

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.ImageButton
import android.widget.RadioGroup
import android.widget.SeekBar
import android.widget.TextView
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import com.google.android.material.navigation.NavigationView
import com.google.firebase.auth.FirebaseAuth

class Settings : AppCompatActivity() {

    //Nav variables
    private lateinit var drawerLayout: DrawerLayout
    private lateinit var actionBarDrawerToggle: ActionBarDrawerToggle
    private lateinit var toolbar: Toolbar

    private lateinit var seekBar: SeekBar
    private lateinit var value: TextView
    private lateinit var unitTextView: TextView
    private var isKilometers = true
    private lateinit var radioGroup: RadioGroup
    private var lastSeekBarProgress = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)

        //Navigation system
        navigationSystem()
        seekBar()
        radioButton()
        loadSavedValues()
    }

    private fun loadSavedValues() {
        val sharedPreferences = getSharedPreferences("MyPrefs", Context.MODE_PRIVATE)
        isKilometers = sharedPreferences.getBoolean("isKilometers", true)
        lastSeekBarProgress = sharedPreferences.getInt("maxTravelDistance", 0)

        // Set the SeekBar progress and update the value TextView
        seekBar.progress = lastSeekBarProgress
        updateValueText(lastSeekBarProgress)

        // Set the selected radio button based on the stored preference
        if (isKilometers) {
            radioGroup.check(R.id.metrics)
        } else {
            radioGroup.check(R.id.offer)
        }
    }

    private fun seekBar(){
        // Initialize the seekBar and value TextView
        initializeUIComponents()
        loadSavedValues()

        // Set an OnSeekBarChangeListener for the seekBar
        seekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar, progress: Int, fromUser: Boolean) {
                updateValueText(progress)
                saveMaxTravelDistance(progress)
            }

            override fun onStartTrackingTouch(seekBar: SeekBar) {
            }

            override fun onStopTrackingTouch(seekBar: SeekBar) {
            }
        })
    }

    private fun setMetric() {
        if (isKilometers) {
            seekBar.max = 50
        } else {
            seekBar.max = 50000
        }
        updateValueText(seekBar.progress)
    }

    private fun initializeUIComponents() {
        seekBar = findViewById(R.id.seekbar)
        value = findViewById(R.id.progressKM)
        unitTextView = value
        radioGroup = findViewById(R.id.toggle)
    }

    private fun radioButton() {
        initializeUIComponents()

        // Add an OnCheckedChangeListener to the RadioGroup
        radioGroup.setOnCheckedChangeListener { _, checkedId ->
            when (checkedId) {
                R.id.metrics -> {
                    isKilometers = true
                }
                R.id.offer -> {
                    isKilometers = false
                }
            }
            setMetric()
            saveUnitPreference(isKilometers)
        }
    }
    @SuppressLint("SetTextI18n")
    private fun updateValueText(progress: Int) {
        val displayValue: Int
        val unit: String
        if (isKilometers) {
            displayValue = progress
            unit = "km"
        } else {
            displayValue = progress * 10
            unit = "m"
        }
        value.text = "$displayValue $unit"
    }

    private fun saveUnitPreference(isKilometers: Boolean) {
        val sharedPreferences = getSharedPreferences("MyPrefs", Context.MODE_PRIVATE)
        val editor = sharedPreferences.edit()
        editor.putBoolean("isKilometers", isKilometers)
        editor.apply()
    }

    fun saveMaxTravelDistance(maxTravelDistance: Int) {
        val sharedPreferences = getSharedPreferences("MyPrefs", Context.MODE_PRIVATE)
        val editor = sharedPreferences.edit()
        editor.putInt("maxTravelDistance", maxTravelDistance)
        editor.apply()
    }

    private fun navigationSystem(){

        // Initialize DrawerLayout
        drawerLayout = findViewById(R.id.drawer_layout)
        // Initialize Toolbar
        toolbar = findViewById(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.title = ""

        // Initialize ActionBarDrawerToggle, which will control the toggle
        actionBarDrawerToggle = ActionBarDrawerToggle(this, drawerLayout, R.string.nav_open, R.string.nav_close)

        // Setup DrawerLayout Listener
        drawerLayout.addDrawerListener(actionBarDrawerToggle)
        actionBarDrawerToggle.syncState()
        val toolbar: Toolbar = findViewById(R.id.toolbar)
        val menuButton: ImageButton = toolbar.findViewById(R.id.menu_button)

        menuButton.setOnClickListener {
            //Handle menu button click
            drawerLayout.openDrawer(GravityCompat.START)
        }

        val navigationView: NavigationView = findViewById(R.id.nav_view)

        navigationView.setNavigationItemSelectedListener { menuItem ->
            // Close the navigation drawer
            drawerLayout.closeDrawers()

            // Handle menu item selected
            when (menuItem.itemId) {
                R.id.nav_Map -> {
                    // Navigate to Home page
                    val intent = Intent(this, HomePage::class.java)
                    startActivity(intent)
                }
                R.id.nav_account -> {
                    // Navigate to My Account page
                    val intent = Intent(this, UserAccount::class.java)
                    startActivity(intent)
                }
                R.id.nav_birdList -> {
                    // Navigate to Settings page
                    val intent = Intent(this, BirdList::class.java)
                    startActivity(intent)
                }
                R.id.nav_logout -> {
                    // Handle Logout

                    // Sign out from Firebase
                    FirebaseAuth.getInstance().signOut()

                    val intent = Intent(this, MainActivity::class.java)
                    startActivity(intent)
                }
            }
            true
        }
    }
}