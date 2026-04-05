package myedu.oshsu.kg

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

/**
 * Manages the list of saved (logged-in) accounts.
 *
 * Account list is persisted in the "myedu_accounts_enc" EncryptedSharedPreferences
 * file so that credentials (email, password, token) are not stored in plaintext.
 * On first run, any data found in the legacy plaintext "myedu_accounts" file is
 * migrated transparently and that file is cleared.
 *
 * Per-account session data (used for background-sync comparison) lives in
 * a separate "myedu_cache_{id}" SharedPreferences file per account so it
 * never collides with the active account's primary cache.
 */
class AccountManager(private val context: Context) {

    private val prefs: SharedPreferences = createEncryptedPrefs(context)
    private val gson = Gson()

    // ── Account list ─────────────────────────────────────────────────────────

    fun getAllAccounts(): List<SavedAccount> {
        val json = prefs.getString(KEY_ACCOUNTS, null) ?: return emptyList()
        return try {
            gson.fromJson(json, object : TypeToken<List<SavedAccount>>() {}.type)
        } catch (e: Exception) {
            emptyList()
        }
    }

    fun saveOrUpdateAccount(account: SavedAccount) {
        val accounts = getAllAccounts().toMutableList()
        val idx = accounts.indexOfFirst { it.id == account.id }
        if (idx >= 0) accounts[idx] = account else accounts.add(account)
        prefs.edit().putString(KEY_ACCOUNTS, gson.toJson(accounts)).apply()
    }

    fun removeAccount(id: String) {
        val accounts = getAllAccounts().toMutableList()
        accounts.removeAll { it.id == id }
        val editor = prefs.edit().putString(KEY_ACCOUNTS, gson.toJson(accounts))
        if (getActiveAccountId() == id) editor.remove(KEY_ACTIVE_ID)
        editor.apply()

        // Clear the per-account session cache so no user data lingers on disk
        getAccountPrefs(id).edit().clear().apply()
    }

    // ── Active account ────────────────────────────────────────────────────────

    fun getActiveAccountId(): String? = prefs.getString(KEY_ACTIVE_ID, null)

    fun getActiveAccount(): SavedAccount? {
        val id = getActiveAccountId() ?: return null
        return getAllAccounts().find { it.id == id }
    }

    fun setActiveAccount(id: String) {
        prefs.edit().putString(KEY_ACTIVE_ID, id).apply()
    }

    // ── Per-account session cache (for background sync comparison) ───────────

    /**
     * Returns a SharedPreferences file dedicated to [accountId].
     * Background sync stores the last-seen session_list here so it can
     * detect grade/portal changes without touching the active account's cache.
     */
    fun getAccountPrefs(accountId: String): SharedPreferences =
        context.getSharedPreferences("myedu_cache_$accountId", Context.MODE_PRIVATE)

    companion object {
        private const val KEY_ACCOUNTS = "accounts_list"
        private const val KEY_ACTIVE_ID = "active_account_id"

        /**
         * Creates (or opens) the encrypted SharedPreferences used to store account
         * credentials. If a legacy plaintext "myedu_accounts" prefs file exists its
         * contents are migrated into the encrypted file and then cleared.
         *
         * Falls back to the plaintext file if the encrypted prefs cannot be created
         * (e.g. on emulators without hardware-backed key storage).
         */
        private fun createEncryptedPrefs(context: Context): SharedPreferences {
            return try {
                val masterKey = MasterKey.Builder(context)
                    .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                    .build()
                val encryptedPrefs = EncryptedSharedPreferences.create(
                    context,
                    "myedu_accounts_enc",
                    masterKey,
                    EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                    EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
                )
                // Migrate from legacy plaintext prefs if any data exists there
                val legacyPrefs = context.getSharedPreferences("myedu_accounts", Context.MODE_PRIVATE)
                if (legacyPrefs.all.isNotEmpty()) {
                    val editor = encryptedPrefs.edit()
                    for ((key, value) in legacyPrefs.all) {
                        when (value) {
                            is String -> editor.putString(key, value)
                            is Boolean -> editor.putBoolean(key, value)
                            is Int -> editor.putInt(key, value)
                            is Long -> editor.putLong(key, value)
                            is Float -> editor.putFloat(key, value)
                            is Set<*> -> {
                                @Suppress("UNCHECKED_CAST")
                                editor.putStringSet(key, value as Set<String>)
                            }
                        }
                    }
                    editor.apply()
                    legacyPrefs.edit().clear().apply()
                }
                encryptedPrefs
            } catch (e: Exception) {
                // Fallback: use plaintext prefs (e.g. emulator without hardware security)
                context.getSharedPreferences("myedu_accounts", Context.MODE_PRIVATE)
            }
        }
    }
}
