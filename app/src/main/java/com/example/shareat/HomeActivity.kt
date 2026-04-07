package com.example.shareat

import android.content.Intent
import android.os.Bundle
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.firebase.firestore.FirebaseFirestore

class HomeActivity : AppCompatActivity() {

    private lateinit var filterButton: ImageButton
    private lateinit var recyclerFoods: RecyclerView
    private lateinit var tvMealCount: TextView
    private lateinit var fabReservations: FloatingActionButton

    private lateinit var navHomeContainer: LinearLayout
    private lateinit var navPostContainer: LinearLayout
    private lateinit var navChatContainer: LinearLayout
    private lateinit var navProfileContainer: LinearLayout

    private lateinit var db: FirebaseFirestore
    private lateinit var foodList: MutableList<FoodPost>
    private lateinit var adapter: FoodPostAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_home)

        filterButton = findViewById(R.id.filterButton)
        recyclerFoods = findViewById(R.id.recyclerFoods)
        tvMealCount = findViewById(R.id.tvMealCount)
        fabReservations = findViewById(R.id.fabReservations)

        navHomeContainer = findViewById(R.id.navHomeContainer)
        navPostContainer = findViewById(R.id.navPostContainer)
        navChatContainer = findViewById(R.id.navChatContainer)
        navProfileContainer = findViewById(R.id.navProfileContainer)

        db = FirebaseFirestore.getInstance()
        foodList = mutableListOf()

        adapter = FoodPostAdapter(foodList)
        recyclerFoods.layoutManager = LinearLayoutManager(this)
        recyclerFoods.adapter = adapter

        filterButton.setOnClickListener {
            Toast.makeText(this, "Filter clicked", Toast.LENGTH_SHORT).show()
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
            startActivity(Intent(this, PostFoodActivity::class.java))
        }

        navProfileContainer.setOnClickListener {
            startActivity(Intent(this, ProfileActivity::class.java))
        }

        loadFoods()
    }

    private fun loadFoods() {
        db.collection("foods")
            .get()
            .addOnSuccessListener { result ->
                foodList.clear()

                for (document in result) {
                    val food = document.toObject(FoodPost::class.java)
                    foodList.add(food)
                }

                adapter.notifyDataSetChanged()
                tvMealCount.text = foodList.size.toString() + " meals"
            }
            .addOnFailureListener { e ->
                Toast.makeText(
                    this,
                    "Error loading foods: ${e.message}",
                    Toast.LENGTH_LONG
                ).show()
            }
    }
}