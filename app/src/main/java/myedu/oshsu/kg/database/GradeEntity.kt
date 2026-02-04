package myedu.oshsu.kg.database

import androidx.room.Entity
import androidx.room.PrimaryKey
import myedu.oshsu.kg.*

@Entity(tableName = "grades")
data class GradeEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val semesterJson: String?,
    val subjectJson: String?,
    val marklistJson: String?,
    val graphicJson: String?
)

// Conversion functions
fun SessionResponse.toEntities(): List<GradeEntity> {
    val gson = com.google.gson.Gson()
    return this.subjects?.map { wrapper ->
        GradeEntity(
            semesterJson = this.semester?.let { gson.toJson(it) },
            subjectJson = wrapper.subject?.let { gson.toJson(it) },
            marklistJson = wrapper.marklist?.let { gson.toJson(it) },
            graphicJson = wrapper.graphic?.let { gson.toJson(it) }
        )
    } ?: emptyList()
}

fun List<GradeEntity>.toSessionResponse(): SessionResponse {
    if (this.isEmpty()) return SessionResponse(null, null)
    
    val gson = com.google.gson.Gson()
    val firstSemester = this.firstOrNull()?.semesterJson?.let { 
        gson.fromJson(it, SemesterObj::class.java) 
    }
    
    val subjects = this.map { entity ->
        SessionSubjectWrapper(
            subject = entity.subjectJson?.let { gson.fromJson(it, NameObj::class.java) },
            marklist = entity.marklistJson?.let { gson.fromJson(it, MarkList::class.java) },
            graphic = entity.graphicJson?.let { gson.fromJson(it, GraphicInfo::class.java) }
        )
    }
    
    return SessionResponse(firstSemester, subjects)
}
