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
        },
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
