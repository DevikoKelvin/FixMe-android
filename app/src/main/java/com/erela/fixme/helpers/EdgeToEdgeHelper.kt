package com.erela.fixme.helpers

import android.graphics.Color
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.enableEdgeToEdge
import com.erela.fixme.R

/**
 * Edge-to-edge with an opaque navigation bar: white with dark icons, or in dark mode the app's
 * surface colour with light icons. `auto` reads the current night mode, so it follows the switch.
 */
fun ComponentActivity.enableEdgeToEdgeOpaqueNav() = enableEdgeToEdge(
    navigationBarStyle = SystemBarStyle.auto(Color.WHITE, getColor(R.color.surface))
)
