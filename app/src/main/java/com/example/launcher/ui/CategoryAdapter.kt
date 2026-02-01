package com.example.launcher.ui

import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.recyclerview.widget.RecyclerView
import com.example.launcher.R
import com.example.launcher.model.AppCategory

class CategoryAdapter(
    private val onCategoryClick: (AppCategory?) -> Unit
) : RecyclerView.Adapter<CategoryAdapter.CategoryViewHolder>() {

    private val categories = listOf(
        null, // Represents "All Apps" (Home)
        AppCategory.COMMUNICATION,
        AppCategory.INTERNET,
        AppCategory.GAMES,
        AppCategory.MEDIA,
        AppCategory.UTILITIES,
        AppCategory.SETTINGS
    )
    
    private var selectedCategory: AppCategory? = null

    class CategoryViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val icon: ImageView = view.findViewById(R.id.categoryIcon)
        val background: View = view.findViewById(R.id.categoryBackground)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CategoryViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_category, parent, false)
        return CategoryViewHolder(view)
    }

    override fun onBindViewHolder(holder: CategoryViewHolder, position: Int) {
        val category = categories[position]
        
        // Custom vector drawable icons for categories
        val iconRes = when (category) {
            null -> R.drawable.ic_category_home
            AppCategory.COMMUNICATION -> R.drawable.ic_category_communication
            AppCategory.INTERNET -> R.drawable.ic_category_internet
            AppCategory.GAMES -> R.drawable.ic_category_games
            AppCategory.MEDIA -> R.drawable.ic_category_media
            AppCategory.UTILITIES -> R.drawable.ic_category_utilities
            AppCategory.SETTINGS -> R.drawable.ic_category_settings
            else -> R.drawable.ic_category_home
        }
        
        holder.icon.setImageResource(iconRes)
        
        val isSelected = category == selectedCategory
        holder.background.setBackgroundColor(if (isSelected) Color.LTGRAY else Color.TRANSPARENT)
        
        holder.itemView.setOnClickListener {
            selectedCategory = category
            notifyDataSetChanged()
            onCategoryClick(category)
        }
    }

    override fun getItemCount() = categories.size
}
