package il.ac.technion.cs.softwaredesign

import il.ac.technion.cs.softwaredesign.exceptions.*
import il.ac.technion.cs.softwaredesign.messages.MediaType
import il.ac.technion.cs.softwaredesign.messages.Message
import il.ac.technion.cs.softwaredesign.messages.MessageFactory
import java.time.LocalDateTime
import java.util.concurrent.CompletableFuture
import kotlin.collections.HashMap


class CourseBotImpl : CourseBot {


    constructor(name: String, token: String, courseApp: CourseApp, messageFac: MessageFactory) {
        // TODO ("does this bot in the storage? if yes load his data else create new empty bot")
        // TODO ("sync with the library - to load or init one of them... depending if there was this bot before the reboot")

        cApp = courseApp
        messageFactory = messageFac
        botName = name
        botToken = token

        // init maps for that bot - remote data structures
        tippingService[botName] = HashMap()
        seenLastTimeService[botName] = HashMap()
        mostActiveService[botName] = HashMap()

        // init maps for that bot - local data structures
        lastSeenCallbacks = HashMap()
        mostActiveCallbacks = HashMap()
        surveysCallbacks = HashMap()

    }

    private val charset = Charsets.UTF_8

    private val baseTip: Long = 1000

    private var botName: String
    private var botToken: String

    private var cApp: CourseApp
    private val messageFactory: MessageFactory

    private var tipCallback: ListenerCallback? = null
    private var calculationCallback: ListenerCallback? = null
    private var lastSeenCallbacks: HashMap<String, ListenerCallback?>                                                   //local     // channel -> ListenerCallback
    private var mostActiveCallbacks: HashMap<String, ListenerCallback?>                                                 //local     // channel -> ListenerCallback
    private val surveysCallbacks: HashMap<String, ListenerCallback>                                                     //local     // surveyID -> ListenerCallback
    private val countersCallbacks: HashMap<Triple<String?, String?, MediaType?>, ListenerCallback> = HashMap()          //local     // Triple(channel, Regex, MediaType) -> ListenerCallback

    companion object {
        //maps from name to list of channels:
        private val channels: HashMap<String, MutableList<String>> = HashMap()                                          //storage   // botName -> list of channels
        private val counters: HashMap<String, HashMap<String, HashMap<Pair<String?, MediaType?>, Long>>> = HashMap()    //storage   // botName -> (channel -> (Pair(Regex, mediaType) -> Long))
        private val triggerPhrase: HashMap<String, String?> = HashMap()                                                 //storage   // botName -> (Calculation Trigger)String
        private var tipTriggerPhrase: HashMap<String, String?> = HashMap()                                              //storage   // botName -> (tip Trigger)String
        private val tippingService: HashMap<String, HashMap<String, HashMap<String, Long>>> = HashMap()                 //storage   // botName -> (channel -> (user, current tip))
        private val seenLastTimeService: HashMap<String, HashMap<String, HashMap<String, LocalDateTime?>>> = HashMap()  //storage   // botName -> (channel -> (user, time))
        private val mostActiveService: HashMap<String, HashMap<String, HashMap<String, Long>>> = HashMap()              //storage   // botName -> (channel ->(user, number of messages sent by user))
        private val surveys: HashMap<String, LongArray> = HashMap()                                                     //storage   // surveyID -> list of counters for answers
        private val surveysHistory: HashMap<String, MutableSet<Pair<String,Int>>> = HashMap()                           //storage   // surveyID -> set of voters in this survey

        //global survey id counter:
        private var surveyIdGen = 0L                                                                                    //storage
    }

    /**
     * Make the bot join the channel, courseBots is managing the login of bot.
     *
     * @throws UserNotAuthorizedException If the channel can't be joined.
     */
    override fun join(channelName: String): CompletableFuture<Unit> {
        try {
            cApp.channelJoin(botToken, channelName).get()
        } catch (e: Exception) {
            throw UserNotAuthorizedException()
        }

        addToChannelsList(channelName)

        // tipping service for new channel
        initTippingService(channelName)

        // last seen for new channel
        initSeenLastTimeIn(channelName)
        val lastSeenCallback = getLastSeenCallback(channelName)
        lastSeenCallbacks[channelName] = lastSeenCallback
        cApp.addListener(botToken, lastSeenCallback).get()

        // most active for new channel
        initMostActiveService(channelName)
        val mostActiveCallback = getMostActiveUserCallback(channelName)
        mostActiveCallbacks[channelName] = mostActiveCallback
        cApp.addListener(botToken, mostActiveCallback).get()

        return CompletableFuture.completedFuture(Unit)
    }

