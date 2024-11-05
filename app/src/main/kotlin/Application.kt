package gay.averyrivers

import gay.averyrivers.lexicon.app.bsky.feed.Generator
import gay.averyrivers.lexicon.app.bsky.feed.Like
import gay.averyrivers.lexicon.app.bsky.feed.Post
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.engine.cio.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.server.util.*
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.Contextual
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.util.logging.Logger

const val FEED_DISPLAY_NAME = "DarkFeed"
const val FEED_DESCRIPTION = "Hi!"
const val FEED_RECORD_KEY = "darkfeed"

fun main() {
    // TODO: Move this to an env file!
    val hostname = ""
    val ownerPds = ""
    val ownerDid = ""
    val ownerAppPassword = ""

    val api = BskyApi(
        pdsUrl = buildUrl {
            protocol = URLProtocol.HTTPS
            host = ownerPds
        }
    )

    // Make sure the feed generator record exists and points to the current
    // feed generator's hostname.
    runBlocking {
        api.login(ownerDid, ownerAppPassword)

        try {
            verifyAndUpdateFeedGeneratorRecord(api, ownerDid, FEED_RECORD_KEY, hostname)
            println("Successfully set feed generator record")
        } catch (error: Exception) {
            println("Failed to verify and update feed generator record: ${error.message}")
        }
    }

    // Serve the feed generator API.
    DarkFeedApi(
        hostname = hostname,
        bskyApi = api,
        port = 8080,
    ).serve()
}

/**
 * Verify the current feed generator record, creating or updating it if necessary.
 *
 * @param api Bluesky API instance. Requires login.
 * @param repo Owner of the record.
 * @param rkey Record key of the record to check.
 * @param labelerHostname Hostname of the feed generator.
 */
suspend fun verifyAndUpdateFeedGeneratorRecord(api: BskyApi, repo: String, rkey: String, labelerHostname: String) {
    // Get the current record.
    var feedGeneratorRecord = api.getFeedGeneratorRecord(repo, rkey)

    // If the current record exists and has the correct DID, nothing needs to be done.
    if (feedGeneratorRecord?.did?.contains(labelerHostname) == true) return

    // Update the current record if one exists, or create a new one if it doesn't.
    feedGeneratorRecord = feedGeneratorRecord
        ?.copy(did = "did:web:$labelerHostname")
        ?: Generator(
            did = "did:web:$labelerHostname",
            displayName = FEED_DISPLAY_NAME,
            description = FEED_DESCRIPTION,
            createdAt = "2024-11-04T15:58:05.074Z",
        )

    // Store the new/updated record in the repo.
    api.putFeedGeneratorRecord(repo, rkey, feedGeneratorRecord)
}

