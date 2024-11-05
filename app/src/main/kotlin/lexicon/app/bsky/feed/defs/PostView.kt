package gay.averyrivers.lexicon.app.bsky.feed.defs

import gay.averyrivers.lexicon.com.atproto.label.defs.Label
import kotlinx.serialization.Serializable

@Serializable
data class PostView(
    val uri: String,
    val cid: String,
    val labels: List<Label>,
)