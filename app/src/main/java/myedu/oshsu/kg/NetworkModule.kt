package myedu.oshsu.kg

import com.google.gson.GsonBuilder
import com.google.gson.annotations.SerializedName
import java.util.ArrayList
import java.util.concurrent.TimeUnit
import okhttp3.*
import okio.Buffer
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.*
import java.nio.charset.Charset
import java.text.SimpleDateFormat
import java.util.*

// --- AUTH & USER MODELS ---
data class LoginRequest(val email: String, val password: String)
data class LoginResponse(val status: String?, val authorisation: AuthData?)
data class AuthData(val token: String?, val is_student: Boolean?)
data class UserResponse(val user: UserData?)
data class UserData(
    val id: Long, 
    val name: String?, 
    val last_name: String?, 
    val father_name: String?, 
    val email: String?,
    val email2: String?, 
    val id_avn_student: Long?, 
    val is_pds_approval: Boolean?, 
    val id_avn: Long?, 
    val id_aryz: Long?, 
    val id_user: Long?, 
    val id_university: Long?, 
    val created_at: String?, 
    val updated_at: String? 
)

// --- DICTIONARY MODELS ---
data class DictionaryItem(
    val id: Int,
    val name_ru: String?,
    val name_en: String?,
    val name_kg: String?,
    // Country uses this:
    @SerializedName("id_integration_citizenship") val idIntegrationCitizenship: Int? = null,
    // Nationality/Region uses this:
    @SerializedName("id_integration") val idIntegration: Int? = null
) {
    fun getName(lang: String): String {
        return when (lang) {
            "ky" -> name_kg ?: name_ru ?: name_en ?: "-"
            "en" -> name_en ?: name_ru ?: name_kg ?: "-"
            else -> name_ru ?: name_kg ?: name_en ?: "-"
        }
    }
}

data class PeriodItem(
    val id: Int,
    val name_ru: String?,
    val name_en: String?,
    val name_kg: String?,
    val start: String?,
    val end: String?,
    val active: Boolean?
) {
    fun getName(lang: String): String = when (lang) {
        "ky" -> name_kg ?: name_ru ?: "-"
        "en" -> name_en ?: name_ru ?: "-"
        else -> name_ru ?: "-"
    }
}

// --- PROFILE MODELS ---
data class StudentInfoResponse(
    @SerializedName("pdsstudentinfo") val pdsstudentinfo: PdsInfo?,
    @SerializedName("studentMovement") val studentMovement: MovementInfo?,
    @SerializedName("pdsstudentmilitary") val pdsstudentmilitary: MilitaryInfo?,
    @SerializedName("avatar") val avatar: String?,
    @SerializedName("active_semester") val active_semester: Int?,
    @SerializedName("active_semesters") val active_semesters: List<NameObj>?, 
    @SerializedName("is_library_debt") val is_library_debt: Boolean?,
    @SerializedName("access_debt_credit_count") val access_debt_credit_count: Int?,
    @SerializedName("studentlibrary") val studentlibrary: List<Any>?,
    @SerializedName("student_debt_transcript") val student_debt_transcript: List<Any>?,
    @SerializedName("total_price") val total_price: List<Any>?
)

data class MilitaryInfo(
    val id: Int?,
    val name: String?,
    val name_military: String?,
    val serial_number: String?,
    val date: String?,
    val created_at: String?,
    val updated_at: String?,
    val id_student: Long?,
    val id_user: Long?
)

