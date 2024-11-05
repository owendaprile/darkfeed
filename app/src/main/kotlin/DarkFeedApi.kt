package gay.averyrivers

import gay.averyrivers.lexicon.app.bsky.feed.FeedSkeleton
import gay.averyrivers.lexicon.app.bsky.feed.defs.SkeletonFeedPost
import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json


class DarkFeedApi(
    private val hostname: String,
    private val bskyApi: BskyApi,
    private val port: Int = 8080,
) {
    @Serializable
    data class DidJson(
        val id: String,
        val service: List<Service>,
    ) {
        @Serializable
        data class Service(
            val id: String,
            val type: String,
            val serviceEndpoint: String,
        )
    }

    // you better work, bitch
    fun serve() {
        embeddedServer(Netty, port = port) {
            install(ContentNegotiation) {
                json(Json {
                    explicitNulls = false
                    ignoreUnknownKeys = true
                })
            }

            routing {
                get("/.well-known/did.json") {
                    handleWellKnownDidJson(call)
                }

                get("/xrpc/app.bsky.feed.getFeedSkeleton") {
                    handleGetFeedSkeleton(call)
                }
            }
        }.start(wait = true)
    }

    private suspend fun handleWellKnownDidJson(call: RoutingCall) {
        call.respond(
            DidJson(
                id = "did:web:$hostname",
                service = listOf(
                    DidJson.Service(
                        id = "#bsky_fg",
                        type = "BskyFeedGenerator",
                        serviceEndpoint = "https://$hostname",
                    )
                )
            )
        )
    }

    // SOOOOO
    // I NEED TO CHECK THE SELF LABEL (at://did:plc:vwivwqztbf6pmkgss3nv2scy/app.bsky.feed.post/3la5sxh4ica2r)
    // AND THE MODERATION.BSKY.APP LABEL (at://did:plc:ujrpupcjf22a4riwjbupdv42/app.bsky.feed.post/3la5qntezsu2v)

    private suspend fun handleGetFeedSkeleton(call: RoutingCall) {
        // TODO: Get requestor's DID from Authorization header.
        call.respond(buildFeedSkeleton("did:plc:zhxv5pxpmojhnvaqy4mwailv"))
    }

    private suspend fun buildFeedSkeleton(requestor: String): FeedSkeleton {
        return FeedSkeleton(
            feed = bskyApi.getLikesByActor(requestor)
                .first
                .map { likeRef -> SkeletonFeedPost(post = likeRef.value.subject.uri) }
        )
    }
}