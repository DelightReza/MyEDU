package myedu.oshsu.kg

/**
 * Manages user settings: theme, language, download mode, custom profile.
 */
class SettingsManager(private val prefs: PrefsManager) {

    fun loadTheme(): String? = prefs.loadData(AppConstants.KEY_THEME_MODE, String::class.java)
    fun saveTheme(mode: String) { prefs.saveData(AppConstants.KEY_THEME_MODE, mode) }

    fun loadDocMode(): String? = prefs.loadData(AppConstants.KEY_DOC_DOWNLOAD_MODE, String::class.java)
    fun saveDocMode(mode: String) { prefs.saveData(AppConstants.KEY_DOC_DOWNLOAD_MODE, mode) }

    fun loadLanguage(): String? = prefs.loadData(AppConstants.KEY_LANGUAGE, String::class.java)?.replace("\"", "")
    fun saveLanguage(lang: String) { prefs.saveData(AppConstants.KEY_LANGUAGE, lang) }

    fun loadCustomName(): String? = prefs.loadData(AppConstants.KEY_CUSTOM_NAME, String::class.java)
    fun loadCustomPhoto(): String? = prefs.loadData(AppConstants.KEY_CUSTOM_PHOTO, String::class.java)

    fun saveLocalProfile(name: String?, photoUri: String?) {
        prefs.saveData(AppConstants.KEY_CUSTOM_NAME, name)
        prefs.saveData(AppConstants.KEY_CUSTOM_PHOTO, photoUri)
    }

    fun loadRememberMe(): Boolean = prefs.loadData(AppConstants.KEY_REMEMBER_ME, Boolean::class.java) ?: false
    fun loadSavedEmail(): String = prefs.loadData(AppConstants.KEY_SAVED_EMAIL, String::class.java) ?: ""
    fun loadSavedPass(): String = prefs.loadData(AppConstants.KEY_SAVED_PASS, String::class.java) ?: ""

    fun loadShowWidgetPromotion(): Boolean = prefs.loadData(AppConstants.KEY_SHOW_WIDGET_PROMOTION, Boolean::class.java) ?: true
    fun saveShowWidgetPromotion(value: Boolean) { prefs.saveData(AppConstants.KEY_SHOW_WIDGET_PROMOTION, value) }
}
