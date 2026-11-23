plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
}

android {
    namespace = "com.kjota.bajalo"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.kjota.bajalo"
        minSdk = 29
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"

        // Solo celulares reales (ARM). Reduce bastante el tamaño del APK.
        ndk {
            abiFilters.addAll(listOf("arm64-v8a", "armeabi-v7a"))
        }
    }

    buildTypes {
        release {
            // Se deja sin minificar: yt-dlp y Python usan reflexión y librerías nativas.
            isMinifyEnabled = false
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
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

    packaging {
        // Necesario para que yt-dlp / Python / ffmpeg se extraigan como binarios ejecutables.
        jniLibs {
            useLegacyPackaging = true
        }
    }
}

dependencies {
    // Motor de descargas (yt-dlp + Python + ffmpeg empaquetados para Android)
    implementation("io.github.junkfood02.youtubedl-android:library:0.18.1")
    implementation("io.github.junkfood02.youtubedl-android:ffmpeg:0.18.1")

    implementation("androidx.core:core-ktx:1.15.0")
    implementation("androidx.documentfile:documentfile:1.0.1")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.9.0")

    implementation("androidx.activity:activity-compose:1.9.3")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.7")
    implementation("androidx.lifecycle:lifecycle-viewmodel-ktx:2.8.7")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.7")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.8.7")

    implementation(platform("androidx.compose:compose-bom:2024.12.01"))
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.foundation:foundation")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-core")

    // Miniaturas (incluye cuadros de video para los estados de WhatsApp)
    implementation("io.coil-kt:coil-compose:2.7.0")
    implementation("io.coil-kt:coil-video:2.7.0")
}
