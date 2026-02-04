package myedu.oshsu.kg.database

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface GradeDao {
    @Query("SELECT * FROM grades")
    fun getAllGrades(): Flow<List<GradeEntity>>
    
    @Query("SELECT * FROM grades")
    suspend fun getAllGradesSync(): List<GradeEntity>
    
    @Query("DELETE FROM grades")
    suspend fun deleteAll()
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(grades: List<GradeEntity>)
    
    @Transaction
    suspend fun replaceAll(grades: List<GradeEntity>) {
        deleteAll()
        insertAll(grades)
    }
}
