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
import android.util.Log
import com.google.android.gms.location.CurrentLocationRequest
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource

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
    private val MAX_DISTANCE_KM = 10.0

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

    private val selectedFilters = mutableSetOf<String>("All")

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
        val checkedItems = BooleanArray(filterOptions.size) { index ->
            selectedFilters.contains(filterOptions[index])
        }

        AlertDialog.Builder(this)
            .setTitle("Filter foods")
            .setMultiChoiceItems(filterOptions, checkedItems) { _, which, isChecked ->
                val option = filterOptions[which]

                if (option == "All") {
                    if (isChecked) {
                        selectedFilters.clear()
                        selectedFilters.add("All")
                        for (i in checkedItems.indices) checkedItems[i] = (i == which)
                    } else {
                        selectedFilters.remove("All")
                    }
                } else {
                    if (isChecked) {
                        selectedFilters.remove("All")
                        selectedFilters.add(option)
                    } else {
                        selectedFilters.remove(option)
                    }
                }
            }
            .setPositiveButton("Apply") { _, _ ->
                if (selectedFilters.isEmpty()) {
                    selectedFilters.add("All")
                }
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
        if (selectedFilters.isEmpty() || selectedFilters.contains("All")) {
            return true
        }

        return selectedFilters.all { filter ->
            when (filter) {
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

                    if (matchesSearch && matchesSelectedFilter(food) && !isExpired(food.pickup_date, food.end_time) && !isConfirmed(food)) {
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
            Log.d("HOME_DEBUG", "Location permission missing")
            return
        }

        val currentLocationRequest = CurrentLocationRequest.Builder()
            .setPriority(Priority.PRIORITY_HIGH_ACCURACY)
            .setMaxUpdateAgeMillis(0)
            .build()

        val cancellationTokenSource = CancellationTokenSource()

        fusedLocationClient.getCurrentLocation(
            currentLocationRequest,
            cancellationTokenSource.token
        )
            .addOnSuccessListener { userLocation ->
                if (userLocation == null) {
                    Toast.makeText(this, "Unable to get your current location", Toast.LENGTH_LONG).show()
                    Log.d("HOME_DEBUG", "getCurrentLocation returned null")
                    return@addOnSuccessListener
                }

                Log.d(
                    "HOME_DEBUG",
                    "Fresh user location: lat=${userLocation.latitude}, lng=${userLocation.longitude}"
                )

                db.collection("foods")
                    .get()
                    .addOnSuccessListener { result ->
                        val filteredFoods = mutableListOf<NearbyFoodPost>()
                        Log.d("HOME_DEBUG", "Fetched ${result.size()} food documents from Firebase")

                        for (document in result) {
                            val food = document.toObject(FoodPost::class.java)

                            Log.d(
                                "HOME_DEBUG",
                                "Checking food: title=${food.title}, pickup_location='${food.pickup_location}', lat=${food.pickup_lat}, lng=${food.pickup_lng}"
                            )

                            if (isExpired(food.pickup_date, food.end_time)) {
                                Log.d("HOME_DEBUG", "Skipped '${food.title}' because it is expired")
                                continue
                            }

                            if (isConfirmed(food)) {
                                Log.d("HOME_DEBUG", "Skipped '${food.title}' because it is confirmed")
                                continue
                            }

                            if (!matchesSelectedFilter(food)) {
                                Log.d("HOME_DEBUG", "Skipped '${food.title}' because filter did not match")
                                continue
                            }

                            val pickupLat = food.pickup_lat
                            val pickupLng = food.pickup_lng

                            if (pickupLat == 0.0 && pickupLng == 0.0) {
                                Log.d("HOME_DEBUG", "Skipped '${food.title}' because pickup coordinates are missing")
                                continue
                            }

                            val results = FloatArray(1)
                            android.location.Location.distanceBetween(
                                userLocation.latitude,
                                userLocation.longitude,
                                pickupLat,
                                pickupLng,
                                results
                            )

                            val distanceKm = results[0] / 1000.0

                            Log.d(
                                "HOME_DEBUG",
                                "Distance to '${food.title}' = %.2f km (max allowed = %.2f km)"
                                    .format(distanceKm, MAX_DISTANCE_KM)
                            )

                            if (distanceKm <= MAX_DISTANCE_KM) {
                                filteredFoods.add(NearbyFoodPost(food, distanceKm))
                                Log.d("HOME_DEBUG", "Included '${food.title}'")
                            } else {
                                Log.d("HOME_DEBUG", "Excluded '${food.title}' because too far")
                            }
                        }

                        filteredFoods.sortBy { it.distanceKm }

                        Log.d("HOME_DEBUG", "Final nearby foods count: ${filteredFoods.size}")

                        nearbyFoodList.clear()
                        nearbyFoodList.addAll(filteredFoods)
                        adapter.notifyDataSetChanged()
                        tvMealCount.text = "${nearbyFoodList.size} meals"
                    }
                    .addOnFailureListener { e ->
                        Log.d("HOME_DEBUG", "Error loading foods from Firebase: ${e.message}")
                        Toast.makeText(
                            this,
                            "Error loading foods: ${e.message}",
                            Toast.LENGTH_LONG
                        ).show()
                    }
            }
            .addOnFailureListener { e ->
                Log.d("HOME_DEBUG", "Error getting current location: ${e.message}")
                Toast.makeText(
                    this,
                    "Error getting current location: ${e.message}",
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

    private fun isExpired(pickupDate: String, endTime: String): Boolean {
        return try {
            val dateTimeFormat = java.text.SimpleDateFormat("yyyy-MM-dd HH:mm", java.util.Locale.getDefault())
            val expiryDateTime = dateTimeFormat.parse("$pickupDate $endTime") ?: return false
            java.util.Date().after(expiryDateTime)
        } catch (e: Exception) {
            false
        }
    }

    private fun isConfirmed(food: FoodPost): Boolean {
        return food.status.equals("confirmed", ignoreCase = true)
    }

    override fun onResume() {
        super.onResume()
        refreshCurrentMode()
    }
}