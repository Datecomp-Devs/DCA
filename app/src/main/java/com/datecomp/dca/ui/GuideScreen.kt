package com.datecomp.dca.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.datecomp.dca.R
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect

/**
 * Pantalla de guía inicial: explica por qué DCA necesita el permiso de
 * Acceso a notificaciones y ofrece el botón directo a Ajustes. Solo se
 * muestra una vez; puede reintentalarse hasta conceder el permiso o saltar.
 */
@Composable
fun GuideScreen(
    viewModel: AppViewModel,
    onDone: () -> Unit,
) {
    val state by viewModel.guideState.collectAsState()
    val context = LocalContext.current

    // Refresca permisos al volver de Ajustes (ON_RESUME).
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
        viewModel.refreshPermissionState()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Spacer(modifier = Modifier.height(48.dp))

        Text(
            text = stringResource(R.string.guide_intro),
            style = MaterialTheme.typography.headlineMedium,
            textAlign = TextAlign.Center,
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = stringResource(R.string.guide_title),
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )

        Spacer(modifier = Modifier.height(32.dp))

        PermissionCard(
            title = "Acceso a notificaciones",
            reason = "DCA necesita leer las notificaciones entrantes de tus apps de mensajería " +
                "para detectar cuándo alguien te escribe. Nunca accede al contenido más allá de " +
                "lo necesario para responder, y nada sale de tu dispositivo.",
            granted = state.notificationListenerGranted,
        )

        Spacer(modifier = Modifier.height(12.dp))

        PermissionCard(
            title = "Notificaciones de DCA",
            reason = "Se usa para mostrarte una confirmación discreta cada vez que DCA envía una " +
                "respuesta automática, así siempre tienes control y trazabilidad.",
            granted = true,
        )

        Spacer(modifier = Modifier.height(12.dp))

        PermissionCard(
            title = "Arranque automático",
            reason = "Tras reiniciar el teléfono, DCA se reconecta automáticamente al sistema de " +
                "notificaciones para no dejar de responder. No consume batería extra: solo " +
                "reacciona cuando llega una notificación.",
            granted = true,
        )

        Spacer(modifier = Modifier.height(12.dp))

        PermissionCard(
            title = "Optimización de batería",
            reason = "Algunos teléfonos cierran apps en segundo plano para ahorrar batería. " +
                "Eximir a DCA evita que el sistema la apague y que dejes de recibir respuestas. " +
                "DCA sin embargo consume muy poca batería: solo reacciona a notificaciones y no " +
                "tiene procesos en segundo plano.",
            granted = state.batteryOptimizationIgnored,
        )

        Spacer(modifier = Modifier.height(32.dp))

        Button(
            onClick = {
                viewModel.openNotificationAccessSettings()
                viewModel.refreshPermissionState()
            },
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
            ),
            enabled = !state.notificationListenerGranted,
        ) {
            Text(text = if (state.notificationListenerGranted) "Permiso concedido ✓" else "Conceder acceso a notificaciones")
        }

        Spacer(modifier = Modifier.height(12.dp))

        OutlinedButton(
            onClick = {
                viewModel.requestIgnoreBatteryOptimizations()
                viewModel.refreshPermissionState()
            },
            modifier = Modifier.fillMaxWidth(),
            enabled = !state.batteryOptimizationIgnored,
        ) {
            Text(text = if (state.batteryOptimizationIgnored) "Batería exenta ✓" else "Eximir de optimización de batería")
        }

        Spacer(modifier = Modifier.height(12.dp))

        OutlinedButton(
            onClick = onDone,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(text = stringResource(R.string.guide_skip))
        }

        Spacer(modifier = Modifier.height(48.dp))
    }
}

/** Tarjeta reutilizable con un permiso, su explicación y estado. */
@Composable
private fun PermissionCard(
    title: String,
    reason: String,
    granted: Boolean,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant,
        ),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    painter = painterResource(R.drawable.ic_reply),
                    contentDescription = null,
                    tint = if (granted) Color.DarkGray else MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.width(20.dp),
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                )
                Spacer(modifier = Modifier.weight(1f))
                Text(
                    text = if (granted) "✓ Listo" else "Requerido",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = reason,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}