data class PdsInfo(
    val id: Long?,
    val id_student: Long?,
    val passport_number: String?, 
    val serial: String?,
    val birthday: String?, 
    val phone: String?,
    val address: String?,
    val residence_phone: String?,
    val marital_status: Int?,
    val is_ethnic: Boolean?,
    val id_male: Int?,
    val id_citizenship: Int?,
    val id_nationality: Int?,
    val pin: String?,
    val is_have_document: Boolean?,
    val release_organ: String?,
    val release_date: String?,
    val info: String?,
    val father_full_name: String?,
    val father_phone: String?,
    val father_info: String?,
    val mother_full_name: String?,
    val mother_phone: String?,
    val mother_info: String?,
    val birth_address: String?,
    val id_country: Int?,
    val id_oblast: Int?,
    val id_region: Int?,
    val id_birth_country: Int?,
    val id_birth_oblast: Int?,
    val id_birth_region: Int?,
    val residence_address: String?,
    val id_residence_country: Int?,
    val id_residence_oblast: Int?,
    val id_residence_region: Int?,
    val id_user: Long?,
    val created_at: String?,
    val updated_at: String?,
    val id_exam_type: Int?,
    val id_round: Int?
) {
    fun getFullPassport(): String {
        val s = (serial ?: "").trim(); val n = (passport_number ?: "").trim()
        if (s.isEmpty()) return n; if (n.isEmpty()) return s
        if (s.endsWith(n)) return s
        return "$s$n"
    }
}

data class MovementInfo(
    val id: Long?,
    val id_movement: Long?,
    val id_speciality: Int?, 
    val id_edu_form: Int?, 
    val avn_group_name: String?, 
    val speciality: NameObj?, 
    val faculty: NameObj?, 
    val edu_form: NameObj?,
    val id_payment_form: Int?,
    val payment_form: NameObj?,
    val period: PeriodItem?,
    val movement_info: MovementType?,
    val language: NameObj?,
    val date_movement: String?,
    val info_description: String?,
    val info: String?,
    val itngyrg: Any?, 
    val id_state_language_level: Int?,
    val id_edu_year: Int?,
    val id_import_archive_user: Int?,
    val citizenship: Int?,
    val id_oo1: Int?,
    val id_zo1: Int?,
    val created_at: String?,
    val updated_at: String?,
    val id_university: Long?,
    val id_tariff_type: Int?,
    val id_avn_group: Int?,
    val id_period: Int?
)

data class MovementType(
    val name_ru: String?,
    val name_en: String?,
    val name_kg: String?,
    val is_student: Boolean?
) {
    fun getName(lang: String): String = when (lang) {
        "ky" -> name_kg ?: name_ru ?: "-"
        "en" -> name_en ?: name_ru ?: "-"
        else -> name_ru ?: "-"
    }
}

data class NameObj(
    val id: Int?,
    val name: String?,
    val name_en: String?, 
    val name_ru: String?, 
    val name_kg: String?,
    val short_name: String?,
    val short_name_en: String?,
    val short_name_ru: String?,
    val short_name_kg: String?,
    val code: String?,
    val faculty: NameObj?,
    val status: Boolean?,
    val id_faculty_type: Int?,
    val id_direction: Int?,
    val id_oo1: Int?,
    val id_zo1: Int?,
    val id_user: Long?,
    val id_university: Long?,
    val created_at: String?,
    val info_ru: String?,
    val info_en: String?,
    val info_kg: String?,
    val number_name: String? 
) {
    fun get(lang: String = "en"): String = when (lang) {
        "ky" -> name_kg ?: name_ru ?: name_en ?: name ?: "Unknown"
        "ru" -> name_ru ?: name_kg ?: name_en ?: name ?: "Unknown"
        else -> name_en ?: name_ru ?: name_kg ?: name ?: "Unknown"
    }
    
    fun getName(lang: String): String = get(lang)
    
    fun getShortName(lang: String): String? = when(lang) {
        "ky" -> short_name_kg ?: short_name_ru ?: short_name_en ?: short_name
        "en" -> short_name_en ?: short_name_ru ?: short_name
        else -> short_name_ru ?: short_name
    }

    fun getInfo(lang: String): String = when(lang) {
        "ky" -> info_kg ?: info_ru ?: ""
        "en" -> info_en ?: info_ru ?: ""
        else -> info_ru ?: ""
    }
}

