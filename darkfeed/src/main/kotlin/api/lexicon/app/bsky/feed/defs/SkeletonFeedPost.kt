package api.lexicon.app.bsky.feed.defs

import kotlinx.serialization.Serializable

@Serializable
data class SkeletonFeedPost(
    val post: String,
    val feedContext: String? = null,
)
