import api.BskyApi
import api.lexicon.app.bsky.feed.Generator
import io.ktor.http.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import server.FeedServer
import kotlin.system.exitProcess

/**
 *
 */
data class AppContext(
    /** PDS of the feed owner's account. */
    val ownerPds: String,
    /** DID of the feed owner's account. */
    val ownerDid: String,
    /** Password for the feed owner's account. */
    val ownerPassword: String,
    /** Hostname of the feed generator server. */
    val hostname: String,
    /** Record key for the feed generator record. */
    val recordKey: String = "darkfeed",
    /** Display name for the feed. */
    val feedDisplayName: String = "DarkFeed",
    /** Description for the feed. */
    val description: String = "hi :3",
)

/**
 * Print a message and exit the application.
 *
 * @param message Message to print.
 * @param code Status code to exit with.
 */
fun printMessageAndExit(message: String, code: Int = 1): Nothing {
    println(message)
    exitProcess(code)
}

/**
 * Verify the current feed generator record, creating or updating it if necessary.
 *
 * @param api Bluesky API instance. Requires login.
 * @param ctx Application context.
 */
suspend fun verifyAndUpdateFeedGeneratorRecord(api: BskyApi, ctx: AppContext) {
    // Get the current record stored in the repo.
    var feedGeneratorRecord = api.getFeedGeneratorRecord(ctx.ownerDid, ctx.recordKey)

    // TODO: Check all fields of the record against the context.
    // If the current record exists and has the correct DID, nothing needs to be done.
    if (feedGeneratorRecord?.did?.contains(ctx.hostname) == true) return

    // Update the current record if one exists, or create a new one if it doesn't.
    feedGeneratorRecord = feedGeneratorRecord
        ?.copy(did = "did:web:${ctx.hostname}")
        ?: Generator(
            did = "did:web:${ctx.hostname}",
            displayName = ctx.feedDisplayName,
            description = ctx.description,
            // TODO: Use the real time here.
            createdAt = "2024-11-04T15:58:05.074Z"
        )

    // Store the new/updated record in the repo.
    api.putFeedGeneratorRecord(ctx.ownerDid, ctx.recordKey, feedGeneratorRecord)
}

fun main() = runBlocking {
    // Create app context from environment variables.
    val ctx = AppContext(
        ownerPds = System.getenv("FEED_ACCOUNT_PDS")
            ?: "bsky.social",
        ownerDid = System.getenv("FEED_ACCOUNT_DID")
            ?: printMessageAndExit("error: variable FEED_ACCOUNT_DID not set"),
        ownerPassword = System.getenv("FEED_ACCOUNT_PASSWORD")
            ?: printMessageAndExit("error: variable FEED_ACCOUNT_PASSWORD not set"),
        hostname = System.getenv("HOSTNAME")
            ?: printMessageAndExit("error: variable HOSTNAME not set"),
    )

    // Create API instance.
    val bskyApi = BskyApi(buildUrl {
        protocol = URLProtocol.HTTPS
        host = ctx.ownerPds
    })

    // Verify and update the feed generator record.
    launch {
        bskyApi.login(ctx.ownerDid, ctx.ownerPassword)

        try {
            verifyAndUpdateFeedGeneratorRecord(bskyApi, ctx)
            println("main: feed generator record verified")
        } catch (error: Exception) {
            println("main: failed to verify and update feed generator record: ${error.message}")
        }
    }.join()

    println("main: starting feed generator server")

    // Start the feed server.
    FeedServer(
        hostname = ctx.hostname,
        bskyApi = bskyApi,
        port = 8080,
    ).serve()
}
