package com.nyx.minichat.network

import okhttp3.Cookie
import okhttp3.CookieJar
import okhttp3.HttpUrl

/**
 * In-memory cookie jar keyed by host. The web app relies on the browser to
 * carry the `gate` and `session` cookies automatically on every same-origin
 * request (see static/js/api.js comment). OkHttp does nothing by default,
 * so this reproduces that behavior for a single remote host.
 *
 * Prototype-grade: memory-only, cleared on process death. Fine since
 * MinichatUI sessions are long-lived server-side (10yr cookie) but the
 * *device* re-login isn't persisted across app restarts yet — acceptable
 * for a prototype, worth persisting (EncryptedSharedPreferences) later.
 */
class SimpleCookieJar : CookieJar {
    private val store = mutableMapOf<String, MutableList<Cookie>>()

    override fun saveFromResponse(url: HttpUrl, cookies: List<Cookie>) {
        val host = url.host
        val existing = store.getOrPut(host) { mutableListOf() }
        for (cookie in cookies) {
            existing.removeAll { it.name == cookie.name }
            existing.add(cookie)
        }
    }

    override fun loadForRequest(url: HttpUrl): List<Cookie> {
        return store[url.host]?.filter { it.expiresAt > System.currentTimeMillis() } ?: emptyList()
    }

    fun clear() = store.clear()

    fun hasCookie(host: String, name: String): Boolean =
        store[host]?.any { it.name == name } == true
}
