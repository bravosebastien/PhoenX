package com.example.phoenx

import android.content.Context
import android.content.Intent
import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.Firebase
import com.google.firebase.auth.auth

class DashboardActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_dashboard)

        val dashboardLayout = findViewById<LinearLayout>(R.id.dashboardLayout)
        val statusCard = findViewById<LinearLayout>(R.id.statusCard)
        val dashboardTitle = findViewById<TextView>(R.id.dashboardTitle)
        val btnCreateCapsule = findViewById<Button>(R.id.btnCreateCapsule)
        val btnConsultIA = findViewById<Button>(R.id.btnConsultIA)
        val btnLogout = findViewById<Button>(R.id.btnLogout)

        // Récupération et application instantanée des couleurs du Cockpit de l'utilisateur
        val prefs = getSharedPreferences("CockpitPrefs", Context.MODE_PRIVATE)
        val bgColorHex = prefs.getString("bgColor", "#00FFFF") ?: "#00FFFF"
        val textColorHex = prefs.getString("textColor", "#FFD700") ?: "#FFD700"

        val colorInt = Color.parseColor(bgColorHex)
        val alphaColor = Color.argb(102, Color.red(colorInt), Color.green(colorInt), Color.blue(colorInt))
        dashboardLayout.background = GradientDrawable(GradientDrawable.Orientation.TOP_BOTTOM, intArrayOf(Color.BLACK, alphaColor, colorInt))

        val textAccentColorInt = Color.parseColor(textColorHex)
        val accentColorStateList = ColorStateList.valueOf(textAccentColorInt)
        
        dashboardTitle.setTextColor(textAccentColorInt)
        btnLogout.setTextColor(textAccentColorInt)

        val cardStyle = GradientDrawable().apply {
            setColor(Color.parseColor("#151515"))
            setStroke(4, textAccentColorInt)
            cornerRadius = 36f
        }
        statusCard.background = cardStyle

        btnCreateCapsule.backgroundTintList = accentColorStateList
        btnCreateCapsule.setTextColor(Color.BLACK)
        btnConsultIA.backgroundTintList = accentColorStateList
        btnConsultIA.setTextColor(Color.BLACK)

        // Gestion de la fermeture de session
        btnLogout.setOnClickListener {
            Firebase.auth.signOut()
            startActivity(Intent(this, MainActivity::class.java))
            finish()
        }
    }
}
