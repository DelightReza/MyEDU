package myedu.oshsu.kg.database

import androidx.room.Entity
import androidx.room.PrimaryKey
import myedu.oshsu.kg.StudentInfoResponse

@Entity(tableName = "profile_data")
data class ProfileDataEntity(
    @PrimaryKey val id: Int = 1, // Single row for profile
    val profileJson: String
)

fun StudentInfoResponse.toEntity(): ProfileDataEntity {
    val gson = com.google.gson.Gson()
    return ProfileDataEntity(
        id = 1,
        profileJson = gson.toJson(this)
    )
}

fun ProfileDataEntity.toProfileData(): StudentInfoResponse {
    val gson = com.google.gson.Gson()
    return gson.fromJson(this.profileJson, StudentInfoResponse::class.java)
}
