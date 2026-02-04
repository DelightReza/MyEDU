package myedu.oshsu.kg.database

import androidx.room.Entity
import androidx.room.PrimaryKey
import myedu.oshsu.kg.NewsItem

@Entity(tableName = "news")
data class NewsEntity(
    @PrimaryKey val id: Int,
    val title: String?,
    val message: String?,
    val created_at: String?
)

fun NewsItem.toEntity(): NewsEntity {
    return NewsEntity(
        id = this.id,
        title = this.title,
        message = this.message,
        created_at = this.created_at
    )
}

fun NewsEntity.toNewsItem(): NewsItem {
    return NewsItem(
        id = this.id,
        title = this.title,
        message = this.message,
        created_at = this.created_at
    )
}
