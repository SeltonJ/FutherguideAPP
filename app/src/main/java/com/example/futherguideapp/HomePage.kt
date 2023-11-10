package com.example.futherguideapp

import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import android.view.MenuItem
import android.widget.ImageButton
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.appcompat.app.AppCompatActivity
import androidx.drawerlayout.widget.DrawerLayout
import androidx.fragment.app.FragmentActivity
import com.example.futherguideapp.models.BirdObservation
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import org.json.JSONArray
import org.json.JSONException
import kotlin.concurrent.thread
import androidx.appcompat.widget.Toolbar
import androidx.core.app.ActivityCompat
import androidx.core.view.GravityCompat
import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Color
import android.view.View
import android.widget.TextView
import android.widget.Toast
import com.google.android.gms.common.ConnectionResult
import com.google.android.gms.common.GoogleApiAvailability
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.Marker
import com.google.android.gms.maps.model.Polyline
import com.google.android.gms.maps.model.PolylineOptions
import com.google.android.material.navigation.NavigationView
import com.google.firebase.auth.FirebaseAuth
import org.json.JSONObject
import java.net.URL
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

class HomePage : FragmentActivity(), OnMapReadyCallback,
    NavigationView.OnNavigationItemSelectedListener {

    private var listOfBirds: MutableList<BirdObservation> = mutableListOf()
    private val tag = "BirdDATA"
    private lateinit var drawerLayout: DrawerLayout
    private lateinit var actionBarDrawerToggle: ActionBarDrawerToggle
    private lateinit var mMap: GoogleMap
    private var isMapReady = false
    private var isLocationPermissionGranted = false
    private var currentRoute: Polyline? = null

    companion object {
        private const val REQUEST_LOCATION_PERMISSION = 1
    }

    interface LocationCallback {
        fun onLocationAvailable(latLng: LatLng)
    }

    private val fusedLocationProviderClient by lazy {
        LocationServices.getFusedLocationProviderClient(this)
    }

    private var start: LatLng? = null
    private var end: LatLng? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_home_page)

        // Navigation system
        navigationSystem()

        val mapFragment = supportFragmentManager
            .findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)

        if (!checkLocationPermission()) {
            requestLocationPermission()
        }

        // Fetch bird observations
        thread {
            val birds = try {
                buildURLForBirds()?.readText()
            } catch (e: Exception) {
                Log.e(tag, "Error fetching birds", e)
                return@thread
            }
            runOnUiThread {
                consumeBirdObservations(birds)
                addMarkersToMap()
            }
        }
    }

    private fun findRoutes(start: LatLng, end: LatLng, marker: Marker) {

        Log.d("ROUTE_INFO", "Finding route from $start to $end")
        val apiKey =  BuildConfig.GOOGLE_MAPS_API_KEY
        val url = "https://maps.googleapis.com/maps/api/directions/json?" +
                "origin=${start.latitude},${start.longitude}&" +
                "destination=${end.latitude},${end.longitude}&" +
                "key=$apiKey"

        thread {
            try {
                val result = URL(url).readText()
                val jsonObject = JSONObject(result)

                if (jsonObject.has("routes")) {
                    val routes = jsonObject.getJSONArray("routes")
                    if (routes.length() > 0) {
                        val route = routes.getJSONObject(0)
                        if (route.has("overview_polyline")) {
                            val polyline = route.getJSONObject("overview_polyline").getString("points")
                            val points = decodePolyline(polyline)

                            if (route.has("legs")) {
                                val legs = route.getJSONArray("legs")
                                if (legs.length() > 0) {
                                    val leg = legs.getJSONObject(0)

                                    val distance = leg.getJSONObject("distance").getString("text")
                                    val duration = leg.getJSONObject("duration").getString("text")

                                    val currentTime = Calendar.getInstance()
                                    currentTime.add(Calendar.SECOND, leg.getJSONObject("duration").getInt("value"))
                                    val arrivalTime = SimpleDateFormat("hh:mm a", Locale.getDefault()).format(currentTime.time)

                                    runOnUiThread {
                                        marker.snippet = "Distance: $distance\nDuration: $duration\nArrival Time: $arrivalTime"
                                        marker.showInfoWindow()
                                    }
                                }
                            }

                            runOnUiThread {
                                currentRoute?.remove() // remove existing route
                                currentRoute = mMap.addPolyline(PolylineOptions().addAll(points).color(Color.BLUE).width(8f))
                            }
                        }
                    } else {
                        runOnUiThread {
                            Toast.makeText(this@HomePage, "No routes found between these two points.", Toast.LENGTH_SHORT).show()
                        }
                    }
                } else if (jsonObject.has("error_message")) {
                    val errorMsg = jsonObject.getString("error_message")
                    Log.e("API_ERROR_MESSAGE", errorMsg)
                    runOnUiThread {
                        Toast.makeText(this@HomePage, "Error fetching route: $errorMsg", Toast.LENGTH_SHORT).show()
                    }
                }
            } catch (e: Exception) {
                Log.e("DIRECTIONS_API_ERROR", e.toString())
                runOnUiThread {
                    Toast.makeText(this@HomePage, "Error fetching route", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    // This method decodes a polyline string from Google Maps Directions API into a list of LatLng
    private fun decodePolyline(encoded: String): List<LatLng> {
        val poly = mutableListOf<LatLng>()
        var index = 0
        val len = encoded.length
        var lat = 0
        var lng = 0

        while (index < len) {
            var b: Int
            var shift = 0
            var result = 0
            do {
                b = encoded[index++].code - 63
                result = result or (b and 0x1f shl shift)
                shift += 5
            } while (b >= 0x20)
            val dLat = if (result and 1 != 0) (result shr 1).inv() else result shr 1
            lat += dLat

            shift = 0
            result = 0
            do {
                b = encoded[index++].code - 63
                result = result or (b and 0x1f shl shift)
                shift += 5
            } while (b >= 0x20)
            val dLng = if (result and 1 != 0) (result shr 1).inv() else result shr 1
            lng += dLng

            val latLng = LatLng(lat.toDouble() / 1E5, lng.toDouble() / 1E5)
            poly.add(latLng)
        }

        return poly
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        when (requestCode) {
            REQUEST_LOCATION_PERMISSION -> {
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    // Permission was granted
                    initializeLocationTracking()
                } else {
                    // Permission was denied
                    Toast.makeText(this, "Permission denied. Can't access location.", Toast.LENGTH_SHORT).show()
                }
                return
            }
        }
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
    }

    private fun initializeLocationTracking() {

        // Check if the device has Google Play Services available
        if (GoogleApiAvailability.getInstance().isGooglePlayServicesAvailable(this) == ConnectionResult.SUCCESS) {
            // Check if the map is ready
            if (isMapReady) {
                // Request location updates
                if (ActivityCompat.checkSelfPermission(
                        this,
                        Manifest.permission.ACCESS_FINE_LOCATION
                    ) == PackageManager.PERMISSION_GRANTED
                ) {
                    mMap.isMyLocationEnabled = true
                    mMap.uiSettings.isMyLocationButtonEnabled = true
                }
            }
        }
    }

    private fun addMarkersToMap() {
        // Check if the map is ready before adding markers
        if (isMapReady) {
            // Clear existing markers from the map
            mMap.clear()

            // Define the reference point (e.g., center of the circle)
            val referencePoint = LatLng(-33.940405, 18.466618)
            val maxTravelDistance = getMaximumTravelDistance()

            // Filter bird observations based on their distance from the reference point
            val filteredBirds = listOfBirds.filter { birdObservation ->
                val birdLocation = LatLng(birdObservation.lat, birdObservation.lng)
                val distance = calculateDistance(referencePoint, birdLocation)
                distance <= maxTravelDistance
            }

            // Add markers for filtered bird observations
            for (observation in filteredBirds) {
                Log.d(tag, "Adding marker for bird: ${observation.comName}")
                mMap.addMarker(
                    MarkerOptions()
                        .position(LatLng(observation.lat, observation.lng))
                        .title(observation.comName)
                        .snippet("Number of birds: ${observation.howMany}, Location: ${observation.locName}")
                )
            }

            // Move the camera to the reference point (optional)
            mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(referencePoint, 10f))
        }
    }

    // Implement this method to get the maximum travel distance from SharedPreferences
    private fun getMaximumTravelDistance(): Double {
        // Get an instance of SharedPreferences
        val sharedPreferences = getSharedPreferences("MyPrefs", Context.MODE_PRIVATE)

        // Retrieve the map parameters from SharedPreferences
        val isKilometers = sharedPreferences.getBoolean("isKilometers", true)

        // Use getFloat if maxTravelDistance is stored as a float, otherwise use getInt
        val userMaxTravelDistance = sharedPreferences.getInt("maxTravelDistance", 5000)

        // Convert the userMaxTravelDistance based on unit preference
        val number = if (isKilometers) {
            userMaxTravelDistance * 1000
        } else {
            userMaxTravelDistance
        }
        // Retrieve the max travel distance (you may store it as a double)
        return number.toDouble()
    }

    // Calculate the distance between two LatLng points using Haversine formula
    private fun calculateDistance(point1: LatLng, point2: LatLng): Double {
        val radius = 6371 // Radius of the Earth in kilometers
        val lat1 = Math.toRadians(point1.latitude)
        val lat2 = Math.toRadians(point2.latitude)
        val deltaLat = Math.toRadians(point2.latitude - point1.latitude)
        val deltaLng = Math.toRadians(point2.longitude - point1.longitude)

        val a = sin(deltaLat / 2) * sin(deltaLat / 2) +
                cos(lat1) * cos(lat2) *
                sin(deltaLng / 2) * sin(deltaLng / 2)

        val c = 2 * atan2(sqrt(a), sqrt(1 - a))

        return (radius * c) * 1000 // Distance in meters
    }

    @SuppressLint("PotentialBehaviorOverride")
    override fun onMapReady(googleMap: GoogleMap) {
        mMap = googleMap
        mMap.uiSettings.isMyLocationButtonEnabled = true
        mMap.uiSettings.isZoomControlsEnabled = true
        mMap.uiSettings.isCompassEnabled = true
        isMapReady = true
        addMarkersToMap()
        initializeLocationTracking()
        getMyLocation()
        isLocationPermissionGranted = checkLocationPermission()

        // Whenever a marker is clicked, reset its snippet
        mMap.setOnMarkerClickListener { marker ->
            marker.snippet = ""
            if (marker.isInfoWindowShown) {
                marker.hideInfoWindow()
                marker.showInfoWindow()
            }
            false
        }

        // Set the custom info window adapter
        mMap.setInfoWindowAdapter(object : GoogleMap.InfoWindowAdapter {
            @SuppressLint("InflateParams")
            override fun getInfoContents(marker: Marker): View? {
                val view = layoutInflater.inflate(R.layout.custom_info_window, null)

                val birdName = view.findViewById<TextView>(R.id.tv_bird_name)
                birdName.text = marker.title ?: "Unknown"

                val distance = view.findViewById<TextView>(R.id.tv_distance)
                val duration = view.findViewById<TextView>(R.id.tv_duration)
                val arrivalTime = view.findViewById<TextView>(R.id.tv_arrival_time)

                // Default visibility to GONE
                distance.visibility = View.GONE
                duration.visibility = View.GONE
                arrivalTime.visibility = View.GONE

                marker.snippet?.let { snippet ->
                    if (snippet.isNotEmpty()) {
                        val details = snippet.split("\n")
                        if (details.size == 3) {
                            distance.text = details[0]
                            duration.text = details[1]
                            arrivalTime.text = details[2]

                            // Setting visibility to VISIBLE for TextViews that have the route details
                            distance.visibility = View.VISIBLE
                            duration.visibility = View.VISIBLE
                            arrivalTime.visibility = View.VISIBLE
                        }
                    }
                }

                return view
            }

            override fun getInfoWindow(marker: Marker): View? {
                // Return null to ensure getInfoContents is called
                return null
            }
        })

        // Handle InfoWindow clicks
        mMap.setOnInfoWindowClickListener { marker ->
            end = marker.position

            getMyLocation(object : LocationCallback {
                override fun onLocationAvailable(latLng: LatLng) {
                    if (end != null && start != null) {
                        findRoutes(start!!, end!!, marker)
                    } else {
                        Toast.makeText(this@HomePage, "Unable to fetch current location.", Toast.LENGTH_SHORT).show()
                    }
                }
            })
        }
    }

    private fun requestLocationPermission() {
        ActivityCompat.requestPermissions(
            this,
            arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
            REQUEST_LOCATION_PERMISSION
        )
    }

    private fun checkLocationPermission(): Boolean {
        return ActivityCompat.checkSelfPermission(
            this,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
    }

    private fun getMyLocation(callback: LocationCallback? = null) {
        try {
            fusedLocationProviderClient.lastLocation.addOnSuccessListener { location ->
                if (location != null) {
                    start = LatLng(location.latitude, location.longitude)
                    callback?.onLocationAvailable(start!!)
                } else {
                    Toast.makeText(this, "Unable to get current location", Toast.LENGTH_SHORT).show()
                }
            }
        } catch (securityException: SecurityException) {
            Log.e("LOCATION_ERROR", "Security exception while accessing location", securityException)
        }
    }

    override fun onNavigationItemSelected(item: MenuItem): Boolean {
        // Handle menu item selected
        when (item.itemId) {
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
        // Close the navigation drawer
        drawerLayout.closeDrawer(GravityCompat.START)
        return true
    }

    private fun navigationSystem() {
        // Initialize DrawerLayout
        drawerLayout = findViewById(R.id.drawer_layout)

        // Initialize Toolbar
        val toolbar: Toolbar = findViewById(R.id.toolbar)
        setSupportActionBar(toolbar)

        // Initialize ActionBarDrawerToggle, which will control the toggle
        actionBarDrawerToggle = ActionBarDrawerToggle(this, drawerLayout, R.string.nav_open, R.string.nav_close)

        // Setup DrawerLayout Listener
        drawerLayout.addDrawerListener(actionBarDrawerToggle)
        actionBarDrawerToggle.syncState()
        val menuButton: ImageButton = toolbar.findViewById(R.id.menu_button)

        menuButton.setOnClickListener {
            // Handle menu button click, for example, open the drawer
            drawerLayout.openDrawer(GravityCompat.START)
        }

        val navigationView: NavigationView = findViewById(R.id.nav_view)
        navigationView.setNavigationItemSelectedListener(this@HomePage)
    }

    private fun FragmentActivity.setSupportActionBar(toolbar: Toolbar) {
        (this as? AppCompatActivity)?.setSupportActionBar(toolbar)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return if (actionBarDrawerToggle.onOptionsItemSelected(item)) true else super.onOptionsItemSelected(item)
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
            } catch (_: JSONException) {}
        }
    }
}