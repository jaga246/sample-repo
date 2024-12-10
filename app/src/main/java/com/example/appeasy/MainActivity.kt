package com.example.appeasy

import android.annotation.SuppressLint
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.graphics.Color
import android.graphics.Path
import android.graphics.drawable.ColorDrawable
import android.view.Gravity
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.widget.*
import androidx.appcompat.app.AlertDialog
import yuku.ambilwarna.AmbilWarnaDialog
import android.graphics.drawable.GradientDrawable
import android.content.SharedPreferences
import android.graphics.drawable.ShapeDrawable
import android.graphics.drawable.shapes.PathShape
import android.app.DownloadManager
import android.content.Context
import android.net.Uri
import android.os.Environment
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream

class MainActivity : AppCompatActivity() {

    private lateinit var modifiableButton: Button
    private lateinit var resizeHandles: Array<View>
    private lateinit var mainLayout: FrameLayout
    private var isEditMode = false // Control editing mode
    private lateinit var preferences: SharedPreferences
    private var xDelta = 0f
    private var yDelta = 0f

    private var alignTop = false
    private var alignBottom = false
    private var alignLeft = false
    private var alignRight = false

    @SuppressLint("ClickableViewAccessibility")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val editToggleButton = findViewById<ToggleButton>(R.id.editToggleButton)
        val saveButton = findViewById<Button>(R.id.saveButton)
        val downloadButton : Button = findViewById(R.id.downloadAppButton)
        mainLayout = findViewById(R.id.mainLayout)

        preferences = getSharedPreferences("ButtonPrefs", MODE_PRIVATE)