// --- SCHEDULE MODELS ---
data class EduYear(val id: Int, val name_en: String?, val active: Boolean)
data class ScheduleWrapper(val schedule_items: List<ScheduleItem>?)
data class ScheduleItem(val day: Int, val id_lesson: Int, val subject: NameObj?, val teacher: TeacherObj?, val room: RoomObj?, val subject_type: NameObj?, val classroom: ClassroomObj?, val stream: StreamObj?)
data class StreamObj(val id: Int, val numeric: Int?)
data class ClassroomObj(val building: BuildingObj?)

data class BuildingObj(val name_en: String?, val name_ru: String?, val name_kg: String?, val info_en: String?, val info_ru: String?, val info_kg: String?) {
    fun getName(lang: String = "en"): String? = when (lang) { "ky" -> name_kg ?: name_ru ?: name_en; "ru" -> name_ru ?: name_kg ?: name_en; else -> name_en ?: name_ru ?: name_kg }
    fun getAddress(lang: String = "en"): String = when (lang) { "ky" -> info_kg ?: info_ru ?: info_en ?: ""; "ru" -> info_ru ?: info_kg ?: info_en ?: ""; else -> info_en ?: info_ru ?: info_kg ?: "" }
}

data class TeacherObj(val name: String?, val last_name: String?) { fun get(): String = "${last_name ?: ""} ${name ?: ""}".trim() }
data class RoomObj(val name_en: String?)
data class LessonTimeResponse(val id_lesson: Int, val begin_time: String?, val end_time: String?, val lesson: LessonNum?)
data class LessonNum(val num: Int)

// --- GRADES ---
data class PayStatusResponse(val paid_summa: Double?, val need_summa: Double?, val access_message: List<String>?) { fun getDebt(): Double = (need_summa ?: 0.0) - (paid_summa ?: 0.0) }
data class NewsItem(val id: Int, val title: String?, val message: String?, val created_at: String?)

data class SessionResponse(val semester: SemesterObj?, val subjects: List<SessionSubjectWrapper>?)
data class SemesterObj(val id: Int, val name_en: String?)
data class SessionSubjectWrapper(val subject: NameObj?, val marklist: MarkList?, @SerializedName("graphic") val graphic: GraphicInfo?)
data class GraphicInfo(val begin: String?, val end: String?)

data class MarkList(
    val point1: Double?, 
    val point2: Double?, 
    val point3: Double?, 
    @SerializedName("finally") val finalScore: Double?, 
    val total: Double?, 
    @SerializedName("updated_at") val updated_at: String?
)

// --- DOCS ---
data class DocIdRequest(val id: Long); data class DocKeyRequest(val key: String)
data class TranscriptYear(@SerializedName("edu_year") val eduYear: String?, @SerializedName("semesters") val semesters: List<TranscriptSemester>?)
data class TranscriptSemester(@SerializedName("semester") val semesterName: String?, @SerializedName("subjects") val subjects: List<TranscriptSubject>?)
data class TranscriptSubject(@SerializedName("subject") val subjectName: String?, @SerializedName("code") val code: String?, @SerializedName("credit") val credit: Double?, @SerializedName("mark_list") val markList: MarkList?, @SerializedName("exam_rule") val examRule: ExamRule?)
data class ExamRule(@SerializedName("alphabetic") val alphabetic: String?, @SerializedName("digital") val digital: Double?, @SerializedName("control_form") val controlForm: String?, @SerializedName("word_ru") val wordRu: String?)
data class Verify2FAResponse(@SerializedName("have_role") val haveRole: Boolean?, @SerializedName("have_bot") val haveBot: Boolean?, @SerializedName("have_2fa") val have2fa: Boolean?)
data class GitHubRelease(@SerializedName("tag_name") val tagName: String, @SerializedName("body") val body: String, @SerializedName("assets") val assets: List<GitHubAsset>)
data class GitHubAsset(@SerializedName("browser_download_url") val downloadUrl: String, @SerializedName("name") val name: String, @SerializedName("content_type") val contentType: String)

