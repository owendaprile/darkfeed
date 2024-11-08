package api.lexicon.com.atproto.label.defs

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class Label(
    val ver: Int?,
    val src: String,
    val uri: String,
    val cid: String?,
    @SerialName("val")
    val value: String,
    val neg: Boolean?,
    val cts: String,
    val exp: String?,
)