    /**
     * Make the bot leave the channel.
     *
     * Leaving the channel resets all statistics for that channel.
     *
     * @throws NoSuchEntityException If the channel can't be parted.
     */
    override fun part(channelName: String): CompletableFuture<Unit> {
        try {
            cApp.channelPart(botToken, channelName).get()
        } catch (e: Exception) {
            throw NoSuchEntityException()
        }
        resetCounterForAllChannelsOfBot(channelToReset = channelName)           //TODO("check")
        removeFromChannelsList(channelName)
        return CompletableFuture.completedFuture(Unit)

        // TODO ("need to remove all the callbacks from this channel before part - assumption for courseApp")
        /*
        resetBotIn(channelName)
        return CompletableFuture.completedFuture(Unit)
        */
    }

    /**
     * Return a list of all the channels the bot is in, in order of joining.
     */
    override fun channels(): CompletableFuture<List<String>> {
        return CompletableFuture.completedFuture(channelsList())
    }

    /**
     * Start counting messages that match [channel], the regular-expression [regex] (see [kotlin.text.Regex]) and
     * [mediaType].
     *
     * If [channel], [regex] and [mediaType] are already registered, restart the count from 0.
     *
     * If [channel], [regex], or [mediaType] are null, they are treated as wildcards.
     *
     * @throws IllegalArgumentException If [regex] and [mediaType] are both null.
     */
    override fun beginCount(channel: String?, regex: String?, mediaType: MediaType?): CompletableFuture<Unit> {
        if (regex == null && mediaType == null)
            throw IllegalArgumentException()

        if (getCounters() == null)
            initCounters()

        val pair = Pair(regex, mediaType)
        if (channel == null)
            initiateCounterForPair(p = pair)
        else {
            initiateCounterForPair(p = pair, channelToReset = channel)
        }
        val callback: ListenerCallback = getCounterCallback(pair, channel)

        countersCallbacks[Triple(channel, regex, mediaType)] = callback
        cApp.addListener(botToken, callback).get()

        return CompletableFuture.completedFuture(Unit)

    }

    /**
     * Return the number of times that a message that matches [regex] and [mediaType] has been seen by this bot in
     * [channel], or in all channels if [channel] is null.
     *
     * @throws IllegalArgumentException if [regex] and [mediaType] have never been registered (passed to [beginCount]).
     */
    override fun count(channel: String?, regex: String?, mediaType: MediaType?): CompletableFuture<Long> {
        val pair = Pair(regex, mediaType)
        if (getCounters() == null)
            throw IllegalArgumentException()
        if (channel != null) {
            if (getCounterOf(channel, pair) == null)
                throw IllegalArgumentException()
            val res = getCounterOf(channel, pair)!!
            return CompletableFuture.completedFuture(res)
        }
        //channel is null, need to return the sum of all of calls in all channels
        val channelOfBot = channels().join()
        var sum = 0.toLong()
        var flagSeen = false
        for (currChannel in channelOfBot) {
            if (!isCountersOfExists(currChannel)) continue
            val num = getCounterOf(currChannel, pair)
            if (num != null) {
                sum += num
                flagSeen = true
            }
        }
        if (!flagSeen) throw IllegalArgumentException()
        return CompletableFuture.completedFuture(sum)
    }

    /**
     * Set the phrase to trigger calculations.
     *
     * If this is set to a non-null value, then after seeing messages in the format "$trigger $expression" in a channel,
     * the bot will evaluate the expression (integers only, 4 arithmetic operators and parenthesis) and send a message
     * with the value to the same channel.
     *
     * @param trigger The phrase, or null to disable calculator mode.
     * @return The previous phrase.
     */
    override fun setCalculationTrigger(trigger: String?): CompletableFuture<String?> {
        val previousPhrase = getTriggerPhrase()
        if (trigger == null) {            //previously was activated, and need to be deactivated.
            if (previousPhrase != null)
                cApp.removeListener(botToken, calculationCallback!!).get()
            setTriggerPhrase(null)
            return CompletableFuture.completedFuture(previousPhrase)
        }

        setTriggerPhrase(trigger)
        val callback = getCalculationCallback(trigger)
        cApp.addListener(botToken, callback).get()
        calculationCallback = callback

        return CompletableFuture.completedFuture(previousPhrase)
    }

