package il.ac.technion.cs.softwaredesign

import com.google.inject.Inject
import il.ac.technion.cs.softwaredesign.exceptions.InvalidTokenException
import il.ac.technion.cs.softwaredesign.messages.MediaType
import il.ac.technion.cs.softwaredesign.messages.MessageFactory
import il.ac.technion.cs.softwaredesign.storage.SecureStorageFactory
import java.lang.Exception
import java.time.LocalDateTime
import java.util.concurrent.CompletableFuture

class CourseBotsImpl : CourseBots {


    @Inject
    constructor(courseApp : CourseApp, messageFac : MessageFactory,
                secStorageFac: SecureStorageFactory ) {
        this.secStorageFac = secStorageFac
        cApp = courseApp
        messageFactory = messageFac
        listFactory =  LinkedListFactory(secStorageFac, "singletonList")
        listOfBotNames = listFactory.get()
        val dicFactory = DictionaryFactory(secStorageFac, "dic")
        botsMapToToken = dicFactory.get()
        botsMapToTime = dicFactory.get()
        idOverKillMap = dicFactory.get()

        //bot maps area:
        diceFactoryForBotMappings = DictionaryFactory(secStorageFac, "botMaps") //factory

        //dictionaries for courseBot:
        /* Explanation: (help with understanding what the hell is happening here) */
        // all the following structures created with increasing indexes(identifiers) : 0, 1 ,2... , so it's
        // restored automatically.
        // Detailed explanation: factory have the same name as other factories in other instances of courseBots
        // so it will open the same secure storage, and will restart the internal counter to zero.
        // with the value of counter the factory decides on the identifier of the collection (in this case dictionary)
        // two collections which saved on the same storage with the same identifier are the same (library promise that),
        // therefor all courseBots share the same collection which they pass to courseBot when asked to supply a new instance.
        channels = diceFactoryForBotMappings.get()
        counters = diceFactoryForBotMappings.get()
        countersChannelListOfPairs = diceFactoryForBotMappings.get()
        triggerPhraseIn = diceFactoryForBotMappings.get()
        tipTriggerPhraseIn = diceFactoryForBotMappings.get()
        tippingServiceIn = diceFactoryForBotMappings.get()
        channelToUsersListsIn = diceFactoryForBotMappings.get()
        seenLastTimeServiceIn = diceFactoryForBotMappings.get()
        mostActiveServiceIn = diceFactoryForBotMappings.get()
        surveysIn = diceFactoryForBotMappings.get()
        surveysHistoryIn = diceFactoryForBotMappings.get()
        idGeneratorIn = diceFactoryForBotMappings.get()
        surveyIdList = diceFactoryForBotMappings.get()
        surveysChannels = diceFactoryForBotMappings.get()
        surveysAnswers = diceFactoryForBotMappings.get()


        // initialise maps here because compiler is stupid
        tipCallbackIn = hashMapOf()
        calculationCallbackIn = hashMapOf()
        lastSeenCallbacksIn = hashMapOf()
        mostActiveCallbacksIn = hashMapOf()
        surveysCallbacksIn = hashMapOf()
        countersCallbacksIn = hashMapOf()
        //

    }


    private val charset = Charsets.UTF_8
    private var secStorageFac: SecureStorageFactory
    //bot maps area:
    private var diceFactoryForBotMappings : DictionaryFactory
    private var channels : Dictionary
    private var counters : Dictionary
    private var countersChannelListOfPairs : Dictionary
    private var triggerPhraseIn : Dictionary
    private var tipTriggerPhraseIn: Dictionary
    private var tippingServiceIn: Dictionary
    private var channelToUsersListsIn: Dictionary
    private var seenLastTimeServiceIn: Dictionary
    private var mostActiveServiceIn: Dictionary
    private var surveysIn:Dictionary
    private var surveysHistoryIn:Dictionary
    private var idGeneratorIn: Dictionary
    private var surveyIdList : Dictionary
    private var surveysChannels: Dictionary
    private var surveysAnswers: Dictionary
    //end

    private var messageFactory: MessageFactory
    private var cApp : CourseApp

    private var botsCollection: HashMap<String, CourseBotImpl> = HashMap()

    private val defaultNamePrefix = "Anna"

    //on storage:
    private val listFactory: LinkedListFactory
    private val listOfBotNames: LinkedList
    private val botsMapToToken: Dictionary  //botName -> token
    private val botsMapToTime: Dictionary   //botName -> LocalTime
    private val idOverKillMap: Dictionary   // id -> Long

    //bot local stuff:
    private val tipCallbackIn: HashMap<String,ListenerCallback?>
    private val calculationCallbackIn: HashMap<String,ListenerCallback?>
    private val lastSeenCallbacksIn: HashMap<String,HashMap<String, ListenerCallback>>
    private val mostActiveCallbacksIn: HashMap<String,HashMap<String, ListenerCallback>>
    private val surveysCallbacksIn: HashMap<String,HashMap<String, ListenerCallback>>
    private val countersCallbacksIn: HashMap<String,HashMap<Triple<String?, String?, MediaType?>, ListenerCallback>>

    private fun rebootProcedure(){
        recreateBots()
        recreateInsideBotSystem()
    }

