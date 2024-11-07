package gay.averyrivers

import com.auth0.jwt.JWT
import gay.averyrivers.lexicon.app.bsky.feed.FeedSkeleton
import gay.averyrivers.lexicon.app.bsky.feed.defs.PostView
import gay.averyrivers.lexicon.app.bsky.feed.defs.SkeletonFeedPost
import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.coroutines.Job
import kotlinx.coroutines.joinAll
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
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
        println("handleWellKnownDid: responding with hostname $hostname")

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

    private suspend fun handleGetFeedSkeleton(call: RoutingCall) {
        val requestor = JWT.decode(call.request.headers["Authorization"]?.removePrefix("Bearer ")).issuer
        val limit = call.queryParameters["limit"]?.toIntOrNull()
        val cursor = call.queryParameters["cursor"]

        println("handleGetFeedSkeleton: requestor: $requestor, limit: $limit, cursor: $cursor")

        call.respond(buildFeedSkeleton(requestor, limit, cursor))
    }

    private suspend fun buildFeedSkeleton(requestor: String, limit: Int? = null, cursor: String? = null): FeedSkeleton {
        val labeledPosts: MutableSet<PostView> = mutableSetOf()
        var apiCallsCount = 0
        var getLikesByActorCursor: String? = cursor?.split(':')?.last()
        var isFeedFinished: Boolean = false

        while (labeledPosts.count() < (limit ?: 10) && apiCallsCount < 10 && !isFeedFinished) {
            val likes = bskyApi.getLikesByActor(requestor, getLikesByActorCursor)
                .also {
                    if (it.second == null) {
                        println("buildFeedSkeleton: $requestor: getLikes cursor is null, no more likes available")
                        isFeedFinished = true
                    }
                }
                .also { getLikesByActorCursor = it.second }
                .first
                .map { likeRef -> likeRef.value.subject.uri }

            println("buildFeedSkeleton: $requestor: got ${likes.count()} likes, new cursor: $getLikesByActorCursor")

            val handles: MutableList<Job> = mutableListOf()

            runBlocking {
                likes
                    .chunked(25)
                    .forEach { likeUris ->
                        launch {
                            bskyApi.getPostLabels(likeUris)
                                .filter { post ->
                                    post.labels?.any { label ->
                                        listOf(
                                            "porn",
                                            "sexual",
                                            "nudity",
                                            "sexual-figurative"
                                        ).contains(label.value)
                                    } ?: false
                                }.also { labeledPosts.addAll(it) }
                        }.also { handles.add(it) }
                    }
            }

            handles.joinAll()

            println("buildFeedSkeleton: $requestor: found ${labeledPosts.count()} labeled likes")

            apiCallsCount++
        }

        println("buildFeedSkeleton: $requestor: returning FeedSkeleton, required $apiCallsCount getLikes calls")

        return FeedSkeleton(
            cursor = if (!isFeedFinished) {
                "$requestor:$getLikesByActorCursor"
            } else {
                null
            },
            feed = labeledPosts.map { post -> SkeletonFeedPost(post = post.uri) }
        )
    }
}
