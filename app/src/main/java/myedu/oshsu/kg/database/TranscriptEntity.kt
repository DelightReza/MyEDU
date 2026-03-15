package myedu.oshsu.kg.database

import androidx.room.Entity
import androidx.room.PrimaryKey
import myedu.oshsu.kg.TranscriptYear

@Entity(tableName = "transcript")
data class TranscriptEntity(
    @PrimaryKey val id: Int = 1,
    val transcriptJson: String
)

fun List<TranscriptYear>.toEntity(): TranscriptEntity {
    val gson = com.google.gson.Gson()
    return TranscriptEntity(id = 1, transcriptJson = gson.toJson(this))
}

fun TranscriptEntity.toTranscriptYears(): List<TranscriptYear> {
    val gson = com.google.gson.Gson()
    val type = object : com.google.gson.reflect.TypeToken<List<TranscriptYear>>() {}.type
    return try {
        gson.fromJson(transcriptJson, type)
    } catch (e: Exception) {
        emptyList()
    }
}
