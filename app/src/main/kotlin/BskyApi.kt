package gay.averyrivers

import gay.averyrivers.lexicon.app.bsky.feed.Generator
import gay.averyrivers.lexicon.app.bsky.feed.LikeRef
import gay.averyrivers.lexicon.app.bsky.feed.defs.PostView
import gay.averyrivers.lexicon.com.atproto.label.defs.Label
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.plugins.logging.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

class BskyApi(
    private var pdsUrl: Url = Url("https://bsky.social"),

    private val httpClient: HttpClient = HttpClient(CIO) {
        install(ContentNegotiation) {
            json(Json {
                explicitNulls = false
                ignoreUnknownKeys = true
            })
        }

        install(Logging)

        defaultRequest {
            url {
                protocol = pdsUrl.protocol
                host = pdsUrl.host
                path("xrpc/")
            }
        }
    },
) {


    data class AuthTokens(
        val accessJwt: String,
        val refreshJwt: String,
        val did: String,
    )

    private var authTokens: AuthTokens? = null

    @Serializable
    data class ErrorResponse(
        val error: String,
        val message: String,
    )

    suspend fun login(identifier: String, password: String) {
        @Serializable
        data class Request(val identifier: String, val password: String)

        @Serializable
        data class Response(val did: String, val accessJwt: String, val refreshJwt: String)

        val response = httpClient.post("com.atproto.server.createSession") {
            contentType(ContentType.Application.Json)
            setBody(Request(identifier, password))
        }

        when (response.status) {
            HttpStatusCode.OK -> {
                val tokens: Response = response.body()
                this.authTokens = AuthTokens(tokens.accessJwt, tokens.refreshJwt, tokens.did)
            }

            HttpStatusCode.BadRequest,
            HttpStatusCode.Unauthorized -> throw RuntimeException("Failed to create session: ${response.bodyAsText()}")

            else -> throw RuntimeException("Unexpected response received: ${response.bodyAsText()}")
        }
    }

    suspend fun getFeedGeneratorRecord(repo: String, rkey: String): Generator? {
        @Serializable
        data class Response(val uri: String, val value: Generator)

        val response = httpClient.get("com.atproto.repo.getRecord") {
            parameter("repo", repo)
            parameter("collection", "app.bsky.feed.generator")
            parameter("rkey", rkey)
        }

        when (response.status) {
            HttpStatusCode.OK -> {
                val record: Response = response.body()
                return record.value
            }

            HttpStatusCode.BadRequest -> if (response.body<ErrorResponse>().error == "RecordNotFound") return null

            HttpStatusCode.Unauthorized -> throw RuntimeException("Failed to get record: ${response.bodyAsText()}")

            else -> throw RuntimeException("Unexpected response received: ${response.bodyAsText()}")
        }

        // Why is this needed?
        return null
    }

    suspend fun putFeedGeneratorRecord(repo: String, rkey: String, record: Generator) {
        @Serializable
        data class Request(
            val repo: String,
            val rkey: String,
            val record: Generator,
            val collection: String = "app.bsky.feed.generator",
        )

        val response = httpClient.post("com.atproto.repo.putRecord") {
            contentType(ContentType.Application.Json)
            setBody(Request(repo, rkey, record))
        }

        when (response.status) {
            HttpStatusCode.BadRequest,
            HttpStatusCode.Unauthorized -> throw RuntimeException("Failed to put record: ${response.bodyAsText()}")
        }
    }

    suspend fun getLikesByActor(actor: String, cursor: String? = null): Pair<List<LikeRef>, String?> {
        @Serializable
        data class Response(val cursor: String?, val records: List<LikeRef>)

        val response = httpClient.get("com.atproto.repo.listRecords") {
            parameter("repo", actor)
            parameter("collection", "app.bsky.feed.like")
            parameter("limit", 100)
            if (cursor != null) parameter("cursor", cursor)
        }

        when (response.status) {
            HttpStatusCode.OK -> {
                val response: Response = response.body()

                val likeRefs = response.records
                val cursor = response.cursor

                return Pair(likeRefs, cursor)
            }

            HttpStatusCode.BadRequest,
            HttpStatusCode.Unauthorized -> throw RuntimeException("Failed to get likes: ${response.bodyAsText()}")

            else -> throw RuntimeException("Unexpected response received: ${response.bodyAsText()}")
        }
    }

    suspend fun getPostLabels(posts: List<String>): List<PostView> {
        @Serializable
        data class Response(val posts: List<PostView>)

        val response = httpClient.get("app.bsky.feed.getPosts") {
            posts.forEach { parameter("uris", it) }
        }

        when (response.status) {
            HttpStatusCode.OK -> {
                val response: Response = response.body()
                return response.posts
            }

            HttpStatusCode.BadRequest,
            HttpStatusCode.Unauthorized -> throw RuntimeException("Failed to get posts: ${response.bodyAsText()}")

            else -> throw RuntimeException("Unexpected response received: ${response.bodyAsText()}")
        }
    }
}