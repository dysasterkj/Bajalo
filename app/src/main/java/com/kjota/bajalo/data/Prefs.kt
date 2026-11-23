package com.kjota.bajalo.data

import android.content.Context

/** Preferencias simples guardadas solo en el teléfono. */
object Prefs {
    private fun sp(ctx: Context) =
        ctx.applicationContext.getSharedPreferences("bajalo", Context.MODE_PRIVATE)

    fun getString(ctx: Context, key: String): String? = sp(ctx).getString(key, null)

    fun putString(ctx: Context, key: String, value: String?) {
        sp(ctx).edit().putString(key, value).apply()
    }

    fun getLong(ctx: Context, key: String, def: Long = 0L): Long = sp(ctx).getLong(key, def)

    fun putLong(ctx: Context, key: String, value: Long) {
        sp(ctx).edit().putLong(key, value).apply()
    }

    fun getStringSet(ctx: Context, key: String): Set<String> =
        sp(ctx).getStringSet(key, emptySet())?.toSet() ?: emptySet()

    fun putStringSet(ctx: Context, key: String, value: Set<String>) {
        sp(ctx).edit().putStringSet(key, HashSet(value)).apply()
    }
}
