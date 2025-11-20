package com.example.myedu

import java.util.concurrent.TimeUnit
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.Response
import okhttp3.ResponseBody
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import retrofit2.http.Headers

// API DEFINITION (Only Data Endpoints)
interface OshSuApi {
    @GET("public/api/user")
    suspend fun getUser(): ResponseBody

    // --- GRADE SCANNER ---
    @GET("public/api/studentSession")
    suspend fun scanSession(): ResponseBody

    @GET("public/api/studentCurricula")
    suspend fun scanCurricula(): ResponseBody

    @GET("public/api/student_mark_list")
    suspend fun scanMarkList(): ResponseBody

    @GET("public/api/student/transcript")
    suspend fun scanTranscript(): ResponseBody
    
    @GET("public/api/studentPayStatus")
    suspend fun scanPayStatus(): ResponseBody
}

// SESSION STORE
object SessionStore {
    var cookies: String? = null
    var userAgent: String? = null
}

// HYBRID INTERCEPTOR
class HybridInterceptor : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val original = chain.request()
        val builder = original.newBuilder()

        // A. Inject Stolen Headers
        SessionStore.userAgent?.let { builder.header("User-Agent", it) }
        SessionStore.cookies?.let { builder.header("Cookie", it) }
        
        // B. Inject Bearer Token (Extracted from Cookie for double-lock)
        SessionStore.cookies?.let { cookies ->
            val jwt = cookies.split(";").find { it.trim().startsWith("myedu-jwt-token=") }
            if (jwt != null) {
                val token = jwt.substringAfter("=").trim()
                builder.header("Authorization", "Bearer $token")
            }
        }

        // C. Standard Browser Headers
        builder.header("Referer", "https://myedu.oshsu.kg/#/main")
        builder.header("Accept", "application/json, text/plain, */*")

        return chain.proceed(builder.build())
    }
}

object NetworkClient {
    private const val BASE_URL = "https://api.myedu.oshsu.kg/" 

    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .addInterceptor(HybridInterceptor())
        .build()

    val api: OshSuApi = Retrofit.Builder()
        .baseUrl(BASE_URL)
        .client(client)
        .addConverterFactory(GsonConverterFactory.create())
        .build()
        .create(OshSuApi::class.java)
}
