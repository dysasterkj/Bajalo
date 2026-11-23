package com.kjota.bajalo.engine

import android.content.Context
import android.webkit.CookieManager
import java.io.File
import java.net.URI

/** Redes donde se puede iniciar sesión para descargar historias o contenido que lo exija. */
enum class Site(
    val label: String,
    val loginUrl: String,
    val cookieUrls: List<String>,
    /** Cookie que solo existe cuando hay sesión iniciada. */
    val marker: String,
) {
    INSTAGRAM(
        "Instagram",
        "https://www.instagram.com/accounts/login/",
        listOf("https://www.instagram.com"),
        "sessionid",
    ),
    FACEBOOK(
        "Facebook",
        "https://www.facebook.com/login",
        listOf("https://www.facebook.com", "https://m.facebook.com"),
        "c_user",
    ),
    X(
        "X (Twitter)",
        "https://x.com/i/flow/login",
        listOf("https://x.com", "https://twitter.com"),
        "auth_token",
    ),
}

/**
 * Maneja las sesiones iniciadas dentro de la app (WebView) y las convierte
 * en un archivo cookies.txt privado para yt-dlp.
 *
 * IMPORTANTE: CookieManager debe usarse desde el hilo principal.
 */
object Accounts {
    private const val FILE_NAME = "cookies.txt"

    private fun hasCookie(url: String, name: String): Boolean {
        return try {
            val raw = CookieManager.getInstance().getCookie(url) ?: return false
            raw.split(";").any { it.trim().startsWith("$name=") }
        } catch (_: Throwable) {
            // WebView no disponible en el teléfono: se trata como "sin sesión".
            false
        }
    }

    fun isLoggedIn(site: Site): Boolean = site.cookieUrls.any { hasCookie(it, site.marker) }

    fun loggedInSites(): Set<Site> = Site.values().filter { isLoggedIn(it) }.toSet()

    fun flush() {
        CookieManager.getInstance().flush()
    }

    /** Cierra la sesión borrando las cookies de esa red. */
    fun logout(site: Site) {
        val cm = CookieManager.getInstance()
        for (url in site.cookieUrls) {
            val raw = cm.getCookie(url) ?: continue
            val domain = "." + baseDomain(url)
            raw.split(";").forEach { part ->
                val name = part.substringBefore("=").trim()
                if (name.isNotEmpty()) {
                    cm.setCookie(url, "$name=; Max-Age=0; Path=/")
                    cm.setCookie(url, "$name=; Max-Age=0; Path=/; Domain=$domain")
                }
            }
        }
        cm.flush()
    }

    private fun baseDomain(url: String): String {
        val host = URI(url).host ?: url
        return host.removePrefix("www.").removePrefix("m.")
    }

    /**
     * Escribe filesDir/cookies.txt solo con las redes donde hay sesión iniciada.
     * Si no hay ninguna, borra el archivo. Llamar desde el hilo principal.
     */
    fun refreshCookieFile(ctx: Context) {
        val file = File(ctx.filesDir, FILE_NAME)
        val expiry = System.currentTimeMillis() / 1000 + 365L * 24 * 3600
        val seen = HashSet<String>()
        val sb = StringBuilder("# Netscape HTTP Cookie File\n")
        var count = 0
        for (site in Site.values()) {
            if (!isLoggedIn(site)) continue
            for (url in site.cookieUrls) {
                val raw = CookieManager.getInstance().getCookie(url) ?: continue
                val domain = "." + baseDomain(url)
                raw.split(";").forEach { part ->
                    val p = part.trim()
                    val idx = p.indexOf('=')
                    if (idx > 0) {
                        val name = p.substring(0, idx)
                        val value = p.substring(idx + 1)
                        if (seen.add("$domain|$name")) {
                            sb.append(domain).append('\t')
                                .append("TRUE").append('\t')
                                .append('/').append('\t')
                                .append("TRUE").append('\t')
                                .append(expiry).append('\t')
                                .append(name).append('\t')
                                .append(value).append('\n')
                            count++
                        }
                    }
                }
            }
        }
        if (count == 0) {
            file.delete()
        } else {
            file.writeText(sb.toString())
        }
    }

    fun cookieFileOrNull(ctx: Context): File? {
        val f = File(ctx.filesDir, FILE_NAME)
        return if (f.exists() && f.length() > 0) f else null
    }
}
