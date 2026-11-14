package com.datecomp.dca.targets

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import androidx.core.graphics.drawable.toBitmap
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import com.datecomp.dca.permissions.PermissionsViewModel

/**
 * Pantalla principal: lista de apps agregadas a DCA y buscador estilo
 * motor de búsqueda — al escribir, un panel de resultados sobrepuesto
 * (con iconos reales de las apps) aparece bajo el campo.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppsScreen(
    targets: TargetsViewModel,
    permissions: PermissionsViewModel,
    onEditTarget: (String) -> Unit,
) {
    val state by targets.state.collectAsState()
    val permissionState by permissions.state.collectAsState()
    val searching = state.query.isNotBlank()

    // Refresca el estado del permiso al volver de Ajustes (ON_RESUME):
    // si el usuario activó el acceso, la tarjeta desaparece al instante.
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
        permissions.refresh()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("DCA") },
                actions = {
                    Surface(
                        color = if (permissionState.notificationListenerGranted) {
                            MaterialTheme.colorScheme.surfaceVariant
                        } else {
                            MaterialTheme.colorScheme.inverseSurface
                        },
                        contentColor = if (permissionState.notificationListenerGranted) {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        } else {
                            MaterialTheme.colorScheme.inverseOnSurface
                        },
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.padding(end = 12.dp),
                    ) {
                        Text(
                            text = if (permissionState.notificationListenerGranted) "Activo" else "Inactivo",
                            style = MaterialTheme.typography.labelMedium,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        )
                    }
                },
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp),
        ) {
            if (!permissionState.notificationListenerGranted) {
                Card(
                    // Colores del esquema (inverseSurface/inverseOnSurface en
                    // claro; alto contraste en oscuro) en lugar de negro fijo:
                    // el negro fijo era ilegible con el tema claro del sistema.
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.inverseSurface,
                        contentColor = MaterialTheme.colorScheme.inverseOnSurface,
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(
                            text = "El acceso a notificaciones está desactivado. DCA no puede responder hasta que lo actives en Ajustes.",
                            style = MaterialTheme.typography.bodyMedium,
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        Button(
                            onClick = { permissions.openNotificationAccessSettings() },
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.surface,
                                contentColor = MaterialTheme.colorScheme.onSurface,
                            ),
                        ) {
                            Icon(
                                Icons.Default.NotificationsActive,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp),
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Activar acceso a notificaciones")
                        }
                    }
                }
            }

            // Campo de búsqueda: ancla del panel de resultados.
            OutlinedTextField(
                value = state.query,
                onValueChange = { targets.setQuery(it) },
                placeholder = { Text("Buscar apps…") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                trailingIcon = {
                    if (searching) {
                        IconButtonClear { targets.setQuery("") }
                    }
                },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp)
                    .zIndex(1f),
            )

            Box(modifier = Modifier.fillMaxSize()) {
                // Contenido base: lista de apps agregadas.
                Column(modifier = Modifier.fillMaxSize()) {
                    Text(
                        text = "Apps agregadas a DCA",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(top = 8.dp, bottom = 4.dp),
                    )
                    if (state.targets.isEmpty()) {
                        Text(
                            text = "Aún no has agregado apps. Usa el buscador de arriba para encontrar tu app de mensajería.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(vertical = 12.dp),
                        )
                    } else {
                        LazyColumn(modifier = Modifier.fillMaxSize()) {
                            items(state.targets, key = { it.packageName }) { target ->
                                TargetRow(
                                    target = target,
                                    onToggle = { enabled -> targets.toggleTarget(target.packageName, enabled) },
                                    onEdit = { onEditTarget(target.packageName) },
                                    onRemove = { targets.removeTarget(target.packageName) },
                                )
                            }
                        }
                    }
                }

                // Panel de resultados estilo buscador: sobrepone el contenido
                // base solo mientras hay texto de búsqueda.
                if (searching) {
                    SearchResultsPanel(
                        results = state.filteredInstalled,
                        query = state.query,
                        addedPackages = state.targets.map { it.packageName }.toSet(),
                        onAdd = { app -> targets.addTarget(app.packageName, app.displayName) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 4.dp)
                            .zIndex(2f),
                    )
                }
            }
        }
    }
}

/** Panel desplegable de resultados, como autocompletado de buscador. */
@Composable
private fun SearchResultsPanel(
    results: List<AppTarget>,
    query: String,
    addedPackages: Set<String>,
    onAdd: (AppTarget) -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    ) {
        if (results.isEmpty()) {
            Text(
                text = "Sin resultados para “$query”",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
            )
        } else {
            LazyColumn(modifier = Modifier.heightIn(max = 360.dp)) {
                items(results, key = { it.packageName }) { app ->
                    SearchResultRow(
                        app = app,
                        alreadyAdded = app.packageName in addedPackages,
                        onAdd = { onAdd(app) },
                    )
                    HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant)
                }
            }
        }
    }
}

