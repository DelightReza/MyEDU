package myedu.oshsu.kg

import okhttp3.MultipartBody
import okhttp3.RequestBody
import okhttp3.ResponseBody
import retrofit2.http.*

// --- MAIN API ---
interface OshSuApi {
    @POST("api/login") suspend fun login(@Body request: LoginRequest): LoginResponse
    @GET("api/user") suspend fun getUser(): UserResponse
    @GET("api/studentinfo") suspend fun getProfile(): StudentInfoResponse
    @GET("api/control/regulations/eduyear") suspend fun getYears(): List<EduYear>
    @GET("api/studentPayStatus") suspend fun getPayStatus(): PayStatusResponse
    @GET("api/studentprice") suspend fun getStudentPrice(): List<StudentPriceResponse>
    @GET("api/appupdate") suspend fun getNews(): List<NewsItem>
    @POST("api/verify2FA") suspend fun verify2FA(): Verify2FAResponse
    @GET("api/ep/schedule/schedulelessontime") suspend fun getLessonTimes(@Query("id_speciality") specId: Int, @Query("id_edu_form") formId: Int, @Query("id_edu_year") yearId: Int): List<LessonTimeResponse>
    @GET("api/studentscheduleitem") suspend fun getSchedule(@Query("id_speciality") specId: Int, @Query("id_edu_form") formId: Int, @Query("id_edu_year") yearId: Int, @Query("id_semester") semId: Int): List<ScheduleWrapper>
    @GET("api/studentsession") suspend fun getSession(@Query("id_semester") semesterId: Int): List<SessionResponse>
    @GET("api/student/journal") suspend fun getJournal(@Query("id_curricula") idCurricula: Int, @Query("id_semester") idSemester: Int, @Query("id_subject_type") idSubjectType: Int, @Query("id_edu_year") idEduYear: Int): List<JournalItem>
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

// --- GITHUB API ---
interface GitHubApi { @GET suspend fun getLatestRelease(@Url url: String): GitHubRelease }
