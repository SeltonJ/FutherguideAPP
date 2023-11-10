package com.example.futherguideapp

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.Manifest
import android.content.Context
import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.provider.OpenableColumns
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
    private lateinit var signUpInstance: SignUp

    // Firebase references
    private val firebaseAuth: FirebaseAuth = FirebaseAuth.getInstance()

    private var isEditMode = false
    private lateinit var imagePickerActivityResult: ActivityResultLauncher<Intent>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_user_account)

        navigationSystem()
        initializeUIComponents()
        editUserDetails()
        setupRecyclerView()
        checkPermissionAndPickImage()

        imagePickerActivityResult = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                // getting URI of selected Image
                val imageUri: Uri? = result.data?.data

                // Use the URI to upload to Firebase
                imageUri?.let { uri ->
                    // Extract the file name with extension
                    val fileName = getFileName(applicationContext, uri)

                    // Assuming 'storageRef' is a reference to your Firebase Storage
                    val fileRef = FirebaseStorage.getInstance().reference.child("users/profile_images/$fileName")

                    fileRef.putFile(uri)
                        .addOnSuccessListener { taskSnapshot ->
                            // Get the download URL and update user profile picture URL in the real-time database
                            taskSnapshot.storage.downloadUrl.addOnSuccessListener { downloadUri ->
                                val userRef = FirebaseDatabase.getInstance().getReference("users").child(firebaseAuth.currentUser!!.uid)
                                userRef.child("profileImageUrl").setValue(downloadUri.toString())
                                    .addOnSuccessListener {
                                        // Image upload successful, handle this case
                                        Glide.with(this).load(downloadUri).into(profileImageView)
                                    }
                                    .addOnFailureListener {
                                        // Handle the failure case
                                    }
                            }
                        }
                        .addOnFailureListener {
                            // Handle the failure case
                        }
                }
            }
        }

        // Replace populateUserDetails(userEmail) with Firebase call
        firebaseAuth.currentUser?.let { user ->
            populateUserDetails(user.uid)
        }
    }

    private fun getFileName(context: Context, uri: Uri): String? {
        if (uri.scheme.equals("content")) {
            val cursor = context.contentResolver.query(uri, null, null, null, null)
            cursor?.use { c ->
                if (c.moveToFirst()) {
                    val index = c.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    if (index != -1) {
                        return c.getString(index)
                    }
                }
            }
        } else if (uri.scheme.equals("file")) {
            return File(uri.path!!).name
        }
        return null
    }

    private fun checkPermissionAndPickImage() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE)
            != PackageManager.PERMISSION_GRANTED) {

            ActivityCompat.requestPermissions(this,
                arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE),
                MY_PERMISSIONS_REQUEST_READ_EXTERNAL_STORAGE)
        } else {
            pickImageFromGallery()
        }
    }

    private fun pickImageFromGallery() {
        val intent = Intent(Intent.ACTION_GET_CONTENT)
        intent.type = "image/*"
        startActivityForResult(intent, IMAGE_PICK_CODE)
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        when (requestCode) {
            MY_PERMISSIONS_REQUEST_READ_EXTERNAL_STORAGE -> {
                if ((grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED)) {
                    pickImageFromGallery()
                } else {
                    Toast.makeText(this, "Permission Denied", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == SELECT_PROFILE_IMAGE_REQUEST && resultCode == Activity.RESULT_OK && data != null) {
            data.data?.let { imageUri ->
                // Handle the picked image
                profileImageView.setImageURI(imageUri)
                uploadImageToFirebase(imageUri)
            }
        }
    }

    private fun uploadImageToFirebase(imageUri: Uri) {
        val userId = FirebaseAuth.getInstance().currentUser?.uid ?: return
        val fileRef = FirebaseStorage.getInstance().reference.child("profile_images/$userId.jpg")

        fileRef.putFile(imageUri)
            .addOnSuccessListener { taskSnapshot ->
                taskSnapshot.storage.downloadUrl.addOnSuccessListener { uri ->
                    val userRef = FirebaseDatabase.getInstance().getReference("users").child(userId)
                    userRef.child("profileImageUrl").setValue(uri.toString())
                        .addOnSuccessListener {
                            Toast.makeText(this, "Profile image updated.", Toast.LENGTH_SHORT).show()
                            Glide.with(this).load(uri).into(profileImageView)
                        }
                        .addOnFailureListener { e ->
                            Toast.makeText(this, "Failed to update profile image in database.", Toast.LENGTH_SHORT).show()
                        }
                }
            }
            .addOnFailureListener { e ->
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

                    val profileImageUrl = dataSnapshot.child("profileImageUrl").getValue(String::class.java)
                    profileImageUrl?.let { url ->
                        Glide.with(this@UserAccount).load(url).into(profileImageView)
                    } ?: run {
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

                    if (signUpInstance.isValidPassword(updatedPassword)) {
                        val currentUser = FirebaseAuth.getInstance().currentUser
                        currentUser?.let { user ->

                            user.updateEmail(updatedEmail)
                                .addOnCompleteListener { task ->
                                    if (task.isSuccessful) {
                                        user.updatePassword(updatedPassword)
                                            .addOnCompleteListener { passwordUpdateTask ->
                                                if (passwordUpdateTask.isSuccessful) {
                                                    val userUpdates = mapOf(
                                                        "userName" to updatedUsername, // Changed from "username" to "userName"
                                                        "userSurname" to updatedSurname // Changed from "surname" to "userSurname"
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


        profileImageView = findViewById(R.id.profileImageView)
        profileImageView.setOnClickListener {
            val intent = Intent(Intent.ACTION_PICK)
            intent.type = "image/*"
            startActivityForResult(intent, SELECT_PROFILE_IMAGE_REQUEST)
        }
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

    companion object {
        private const val SELECT_PROFILE_IMAGE_REQUEST = 1001
        private val MY_PERMISSIONS_REQUEST_READ_EXTERNAL_STORAGE = 101
        private val IMAGE_PICK_CODE = 102
    }
}