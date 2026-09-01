package com.datecomp.dca.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.draw.alpha
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.datecomp.dca.core.AppTarget

/**
 * Editor por app. Primera sección: "Mensaje de auto-respuesta", que incluye
 * el interruptor "Activar Chat Bot". Con el chat bot encendido el bot
 * responde automáticamente según el mensaje del cliente (mismo sistema de
 * matching que el chat bot web de DateComp), por lo que el mensaje
 * personalizado, "Repetir" y demás quedan bloqueados; solo la demora antes
 * de responder (cooldown) sigue siendo editable.
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun EditTargetScreen(
    viewModel: AppViewModel,
    packageName: String,
    onBack: () -> Unit,
) {
    val state by viewModel.appsState.collectAsState()
    val target = state.targets.firstOrNull { it.packageName == packageName }

    var message by remember(target?.packageName) { mutableStateOf(target?.replyMessage.orEmpty()) }
    var cooldown by remember(target?.packageName) { mutableStateOf(target?.cooldownSeconds ?: 0L) }
    var repeat by remember(target?.packageName) { mutableStateOf(target?.repeatSeconds ?: 0L) }
    var chatBot by remember(target?.packageName) { mutableStateOf(target?.chatBotEnabled ?: false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(target?.displayName ?: "App") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver")
                    }
                },
            )
        },
    ) { padding ->
        if (target == null) {
            Text(
                text = "Esta app ya no está en la lista.",
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(24.dp),
            )
            return@Scaffold
        }

        val chatBotValid = message.length <= AppTarget.MAX_REPLY_LENGTH
        val messageValid = message.isNotEmpty() && message.length in 1..AppTarget.MAX_REPLY_LENGTH
        val cooldownValid = cooldown in AppTarget.MIN_COOLDOWN_SECONDS..AppTarget.MAX_COOLDOWN_SECONDS
        val repeatValid = repeat in AppTarget.MIN_REPEAT_SECONDS..AppTarget.MAX_REPEAT_SECONDS
        // Con el bot encendido el mensaje ya no es requerido (se genera).
        val canSave = if (chatBot) chatBotValid && cooldownValid && repeatValid
        else messageValid && cooldownValid && repeatValid

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
        ) {
            // ─── Mensaje de auto-respuesta + Activar Chat Bot ────────
            Text(
                text = "Mensaje de auto-respuesta",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
            )
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    text = "Activar Chat Bot",
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.weight(1f, fill = false),
                )
                Spacer(modifier = Modifier.width(12.dp))
                Switch(
                    checked = chatBot,
                    onCheckedChange = { chatBot = it },
                )
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = if (chatBot) {
                    "Encendido: responde automáticamente a cada pregunta del cliente."
                } else {
                    "Apagado: se envía el mensaje personalizado de abajo."
                },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            if (chatBot) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "El Chat Bot responde según el mensaje del cliente usando las preguntas frecuentes de DateComp. Si el cliente pide hablar con una persona, DCA le avisa y traspasa la conversación.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedTextField(
                value = message,
                onValueChange = { message = it.take(AppTarget.MAX_REPLY_LENGTH) },
                modifier = Modifier
                    .fillMaxWidth()
                    .alpha(if (chatBot) 0.5f else 1f),
                minLines = 3,
                isError = !chatBot && message.isNotEmpty() && !messageValid,
                supportingText = { Text("${message.length}/${AppTarget.MAX_REPLY_LENGTH} caracteres") },
                enabled = !chatBot,
                label = if (chatBot) {
                    { Text("Gestionado por el Chat Bot") }
                } else {
                    null
                },
            )

            Spacer(modifier = Modifier.height(24.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant)
            Spacer(modifier = Modifier.height(24.dp))

            // ─── Cooldown (demora antes de responder) ────────────────
            Text(
                text = "Cooldown — demora antes de responder",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Segundos que DCA esperará desde que llega la notificación antes de enviar la respuesta.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = formatDelay(cooldown),
                style = MaterialTheme.typography.headlineMedium,
            )
            Slider(
                value = cooldown.toFloat(),
                onValueChange = { cooldown = it.toLong() },
                valueRange = 0f..60f,
            )
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                OutlinedTextField(
                    value = cooldown.toString(),
                    onValueChange = { raw ->
                        cooldown = raw.filter { it.isDigit() }.toLongOrNull()
                            ?.coerceIn(AppTarget.MIN_COOLDOWN_SECONDS, AppTarget.MAX_COOLDOWN_SECONDS)
                            ?: 0L
                    },
                    label = { Text("Segundos") },
                    modifier = Modifier.width(140.dp),
                    singleLine = true,
                )
                listOf(0L, 5L, 15L, 30L, 60L).forEach { preset ->
                    FilterChip(
                        selected = cooldown == preset,
                        onClick = { cooldown = preset },
                        label = { Text(if (preset == 0L) "Ya" else "${preset}s") },
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant)
            Spacer(modifier = Modifier.height(24.dp))

            // ─── Repetir (intervalo entre respuestas) ────────────────
            Text(
                text = "Repetir — cada cuánto reenviar",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = if (chatBot) {
                    "Bloqueado con el Chat Bot activado: responde a cada mensaje del cliente."
                } else {
                    "Tiempo mínimo entre respuestas automáticas a esta app. Ej.: 30 min responde como máximo una vez cada media hora, aunque lleguen más mensajes."
                },
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = formatRepeat(repeat),
                style = MaterialTheme.typography.headlineMedium,
                modifier = Modifier.alpha(if (chatBot) 0.5f else 1f),
            )
            Spacer(modifier = Modifier.height(8.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                // Presets del usuario: 30 min, 5 h, 1 día.
                listOf(
                    1_800L to "30m",
                    18_000L to "5h",
                    86_400L to "1d",
                    0L to "Siempre",
                ).forEach { (seconds, label) ->
                    FilterChip(
                        selected = repeat == seconds,
                        onClick = { repeat = seconds },
                        enabled = !chatBot,
                        label = { Text(label) },
                        modifier = Modifier.weight(1f),
                    )
                }
            }
            OutlinedTextField(
                value = repeat.toString(),
                onValueChange = { raw ->
                    repeat = raw.filter { it.isDigit() }.toLongOrNull()
                        ?.coerceIn(AppTarget.MIN_REPEAT_SECONDS, AppTarget.MAX_REPEAT_SECONDS)
                        ?: 0L
                },
                label = { Text("Segundos exactos (avanzado)") },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp)
                    .alpha(if (chatBot) 0.5f else 1f),
                singleLine = true,
                enabled = !chatBot,
            )

            Spacer(modifier = Modifier.height(32.dp))

            Button(
                onClick = {
                    viewModel.updateChatBotEnabled(target.packageName, chatBot)
                    viewModel.updateReplyMessage(target.packageName, message)
                    viewModel.updateCooldown(target.packageName, cooldown)
                    viewModel.updateRepeat(target.packageName, repeat)
                    onBack()
                },
                enabled = canSave,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("Guardar")
            }

            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = if (chatBot) {
                    "Chat Bot encendido: responde automáticamente a las preguntas del cliente. La demora se aplica antes de cada respuesta."
                } else {
                    "Nota: si la app reenvía la misma notificación, DCA la descarta como duplicada. La demora se cancela si la app se detiene."
                },
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

/** Formato humano para la demora: "0s (inmediata)", "15s", "60s". */
private fun formatDelay(seconds: Long): String =
    if (seconds == 0L) "Inmediata" else "$seconds s"

/** Formato humano para el intervalo: "30 min", "5 h", "1 día"... */
private fun formatRepeat(seconds: Long): String {
    if (seconds == 0L) return "Siempre responde"
    val h = seconds / 3600
    val m = (seconds % 3600) / 60
    val s = seconds % 60
    return when {
        h > 0 && m > 0 -> "$h h $m min"
        h > 0 -> if (h == 1L) "1 hora" else "$h horas"
        m > 0 -> "$m min"
        else -> "$s s"
    }
}