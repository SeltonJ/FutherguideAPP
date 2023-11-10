package com.example.futherguideapp

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.ImageButton
import android.widget.Toast
import androidx.appcompat.widget.SearchView
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.futherguideapp.models.BirdObservation
import com.example.futherguideapp.adapters.MyBirdAdapter
import com.example.futherguideapp.models.Bird
import com.google.android.material.navigation.NavigationView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import org.json.JSONArray
import org.json.JSONException
import java.util.Locale
import kotlin.concurrent.thread

class BirdList : AppCompatActivity() {

    // Reference to the Firebase Realtime Database
    private lateinit var databaseReference: DatabaseReference

    //birds cards variables
    private val listOfBirds: MutableList<BirdObservation> = mutableListOf()
    private lateinit var newRecyclerView: RecyclerView
    private lateinit var adapter: MyBirdAdapter
    private val bird = "BirdDATA"

    //Nav variables
    private lateinit var drawerLayout: DrawerLayout
    private lateinit var actionBarDrawerToggle: ActionBarDrawerToggle
    private lateinit var toolbar: Toolbar

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_bird_list)

        // Initialize the DatabaseReference
        databaseReference = FirebaseDatabase.getInstance().reference.child("birds")

        navigationSystem()
        initializingBirdCardsAdapter()
        loadBirdsFromDatabase()
        searchBarCode()
        addNewBird()
    }

    override fun onResume() {
        super.onResume()
        loadBirdsFromDatabase()
    }

    private fun searchBarCode(){
        val searchView: SearchView = findViewById(R.id.search_view)

        searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                return false
            }

            override fun onQueryTextChange(newText: String?): Boolean {
                filterList(newText)
                return true
            }
        })
    }

    private fun filterList(query: String?) {
        if (query != null) {
            val filteredList = ArrayList<BirdObservation>()
            val lowerCaseQuery = query.lowercase(Locale.ROOT)

            for (bird in listOfBirds) {
                if (bird.comName.lowercase(Locale.ROOT).contains(lowerCaseQuery)
                ) {
                    filteredList.add(bird)
                }
            }

            if (filteredList.isEmpty()) {
                Toast.makeText(this, "No Data found", Toast.LENGTH_SHORT).show()
            } else {
                adapter.setFilteredList(filteredList)
            }
        }
    }

    @SuppressLint("NotifyDataSetChanged")
    private fun initializingBirdCardsAdapter(){

        //List of birds code
        newRecyclerView = findViewById(R.id.recycler_view)
        newRecyclerView.layoutManager = LinearLayoutManager(this)
        newRecyclerView.setHasFixedSize(true)

        adapter = MyBirdAdapter(listOfBirds)
        newRecyclerView.adapter = adapter

        adapter.onBirdCardClick = { clickedBird ->
            val intent = Intent(this, BirdDetails::class.java)
            intent.putExtra("bird_data", clickedBird)
            startActivity(intent)
        }

        thread {
            val birds = try {
                buildURLForBirds()?.readText()
            } catch (e: Exception) {
                Log.e(bird, "Error fetching birds", e)
                return@thread
            }
            runOnUiThread {
                consumeBirdObservations(birds)
                adapter.notifyDataSetChanged()
            }
        }
    }

    private fun addNewBird (){
        val addNewBird: ImageButton = findViewById(R.id.btn_addBird)

        addNewBird.setOnClickListener {
            val intent = Intent(this, AddNewBird::class.java)
            startActivity(intent)
        }
    }

    private fun navigationSystem(){

        // Initialize DrawerLayout
        drawerLayout = findViewById(R.id.drawer_layout)
        // Initialize Toolbar
        toolbar = findViewById(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.title = ""

        // Initialize Toolbar
        toolbar = findViewById(R.id.toolbar)
        setSupportActionBar(toolbar)

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
                R.id.nav_settings -> {
                    // Navigate to Settings page
                    val intent = Intent(this, Settings::class.java)
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

    private fun loadBirdsFromDatabase() {
        val uid = FirebaseAuth.getInstance().currentUser?.uid
        if (uid != null) {
            databaseReference.child("birds").child(uid)
                .addListenerForSingleValueEvent(object : ValueEventListener {
                    override fun onDataChange(dataSnapshot: DataSnapshot) {
                        if (!dataSnapshot.exists()) {
                            Log.d("FirebaseData", "No bird data found under 'birds/$uid'")
                            return
                        }

                        listOfBirds.clear()
                        dataSnapshot.children.forEach { childSnapshot ->
                            val bird = childSnapshot.getValue(Bird::class.java)
                            bird?.let {
                                val birdObservation = BirdObservation(
                                    comName = it.comName ?: "Unknown",
                                    sciName = it.sciName ?: "Unknown",
                                    locName = it.locName ?: "Unknown",
                                    howMany = it.howMany ?: 0,
                                    lat = 0.0, // Assuming you don't have lat/lng in Bird class
                                    lng = 0.0
                                )
                                listOfBirds.add(birdObservation)
                            }
                        }
                        adapter.notifyDataSetChanged()
                        Log.d("FirebaseData", "Total birds loaded: ${listOfBirds.size}")
                    }

                    override fun onCancelled(databaseError: DatabaseError) {
                        Log.e("FirebaseData", "loadPost:onCancelled", databaseError.toException())
                    }
                })
        } else {
            Toast.makeText(this, "User must be logged in to view birds", Toast.LENGTH_SHORT).show()
        }
    }

    private fun consumeBirdObservations(birdObservationJSON: String?) {
        if (birdObservationJSON != null) {
            try {
                val birdsObservations = JSONArray(birdObservationJSON)
                for (i in 0 until birdsObservations.length()) {
                    val birdsObservation = birdsObservations.getJSONObject(i)

                    // get name
                    val name = birdsObservation.optString("sciName", "Unknown")

                    // get amount
                    val number = birdsObservation.optInt("howMany", 0)

                    // get comName
                    val comName = birdsObservation.optString("comName", "Unknown")

                    // get Location name
                    val locName = birdsObservation.optString("locName", "Unknown")

                    // get Location name
                    val lat = birdsObservation.getDouble("lat")

                    // get Location name
                    val lng = birdsObservation.getDouble("lng")

                    // Create a BirdObservation object with the retrieved values
                    val birdsObject = BirdObservation(comName, name, locName, number, lat, lng)

                    listOfBirds.add(birdsObject)
                }
            } catch (e: JSONException) {
                Log.e(bird, "JSON parsing error", e)
            }
        }
    }
}