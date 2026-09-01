package com.example.phoenx.ui.util

import android.util.LruCache

/**
 * Cache mémoire pour les octets déchiffrés (v9.7.9)
 * Évite de redéchiffrer les mêmes médias lors du scroll dans une LazyColumn.
 */
object DecryptedCache {
    // Cache de 20 Mo pour les octets déchiffrés
    private val cache = LruCache<String, ByteArray>(20 * 1024 * 1024)

    fun get(key: String): ByteArray? = cache.get(key)

    fun put(key: String, bytes: ByteArray) {
        cache.put(key, bytes)
    }

    fun clear() {
        cache.evictAll()
    }
}
