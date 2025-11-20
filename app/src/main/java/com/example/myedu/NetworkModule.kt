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

interface OshSuApi {
    // We skip login. We only hit the data endpoints.
    @GET("public/api/user")
    suspend fun getUser(): ResponseBody
    
    // Adding specific grade endpoints to test blindly
    @GET("public/api/studentPayStatus") 
    suspend fun getPayStatus(): ResponseBody
}

object TokenStore {
    var manualToken: String = ""
}

class ManualAuthInterceptor : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val original = chain.request()
        val builder = original.newBuilder()

        // 1. HEADER MIMICRY
        builder.header("User-Agent", "Mozilla/5.0 (Linux; Android 10; K) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/142.0.0.0 Mobile Safari/537.36")
        builder.header("Referer", "https://myedu.oshsu.kg/#/main")
        builder.header("Accept", "application/json, text/plain, */*")

        // 2. INJECT THE PASTED TOKEN
        if (TokenStore.manualToken.isNotEmpty()) {
            val t = TokenStore.manualToken.trim()
            builder.header("Authorization", "Bearer $t")
            // Some servers require the cookie too, so we send both to be safe
            builder.header("Cookie", "myedu-jwt-token=$t")
        }

        return chain.proceed(builder.build())
    }
}

object NetworkClient {
    private const val BASE_URL = "https://api.myedu.oshsu.kg/" 

    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .addInterceptor(ManualAuthInterceptor())
        .build()

    val api: OshSuApi = Retrofit.Builder()
        .baseUrl(BASE_URL)
        .client(client)
        .addConverterFactory(GsonConverterFactory.create())
        .build()
        .create(OshSuApi::class.java)
}
