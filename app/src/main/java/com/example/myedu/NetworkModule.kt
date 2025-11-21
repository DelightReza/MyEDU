package com.example.myedu

import java.util.ArrayList
import java.util.HashMap
import java.util.concurrent.TimeUnit
import okhttp3.Cookie
import okhttp3.CookieJar
import okhttp3.HttpUrl
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.Response
import okhttp3.ResponseBody
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST

// DATA MODELS
data class LoginRequest(val email: String, val password: String)
data class LoginResponse(val status: String?, val authorisation: AuthData?)
data class AuthData(val token: String?, val is_student: Boolean?)

data class StudentInfoResponse(val pdsstudentinfo: Any?, val studentMovement: Any?, val avatar: String?)

// API
interface OshSuApi {
    @POST("public/api/login")
    suspend fun login(@Body request: LoginRequest): LoginResponse

    @GET("public/api/studentinfo")
    suspend fun getProfile(): StudentInfoResponse
}

// 1. COOKIE JAR (Essential for Session)
class WindowsCookieJar : CookieJar {
    private val cookieStore = HashMap<String, List<Cookie>>()

    override fun saveFromResponse(url: HttpUrl, cookies: List<Cookie>) {
        cookieStore[url.host()] = cookies
    }

    override fun loadForRequest(url: HttpUrl): List<Cookie> {
        return cookieStore[url.host()] ?: ArrayList()
    }
}

// 2. INTERCEPTOR (The Windows Disguise)
class WindowsInterceptor : Interceptor {
    // Holds the Bearer token once we get it
    var authToken: String? = null

    override fun intercept(chain: Interceptor.Chain): Response {
        val original = chain.request()
        val builder = original.newBuilder()

        // --- THE WINDOWS HEADER SUITE ---
        // This mimics Chrome 120 on Windows 10 exactly
        builder.header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36")
        builder.header("Accept", "application/json, text/plain, */*")
        builder.header("Accept-Language", "en-US,en;q=0.9,ru;q=0.8")
        builder.header("Accept-Encoding", "gzip, deflate, br")
        builder.header("Referer", "https://myedu.oshsu.kg/")
        builder.header("Origin", "https://myedu.oshsu.kg")
        
        // Modern Browser Security Headers (Crucial for bypassing WAFs)
        builder.header("sec-ch-ua", "\"Not_A Brand\";v=\"8\", \"Chromium\";v=\"120\", \"Google Chrome\";v=\"120\"")
        builder.header("sec-ch-ua-mobile", "?0")
        builder.header("sec-ch-ua-platform", "\"Windows\"")
        builder.header("Sec-Fetch-Dest", "empty")
        builder.header("Sec-Fetch-Mode", "cors")
        builder.header("Sec-Fetch-Site", "same-site")
        
        // X-Headers often required by Laravel/Vue apps
        builder.header("X-Requested-With", "XMLHttpRequest")

        // Inject Token if we have it
        authToken?.let { token ->
            builder.header("Authorization", "Bearer $token")
        }

        return chain.proceed(builder.build())
    }
}

object NetworkClient {
    private const val BASE_URL = "https://api.myedu.oshsu.kg/" 
    
    private val cookieJar = WindowsCookieJar()
    val interceptor = WindowsInterceptor() // Accessible so we can set token later

    private val client = OkHttpClient.Builder()
        .cookieJar(cookieJar)
        .addInterceptor(interceptor)
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .build()

    val api: OshSuApi = Retrofit.Builder()
        .baseUrl(BASE_URL)
        .client(client)
        .addConverterFactory(GsonConverterFactory.create())
        .build()
        .create(OshSuApi::class.java)
}
