package com.example.shareat

import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AppCompatActivity

import android.app.AlertDialog
import android.content.Intent
import android.graphics.Bitmap
import android.provider.MediaStore
import androidx.activity.result.contract.ActivityResultContracts

import android.Manifest
import android.content.pm.PackageManager
import android.location.Geocoder
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.google.android.gms.location.LocationServices
import java.util.Locale


import android.speech.RecognizerIntent


class PostFoodActivity : AppCompatActivity() {

    // UI components
    private lateinit var radioDonate: RadioButton
    private lateinit var radioSell: RadioButton

    private lateinit var mealName: EditText
    private lateinit var category: EditText

    private lateinit var allergenDairy: CheckBox
    private lateinit var allergenGluten: CheckBox
    private lateinit var allergenEggs: CheckBox
    private lateinit var allergenNuts: CheckBox

    private lateinit var veg: CheckBox
    private lateinit var vegan: CheckBox
    private lateinit var glutenFree: CheckBox

    private lateinit var quantity: EditText
    private lateinit var unitSpinner: Spinner

    private lateinit var location: EditText
    private lateinit var pickupDate: EditText
    private lateinit var startTime: EditText
    private lateinit var endTime: EditText

    private lateinit var postDonation: Button
    private lateinit var foodImage: ImageView

    private lateinit var locationPicker: ImageButton
    private val LOCATION_PERMISSION_CODE = 100

    private lateinit var foodDescription: EditText
    private lateinit var speechButton: ImageButton

    private val cameraLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->

            if (result.resultCode == RESULT_OK) {

                val imageBitmap = result.data?.extras?.get("data") as? Bitmap

                imageBitmap?.let {
                    foodImage.setImageBitmap(it)
                }
            }
        }

    private val galleryLauncher =
        registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->

            uri?.let {
                foodImage.setImageURI(it)
            }
        }

    private val speechLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->

            if (result.resultCode == RESULT_OK) {

                val spokenText = result.data
                    ?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
                    ?.get(0)

                spokenText?.let {
                    foodDescription.setText(it)
                }
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_post_food)

        initializeViews()
        setupSpinner()
        setupImagePicker()
        setupLocationPicker()
        setupSpeechInput()
        setupPostButton()
    }

    private fun initializeViews() {

        radioDonate = findViewById(R.id.radioDonate)
        radioSell = findViewById(R.id.radioSell)

        mealName = findViewById(R.id.mealName)
        category = findViewById(R.id.category)
        foodDescription = findViewById(R.id.foodDescription)
        speechButton = findViewById(R.id.speechButton)

        allergenDairy = findViewById(R.id.allergenDairy)
        allergenGluten = findViewById(R.id.allergenGluten)
        allergenEggs = findViewById(R.id.allergenEggs)
        allergenNuts = findViewById(R.id.allergenNuts)

        veg = findViewById(R.id.veg)
        vegan = findViewById(R.id.vegan)
        glutenFree = findViewById(R.id.glutenFree)

        quantity = findViewById(R.id.quantity)
        unitSpinner = findViewById(R.id.unitSpinner)

        location = findViewById(R.id.location)
        locationPicker = findViewById(R.id.locationPicker)

        pickupDate = findViewById(R.id.pickupDate)
        startTime = findViewById(R.id.startTime)
        endTime = findViewById(R.id.endTime)

        postDonation = findViewById(R.id.postDonation)
        foodImage = findViewById(R.id.foodImage)

    }

    private fun setupSpinner() {

        val units = arrayOf(
            "servings",
            "items",
            "plates",
            "boxes"
        )

        val adapter = ArrayAdapter(
            this,
            android.R.layout.simple_spinner_dropdown_item,
            units
        )

        unitSpinner.adapter = adapter
    }

    private fun setupPostButton() {

        postDonation.setOnClickListener {

            val meal = mealName.text.toString()
            val categoryText = category.text.toString()
            val qty = quantity.text.toString()
            val unit = unitSpinner.selectedItem.toString()
            val pickupLocation = location.text.toString()

            val isDonate = radioDonate.isChecked
            val isSell = radioSell.isChecked

            val allergens = mutableListOf<String>()

            if (allergenDairy.isChecked) allergens.add("Dairy")
            if (allergenGluten.isChecked) allergens.add("Gluten")
            if (allergenEggs.isChecked) allergens.add("Eggs")
            if (allergenNuts.isChecked) allergens.add("Nuts")

            val diets = mutableListOf<String>()

            if (veg.isChecked) diets.add("Vegetarian")
            if (vegan.isChecked) diets.add("Vegan")
            if (glutenFree.isChecked) diets.add("Gluten Free")

            Toast.makeText(
                this,
                "Posting: $meal ($qty $unit)",
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    private fun setupImagePicker() {

        foodImage.setOnClickListener {

            val options = arrayOf("Take Photo", "Choose from Gallery")

            val builder = AlertDialog.Builder(this)
            builder.setTitle("Upload Food Photo")

            builder.setItems(options) { _, which ->

                when (which) {

                    0 -> {  // Camera
                        val intent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
                        cameraLauncher.launch(intent)
                    }

                    1 -> {  // Gallery
                        galleryLauncher.launch("image/*")
                    }
                }
            }

            builder.show()
        }
    }

    private fun setupLocationPicker() {

        locationPicker.setOnClickListener {

            if (ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.ACCESS_FINE_LOCATION
                ) != PackageManager.PERMISSION_GRANTED
            ) {

                ActivityCompat.requestPermissions(
                    this,
                    arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                    LOCATION_PERMISSION_CODE
                )

            } else {

                fetchLocation()
            }
        }
    }

    private fun fetchLocation() {

        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED &&
            ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            return
        }

        val fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        fusedLocationClient.lastLocation.addOnSuccessListener { userLocation ->

            if (userLocation != null) {

                val geocoder = Geocoder(this, Locale.getDefault())

                val addresses = geocoder.getFromLocation(
                    userLocation.latitude,
                    userLocation.longitude,
                    1
                )

                if (!addresses.isNullOrEmpty()) {

                    val address = addresses[0].getAddressLine(0)

                    location.setText(address)
                }
            }
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {

        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        if (requestCode == LOCATION_PERMISSION_CODE) {

            if (grantResults.isNotEmpty() &&
                grantResults[0] == PackageManager.PERMISSION_GRANTED
            ) {

                fetchLocation()
            }
        }
    }

    private fun setupSpeechInput() {

        speechButton.setOnClickListener {

            val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH)

            intent.putExtra(
                RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                RecognizerIntent.LANGUAGE_MODEL_FREE_FORM
            )

            intent.putExtra(
                RecognizerIntent.EXTRA_PROMPT,
                "Describe the food"
            )

            speechLauncher.launch(intent)
        }
    }
}