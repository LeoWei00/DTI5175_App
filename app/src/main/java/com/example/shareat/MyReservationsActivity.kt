package com.example.shareat

import android.os.Bundle
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class MyReservationsActivity : AppCompatActivity() {

    private lateinit var recyclerReservations: RecyclerView
    private lateinit var tvReservationCount: TextView
    private lateinit var btnBack: ImageButton

    private lateinit var db: FirebaseFirestore
    private lateinit var reservationList: MutableList<NearbyFoodPost>
    private lateinit var adapter: FoodPostAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_my_reservations)

        recyclerReservations = findViewById(R.id.recyclerReservations)
        tvReservationCount = findViewById(R.id.tvReservationCount)
        btnBack = findViewById(R.id.btnBack)

        db = FirebaseFirestore.getInstance()
        reservationList = mutableListOf()

        adapter = FoodPostAdapter(reservationList)
        recyclerReservations.layoutManager = LinearLayoutManager(this)
        recyclerReservations.adapter = adapter

        btnBack.setOnClickListener {
            finish()
        }

        loadReservations()
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

    private fun finishReservationRefresh(expiredRemovedCount: Int) {
        adapter.notifyDataSetChanged()
        tvReservationCount.text = "${reservationList.size} reservations"

        if (expiredRemovedCount > 0) {
            Toast.makeText(
                this,
                "$expiredRemovedCount expired reservation(s) were removed.",
                Toast.LENGTH_LONG
            ).show()
        }
    }

    private fun loadReservations() {
        val currentUser = FirebaseAuth.getInstance().currentUser
        if (currentUser == null) {
            Toast.makeText(this, "You must be logged in", Toast.LENGTH_LONG).show()
            return
        }

        db.collection("foods")
            .whereEqualTo("reserved_by", currentUser.uid)
            .get()
            .addOnSuccessListener { result ->
                reservationList.clear()

                var expiredRemovedCount = 0
                var processedCount = 0
                val totalCount = result.size()

                if (totalCount == 0) {
                    adapter.notifyDataSetChanged()
                    tvReservationCount.text = "0 reservations"
                    return@addOnSuccessListener
                }

                for (document in result) {
                    val food = document.toObject(FoodPost::class.java)

                    if (isExpired(food.pickup_date, food.end_time)) {
                        document.reference.update(
                            mapOf(
                                "status" to "available",
                                "reserved_by" to "",
                                "reserved_by_name" to "",
                                "reserved_at" to 0L,
                                "updated_at" to System.currentTimeMillis()
                            )
                        ).addOnCompleteListener {
                            expiredRemovedCount++
                            processedCount++

                            if (processedCount == totalCount) {
                                finishReservationRefresh(expiredRemovedCount)
                            }
                        }
                    } else {
                        reservationList.add(NearbyFoodPost(food, -1.0))
                        processedCount++

                        if (processedCount == totalCount) {
                            finishReservationRefresh(expiredRemovedCount)
                        }
                    }
                }
            }
            .addOnFailureListener { e ->
                Toast.makeText(
                    this,
                    "Error loading reservations: ${e.message}",
                    Toast.LENGTH_LONG
                ).show()
            }
    }
}