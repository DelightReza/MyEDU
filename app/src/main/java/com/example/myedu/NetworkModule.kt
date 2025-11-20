package com.example.myedu

import java.util.concurrent.TimeUnit
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.Response
import okhttp3.ResponseBody
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.Headers
import retrofit2.http.POST

// 1. API DEFINITION
data class LoginRequest(val email: String, val password: String)
data class LoginResponse(val status: String?, val authorisation: AuthData?)
data class AuthData(val token: String?, val is_student: Boolean?)

interface OshSuApi {
    @Headers("Content-Type: application/json", "Accept: application/json")
    @POST("public/api/login")
    suspend fun login(@Body request: LoginRequest): LoginResponse

    @GET("public/api/user")
    suspend fun getUser(): ResponseBody

    // --- GRADE SCANNER ENDPOINTS ---
    @GET("public/api/studentSession")
    suspend fun scanSession(): ResponseBody

    @GET("public/api/studentCurricula")
    suspend fun scanCurricula(): ResponseBody

    @GET("public/api/student_mark_list")
    suspend fun scanMarkList(): ResponseBody

    @GET("public/api/student/transcript")
    suspend fun scanTranscript(): ResponseBody
}

// 2. GLOBAL TOKEN STORE
object TokenStore {
    var jwtToken: String? = null
}

// 3. INTERCEPTOR (The "Fake Browser" Engine)
class BrowserInterceptor : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val original = chain.request()
        val builder = original.newBuilder()

        // A. SPOOF HEADERS (Look like Chrome)
        builder.header("User-Agent", "Mozilla/5.0 (Linux; Android 10; K) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/142.0.0.0 Mobile Safari/537.36")
        builder.header("Referer", "https://myedu.oshsu.kg/")
        builder.header("Accept", "application/json, text/plain, */*")
        builder.header("Connection", "keep-alive")

        // B. INJECT CREDENTIALS (If we have logged in)
        TokenStore.jwtToken?.let { token ->
            // 1. Authorization Header
            builder.header("Authorization", "Bearer $token")

            // 2. Generate Timestamp (Just like the browser cookie 'my_edu_update')
            val sdf = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.000000'Z'", Locale.US)
            sdf.timeZone = TimeZone.getTimeZone("UTC")
            val timestamp = sdf.format(Date())

            // 3. Cookie Header (Manual Injection)
            // This is what the browser's JS does automatically
            val cookieStr = "myedu-jwt-token=$token; my_edu_update=$timestamp"
            builder.header("Cookie", cookieStr)
        }

        return chain.proceed(builder.build())
    }
}

object NetworkClient {
    private const val BASE_URL = "https://api.myedu.oshsu.kg/" 

    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .addInterceptor(BrowserInterceptor()) // Plug in our fake browser engine
        .build()

    val api: OshSuApi = Retrofit.Builder()
        .baseUrl(BASE_URL)
        .client(client)
        .addConverterFactory(GsonConverterFactory.create())
        .build()
        .create(OshSuApi::class.java)
}
