package gay.averyrivers.lexicon.app.bsky.feed

import gay.averyrivers.lexicon.app.bsky.feed.defs.SkeletonFeedPost
import kotlinx.serialization.Serializable

@Serializable
data class FeedSkeleton(
    val cursor: String? = null,
    val feed: List<SkeletonFeedPost>,
)