package myedu.oshsu.kg.database

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface UserDataDao {
    @Query("SELECT * FROM user_data LIMIT 1")
    fun getUserData(): Flow<UserDataEntity?>
    
    @Query("SELECT * FROM user_data LIMIT 1")
    suspend fun getUserDataSync(): UserDataEntity?
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(userData: UserDataEntity)
    
    @Query("DELETE FROM user_data")
    suspend fun deleteAll()
}

@Dao
interface ProfileDataDao {
    @Query("SELECT * FROM profile_data WHERE id = 1")
    fun getProfileData(): Flow<ProfileDataEntity?>
    
    @Query("SELECT * FROM profile_data WHERE id = 1")
    suspend fun getProfileDataSync(): ProfileDataEntity?
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(profileData: ProfileDataEntity)
    
    @Query("DELETE FROM profile_data")
    suspend fun deleteAll()
}

@Dao
interface PayStatusDao {
    @Query("SELECT * FROM pay_status WHERE id = 1")
    fun getPayStatus(): Flow<PayStatusEntity?>
    
    @Query("SELECT * FROM pay_status WHERE id = 1")
    suspend fun getPayStatusSync(): PayStatusEntity?
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(payStatus: PayStatusEntity)
    
    @Query("DELETE FROM pay_status")
    suspend fun deleteAll()
}

@Dao
interface NewsDao {
    @Query("SELECT * FROM news ORDER BY created_at DESC")
    fun getAllNews(): Flow<List<NewsEntity>>
    
    @Query("SELECT * FROM news ORDER BY created_at DESC")
    suspend fun getAllNewsSync(): List<NewsEntity>
    
    @Query("DELETE FROM news")
    suspend fun deleteAll()
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(news: List<NewsEntity>)
    
    @Transaction
    suspend fun replaceAll(news: List<NewsEntity>) {
        deleteAll()
        insertAll(news)
    }
}

@Dao
interface TimeMapDao {
    @Query("SELECT * FROM time_map ORDER BY lessonId")
    fun getTimeMap(): Flow<List<TimeMapEntity>>
    
    @Query("SELECT * FROM time_map ORDER BY lessonId")
    suspend fun getTimeMapSync(): List<TimeMapEntity>
    
    @Query("DELETE FROM time_map")
    suspend fun deleteAll()
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(times: List<TimeMapEntity>)
    
    @Transaction
    suspend fun replaceAll(times: List<TimeMapEntity>) {
        deleteAll()
        insertAll(times)
    }
}