    /**
     * Set the phrase to trigger tipping.
     *
     * If this is set to a non-null value, then after seeing messages in the format "$trigger $number $user" in a
     * channel, the bot will transfer $number bits from the user who sent the message to $user.
     *
     * @param trigger The phrase, or null to disable tipping mode.
     * @return The previous phrase.
     */
    override fun setTipTrigger(trigger: String?): CompletableFuture<String?> {
        val previousPhrase = getTipTriggerPhrase()
        setTipTriggerPhrase(trigger)

        if (tipCallback != null)
            cApp.removeListener(botToken, tipCallback!!).get()

        if (trigger != null) {
            tipCallback = getTipCallback(trigger)
            cApp.addListener(botToken, tipCallback!!).get()
        }

        return CompletableFuture.completedFuture(previousPhrase)
    }

    /**
     * Return the creation time of the last message sent by [user] in all channels that the bot is in.
     */
    override fun seenTime(user: String): CompletableFuture<LocalDateTime?> {
        var lastMessageTime: LocalDateTime? = null
        for (channel in channelsList()) {
            if (cApp.isUserInChannel(botToken, channel, user).get() == true) {
                val time = getSeenLastTimeFor(channel, user)
                if (time!! > lastMessageTime) {
                    lastMessageTime = time
                }
            }
        }

        return CompletableFuture.completedFuture(lastMessageTime)
    }

    /**
     * Return the name of the user that has sent the most messages to [channel], or null if we haven't seen any messages.
     *
     * @throws NoSuchEntityException If the bot is not in [channel].
     */
    override fun mostActiveUser(channel: String): CompletableFuture<String?> {
        val checker = cApp.isUserInChannel(botToken, channel, botName).get()
        if (checker == false || checker == null) {
            throw NoSuchEntityException()
        }

        return CompletableFuture.completedFuture(getMostActiveUser(channel))
    }

    /**
     * Return the name of the richest user in [channel]'s tip system, or null if no tipping has occured or no one user is
     * the richest.
     *
     * @throws NoSuchEntityException If the bot is not in [channel].
     */
    override fun richestUser(channel: String): CompletableFuture<String?> {
        val checker: Boolean?
        try {
            checker = cApp.isUserInChannel(botToken, channel, botName).get()
        } catch (e: Exception) {
            throw NoSuchEntityException()
        }
        if (checker == false || checker == null) {
            throw NoSuchEntityException()
        }

        return CompletableFuture.completedFuture(getRichestUser(channel))
    }

    /**
     * Start a survey by sending [question] to [channel], and looking for the literal string in [answers] in future
     * messages to [channel], recording the survey results.
     *
     * @param question The survey question to sent to [channel]
     * @param answers List of phrases to recognize as survey answers
     * @throws NoSuchEntityException If the bot is not in [channel].
     * @return A string identifying this survey.
     */
    override fun runSurvey(channel: String, question: String, answers: List<String>): CompletableFuture<String> {
        val checker = cApp.isUserInChannel(botToken, channel, botName).get()
        if (checker == false || checker == null) {
            throw NoSuchEntityException()
        }

        val surveyId = getSurveyId()

        sendSurveyQuestion(channel, question)
        val callback = getSurveyCallback(channel, answers, surveyId)

        surveysCallbacks[surveyId] = callback
        cApp.addListener(botToken, callback).get()

        return CompletableFuture.completedFuture(surveyId)
    }

    /**
     * Return the results of the survey.
     *
     * @throws NoSuchEntityException If [identifier] does not refer to a known survey.
     * @param identifier A survey identifier that was returned from [runSurvey].
     * @return A list of counters, one for each possible survery answer, in the order the answers appeared in [runSurvey].
     */
    override fun surveyResults(identifier: String): CompletableFuture<List<Long>> {
        if (!isSurveysExists(identifier))
            throw NoSuchEntityException()

        val results = getSurveys(identifier)!!.toList()

        return CompletableFuture.completedFuture(results)
    }