//@Serializable
//data class DidDoc(
//    val id: String,
//    val service: List<Service>,
//) {
//    @Serializable
//    data class Service(
//        val id: String,
//        val type: String,
//        val serviceEndpoint: String,
//    )
//}
//
//@Serializable
//data class FeedSkeleton(
//    val cursor: String? = null,
//    val feed: List<FeedObject>,
//) {
//    @Serializable
//    data class FeedObject(
//        val post: String,
//        val reason: ReasonRepost? = null,
//        val feedContext: String? = null,
//    ) {
//        @Serializable
//        data class ReasonRepost(
//            val repost: String,
//        )
//    }
//}
//
//@Serializable
//data class AppBskyFeedGenerator(
//    val did: String,
//    @SerialName("\$type")
//    val type: String? = null,
//    val displayName: String? = null,
//    val createdAt: String? = null,
//)
//
//fun main() {
//    val hostname = ""
//    val ownerPds = ""
//    val ownerDid = ""
//    val ownerAppPassword = ""
//    val feedRecordKey = ""
//
//    val client = HttpClient(CIO) {
//        install(io.ktor.client.plugins.contentnegotiation.ContentNegotiation) {
//            json(
//                Json {
//                    explicitNulls = false
//                    ignoreUnknownKeys = true
//                }
//            )
//        }
//    }
//
//    runBlocking {
//        val getRecordResponse = client.get("https://$ownerPds/xrpc/com.atproto.repo.getRecord") {
//            url {
//                parameters.append("repo", ownerDid)
//                parameters.append("collection", "app.bsky.feed.generator")
//                parameters.append("rkey", feedRecordKey)
//            }
//        }
//
//        @Serializable
//        data class GetRecordResponse(
//            val uri: String,
//            val cid: String?,
//            val value: AppBskyFeedGenerator,
//        )
//
////        val appBskyFeedGeneratorRecord: GetRecordResponse = getRecordResponse.body()
//
//        val appBskyFeedGeneratorRecord: GetRecordResponse? = if (getRecordResponse.status == HttpStatusCode.OK) {
//            getRecordResponse.body()
//        } else {
//            null
//        }
//
//        if (appBskyFeedGeneratorRecord?.value?.did?.contains(hostname) == true) {
//            return@runBlocking
//        }
//
//        println("Updating app.bsky.feed.record did with new hostname $hostname")
//
//        @Serializable
//        data class CreateSessionRequest(
//            val identifier: String,
//            val password: String,
//        )
//
//        @Serializable
//        data class CreateSessionResponse(
//            val accessJwt: String,
//            val refreshJwt: String,
//            val did: String,
//        )
//
//        val sessionTokens: CreateSessionResponse =
//            client.post("https://$ownerPds/xrpc/com.atproto.server.createSession") {
//                contentType(ContentType.Application.Json)
//                setBody(
//                    CreateSessionRequest(
//                        identifier = ownerDid,
//                        password = ownerAppPassword,
//                    )
//                )
//            }.body()
//
//
//        if (sessionTokens.did != ownerDid) {
//            println("How is this possible!? :O")
//            return@runBlocking
//        }
//
//        @Serializable
//        data class PutRecordRequest(
//            val repo: String,
//            val collection: String,
//            val rkey: String,
//            val validate: Boolean? = null,
//            val record: AppBskyFeedGenerator,
//            val swapRecord: String? = null,
//            val swapCommit: String? = null,
//        )
//
//        val putRecordResponse = client.post("https://$ownerPds/xrpc/com.atproto.repo.putRecord") {
//            header("Authorization", "Bearer ${sessionTokens.accessJwt}")
//            contentType(ContentType.Application.Json)
//            setBody(
//                PutRecordRequest(
//                    repo = ownerDid,
//                    collection = "app.bsky.feed.generator",
//                    rkey = feedRecordKey,
//                    record = AppBskyFeedGenerator(
//                        did = "did:web:$hostname",
//                        displayName = "DarkFeed",
//                        createdAt = "2024-11-04T15:58:05.074Z",
//                    )
//                )
//            )
//        }
//
//        if (putRecordResponse.status != HttpStatusCode.OK) {
//            println("Failed to update hostname in feed generator record: ${putRecordResponse.bodyAsText()}")
//        }
//    }
//
//    // Run DarkFeed server.
//    embeddedServer(Netty, port = 8080) {
//        install(ContentNegotiation) {
//            json(Json { explicitNulls = false })
//        }
//
//        routing {
//            get("/.well-known/did.json") {
//                call.respond(
//                    DidDoc(
//                        id = "did:web:$hostname",
//                        service = listOf(
//                            DidDoc.Service(
//                                id = "#bsky_fg",
//                                type = "BskyFeedGenerator",
//                                serviceEndpoint = "https://$hostname",
//                            )
//                        )
//                    )
//                )
//            }
//
//            get("/xrpc/app.bsky.feed.getFeedSkeleton") {
//                call.respond(buildFeedSkeleton(actor = ownerDid))
//            }
//        }
//    }.start(wait = true)
//}
//
//// SOOOOO
//// I NEED TO CHECK THE SELF LABEL (at://did:plc:vwivwqztbf6pmkgss3nv2scy/app.bsky.feed.post/3la5sxh4ica2r)
//// AND THE MODERATION.BSKY.APP LABEL (at://did:plc:ujrpupcjf22a4riwjbupdv42/app.bsky.feed.post/3la5qntezsu2v)
//suspend fun buildFeedSkeleton(actor: String) = FeedSkeleton(
//    feed = getActorLikes(actor = actor)
//        .map { FeedSkeleton.FeedObject(post = it) }
//)
//
//suspend fun getPostLabels(uris: List<String>): Unit {
//    val client = HttpClient(CIO) {
//        install(io.ktor.client.plugins.contentnegotiation.ContentNegotiation) {
//            json(
//                Json {
//                    explicitNulls = false
//                    ignoreUnknownKeys = true
//                }
//            )
//        }
//    }
//
//    @Serializable
//    data class GetPostsRequest(
//        val uris: List<String>,
//    )
//
//    @Serializable
//    data class Label(
//        val ver: Int,
//        val src: String,
//        val uri: String,
//        val cid: String,
//        @SerialName("val")
//        val _val: String,
//        val cts: String,
//    )
//
//    @Serializable
//    data class HydratedPostView(
//        val uri: String,
//        val cid: String,
//        val labels: List<Label>,
//    )
//
//    @Serializable
//    data class GetPostsResponse(
//        val posts: List<HydratedPostView>,
//    )
//
//    val getPostsResponse = client.get("https://bsky.social/xrpc/app.bsky.geed.getPosts") {
//        parameters {
//            uris.forEach { uri -> parameter("uris", uri) }
//        }
//    }
//
//    println(getPostsResponse.bodyAsText())
//
//    val posts: GetPostsResponse = getPostsResponse.body()
//
//    println(getPostsResponse)
//}
//
//suspend fun getActorLikes(actor: String, cursor: String? = null): List<String> {
//    val client = HttpClient(CIO) {
//        install(io.ktor.client.plugins.contentnegotiation.ContentNegotiation) {
//            json(
//                Json {
//                    explicitNulls = false
//                    ignoreUnknownKeys = true
//                }
//            )
//        }
//    }
//
//    @Serializable
//    data class RefRecord(
//        val uri: String,
//        val cid: String,
//        val value: Like,
//    )
//
//    @Serializable
//    data class ListRecordsResponse(
//        val cursor: String?,
//        val records: List<RefRecord>,
//    )
//
//    val response = client.get("https://bsky.social/xrpc/com.atproto.repo.listRecords") {
//        parameters {
//            parameter("repo", actor)
//            parameter("collection", "app.bsky.feed.like")
//            parameter("limit", 100)
//            if (cursor != null) parameter("cursor", cursor)
//        }
//    }
//
//    return response.body<ListRecordsResponse>()
//        .records
//        .map { it.value.subject.uri }
//        .toList()
//}