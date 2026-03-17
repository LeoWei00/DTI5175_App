package com.example.shareat

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

class FoodDetailActivity : AppCompatActivity() {

    private lateinit var foodDetailImage: ImageView
    private lateinit var foodDetailName: TextView
    private lateinit var foodDetailType: TextView
    private lateinit var postedByUser: TextView
    private lateinit var foodDetailCategory: TextView
    private lateinit var foodDetailDescription: TextView
    private lateinit var foodDetailQuantity: TextView
    private lateinit var foodDetailAllergens: TextView
    private lateinit var foodDetailDiets: TextView
    private lateinit var foodDetailLocation: TextView
    private lateinit var foodDetailPickupTime: TextView
    private lateinit var viewOnMapButton: Button
    private lateinit var getDirectionsButton: Button
    private lateinit var chatButton: Button
    private lateinit var callButton: Button
    private lateinit var smsButton: Button
    private lateinit var reserveButton: Button

    private var pickupLocation: String = ""
    private var ownerPhone: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_food_details)

        initializeViews()
        loadFoodDetails()
        setupMapButton()
        setupDirectionsButton()
        setupChatButton()
        setupCallButton()
        setupSmsButton()
        setupReserveButton()
    }

    private fun initializeViews() {

        foodDetailImage = findViewById(R.id.foodDetailImage)
        foodDetailName = findViewById(R.id.foodDetailName)
        foodDetailType = findViewById(R.id.foodDetailType)
        postedByUser = findViewById(R.id.postedByUser)
        foodDetailCategory = findViewById(R.id.foodDetailCategory)
        foodDetailDescription = findViewById(R.id.foodDetailDescription)
        foodDetailQuantity = findViewById(R.id.foodDetailQuantity)
        foodDetailAllergens = findViewById(R.id.foodDetailAllergens)
        foodDetailDiets = findViewById(R.id.foodDetailDiets)
        foodDetailLocation = findViewById(R.id.foodDetailLocation)
        foodDetailPickupTime = findViewById(R.id.foodDetailPickupTime)
        viewOnMapButton = findViewById(R.id.viewOnMapButton)
        getDirectionsButton = findViewById(R.id.getDirectionsButton)
        chatButton = findViewById(R.id.chatButton)
        callButton = findViewById(R.id.callButton)
        smsButton = findViewById(R.id.smsButton)
        reserveButton = findViewById(R.id.reserveButton)
    }

    private fun loadFoodDetails() {

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
        val phone = intent.getStringExtra("PHONE") ?: ""

        ownerPhone = phone

        pickupLocation = location

        // -----------------------------------------------
        // Set food name and type badge
        // -----------------------------------------------
        foodDetailName.text = foodName
        foodDetailType.text = type

        // Change badge color based on type
        if (type == "Sell") {
            foodDetailType.setBackgroundResource(R.drawable.badge_sell)
        } else {
            foodDetailType.setBackgroundResource(R.drawable.badge_background)
        }

        postedByUser.text = "Posted by: $userName"
        foodDetailCategory.text = "Category: $category"
        foodDetailDescription.text = description

        // -----------------------------------------------
        // These now show ONLY the values (no labels)
        // because labels are already in the XML layout
        // -----------------------------------------------
        foodDetailQuantity.text = "$quantity $unit"
        foodDetailAllergens.text = allergens
        foodDetailDiets.text = diets

        foodDetailLocation.text = location
        foodDetailPickupTime.text = "$pickupDate, $startTime - $endTime"
    }

    private fun setupMapButton() {

        viewOnMapButton.setOnClickListener {

            if (pickupLocation.isNotEmpty()) {

                val mapUri = Uri.parse(
                    "geo:0,0?q=${Uri.encode(pickupLocation)}"
                )

                val mapIntent = Intent(Intent.ACTION_VIEW, mapUri)
                mapIntent.setPackage("com.google.android.apps.maps")

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

    private fun setupDirectionsButton() {

        getDirectionsButton.setOnClickListener {

            if (pickupLocation.isNotEmpty()) {

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
    private fun setupChatButton() {

        chatButton.setOnClickListener {

            val chatIntent = Intent(this, ChatActivity::class.java)

            // Send the other user's name so chat knows who you're talking to
            chatIntent.putExtra("CHAT_WITH", intent.getStringExtra("USER_NAME"))

            startActivity(chatIntent)
        }
    }
    // -----------------------------------------------
    // CALL - Opens phone dialer with owner's number
    // -----------------------------------------------
    private fun setupCallButton() {

        callButton.setOnClickListener {

            if (ownerPhone.isNotEmpty()) {

                val callUri = Uri.parse("tel:$ownerPhone")

                val callIntent = Intent(Intent.ACTION_DIAL, callUri)

                startActivity(callIntent)

            } else {

                Toast.makeText(
                    this,
                    "Phone number not available",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }
    private fun setupSmsButton() {

        smsButton.setOnClickListener {

            if (ownerPhone.isNotEmpty()) {

                val smsUri = Uri.parse("smsto:$ownerPhone")

                val smsIntent = Intent(Intent.ACTION_SENDTO, smsUri)

                smsIntent.putExtra(
                    "sms_body",
                    "Hi! I'm interested in your food listing on ShareAt."
                )

                startActivity(smsIntent)

            } else {

                Toast.makeText(
                    this,
                    "Phone number not available",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }
    private fun setupReserveButton() {

        reserveButton.setOnClickListener {

            // Show confirmation dialog
            val builder = androidx.appcompat.app.AlertDialog.Builder(this)

            builder.setTitle("Reserve This Meal")

            builder.setMessage(
                "Are you sure you want to reserve this meal? " +
                        "You are confirming that you will pick it up."
            )

            builder.setPositiveButton("Yes, Reserve") { _, _ ->

                Toast.makeText(
                    this,
                    "Meal Reserved! Don't forget to pick it up.",
                    Toast.LENGTH_LONG
                ).show()

                // Disable button after reservation
                reserveButton.text = "✅ Reserved"
                reserveButton.isEnabled = false
                reserveButton.setBackgroundColor(0xFF9E9E9E.toInt())
            }

            builder.setNegativeButton("Cancel") { dialog, _ ->

                dialog.dismiss()
            }

            builder.show()
        }
    }
}