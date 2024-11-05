package gay.averyrivers.lexicon.com.atproto.repo

import kotlinx.serialization.Serializable

@Serializable
data class StrongRef(
    val uri: String,
    val cid: String,
)