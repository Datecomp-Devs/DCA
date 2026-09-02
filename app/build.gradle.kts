plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
    id("org.jetbrains.kotlin.plugin.serialization")
}

// Firma de release: credenciales en keystore/keystore.properties (no versionado).
// En CI (sin keystore) queda vacío y el release se genera sin firmar; la
// publicación de Releases usa el APK pre-compilado firmado en /apk.
val keystoreProps: Map<String, String> = buildMap {
    val f = rootProject.file("keystore/keystore.properties")
    if (f.exists()) {
        f.readLines().forEach { line ->
            val idx = line.indexOf('=')
            if (idx > 0) put(line.substring(0, idx).trim(), line.substring(idx + 1).trim())
        }
    }
}

android {
    namespace = "com.datecomp.dca"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.datecomp.dca"
        minSdk = 23
        targetSdk = 35
        versionCode = 6
        versionName = "1.0.5"

        // Fallback a un nombre de APK determinista y estable.
        setProperty("archivesBaseName", "DCA-v$versionName")
    }

    signingConfigs {
        if (keystoreProps.isNotEmpty()) {
            create("release") {
                storeFile = rootProject.file(keystoreProps["STORE_FILE"] ?: "")
                storePassword = keystoreProps["STORE_PASSWORD"] ?: ""
                keyAlias = keystoreProps["KEY_ALIAS"] ?: ""
                keyPassword = keystoreProps["KEY_PASSWORD"] ?: ""
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            if (keystoreProps.isNotEmpty()) {
                signingConfig = signingConfigs.getByName("release")
            }
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = "17"
    }
    buildFeatures {
        compose = true
    }
}

dependencies {
    val composeBom = platform("androidx.compose:compose-bom:2024.12.01")
    implementation(composeBom)

    implementation("androidx.core:core-ktx:1.15.0")
    implementation("androidx.core:core-splashscreen:1.0.1")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.7")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.8.7")
    implementation("androidx.activity:activity-compose:1.9.3")
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended")
    implementation("androidx.datastore:datastore-preferences:1.1.1")
    implementation("androidx.navigation:navigation-compose:2.8.5")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.7.3")

    testImplementation("junit:junit:4.13.2")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.9.0")

    androidTestImplementation("androidx.test.ext:junit:1.2.1")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.6.1")
    androidTestImplementation(composeBom)
    androidTestImplementation("androidx.compose.ui:ui-test-junit4")

    debugImplementation("androidx.compose.ui:ui-tooling")
    debugImplementation("androidx.compose.ui:ui-test-manifest")
}