    private fun getCounterCallback(p: Pair<String?, MediaType?>, targetChannel: String?): ListenerCallback {
        return { source, message ->
            if ((p.second == null && p.first != null && Regex(p.first!!).containsMatchIn(message.contents.toString(charset))) ||
                    (p.second == message.media && p.first == null) ||
                    (p.second == message.media && p.first != null && Regex(p.first!!).containsMatchIn(message.contents.toString(charset)))) {
                val channelName = source.split('@')[0]
                if (isCountersExists() && isCountersOfExists(channelName) && (targetChannel == null || targetChannel == channelName)) {
                    incCounterOf(channelName, p)
                }
            }
            CompletableFuture.completedFuture(Unit)
        }
    }

    private fun getCalculationCallback(trigger: String): ListenerCallback {
        return { source, message ->
            val content = message.contents.toString(charset)
            val channelName = source.split("@")[0]
            if (channelsList().contains(channelName) && content.startsWith(trigger)) {
                val expression = content.removePrefix("$trigger ")
                val result = evaluateExpresion(expression).toString()

                val retMessage = messageFactory.create(MediaType.TEXT, result.toByteArray(charset)).get()
                cApp.channelSend(botToken, channelName, retMessage).get()

                CompletableFuture.completedFuture(Unit)
            } else {
                CompletableFuture.completedFuture(Unit)
            }
        }
    }

    private fun getTipCallback(trigger: String): ListenerCallback {
        return { source, message ->
            val content = message.contents.toString(charset)
            val channelName = source.split("@")[0]
            val fromUser = source.split("@")[1]
            if (channelsList().contains(channelName) && content.startsWith(trigger)) {        // matan: assumption that the message for tipping only inside channels
                val number = content.split(" ")[1].toLong()
                val toUser = content.split(" ")[2]
                if (cApp.isUserInChannel(botToken, channelName, toUser).get() == true) {  // matan: assumption that the 'To' user need to be in the same channel
                    transferTip(channelName, fromUser, toUser, number)
                }

                CompletableFuture.completedFuture(Unit)
            } else {
                CompletableFuture.completedFuture(Unit)
            }
        }
    }

    private fun getSurveyCallback(channel: String, answers: List<String>, surveyId: String): ListenerCallback {
        initSurveys(surveyId, answers.size)
        initSurveysHistory(surveyId)

        return { source, message ->
            val inChannel = source.split('@')[0]
            val voter = source.split('@')[1]

            if (voter != botName && inChannel == channel) {
                val i = getAnswerNumber(answers, message)
                if (i != -1) {
                    if (!isUserVoteFor(surveyId, voter)) {
                        incSurveyVote(surveyId, i)
                    }
                    addToSurveysHistory(surveyId, Pair(voter, i))
                }

                CompletableFuture.completedFuture(Unit)
            } else {
                CompletableFuture.completedFuture(Unit)
            }
        }
    }

    private fun getLastSeenCallback(channel: String): ListenerCallback {
        return { source, _ ->
            if (source.split("@")[0] == channel) {
                val user = source.split("@")[1]

                setSeenLastTimeFor(channel, user)
                CompletableFuture.completedFuture(Unit)
            } else {
                CompletableFuture.completedFuture(Unit)
            }
        }
    }

    private fun getMostActiveUserCallback(channel: String): ListenerCallback {
        return { source, _ ->
            if (source.split("@")[0] == channel) {
                val user = source.split("@")[1]

                val counter = getMostActiveServiceOf(channel, user)
                if (counter == null)
                    setMostActiveServiceOf(channel, user, 1L)
                else
                    setMostActiveServiceOf(channel, user, counter + 1)
                CompletableFuture.completedFuture(Unit)
            } else {
                CompletableFuture.completedFuture(Unit)
            }
        }
    }

