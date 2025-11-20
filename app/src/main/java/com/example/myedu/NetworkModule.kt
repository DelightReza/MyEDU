package com.example.myedu

import java.util.concurrent.TimeUnit
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET

// --- COMPLEX DATA MODELS (Based on your JSON) ---

data class StudentInfoResponse(
    val pdsstudentinfo: PersonalInfo?,
    val studentMovement: StudentMovement?,
    val avatar: String?
)

data class PersonalInfo(
    val id: Long,
    val passport_number: String?,
    val pin: String?,
    val birthday: String?,
    val phone: String?,
    val address: String?,
    val father_full_name: String?,
    val mother_full_name: String?
)

data class StudentMovement(
    val avn_group_name: String?,
    val speciality: NameData?,
    val faculty: NameData?,
    val edu_form: NameData?,
    val payment_form: NameData?,
    val info: String? // e.g. Order Number
)

data class NameData(
    val name_ru: String?,
    val name_en: String?,
    val name_kg: String?
) {
    // Helper to get best available name
    fun get(): String = name_en ?: name_ru ?: name_kg ?: "Unknown"
}

// --- API DEFINITION ---

interface OshSuApi {
    @GET("public/api/studentinfo")
    suspend fun getStudentInfo(): StudentInfoResponse
}

// --- HYBRID SESSION ENGINE ---

object SessionStore {
    var cookies: String? = null
    var userAgent: String? = null
}

class HybridInterceptor : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val original = chain.request()
        val builder = original.newBuilder()

        SessionStore.userAgent?.let { builder.header("User-Agent", it) }
        SessionStore.cookies?.let { builder.header("Cookie", it) }
        
        SessionStore.cookies?.let { cookies ->
            val jwt = cookies.split(";").find { it.trim().startsWith("myedu-jwt-token=") }
            if (jwt != null) {
                val token = jwt.substringAfter("=").trim()
                builder.header("Authorization", "Bearer $token")
            }
        }

        builder.header("Referer", "https://myedu.oshsu.kg/#/main")
        builder.header("Accept", "application/json, text/plain, */*")

        return chain.proceed(builder.build())
    }
}

object NetworkClient {
    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .addInterceptor(HybridInterceptor())
        .build()

    val api: OshSuApi = Retrofit.Builder()
        .baseUrl("https://api.myedu.oshsu.kg/")
        .client(client)
        .addConverterFactory(GsonConverterFactory.create())
        .build()
        .create(OshSuApi::class.java)
}
