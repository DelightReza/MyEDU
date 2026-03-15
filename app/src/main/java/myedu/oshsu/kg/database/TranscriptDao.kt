package myedu.oshsu.kg.database

import androidx.room.*

@Dao
interface TranscriptDao {
    @Query("SELECT * FROM transcript WHERE id = 1")
    suspend fun getTranscriptSync(): TranscriptEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entity: TranscriptEntity)

    @Query("DELETE FROM transcript")
    suspend fun deleteAll()
}
