package com.vertigo.launcher.ui

import android.app.Dialog
import android.content.Context
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.view.Window
import android.widget.*
import com.vertigo.launcher.R
import com.vertigo.launcher.utils.FolderManager

/**
 * FolderDialog - Dialog for viewing and editing folder contents
 */
class FolderDialog(
    context: Context,
    private val folder: FolderManager.Folder,
    private val folderManager: FolderManager,
    private val onAppClick: (String) -> Unit,
    private val onFolderUpdated: () -> Unit
) : Dialog(context) {

    private lateinit var titleEdit: EditText
    private lateinit var appsGrid: GridLayout
    private lateinit var colorPicker: LinearLayout
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requestWindowFeature(Window.FEATURE_NO_TITLE)
        
        val container = createDialogLayout()
        setContentView(container)
        
        window?.let { w ->
            w.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
            w.setLayout(
                (context.resources.displayMetrics.widthPixels * 0.9).toInt(),
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
        }
        
        populateApps()
    }
    
    private fun createDialogLayout(): LinearLayout {
        return LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(32, 32, 32, 32)
            setBackgroundColor(0xF0121214.toInt())
            
            // Title row
            val titleRow = LinearLayout(context).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
            }
            
            titleEdit = EditText(context).apply {
                setText(folder.name)
                setTextColor(0xFFFFFFFF.toInt())
                textSize = 20f
                setBackgroundColor(Color.TRANSPARENT)
                setPadding(0, 8, 8, 8)
                layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
                
                setOnFocusChangeListener { _, hasFocus ->
                    if (!hasFocus) {
                        folderManager.updateFolder(folder.id, name = text.toString())
                        onFolderUpdated()
                    }
                }
            }
            titleRow.addView(titleEdit)
            
            // Delete button
            val deleteBtn = ImageView(context).apply {
                setImageResource(android.R.drawable.ic_menu_delete)
                setColorFilter(0xFFFF006E.toInt())
                setPadding(16, 16, 16, 16)
                setOnClickListener {
                    folderManager.deleteFolder(folder.id)
                    onFolderUpdated()
                    dismiss()
                }
            }
            titleRow.addView(deleteBtn)
            
            addView(titleRow, LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            ).apply { setMargins(0, 0, 0, 16) })
            
            // Color picker
            colorPicker = LinearLayout(context).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER
            }
            
            val colors = listOf(
                0xFF00F0FF.toInt(), // Cyan
                0xFFFF006E.toInt(), // Pink
                0xFFBF00FF.toInt(), // Purple
                0xFF00FF88.toInt(), // Green
                0xFFFFAA00.toInt(), // Orange
                0xFFFFFFFF.toInt()  // White
            )
            
            colors.forEach { color ->
                val colorView = View(context).apply {
                    setBackgroundColor(color)
                    layoutParams = LinearLayout.LayoutParams(48, 48).apply {
                        setMargins(8, 0, 8, 0)
                    }
                    
                    if (color == folder.iconColor) {
                        alpha = 1f
                        scaleX = 1.2f
                        scaleY = 1.2f
                    } else {
                        alpha = 0.6f
                    }
                    
                    setOnClickListener {
                        folderManager.updateFolder(folder.id, color = color)
                        folder.iconColor = color
                        onFolderUpdated()
                        updateColorSelection(this, color)
                    }
                }
                colorPicker.addView(colorView)
            }
            
            addView(colorPicker, LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            ).apply { setMargins(0, 0, 0, 24) })
            
            // Apps grid
            appsGrid = GridLayout(context).apply {
                columnCount = folder.gridSize
                layoutParams = LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
                )
            }
            
            addView(appsGrid)
        }
    }
    
    private fun updateColorSelection(selected: View, color: Int) {
        for (i in 0 until colorPicker.childCount) {
            val child = colorPicker.getChildAt(i)
            if (child == selected) {
                child.alpha = 1f
                child.scaleX = 1.2f
                child.scaleY = 1.2f
            } else {
                child.alpha = 0.6f
                child.scaleX = 1f
                child.scaleY = 1f
            }
        }
    }
    
    private fun populateApps() {
        appsGrid.removeAllViews()
        
        val pm = context.packageManager
        val iconSize = 64
        
        folder.apps.forEach { packageName ->
            try {
                val appInfo = pm.getApplicationInfo(packageName, 0)
                val icon = pm.getApplicationIcon(appInfo)
                val label = pm.getApplicationLabel(appInfo).toString()
                
                val appView = LinearLayout(context).apply {
                    orientation = LinearLayout.VERTICAL
                    gravity = Gravity.CENTER
                    setPadding(16, 16, 16, 16)
                    
                    val iconView = ImageView(context).apply {
                        setImageDrawable(icon)
                        layoutParams = LinearLayout.LayoutParams(iconSize, iconSize)
                    }
                    addView(iconView)
                    
                    val labelView = TextView(context).apply {
                        text = label
                        setTextColor(0xFFFFFFFF.toInt())
                        textSize = 11f
                        maxLines = 1
                        ellipsize = android.text.TextUtils.TruncateAt.END
                        gravity = Gravity.CENTER
                    }
                    addView(labelView)
                    
                    setOnClickListener {
                        onAppClick(packageName)
                        dismiss()
                    }
                    
                    setOnLongClickListener {
                        showAppOptions(packageName)
                        true
                    }
                }
                
                val params = GridLayout.LayoutParams().apply {
                    width = 0
                    height = ViewGroup.LayoutParams.WRAP_CONTENT
                    columnSpec = GridLayout.spec(GridLayout.UNDEFINED, 1f)
                }
                appsGrid.addView(appView, params)
                
            } catch (e: Exception) {
                // App not found, skip
            }
        }
    }
    
    private fun showAppOptions(packageName: String) {
        val popup = PopupMenu(context, appsGrid)
        popup.menu.add("Remove from Folder")
        
        popup.setOnMenuItemClickListener { item ->
            when (item.title) {
                "Remove from Folder" -> {
                    folderManager.removeAppFromFolder(folder.id, packageName)
                    folder.apps.remove(packageName)
                    
                    if (folder.apps.size <= 1) {
                        onFolderUpdated()
                        dismiss()
                    } else {
                        populateApps()
                        onFolderUpdated()
                    }
                }
            }
            true
        }
        popup.show()
    }
}
