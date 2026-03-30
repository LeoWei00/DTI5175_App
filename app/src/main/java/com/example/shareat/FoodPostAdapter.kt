package com.example.shareat

import android.content.Intent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide

class FoodPostAdapter(
    private val foodList: List<NearbyFoodPost>
) : RecyclerView.Adapter<FoodPostAdapter.FoodViewHolder>() {

    class FoodViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val title: TextView = itemView.findViewById(R.id.tvTitle)
        val description: TextView = itemView.findViewById(R.id.tvDescription)
        val category: TextView = itemView.findViewById(R.id.tvCategory)
        val meta: TextView = itemView.findViewById(R.id.tvMeta)
        val user: TextView = itemView.findViewById(R.id.tvUser)
        val image: ImageView = itemView.findViewById(R.id.imgFood)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FoodViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_food_post, parent, false)
        return FoodViewHolder(view)
    }

    override fun onBindViewHolder(holder: FoodViewHolder, position: Int) {
        val nearbyFood = foodList[position]
        val food = nearbyFood.post

        holder.title.text = food.title
        holder.description.text = food.description
        holder.category.text = food.category

        if (nearbyFood.distanceKm >= 0) {
            holder.meta.text = String.format(
                "%.2f km away • %s • %s-%s",
                nearbyFood.distanceKm,
                food.pickup_date,
                food.start_time,
                food.end_time
            )
        } else {
            holder.meta.text = "${food.pickup_date} • ${food.start_time}-${food.end_time}"
        }

        holder.user.text = if (food.owner_name.isNotEmpty()) {
            food.owner_name
        } else {
            food.owner_id
        }

        if (food.image_url.isNotEmpty()) {
            Glide.with(holder.itemView.context)
                .load(food.image_url)
                .placeholder(R.drawable.shareat_placeholder)
                .into(holder.image)
        } else {
            holder.image.setImageResource(R.drawable.shareat_placeholder)
        }

        holder.itemView.setOnClickListener {
            val context = holder.itemView.context
            val intent = Intent(context, FoodDetailActivity::class.java)

            intent.putExtra("FOOD_NAME", food.title)
            intent.putExtra("USER_NAME", if (food.owner_name.isNotEmpty()) food.owner_name else food.owner_id)
            intent.putExtra("CATEGORY", food.category)
            intent.putExtra("DESCRIPTION", food.description)
            intent.putExtra("INGREDIENTS", food.ingredients)
            intent.putExtra("QUANTITY", food.quantity.toString())
            intent.putExtra("UNIT", food.unit)
            intent.putExtra("ALLERGENS", food.allergen_information.joinToString(", "))
            intent.putExtra("DIETS", food.dietary_labels.joinToString(", "))
            intent.putExtra("LOCATION", food.pickup_location)
            intent.putExtra("PICKUP_DATE", food.pickup_date)
            intent.putExtra("START_TIME", food.start_time)
            intent.putExtra("END_TIME", food.end_time)
            intent.putExtra("TYPE", food.post_type)
            intent.putExtra("IMAGE_URL", food.image_url)

            context.startActivity(intent)
        }
    }

    override fun getItemCount(): Int {
        return foodList.size
    }
}