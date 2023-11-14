package com.example.futherguideapp

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.Manifest
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.media.ExifInterface
import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.provider.MediaStore
import android.provider.OpenableColumns
import android.util.Base64
import android.util.Log
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.appcompat.widget.Toolbar
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.futherguideapp.adapters.BirdObservationAdapter
import com.example.futherguideapp.models.UserBirdObservation
import com.google.android.material.navigation.NavigationView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.google.firebase.storage.FirebaseStorage
import java.io.ByteArrayOutputStream
import java.io.File

class UserAccount : AppCompatActivity() {

    //Nav variables
    private lateinit var drawerLayout: DrawerLayout
    private lateinit var actionBarDrawerToggle: ActionBarDrawerToggle
    private lateinit var toolbar: Toolbar

    private lateinit var userName: EditText
    private lateinit var userSurname : EditText
    private lateinit var userEmail: EditText
    private lateinit var userPassword: EditText

    private lateinit var methods: SignUp
    private lateinit var birdObservationRecyclerView: RecyclerView
    private lateinit var birdObservationAdapter: BirdObservationAdapter

    private lateinit var profileImageView: ImageView

    private val PICK_IMAGE_REQUEST = 1

    // Firebase references
    private val firebaseAuth: FirebaseAuth = FirebaseAuth.getInstance()

