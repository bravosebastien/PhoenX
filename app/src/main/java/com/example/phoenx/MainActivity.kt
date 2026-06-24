package com.example.phoenx

import android.content.Context
import android.content.Intent
import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.firestore
import com.google.firebase.vertexai.vertexAI
import com.google.firebase.vertexai.type.content
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore
    private val activityScope = MainScope()

    private var currentBgColorHex = "#00FFFF"
    private var currentTextColorHex = "#FFD700"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Initialisation Firebase
        auth = Firebase.auth
        db = Firebase.firestore

        // Vérification automatique : si déjà connecté, on saute l'identification
        if (auth.currentUser != null) {
            startActivity(Intent(this, DashboardActivity::class.java))
            finish()
        }

        setContentView(R.layout.activity_main)

        val mainLayout = findViewById<LinearLayout>(R.id.mainLayout)
        val hubCard = findViewById<LinearLayout>(R.id.hubCard)
        val mainTitle = findViewById<TextView>(R.id.mainTitle)
        val loginButton = findViewById<Button>(R.id.loginButton)
        val registerButton = findViewById<Button>(R.id.registerButton)
        val aiTestButton = findViewById<Button>(R.id.aiTestButton)
        val emailInput = findViewById<EditText>(R.id.emailInput)
        val passwordInput = findViewById<EditText>(R.id.passwordInput)

        val prefs = getSharedPreferences("CockpitPrefs", Context.MODE_PRIVATE)
        currentBgColorHex = prefs.getString("bgColor", "#00FFFF") ?: "#00FFFF"
        currentTextColorHex = prefs.getString("textColor", "#FFD700") ?: "#FFD700"

        appliquerDesignCockpit(mainLayout, hubCard, mainTitle, loginButton, registerButton, aiTestButton, emailInput, passwordInput)

        // Boutons de changement de thème
        findViewById<Button>(R.id.btnBgNeon).setOnClickListener {
            currentBgColorHex = "#00FFFF"
            prefs.edit().putString("bgColor", currentBgColorHex).apply()
            appliquerDesignCockpit(mainLayout, hubCard, mainTitle, loginButton, registerButton, aiTestButton, emailInput, passwordInput)
        }
        findViewById<Button>(R.id.btnBgMagma).setOnClickListener {
            currentBgColorHex = "#FF4500"
            prefs.edit().putString("bgColor", currentBgColorHex).apply()
            appliquerDesignCockpit(mainLayout, hubCard, mainTitle, loginButton, registerButton, aiTestButton, emailInput, passwordInput)
        }
        findViewById<Button>(R.id.btnBgSolaire).setOnClickListener {
            currentBgColorHex = "#FFD700"
            prefs.edit().putString("bgColor", currentBgColorHex).apply()
            appliquerDesignCockpit(mainLayout, hubCard, mainTitle, loginButton, registerButton, aiTestButton, emailInput, passwordInput)
        }

        findViewById<Button>(R.id.btnTextArgent).setOnClickListener {
            currentTextColorHex = "#C0C0C0"
            prefs.edit().putString("textColor", currentTextColorHex).apply()
            appliquerDesignCockpit(mainLayout, hubCard, mainTitle, loginButton, registerButton, aiTestButton, emailInput, passwordInput)
        }
        findViewById<Button>(R.id.btnTextMagenta).setOnClickListener {
            currentTextColorHex = "#FF00FF"
            prefs.edit().putString("textColor", currentTextColorHex).apply()
            appliquerDesignCockpit(mainLayout, hubCard, mainTitle, loginButton, registerButton, aiTestButton, emailInput, passwordInput)
        }
        findViewById<Button>(R.id.btnTextSolaire).setOnClickListener {
            currentTextColorHex = "#FFD700"
            prefs.edit().putString("textColor", currentTextColorHex).apply()
            appliquerDesignCockpit(mainLayout, hubCard, mainTitle, loginButton, registerButton, aiTestButton, emailInput, passwordInput)
        }

        // --- LOGIQUE FIREBASE ---

        registerButton.setOnClickListener {
            val email = emailInput.text.toString().trim()
            val password = passwordInput.text.toString().trim()

            if (email.isNotEmpty() && password.isNotEmpty()) {
                auth.createUserWithEmailAndPassword(email, password)
                    .addOnCompleteListener(this) { task ->
                        if (task.isSuccessful) {
                            val userId = auth.currentUser?.uid
                            val userProfile = hashMapOf(
                                "uid" to userId,
                                "email" to email,
                                "createdAt" to com.google.firebase.Timestamp.now(),
                                "role" to "user"
                            )
                            if (userId != null) {
                                db.collection("users").document(userId).set(userProfile)
                                    .addOnSuccessListener {
                                        Toast.makeText(this, "Compte et profil créés !", Toast.LENGTH_SHORT).show()
                                        // Redirection vers le Dashboard
                                        startActivity(Intent(this, DashboardActivity::class.java))
                                        finish()
                                    }
                            }
                        } else {
                            Toast.makeText(this, "Erreur : ${task.exception?.message}", Toast.LENGTH_LONG).show()
                        }
                    }
            }
        }

        loginButton.setOnClickListener {
            val email = emailInput.text.toString().trim()
            val password = passwordInput.text.toString().trim()

            if (email.isNotEmpty() && password.isNotEmpty()) {
                auth.signInWithEmailAndPassword(email, password)
                    .addOnCompleteListener(this) { task ->
                        if (task.isSuccessful) {
                            Toast.makeText(this, "Connexion réussie !", Toast.LENGTH_SHORT).show()
                            // Redirection vers le Dashboard
                            startActivity(Intent(this, DashboardActivity::class.java))
                            finish()
                        } else {
                            Toast.makeText(this, "Erreur : ${task.exception?.message}", Toast.LENGTH_LONG).show()
                        }
                    }
            }
        }

        aiTestButton.setOnClickListener {
            val currentUser = auth.currentUser
            if (currentUser == null) {
                Toast.makeText(this, "Connectez-vous d'abord", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            Toast.makeText(this, "Phoen-X réfléchit...", Toast.LENGTH_SHORT).show()

            activityScope.launch {
                try {
                    val model = Firebase.vertexAI.generativeModel("gemini-1.5-flash")
                    val response = model.generateContent(content { text("Phrase de bienvenue futuriste.") })
                    val iaResponseText = response.text ?: "Erreur IA"

                    val aiLog = hashMapOf(
                        "prompt" to "Phrase de bienvenue",
                        "response" to iaResponseText,
                        "timestamp" to com.google.firebase.Timestamp.now()
                    )

                    db.collection("users").document(currentUser.uid)
                        .collection("ai_tests").add(aiLog)
                        .addOnSuccessListener {
                            Toast.makeText(applicationContext, "IA : $iaResponseText", Toast.LENGTH_LONG).show()
                        }
                } catch (e: Exception) {
                    Toast.makeText(applicationContext, "Erreur IA : ${e.message}", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    private fun appliquerDesignCockpit(
        layout: LinearLayout, card: LinearLayout, title: TextView, 
        btnLogin: Button, btnReg: Button, btnAi: Button, email: EditText, pass: EditText
    ) {
        val colorInt = Color.parseColor(currentBgColorHex)
        val alphaColor = Color.argb(102, Color.red(colorInt), Color.green(colorInt), Color.blue(colorInt))
        
        val backgroundGradient = GradientDrawable(
            GradientDrawable.Orientation.TOP_BOTTOM,
            intArrayOf(Color.BLACK, alphaColor, colorInt)
        )
        layout.background = backgroundGradient

        val textAccentColorInt = Color.parseColor(currentTextColorHex)
        val accentColorStateList = ColorStateList.valueOf(textAccentColorInt)
        
        title.setTextColor(textAccentColorInt)
        email.backgroundTintList = accentColorStateList
        pass.backgroundTintList = accentColorStateList

        val cardStyle = GradientDrawable().apply {
            setColor(Color.parseColor("#151515"))
            setStroke(4, textAccentColorInt)
            cornerRadius = 36f
        }
        card.background = cardStyle

        btnLogin.backgroundTintList = accentColorStateList
        btnLogin.setTextColor(Color.BLACK)
        btnReg.setTextColor(textAccentColorInt)
        btnAi.backgroundTintList = accentColorStateList
        btnAi.setTextColor(Color.BLACK)
    }
}