// --- INTERFACES ---
interface OshSuApi {
    @POST("api/login") suspend fun login(@Body request: LoginRequest): LoginResponse
    @GET("api/user") suspend fun getUser(): UserResponse
    @GET("api/studentinfo") suspend fun getProfile(): StudentInfoResponse
    @GET("api/control/regulations/eduyear") suspend fun getYears(): List<EduYear>
    @GET("api/studentPayStatus") suspend fun getPayStatus(): PayStatusResponse
    @GET("api/appupdate") suspend fun getNews(): List<NewsItem>
    @POST("api/verify2FA") suspend fun verify2FA(): Verify2FAResponse
    @GET("api/ep/schedule/schedulelessontime") suspend fun getLessonTimes(@Query("id_speciality") specId: Int, @Query("id_edu_form") formId: Int, @Query("id_edu_year") yearId: Int): List<LessonTimeResponse>
    @GET("api/studentscheduleitem") suspend fun getSchedule(@Query("id_speciality") specId: Int, @Query("id_edu_form") formId: Int, @Query("id_edu_year") yearId: Int, @Query("id_semester") semId: Int): List<ScheduleWrapper>
    @GET("api/studentsession") suspend fun getSession(@Query("id_semester") semesterId: Int): List<SessionResponse>
    @GET("api/studenttranscript") suspend fun getTranscript(@Query("id_student") studentId: Long, @Query("id_movement") movementId: Long): List<TranscriptYear>
    @GET("api/searchstudentinfo") suspend fun getStudentInfoRaw(@Query("id_student") studentId: Long): ResponseBody
    @GET("api/studenttranscript") suspend fun getTranscriptDataRaw(@Query("id_student") sId: Long, @Query("id_movement") mId: Long): ResponseBody
    @GET("api/control/structure/specialitylicense") suspend fun getSpecialityLicense(@Query("id_speciality") sId: Int, @Query("id_edu_form") eId: Int): ResponseBody
    @GET("api/control/structure/university") suspend fun getUniversityInfo(): ResponseBody
    @POST("api/student/doc/form13link") suspend fun getTranscriptLink(@Body req: DocIdRequest): ResponseBody
    @Multipart @POST("api/student/doc/form13") suspend fun uploadPdf(@Part("id") id: RequestBody, @Part("id_student") idStudent: RequestBody, @Part pdf: MultipartBody.Part): ResponseBody
    @POST("api/student/doc/form8link") suspend fun getReferenceLink(@Body req: DocIdRequest): ResponseBody
    @Multipart @POST("api/student/doc/form8") suspend fun uploadReferencePdf(@Part("id") id: RequestBody, @Part("id_student") idStudent: RequestBody, @Part pdf: MultipartBody.Part): ResponseBody
    @POST("api/open/doc/showlink") suspend fun resolveDocLink(@Body req: DocKeyRequest): ResponseBody

    // --- DICTIONARIES ---
    @GET("api/open/pdscountry") suspend fun getCountries(): List<DictionaryItem>
    @GET("api/open/pdsoblast") suspend fun getOblasts(): List<DictionaryItem>
    @GET("api/open/pdsregion") suspend fun getRegions(): List<DictionaryItem>
    @GET("api/open/pdsnational") suspend fun getNationalities(): List<DictionaryItem>
    @GET("api/open/pdsschool") suspend fun getSchools(): List<DictionaryItem>
    @GET("api/open/pdsmale") suspend fun getGenders(): List<DictionaryItem>
    @GET("api/control/regulations/period") suspend fun getPeriods(): List<PeriodItem>
}

interface GitHubApi { @GET suspend fun getLatestRelease(@Url url: String): GitHubRelease }

// --- CLIENT ---
class UniversalCookieJar : CookieJar {
    private val cookieStore = ArrayList<Cookie>()
    override fun saveFromResponse(url: HttpUrl, cookies: List<Cookie>) { val names = cookies.map { it.name }; cookieStore.removeAll { it.name in names }; cookieStore.addAll(cookies) }
    override fun loadForRequest(url: HttpUrl): List<Cookie> = ArrayList(cookieStore)
    fun injectSessionCookies(token: String) {
        val sdf = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.000000'Z'", Locale.US).apply { timeZone = TimeZone.getTimeZone("UTC") }
        cookieStore.removeAll { it.name == "myedu-jwt-token" || it.name == "my_edu_update" || it.name == "have_2fa" }
        cookieStore.add(Cookie.Builder().domain("myedu.oshsu.kg").path("/").name("myedu-jwt-token").value(token).build())
        cookieStore.add(Cookie.Builder().domain("myedu.oshsu.kg").path("/").name("my_edu_update").value(sdf.format(Date())).build())
        cookieStore.add(Cookie.Builder().domain("myedu.oshsu.kg").path("/").name("have_2fa").value("yes").build())
    }
    fun clear() { cookieStore.clear() }
}

