package com.datecomp.dca.shell

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.datecomp.dca.shell.theme.DcaTheme

/**
 * Actividad única de DCA. Toda la app es Compose; la navegación interna
 * vive en [DcaNavHost] y cada slice expone su propio ViewModel.
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
