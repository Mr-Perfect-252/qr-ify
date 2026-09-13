import java.util.Properties

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

// ---------------------------------------------------------------------------
// Optional release signing.
// The signing material is provided either via a local `keystore.properties`
// file (never committed) or via environment variables in CI (populated from
// GitHub Actions secrets by the "Build APK" workflow). If neither is present,
// release builds fall back to the debug signing config so the build never
// breaks for contributors who don't hold the keystore.
// ---------------------------------------------------------------------------
val keystorePropertiesFile = rootProject.file("keystore.properties")
val keystoreProperties = Properties().apply {
    if (keystorePropertiesFile.exists()) {
        keystorePropertiesFile.inputStream().use { load(it) }
    }
}

fun signingValue(propKey: String, envKey: String): String? =
    (keystoreProperties[propKey] as String?)?.takeIf { it.isNotBlank() }
        ?: System.getenv(envKey)?.takeIf { it.isNotBlank() }

val ksStoreFile = signingValue("storeFile", "SIGNING_STORE_FILE")
val ksStorePassword = signingValue("storePassword", "SIGNING_STORE_PASSWORD")
val ksKeyAlias = signingValue("keyAlias", "SIGNING_KEY_ALIAS")
val ksKeyPassword = signingValue("keyPassword", "SIGNING_KEY_PASSWORD")
val hasReleaseSigning =
    ksStoreFile != null && ksStorePassword != null && ksKeyAlias != null && ksKeyPassword != null

android {
    namespace = "com.qrify.app"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.qrify.app"
        minSdk = 24
        targetSdk = 34
        versionCode = 1
        versionName = "1.0.0"

        // ApexHub OTA configuration. Replace with your real values from the
        // ApexHub Console (Settings tab). Left as placeholders so the project
        // compiles out of the box; update-checks simply no-op until set.
        buildConfigField("String", "APEXHUB_PUBLIC_KEY", "\"${project.findProperty("APEXHUB_PUBLIC_KEY") ?: "pk_live_REPLACE_ME"}\"")
        buildConfigField("String", "APEXHUB_APP_ID", "\"${project.findProperty("APEXHUB_APP_ID") ?: "app_REPLACE_ME"}\"")
        buildConfigField("String", "APEXHUB_PACKAGE", "\"com.qrify.app\"")
    }

    signingConfigs {
        if (hasReleaseSigning) {
            create("release") {
                storeFile = file(ksStoreFile!!)
                storePassword = ksStorePassword
                keyAlias = ksKeyAlias
                keyPassword = ksKeyPassword
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            signingConfig = if (hasReleaseSigning) {
                signingConfigs.getByName("release")
            } else {
                signingConfigs.getByName("debug")
            }
        }
        debug {
            applicationIdSuffix = ".debug"
            versionNameSuffix = "-debug"
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
        viewBinding = true
        buildConfig = true
    }
}

dependencies {
    // ApexHub Android SDK — OTA auto-updates, background checks, analytics.
    // Published on Maven Central; pulls its own transitive deps (okhttp,
    // gson, WorkManager, coroutines, appcompat).
    implementation("io.github.mr-perfect-252:sdk:1.0.0")

    // QR code generation (pure-Java, no camera/scanner needed).
    implementation("com.google.zxing:core:3.5.3")

    implementation("androidx.core:core-ktx:1.13.1")
    implementation("androidx.appcompat:appcompat:1.7.0")
    implementation("com.google.android.material:material:1.12.0")
    implementation("androidx.constraintlayout:constraintlayout:2.1.4")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.4")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.8.1")
}
