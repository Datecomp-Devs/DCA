package com.datecomp.dca.onboarding

import androidx.compose.foundation.background
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
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.datecomp.dca.R
import com.datecomp.dca.permissions.PermissionsViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect

/**
 * Pantalla de guía inicial: explica por qué DCA necesita el permiso de
 * Acceso a notificaciones y ofrece el botón directo a Ajustes. Solo se
 * muestra una vez; puede reintentalarse hasta conceder el permiso o saltar.
 */
@Composable
fun GuideScreen(
    permissions: PermissionsViewModel,
    onDone: () -> Unit,
) {
    val state by permissions.state.collectAsState()

    // Refresca permisos al volver de Ajustes (ON_RESUME).
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
        permissions.refresh()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
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
            title = stringResource(R.string.guide_permission_notifications_title),
            reason = stringResource(R.string.guide_permission_notifications_reason),
            granted = state.notificationListenerGranted,
        )

        Spacer(modifier = Modifier.height(12.dp))

        PermissionCard(
            title = stringResource(R.string.guide_permission_dca_notifications_title),
            reason = stringResource(R.string.guide_permission_dca_notifications_reason),
            granted = true,
        )

        Spacer(modifier = Modifier.height(12.dp))

        PermissionCard(
            title = stringResource(R.string.guide_permission_boot_title),
            reason = stringResource(R.string.guide_permission_boot_reason),
            granted = true,
        )

        Spacer(modifier = Modifier.height(12.dp))

        PermissionCard(
            title = stringResource(R.string.guide_permission_battery_title),
            reason = stringResource(R.string.guide_permission_battery_reason),
            granted = state.batteryOptimizationIgnored,
        )

        Spacer(modifier = Modifier.height(32.dp))

        Button(
            onClick = {
                permissions.openNotificationAccessSettings()
                permissions.refresh()
            },
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                // Estado deshabilitado ("Permiso concedido ✓"): surfaceVariant
                // garantiza contraste con el contenido en ambos temas; el alfa
                // de M3 se neutraliza en el texto (ver DisabledAwareText).
                disabledContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                disabledContentColor = MaterialTheme.colorScheme.onSurface,
            ),
            enabled = !state.notificationListenerGranted,
        ) {
            Text(
                text = if (state.notificationListenerGranted) {
                    stringResource(R.string.guide_button_permission_granted)
                } else {
                    stringResource(R.string.guide_button_grant_access)
                },
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        OutlinedButton(
            onClick = {
                permissions.requestIgnoreBatteryOptimizations()
                permissions.refresh()
            },
            modifier = Modifier.fillMaxWidth(),
            enabled = !state.batteryOptimizationIgnored,
            colors = ButtonDefaults.outlinedButtonColors(
                contentColor = MaterialTheme.colorScheme.onSurface,
                // Texto de alto contraste también en estado deshabilitado
                // ("Batería exenta ✓"); el default de M3 lo deja tenue.
                disabledContentColor = MaterialTheme.colorScheme.onSurface,
            ),
        ) {
            Text(
                text = if (state.batteryOptimizationIgnored) {
                    stringResource(R.string.guide_button_battery_exempt)
                } else {
                    stringResource(R.string.guide_button_exempt_battery)
                },
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        OutlinedButton(
            onClick = onDone,
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.outlinedButtonColors(
                contentColor = MaterialTheme.colorScheme.onSurface,
            ),
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
                    // DarkGray fijo era ilegible en modo oscuro sobre
                    // surfaceVariant (#212121); el esquema garantiza
                    // contraste en ambos temas.
                    tint = if (granted) {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    } else {
                        MaterialTheme.colorScheme.onSurface
                    },
                    modifier = Modifier.width(20.dp),
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                )
                Spacer(modifier = Modifier.weight(1f))
                Text(
                    text = if (granted) {
                        stringResource(R.string.guide_status_ready)
                    } else {
                        stringResource(R.string.guide_status_required)
                    },
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