        // Create a button programmatically
        modifiableButton = Button(this).apply {
            text = "Button"
            setBackgroundColor(Color.parseColor("#FF6200EE"))
            layoutParams = FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.WRAP_CONTENT,
                FrameLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                leftMargin = 200
                topMargin = 200
            }
        }

        mainLayout.addView(modifiableButton)
        restoreButtonProperties()

        val handles = Array(8) { View(this) }
        handles.forEach { handle ->
            handle.setBackgroundColor(Color.DKGRAY)
            handle.layoutParams = FrameLayout.LayoutParams(65, 65)
            mainLayout.addView(handle)
        }

        resizeHandles = handles
        updateHandlesPosition()

        editToggleButton.setOnCheckedChangeListener { _, isChecked ->
            isEditMode = isChecked
            if (isEditMode) {
                Toast.makeText(this, "Edit mode enabled", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this, "Edit mode disabled", Toast.LENGTH_SHORT).show()
            }
        }

        saveButton.setOnClickListener {
            saveButtonProperties()
            Toast.makeText(this, "Changes saved", Toast.LENGTH_SHORT).show()
        }

        var lastClickTime: Long = 0

        modifiableButton.setOnTouchListener { view, motionEvent ->
            if (!isEditMode) {
                return@setOnTouchListener false // Ignore touch events when not in edit mode
            }
            when (motionEvent.action) {
                MotionEvent.ACTION_DOWN -> {
                    xDelta = view.x - motionEvent.rawX
                    yDelta = view.y - motionEvent.rawY

                    val clickTime = System.currentTimeMillis()
                    if (clickTime - lastClickTime < 300) { // Detect double-click (within 300 ms)
                        showFeaturePopup(view)
                    }
                    lastClickTime = clickTime
                }
                MotionEvent.ACTION_MOVE -> {
                    view.animate()
                        .x(motionEvent.rawX + xDelta)
                        .y(motionEvent.rawY + yDelta)
                        .setDuration(0)
                        .start()
                    updateHandlesPosition()
                }
                MotionEvent.ACTION_UP -> {
                    view.performClick()
                }
            }
            false
        }

        setupResizeFeature()


        downloadButton.setOnClickListener{
            buildAndDownload()
        }

    }

    private fun saveButtonProperties() {
        val preferences = getSharedPreferences("ButtonPrefs", MODE_PRIVATE)
        val editor = preferences.edit()

        // Save button text
        editor.putString("buttonText", modifiableButton.text.toString())

        // Save button color
        val backgroundColor = (modifiableButton.background as? GradientDrawable)?.color?.defaultColor ?: Color.WHITE
        editor.putInt("buttonColor", backgroundColor)

        // Save button corner radii
        val cornerRadii = (modifiableButton.background as? GradientDrawable)?.cornerRadii ?: FloatArray(8)
        editor.putFloat("topLeftRadius", cornerRadii.getOrNull(0) ?: 0f)
        editor.putFloat("topRightRadius", cornerRadii.getOrNull(2) ?: 0f)
        editor.putFloat("bottomRightRadius", cornerRadii.getOrNull(4) ?: 0f)
        editor.putFloat("bottomLeftRadius", cornerRadii.getOrNull(6) ?: 0f)

        // Save button position
        val layoutParams = modifiableButton.layoutParams as FrameLayout.LayoutParams
        editor.putInt("buttonWidth", modifiableButton.width)
        editor.putInt("buttonHeight", modifiableButton.height)
        editor.putInt("buttonLeftMargin", layoutParams.leftMargin)
        editor.putInt("buttonTopMargin", layoutParams.topMargin)
        editor.putInt("buttonBottomMargin",layoutParams.bottomMargin)
        editor.putInt("buttonRightMargin",layoutParams.rightMargin)
        editor.putInt("buttonGravity",layoutParams.gravity)

        editor.putBoolean("alignTop", alignTop)
        editor.putBoolean("alignBottom", alignBottom)
        editor.putBoolean("alignLeft", alignLeft)
        editor.putBoolean("alignRight", alignRight)

        editor.apply()

    }

    private fun restoreButtonProperties() {
        val preferences = getSharedPreferences("ButtonPrefs", MODE_PRIVATE)

        // Restore button text
        val buttonText = preferences.getString("buttonText", "Button")
        modifiableButton.text = buttonText

        // Restore button color
        val buttonColor = preferences.getInt("buttonColor", Color.CYAN)
        val buttonBackground = GradientDrawable().apply {
            setColor(buttonColor)
        }

        // Restore button corner radii
        val topLeftRadius = preferences.getFloat("topLeftRadius", 0f)
        val topRightRadius = preferences.getFloat("topRightRadius", 0f)
        val bottomRightRadius = preferences.getFloat("bottomRightRadius", 0f)
        val bottomLeftRadius = preferences.getFloat("bottomLeftRadius", 0f)

        buttonBackground.cornerRadii = floatArrayOf(
            topLeftRadius, topLeftRadius,    // Top-Left
            topRightRadius, topRightRadius, // Top-Right
            bottomRightRadius, bottomRightRadius, // Bottom-Right
            bottomLeftRadius, bottomLeftRadius    // Bottom-Left
        )
        modifiableButton.background = buttonBackground


        val width = preferences.getInt("buttonWidth", FrameLayout.LayoutParams.WRAP_CONTENT)
        val height = preferences.getInt("buttonHeight", FrameLayout.LayoutParams.WRAP_CONTENT)
        val leftMargin = preferences.getInt("buttonLeftMargin", 200)
        val topMargin = preferences.getInt("buttonTopMargin", 200)
        val bottomMargin = preferences.getInt("buttonBottomMargin",200)
        val rightMargin = preferences.getInt("buttonRightMargin",200)
        val gravity = preferences.getInt("buttonGravity",Gravity.CENTER)

        val layoutParams = FrameLayout.LayoutParams(width, height).apply {
            this.leftMargin = leftMargin
            this.topMargin = topMargin
            this.bottomMargin = bottomMargin
            this.rightMargin = rightMargin
            this.gravity = gravity
        }
        modifiableButton.layoutParams = layoutParams

        alignTop = preferences.getBoolean("alignTop", false)
        alignBottom = preferences.getBoolean("alignBottom", false)
        alignLeft = preferences.getBoolean("alignLeft", false)
        alignRight = preferences.getBoolean("alignRight", false)

        applyConstraints()

    }


    @SuppressLint("ClickableViewAccessibility")
    private fun setupResizeFeature() {
        resizeHandles.forEachIndexed { index, handle ->
            handle.setOnTouchListener { _, event ->

                if (!isEditMode) {
                    return@setOnTouchListener false // Ignore touch events when not in edit mode
                }

                when (event.action) {
                    MotionEvent.ACTION_MOVE -> {
                        val parent = modifiableButton.parent as FrameLayout
                        val layoutParams = modifiableButton.layoutParams as FrameLayout.LayoutParams

                        // Adjust based on handle position
                        when (index) {
                            0 -> { // Top-Left
                                layoutParams.leftMargin += (event.rawX - modifiableButton.x).toInt()
                                layoutParams.topMargin += (event.rawY - modifiableButton.y).toInt()
                                layoutParams.width -= (event.rawX - modifiableButton.x).toInt()
                                layoutParams.height -= (event.rawY - modifiableButton.y).toInt()
                            }
                            1 -> { // Top
                                layoutParams.topMargin += (event.rawY - modifiableButton.y).toInt()
                                layoutParams.height -= (event.rawY - modifiableButton.y).toInt()
                            }
                            2 -> { // Top-Right
                                layoutParams.width = (event.rawX - modifiableButton.x).toInt()
                                layoutParams.topMargin += (event.rawY - modifiableButton.y).toInt()
                                layoutParams.height -= (event.rawY - modifiableButton.y).toInt()
                            }
                            3 -> { // Right
                                layoutParams.width = (event.rawX - modifiableButton.x).toInt()
                            }
                            4 -> { // Bottom-Right
                                layoutParams.width = (event.rawX - modifiableButton.x).toInt()
                                layoutParams.height = (event.rawY - modifiableButton.y).toInt()
                            }
                            5 -> { // Bottom
                                layoutParams.height = (event.rawY - modifiableButton.y).toInt()
                            }
                            6 -> { // Bottom-Left
                                layoutParams.leftMargin += (event.rawX - modifiableButton.x).toInt()
                                layoutParams.width -= (event.rawX - modifiableButton.x).toInt()
                                layoutParams.height = (event.rawY - modifiableButton.y).toInt()
                            }
                            7 -> { // Left
                                layoutParams.leftMargin += (event.rawX - modifiableButton.x).toInt()
                                layoutParams.width -= (event.rawX - modifiableButton.x).toInt()
                            }
                        }

                        // Update button layout
                        if (layoutParams.width > 100 && layoutParams.height > 50) { // Minimum size
                            modifiableButton.layoutParams = layoutParams
                        }
                        updateHandlesPosition()
                    }
                }
                true
            }
        }
    }

    private fun updateHandlesPosition() {
        val buttonX = modifiableButton.x
        val buttonY = modifiableButton.y
        val buttonWidth = modifiableButton.width
        val buttonHeight = modifiableButton.height

        // Update handles based on button position
        val positions = arrayOf(
            Pair(buttonX - 10, buttonY - 10), // Top-Left
            Pair(buttonX + buttonWidth / 2 - 25, buttonY - 25), // Top
            Pair(buttonX + buttonWidth - 10, buttonY - 10), // Top-Right
            Pair(buttonX + buttonWidth - 15, buttonY + buttonHeight / 2 - 15), // Right
            Pair(buttonX + buttonWidth - 10, buttonY + buttonHeight - 10), // Bottom-Right
            Pair(buttonX + buttonWidth / 2 - 10, buttonY + buttonHeight - 10), // Bottom
            Pair(buttonX - 10, buttonY + buttonHeight - 10), // Bottom-Left
            Pair(buttonX - 25, buttonY + buttonHeight / 2 - 25) // Left
        )

        resizeHandles.forEachIndexed { index, handle ->
            handle.x = positions[index].first
            handle.y = positions[index].second
        }
    }

    private fun showFeaturePopup(anchorView: View) {
        // Inflate the custom popup layout
        val inflater = LayoutInflater.from(this)
        val popupView = inflater.inflate(R.layout.popup_features, null)

        // Create the PopupWindow
        val popupWindow = PopupWindow(
            popupView,
            LinearLayout.LayoutParams.WRAP_CONTENT,
            LinearLayout.LayoutParams.WRAP_CONTENT,
            true
        )

        // Handle feature item clicks
        popupView.findViewById<TextView>(R.id.feature_color).setOnClickListener {
            popupWindow.dismiss()
            openColorPicker()
        }

        popupView.findViewById<TextView>(R.id.feature_radius).setOnClickListener {
            popupWindow.dismiss()
            changeRadius()
        }

        popupView.findViewById<TextView>(R.id.feature_text).setOnClickListener {
            popupWindow.dismiss()
            changeText()
        }
        popupView.findViewById<TextView>(R.id.feature_constraint).setOnClickListener {
            popupWindow.dismiss()
            showConstraintPopup()
        }

        // Show the popup window next to the button
        popupWindow.showAsDropDown(anchorView, 10, 10, Gravity.START)
    }

    private fun openColorPicker() {
        // Extract initial color from the button background
        val initialColor = when (val background = modifiableButton.background) {
            is ColorDrawable -> background.color
            is GradientDrawable -> {
                // Try to extract the fill color from the GradientDrawable
                val colorStateList = background.color
                colorStateList?.defaultColor ?: Color.WHITE // Default to white if color is unavailable
            }
            else -> Color.WHITE // Default to white for unsupported background types
        }

        val colorPicker = AmbilWarnaDialog(this, initialColor, object : AmbilWarnaDialog.OnAmbilWarnaListener {
            override fun onOk(dialog: AmbilWarnaDialog?, color: Int) {
                // Retain the current shape, and update the color only
                val currentBackground = modifiableButton.background

                // If background is GradientDrawable, we update the color
                if (currentBackground is GradientDrawable) {
                    currentBackground.setColor(color)
                } else {
                    // If it's not a GradientDrawable, we need to create one
                    val newBackground = GradientDrawable().apply {
                        setColor(color)
                        cornerRadius = 0f // Retain default radius (can be adjusted)
                    }
                    modifiableButton.background = newBackground
                }
            }

            override fun onCancel(dialog: AmbilWarnaDialog?) {
                // Do nothing if the user cancels
            }
        })
        colorPicker.show()
    }
    private fun changeText() {
        val builder = AlertDialog.Builder(this)
        builder.setTitle("Edit Button Text")

        // Create an input field for button text
        val input = EditText(this).apply {
            hint = "Enter Button Text"
        }
        builder.setView(input)

        // Buttons to save or cancel
        builder.setPositiveButton("Save") { _, _ ->
            val newText = input.text.toString()
            if (newText.isNotEmpty()) {
                modifiableButton.text = newText
            } else {
                Toast.makeText(this, "Text cannot be empty!", Toast.LENGTH_SHORT).show()
            }
        }
        builder.setNegativeButton("Cancel") { dialog, _ -> dialog.cancel() }

        builder.show()
    }
    private var buttonBackground: GradientDrawable? = null
    private var topLeftRadius: Float = 0f
    private var topRightRadius: Float = 0f
    private var bottomLeftRadius: Float = 0f
    private var bottomRightRadius: Float = 0f
    private fun changeRadius() {
        // Retrieve the current background and update stored radius values if they exist
        val currentBackground = modifiableButton.background as? GradientDrawable
        if (currentBackground != null) {
            val cornerRadii = currentBackground.cornerRadii
            // Ensure the cornerRadii array has values to avoid index out of bounds
            if (cornerRadii != null && cornerRadii.size >= 8) {
                // Store current values
                topLeftRadius = cornerRadii[0]  // top-left
                topRightRadius = cornerRadii[2] // top-right
                bottomRightRadius = cornerRadii[4] // bottom-right
                bottomLeftRadius = cornerRadii[6] // bottom-left
            }
        }

        // Build the dialog to allow radius input
        val builder = AlertDialog.Builder(this)
        builder.setTitle("Set Corner Radius")

        // Create input fields for each corner radius
        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(16, 16, 16, 16)
        }

        val topLeftInput = EditText(this).apply {
            hint = "Top-Left Radius (px)"
            setText(topLeftRadius.toString())  // Set current value as default
        }
        val topRightInput = EditText(this).apply {
            hint = "Top-Right Radius (px)"
            setText(topRightRadius.toString())  // Set current value as default
        }
        val bottomLeftInput = EditText(this).apply {
            hint = "Bottom-Left Radius (px)"
            setText(bottomLeftRadius.toString())  // Set current value as default
        }
        val bottomRightInput = EditText(this).apply {
            hint = "Bottom-Right Radius (px)"
            setText(bottomRightRadius.toString())  // Set current value as default
        }

        layout.addView(topLeftInput)
        layout.addView(topRightInput)
        layout.addView(bottomLeftInput)
        layout.addView(bottomRightInput)

        builder.setView(layout)

        builder.setPositiveButton("Apply") { _, _ ->
            try {
                // Get the values from the EditText inputs
                topLeftRadius = topLeftInput.text.toString().toFloatOrNull() ?: 0f
                topRightRadius = topRightInput.text.toString().toFloatOrNull() ?: 0f
                bottomLeftRadius = bottomLeftInput.text.toString().toFloatOrNull() ?: 0f
                bottomRightRadius = bottomRightInput.text.toString().toFloatOrNull() ?: 0f

                // Initialize or update the GradientDrawable
                if (buttonBackground == null) {
                    buttonBackground = GradientDrawable().apply {
                        setColor((modifiableButton.background as? ColorDrawable)?.color ?: Color.WHITE) // Default to white if no color
                    }
                }

                // Set corner radii in correct order: top-left, top-right, bottom-right, bottom-left
                buttonBackground?.cornerRadii = floatArrayOf(
                    topLeftRadius, topLeftRadius,      // Top-Left
                    topRightRadius, topRightRadius,    // Top-Right
                    bottomRightRadius, bottomRightRadius, // Bottom-Right
                    bottomLeftRadius, bottomLeftRadius    // Bottom-Left
                )

                // Apply the updated drawable to the button
                modifiableButton.background = buttonBackground
            } catch (e: Exception) {
                Toast.makeText(this, "Invalid input. Please enter valid numbers.", Toast.LENGTH_SHORT).show()
            }
        }

        builder.setNegativeButton("Cancel") { dialog, _ -> dialog.cancel() }

        builder.show()
    }

    private fun applyConstraints() {
        val layoutParams = modifiableButton.layoutParams as FrameLayout.LayoutParams

        if (alignTop) layoutParams.topMargin = 0
        if (alignBottom) layoutParams.topMargin = mainLayout.height - modifiableButton.height
        if (alignLeft) layoutParams.leftMargin = 0
        if (alignRight) layoutParams.leftMargin = mainLayout.width - modifiableButton.width

        modifiableButton.layoutParams = layoutParams
    }
    private fun showConstraintPopup() {
        val popupView = layoutInflater.inflate(R.layout.popup_constraints, null)

        val alignTopCheckbox = popupView.findViewById<CheckBox>(R.id.checkboxAlignTop)
        val alignBottomCheckbox = popupView.findViewById<CheckBox>(R.id.checkboxAlignBottom)
        val alignLeftCheckbox = popupView.findViewById<CheckBox>(R.id.checkboxAlignLeft)
        val alignRightCheckbox = popupView.findViewById<CheckBox>(R.id.checkboxAlignRight)

        alignTopCheckbox.isChecked = alignTop
        alignBottomCheckbox.isChecked = alignBottom
        alignLeftCheckbox.isChecked = alignLeft
        alignRightCheckbox.isChecked = alignRight

        val popupWindow = PopupWindow(
            popupView,
            LinearLayout.LayoutParams.WRAP_CONTENT,
            LinearLayout.LayoutParams.WRAP_CONTENT,
            true
        )

        popupView.findViewById<Button>(R.id.buttonApplyConstraints).setOnClickListener {
            alignTop = alignTopCheckbox.isChecked
            alignBottom = alignBottomCheckbox.isChecked
            alignLeft = alignLeftCheckbox.isChecked
            alignRight = alignRightCheckbox.isChecked

            applyConstraints()
            popupWindow.dismiss()
        }

        popupWindow.showAtLocation(mainLayout, Gravity.CENTER, 0, 0)
    }

    private fun buildAndDownload(){
        val builder = AlertDialog.Builder(this).apply {
            setMessage("Downloading..")
            setCancelable(false)
            create()
        }.show()
        try {
            val appDir = File(
                Environment.getExternalStorageDirectory(),
                "Android/data/com.yourapp.package"
            )
            val buildDir = File(appDir, "build")
            val process = ProcessBuilder("", "assembleRelease")
                .directory(buildDir)
                .start()

            val exitCode = process.waitFor()
            Toast.makeText(this,"Build Process Exited with code: $exitCode",Toast.LENGTH_SHORT).show()
            if(exitCode == 0) {
                builder.dismiss()
            }else{
                Toast.makeText(this,"Build failed with error ${exitCode}",Toast.LENGTH_SHORT).show()
                builder.dismiss()
            }

        } catch (e: Exception) {
            Toast.makeText(this,"Error ${e.message}",Toast.LENGTH_SHORT).show()
            builder.dismiss()
        }
    }

    private fun downloadApk(){
        try {
            // Path to the built APK (adjust this path as per your project structure)
            val projectDir = "/home/jagadeeshwar_m/AndroidStudioProjects/AppEasy"// Replace with your project path
            val apkPath = File("$projectDir/app/build/outputs/apk/release/app-release-unsigned.apk")

            // Verify if the APK exists
            if (!apkPath.exists()) {
                Toast.makeText(this,"APK file not found at: ${apkPath.absolutePath}",Toast.LENGTH_SHORT).show()
                return
            }

            // Get the user's Download directory
            val downloadDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
            if (!downloadDir.exists()) {
                downloadDir.mkdirs() // Create the Download directory if it doesn't exist
            }

            // Destination path for the APK in the Download directory
            val destinationPath = File(downloadDir, "Your-App.apk")

            // Copy the APK file
            FileInputStream(apkPath).use { inputStream ->
                FileOutputStream(destinationPath).use { outputStream ->
                    inputStream.copyTo(outputStream)
                }
            }
            Toast.makeText(this,"APK successfully copied to: ${destinationPath.absolutePath}",Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            Toast.makeText(this,"Failed to copy the APK ${e.message}",Toast.LENGTH_SHORT).show()
        }
    }
}