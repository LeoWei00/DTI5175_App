package com.example.shareat

import android.Manifest
import android.app.AlertDialog
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.widget.EditText
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.gms.location.LocationServices
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.firebase.firestore.FirebaseFirestore

class HomeActivity : AppCompatActivity() {

    private lateinit var searchBar: EditText
    private lateinit var filterButton: ImageButton
    private lateinit var recyclerFoods: RecyclerView
    private lateinit var tvMealCount: TextView
    private lateinit var fabReservations: FloatingActionButton

    private lateinit var navHomeContainer: LinearLayout
    private lateinit var navPostContainer: LinearLayout
    private lateinit var navChatContainer: LinearLayout
    private lateinit var navProfileContainer: LinearLayout

    private lateinit var db: FirebaseFirestore
    private lateinit var nearbyFoodList: MutableList<NearbyFoodPost>
    private lateinit var adapter: FoodPostAdapter

    private val LOCATION_PERMISSION_CODE = 200
    private val MAX_DISTANCE_KM = 1.5

    private val filterOptions = arrayOf(
        "All",
        "Donate only",
        "Sell only",
        "Vegetarian",
        "Vegan",
        "Halal",
        "Gluten-Free",
        "Homemade",
        "Baked Goods"
    )

    private var selectedFilter = "All"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_home)

        searchBar = findViewById(R.id.searchBar)
        filterButton = findViewById(R.id.filterButton)
        recyclerFoods = findViewById(R.id.recyclerFoods)
        tvMealCount = findViewById(R.id.tvMealCount)
        fabReservations = findViewById(R.id.fabReservations)

        navHomeContainer = findViewById(R.id.navHomeContainer)
        navPostContainer = findViewById(R.id.navPostContainer)
        navChatContainer = findViewById(R.id.navChatContainer)
        navProfileContainer = findViewById(R.id.navProfileContainer)

        db = FirebaseFirestore.getInstance()
        nearbyFoodList = mutableListOf()

        adapter = FoodPostAdapter(nearbyFoodList)
        recyclerFoods.layoutManager = LinearLayoutManager(this)
        recyclerFoods.adapter = adapter

        filterButton.setOnClickListener {
            showFilterDialog()
        }

        fabReservations.setOnClickListener {
            startActivity(Intent(this, MyReservationsActivity::class.java))
        }

        navHomeContainer.setOnClickListener {
            // Already on Home
        }

        navPostContainer.setOnClickListener {
            startActivity(Intent(this, PostFoodActivity::class.java))
        }

        navChatContainer.setOnClickListener {
            startActivity(Intent(this, MessagesActivity::class.java))
        }

        navProfileContainer.setOnClickListener {
            startActivity(Intent(this, ProfileActivity::class.java))
        }

        setupSearch()
        refreshCurrentMode()
    }

    private fun showFilterDialog() {
        val checkedItem = filterOptions.indexOf(selectedFilter).coerceAtLeast(0)

        AlertDialog.Builder(this)
            .setTitle("Filter foods")
            .setSingleChoiceItems(filterOptions, checkedItem) { dialog, which ->
                selectedFilter = filterOptions[which]
                dialog.dismiss()
                refreshCurrentMode()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun refreshCurrentMode() {
        val query = searchBar.text.toString().trim()
        if (query.isEmpty()) {
            checkLocationAndLoadFoods()
        } else {
            searchFoods(query)
        }
    }

    private fun setupSearch() {
        searchBar.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                val query = s?.toString()?.trim().orEmpty()

                if (query.isEmpty()) {
                    checkLocationAndLoadFoods()
                } else {
                    searchFoods(query)
                }
            }

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
            }

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
            }
        })
    }

    private fun matchesSelectedFilter(food: FoodPost): Boolean {
        return when (selectedFilter) {
            "All" -> true
            "Donate only" -> food.post_type.equals("donate", ignoreCase = true)
            "Sell only" -> food.post_type.equals("sell", ignoreCase = true)
            "Vegetarian" -> food.dietary_labels.any { it.equals("Vegetarian", ignoreCase = true) }
            "Vegan" -> food.dietary_labels.any { it.equals("Vegan", ignoreCase = true) }
            "Halal" -> food.dietary_labels.any { it.equals("Halal", ignoreCase = true) }
            "Gluten-Free" -> food.dietary_labels.any { it.equals("Gluten-Free", ignoreCase = true) }
            "Homemade" -> food.category.equals("Homemade", ignoreCase = true)
            "Baked Goods" -> food.category.equals("Baked Goods", ignoreCase = true)
            else -> true
        }
    }

    private fun searchFoods(query: String) {
        val lowerQuery = query.lowercase()

        db.collection("foods")
            .get()
            .addOnSuccessListener { result ->
                val matchedFoods = mutableListOf<NearbyFoodPost>()

                for (document in result) {
                    val food = document.toObject(FoodPost::class.java)

                    val matchesSearch =
                        food.title.lowercase().contains(lowerQuery) ||
                                food.description.lowercase().contains(lowerQuery) ||
                                food.category.lowercase().contains(lowerQuery) ||
                                food.ingredients.lowercase().contains(lowerQuery) ||
                                food.owner_name.lowercase().contains(lowerQuery) ||
                                food.pickup_location.lowercase().contains(lowerQuery) ||
                                food.dietary_labels.any { it.lowercase().contains(lowerQuery) } ||
                                food.allergen_information.any { it.lowercase().contains(lowerQuery) }

                    if (matchesSearch && matchesSelectedFilter(food)) {
                        matchedFoods.add(NearbyFoodPost(food, -1.0))
                    }
                }

                nearbyFoodList.clear()
                nearbyFoodList.addAll(matchedFoods)
                adapter.notifyDataSetChanged()
                tvMealCount.text = "${nearbyFoodList.size} meals"
            }
            .addOnFailureListener { e ->
                Toast.makeText(
                    this,
                    "Error searching foods: ${e.message}",
                    Toast.LENGTH_LONG
                ).show()
            }
    }

    private fun checkLocationAndLoadFoods() {
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
            loadNearbyFoods()
        }
    }

    private fun loadNearbyFoods() {
        val fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED &&
            ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            Toast.makeText(this, "Location permission is required", Toast.LENGTH_LONG).show()
            return
        }

        fusedLocationClient.lastLocation
            .addOnSuccessListener { userLocation ->
                if (userLocation == null) {
                    Toast.makeText(this, "Unable to get your location", Toast.LENGTH_LONG).show()
                    return@addOnSuccessListener
                }

                db.collection("foods")
                    .get()
                    .addOnSuccessListener { result ->
                        val filteredFoods = mutableListOf<NearbyFoodPost>()

                        for (document in result) {
                            val food = document.toObject(FoodPost::class.java)

                            if (!matchesSelectedFilter(food)) continue
                            if (food.pickup_location.isBlank()) continue

                            try {
                                val geocoder = android.location.Geocoder(this)
                                val addresses = geocoder.getFromLocationName(food.pickup_location, 1)

                                if (!addresses.isNullOrEmpty()) {
                                    val address = addresses[0]

                                    val results = FloatArray(1)
                                    android.location.Location.distanceBetween(
                                        userLocation.latitude,
                                        userLocation.longitude,
                                        address.latitude,
                                        address.longitude,
                                        results
                                    )

                                    val distanceKm = results[0] / 1000.0

                                    if (distanceKm <= MAX_DISTANCE_KM) {
                                        filteredFoods.add(NearbyFoodPost(food, distanceKm))
                                    }
                                }
                            } catch (_: Exception) {
                            }
                        }

                        filteredFoods.sortBy { it.distanceKm }

                        nearbyFoodList.clear()
                        nearbyFoodList.addAll(filteredFoods)
                        adapter.notifyDataSetChanged()
                        tvMealCount.text = "${nearbyFoodList.size} meals"
                    }
                    .addOnFailureListener { e ->
                        Toast.makeText(
                            this,
                            "Error loading foods: ${e.message}",
                            Toast.LENGTH_LONG
                        ).show()
                    }
            }
            .addOnFailureListener { e ->
                Toast.makeText(
                    this,
                    "Error getting location: ${e.message}",
                    Toast.LENGTH_LONG
                ).show()
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
                checkLocationAndLoadFoods()
            } else {
                Toast.makeText(
                    this,
                    "Location permission denied. Cannot show nearby meals.",
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }

    override fun onResume() {
        super.onResume()
        refreshCurrentMode()
    }
}