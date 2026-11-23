package com.kjota.bajalo.util

/** Convierte los errores técnicos de yt-dlp en mensajes que se entienden. */
object Errors {

    fun friendly(t: Throwable): String {
        val raw = (t.message ?: t.toString()).trim()
        val m = raw.lowercase()

        val text = when {
            "drm" in m ->
                "Este contenido tiene protección anticopia (DRM), como Netflix, Disney+ o Prime Video. No se puede descargar."

            "youtube" in m && ("confirm you" in m || "not a bot" in m || "sign in" in m) ->
                "YouTube pidió verificación. Espera un rato, actualiza el motor en Ajustes e inténtalo otra vez."

            "login required" in m || "log in" in m || "sign in" in m || "cookies" in m ||
                "private" in m || "requested content is not available" in m ||
                "restricted video" in m || "empty media response" in m ->
                "Este contenido necesita que inicies sesión. Ve a Ajustes → Cuentas, entra a tu cuenta de Instagram, Facebook o X y vuelve a intentarlo."

            "unsupported url" in m ->
                "Este enlace no es compatible o no contiene un video."

            "no video could be found" in m || "no video formats" in m ->
                "No encontré ningún video en ese enlace."

            "unable to resolve" in m || "network is unreachable" in m || "timed out" in m ||
                "connection reset" in m || "name or service not known" in m ->
                "No hay conexión a internet (o es muy lenta). Revísala e inténtalo de nuevo."

            "429" in m || "too many requests" in m || "rate limit" in m || "rate-limit" in m ->
                "La red pide esperar un poco por demasiadas solicitudes. Intenta de nuevo en unos minutos."

            "video unavailable" in m || "not available" in m || "has been removed" in m ||
                "does not exist" in m || "404" in m ->
                "El video no está disponible (puede ser privado, estar eliminado o bloqueado en tu país)."

            "no space left" in m ->
                "Tu teléfono no tiene espacio suficiente."

            else -> null
        }

        if (text != null) return text

        val lastError = raw.lines().lastOrNull { it.contains("ERROR", ignoreCase = true) } ?: raw
        return "No pude descargar este enlace. Prueba actualizar el motor en Ajustes.\n\nDetalle: ${lastError.take(220)}"
    }
}
