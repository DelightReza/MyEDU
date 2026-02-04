package myedu.oshsu.kg.database

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface ScheduleDao {
    @Query("SELECT * FROM schedule ORDER BY day, id_lesson")
    fun getAllSchedules(): Flow<List<ScheduleEntity>>
    
    @Query("SELECT * FROM schedule ORDER BY day, id_lesson")
    suspend fun getAllSchedulesSync(): List<ScheduleEntity>
    
    @Query("SELECT * FROM schedule WHERE day = :day ORDER BY id_lesson")
    fun getSchedulesByDay(day: Int): Flow<List<ScheduleEntity>>
    
    @Query("DELETE FROM schedule")
    suspend fun deleteAll()
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(schedules: List<ScheduleEntity>)
    
    @Transaction
    suspend fun replaceAll(schedules: List<ScheduleEntity>) {
        deleteAll()
        insertAll(schedules)
    }
}
