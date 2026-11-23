# Bajalo — descargador de videos sin anuncios (Android)

App Android para descargar videos de YouTube, Instagram, Facebook, X, TikTok, Reddit, Vimeo, Twitch y más de mil sitios,
además de **estados de WhatsApp** e **historias** de Instagram/Facebook. Sin anuncios, sin rastreadores, sin cuentas.

Motor: [yt-dlp](https://github.com/yt-dlp/yt-dlp) + [youtubedl-android](https://github.com/yausername/youtubedl-android) (ya vienen dentro de la app).

## Cómo convertirla en una app instalable (APK)

> Este proyecto se escribió sin poder compilarlo (el entorno donde se creó no puede bajar el Android SDK).
> Es normal que la primera compilación pida corregir algún detalle: Claude Code lo arregla en un par de pasos.

### Opción A — con Claude Code (la más fácil)

1. Descomprime el .zip en una carpeta, por ejemplo `C:\Proyectos\Bajalo`.
2. Abre Claude Code en esa carpeta y pega esto:

   ```
   Lee CLAUDE.md. Compila este proyecto Android con ./gradlew assembleDebug.
   Si hay errores, corrígelos hasta que compile. Al final dime dónde quedó el APK.
   ```

### Opción B — con Android Studio

1. Instala [Android Studio](https://developer.android.com/studio) (trae el SDK y Java).
2. `File → Open` y elige la carpeta del proyecto. Espera a que termine "Gradle sync".
3. Menú `Build → Build Bundle(s) / APK(s) → Build APK(s)`.

El APK queda en: `app/build/outputs/apk/debug/app-debug.apk`

## Instalarla en tu celular

1. Pasa `app-debug.apk` al teléfono (cable USB, Drive, WhatsApp a ti mismo, etc.).
2. Ábrelo. Android te pedirá permitir "instalar apps de origen desconocido" para ese origen: acepta.
3. Abre **Bajalo**. La primera vez tarda unos segundos en "preparar el motor".

Requiere Android 10 o superior.

## Cómo se usa

- **Descargar:** copia el enlace del video, pégalo en Bajalo y toca *Analizar* → elige calidad → *Descargar*.
  Atajo: desde Instagram, Facebook, X, YouTube o TikTok toca **Compartir → Bajalo** y el enlace se analiza solo.
- **Estados de WhatsApp:** pestaña *Estados*. Primero mira los estados en WhatsApp, luego conecta la carpeta una sola vez
  (toca «Usar esta carpeta» → «Permitir») y elige los que quieras guardar.
- **Historias de Instagram/Facebook:** *Ajustes → Cuentas → Iniciar sesión*, y luego pega el enlace de la historia
  (por ejemplo `https://www.instagram.com/stories/usuario/`). La sesión se queda solo en tu teléfono.
- **Descargas:** todo queda en tu galería, carpetas `Movies/Bajalo`, `Pictures/Bajalo` y `Music/Bajalo`.
- Si algún sitio deja de funcionar: *Ajustes → Actualizar ahora* (también se actualiza solo una vez al día).

## Límites (a propósito)

- **No** descarga contenido con protección anticopia (DRM): Netflix, Disney+, Prime Video, Max, etc.
- Úsala con contenido tuyo o con permiso de su autor, y respeta las condiciones de cada plataforma.
- Las historias y cuentas privadas solo se pueden bajar con **tu propia** sesión iniciada.

## Estructura del código

```
app/src/main/java/com/kjota/bajalo/
  MainActivity.kt, MainViewModel.kt, BajaloApp.kt
  engine/    Engine (arranque y actualización de yt-dlp), Prober (analiza enlaces), Accounts (sesiones/cookies)
  download/  DownloadService (descarga en segundo plano), DownloadCenter (estado compartido)
  data/      MediaSaver (guarda en la galería), WhatsAppStatuses (lee la carpeta de estados), Prefs
  ui/        Pantallas en Jetpack Compose: Home, Status, Library, Settings, Login
```

## Diseño

Tema oscuro fijo "aurora": fondo de malla de degradados animado, tarjetas de vidrio (translúcidas con borde de luz) y bento grid en la pantalla principal. Ver ui/Aurora.kt.
