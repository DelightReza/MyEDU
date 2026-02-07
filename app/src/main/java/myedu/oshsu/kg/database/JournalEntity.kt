package myedu.oshsu.kg.database

import androidx.room.Entity
import androidx.room.PrimaryKey
import myedu.oshsu.kg.JournalItem

@Entity(tableName = "journal")
data class JournalEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val curriculaId: Int,
    val semesterId: Int,
    val subjectType: Int,
    val eduYearId: Int,
    val date: String?,
    val theme: String?,
    val mark: String?,
    val label: Boolean?,
    val idStream: Long?,
    val teacherName: String?
)

fun JournalItem.toEntity(curriculaId: Int, semesterId: Int, subjectType: Int, eduYearId: Int): JournalEntity {
    return JournalEntity(
        curriculaId = curriculaId,
        semesterId = semesterId,
        subjectType = subjectType,
        eduYearId = eduYearId,
        date = this.date,
        theme = this.theme,
        mark = this.mark,
        label = this.label,
        idStream = this.idStream,
        teacherName = this.teacherName
    )
}

fun JournalEntity.toJournalItem(): JournalItem {
    return JournalItem(
        date = this.date,
        theme = this.theme,
        mark = this.mark,
        label = this.label,
        idStream = this.idStream,
        teacherName = this.teacherName
    )
}

fun List<JournalEntity>.toJournalItems(): List<JournalItem> {
    return this.map { it.toJournalItem() }
}

fun List<JournalItem>.toEntities(curriculaId: Int, semesterId: Int, subjectType: Int, eduYearId: Int): List<JournalEntity> {
    return this.map { it.toEntity(curriculaId, semesterId, subjectType, eduYearId) }
}