/** Fila de resultado con icono real de la app instalada. */
@Composable
private fun SearchResultRow(
    app: AppTarget,
    alreadyAdded: Boolean,
    onAdd: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = !alreadyAdded, onClick = onAdd)
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        AppIcon(packageName = app.packageName, size = 40.dp)
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = app.displayName,
                style = MaterialTheme.typography.titleMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = app.packageName,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        if (alreadyAdded) {
            Icon(
                Icons.Default.Check,
                contentDescription = "Ya está en DCA",
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        } else {
            Icon(Icons.Default.Add, contentDescription = "Agregar ${app.displayName}")
        }
    }
}

/** Fila de app configurada: icono, switch, editar y quitar. */
@Composable
private fun TargetRow(
    target: AppTarget,
    onToggle: (Boolean) -> Unit,
    onEdit: () -> Unit,
    onRemove: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        AppIcon(packageName = target.packageName, size = 40.dp)
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = target.displayName,
                style = MaterialTheme.typography.titleMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = buildString {
                    append(if (target.cooldownSeconds == 0L) "Responde ya" else "Demora: ${target.cooldownSeconds}s")
                    if (target.repeatSeconds > 0L) append(" · Repetir: ${formatRepeatShort(target.repeatSeconds)}")
                    if (!target.enabled) append(" · Pausada")
                },
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        Switch(checked = target.enabled, onCheckedChange = onToggle, modifier = Modifier.padding(start = 4.dp))
        IconButtonCompat(
            icon = Icons.Default.Edit,
            description = "Editar ${target.displayName}",
            onClick = onEdit,
        )
        IconButtonCompat(
            icon = Icons.Default.Delete,
            description = "Quitar ${target.displayName}",
            onClick = onRemove,
        )
    }
}

/**
 * Icono real de la app instalada, cargado una vez por paquete y cacheado
 * durante la composición. Placeholder neutro si el icono no está disponible.
 */
@Composable
fun AppIcon(packageName: String, size: androidx.compose.ui.unit.Dp) {
    val context = LocalContext.current
    val bitmap = remember(packageName) {
        runCatching {
            context.packageManager.getApplicationIcon(packageName)
                .toBitmap()
                .asImageBitmap()
        }.getOrNull()
    }
    if (bitmap != null) {
        Image(
            bitmap = bitmap,
            contentDescription = null,
            modifier = Modifier
                .size(size)
                .clip(RoundedCornerShape(8.dp)),
        )
    } else {
        Box(
            modifier = Modifier
                .size(size)
                .clip(RoundedCornerShape(8.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = packageName.take(1).uppercase(),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

/** Formato corto del intervalo de repetición para listas. */
private fun formatRepeatShort(seconds: Long): String = when {
    seconds % 86_400L == 0L && seconds >= 86_400L -> "${seconds / 86_400L}d"
    seconds % 3_600L == 0L && seconds >= 3_600L -> "${seconds / 3_600L}h"
    seconds % 60L == 0L && seconds >= 60L -> "${seconds / 60L}min"
    else -> "${seconds}s"
}

/** Wrapper tipado para iconos de acción. */
@Composable
private fun IconButtonCompat(icon: ImageVector, description: String, onClick: () -> Unit) {
    androidx.compose.material3.IconButton(onClick = onClick) {
        Icon(icon, contentDescription = description)
    }
}

/** Botón claro (X) para vaciar el buscador. */
@Composable
private fun IconButtonClear(onClick: () -> Unit) {
    androidx.compose.material3.IconButton(onClick = onClick) {
        Icon(Icons.Default.Close, contentDescription = "Limpiar búsqueda")
    }
}