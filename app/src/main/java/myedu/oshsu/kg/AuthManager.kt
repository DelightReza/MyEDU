package myedu.oshsu.kg

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Handles authentication operations: login, logout, credential persistence.
 */
class AuthManager(private val prefs: PrefsManager) {

    suspend fun login(email: String, pass: String, rememberMe: Boolean): LoginResult = withContext(Dispatchers.IO) {
        NetworkClient.cookieJar.clear()
        NetworkClient.interceptor.authToken = null

        val normalizedEmail = EmailHelper.normalizeEmail(email)
        persistCredentials(normalizedEmail, pass, rememberMe)

        try {
            val resp = NetworkClient.api.login(LoginRequest(normalizedEmail.trim(), pass.trim()))
            val token = resp.authorisation?.token
            if (token != null) {
                prefs.saveToken(token)
                NetworkClient.interceptor.authToken = token
                NetworkClient.cookieJar.injectSessionCookies(token)
                LoginResult.Success(token, normalizedEmail)
            } else {
                LoginResult.InvalidCredentials
            }
        } catch (e: Exception) {
            LoginResult.Error(e)
        }
    }

    fun logout(
        themeMode: String,
        downloadMode: String,
        language: String,
        dictionaryJson: String,
        rememberMe: Boolean,
        email: String,
        pass: String
    ) {
        prefs.clearAll()
        NetworkClient.cookieJar.clear()
        NetworkClient.interceptor.authToken = null

        // Restore user preferences
        prefs.saveData(AppConstants.KEY_THEME_MODE, themeMode)
        prefs.saveData(AppConstants.KEY_DOC_DOWNLOAD_MODE, downloadMode)
        prefs.saveData(AppConstants.KEY_LANGUAGE, language)
        prefs.saveData(AppConstants.KEY_CUSTOM_DICTIONARY, dictionaryJson)

        if (rememberMe) {
            prefs.saveData(AppConstants.KEY_REMEMBER_ME, true)
            prefs.saveData(AppConstants.KEY_SAVED_EMAIL, email)
            prefs.saveData(AppConstants.KEY_SAVED_PASS, pass)
        }
    }

    fun restoreSession(): String? {
        val token = prefs.getToken()
        if (token != null) {
            NetworkClient.interceptor.authToken = token
            NetworkClient.cookieJar.injectSessionCookies(token)
        }
        return token
    }

    private fun persistCredentials(email: String, pass: String, rememberMe: Boolean) {
        if (rememberMe) {
            prefs.saveData(AppConstants.KEY_REMEMBER_ME, true)
            prefs.saveData(AppConstants.KEY_SAVED_EMAIL, email)
            prefs.saveData(AppConstants.KEY_SAVED_PASS, pass)
        } else {
            prefs.saveData(AppConstants.KEY_REMEMBER_ME, false)
            prefs.saveData(AppConstants.KEY_SAVED_EMAIL, "")
            prefs.saveData(AppConstants.KEY_SAVED_PASS, "")
        }
    }

    sealed class LoginResult {
        data class Success(val token: String, val normalizedEmail: String) : LoginResult()
        data object InvalidCredentials : LoginResult()
        data class Error(val exception: Exception) : LoginResult()
    }
}
