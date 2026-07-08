package com.example.phoenx.ui.theme

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.phoenx.data.preferences.PreferenceManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ThemeViewModel @Inject constructor(
    private val preferenceManager: PreferenceManager
) : ViewModel() {

    val accentColor: StateFlow<Color> = preferenceManager.accentColor
        .map { it?.let { Color(it) } ?: AccentPrimary }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), AccentPrimary)

    val backgroundColor: StateFlow<Color> = preferenceManager.backgroundColor
        .map { it?.let { Color(it) } ?: BackgroundPrimary }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), BackgroundPrimary)

    val backgroundStyle: StateFlow<String> = preferenceManager.backgroundStyle
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "RADIAL")

    fun setAccent(color: Color) {
        viewModelScope.launch {
            preferenceManager.setAccentColor(color.toArgb())
        }
    }

    fun setBackground(color: Color) {
        viewModelScope.launch {
            preferenceManager.setBackgroundColor(color.toArgb())
        }
    }

    fun setBackgroundStyle(style: String) {
        viewModelScope.launch {
            preferenceManager.setBackgroundStyle(style)
        }
    }
}
