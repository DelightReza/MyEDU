package myedu.oshsu.kg.database

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "time_map")
data class TimeMapEntity(
    @PrimaryKey val lessonId: Int,
    val timeString: String
)

fun Map<Int, String>.toEntities(): List<TimeMapEntity> {
    return this.map { (lessonId, timeString) ->
        TimeMapEntity(lessonId = lessonId, timeString = timeString)
    }
}

fun List<TimeMapEntity>.toTimeMap(): Map<Int, String> {
    return this.associate { it.lessonId to it.timeString }
}