class WindowsInterceptor : Interceptor {
    var authToken: String? = null
    override fun intercept(chain: Interceptor.Chain): Response {
        val builder = chain.request().newBuilder()
            .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64)")
            .header("Accept", "application/json, text/plain, */*")
            .header("Referer", "https://myedu.oshsu.kg/")
            .header("Origin", "https://myedu.oshsu.kg") 
        if (authToken != null) builder.header("Authorization", "Bearer $authToken")
        return chain.proceed(builder.build())
    }
}

class DeepSpyInterceptor : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        val url = request.url.toString()
        var reqLog = "REQ: ${request.method} $url"
        val reqBody = request.body
        if (reqBody != null) {
            try {
                val buffer = Buffer()
                reqBody.writeTo(buffer)
                val charset = reqBody.contentType()?.charset(Charset.forName("UTF-8")) ?: Charset.forName("UTF-8")
                if (isPlaintext(buffer)) reqLog += "\nBODY: ${buffer.readString(charset)}"
            } catch (e: Exception) {}
        }
        DebugLogger.log("NET_REQ", reqLog)
        val response = chain.proceed(request)
        var resLog = "RES: ${response.code} $url"
        val resBody = response.body
        if (resBody != null && response.code != 204) {
            try {
                val source = resBody.source()
                source.request(Long.MAX_VALUE)
                val buffer = source.buffer.clone()
                val contentType = resBody.contentType()?.toString()
                if (isPlaintext(buffer) && (contentType == null || !contentType.contains("pdf"))) {
                    val charset = resBody.contentType()?.charset(Charset.forName("UTF-8")) ?: Charset.forName("UTF-8")
                    val content = buffer.readString(charset)
                    resLog += if (content.length > 3000) "\nBODY: ${content.take(3000)}... (Truncated)" else "\nBODY: $content"
                }
            } catch (e: Exception) {}
        }
        DebugLogger.log("NET_RES", resLog)
        return response
    }
    private fun isPlaintext(buffer: Buffer): Boolean {
        try {
            val prefix = Buffer()
            val byteCount = if (buffer.size < 64) buffer.size else 64
            buffer.copyTo(prefix, 0, byteCount)
            for (i in 0 until 16) {
                if (prefix.exhausted()) break
                val codePoint = prefix.readUtf8CodePoint()
                if (Character.isISOControl(codePoint) && !Character.isWhitespace(codePoint)) return false
            }
            return true
        } catch (e: Exception) { return false }
    }
}

object NetworkClient {
    val cookieJar = UniversalCookieJar()
    val interceptor = WindowsInterceptor()
    val deepSpy = DeepSpyInterceptor()
    
    val api: OshSuApi = Retrofit.Builder().baseUrl("https://api.myedu.oshsu.kg/public/")
        .client(OkHttpClient.Builder()
            .cookieJar(cookieJar)
            .addInterceptor(interceptor)
            .addInterceptor(deepSpy)
            .connectTimeout(60, TimeUnit.SECONDS)
            .readTimeout(60, TimeUnit.SECONDS)
            .build())
        .addConverterFactory(GsonConverterFactory.create(GsonBuilder().setLenient().create()))
        .build().create(OshSuApi::class.java)

    val githubApi: GitHubApi = Retrofit.Builder().baseUrl("https://api.github.com/")
        .client(OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .build())
        .addConverterFactory(GsonConverterFactory.create())
        .build().create(GitHubApi::class.java)
}