    private fun recreateBots(){
        //go over each bot in list and check if already logged in with the token saved on server
        // if logged in get the token and create courseBot with it.
        //if not logged in, log in the bot and update the token on storage.
        //check if logged in through courseApp, token is in storage, time should not be changed
        for(botName in listOfBotNames){
            val token = botsMapToToken.read(botName)!!  //assert not null because of consistent information between listOfBotNames and botsMapToToken
            val flag : Boolean? = try {
                 cApp.isUserLoggedIn(token, botName).get()
            }catch(e : InvalidTokenException){
                 false
            }
            if(flag == null || flag == false){
                val password = "pass%$botName%ssap"
                val newToken = cApp.login(botName, password).get()!!    //name and password are same to the last time this bot was logged in.
                botsMapToToken.write(botName, newToken)
            }
        }
    }

    override fun start(): CompletableFuture<Unit> {
        rebootProcedure()
        return  CompletableFuture.completedFuture(Unit)
    }

    override fun prepare(): CompletableFuture<Unit> {
        return  CompletableFuture.completedFuture(Unit)
    }

    private fun recreateInsideBotSystem(){
        for(botName in listOfBotNames) {
            if(botsCollection.contains(botName))
                continue
            val token = botsMapToToken.read(botName)!!
            val bot = CourseBotImpl(botName, token, cApp, messageFactory, secStorageFac, diceFactoryForBotMappings, listFactory,
                    channels, counters, countersChannelListOfPairs, triggerPhraseIn, tipTriggerPhraseIn, tippingServiceIn,
                    channelToUsersListsIn, seenLastTimeServiceIn, mostActiveServiceIn, surveysIn, surveysHistoryIn,idGeneratorIn,
                    surveyIdList, surveysChannels, surveysAnswers,
                    tipCallbackIn, calculationCallbackIn, lastSeenCallbacksIn, mostActiveCallbacksIn, surveysCallbacksIn, countersCallbacksIn)
            botsCollection[botName] = bot

            restoreLastSeenListeners(bot)
            restoreActiveListeners(bot)
            restoreCalculationsListeners(bot)
            restoreTipListeners(bot)
            restoreSurveysListeners(bot)
            restoreCounterListeners(bot)

        }
    }

    private fun restoreCounterListeners(bot: CourseBotImpl) {
        bot.restoreCounterListeners()
    }

    private fun restoreSurveysListeners(bot: CourseBotImpl) {
        bot.restoreSurveysListeners()
    }

    private fun restoreActiveListeners(bot: CourseBotImpl) {
        bot.restoreActiveListeners()
    }

    private fun restoreLastSeenListeners(bot: CourseBotImpl) {
        bot.restoreLastSeenListeners()
    }

    private fun restoreCalculationsListeners(bot: CourseBotImpl) {
        bot.restoreCalculationsListeners()
    }

    private fun restoreTipListeners(bot: CourseBotImpl) {
        bot.restoreTipListeners()
    }

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
        if(botsCollection.contains(botName)){
            return CompletableFuture.completedFuture(botsCollection[botName])
        }
        val password = "pass%$botName%ssap"
        var token = botsMapToToken.read(botName)
        if(token == null){
            token = cApp.login(botName, password).get()!!
            addToBotsMap(botName, token)
            listOfBotNames.add(botName)
        }

        val bot = CourseBotImpl(botName, token, cApp, messageFactory, secStorageFac, diceFactoryForBotMappings, listFactory,
                    channels, counters, countersChannelListOfPairs, triggerPhraseIn, tipTriggerPhraseIn, tippingServiceIn,
                channelToUsersListsIn, seenLastTimeServiceIn, mostActiveServiceIn, surveysIn, surveysHistoryIn,idGeneratorIn,
                surveyIdList, surveysChannels, surveysAnswers,
                tipCallbackIn, calculationCallbackIn, lastSeenCallbacksIn, mostActiveCallbacksIn, surveysCallbacksIn, countersCallbacksIn)

        botsCollection[botName] = bot

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
        val id = idOverKillMap.read("id")
        if(id == null){
            idOverKillMap.write("id", "1")
            return 0L
        }
        idOverKillMap.write("id", (id.toInt()+1).toString())
        return id.toLong()
    }

    private fun getAllBots(): CompletableFuture<List<String>> {
        val botsMap: HashMap<String, Pair<String, LocalDateTime>> = HashMap()
        for(botName in listOfBotNames){
            botsMap[botName] = Pair(botsMapToToken.read(botName)!!, LocalDateTime.parse(botsMapToTime.read(botName)!!))
        }
        return CompletableFuture.completedFuture(botsMap.asSequence().sortedBy { it.value.second }.map { it.key }.toList())
    }

    private fun getBotsOfChannel(channel: String): CompletableFuture<List<String>> {
        val botsMap: HashMap<String, Pair<String, LocalDateTime>> = HashMap()
        for(botName in listOfBotNames){
            botsMap[botName] = Pair(botsMapToToken.read(botName)!!, LocalDateTime.parse(botsMapToTime.read(botName)!!))
        }
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
        botsMapToToken.write(botName, token)
        botsMapToTime.write(botName, LocalDateTime.now().toString())
    }

}