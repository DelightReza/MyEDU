package myedu.oshsu.kg.database

import androidx.room.Entity
import androidx.room.PrimaryKey
import myedu.oshsu.kg.UserData

@Entity(tableName = "user_data")
data class UserDataEntity(
    @PrimaryKey val id: Long,
    val name: String?,
    val last_name: String?,
    val father_name: String?,
    val email: String?,
    val email2: String?,
    val id_avn_student: Long?,
    val is_pds_approval: Boolean?,
    val id_avn: Long?,
    val id_aryz: Long?,
    val id_user: Long?,
    val id_university: Long?,
    val created_at: String?,
    val updated_at: String?
)

fun UserData.toEntity(): UserDataEntity {
    return UserDataEntity(
        id = this.id,
        name = this.name,
        last_name = this.last_name,
        father_name = this.father_name,
        email = this.email,
        email2 = this.email2,
        id_avn_student = this.id_avn_student,
        is_pds_approval = this.is_pds_approval,
        id_avn = this.id_avn,
        id_aryz = this.id_aryz,
        id_user = this.id_user,
        id_university = this.id_university,
        created_at = this.created_at,
        updated_at = this.updated_at
    )
}

fun UserDataEntity.toUserData(): UserData {
    return UserData(
        id = this.id,
        name = this.name,
        last_name = this.last_name,
        father_name = this.father_name,
        email = this.email,
        email2 = this.email2,
        id_avn_student = this.id_avn_student,
        is_pds_approval = this.is_pds_approval,
        id_avn = this.id_avn,
        id_aryz = this.id_aryz,
        id_user = this.id_user,
        id_university = this.id_university,
        created_at = this.created_at,
        updated_at = this.updated_at
    )
}
