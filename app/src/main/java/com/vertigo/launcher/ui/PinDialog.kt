package com.vertigo.launcher.ui

import android.app.Dialog
import android.content.Context
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.Gravity
import android.view.ViewGroup
import android.view.Window
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import com.vertigo.launcher.R
import com.vertigo.launcher.utils.PreferencesManager

/**
 * PinDialog - PIN entry dialog for protected features
 */
class PinDialog(
    context: Context,
    private val preferencesManager: PreferencesManager,
    private val onSuccess: () -> Unit,
    private val onCancel: () -> Unit = {}
) : Dialog(context) {

    private lateinit var pinInput: EditText
    private lateinit var titleText: TextView
    private var isSettingPin = false
    private var firstPin: String? = null
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requestWindowFeature(Window.FEATURE_NO_TITLE)
        
        val container = createDialogLayout()
        setContentView(container)
        
        window?.let { w ->
            w.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
            w.setLayout(
                (context.resources.displayMetrics.widthPixels * 0.85).toInt(),
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
        }
        
        setCanceledOnTouchOutside(false)
        
        // Check if PIN is already set
        isSettingPin = !preferencesManager.isPinSet()
        updateTitle()
    }
    
    private fun createDialogLayout(): LinearLayout {
        return LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(48, 48, 48, 48)
            setBackgroundColor(0xE6121214.toInt()) // Dark with transparency
            
            // Title
            titleText = TextView(context).apply {
                textSize = 18f
                setTextColor(0xFF00F0FF.toInt()) // Neon cyan
                gravity = Gravity.CENTER
                setPadding(0, 0, 0, 32)
            }
            addView(titleText)
            
            // PIN Input
            pinInput = EditText(context).apply {
                hint = "Enter 4-digit PIN"
                inputType = android.text.InputType.TYPE_CLASS_NUMBER or
                        android.text.InputType.TYPE_NUMBER_VARIATION_PASSWORD
                maxLines = 1
                setTextColor(0xFFFFFFFF.toInt())
                setHintTextColor(0x80FFFFFF.toInt())
                textSize = 24f
                gravity = Gravity.CENTER
                setBackgroundColor(0x33FFFFFF)
                setPadding(32, 24, 32, 24)
                filters = arrayOf(android.text.InputFilter.LengthFilter(4))
            }
            addView(pinInput, LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            ).apply {
                setMargins(0, 0, 0, 32)
            })
            
            // Button row
            val buttonRow = LinearLayout(context).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER
            }
            
            // Cancel button
            val cancelBtn = TextView(context).apply {
                text = "Cancel"
                textSize = 16f
                setTextColor(0xFFFF006E.toInt()) // Neon pink
                setPadding(32, 16, 32, 16)
                setOnClickListener {
                    onCancel()
                    dismiss()
                }
            }
            buttonRow.addView(cancelBtn)
            
            // Spacer
            buttonRow.addView(android.view.View(context), LinearLayout.LayoutParams(48, 1))
            
            // Confirm button
            val confirmBtn = TextView(context).apply {
                text = "Confirm"
                textSize = 16f
                setTextColor(0xFF00F0FF.toInt()) // Neon cyan
                setPadding(32, 16, 32, 16)
                setOnClickListener { onConfirmClicked() }
            }
            buttonRow.addView(confirmBtn)

            // Biometric button (if supported)
            val biometricManager = androidx.biometric.BiometricManager.from(context)
            val canAuth = biometricManager.canAuthenticate(
                androidx.biometric.BiometricManager.Authenticators.BIOMETRIC_WEAK or
                androidx.biometric.BiometricManager.Authenticators.DEVICE_CREDENTIAL
            )
            if (canAuth == androidx.biometric.BiometricManager.BIOMETRIC_SUCCESS && preferencesManager.isPinSet()) {
                val bioBtn = TextView(context).apply {
                    text = "Fingerprint"
                    textSize = 16f
                    setTextColor(0xFFAC00FF.toInt()) // Neon purple
                    setPadding(32, 16, 32, 16)
                    setOnClickListener { triggerBiometricPrompt() }
                }
                buttonRow.addView(android.view.View(context), LinearLayout.LayoutParams(48, 1))
                buttonRow.addView(bioBtn)
            }
            
            addView(buttonRow)
        }
    }

    private fun triggerBiometricPrompt() {
        val activity = context as? androidx.fragment.app.FragmentActivity ?: return
        val executor = androidx.core.content.ContextCompat.getMainExecutor(context)
        val callback = object : androidx.biometric.BiometricPrompt.AuthenticationCallback() {
            override fun onAuthenticationSucceeded(result: androidx.biometric.BiometricPrompt.AuthenticationResult) {
                dismiss()
                onSuccess()
            }
        }
        val promptInfo = androidx.biometric.BiometricPrompt.PromptInfo.Builder()
            .setTitle("Unlock V-Launcher")
            .setSubtitle("Confirm your fingerprint or device credentials")
            .setAllowedAuthenticators(
                androidx.biometric.BiometricManager.Authenticators.BIOMETRIC_WEAK or
                androidx.biometric.BiometricManager.Authenticators.DEVICE_CREDENTIAL
            )
            .build()
        androidx.biometric.BiometricPrompt(activity, executor, callback).authenticate(promptInfo)
    }

    override fun show() {
        super.show()
        val biometricManager = androidx.biometric.BiometricManager.from(context)
        val canAuth = biometricManager.canAuthenticate(
            androidx.biometric.BiometricManager.Authenticators.BIOMETRIC_WEAK or
            androidx.biometric.BiometricManager.Authenticators.DEVICE_CREDENTIAL
        )
        if (canAuth == androidx.biometric.BiometricManager.BIOMETRIC_SUCCESS && preferencesManager.isPinSet()) {
            triggerBiometricPrompt()
        }
    }
    
    private fun updateTitle() {
        titleText.text = when {
            isSettingPin && firstPin == null -> "Set a PIN"
            isSettingPin && firstPin != null -> "Confirm PIN"
            else -> "Enter PIN"
        }
    }
    
    private fun onConfirmClicked() {
        val pin = pinInput.text.toString()
        
        if (pin.length != 4) {
            Toast.makeText(context, "PIN must be 4 digits", Toast.LENGTH_SHORT).show()
            return
        }
        
        when {
            isSettingPin && firstPin == null -> {
                // First entry - save and ask for confirmation
                firstPin = pin
                pinInput.text.clear()
                updateTitle()
            }
            isSettingPin && firstPin != null -> {
                // Confirmation entry
                if (pin == firstPin) {
                    preferencesManager.setPin(pin)
                    Toast.makeText(context, "PIN set successfully", Toast.LENGTH_SHORT).show()
                    dismiss()
                    onSuccess()
                } else {
                    Toast.makeText(context, "PINs don't match", Toast.LENGTH_SHORT).show()
                    firstPin = null
                    pinInput.text.clear()
                    updateTitle()
                }
            }
            else -> {
                // Verifying existing PIN
                if (preferencesManager.verifyPin(pin)) {
                    dismiss()
                    onSuccess()
                } else {
                    Toast.makeText(context, "Wrong PIN", Toast.LENGTH_SHORT).show()
                    pinInput.text.clear()
                }
            }
        }
    }
}
