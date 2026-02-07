package myedu.oshsu.kg.database

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface JournalDao {
    @Query("SELECT * FROM journal WHERE curriculaId = :curriculaId AND semesterId = :semesterId AND subjectType = :subjectType AND eduYearId = :eduYearId ORDER BY date DESC")
    fun getJournalEntries(curriculaId: Int, semesterId: Int, subjectType: Int, eduYearId: Int): Flow<List<JournalEntity>>
    
    @Query("SELECT * FROM journal WHERE curriculaId = :curriculaId AND semesterId = :semesterId AND subjectType = :subjectType AND eduYearId = :eduYearId ORDER BY date DESC")
    suspend fun getJournalEntriesSync(curriculaId: Int, semesterId: Int, subjectType: Int, eduYearId: Int): List<JournalEntity>
    
    @Query("DELETE FROM journal WHERE curriculaId = :curriculaId AND semesterId = :semesterId AND subjectType = :subjectType AND eduYearId = :eduYearId")
    suspend fun deleteJournalEntries(curriculaId: Int, semesterId: Int, subjectType: Int, eduYearId: Int)
    
    @Query("DELETE FROM journal")
    suspend fun deleteAll()
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(entries: List<JournalEntity>)
    
    @Transaction
    suspend fun replaceJournalEntries(curriculaId: Int, semesterId: Int, subjectType: Int, eduYearId: Int, entries: List<JournalEntity>) {
        deleteJournalEntries(curriculaId, semesterId, subjectType, eduYearId)
        insertAll(entries)
    }
}
