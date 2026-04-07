package com.example.shareat

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.bumptech.glide.Glide
import com.google.firebase.firestore.FirebaseFirestore

class FoodDetailActivity : AppCompatActivity() {

    private lateinit var db: FirebaseFirestore
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
    private var ownerName: String = ""


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
        db = FirebaseFirestore.getInstance()
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

        val foodId = intent.getStringExtra("FOOD_ID")

        if (foodId == null) {
            Toast.makeText(this, "Error: No food ID", Toast.LENGTH_LONG).show()
            finish()
            return
        }

        db.collection("foods")
            .document(foodId)
            .get()
            .addOnSuccessListener { document ->

                if (document.exists()) {

                    val foodName = document.getString("title") ?: "Unknown"
                    val userName = document.getString("owner_name") ?: "Unknown"
                    ownerName = userName
                    val category = document.getString("category") ?: ""
                    val description = document.getString("description") ?: ""
                    val quantity = document.getLong("quantity")?.toString() ?: ""
                    val unit = document.getString("unit") ?: ""
                    val location = document.getString("pickup_location") ?: ""
                    val pickupDate = document.getString("pickup_date") ?: ""
                    val startTime = document.getString("start_time") ?: ""
                    val endTime = document.getString("end_time") ?: ""
                    val type = document.getString("post_type") ?: "donate"
                    val imageUrl = document.getString("image_url") ?: ""
                    val phone = document.getString("phone") ?: ""
                    val status = document.getString("status") ?: ""
                    val allergensList = document.get("allergen_information") as? List<String> ?: emptyList()
                    val dietsList = document.get("dietary_labels") as? List<String> ?: emptyList()

                    val allergens = if (allergensList.isEmpty()) "None" else allergensList.joinToString(", ")
                    val diets = dietsList.joinToString(", ")

                    pickupLocation = location
                    ownerPhone = phone
                    if (status.equals("reserved", ignoreCase = true)) {
                        markAsReserved()
                    }
                    //  SET UI
                    foodDetailName.text = foodName
                    foodDetailType.text = type.replaceFirstChar { it.uppercase() }

                    if (type.equals("sell", ignoreCase = true)) {
                        foodDetailType.setBackgroundResource(R.drawable.badge_sell)
                    } else {
                        foodDetailType.setBackgroundResource(R.drawable.badge_background)
                    }

                    postedByUser.text = "Posted by: $userName"
                    foodDetailCategory.text = "Category: $category"
                    foodDetailDescription.text = description
                    foodDetailQuantity.text = "$quantity $unit"
                    foodDetailAllergens.text = allergens
                    foodDetailDiets.text = diets
                    foodDetailLocation.text = location
                    foodDetailPickupTime.text = "$pickupDate • $startTime - $endTime"

                    // set IMAGE
                    if (imageUrl.isNotEmpty()) {
                        Glide.with(this)
                            .load(imageUrl)
                            .placeholder(R.drawable.shareat_placeholder)
                            .into(foodDetailImage)
                    } else {
                        foodDetailImage.setImageResource(R.drawable.shareat_placeholder)
                    }

                } else {
                    Toast.makeText(this, "Food not found", Toast.LENGTH_LONG).show()
                    finish()
                }
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Error: ${e.message}", Toast.LENGTH_LONG).show()
            }
    }

    private fun setupMapButton() {
        viewOnMapButton.setOnClickListener {
            if (pickupLocation.isNotEmpty()) {
                val mapUri = Uri.parse("geo:0,0?q=${Uri.encode(pickupLocation)}")
                val mapIntent = Intent(Intent.ACTION_VIEW, mapUri)


                if (mapIntent.resolveActivity(packageManager) != null) {
                    startActivity(mapIntent)
                } else {
                    // Google Maps app not found, open in browser instead
                    val browserUri = Uri.parse("https://www.google.com/maps/search/?q=${Uri.encode(pickupLocation)}")
                    val browserIntent = Intent(Intent.ACTION_VIEW, browserUri)
                    startActivity(browserIntent)
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
                val directionUri = Uri.parse("google.navigation:q=${Uri.encode(pickupLocation)}")
                val directionIntent = Intent(Intent.ACTION_VIEW, directionUri)


                if (directionIntent.resolveActivity(packageManager) != null) {
                    startActivity(directionIntent)
                } else {
                    // Google Maps app not found, open directions in browser instead
                    val browserUri = Uri.parse("https://www.google.com/maps/dir/?api=1&destination=${Uri.encode(pickupLocation)}")
                    val browserIntent = Intent(Intent.ACTION_VIEW, browserUri)
                    startActivity(browserIntent)
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
            val userName = intent.getStringExtra("USER_NAME") ?: "this user"

            Toast.makeText(
                this,
                "Chat with $userName coming soon",
                Toast.LENGTH_SHORT
            ).show()
        }
    }

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
            val builder = androidx.appcompat.app.AlertDialog.Builder(this)

            builder.setTitle("Reserve This Meal")
            builder.setMessage(
                "Are you sure you want to reserve this meal? " +
                        "You are confirming that you will pick it up."
            )

            builder.setPositiveButton("Yes, Reserve") { _, _ ->
                val foodId = intent.getStringExtra("FOOD_ID") ?: return@setPositiveButton
                db.collection("foods")
                    .document(foodId)
                    .update("status", "reserved")
                    .addOnSuccessListener {
                        // runs if Firebase update succeeded
                        Toast.makeText(this, "Meal Reserved! Don't forget to pick it up.", Toast.LENGTH_LONG).show()
                        markAsReserved()
                        // ↑ now grey out the button
                    }
                    .addOnFailureListener { e ->

                        Toast.makeText(this, "Failed to reserve: ${e.message}", Toast.LENGTH_LONG).show()
                    }


            }

            builder.setNegativeButton("Cancel") { dialog, _ ->
                dialog.dismiss()
            }

            builder.show()
        }
    }

    private fun markAsReserved() {

        reserveButton.text = "✅ Reserved"
        reserveButton.isEnabled = false
        reserveButton.setBackgroundColor(0xFF9E9E9E.toInt())
    }
}