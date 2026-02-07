package myedu.oshsu.kg.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [
        ScheduleEntity::class,
        GradeEntity::class,
        UserDataEntity::class,
        ProfileDataEntity::class,
        PayStatusEntity::class,
        NewsEntity::class,
        TimeMapEntity::class,
        JournalEntity::class
    ],
    version = 2,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun scheduleDao(): ScheduleDao
    abstract fun gradeDao(): GradeDao
    abstract fun userDataDao(): UserDataDao
    abstract fun profileDataDao(): ProfileDataDao
    abstract fun payStatusDao(): PayStatusDao
    abstract fun newsDao(): NewsDao
    abstract fun timeMapDao(): TimeMapDao
    abstract fun journalDao(): JournalDao
    
    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null
        
        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "myedu_database"
                )
                .fallbackToDestructiveMigration()
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
