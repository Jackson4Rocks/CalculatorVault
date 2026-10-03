package com.leon.calculatorvault

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import java.io.File
import java.security.KeyStore
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.CipherOutputStream
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

data class VaultItem(
    val id: String,
    val name: String,
    val size: Long
) {
    val sizeLabel: String
        get() = when {
            size < 1024 -> size.toString() + " B"
            size < 1024 * 1024 -> (size / 1024).toString() + " KB"
            size < 1024L * 1024L * 1024L -> (size / (1024 * 1024)).toString() + " MB"
            else -> (size / (1024L * 1024L * 1024L)).toString() + " GB"
        }
}

class VaultStore(private val context: Context) {
    private val directory = File(context.filesDir, "vault").apply { mkdirs() }
    private val prefs = context.getSharedPreferences("vault_items", Context.MODE_PRIVATE)

    fun list(): List<VaultItem> {
        val array = org.json.JSONArray(prefs.getString("items", "[]") ?: "[]")
        return buildList {
            for (i in 0 until array.length()) {
                val item = array.getJSONObject(i)
                add(VaultItem(
                    item.getString("id"),
                    item.getString("name"),
                    item.getLong("size")
                ))
            }
        }
    }

    fun importUri(uri: Uri) {
        val id = java.util.UUID.randomUUID().toString()
        val output = File(directory, id + ".bin")
        val input = context.contentResolver.openInputStream(uri)
            ?: error("Cannot read selected file")

        input.use { source ->
            output.outputStream().use { raw ->
                val iv = ByteArray(12).also { SecureRandom().nextBytes(it) }
                raw.write(iv)

                val cipher = Cipher.getInstance("AES/GCM/NoPadding").apply {
                    init(
                        Cipher.ENCRYPT_MODE,
                        KeyStoreCrypto.key(),
                        GCMParameterSpec(128, iv)
                    )
                }

                CipherOutputStream(raw, cipher).use { encrypted ->
                    source.copyTo(encrypted, 64 * 1024)
                }
            }
        }

        val name = queryName(uri)
        val array = org.json.JSONArray(prefs.getString("items", "[]") ?: "[]")
        array.put(
            org.json.JSONObject()
                .put("id", id)
                .put("name", name)
                .put("size", output.length())
        )
        prefs.edit().putString("items", array.toString()).apply()
    }

    fun delete(item: VaultItem) {
        File(directory, item.id + ".bin").delete()

        val old = org.json.JSONArray(prefs.getString("items", "[]") ?: "[]")
        val replacement = org.json.JSONArray()
        for (i in 0 until old.length()) {
            val obj = old.getJSONObject(i)
            if (obj.getString("id") != item.id) replacement.put(obj)
        }
        prefs.edit().putString("items", replacement.toString()).apply()
    }

    private fun queryName(uri: Uri): String {
        context.contentResolver.query(
            uri,
            arrayOf(OpenableColumns.DISPLAY_NAME),
            null,
            null,
            null
        )?.use { cursor ->
            if (cursor.moveToFirst()) return cursor.getString(0)
        }
        return uri.lastPathSegment ?: "file"
    }
}

private object KeyStoreCrypto {
    private const val ALIAS = "calculator_vault_aes"

    fun key(): SecretKey {
        val store = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }

        if (store.containsAlias(ALIAS)) {
            return (store.getEntry(ALIAS, null) as KeyStore.SecretKeyEntry).secretKey
        }

        val generator = KeyGenerator.getInstance(
            android.security.keystore.KeyProperties.KEY_ALGORITHM_AES,
            "AndroidKeyStore"
        )

        generator.init(
            android.security.keystore.KeyGenParameterSpec.Builder(
                ALIAS,
                android.security.keystore.KeyProperties.PURPOSE_ENCRYPT or
                    android.security.keystore.KeyProperties.PURPOSE_DECRYPT
            )
                .setKeySize(256)
                .setBlockModes(android.security.keystore.KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(
                    android.security.keystore.KeyProperties.ENCRYPTION_PADDING_NONE
                )
                .build()
        )

        return generator.generateKey()
    }
}
