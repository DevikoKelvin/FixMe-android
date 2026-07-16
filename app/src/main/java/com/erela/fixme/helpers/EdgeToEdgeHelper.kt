package com.erela.fixme.helpers

import android.graphics.Color
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.enableEdgeToEdge

/** Edge-to-edge with an opaque white navigation bar (dark icons). */
fun ComponentActivity.enableEdgeToEdgeOpaqueNav() = enableEdgeToEdge(
    navigationBarStyle = SystemBarStyle.light(Color.WHITE, Color.WHITE)
)