    private fun evaluateExpresion(expression: String): Double {

//        val newExpresion = expression.replace(" ","")
//                .replace(Regex(".(?!$)"), "$0 ")
//                .split(" ")
//                .dropLastWhile { it.isEmpty() }
//                .toTypedArray()                     // matan: can be spaces in the exp...
        val newExpresion = expression.split(" ".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()

        val outputRPN = ExpressionParser.infixToRPN(newExpresion)
        return ExpressionParser.RPNtoDouble(outputRPN)
    }

    private fun getAnswerNumber(answers: List<String>, message: Message): Int {
        val content = message.contents.toString(charset)
        return answers.indexOf(content)
    }

    private fun transferTip(channel: String, fromUser: String, toUser: String, number: Long) {
        if (getTippingServiceFor(channel, fromUser) == null)
            setTippingService(channel, fromUser, getBaseTip())

        if (getTippingServiceFor(channel, toUser) == null)
            setTippingService(channel, toUser, getBaseTip())

        if (getTippingServiceFor(channel, fromUser)!! >= number) {
            setTippingService(channel, fromUser, getTippingServiceFor(channel, fromUser)!! - number)
            setTippingService(channel, toUser, getTippingServiceFor(channel, toUser)!! + number)
        }
    }

    private fun sendSurveyQuestion(channel: String, question: String) {
        messageFactory.create(MediaType.TEXT, question.toByteArray(charset)).thenCompose { cApp.channelSend(botToken, channel, it) }.get()
    }

    private fun getBaseTip(): Long {
        return baseTip
    }

    private fun initiateCounterForPair(p: Pair<String?, MediaType?>? = null, channelToReset: String? = null) {
        assert(p != null)
        if (channelToReset == null) {
            val channelOfBot = channels().join()
            for (channel in channelOfBot) {
                initiateSinglePairInChannel(channel, p)
            }
        } else {
            initiateSinglePairInChannel(channelToReset, p)
        }
    }

    private fun getMostActiveUser(channel: String): String? {
        var mostActiveUser: String? = null
        var mostActiveCounter: Long = 0
        getMostActiveService(channel)!!.asSequence().forEach {
            if (mostActiveUser == null || mostActiveCounter < it.value) {
                mostActiveUser = it.key
                mostActiveCounter = it.value
            } else if (mostActiveCounter == it.value) {
                return null
            }
        }
        return mostActiveUser
    }

    /*
    resets the counter for all the channels of bot with pair,
    if pair is null: resets all channels and all pairs.
 */
    private fun resetCounterForAllChannelsOfBot(p: Pair<String?, MediaType?>? = null, channelToReset: String? = null) {
        if (!isCountersExists()) return

        val channelOfBot = channels().join()
        if (p == null) {
            if (channelToReset == null) {
                for (channel in channelOfBot) {
                    initCounters()
                    initCountersOf(channel)
                }
            } else {
                initCountersOf(channelToReset)
            }
        } else {
            if (channelToReset == null) {
                for (channel in channelOfBot) {
                    removeCounterFrom(channel, p)
                }
            } else {
                removeCounterFrom(channelToReset, p)
            }
        }
    }

    private fun initiateSinglePairInChannel(channelToReset: String, p: Pair<String?, MediaType?>?) {
        if (!isCountersExists()) {
            initCounters()
        }
        if (!isCountersOfExists(channelToReset))
            initCountersOf(channelToReset)
        resetCounter(channelToReset, p!!)
    }

    private fun getRichestUser(channel: String): String? {
        var richestUser: String? = null
        var richestAmount: Long = 0
        if (getTippingService(channel) == null) return null
        getTippingService(channel)!!.asSequence().forEach {
            if (richestUser == null || richestAmount < it.value) {
                richestUser = it.key
                richestAmount = it.value
            } else if (richestAmount == it.value) {
                return null
            }
        }
        return richestUser
    }

    private fun resetBotIn(channel: String) {
        /*
        // reset surveys
        surveys.asSequence().filter {
            !surveyCallbacks[channel]!!.contains(it.value.first)
        }.toList()          // TODO ("fix it")

        //TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
        */
    }

    // ========================== getters, setters & inits for storage structures ======================================
    private fun channelsList(): MutableList<String> {
        if (channels[botName] == null)
            channels[botName] = mutableListOf()
        return channels[botName]!!
    }

    private fun addToChannelsList(channelName: String) {
        if (channels[botName] == null)       //if list not exist yet, make a new list
            channels[botName] = mutableListOf()
        if (channels[botName]!!.contains(channelName))
            return
        channels[botName]!!.add(channelName)

    }

    private fun removeFromChannelsList(channelName: String) {
        if (channels[botName] == null || !channels[botName]!!.contains(channelName))
            throw Exception()
        channels[botName]!!.remove(channelName)
    }

    private fun getTriggerPhrase(): String? {
        return triggerPhrase[botName]
    }

    private fun setTriggerPhrase(trigger: String?) {
        triggerPhrase[botName] = trigger
    }

    private fun getTipTriggerPhrase(): String? {
        return tipTriggerPhrase[botName]
    }

    private fun setTipTriggerPhrase(trigger: String?) {
        tipTriggerPhrase[botName] = trigger
    }

    private fun getSurveys(id: String): LongArray? {
        return surveys[id]
    }

    private fun isSurveysExists(id: String): Boolean {
        return surveys[id] != null
    }

    private fun initSurveys(id: String, size: Int) {
        val resultsList = LongArray(size) { 0 }

        surveys[id] = resultsList
    }

    private fun incSurveyVote(surveyId: String, answerId: Int) {
        surveys[surveyId]!![answerId]++
    }

    private fun initSurveysHistory(id: String) {
        surveysHistory[id] = mutableSetOf()
    }

    private fun addToSurveysHistory(id: String, p: Pair<String, Int>) {
        surveysHistory[id]!!.removeIf { it.first == p.first }
        surveysHistory[id]!!.add(p)
    }

    private fun isUserVoteFor(id: String, voter: String): Boolean {
        surveysHistory[id]!!.asSequence().forEach {
            if (it.first == voter)
                return true
        }
        return false
    }

    private fun getSurveyId(): String {
        val retId = surveyIdGen.toString()
        surveyIdGen++
        return retId
    }

    private fun initSeenLastTimeIn(channel: String) {
        seenLastTimeService[botName]!![channel] = HashMap()
    }

    private fun setSeenLastTimeFor(channel: String, user: String) {
        seenLastTimeService[botName]!![channel]!![user] = LocalDateTime.now()
    }

    private fun getSeenLastTimeFor(channel: String, user: String): LocalDateTime? {
        return seenLastTimeService[botName]!![channel]!![user]
    }

    private fun initCounters() {
        counters[botName] = hashMapOf()
    }

    private fun initCountersOf(channel: String) {
        counters[botName]!![channel] = hashMapOf()
    }

    private fun isCountersExists(): Boolean {
        return counters[botName] != null
    }

    private fun getCounters(): HashMap<String, HashMap<Pair<String?, MediaType?>, Long>>? {
        return counters[botName]
    }

    private fun getCounterOf(channel: String, p: Pair<String?, MediaType?>): Long? {
        return counters[botName]!![channel]!![p]
    }

    private fun incCounterOf(channel: String, p: Pair<String?, MediaType?>) {
        val previousCounter = counters[botName]!![channel]!![p] ?: 0.toLong()
        counters[botName]!![channel]!![p] = previousCounter + 1
    }

    private fun isCountersOfExists(channel: String): Boolean {
        return counters[botName]!![channel] != null
    }

    private fun removeCounterFrom(channel: String, p: Pair<String?, MediaType?>) {
        counters[botName]!![channel]?.remove(p)
    }

    private fun resetCounter(channel: String, p: Pair<String?, MediaType?>) {
        counters[botName]!![channel]!![p] = 0.toLong()
    }

    private fun initMostActiveService(channel: String) {
        mostActiveService[botName]!![channel] = HashMap()
    }

    private fun getMostActiveServiceOf(channel: String, user: String): Long? {
        return mostActiveService[botName]!![channel]!![user]
    }

    private fun setMostActiveServiceOf(channel: String, user: String, value: Long) {
        mostActiveService[botName]!![channel]!![user] = value
    }

    private fun getMostActiveService(channel: String): HashMap<String, Long>? {
        return mostActiveService[botName]!![channel]
    }

    private fun initTippingService(channel: String) {
        tippingService[botName]!![channel] = HashMap()
    }

    private fun getTippingServiceFor(channel: String, user: String): Long? {
        return tippingService[botName]!![channel]!![user]
    }

    private fun setTippingService(channel: String, user: String, value: Long) {
        tippingService[botName]!![channel]!![user] = value
    }

    private fun getTippingService(channel: String): HashMap<String, Long>? {
        return tippingService[botName]!![channel]
    }

    // TODO ("counter with triplet issue...")
    // TODO ("issue with zeroing counters and all the stats for channels after parting the channel")
    // TODO ("choose library")
    // TODO ("move data structures library")
    // TODO ("pass to tests")
    // TODO ("excretions eval - need to correct it... examples: (20) +2 ; (2 + 4) ; (2 + -2) ; 2*20 + 5 : -2 + -2 : (2+3)*5 : (2 +     6 ) *     7")
}