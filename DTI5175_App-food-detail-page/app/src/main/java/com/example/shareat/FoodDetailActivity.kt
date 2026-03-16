package com.example.shareat

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import android.content.Intent
import android.net.Uri
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast

class FoodDetailActivity : AppCompatActivity() {

    // UI components
    private lateinit var foodDetailImage: ImageView
    private lateinit var foodDetailName: TextView
    private lateinit var postedByUser: TextView
    private lateinit var foodDetailCategory: TextView
    private lateinit var foodDetailDescription: TextView
    private lateinit var foodDetailQuantity: TextView
    private lateinit var foodDetailAllergens: TextView
    private lateinit var foodDetailDiets: TextView
    private lateinit var foodDetailLocation: TextView
    private lateinit var foodDetailPickupTime: TextView
    private lateinit var foodDetailType: TextView
    private lateinit var viewOnMapButton: Button
    private lateinit var getDirectionsButton: Button

    // Store location for map buttons
    private var pickupLocation: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_food_details)

        initializeViews()
        loadFoodDetails()
        setupMapButton()
        setupDirectionsButton()
    }

    private fun initializeViews() {

        foodDetailImage = findViewById(R.id.foodDetailImage)
        foodDetailName = findViewById(R.id.foodDetailName)
        postedByUser = findViewById(R.id.postedByUser)
        foodDetailCategory = findViewById(R.id.foodDetailCategory)
        foodDetailDescription = findViewById(R.id.foodDetailDescription)
        foodDetailQuantity = findViewById(R.id.foodDetailQuantity)
        foodDetailAllergens = findViewById(R.id.foodDetailAllergens)
        foodDetailDiets = findViewById(R.id.foodDetailDiets)
        foodDetailLocation = findViewById(R.id.foodDetailLocation)
        foodDetailPickupTime = findViewById(R.id.foodDetailPickupTime)
        foodDetailType = findViewById(R.id.foodDetailType)
        viewOnMapButton = findViewById(R.id.viewOnMapButton)
        getDirectionsButton = findViewById(R.id.getDirectionsButton)
    }

    private fun loadFoodDetails() {

        // -----------------------------------------------
        // Get all data passed from the Home Page
        // When user clicks a food card, Home Page sends
        // all the food info using intent.putExtra()
        // -----------------------------------------------

        val foodName = intent.getStringExtra("FOOD_NAME") ?: "Unknown"
        val userName = intent.getStringExtra("USER_NAME") ?: "Unknown"
        val category = intent.getStringExtra("CATEGORY") ?: ""
        val description = intent.getStringExtra("DESCRIPTION") ?: ""
        val quantity = intent.getStringExtra("QUANTITY") ?: ""
        val unit = intent.getStringExtra("UNIT") ?: ""
        val allergens = intent.getStringExtra("ALLERGENS") ?: "None"
        val diets = intent.getStringExtra("DIETS") ?: ""
        val location = intent.getStringExtra("LOCATION") ?: ""
        val pickupDate = intent.getStringExtra("PICKUP_DATE") ?: ""
        val startTime = intent.getStringExtra("START_TIME") ?: ""
        val endTime = intent.getStringExtra("END_TIME") ?: ""
        val type = intent.getStringExtra("TYPE") ?: "Donate"

        // Save location for map buttons
        pickupLocation = location

        // -----------------------------------------------
        // Display everything on screen
        // -----------------------------------------------

        foodDetailName.text = foodName
        postedByUser.text = "Posted by: $userName"
        foodDetailCategory.text = "Category: $category"
        foodDetailDescription.text = description
        foodDetailQuantity.text = "Quantity: $quantity $unit"
        foodDetailAllergens.text = "Allergens: $allergens"
        foodDetailDiets.text = "Diet: $diets"
        foodDetailLocation.text = "Pickup: $location"
        foodDetailPickupTime.text = "Time: $pickupDate, $startTime - $endTime"
        foodDetailType.text = "Type: $type"
    }

    // -----------------------------------------------
    // VIEW ON MAP
    // Opens Google Maps and drops a pin at the
    // pickup location
    // -----------------------------------------------
    private fun setupMapButton() {

        viewOnMapButton.setOnClickListener {

            if (pickupLocation.isNotEmpty()) {

                // "geo:0,0?q=ADDRESS" tells Google Maps
                // to search for this address and show it
                val mapUri = Uri.parse(
                    "geo:0,0?q=${Uri.encode(pickupLocation)}"
                )

                val mapIntent = Intent(Intent.ACTION_VIEW, mapUri)

                // Make sure it opens in Google Maps app
                mapIntent.setPackage("com.google.android.apps.maps")

                // Check if Google Maps is installed
                if (mapIntent.resolveActivity(packageManager) != null) {
                    startActivity(mapIntent)
                } else {
                    Toast.makeText(
                        this,
                        "Google Maps is not installed",
                        Toast.LENGTH_SHORT
                    ).show()
                }

            } else {

                Toast.makeText(
                    this,
                    "No location available",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    // -----------------------------------------------
    // GET DIRECTIONS
    // Opens Google Maps with turn-by-turn navigation
    // from user's current location to pickup location
    // -----------------------------------------------
    private fun setupDirectionsButton() {

        getDirectionsButton.setOnClickListener {

            if (pickupLocation.isNotEmpty()) {

                // "google.navigation:q=ADDRESS" tells
                // Google Maps to start navigation mode
                val directionUri = Uri.parse(
                    "google.navigation:q=${Uri.encode(pickupLocation)}"
                )

                val directionIntent = Intent(
                    Intent.ACTION_VIEW,
                    directionUri
                )

                directionIntent.setPackage("com.google.android.apps.maps")

                if (directionIntent.resolveActivity(packageManager) != null) {
                    startActivity(directionIntent)
                } else {
                    Toast.makeText(
                        this,
                        "Google Maps is not installed",
                        Toast.LENGTH_SHORT
                    ).show()
                }

            } else {

                Toast.makeText(
                    this,
                    "No location available",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }
}