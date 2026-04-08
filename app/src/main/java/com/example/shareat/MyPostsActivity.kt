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

class MyPostsActivity : AppCompatActivity() {

    private lateinit var recyclerMyPosts: RecyclerView
    private lateinit var tvMyPostsCount: TextView
    private lateinit var btnBack: ImageButton

    private lateinit var db: FirebaseFirestore
    private lateinit var myPostsList: MutableList<NearbyFoodPost>
    private lateinit var adapter: FoodPostAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_my_posts)

        recyclerMyPosts = findViewById(R.id.recyclerMyPosts)
        tvMyPostsCount = findViewById(R.id.tvMyPostsCount)
        btnBack = findViewById(R.id.btnBack)

        db = FirebaseFirestore.getInstance()
        myPostsList = mutableListOf()

        adapter = FoodPostAdapter(myPostsList)
        recyclerMyPosts.layoutManager = LinearLayoutManager(this)
        recyclerMyPosts.adapter = adapter

        btnBack.setOnClickListener {
            finish()
        }

        loadMyPosts()
    }

    private fun loadMyPosts() {
        val currentUserId = FirebaseAuth.getInstance().currentUser?.uid ?: return

        db.collection("foods")
            .whereEqualTo("owner_id", currentUserId)
            .get()
            .addOnSuccessListener { result ->
                myPostsList.clear()

                for (document in result) {
                    val food = document.toObject(FoodPost::class.java)
                    myPostsList.add(NearbyFoodPost(food, -1.0))
                }

                adapter.notifyDataSetChanged()
                tvMyPostsCount.text = "${myPostsList.size} meals"
            }
    }
}