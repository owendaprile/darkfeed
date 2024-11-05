package gay.averyrivers.lexicon.app.bsky.feed

import kotlinx.serialization.Serializable

@Serializable
data class Post(
    val text: String,
    val createdAt: String,
)