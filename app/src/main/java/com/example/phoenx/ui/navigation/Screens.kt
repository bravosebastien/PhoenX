package com.example.phoenx.ui.navigation

sealed class Screen(val route: String) {
    object Auth : Screen("auth")
    object Timeline : Screen("timeline")
    object AddEntry : Screen("add_entry")
    object Cockpit : Screen("cockpit")
}
