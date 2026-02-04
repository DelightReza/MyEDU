package myedu.oshsu.kg.database

import android.content.Context
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import myedu.oshsu.kg.*

class MyEduRepository(context: Context) {
    private val database = AppDatabase.getDatabase(context)
    private val scheduleDao = database.scheduleDao()
    private val gradeDao = database.gradeDao()
    private val userDataDao = database.userDataDao()
    private val profileDataDao = database.profileDataDao()
    private val payStatusDao = database.payStatusDao()
    private val newsDao = database.newsDao()
    private val timeMapDao = database.timeMapDao()
    
    // Schedule operations
    fun getAllSchedules(): Flow<List<ScheduleItem>> {
        return scheduleDao.getAllSchedules().map { entities ->
            entities.map { it.toScheduleItem() }
        }
    }
    
    suspend fun getAllSchedulesSync(): List<ScheduleItem> {
        return scheduleDao.getAllSchedulesSync().map { it.toScheduleItem() }
    }
    
    fun getSchedulesByDay(day: Int): Flow<List<ScheduleItem>> {
        return scheduleDao.getSchedulesByDay(day).map { entities ->
            entities.map { it.toScheduleItem() }
        }
    }
    
    suspend fun updateSchedules(schedules: List<ScheduleItem>) {
        val entities = schedules.map { it.toEntity() }
        scheduleDao.replaceAll(entities)
    }
    
    // Grade operations
    fun getAllGrades(): Flow<List<SessionResponse>> {
        return gradeDao.getAllGrades().map { entities ->
            listOf(entities.toSessionResponse())
        }
    }
    
    suspend fun getAllGradesSync(): SessionResponse {
        return gradeDao.getAllGradesSync().toSessionResponse()
    }
    
    suspend fun updateGrades(session: SessionResponse) {
        val entities = session.toEntities()
        gradeDao.replaceAll(entities)
    }
    
    // User data operations
    fun getUserData(): Flow<UserData?> {
        return userDataDao.getUserData().map { it?.toUserData() }
    }
    
    suspend fun getUserDataSync(): UserData? {
        return userDataDao.getUserDataSync()?.toUserData()
    }
    
    suspend fun updateUserData(userData: UserData) {
        userDataDao.insert(userData.toEntity())
    }
    
    // Profile data operations
    fun getProfileData(): Flow<StudentInfoResponse?> {
        return profileDataDao.getProfileData().map { it?.toProfileData() }
    }
    
    suspend fun getProfileDataSync(): StudentInfoResponse? {
        return profileDataDao.getProfileDataSync()?.toProfileData()
    }
    
    suspend fun updateProfileData(profileData: StudentInfoResponse) {
        profileDataDao.insert(profileData.toEntity())
    }
    
    // Pay status operations
    fun getPayStatus(): Flow<PayStatusResponse?> {
        return payStatusDao.getPayStatus().map { it?.toPayStatus() }
    }
    
    suspend fun getPayStatusSync(): PayStatusResponse? {
        return payStatusDao.getPayStatusSync()?.toPayStatus()
    }
    
    suspend fun updatePayStatus(payStatus: PayStatusResponse) {
        payStatusDao.insert(payStatus.toEntity())
    }
    
    // News operations
    fun getAllNews(): Flow<List<NewsItem>> {
        return newsDao.getAllNews().map { entities ->
            entities.map { it.toNewsItem() }
        }
    }
    
    suspend fun getAllNewsSync(): List<NewsItem> {
        return newsDao.getAllNewsSync().map { it.toNewsItem() }
    }
    
    suspend fun updateNews(news: List<NewsItem>) {
        val entities = news.map { it.toEntity() }
        newsDao.replaceAll(entities)
    }
    
    // Time map operations
    fun getTimeMap(): Flow<Map<Int, String>> {
        return timeMapDao.getTimeMap().map { it.toTimeMap() }
    }
    
    suspend fun getTimeMapSync(): Map<Int, String> {
        return timeMapDao.getTimeMapSync().toTimeMap()
    }
    
    suspend fun updateTimeMap(timeMap: Map<Int, String>) {
        val entities = timeMap.toEntities()
        timeMapDao.replaceAll(entities)
    }
    
    // Clear all data
    suspend fun clearAll() {
        scheduleDao.deleteAll()
        gradeDao.deleteAll()
        userDataDao.deleteAll()
        profileDataDao.deleteAll()
        payStatusDao.deleteAll()
        newsDao.deleteAll()
        timeMapDao.deleteAll()
    }
}
