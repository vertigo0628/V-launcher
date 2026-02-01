package com.example.launcher.ui

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.launcher.R
import com.example.launcher.model.AppModel

class AppAdapter(
    private var apps: List<AppModel>,
    private val onAppClick: (AppModel) -> Unit
) : RecyclerView.Adapter<AppAdapter.AppViewHolder>() {

    class AppViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val icon: ImageView = view.findViewById(R.id.appIcon)
        val name: TextView = view.findViewById(R.id.appName)
        val badge: TextView = view.findViewById(R.id.appBadge)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AppViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_app, parent, false)
        return AppViewHolder(view)
    }

    override fun onBindViewHolder(holder: AppViewHolder, position: Int) {
        val app = apps[position]
        holder.name.text = app.label
        holder.icon.setImageDrawable(app.icon)
        
        // Show badge if there are notifications
        if (app.badgeCount > 0) {
            holder.badge.visibility = View.VISIBLE
            holder.badge.text = if (app.badgeCount > 99) "99+" else app.badgeCount.toString()
        } else {
            holder.badge.visibility = View.GONE
        }
        
        holder.itemView.setOnClickListener {
            onAppClick(app)
        }
        
        // Long-press menu
        holder.itemView.setOnLongClickListener {
            showAppContextMenu(holder.itemView, app)
            true
        }
    }

    private fun showAppContextMenu(view: View, app: AppModel) {
        val popup = android.widget.PopupMenu(view.context, view)
        popup.menu.add("App Info")
        popup.menu.add("Uninstall")
        popup.menu.add("Hide App")
        
        popup.setOnMenuItemClickListener { item ->
            when (item.title) {
                "App Info" -> {
                    val intent = android.content.Intent(android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                    intent.data = android.net.Uri.parse("package:${app.packageName}")
                    view.context.startActivity(intent)
                    true
                }
                "Uninstall" -> {
                    val intent = android.content.Intent(android.content.Intent.ACTION_DELETE)
                    intent.data = android.net.Uri.parse("package:${app.packageName}")
                    view.context.startActivity(intent)
                    true
                }
                "Hide App" -> {
                    val prefsManager = com.example.launcher.utils.PreferencesManager(view.context)
                    prefsManager.hideApp(app.packageName)
                    android.widget.Toast.makeText(view.context, "${app.label} hidden. Restart launcher to apply.", android.widget.Toast.LENGTH_SHORT).show()
                    true
                }
                else -> false
            }
        }
        popup.show()
    }

    override fun getItemCount() = apps.size
    
    fun updateApps(newApps: List<AppModel>) {
        apps = newApps
        notifyDataSetChanged()
    }
}
