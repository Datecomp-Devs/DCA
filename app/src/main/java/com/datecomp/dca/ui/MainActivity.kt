package com.datecomp.dca.ui

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.datecomp.dca.ui.theme.DcaTheme

/**
 * Actividad única de DCA. Toda la app es Compose; la navegación interna
 * vive en [com.datecomp.dca.ui.DcaNavHost] y el estado en [AppViewModel].
 */
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            DcaTheme {
                DcaNavHost()
            }
        }
    }
}