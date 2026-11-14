# DCA — Auto respuesta para mensajería

DCA es una app Android minimalista (blanco, negro y grises) que envía respuestas automáticas a las apps de mensajería que tú agregues a su lista. Construida con arquitectura VSA ( Vertical Slice Architecture ).

## Cómo funciona

DCA usa el permiso **Acceso a notificaciones** (NotificationListenerService) para detectar cuando una app de mensajería te envía una notificación de mensaje. Cuando detecta una, envía tu mensaje personalizado a través de la acción de **respuesta rápida** (RemoteInput) de la propia notificación — la misma vía que usan las respuestas inline del sistema Android.

- **Sin servidores. Sin cuentas. Sin nube.** Todo el procesamiento ocurre en tu dispositivo.
- **Ahorro de batería:** sin polling ni servicios en foreground — el código solo se ejecuta cuando llega una notificación.

## Requisitos y permisos

| Permiso / requisito | Por qué lo necesita DCA |
|---|---|
| **Acceso a notificaciones** (obligatorio) | Ver notificaciones entrantes de las apps que agregues. Es un permiso de sistema que se concede en Ajustes → Acceso especial → Acceso a notificaciones. La app te guía y lleva directo al panel correcto. |
| **Notificaciones de DCA** (recomendado) | Muestra una confirmación discreta cada vez que DCA responde, para trazabilidad total. |
| **Optimización de batería (exención)** (recomendado) | Algunos fabricantes (Xiaomi, Samsung, Huawei…) cierran apps en segundo plano agresivamente. La exención evita que el sistema detenga el listener de notificaciones. El diálogo se muestra desde la propia app. |
| **Recibir al reiniciar** (RECEIVE_BOOT_COMPLETED) | Reconecta el listener tras reiniciar el dispositivo sin intervención. |
| **La app objetivo debe tener respuesta rápida** | WhatsApp, Telegram, Signal, Messenger y la mayoría de apps de mensajería incluyen *quick reply* en sus notificaciones; es la vía por la que DCA envía el mensaje. |

> **Nota sobre ahorro de batería:** DCA no tiene procesos en segundo plano ni polling. El sistema Android despierta el listener únicamente cuando llega una notificación, por lo que su consumo en reposo es ~0.

## Funciones

- **Cualquier app instalada**: el buscador lista todas las apps del dispositivo (solo excluye DCA y apps sin interfaz). Nota: la respuesta automática solo funciona en apps cuyas notificaciones incluyan respuesta rápida (la mayoría de apps de mensajería).
- **Chat Bot (Encendido/Apagado por app)**: interruptor "Activar Chat Bot" en la sección "Mensaje de auto-respuesta" del editor de cada app. Encendido, DCA atiende al cliente automáticamente usando las preguntas frecuentes de DateComp — el mismo sistema de respuesta del chat bot del widget web: normaliza el mensaje (minúsculas, sin acentos), puntúa cada categoría sumando la longitud de las keywords coincidentes (las específicas pesan más, con plural simple) y responde la más cercana. Prioridades: pedir un humano → FAQ → saludo → despedida → "no encontrada" con opciones sugeridas. **Bilingüe ES/EN**: detecta el idioma del mensaje y resuelve hasta 15 categorías por idioma (13 FAQs del web + "horario de atención" y "app DCA"). Coherente con mensajería: el traspaso humano promete atención por turno abierto en el mismo chat (sin horario fijo: se trabaja por demanda), el contacto no remite de nuevo a WhatsApp y las referencias a la web incluyen la URL completa (datecomp.com). Con el bot encendido, el resto de opciones del editor quedan bloqueadas salvo la demora (el bot responde de forma automática a cada mensaje).
- **Mensaje por app**: personaliza el texto de auto-respuesta por aplicación (1–500 caracteres). Requerido solo con el Chat Bot apagado.
- **Cooldown (demora)**: segundos que DCA espera desde que llega la notificación antes de enviar la respuesta (0 = inmediata, máx. 1 h). Funciona igual con el Chat Bot encendido o apagado.
- **Repetir**: intervalo mínimo entre respuestas automáticas a la misma app — presets 30 min, 5 h, 1 día o valor manual en segundos (máx. 7 días). Aunque lleguen 50 mensajes, DCA responde como máximo una vez por intervalo. Bloqueado mientras el Chat Bot está encendido.
- **Detección de duplicados**: la misma notificación re-posteada no dispara doble respuesta.
- **Guía inicial**: explica cada permiso, por qué se pide, y estado concedido/requerido en tiempo real.
- **Modo oscuro automático** siguiendo al sistema, manteniendo la paleta blanco/negro/grises.