    private var isEditMode = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_user_account)

        navigationSystem()
        initializeUIComponents()
        editUserDetails()
        setupRecyclerView()

        // Add this to your initializeUIComponents function
        profileImageView.setOnClickListener {
            val intent = Intent(Intent.ACTION_PICK)
            intent.type = "image/*"
            startActivityForResult(intent, PICK_IMAGE_REQUEST)
        }

        // Replace populateUserDetails(userEmail) with Firebase call
        firebaseAuth.currentUser?.let { user ->
            populateUserDetails(user.uid)
        }
    }

    // Override onActivityResult to handle image selection
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == PICK_IMAGE_REQUEST && resultCode == Activity.RESULT_OK && data != null && data.data != null) {
            val imageUri: Uri? = data.data
            imageUri?.let { uri ->
                val bitmap = MediaStore.Images.Media.getBitmap(contentResolver, uri)
                val rotatedBitmap = getCorrectlyOrientedImage(this, uri)
                uploadImageToFirebase(rotatedBitmap ?: bitmap) // Upload the rotated bitmap, or the original if rotation fails
            }
        }
    }

    private fun getCorrectlyOrientedImage(context: Context, photoUri: Uri): Bitmap? {
        val inputStream = context.contentResolver.openInputStream(photoUri)
        val ei = ExifInterface(inputStream!!)
        val orientation = ei.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_UNDEFINED)
        inputStream.close()

        val bitmap = MediaStore.Images.Media.getBitmap(context.contentResolver, photoUri)

        return when (orientation) {
            ExifInterface.ORIENTATION_ROTATE_90 -> rotateImage(bitmap, 90f)
            ExifInterface.ORIENTATION_ROTATE_180 -> rotateImage(bitmap, 180f)
            ExifInterface.ORIENTATION_ROTATE_270 -> rotateImage(bitmap, 270f)
            else -> bitmap
        }
    }

    private fun rotateImage(source: Bitmap, angle: Float): Bitmap {
        val matrix = Matrix()
        matrix.postRotate(angle)
        return Bitmap.createBitmap(source, 0, 0, source.width, source.height, matrix, true)
    }

    // Upload the image to Firebase
    private fun uploadImageToFirebase(bitmap: Bitmap) {
        val userId = FirebaseAuth.getInstance().currentUser?.uid ?: return
        val baos = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, 100, baos)
        val byteArray = baos.toByteArray()
        val encodedImage = Base64.encodeToString(byteArray, Base64.DEFAULT)

        FirebaseDatabase.getInstance().getReference("users").child(userId)
            .child("profileImageBase64").setValue(encodedImage)
            .addOnSuccessListener {
                Glide.with(this).load(bitmap).into(profileImageView)
                Toast.makeText(this, "Profile image updated.", Toast.LENGTH_SHORT).show()
            }
            .addOnFailureListener {
                Toast.makeText(this, "Failed to upload image.", Toast.LENGTH_SHORT).show()
            }
    }

    private fun setupRecyclerView() {
        birdObservationRecyclerView = findViewById(R.id.rv_birdObservations)
        birdObservationRecyclerView.layoutManager = LinearLayoutManager(this)
        birdObservationAdapter = BirdObservationAdapter()
        birdObservationRecyclerView.adapter = birdObservationAdapter

        val adapter = BirdObservationAdapter()
        birdObservationRecyclerView.adapter = adapter

        adapter.onItemClickListener = { birdObservation ->
            val intent = Intent(this, BirdObservationDetails::class.java)
            intent.putExtra("birdName", birdObservation.birdName)
            intent.putExtra("location", birdObservation.location)
            intent.putExtra("observationDate", birdObservation.observationDate)
            intent.putExtra("quantity", birdObservation.quantity)
            startActivity(intent)
        }

        // Fetch bird observations from Firebase
        getAllBirdObservations()
    }

    private fun getAllBirdObservations() {
        val currentUser = FirebaseAuth.getInstance().currentUser
        currentUser?.let { user ->
            val userObservationsRef = FirebaseDatabase.getInstance()
                .getReference("observations/userObservations/${user.uid}")

            userObservationsRef.addValueEventListener(object : ValueEventListener {
                override fun onDataChange(dataSnapshot: DataSnapshot) {
                    if (dataSnapshot.exists()) {
                        val birdObservations = mutableListOf<UserBirdObservation>()
                        for (snapshot in dataSnapshot.children) {
                            snapshot.getValue(UserBirdObservation::class.java)?.let { observation ->
                                birdObservations.add(observation)
                            }
                        }
                        (birdObservationRecyclerView.adapter as BirdObservationAdapter).updateData(birdObservations)
                    }
                }

                override fun onCancelled(databaseError: DatabaseError) {
                    Log.w("FirebaseData", "loadUserObservations:onCancelled", databaseError.toException())
                }
            })
        } ?: run {
            Toast.makeText(this, "User not logged in", Toast.LENGTH_SHORT).show()
        }
    }

    private fun populateUserDetails(userId: String) {
        // Assuming 'databaseReference' is initialized to point to the root of your Firebase Realtime Database
        val userRef = FirebaseDatabase.getInstance().getReference("users").child(userId)

        // Retrieve user details from Firebase Realtime Database
        userRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                if (dataSnapshot.exists()) {
                    // Get the values from the dataSnapshot and set them to the EditText fields
                    val userNameValue = dataSnapshot.child("userName").getValue(String::class.java)
                    val userEmailValue = dataSnapshot.child("userEmail").getValue(String::class.java)
                    val userSurnameValue = dataSnapshot.child("userSurname").getValue(String::class.java)
                    val userPasswordValue = dataSnapshot.child("userPassword").getValue(String::class.java)

                    // Set the user details to EditTexts
                    userName.setText(userNameValue)
                    userEmail.setText(userEmailValue)
                    userSurname.setText(userSurnameValue)
                    userPassword.setText(userPasswordValue)

                    // Retrieve and display the profile image
                    val profileImageBase64 = dataSnapshot.child("profileImageBase64").getValue(String::class.java)
                    profileImageBase64?.let {
                        val decodedBytes = Base64.decode(it, Base64.DEFAULT)
                        val decodedBitmap = BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.size)
                        Glide.with(this@UserAccount).load(decodedBitmap).into(profileImageView)
                    } ?: run {
                        // Set default image if no image is found
                        Glide.with(this@UserAccount).load(R.drawable.user).into(profileImageView)
                    }
                } else {
                    // Handle the case where the user data does not exist
                    Toast.makeText(this@UserAccount, "User data not found", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onCancelled(databaseError: DatabaseError) {
                // Handle any errors
                Toast.makeText(this@UserAccount, databaseError.message, Toast.LENGTH_SHORT).show()
            }
        })
    }

    @SuppressLint("SetTextI18n")
    private fun editUserDetails() {
        val editUserDetailsButton: Button = findViewById(R.id.btn_editUserDetails)

        doNotAllowEditText()

        editUserDetailsButton.setOnClickListener {

            if (isEditMode) {
                if (!checkEditTextStatus()) {
                    Toast.makeText(this, "Please fill in all fields.", Toast.LENGTH_SHORT).show()
                } else {
                    val updatedUsername = userName.text.toString().trim()
                    val updatedSurname = userSurname.text.toString().trim()
                    val updatedEmail = userEmail.text.toString().trim()
                    val updatedPassword = userPassword.text.toString().trim()

                    if (SignUp.isValidPassword(updatedPassword)) {

                        val currentUser = FirebaseAuth.getInstance().currentUser
                        currentUser?.let { user ->

                            user.updateEmail(updatedEmail)
                                .addOnCompleteListener { task ->
                                    if (task.isSuccessful) {
                                        user.updatePassword(updatedPassword)
                                            .addOnCompleteListener { passwordUpdateTask ->
                                                if (passwordUpdateTask.isSuccessful) {
                                                    val userUpdates = mapOf(
                                                        "userName" to updatedUsername,
                                                        "userSurname" to updatedSurname
                                                    )

                                                    FirebaseDatabase.getInstance().reference.child("users").child(user.uid)
                                                        .updateChildren(userUpdates)
                                                        .addOnSuccessListener {
                                                            Toast.makeText(this, "Details updated successfully!", Toast.LENGTH_SHORT).show()
                                                            doNotAllowEditText()
                                                            editUserDetailsButton.text = "Edit"
                                                            isEditMode = false
                                                        }
                                                        .addOnFailureListener {
                                                            Toast.makeText(this, "Failed to update user details.", Toast.LENGTH_SHORT).show()
                                                        }
                                                } else {
                                                    Toast.makeText(this, "Failed to update password.", Toast.LENGTH_SHORT).show()
                                                }
                                            }
                                    } else {
                                        Toast.makeText(this, "Failed to update email.", Toast.LENGTH_SHORT).show()
                                    }
                                }
                        }
                    } else {
                        Toast.makeText(this, "Invalid password format.", Toast.LENGTH_LONG).show()
                    }
                }
            } else {
                allowEditText()
                editUserDetailsButton.text = "Save"
                isEditMode = true
            }
        }
    }

    private fun initializeUIComponents() {
        userName = findViewById(R.id.userNameEt)
        userSurname = findViewById(R.id.userSurnameEt)
        userEmail = findViewById(R.id.emailEt)
        userPassword = findViewById(R.id.passwordEt)
        profileImageView = findViewById(R.id.profileImageView)
        birdObservationRecyclerView = findViewById(R.id.rv_birdObservations)
        methods = SignUp()
    }

    private fun allowEditText(){
        userName.isEnabled = true
        userSurname.isEnabled = true
        userEmail.isEnabled = true
        userPassword.isEnabled = true
    }

    private fun doNotAllowEditText(){
        userName.isEnabled = false
        userSurname.isEnabled = false
        userEmail.isEnabled = false
        userPassword.isEnabled = false
    }

    private fun checkEditTextStatus(): Boolean {

        // Initialize your UI components here
        userName = findViewById(R.id.userNameEt)
        userSurname = findViewById(R.id.userSurnameEt)
        userEmail = findViewById(R.id.emailEt)
        userPassword = findViewById(R.id.passwordEt)

        // Getting values from EditTexts and storing them in String Variables.
        val username = userName.text.toString()
        val surname = userSurname.text.toString()
        val email = userEmail.text.toString()
        val password = userPassword.text.toString()

        // Checking if EditTexts are empty or not using Kotlin's isNullOrEmpty() function.
        return username.isNotEmpty() && surname.isNotEmpty() && email.isNotEmpty() && password.isNotEmpty()
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
            true
        }
    }
}