package myedu.oshsu.kg

import okhttp3.Cookie
import okhttp3.CookieJar
import okhttp3.HttpUrl
import java.text.SimpleDateFormat
import java.util.*

class UniversalCookieJar : CookieJar {
    private val cookieStore = mutableListOf<Cookie>()
    private val lock = Any()

    override fun saveFromResponse(url: HttpUrl, cookies: List<Cookie>) {
        synchronized(lock) {
            val names = cookies.map { it.name }
            cookieStore.removeAll { it.name in names }
            cookieStore.addAll(cookies)
        }
    }

    override fun loadForRequest(url: HttpUrl): List<Cookie> = synchronized(lock) { ArrayList(cookieStore) }

    fun injectSessionCookies(token: String) {
        val sdf = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.000000'Z'", Locale.US).apply { timeZone = TimeZone.getTimeZone("UTC") }
        synchronized(lock) {
            cookieStore.removeAll { it.name == AppConstants.COOKIE_JWT || it.name == AppConstants.COOKIE_LAST_UPDATE || it.name == AppConstants.COOKIE_2FA }
            cookieStore.add(Cookie.Builder().domain(AppConstants.COOKIE_DOMAIN).path(AppConstants.COOKIE_PATH).name(AppConstants.COOKIE_JWT).value(token).build())
            cookieStore.add(Cookie.Builder().domain(AppConstants.COOKIE_DOMAIN).path(AppConstants.COOKIE_PATH).name(AppConstants.COOKIE_LAST_UPDATE).value(sdf.format(Date())).build())
            cookieStore.add(Cookie.Builder().domain(AppConstants.COOKIE_DOMAIN).path(AppConstants.COOKIE_PATH).name(AppConstants.COOKIE_2FA).value(AppConstants.COOKIE_2FA_VALUE).build())
        }
    }

    fun clear() { synchronized(lock) { cookieStore.clear() } }
}
