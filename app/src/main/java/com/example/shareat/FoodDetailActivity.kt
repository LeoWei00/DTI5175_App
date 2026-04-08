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
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

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
    private var postId: String = ""
    private var currentStatus: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_food_details)

        initializeViews()
        loadFoodDetails()
        loadOwnerPhone()
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
        postId = intent.getStringExtra("POST_ID") ?: ""
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
        val imageUrl = intent.getStringExtra("IMAGE_URL") ?: ""
        currentStatus = intent.getStringExtra("STATUS") ?: "available"

        pickupLocation = location

        foodDetailName.text = foodName
        foodDetailType.text = "Donate"
        foodDetailType.setBackgroundResource(R.drawable.badge_background)

        postedByUser.text = "Posted by: $userName"
        foodDetailCategory.text = "Category: $category"
        foodDetailDescription.text = description
        foodDetailQuantity.text = "$quantity $unit"
        foodDetailAllergens.text = allergens
        foodDetailDiets.text = diets
        foodDetailLocation.text = location
        foodDetailPickupTime.text = "$pickupDate • $startTime - $endTime"

        if (imageUrl.isNotEmpty()) {
            Glide.with(this)
                .load(imageUrl)
                .placeholder(R.drawable.shareat_placeholder)
                .into(foodDetailImage)
        } else {
            foodDetailImage.setImageResource(R.drawable.shareat_placeholder)
        }

        if (currentStatus.equals("reserved", ignoreCase = true)) {
            reserveButton.text = "✅ Already Reserved"
            reserveButton.isEnabled = false
        }
    }

    private fun loadOwnerPhone() {
        val ownerId = intent.getStringExtra("OWNER_ID") ?: ""
        if (ownerId.isEmpty()) return

        FirebaseFirestore.getInstance()
            .collection("users")
            .document(ownerId)
            .get()
            .addOnSuccessListener { document ->
                if (document.exists()) {
                    ownerPhone = document.getString("phone_number") ?: ""
                }

                if (ownerPhone.isBlank()) {
                    callButton.isEnabled = false
                    smsButton.isEnabled = false
                }
            }
            .addOnFailureListener { e ->
                callButton.isEnabled = false
                smsButton.isEnabled = false
                Toast.makeText(this, "Failed to load phone: ${e.message}", Toast.LENGTH_SHORT).show()
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
                    val webUri = Uri.parse(
                        "https://www.google.com/maps/search/?api=1&query=${Uri.encode(pickupLocation)}"
                    )
                    val webIntent = Intent(Intent.ACTION_VIEW, webUri)
                    startActivity(webIntent)
                }
            } else {
                Toast.makeText(this, "No location available", Toast.LENGTH_SHORT).show()
            }
        }
    }


    private fun setupDirectionsButton() {
        getDirectionsButton.setOnClickListener {
            if (pickupLocation.isNotEmpty()) {
                val navUri = Uri.parse("google.navigation:q=${Uri.encode(pickupLocation)}")
                val navIntent = Intent(Intent.ACTION_VIEW, navUri)

                if (navIntent.resolveActivity(packageManager) != null) {
                    startActivity(navIntent)
                } else {
                    val webUri = Uri.parse(
                        "https://www.google.com/maps/dir/?api=1&destination=${Uri.encode(pickupLocation)}"
                    )
                    val webIntent = Intent(Intent.ACTION_VIEW, webUri)
                    startActivity(webIntent)
                }
            } else {
                Toast.makeText(this, "No location available", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun setupChatButton() {
        chatButton.setOnClickListener {
            val ownerId = intent.getStringExtra("OWNER_ID") ?: ""
            val ownerName = intent.getStringExtra("USER_NAME") ?: "User"
            val postTitle = intent.getStringExtra("FOOD_NAME") ?: "Meal"

            val currentUserId = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser?.uid ?: ""

            if (ownerId.isEmpty() || postId.isEmpty()) {
                Toast.makeText(this, "Missing chat info", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (ownerId == currentUserId) {
                Toast.makeText(this, "You cannot chat with yourself", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val chatIntent = Intent(this, ChatActivity::class.java)
            chatIntent.putExtra("OTHER_USER_ID", ownerId)
            chatIntent.putExtra("OTHER_USER_NAME", ownerName)
            chatIntent.putExtra("POST_ID", postId)
            chatIntent.putExtra("POST_TITLE", postTitle)
            startActivity(chatIntent)
        }
    }

    private fun setupCallButton() {
        callButton.setOnClickListener {
            if (ownerPhone.isNotEmpty()) {
                val callUri = Uri.parse("tel:$ownerPhone")
                val callIntent = Intent(Intent.ACTION_DIAL, callUri)
                startActivity(callIntent)
            } else {
                Toast.makeText(this, "Phone number not available", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun setupSmsButton() {
        smsButton.setOnClickListener {
            if (ownerPhone.isNotEmpty()) {
                val smsUri = Uri.parse("smsto:$ownerPhone")
                val smsIntent = Intent(Intent.ACTION_SENDTO, smsUri)
                smsIntent.putExtra("sms_body", "Hi! I'm interested in your food listing on SharEat.")
                startActivity(smsIntent)
            } else {
                Toast.makeText(this, "Phone number not available", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun setupReserveButton() {
        reserveButton.setOnClickListener {
            if (postId.isEmpty()) {
                Toast.makeText(this, "Post ID not found", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val currentUser = FirebaseAuth.getInstance().currentUser
            if (currentUser == null) {
                Toast.makeText(this, "You must be logged in", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val reservedBy = currentUser.uid
            val reservedByName = currentUser.displayName ?: "User"
            val reservedAt = System.currentTimeMillis()

            val updates = hashMapOf<String, Any>(
                "status" to "reserved",
                "reserved_by" to reservedBy,
                "reserved_by_name" to reservedByName,
                "reserved_at" to reservedAt,
                "updated_at" to reservedAt
            )

            FirebaseFirestore.getInstance()
                .collection("foods")
                .document(postId)
                .update(updates)
                .addOnSuccessListener {
                    Toast.makeText(this, "Meal reserved successfully", Toast.LENGTH_LONG).show()
                    startActivity(Intent(this, HomeActivity::class.java))
                    finish()
                }
                .addOnFailureListener { e ->
                    Toast.makeText(this, "Failed to reserve: ${e.message}", Toast.LENGTH_LONG).show()
                }
        }
    }
}