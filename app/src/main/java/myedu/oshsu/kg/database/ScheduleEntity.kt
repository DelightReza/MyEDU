package myedu.oshsu.kg.database

import androidx.room.Entity
import androidx.room.PrimaryKey
import myedu.oshsu.kg.*

@Entity(tableName = "schedule")
data class ScheduleEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val day: Int,
    val id_lesson: Int,
    val subjectJson: String?,
    val teacherJson: String?,
    val roomJson: String?,
    val subjectTypeJson: String?,
    val classroomJson: String?,
    val streamJson: String?
)

// Conversion functions
fun ScheduleItem.toEntity(): ScheduleEntity {
    val gson = com.google.gson.Gson()
    return ScheduleEntity(
        day = this.day,
        id_lesson = this.id_lesson,
        subjectJson = this.subject?.let { gson.toJson(it) },
        teacherJson = this.teacher?.let { gson.toJson(it) },
        roomJson = this.room?.let { gson.toJson(it) },
        subjectTypeJson = this.subject_type?.let { gson.toJson(it) },
        classroomJson = this.classroom?.let { gson.toJson(it) },
        streamJson = this.stream?.let { gson.toJson(it) }
    )
}

fun ScheduleEntity.toScheduleItem(): ScheduleItem {
    val gson = com.google.gson.Gson()
    return ScheduleItem(
        day = this.day,
        id_lesson = this.id_lesson,
        subject = this.subjectJson?.let { gson.fromJson(it, NameObj::class.java) },
        teacher = this.teacherJson?.let { gson.fromJson(it, TeacherObj::class.java) },
        room = this.roomJson?.let { gson.fromJson(it, RoomObj::class.java) },
        subject_type = this.subjectTypeJson?.let { gson.fromJson(it, NameObj::class.java) },
        classroom = this.classroomJson?.let { gson.fromJson(it, ClassroomObj::class.java) },
        stream = this.streamJson?.let { gson.fromJson(it, StreamObj::class.java) }
    )
}
