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

// We no longer need Login API here, WebView handles it.
interface OshSuApi {
    @GET("public/api/user")
    suspend fun getUser(): ResponseBody
}

// GLOBAL STORE FOR STOLEN CREDENTIALS
object CredentialStore {
    var cookies: String? = null
    var userAgent: String? = null
}

class HybridInterceptor : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val original = chain.request()
        val builder = original.newBuilder()

        // Use the EXACT Cookies and UA from the WebView
        CredentialStore.cookies?.let { builder.header("Cookie", it) }
        CredentialStore.userAgent?.let { builder.header("User-Agent", it) }
        
        // If we found the JWT inside the cookie, add it as Bearer too (Double-Lock)
        CredentialStore.cookies?.let { cookies ->
            val jwt = cookies.split(";").find { it.trim().startsWith("myedu-jwt-token=") }
            if (jwt != null) {
                val token = jwt.substringAfter("=").trim()
                builder.header("Authorization", "Bearer $token")
            }
        }

        builder.header("Referer", "https://myedu.oshsu.kg/")
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
