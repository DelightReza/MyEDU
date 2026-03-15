package myedu.oshsu.kg

import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

/**
 * Manages the user's custom dictionary: load, save, add, remove, reset.
 */
class DictionaryManager(private val prefs: PrefsManager) {

    private val dictUtils = DictionaryUtils()

    fun load(): Map<String, String> {
        val savedJson = prefs.loadData(AppConstants.KEY_CUSTOM_DICTIONARY, String::class.java)
        return if (savedJson != null) {
            try {
                val type = object : TypeToken<Map<String, String>>() {}.type
                Gson().fromJson(savedJson, type)
            } catch (e: Exception) {
                val default = dictUtils.getDefaultDictionary()
                save(default)
                default
            }
        } else {
            val default = dictUtils.getDefaultDictionary()
            save(default)
            default
        }
    }

    fun save(dictionary: Map<String, String>) {
        prefs.saveData(AppConstants.KEY_CUSTOM_DICTIONARY, Gson().toJson(dictionary))
    }

    fun addOrUpdate(dictionary: Map<String, String>, key: String, value: String): Map<String, String> {
        val mutable = dictionary.toMutableMap()
        mutable[key.trim()] = value.trim()
        save(mutable)
        return mutable
    }

    fun remove(dictionary: Map<String, String>, key: String): Map<String, String> {
        val mutable = dictionary.toMutableMap()
        mutable.remove(key)
        save(mutable)
        return mutable
    }

    fun resetToDefault(): Map<String, String> {
        val default = dictUtils.getDefaultDictionary()
        save(default)
        return default
    }
}
