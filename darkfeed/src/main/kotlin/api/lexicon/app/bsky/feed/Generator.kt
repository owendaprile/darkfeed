package api.lexicon.app.bsky.feed

import kotlinx.serialization.Serializable

@Serializable
data class Generator(
    val did: String,
    val displayName: String,
    val description: String?,
    val createdAt: String,
)
