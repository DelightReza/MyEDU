package myedu.oshsu.kg

import android.content.Context
import android.content.SharedPreferences
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.launch
import myedu.oshsu.kg.database.MyEduRepository

class PrefsManager(private val context: Context) {
    // @PublishedApi makes these accessible to the inline function below
    @PublishedApi
    internal val prefs: SharedPreferences = context.getSharedPreferences("myedu_offline_cache", Context.MODE_PRIVATE)
    
    @PublishedApi
    internal val gson = Gson()
    
    // Repository for Room Database
    private val repository = MyEduRepository(context)

    // --- AUTH TOKEN MANAGEMENT ---
    fun saveToken(token: String) {
        prefs.edit().putString("auth_token", token).apply()
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

