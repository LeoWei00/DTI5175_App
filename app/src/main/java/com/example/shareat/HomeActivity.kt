package com.example.shareat

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Geocoder
import android.location.Location
import android.os.Bundle
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
import com.google.firebase.firestore.FirebaseFirestore
import java.util.Locale
import kotlin.concurrent.thread

class HomeActivity : AppCompatActivity() {

    private lateinit var filterButton: ImageButton
    private lateinit var recyclerFoods: RecyclerView
    private lateinit var tvMealCount: TextView

    private lateinit var navPostContainer: LinearLayout
    private lateinit var navChatContainer: LinearLayout
    private lateinit var navProfileContainer: LinearLayout

    private lateinit var db: FirebaseFirestore
    private lateinit var nearbyFoodList: MutableList<NearbyFoodPost>
    private lateinit var adapter: FoodPostAdapter

    private val LOCATION_PERMISSION_CODE = 200
    private val MAX_DISTANCE_KM = 1.5

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_home)

        filterButton = findViewById(R.id.filterButton)
        recyclerFoods = findViewById(R.id.recyclerFoods)
        tvMealCount = findViewById(R.id.tvMealCount)

        navPostContainer = findViewById(R.id.navPostContainer)
        navChatContainer = findViewById(R.id.navChatContainer)
        navProfileContainer = findViewById(R.id.navProfileContainer)

        db = FirebaseFirestore.getInstance()
        nearbyFoodList = mutableListOf()

        adapter = FoodPostAdapter(nearbyFoodList)
        recyclerFoods.layoutManager = LinearLayoutManager(this)
        recyclerFoods.adapter = adapter

        filterButton.setOnClickListener {
            Toast.makeText(this, "Filter clicked", Toast.LENGTH_SHORT).show()
        }

        navPostContainer.setOnClickListener {
            startActivity(Intent(this, PostFoodActivity::class.java))
        }

        navChatContainer.setOnClickListener {
            startActivity(Intent(this, PostFoodActivity::class.java))
        }

        navProfileContainer.setOnClickListener {
            startActivity(Intent(this, PostFoodActivity::class.java))
        }

        checkLocationAndLoadFoods()
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

                        // Toast.makeText(this, "Docs returned: ${result.size()}", Toast.LENGTH_LONG).show()
                        // android.util.Log.d("HOME_DEBUG", "Docs returned: ${result.size()}")

                        thread {
                            val geocoder = Geocoder(this, Locale.getDefault())
                            val filteredFoods = mutableListOf<NearbyFoodPost>()

                            for (document in result) {
                                // android.util.Log.d("HOME_DEBUG", "Raw document: ${document.data}")

                                val food = document.toObject(FoodPost::class.java)
                                // android.util.Log.d("HOME_DEBUG", "Loaded food title: ${food.title}")
                                // android.util.Log.d("HOME_DEBUG", "Pickup location: ${food.pickup_location}")

                                if (food.pickup_location.isBlank()) {
                                    // android.util.Log.d("HOME_DEBUG", "Skipped because pickup location is blank")
                                    continue
                                }

                                try {
                                    val addresses = geocoder.getFromLocationName(food.pickup_location, 1)
                                    if (!addresses.isNullOrEmpty()) {
                                        val address = addresses[0]

                                        val postLocation = Location("postLocation").apply {
                                            latitude = address.latitude
                                            longitude = address.longitude
                                        }

                                        val distanceMeters = userLocation.distanceTo(postLocation)
                                        val distanceKm = distanceMeters / 1000.0

                                        // android.util.Log.d(
                                        //     "HOME_DEBUG",
                                        //     "Distance for ${food.title}: $distanceKm km"
                                        // )

                                        if (distanceKm <= MAX_DISTANCE_KM) {
                                            filteredFoods.add(NearbyFoodPost(food, distanceKm))
                                            // android.util.Log.d("HOME_DEBUG", "Added ${food.title}")
                                        } else {
                                            // android.util.Log.d("HOME_DEBUG", "Filtered out ${food.title}")
                                        }
                                    } else {
                                        // android.util.Log.d(
                                        //     "HOME_DEBUG",
                                        //     "Geocoder found no address for: ${food.pickup_location}"
                                        // )
                                    }
                                } catch (e: Exception) {
                                    // android.util.Log.e(
                                    //     "HOME_DEBUG",
                                    //     "Geocoding failed for ${food.pickup_location}",
                                    //     e
                                    // )
                                }
                            }

                            filteredFoods.sortBy { it.distanceKm }

                            runOnUiThread {
                                nearbyFoodList.clear()
                                nearbyFoodList.addAll(filteredFoods)
                                adapter.notifyDataSetChanged()
                                tvMealCount.text = "${nearbyFoodList.size} meals"
                            }
                        }
                    }
                    .addOnFailureListener { e ->
                        // android.util.Log.e("HOME_DEBUG", "Firestore error", e)

                        Toast.makeText(
                            this,
                            "Error loading foods: ${e.message}",
                            Toast.LENGTH_LONG
                        ).show()
                    }
            }
            .addOnFailureListener { e ->
                // android.util.Log.e("HOME_DEBUG", "Location error", e)

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
                loadNearbyFoods()
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
        checkLocationAndLoadFoods()
    }
}