package myedu.oshsu.kg

import java.util.UUID

data class SavedAccount(
    val id: String = UUID.randomUUID().toString(),
    val email: String,
    val password: String,
    val token: String? = null,
    val name: String? = null,
    val lastName: String? = null,
    val avatarUrl: String? = null,
    val cachedAvatarPath: String? = null
) {
    val displayName: String
        get() = listOfNotNull(lastName?.ifBlank { null }, name?.ifBlank { null })
            .joinToString(" ")
            .ifEmpty { email }
}
