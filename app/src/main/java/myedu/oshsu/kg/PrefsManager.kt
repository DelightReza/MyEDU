package myedu.oshsu.kg

import android.content.Context
import android.content.SharedPreferences
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.launch
import myedu.oshsu.kg.database.MyEduRepository

class PrefsManager(private val context: Context, accountId: String = "default") {
    // @PublishedApi makes these accessible to the inline function below
    @PublishedApi
    internal val prefs: SharedPreferences = run {
        val targetName = "myedu_offline_cache_$accountId"
        val targetPrefs = context.getSharedPreferences(targetName, Context.MODE_PRIVATE)
        // One-time migration: if the account-specific file is empty, copy data from the
        // legacy single-file cache ("myedu_offline_cache") or from the intermediate
        // "myedu_offline_cache_default" file so existing users don't lose their data.
        if (targetPrefs.all.isEmpty()) {
            val legacy = context.getSharedPreferences("myedu_offline_cache", Context.MODE_PRIVATE)
            val source = if (legacy.all.isNotEmpty()) legacy
                         else context.getSharedPreferences("myedu_offline_cache_default", Context.MODE_PRIVATE)
            if (source.all.isNotEmpty()) {
                val editor = targetPrefs.edit()
                for ((key, value) in source.all) {
                    when (value) {
                        is String  -> editor.putString(key, value)
                        is Boolean -> editor.putBoolean(key, value)
                        is Int     -> editor.putInt(key, value)
                        is Long    -> editor.putLong(key, value)
                        is Float   -> editor.putFloat(key, value)
                        is Set<*>  -> @Suppress("UNCHECKED_CAST") editor.putStringSet(key, value as Set<String>)
                    }
                }
                editor.apply()
                source.edit().clear().apply()
            }
        }
        targetPrefs
    }
    
    @PublishedApi
    internal val gson = Gson()
    
    // Repository for Room Database
    private val repository = MyEduRepository(context)

    // --- AUTH TOKEN MANAGEMENT ---
    fun saveToken(token: String) {
        prefs.edit().putString("auth_token", token).apply()
    }

    fun clearToken() {
        prefs.edit().remove("auth_token").apply()
    }

    fun getToken(): String? {
        return prefs.getString("auth_token", null)
    }

    fun clearAll() {
        prefs.edit().clear().apply()
        // Also clear Room Database - use application scope for cleanup
        kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.IO).launch {
            try {
                repository.clearAll()
            } catch (e: Exception) {
                // Ignore errors during cleanup
            }
        }
    }

    // --- DATA SAVING (GENERIC) ---
    fun <T> saveData(key: String, data: T) {
        val json = gson.toJson(data)
        prefs.edit().putString(key, json).apply()
    }

    // --- DATA LOADING (GENERIC) ---
    fun <T> loadData(key: String, type: Class<T>): T? {
        val json = prefs.getString(key, null) ?: return null
        return try {
            gson.fromJson(json, type)
        } catch (e: Exception) {
            null
        }
    }

    // --- LIST SAVING ---
    fun <T> saveList(key: String, list: List<T>) {
        val json = gson.toJson(list)
        prefs.edit().putString(key, json).apply()
    }

    // --- LIST LOADING ---
    inline fun <reified T> loadList(key: String): List<T> {
        val json = prefs.getString(key, null) ?: return emptyList()
        val type = object : TypeToken<List<T>>() {}.type
        return try {
            gson.fromJson(json, type)
        } catch (e: Exception) {
            emptyList()
        }
    }
    
    // --- ROOM DATABASE ACCESS ---
    fun getRepository(): MyEduRepository = repository
}

