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
    private lateinit var reservationList: MutableList<FoodPost>
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

                for (document in result) {
                    val food = document.toObject(FoodPost::class.java)
                    reservationList.add(food)
                }

                adapter.notifyDataSetChanged()
                tvReservationCount.text = reservationList.size.toString() + " reservations"
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