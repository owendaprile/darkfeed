package api.lexicon.app.bsky.feed.defs

import api.lexicon.com.atproto.label.defs.Label
import kotlinx.serialization.Serializable

@Serializable
data class PostView(
    val uri: String,
    val cid: String,
    val labels: List<Label>?,
)
