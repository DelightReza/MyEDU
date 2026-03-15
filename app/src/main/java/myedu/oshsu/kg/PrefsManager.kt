package myedu.oshsu.kg

import android.content.Context
import android.content.SharedPreferences
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.launch
import myedu.oshsu.kg.database.MyEduRepository

class PrefsManager(private val context: Context) {
    @PublishedApi
    internal val prefs: SharedPreferences = context.getSharedPreferences(AppConstants.PREFS_NAME, Context.MODE_PRIVATE)
    
    @PublishedApi
    internal val gson = Gson()
    
    private val repository = MyEduRepository(context)

    fun saveToken(token: String) {
        prefs.edit().putString(AppConstants.KEY_AUTH_TOKEN, token).apply()
    }

    fun getToken(): String? {
        return prefs.getString(AppConstants.KEY_AUTH_TOKEN, null)
    }

    fun clearAll() {
        prefs.edit().clear().apply()
        kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.IO).launch {
            try {
                repository.clearAll()
            } catch (e: Exception) { }
        }
    }

    fun <T> saveData(key: String, data: T) {
        val json = gson.toJson(data)
        prefs.edit().putString(key, json).apply()
    }

    fun <T> loadData(key: String, type: Class<T>): T? {
        val json = prefs.getString(key, null) ?: return null
        return try {
            gson.fromJson(json, type)
        } catch (e: Exception) {
            null
        }
    }

    fun <T> saveList(key: String, list: List<T>) {
        val json = gson.toJson(list)
        prefs.edit().putString(key, json).apply()
    }

    inline fun <reified T> loadList(key: String): List<T> {
        val json = prefs.getString(key, null) ?: return emptyList()
        val type = object : TypeToken<List<T>>() {}.type
        return try {
            gson.fromJson(json, type)
        } catch (e: Exception) {
            emptyList()
        }
    }
    
    fun getRepository(): MyEduRepository = repository
}

