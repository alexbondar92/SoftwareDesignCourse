package il.ac.technion.cs.softwaredesign

import com.authzee.kotlinguice4.getInstance
import com.google.inject.Guice
import com.google.inject.Inject
import il.ac.technion.cs.softwaredesign.messages.MessageFactory
import il.ac.technion.cs.softwaredesign.storage.SecureStorageModule
import java.lang.Exception
import java.time.LocalDateTime
import java.util.concurrent.CompletableFuture

class CourseBotsImpl : CourseBots {

    @Inject
    constructor(courseApp : CourseApp, messageFac : MessageFactory) {
        cApp = courseApp
        messageFactory = messageFac
    }
    private var messageFactory: MessageFactory
    private var cApp : CourseApp

    companion object {
        private val botsMap: HashMap<String, Pair<String, LocalDateTime>> = HashMap()   // botName -> Pair(token, creation time)
        private var id = 0.toLong()
    }

    // TODO ("does it need to be persistent at reboots?!?!? .... if yes what to do with all the listeners?? they need to be inserted again?")
    // TODO ("add get() for all the API methods of CoureApp")
    // TODO ("get all the data from the remote storage for CourseBots... - if was some...")
    // TODO ("matan - after reboot, all the bots initialize here(heavy opp)")


    private val defaultNamePrefix = "Anna"


    /**
     * Get an instance of CourseBot for a bot named [name].
     *
     * If the bot did not previously exist, it will be created. The default name for a bot is "Anna$n", where n is the
     * number of bots that currently exist, e.g. Anna0.
     *
     * @param name The requested name for the bot, or a default name if null.
     */
    override fun bot(name: String?): CompletableFuture<CourseBot> {
        val botName = name ?: getDefaultName()
        val password = "pass%$botName%ssap"

        var token = botsMap[botName]?.first
        if(token == null){
            token = cApp.login(botName, password).get()!!
            addToBotsMap(botName, token)
        }
        val bot = CourseBotImpl(botName, token, cApp, messageFactory)

        return CompletableFuture.completedFuture(bot)
    }

    /**
     * List the names of bots that currently exist in the system. If [channel] is specified, only bots that are in the
     * named channel will be returned.
     *
     * @param channel Limit the query to bots that are in this channel, or all channels if null.
     * @return List of bot names in order of bot creation.
     */
    override fun bots(channel: String?): CompletableFuture<List<String>> {
        return if (channel == null) {
            getAllBots()
        } else {
            getBotsOfChannel(channel)
        }
    }

    private fun getDefaultName(): String {
        val id = idGenerator()
        return "$defaultNamePrefix$id"
    }

    private fun idGenerator(): Long {
        val retId = id
        id ++
        return retId
    }

    private fun getAllBots(): CompletableFuture<List<String>> {
        return CompletableFuture.completedFuture(
                botsMap.asSequence().sortedBy { it.value.second }.map { it.key }.toList()
        )
    }

    private fun getBotsOfChannel(channel: String): CompletableFuture<List<String>> {
        return CompletableFuture.completedFuture(
                botsMap.asSequence().filter {
                    try {
                        val result = cApp.isUserInChannel(it.value.first, channel, it.key).get()
                        if (result != null) {
                            result == true
                        } else {
                            false
                        }
                    } catch (e: Exception){false}
                }.sortedBy { it.value.second }.map { it.key }.toList()
        )
    }

    private fun addToBotsMap(botName: String, token: String) {
        botsMap[botName] = Pair(token, LocalDateTime.now())
    }

    private fun getFromBotsMap(botName: String): Pair<String, LocalDateTime>? {     // in case we need a getter from the map( + remote map/tree)....
        return botsMap[botName]
    }
}