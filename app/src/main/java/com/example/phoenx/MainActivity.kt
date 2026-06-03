package com.example.phoenx

import android.content.Context
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

    // Variables pour stocker les couleurs sélectionnées (Valeurs par défaut)
    private var currentBgColorHex = "#00FFFF"   // Néon par défaut
    private var currentTextColorHex = "#FFD700" // Solaire par défaut

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Initialisation de Firebase Auth et Firestore
        auth = Firebase.auth
        db = Firebase.firestore

        // Récupération des composants majeurs de l'écran
        val mainLayout = findViewById<LinearLayout>(R.id.mainLayout)
        val hubCard = findViewById<LinearLayout>(R.id.hubCard)
        val mainTitle = findViewById<TextView>(R.id.mainTitle)
        val loginButton = findViewById<Button>(R.id.loginButton)
        val registerButton = findViewById<Button>(R.id.registerButton)
        val aiTestButton = findViewById<Button>(R.id.aiTestButton)
        val emailInput = findViewById<EditText>(R.id.emailInput)
        val passwordInput = findViewById<EditText>(R.id.passwordInput)

        // Chargement des préférences enregistrées
        val prefs = getSharedPreferences("CockpitPrefs", Context.MODE_PRIVATE)
        currentBgColorHex = prefs.getString("bgColor", "#00FFFF") ?: "#00FFFF"
        currentTextColorHex = prefs.getString("textColor", "#FFD700") ?: "#FFD700"

        // Application immédiate du design sauvegardé
        appliquerDesignCockpit(mainLayout, hubCard, mainTitle, loginButton, registerButton, aiTestButton, emailInput, passwordInput)

        // --- COMMANDES DE MODIFICATION DU FOND ---
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

        // --- COMMANDES DE MODIFICATION DES TEXTES / BORDURES ---
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

        // Inscription
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
                                    }
                            }
                        } else {
                            Toast.makeText(this, "Erreur : ${task.exception?.message}", Toast.LENGTH_LONG).show()
                        }
                    }
            }
        }

        // Connexion
        loginButton.setOnClickListener {
            val email = emailInput.text.toString().trim()
            val password = passwordInput.text.toString().trim()

            if (email.isNotEmpty() && password.isNotEmpty()) {
                auth.signInWithEmailAndPassword(email, password)
                    .addOnCompleteListener(this) { task ->
                        if (task.isSuccessful) {
                            Toast.makeText(this, "Connexion réussie !", Toast.LENGTH_SHORT).show()
                        } else {
                            Toast.makeText(this, "Erreur : ${task.exception?.message}", Toast.LENGTH_LONG).show()
                        }
                    }
            }
        }

        // Déclenchement de l'IA Gemini
        aiTestButton.setOnClickListener {
            val currentUser = auth.currentUser
            if (currentUser == null) {
                Toast.makeText(this, "Connectez-vous d'abord pour activer l'IA", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            Toast.makeText(this, "Phoen-X réfléchit...", Toast.LENGTH_SHORT).show()

            activityScope.launch {
                try {
                    val model = Firebase.vertexAI.generativeModel("gemini-1.5-flash")
                    val response = model.generateContent(
                        content {
                            text("Rédige une phrase de bienvenue futuriste et poétique pour un utilisateur de l'application Phoen-X, une capsule temporelle numérique.")
                        }
                    )

                    val iaResponseText = response.text ?: "L'IA n'a pas pu répondre."

                    val aiLog = hashMapOf(
                        "prompt" to "Phrase de bienvenue futuriste",
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

    // Fonction qui redessine l'écran à la volée
    private fun appliquerDesignCockpit(
        layout: LinearLayout, card: LinearLayout, title: TextView, 
        btnLogin: Button, btnReg: Button, btnAi: Button, email: EditText, pass: EditText
    ) {
        // 1. Génération du dégradé à 3 couches pour le fond
        val colorInt = Color.parseColor(currentBgColorHex)
        val alphaColor = Color.argb(102, Color.red(colorInt), Color.green(colorInt), Color.blue(colorInt))
        
        val backgroundGradient = GradientDrawable(
            GradientDrawable.Orientation.TOP_BOTTOM,
            intArrayOf(Color.BLACK, alphaColor, colorInt)
        )
        layout.background = backgroundGradient

        // 2. Application de la couleur d'accentuation
        val textAccentColorInt = Color.parseColor(currentTextColorHex)
        val accentColorStateList = ColorStateList.valueOf(textAccentColorInt)
        
        title.setTextColor(textAccentColorInt)
        email.backgroundTintList = accentColorStateList
        pass.backgroundTintList = accentColorStateList

        // 3. Redessiner la Hub Card
        val cardStyle = GradientDrawable()
        cardStyle.setColor(Color.parseColor("#151515"))
        cardStyle.setStroke(4, textAccentColorInt)
        cardStyle.cornerRadius = 36f
        card.background = cardStyle

        // 4. Style des boutons
        btnLogin.backgroundTintList = accentColorStateList
        btnLogin.setTextColor(Color.BLACK)
        
        btnReg.setTextColor(textAccentColorInt)
        
        btnAi.backgroundTintList = accentColorStateList
        btnAi.setTextColor(Color.BLACK)
    }
}
