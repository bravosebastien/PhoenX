package com.example.phoenx.services

import android.app.PendingIntent
import android.content.Intent
import android.os.Build
import android.service.quicksettings.TileService
import com.example.phoenx.MainActivity
import com.example.phoenx.ui.navigation.Screen

/**
 * Service pour le volet de raccourcis Android (Quick Settings).
 * Permet de déclencher le Mode 3h du Matin à l'aveugle.
 */
class NightCaptureTileService : TileService() {

    override fun onClick() {
        super.onClick()
        
        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra("START_ROUTE", Screen.Capture.createRoute(Screen.Capture.TYPE_NIGHT))
        }

        val pendingIntent = PendingIntent.getActivity(
            this, 0, intent, 
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                // API 34+ (Android 14+)
                startActivityAndCollapse(pendingIntent)
            } else {
                // Rétrocompatibilité pour versions antérieures
                val method = TileService::class.java.getMethod("startActivityAndCollapse", Intent::class.java)
                method.invoke(this, intent)
            }
        } catch (e: Exception) {
            // Fallback ultime
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            startActivity(intent)
        }
    }
}
