# Bajalo — notas para Claude Code

App Android (Kotlin + Jetpack Compose) para descargar videos con yt-dlp. Paquete: `com.kjota.bajalo`. Idioma de la interfaz: español.

## Estado
El proyecto fue escrito **sin poder compilarse** (entorno sin acceso al Android SDK ni a Maven). La lógica (data, engine, download, ViewModel)
se verificó con el compilador de Kotlin contra el android.jar real, pero **la capa de UI (Compose) y la configuración de Gradle nunca se compilaron**.
La primera tarea es compilar y corregir lo que falle.

## Comandos
- Compilar APK debug: `./gradlew assembleDebug` → `app/build/outputs/apk/debug/app-debug.apk`
- Requisitos: JDK 17, Android SDK platform 35 (si falta, instalarlo con Android Studio o `sdkmanager "platforms;android-35"`).

## Si algo falla al sincronizar/compilar
- Versiones usadas: AGP 8.7.3, Gradle 8.9, Kotlin 2.0.21 (plugin `org.jetbrains.kotlin.plugin.compose`), Compose BOM 2024.12.01, Coil 2.7.0,
  youtubedl-android 0.18.1 (`io.github.junkfood02.youtubedl-android:library` y `:ffmpeg`, en Maven Central). Se pueden subir si hace falta.
- La librería youtubedl-android exige `android:extractNativeLibs="true"` (manifest) y `useLegacyPackaging = true` (app/build.gradle.kts). No quitarlos.
- `minSdk = 29` a propósito (MediaStore con RELATIVE_PATH sin permisos de almacenamiento).

## Decisiones de diseño (no cambiar sin motivo)
- Las descargas corren en `DownloadService` (servicio en primer plano, tipo dataSync) y publican su estado en `DownloadCenter` (StateFlow).
- Se descarga a `cacheDir/dl/<id>` y luego se copia a la galería con `MediaSaver` (MediaStore). Nada de rutas directas.
- Estados de WhatsApp: carpeta `.Statuses` leída con SAF (`OpenDocumentTree` + permiso persistente), nunca con rutas directas.
- Sesión para historias: WebView (`LoginScreen`) → cookies → `filesDir/cookies.txt` (formato Netscape) → `--cookies` de yt-dlp.
  `CookieManager` solo se toca desde el hilo principal.
- No se implementa (ni se debe implementar) nada para saltarse DRM (Netflix, Disney+, etc.).
- Sin anuncios ni analítica: no agregar SDKs de publicidad ni de telemetría.

## Diseño

Tema oscuro fijo "aurora": fondo de malla de degradados animado, tarjetas de vidrio (translúcidas con borde de luz) y bento grid en la pantalla principal. Ver ui/Aurora.kt.

## Actualizaciones desde la app
`update/Updater.kt` consulta `releases/latest` del repo en GitHub (dysasterkj/Bajalo, debe ser público), compara el número de build (`versionCode` = número de ejecución de Actions = último número del tag `v1.0.N`),
descarga el APK a `cacheDir/updates` y abre el instalador con FileProvider (`REQUEST_INSTALL_PACKAGES`). Aparece en Ajustes → "Actualizaciones", con aviso al abrir la app.