## Estructura (VSA)

```
DCA/
├─ app/src/main/java/com/datecomp/dca/
│  ├─ targets/               # Apps objetivo: contrato, persistencia, lista y editor
│  │  ├─ AppTarget.kt        # Modelo de dominio + validación + estado de repetición
│  │  ├─ TargetsStore.kt     # DataStore + JSON (edición atómica)
│  │  ├─ InstalledApps.kt    # PackageManager: apps con launcher
│  │  ├─ TargetsViewModel.kt
│  │  ├─ AppsScreen.kt
│  │  └─ EditTargetScreen.kt
│  ├─ autoreply/             # Decisión y envío de auto-respuestas
│  │  ├─ AutoReplyEngine.kt  # Motor puro (cooldown, repetición, duplicados)
│  │  ├─ NotificationKeys.kt # Firma de contenido
│  │  ├─ AutoReplyNotificationListener.kt
│  │  ├─ NotificationReplier.kt # Salida RemoteInput
│  │  ├─ ReplyNotifier.kt       # Confirmación visible
│  │  └─ BootCompletedReceiver.kt
│  ├─ chatbot/               # Chat bot FAQ bilingüe
│  │  ├─ ChatBot.kt
│  │  ├─ ChatBotKnowledge.kt
│  │  └─ ChatBotStore.kt
│  ├─ onboarding/            # Guía inicial de permisos
│  │  ├─ GuideScreen.kt
│  │  ├─ OnboardingStore.kt
│  │  └─ OnboardingViewModel.kt
│  ├─ permissions/           # Estado y solicitud de permisos del sistema
│  │  ├─ SystemPermissions.kt
│  │  └─ PermissionsViewModel.kt
│  ├─ shell/                 # Arranque, navegación y tema
│  │  ├─ DcaApp.kt
│  │  ├─ MainActivity.kt
│  │  ├─ DcaNavHost.kt
│  │  └─ theme/ (Theme.kt, Type.kt)
│  └─ LocalDataStore.kt      # Instancia única de DataStore
└─ app/src/test/.../         # Tests por slice: targets, autoreply y chatbot
```

## Requisitos para compilar

- JDK 17+
- Android SDK (platform 35, build-tools 35) — se puede auto-instalar con el wrapper:
  ```
  gradlew.bat testDebugUnitTest assembleDebug
  ```
- `local.properties` con `sdk.dir` apuntando al SDK (si no usas ANDROID_HOME).

## Compatibilidad

- **Android 6.0 (API 23) o superior**, objetivo API 35 (Android 15).
- NotificationListenerService disponible desde API 18; RemoteInput en notificaciones desde API 24 (API 23 sin garantía de quick-reply en todas las apps).
- Sin dependencias nativas: un solo APK universal.

## Estado del proyecto

- ✅ Compila correctamente (BUILD SUCCESSFUL).
- ✅ 55/55 pruebas unitarias pasando (motor de decisión, firma de notificaciones, contrato de app objetivo, ChatBot bilingüe y cobertura de preguntas frecuentes).
- ✅ APK release firmado compilado (DCA-v1.0.5.apk en /apk).
- ⏳ Sin validar en dispositivo/emulador real (no disponible en este entorno).
- ⏳ Sin validar en Play Store (el usuario decide distribución).