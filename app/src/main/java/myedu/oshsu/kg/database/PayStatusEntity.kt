package myedu.oshsu.kg.database

import androidx.room.Entity
import androidx.room.PrimaryKey
import myedu.oshsu.kg.PayStatusResponse

@Entity(tableName = "pay_status")
data class PayStatusEntity(
    @PrimaryKey val id: Int = 1, // Single row for pay status
    val paid_summa: Double?,
    val need_summa: Double?,
    val accessMessagesJson: String?
)

fun PayStatusResponse.toEntity(): PayStatusEntity {
    val gson = com.google.gson.Gson()
    return PayStatusEntity(
        id = 1,
        paid_summa = this.paid_summa,
        need_summa = this.need_summa,
        accessMessagesJson = this.access_message?.let { gson.toJson(it) }
    )
}

fun PayStatusEntity.toPayStatus(): PayStatusResponse {
    val gson = com.google.gson.Gson()
    val type = object : com.google.gson.reflect.TypeToken<List<String>>() {}.type
    val accessMessages = this.accessMessagesJson?.let { gson.fromJson<List<String>>(it, type) }
    return PayStatusResponse(
        paid_summa = this.paid_summa,
        need_summa = this.need_summa,
        access_message = accessMessages
    )
}
