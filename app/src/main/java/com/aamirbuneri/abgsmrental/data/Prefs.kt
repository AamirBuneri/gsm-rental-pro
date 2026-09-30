package com.aamirbuneri.abgsmrental.data

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

private val Context.store: DataStore<Preferences> by preferencesDataStore(name = "session")

enum class ThemeMode { SYSTEM, LIGHT, DARK }

data class Settings(
    val site: String = "",
    val siteName: String = "",
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val notifications: Boolean = true,
    val signedIn: Boolean = false,
    val username: String = "",
)

/** App settings + the sign-in token (encrypted with a key that never leaves the phone's keystore). */
class Prefs(private val context: Context) {
    private object K {
        val site = stringPreferencesKey("site")
        val siteName = stringPreferencesKey("site_name")
        val theme = stringPreferencesKey("theme")
        val notifications = booleanPreferencesKey("notifications")
        val token = stringPreferencesKey("token")
        val username = stringPreferencesKey("username")
        val lastNotice = intPreferencesKey("last_notice")
        val askedPermission = booleanPreferencesKey("asked_notification_permission")
    }

    val settings: Flow<Settings> = context.store.data.map { p ->
        Settings(
            site = p[K.site].orEmpty(),
            siteName = p[K.siteName].orEmpty(),
            themeMode = runCatching { ThemeMode.valueOf(p[K.theme] ?: "SYSTEM") }.getOrDefault(ThemeMode.SYSTEM),
            notifications = p[K.notifications] ?: true,
            signedIn = !p[K.token].isNullOrEmpty(),
            username = p[K.username].orEmpty(),
        )
    }

    suspend fun snapshot(): Settings = settings.first()

    suspend fun site(): String = context.store.data.first()[K.site].orEmpty()

    suspend fun token(): String? = context.store.data.first()[K.token]?.let { TokenCipher.decrypt(it) }

    suspend fun setSite(site: String, name: String) = context.store.edit {
        it[K.site] = site
        it[K.siteName] = name
    }

    suspend fun setSiteName(name: String) = context.store.edit { it[K.siteName] = name }

    suspend fun signIn(token: String, username: String) = context.store.edit {
        it[K.token] = TokenCipher.encrypt(token)
        it[K.username] = username
        it[K.lastNotice] = 0
    }

    suspend fun signOut() = context.store.edit {
        it.remove(K.token)
        it[K.lastNotice] = 0
    }

    suspend fun setTheme(mode: ThemeMode) = context.store.edit { it[K.theme] = mode.name }

    suspend fun setNotifications(on: Boolean) = context.store.edit { it[K.notifications] = on }

    suspend fun lastNotice(): Int = context.store.data.first()[K.lastNotice] ?: 0

    suspend fun setLastNotice(id: Int) = context.store.edit { it[K.lastNotice] = id }

    suspend fun askedPermission(): Boolean = context.store.data.first()[K.askedPermission] ?: false

    suspend fun setAskedPermission() = context.store.edit { it[K.askedPermission] = true }
}

/** AES-GCM with an Android Keystore key. Falls back to plain storage on the rare phone with a broken keystore. */
private object TokenCipher {
    private const val ALIAS = "ab_gsm_session"
    private const val TRANSFORM = "AES/GCM/NoPadding"

    private fun key(): SecretKey {
        val ks = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
        (ks.getKey(ALIAS, null) as? SecretKey)?.let { return it }
        val gen = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore")
        gen.init(
            KeyGenParameterSpec.Builder(ALIAS, KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT)
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setKeySize(256)
                .build()
        )
        return gen.generateKey()
    }

    fun encrypt(plain: String): String = runCatching {
        val c = Cipher.getInstance(TRANSFORM)
        c.init(Cipher.ENCRYPT_MODE, key())
        val out = c.iv + c.doFinal(plain.toByteArray(Charsets.UTF_8))
        "k:" + Base64.encodeToString(out, Base64.NO_WRAP)
    }.getOrElse { "p:$plain" }

    fun decrypt(stored: String): String? = when {
        stored.startsWith("p:") -> stored.substring(2)
        stored.startsWith("k:") -> runCatching {
            val all = Base64.decode(stored.substring(2), Base64.NO_WRAP)
            val c = Cipher.getInstance(TRANSFORM)
            c.init(Cipher.DECRYPT_MODE, key(), GCMParameterSpec(128, all, 0, 12))
            String(c.doFinal(all, 12, all.size - 12), Charsets.UTF_8)
        }.getOrNull()
        else -> null
    }
}
