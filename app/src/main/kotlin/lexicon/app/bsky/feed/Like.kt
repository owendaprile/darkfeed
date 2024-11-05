package gay.averyrivers.lexicon.app.bsky.feed

import gay.averyrivers.lexicon.com.atproto.repo.StrongRef
import kotlinx.serialization.Serializable

@Serializable
data class Like(
    val subject: StrongRef,
    val createdAt: String,
)

@Serializable
data class LikeRef(
    val uri: String,
    val cid: String,
    val value: Like,
)